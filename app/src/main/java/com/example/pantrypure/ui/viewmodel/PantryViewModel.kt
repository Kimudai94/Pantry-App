package com.example.pantrypure.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrypure.data.model.ConsumptionRecord
import com.example.pantrypure.data.model.Meal
import com.example.pantrypure.data.model.MealCategory
import com.example.pantrypure.data.model.MealConsumptionResult
import com.example.pantrypure.data.model.MealIngredient
import com.example.pantrypure.data.model.MealPlan
import com.example.pantrypure.data.model.MealPlanWithDetails
import com.example.pantrypure.data.model.MealWithIngredients
import com.example.pantrypure.data.model.PantryItem
import com.example.pantrypure.data.repository.PantryRepository
import com.example.pantrypure.data.model.MissingIngredient
import com.example.pantrypure.data.model.PantryUnit
import com.example.pantrypure.data.util.UnitConverter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class SortOption { NAME, EXPIRY_DATE }
enum class FilterOption { ALL, OVERDUE, EXPIRING_SOON }

class PantryViewModel(private val repository: PantryRepository) : ViewModel() {

    // ============== Pantry Item State ==============
    private val _sortOption = MutableStateFlow(SortOption.EXPIRY_DATE)
    val sortOption: StateFlow<SortOption> = _sortOption
    fun setSortOption(option: SortOption) { _sortOption.value = option }
    private val _filterOption = MutableStateFlow(FilterOption.ALL)
    val filterOption: StateFlow<FilterOption> = _filterOption
    fun setFilterOption(option: FilterOption) { _filterOption.value = option }
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    val pantryItems: StateFlow<List<PantryItem>> = combine(
        repository.getAllItems(),
        _sortOption,
        _filterOption,
        _searchQuery
    ) { items, sort, filter, query ->
        val now = System.currentTimeMillis()

        items.filter { item ->
            val matchesSearch = item.name.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                FilterOption.ALL -> true
                FilterOption.OVERDUE -> item.expiryDate != null && item.expiryDate < now
                FilterOption.EXPIRING_SOON -> {
                    if (item.expiryDate == null) false
                    else {
                        val thresholdMillis = item.expiryThresholdDays * 24 * 60 * 60 * 1000L
                        item.expiryDate in now..(now + thresholdMillis)
                    }
                }
            }
            matchesSearch && matchesFilter
        }.sortedWith { a, b ->
            when (sort) {
                SortOption.NAME -> a.name.compareTo(b.name, ignoreCase = true)
                SortOption.EXPIRY_DATE -> {
                    val dateA = a.expiryDate ?: Long.MAX_VALUE
                    val dateB = b.expiryDate ?: Long.MAX_VALUE
                    dateA.compareTo(dateB)
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ============== Pantry Item Methods ==============
    suspend fun addItem(item: PantryItem): Long { return repository.insertItem(item) }
    suspend fun updateItem(item: PantryItem) { repository.updateItem(item) }
    suspend fun deleteItem(item: PantryItem) { repository.deleteItem(item) }
    suspend fun getItemById(id: Long): PantryItem? { return repository.getItemById(id) }
    fun duplicateItem(item: PantryItem) {
        viewModelScope.launch {
            repository.insertItem(item.copy(id = 0, name = "${item.name} (Copy)"))
        }
    }
    fun consumeOne(item: PantryItem) {
        if (item.quantity > 0) {
            viewModelScope.launch {
                val newQuantity = (item.quantity - 1.0).coerceAtLeast(0.0)
                if (newQuantity <= item.quantityThreshold && !item.isOnShoppingList) {
                  toggleShoppingListStatus(item)
                }
                repository.updateItem(item.copy(quantity = newQuantity))

                repository.insertConsumptionRecord(
                    ConsumptionRecord(
                        itemId = item.id,
                        itemName = item.name,
                        quantityConsumed = 1.0,
                        unit = item.unit
                    )
                )
            }
        }
    }
    val consumptionHistory: StateFlow<List<ConsumptionRecord>> = repository.getConsumptionHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    fun clearHistory() { viewModelScope.launch { repository.clearHistory() } }
    val shoppingListItems: StateFlow<List<PantryItem>> = repository.getShoppingListItems()
        .map { items ->
            items.groupBy { it.name }.map { (_, group) ->
                val firstItem = group.first()
                val totalQuantity = group.sumOf { it.quantity }
                val totalNeeded = group.sumOf { it.neededQuantity }
                
                firstItem.copy(
                    quantity = totalQuantity,
                    neededQuantity = totalNeeded
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    fun toggleShoppingListStatus(item: PantryItem) {
        viewModelScope.launch {
            repository.updateShoppingListStatusByName(item.name, !item.isOnShoppingList)
        }
    }

    // ============== Meal State ==============
    private val _selectedMealCategory = MutableStateFlow(MealCategory.OTHER)
    val selectedMealCategory: StateFlow<MealCategory> = _selectedMealCategory
    val allMealsWithIngredients: StateFlow<List<MealWithIngredients>> = repository.getAllMealsWithIngredients()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val mealsByCategory: StateFlow<List<MealWithIngredients>> = combine(
        allMealsWithIngredients,
        _selectedMealCategory
    ) { meals, category ->
        meals.filter { it.meal.category == category }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    fun setMealCategory(category: MealCategory) { _selectedMealCategory.value = category }

    private val _mealOperationState = MutableStateFlow<MealOperationState>(MealOperationState.Idle)
    val mealOperationState: StateFlow<MealOperationState> = _mealOperationState
    fun clearMealOperationState() { _mealOperationState.value = MealOperationState.Idle }

    // ============== Meal Methods ==============

    suspend fun getMealWithIngredients(mealId: Long): MealWithIngredients? {
        return repository.getMealWithIngredients(mealId)
    }

    fun saveMeal(meal: Meal, ingredients: List<MealIngredient>) {
        viewModelScope.launch {
            try {
                repository.saveMealWithIngredients(meal, ingredients)
                _mealOperationState.value = MealOperationState.Success(
                    if (meal.id == 0L) "Mahlzeit erstellt" else "Mahlzeit aktualisiert"
                )
            } catch (e: Exception) {
                _mealOperationState.value = MealOperationState.Error(e.message ?: "Fehler beim Speichern")
            }
        }
    }

    fun consumeMeal(mealId: Long) {
        viewModelScope.launch {
            _mealOperationState.value = MealOperationState.Loading

            val result = repository.consumeMeal(mealId)
            _mealOperationState.value = when (result) {
                MealConsumptionResult.Success ->
                    MealOperationState.Success("Mahlzeit verbraucht")
                MealConsumptionResult.NotFound ->
                    MealOperationState.Error("Mahlzeit nicht gefunden")
                is MealConsumptionResult.InsufficientIngredients ->
                    MealOperationState.InsufficientIngredients(result.missingIngredients)
                is MealConsumptionResult.Error ->
                    MealOperationState.Error(result.message)
            }
        }
    }

    fun deleteMeal(meal: Meal) {
        viewModelScope.launch {
            try {
                repository.deleteMeal(meal)
                _mealOperationState.value = MealOperationState.Success("Mahlzeit gelöscht")
            } catch (e: Exception) {
                _mealOperationState.value = MealOperationState.Error(e.message ?: "Fehler beim Löschen")
            }
        }
    }

    // ============== Meal Plan State ==============
    val _currentPlannerDate = MutableStateFlow(getStartOfWeek(System.currentTimeMillis()))
    val currentPlannerDate: StateFlow<Long> = _currentPlannerDate

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val weeklyMealPlans: StateFlow<List<MealPlanWithDetails>> = _currentPlannerDate
        .flatMapLatest { startDate ->
            val endDate = startDate + (7 * 24 * 60 * 60 * 1000L) - 1
            repository.getMealPlansInRange(startDate, endDate)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val mealPlanSimulation: StateFlow<Map<Long, List<MissingIngredient>>> = combine(
        repository.getAllItems(),
        weeklyMealPlans
    ) { inventory, plans ->
        simulateConsumption(inventory, plans)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    /**
     * Aggregiert alle Defizite aus der Mahlzeitenplanung für die Einkaufsliste.
     */
    val plannedShoppingNeeds: StateFlow<List<MissingIngredient>> = mealPlanSimulation
        .map { simulation ->
            simulation.values.flatten()
                .groupBy { it.itemName.trim().lowercase() }
                .mapNotNull { (name, group) ->
                    // Wir gruppieren innerhalb des Namens noch nach kompatiblen Einheiten (z.B. L/ml vs. Stücke)
                    val subgroupedByBaseUnit = group.groupBy { UnitConverter.getBaseUnit(it.unit) }
                    
                    subgroupedByBaseUnit.map { (baseUnit, subGroup) ->
                        val totalRequiredInBase = subGroup.sumOf { UnitConverter.convert(it.required, it.unit, baseUnit) }
                        val totalDeficitInBase = subGroup.sumOf { UnitConverter.convert(it.deficit, it.unit, baseUnit) }
                        
                        // Liter und KG als Standard für die Zusammenfassung verwenden
                        var finalUnit = baseUnit
                        var finalRequired = totalRequiredInBase
                        var finalDeficit = totalDeficitInBase
                        
                        if (baseUnit == PantryUnit.MILLILITERS) {
                            finalUnit = PantryUnit.LITERS
                            finalRequired = totalRequiredInBase / 1000.0
                            finalDeficit = totalDeficitInBase / 1000.0
                        } else if (baseUnit == PantryUnit.GRAMS) {
                            finalUnit = PantryUnit.KILOGRAMS
                            finalRequired = totalRequiredInBase / 1000.0
                            finalDeficit = totalDeficitInBase / 1000.0
                        }

                        val first = subGroup.first()
                        MissingIngredient(
                            pantryItemId = first.pantryItemId,
                            itemName = first.itemName.trim(),
                            unit = finalUnit,
                            required = finalRequired,
                            available = (finalRequired - finalDeficit).coerceAtLeast(0.0),
                            deficit = finalDeficit
                        )
                    }
                }
                .flatten()
                .filter { it.deficit > 0.001 }
                .sortedBy { it.itemName }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun simulateConsumption(
        inventory: List<PantryItem>,
        plans: List<MealPlanWithDetails>
    ): Map<Long, List<MissingIngredient>> {
        val virtualInventory = inventory.groupBy { it.name }.mapValues { entry ->
            entry.value.toMutableList()
        }.toMutableMap()

        val missingResults = mutableMapOf<Long, List<MissingIngredient>>()

        // Sort plans by date to simulate chronological consumption
        plans.sortedBy { it.plan.plannedDate }.forEach { planWithDetails ->
            if (planWithDetails.plan.isConsumed) return@forEach

            val missingForPlan = mutableListOf<MissingIngredient>()

            planWithDetails.ingredients.forEach { ingredient ->
                val items = virtualInventory[ingredient.ingredientName] ?: mutableListOf()
                val remainingToNeed = ingredient.requiredQuantity * planWithDetails.plan.servings

                // Calculate total available for this ingredient in virtual inventory
                var totalAvailable = 0.0
                items.forEach { item ->
                    try {
                        totalAvailable += UnitConverter.convert(item.quantity, item.unit, ingredient.requiredUnit)
                    } catch (e: Exception) {}
                }

                if (totalAvailable < remainingToNeed) {
                    missingForPlan.add(
                        MissingIngredient(
                            pantryItemId = ingredient.pantryItemId,
                            itemName = ingredient.ingredientName,
                            required = remainingToNeed,
                            available = totalAvailable,
                            unit = ingredient.requiredUnit,
                            deficit = remainingToNeed - totalAvailable
                        )
                    )
                } else {
                    // Deduct from virtual inventory
                    var deducted = 0.0
                    val iterator = items.listIterator()
                    while (iterator.hasNext() && deducted < remainingToNeed) {
                        val item = iterator.next()
                        try {
                            val availInReq = UnitConverter.convert(item.quantity, item.unit, ingredient.requiredUnit)
                            val toTake = minOf(availInReq, remainingToNeed - deducted)
                            val toTakeInOrig = UnitConverter.convert(toTake, ingredient.requiredUnit, item.unit)
                            
                            val newQty = item.quantity - toTakeInOrig
                            iterator.set(item.copy(quantity = newQty))
                            deducted += toTake
                        } catch (e: Exception) {}
                    }
                }
            }
            if (missingForPlan.isNotEmpty()) {
                val aggregatedMissing = missingForPlan.groupBy { it.itemName.trim().lowercase() to it.unit }
                    .map { (key, group) ->
                        val first = group.first()
                        val totalRequired = group.sumOf { it.required }
                        val totalDeficit = group.sumOf { it.deficit }
                        MissingIngredient(
                            pantryItemId = first.pantryItemId,
                            itemName = first.itemName.trim(),
                            unit = key.second,
                            required = totalRequired,
                            available = (totalRequired - totalDeficit).coerceAtLeast(0.0),
                            deficit = totalDeficit
                        )
                    }
                missingResults[planWithDetails.plan.id] = aggregatedMissing
            }
        }
        return missingResults
    }

    fun movePlannerWeek(weeks: Int) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = _currentPlannerDate.value
        calendar.add(Calendar.WEEK_OF_YEAR, weeks)
        _currentPlannerDate.value = getStartOfWeek(calendar.timeInMillis)
    }

    fun planMeal(mealId: Long, date: Long) {
        viewModelScope.launch {
            repository.insertMealPlan(MealPlan(mealId = mealId, plannedDate = getStartOfDay(date)))
        }
    }

    fun updateMealPlanDate(plan: MealPlan, newDate: Long) {
        viewModelScope.launch {
            repository.updateMealPlan(plan.copy(plannedDate = getStartOfDay(newDate)))
        }
    }

    fun updateMealPlanServings(plan: MealPlan, servings: Int) {
        if (servings < 1) return
        viewModelScope.launch {
            repository.updateMealPlan(plan.copy(servings = servings))
        }
    }

    fun deleteMealPlan(plan: MealPlan) {
        viewModelScope.launch {
            repository.deleteMealPlan(plan)
        }
    }

    fun toggleMealPlanConsumed(plan: MealPlan) {
        viewModelScope.launch {
            repository.updateConsumedStatus(plan.id, !plan.isConsumed)
        }
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getStartOfWeek(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Auf Montag setzen
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        return calendar.timeInMillis
    }
}
