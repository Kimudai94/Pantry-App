package com.example.foodtracker.domain.usecase

import com.example.foodtracker.domain.model.Meal

class ConsumeMealPortion {
    operator fun invoke(meal: Meal): Meal {
        require(meal.totalPortions > 0) { "No portions left" }
        return meal.copy(totalPortions = meal.totalPortions - 1)
    }
}
