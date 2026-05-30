package com.example.pantrypure.data.dao

import androidx.room.*
import com.example.pantrypure.data.model.PantryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry_items WHERE quantity > 0 ORDER BY expiryDate ASC")
    fun getAllItems(): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items WHERE id = :id")
    suspend fun getItemById(id: Long): PantryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PantryItem): Long

    @Update
    suspend fun updateItem(item: PantryItem)

    @Delete
    suspend fun deleteItem(item: PantryItem)

    @Query("SELECT * FROM pantry_items WHERE category = :category")
    fun getItemsByCategory(category: String): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items WHERE isOnShoppingList = 1 ORDER BY name ASC")
    fun getShoppingListItems(): Flow<List<PantryItem>>

    @Query("UPDATE pantry_items SET isOnShoppingList = :isOnList WHERE name = :name")
    suspend fun updateShoppingListStatusByName(name: String, isOnList: Boolean)

    @Query("SELECT * FROM pantry_items WHERE name = :name AND quantity > 0 ORDER BY CASE WHEN expiryDate IS NULL THEN 1 ELSE 0 END, expiryDate ASC")
    suspend fun getItemsByNameSortedByExpiry(name: String): List<PantryItem>

    @Query("SELECT SUM(quantity) FROM pantry_items WHERE name = :name")
    suspend fun getTotalQuantityByName(name: String): Double?
    
    @Query("UPDATE pantry_items SET neededQuantity = 0.0 WHERE name = :name")
    suspend fun resetNeededQuantityByName(name: String)

    @Query("UPDATE pantry_items SET neededQuantity = :needed, isOnShoppingList = :isOnList WHERE id = :id")
    suspend fun updateShoppingStatusAndNeeded(id: Long, needed: Double, isOnList: Boolean)
}
