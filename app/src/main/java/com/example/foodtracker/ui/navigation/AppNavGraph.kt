package com.example.foodtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun AppNavGraph(
    dashboard: @Composable (onMealClick: (Long) -> Unit) -> Unit,
    inventory: @Composable () -> Unit,
    meal: @Composable (mealId: Long) -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD
    ) {
        composable(Routes.DASHBOARD) {
            dashboard { mealId ->
                navController.navigate(Routes.meal(mealId))
            }
        }

        composable(Routes.INVENTORY) {
            inventory()
        }

        composable(
            route = "${Routes.MEAL}/{mealId}",
            arguments = listOf(navArgument("mealId") { type = NavType.LongType })
        ) { backStack ->
            val mealId = backStack.arguments!!.getLong("mealId")
            meal(mealId)
        }
    }
}