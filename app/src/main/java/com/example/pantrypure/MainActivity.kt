package com.example.pantrypure

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pantrypure.ui.navigation.Screen
import com.example.pantrypure.ui.screen.AddEditItemScreen
import com.example.pantrypure.ui.screen.HistoryScreen
import com.example.pantrypure.ui.screen.InventoryListScreen
import com.example.pantrypure.ui.screen.ShoppingListScreen
import com.example.pantrypure.ui.theme.PantryPureTheme
import com.example.pantrypure.ui.viewmodel.PantryViewModel
import com.example.pantrypure.ui.viewmodel.PantryViewModelFactory
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    private val viewModel: PantryViewModel by viewModels {
        PantryViewModelFactory((application as PantryPureApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PantryPureTheme {
                NotificationPermissionEffect()
                PantryPureApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionEffect() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(Unit) {
            if (!permissionState.status.isGranted) {
                permissionState.launchPermissionRequest()
            }
        }
    }
}

@Composable
fun PantryPureApp(viewModel: PantryViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.InventoryList.route) {
        composable(Screen.InventoryList.route) {
            InventoryListScreen(
                viewModel = viewModel,
                onAddItemClick = {
                    navController.navigate(Screen.AddEditItem.createRoute(-1L))
                },
                onItemClick = { itemId ->
                    navController.navigate(Screen.AddEditItem.createRoute(itemId))
                },
                onShoppingListClick = {
                    navController.navigate(Screen.ShoppingList.route)
                },
                onHistoryClick = {
                    navController.navigate(Screen.History.route)
                }
            )
        }
        composable(Screen.ShoppingList.route) {
            ShoppingListScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.AddEditItem.route,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong("itemId")
            AddEditItemScreen(
                viewModel = viewModel,
                itemId = if (itemId == -1L) null else itemId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
