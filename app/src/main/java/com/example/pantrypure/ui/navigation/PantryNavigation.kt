package com.example.pantrypure.ui.navigation

sealed class Screen(val route: String) {
    object InventoryList : Screen("inventory_list")
    object ShoppingList : Screen("shopping_list")
    object History : Screen("history")
    object AddEditItem : Screen("add_edit_item?itemId={itemId}") {
        fun createRoute(itemId: Long?) = "add_edit_item?itemId=$itemId"
    }
}
