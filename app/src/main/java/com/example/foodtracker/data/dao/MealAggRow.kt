package com.example.foodtracker.data.dao

import java.time.LocalDate

data class MealAggRow(
    val id: Long,
    val name: String,
    val prepDate: LocalDate,
    val storage: String,
    val totalPortions: Int,
    val shelfLifeDays: Int,
    val notes: String?,
    val eatenPortions: Int
)