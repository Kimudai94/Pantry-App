package com.example.pantrypure.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "consumption_history")
data class ConsumptionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: Long,
    val itemName: String,
    val quantityConsumed: Double,
    val unit: PantryUnit,
    val consumptionDate: Long = System.currentTimeMillis()
)
