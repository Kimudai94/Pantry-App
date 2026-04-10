package com.example.foodtracker.ui.navigation

object Routes {
    const val DASHBOARD = "dashboard"
    const val INVENTORY = "inventory"
    const val MEAL = "meal"

    fun meal(mealId: Long) = "$MEAL/$mealId"
}