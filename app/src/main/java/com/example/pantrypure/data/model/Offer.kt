package com.example.pantrypure.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offers")
data class Offer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemName: String,
    val store: String,
    val price: Double,
    val originalPrice: Double? = null,
    val offerQuantity: Double,
    val offerUnit: PantryUnit,
    val validFrom: Long,
    val validUntil: Long,
    val isAddedToShoppingList: Boolean = false,
    val category: String = "Prospekt"
) {
    val pricePerUnit: Double
        get() = if (offerQuantity > 0) price / offerQuantity else 0.0
}

data class ConsumptionSummary(
    val itemName: String,
    val totalConsumed: Double,
    val unit: PantryUnit
)
