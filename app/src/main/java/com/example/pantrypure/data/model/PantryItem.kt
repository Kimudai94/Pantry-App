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
  val expiryDate: Long? = null, // Timestamp in milliseconds
  val expiryThresholdDays: Int = 3, // Default alert threshold
  val isOnShoppingList: Boolean = false,
  val category: String = "Other",
  val location: String = "Fridge",
  val notes: String = "",
  val timesBought: Int = 0,
  val neededQuantity: Double = 0.0,
)

enum class ExpiryStatus { NORMAL, EXPIRING_SOON, OVERDUE }

fun PantryItem.getExpiryStatus(now: Long = System.currentTimeMillis()): ExpiryStatus {
  if (expiryDate == null) return ExpiryStatus.NORMAL
  val thresholdMillis = expiryThresholdDays * 24 * 60 * 60 * 1000L
  return when {
    expiryDate < now -> ExpiryStatus.OVERDUE
    expiryDate <= (now + thresholdMillis) -> ExpiryStatus.EXPIRING_SOON
    else -> ExpiryStatus.NORMAL
  }
}