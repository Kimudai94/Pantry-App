package com.example.foodtracker.domain.usecase

import com.example.foodtracker.domain.model.Meal
import com.example.foodtracker.domain.model.MealStatus
import java.time.LocalDate

class ComputeMealStatus {
  operator fun invoke(meal: Meal, today: LocalDate = LocalDate.now()): MealStatus {
    val expiresAt = meal.prepDate.plusDays(meal.shelfLifeDays.toLong())
    return when {
      today.isAfter(expiresAt) -> MealStatus.EXPIRED
      today.isAfter(expiresAt.minusDays(1)) -> MealStatus.SOON
      else -> MealStatus.FRESH
    }
  }
}
