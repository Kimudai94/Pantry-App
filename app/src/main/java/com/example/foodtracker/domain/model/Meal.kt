package com.example.foodtracker.domain.model

import java.time.LocalDate

/** Ein vorbereitetes Gericht (Batch) */
data class Meal(
  val id: Long,
  val name: String,
  val prepDate: LocalDate,
  val storage: StorageLocation,
  val totalPortions: Int,
  val shelfLifeDays: Int,
  val notes: String? = null
)

enum class MealStatus {
  FRESH,
  SOON,
  EXPIRED
}

enum class StorageLocation {
  FRIDGE,
  FREEZER
}
