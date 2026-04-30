package com.example.pantrypure.ui.viewmodel

import com.example.pantrypure.data.model.MissingIngredient

sealed class MealOperationState {
    object Idle : MealOperationState()
    object Loading : MealOperationState()
    data class Success(val message: String) : MealOperationState()
    data class Error(val message: String) : MealOperationState()
    data class InsufficientIngredients(
        val missingItems: List<MissingIngredient>
    ) : MealOperationState()
}
