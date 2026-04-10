package com.example.foodtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.room.Room
import com.example.foodtracker.data.db.AppDatabase
import com.example.foodtracker.data.repository.InventoryRepository
import com.example.foodtracker.data.repository.MealRepository
import com.example.foodtracker.domain.usecase.ComputeMealStatus
import com.example.foodtracker.ui.dashboard.DashboardScreen
import com.example.foodtracker.ui.dashboard.DashboardViewModel
import com.example.foodtracker.ui.inventory.InventoryScreen
import com.example.foodtracker.ui.inventory.InventoryViewModel
import com.example.foodtracker.ui.meal.MealScreen
import com.example.foodtracker.ui.meal.MealViewModel
import com.example.foodtracker.ui.navigation.AppNavGraph

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val db = Room.databaseBuilder(
      applicationContext,
      AppDatabase::class.java, "food_tracker_database"
    ).build()
    val mealRepo = MealRepository(db.mealDao(), db.mealEventDao())
    val inventoryRepo = InventoryRepository(db.inventoryDao())
    setContent {
      val computeStatus = remember { ComputeMealStatus() }
      val dashboardViewModel = remember { DashboardViewModel(mealRepo.observeAll(), computeStatus) }
      val mealViewModelFactory: (Long) -> MealViewModel = { mealId ->
        MealViewModel(
          mealFlow = mealRepo.observeById(mealId),
          onEatOnePortion = { mealRepo.consumePortion(mealId) },
          computeMealStatus = computeStatus
        )
      }
      val inventoryViewModel = remember {
        InventoryViewModel(
          inventoryFlow = inventoryRepo.observeInventory(),
          changeQuantity = { id, delta -> inventoryRepo.changeQuantity(id, delta) }
        )
      }
      AppNavGraph(
        dashboard = { onMealClick ->
          DashboardScreen(dashboardViewModel, onMealClick)
        },
        inventory = {
          InventoryScreen(inventoryViewModel)
        },
        meal = { mealId ->
          MealScreen(mealViewModelFactory(mealId))
        }
      )
    }
  }
}