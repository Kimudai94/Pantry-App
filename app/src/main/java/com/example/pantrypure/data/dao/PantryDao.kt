package com.example.pantrypure.data.dao

import androidx.room.*
import com.example.pantrypure.data.model.PantryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry_items ORDER BY expiryDate ASC")
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

    @Query("SELECT * FROM pantry_items WHERE isOnShoppingList = 1")
    fun getShoppingListItems(): Flow<List<PantryItem>>

    @Query("UPDATE pantry_items SET isOnShoppingList = :isOnList WHERE id = :id")
    suspend fun updateShoppingListStatus(id: Long, isOnList: Boolean)
}
