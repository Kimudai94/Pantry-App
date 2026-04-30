package com.example.pantrypure.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrypure.data.model.*
import com.example.pantrypure.data.repository.PantryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOption { NAME, EXPIRY_DATE }
enum class FilterOption { ALL, OVERDUE, EXPIRING_SOON }

class PantryViewModel(private val repository: PantryRepository) : ViewModel() {

    // ============== Pantry Item State ==============
    private val _sortOption = MutableStateFlow(SortOption.EXPIRY_DATE)
    val sortOption: StateFlow<SortOption> = _sortOption

    private val _filterOption = MutableStateFlow(FilterOption.ALL)
    val filterOption: StateFlow<FilterOption> = _filterOption

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

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
    fun addItem(item: PantryItem) {
        viewModelScope.launch {
            repository.insertItem(item)
        }
    }

    fun updateItem(item: PantryItem) {
        viewModelScope.launch {
            repository.updateItem(item)
        }
    }

    fun deleteItem(item: PantryItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    fun duplicateItem(item: PantryItem) {
        viewModelScope.launch {
            repository.insertItem(item.copy(id = 0, name = "${item.name} (Copy)"))
        }
    }

    fun consumeOne(item: PantryItem) {
        if (item.quantity > 0) {
            viewModelScope.launch {
                val newQuantity = (item.quantity - 1.0).coerceAtLeast(0.0)
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

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    suspend fun getItemById(id: Long): PantryItem? {
        return repository.getItemById(id)
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun setFilterOption(option: FilterOption) {
        _filterOption.value = option
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val shoppingListItems: StateFlow<List<PantryItem>> = repository.getShoppingListItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleShoppingListStatus(item: PantryItem) {
        viewModelScope.launch {
            repository.updateShoppingListStatus(item.id, !item.isOnShoppingList)
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

    private val _mealOperationState = MutableStateFlow<MealOperationState>(MealOperationState.Idle)
    val mealOperationState: StateFlow<MealOperationState> = _mealOperationState

    // ============== Meal Methods ==============
    fun createMeal(meal: Meal) {
        viewModelScope.launch {
            try {
                repository.insertMeal(meal)
                _mealOperationState.value = MealOperationState.Success("Mahlzeit erstellt")
            } catch (e: Exception) {
                _mealOperationState.value = MealOperationState.Error(e.message ?: "Fehler beim Erstellen")
            }
        }
    }

    fun updateMeal(meal: Meal) {
        viewModelScope.launch {
            try {
                repository.updateMeal(meal)
                _mealOperationState.value = MealOperationState.Success("Mahlzeit aktualisiert")
            } catch (e: Exception) {
                _mealOperationState.value = MealOperationState.Error(e.message ?: "Fehler beim Aktualisieren")
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

    suspend fun getMealWithIngredients(mealId: Long): MealWithIngredients? {
        return repository.getMealWithIngredients(mealId)
    }

    fun addIngredientToMeal(ingredient: MealIngredient) {
        viewModelScope.launch {
            try {
                repository.addIngredientToMeal(ingredient)
                _mealOperationState.value = MealOperationState.Success("Zutat hinzugefügt")
            } catch (e: Exception) {
                _mealOperationState.value = MealOperationState.Error(e.message ?: "Fehler beim Hinzufügen der Zutat")
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

    fun setMealCategory(category: MealCategory) {
        _selectedMealCategory.value = category
    }

    fun clearMealOperationState() {
        _mealOperationState.value = MealOperationState.Idle
    }
}
