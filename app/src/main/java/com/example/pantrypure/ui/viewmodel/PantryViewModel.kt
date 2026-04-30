package com.example.pantrypure.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrypure.data.model.PantryItem
import com.example.pantrypure.data.model.ConsumptionRecord
import com.example.pantrypure.data.repository.PantryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOption { NAME, EXPIRY_DATE }
enum class FilterOption { ALL, OVERDUE, EXPIRING_SOON }

class PantryViewModel(private val repository: PantryRepository) : ViewModel() {

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
}
