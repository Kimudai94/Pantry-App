package com.example.foodtracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "meal_event")
data class MealEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,
    val type: String, // EATEN / DISCARDED
    val portions: Int,
    val occurredAt: LocalDateTime
)
