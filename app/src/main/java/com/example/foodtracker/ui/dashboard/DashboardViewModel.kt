package com.example.foodtracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtracker.domain.model.Meal
import com.example.foodtracker.domain.model.MealStatus
import com.example.foodtracker.domain.usecase.ComputeMealStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class DashboardViewModel(
  mealsFlow: Flow<List<Meal>>,
  private val computeMealStatus: ComputeMealStatus,
  private val today: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {
  val state: StateFlow<DashboardUiState> = mealsFlow
    .map { meals -> meals.toDashboardUiState(today()) }
    .stateIn( viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState(isLoading = true) )

  private fun List<Meal>.toDashboardUiState(today: LocalDate): DashboardUiState {
    val items = map { meal ->
      val status = computeMealStatus(meal, today)
      val expiresAt = meal.prepDate.plusDays(meal.shelfLifeDays.toLong())
      val expiresInDays = ChronoUnit.DAYS.between(today, expiresAt)

      DashboardMealItem(
        mealId = meal.id,
        name = meal.name,
        remainingPortions = meal.totalPortions,
        expiresInDays = expiresInDays,
        status = status
      )
    }

    return DashboardUiState(
      nowEating = items.filter { it.status == MealStatus.SOON || it.status == MealStatus.EXPIRED },
      thisWeek = items.filter { it.status == MealStatus.FRESH && it.expiresInDays <= 7 },
      freezerReserve = items.filter { it.status == MealStatus.FRESH }, // später: storage == FREEZER
      isLoading = false,
      error = null
    )
  }
}