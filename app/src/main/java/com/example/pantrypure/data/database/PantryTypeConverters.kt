package com.example.pantrypure.data.database

import androidx.room.TypeConverter
import com.example.pantrypure.data.model.PantryUnit

class PantryTypeConverters {
    @TypeConverter
    fun fromPantryUnit(unit: PantryUnit): String {
        return unit.name
    }

    @TypeConverter
    fun toPantryUnit(unit: String): PantryUnit {
        return PantryUnit.valueOf(unit)
    }
}
