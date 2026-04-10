package com.example.foodtracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.foodtracker.data.dao.MealDao
import com.example.foodtracker.data.dao.MealEventDao
import com.example.foodtracker.data.dao.InventoryDao
import com.example.foodtracker.data.db.entity.MealEntity
import com.example.foodtracker.data.db.entity.MealEventEntity
import com.example.foodtracker.data.db.entity.InventoryItemEntity


@Database(
    entities = [MealEntity::class, MealEventEntity::class, InventoryItemEntity::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
  abstract fun mealEventDao(): MealEventDao
  abstract fun inventoryDao(): InventoryDao
}