package com.example.pantrypure.data.database

import androidx.room.TypeConverter
import com.example.pantrypure.data.model.PantryUnit
import com.example.pantrypure.data.model.MealCategory

class PantryTypeConverters {
    @TypeConverter
    fun fromPantryUnit(unit: PantryUnit): String {
        return unit.name
    }

    @TypeConverter
    fun toPantryUnit(unit: String): PantryUnit {
        return PantryUnit.valueOf(unit)
    }

    @TypeConverter
    fun fromMealCategory(category: MealCategory): String {
        return category.name
    }

    @TypeConverter
    fun toMealCategory(category: String): MealCategory {
        return MealCategory.valueOf(category)
    }
}
