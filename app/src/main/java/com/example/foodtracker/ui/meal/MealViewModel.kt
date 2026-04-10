package com.example.foodtracker.ui.meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtracker.domain.model.Meal
import com.example.foodtracker.domain.usecase.ComputeMealStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MealViewModel(
  mealFlow: Flow<Meal>,
  private val onEatOnePortion: suspend () -> Unit,
  private val computeMealStatus: ComputeMealStatus,
  private val today: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {
  val state: StateFlow<MealUiState> = mealFlow
    .map { meal -> meal.toUi(today()) }
    .stateIn( viewModelScope, SharingStarted.WhileSubscribed(5_000), MealUiState(isLoading = true) )

  fun eatOnePortion() {
    viewModelScope.launch { onEatOnePortion() }
  }

  private fun Meal.toUi(today: LocalDate): MealUiState {
    val expiresAt = prepDate.plusDays(shelfLifeDays.toLong())
    val status = computeMealStatus(this, today)
    return MealUiState(
      mealId = id,
      name = name,
      prepDate = prepDate,
      storage = storage,
      totalPortions = totalPortions,
      remainingPortions = totalPortions,
      shelfLifeDays = shelfLifeDays,
      status = status,
      expiresAt = expiresAt,
      notes = notes,
      isLoading = false
    )
  }
}
