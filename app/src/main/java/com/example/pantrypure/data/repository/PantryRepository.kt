package com.example.pantrypure.data.repository

import com.example.pantrypure.data.dao.PantryDao
import com.example.pantrypure.data.dao.ConsumptionDao
import com.example.pantrypure.data.model.PantryItem
import com.example.pantrypure.data.model.ConsumptionRecord
import kotlinx.coroutines.flow.Flow

class PantryRepository(
    private val pantryDao: PantryDao,
    private val consumptionDao: ConsumptionDao
) {
    fun getAllItems(): Flow<List<PantryItem>> = pantryDao.getAllItems()

    suspend fun getItemById(id: Long): PantryItem? = pantryDao.getItemById(id)

    suspend fun insertItem(item: PantryItem) = pantryDao.insertItem(item)

    suspend fun updateItem(item: PantryItem) = pantryDao.updateItem(item)

    suspend fun deleteItem(item: PantryItem) = pantryDao.deleteItem(item)

    fun getItemsByCategory(category: String): Flow<List<PantryItem>> = pantryDao.getItemsByCategory(category)

    fun getShoppingListItems(): Flow<List<PantryItem>> = pantryDao.getShoppingListItems()

    suspend fun updateShoppingListStatus(id: Long, isOnList: Boolean) = pantryDao.updateShoppingListStatus(id, isOnList)

    fun getConsumptionHistory(): Flow<List<ConsumptionRecord>> = consumptionDao.getAllHistory()

    suspend fun insertConsumptionRecord(record: ConsumptionRecord) = consumptionDao.insertRecord(record)

    suspend fun clearHistory() = consumptionDao.clearHistory()
}
