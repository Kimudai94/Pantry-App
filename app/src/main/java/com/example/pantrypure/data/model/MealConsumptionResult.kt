package com.example.pantrypure.data.model

sealed class MealConsumptionResult {
    object Success : MealConsumptionResult()
    object NotFound : MealConsumptionResult()
    data class InsufficientIngredients(
        val missingIngredients: List<MissingIngredient>
    ) : MealConsumptionResult()
    data class Error(val message: String) : MealConsumptionResult()
}

data class MissingIngredient(
    val pantryItemId: Long,
    val itemName: String,
    val required: Double,
    val available: Double,
    val unit: PantryUnit,
    val deficit: Double = required - available
)

sealed class AvailabilityCheck {
    object Success : AvailabilityCheck()
    data class Failure(
        val missingItems: List<MissingIngredient>
    ) : AvailabilityCheck()
}
