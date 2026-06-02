package com.example.pantrypure.ui.navigation

sealed class Screen(val route: String) {
    object InventoryList : Screen("inventory_list")
    object ShoppingList : Screen("shopping_list")
    object History : Screen("history")
    object MealsList : Screen("meals_list?pickDate={pickDate}") {
        fun createRoute(pickDate: Long? = null) = if (pickDate != null) "meals_list?pickDate=$pickDate" else "meals_list?pickDate=-1"
    }
    object MealPlanner : Screen("meal_planner")
    object AddEditItem : Screen("add_edit_item?itemId={itemId}") {
        fun createRoute(itemId: Long?) = "add_edit_item?itemId=$itemId"
    }
    object AddEditMeal : Screen("add_edit_meal?mealId={mealId}") {
        fun createRoute(mealId: Long?) = "add_edit_meal?mealId=$mealId"
    }
    object MealDetail : Screen("meal_detail?mealId={mealId}") {
        fun createRoute(mealId: Long) = "meal_detail?mealId=$mealId"
    }
    object Scanner : Screen("scanner")
}
