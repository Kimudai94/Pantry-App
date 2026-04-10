package com.example.foodtracker.ui.dashboard

import com.example.foodtracker.domain.model.MealStatus
data class DashboardUiState(
  val nowEating: List<DashboardMealItem> = emptyList(),
  val thisWeek: List<DashboardMealItem> = emptyList(),
  val freezerReserve: List<DashboardMealItem> = emptyList(),
  val isLoading: Boolean = true,
  val error: String? = null
)
data class DashboardMealItem(
  val mealId: Long,
  val name: String,
  val remainingPortions: Int,
  val expiresInDays: Long,
  val status: MealStatus
)
