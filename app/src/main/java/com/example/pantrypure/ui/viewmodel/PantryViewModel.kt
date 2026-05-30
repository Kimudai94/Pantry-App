package com.example.pantrypure.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrypure.data.model.ConsumptionRecord
import com.example.pantrypure.data.model.Meal
import com.example.pantrypure.data.model.MealCategory
import com.example.pantrypure.data.model.MealConsumptionResult
import com.example.pantrypure.data.model.MealIngredient
import com.example.pantrypure.data.model.MealWithIngredients
import com.example.pantrypure.data.model.PantryItem
import com.example.pantrypure.data.repository.PantryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
}
