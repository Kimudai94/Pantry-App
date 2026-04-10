package com.example.foodtracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "meal")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val prepDate: LocalDate,
    val storage: String, // FRIDGE / FREEZER
    val totalPortions: Int,
    val shelfLifeDays: Int,
    val notes: String?
) {
}
