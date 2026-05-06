package com.example.pantrypure.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pantry_items")
data class PantryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val quantity: Double = 0.0,
    val quantityThreshold: Double = 1.0,
    val unit: PantryUnit,
    val expiryDate: Long?, // Timestamp in milliseconds
    val expiryThresholdDays: Int = 3, // Default alert threshold
    val isOnShoppingList: Boolean = false,
    val category: String,
    val location: String = "Fridge",
    val notes: String = ""
)
