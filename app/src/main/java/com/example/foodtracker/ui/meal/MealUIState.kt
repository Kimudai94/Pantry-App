package com.example.foodtracker.ui.meal

import com.example.foodtracker.domain.model.MealStatus
import com.example.foodtracker.domain.model.StorageLocation
import java.time.LocalDate

data class MealUiState(
  val mealId: Long = 0,
  val name: String = "",
  val prepDate: LocalDate = LocalDate.now(),
  val storage: StorageLocation = StorageLocation.FRIDGE,
  val totalPortions: Int = 0,
  val remainingPortions: Int = 0,
  val shelfLifeDays: Int = 0,
  val status: MealStatus = MealStatus.FRESH,
  val expiresAt: LocalDate = LocalDate.now(),
  val notes: String? = null,
  val isLoading: Boolean = true,
  val error: String? = null
)