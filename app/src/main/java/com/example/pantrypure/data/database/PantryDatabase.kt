package com.example.pantrypure.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.pantrypure.data.dao.PantryDao
import com.example.pantrypure.data.dao.ConsumptionDao
import com.example.pantrypure.data.model.PantryItem
import com.example.pantrypure.data.model.ConsumptionRecord

@Database(entities = [PantryItem::class, ConsumptionRecord::class], version = 4, exportSchema = false)
@TypeConverters(PantryTypeConverters::class)
abstract class PantryDatabase : RoomDatabase() {
    abstract fun pantryDao(): PantryDao
    abstract fun consumptionDao(): ConsumptionDao
}
