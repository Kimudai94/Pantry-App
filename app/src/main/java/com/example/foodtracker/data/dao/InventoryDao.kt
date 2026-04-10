package com.example.foodtracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.foodtracker.data.db.entity.InventoryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

  @Query("SELECT * FROM inventory_item ORDER BY name")
  fun observeInventory(): Flow<List<InventoryItemEntity>>

  @Query("SELECT * FROM inventory_item WHERE id = :id")
  fun getById(id: Long): InventoryItemEntity

    @Update
    suspend fun update(item: InventoryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InventoryItemEntity): Long
}