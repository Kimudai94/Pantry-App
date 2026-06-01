package com.example.pantrypure.ui.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.pantrypure.data.model.*
import com.example.pantrypure.data.repository.PantryRepository
import com.example.pantrypure.ui.viewmodel.PantryViewModel
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*

class MealPlannerUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mealPlanner_displaysDaysOfWeek() {
        val mockRepo: PantryRepository = mock()
        whenever(mockRepo.getMealPlansInRange(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(mockRepo.getAllMealsWithIngredients()).thenReturn(flowOf(emptyList()))
        whenever(mockRepo.getAllItems()).thenReturn(flowOf(emptyList()))
        
        val viewModel = PantryViewModel(mockRepo)

        composeTestRule.setContent {
            MealPlannerScreen(
                viewModel = viewModel,
                onNavigateBack = {},
                onAddMealToPlan = {}
            )
        }

        // Überprüfe, ob wir 7 "Add Meal" Buttons haben (einen pro Tag)
        composeTestRule.onAllNodesWithContentDescription("Add Meal").assertCountEquals(7)
    }

    @Test
    fun mealPlanner_showsMissingIngredientWarning() {
        val mockRepo: PantryRepository = mock()
        val calendar = java.util.Calendar.getInstance()
        // Ensure we are at the start of the week to match MealPlannerScreen logic
        calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val monday = calendar.timeInMillis

        // Setup: Eine Mahlzeit ist geplant, aber Zutaten fehlen
        val plan = MealPlanWithDetails(
            plan = MealPlan(id = 1, mealId = 10, plannedDate = monday),
            meal = Meal(id = 10, name = "Test Pasta"),
            ingredients = listOf(
                MealIngredientWithName(
                    id = 1,
                    mealId = 10,
                    ingredientName = "Tomato", 
                    requiredQuantity = 1.0, 
                    requiredUnit = PantryUnit.PIECES
                )
            )
        )

        whenever(mockRepo.getMealPlansInRange(any(), any())).thenReturn(flowOf(listOf(plan)))
        whenever(mockRepo.getAllItems()).thenReturn(flowOf(emptyList())) // Leeres Inventar -> Defizit
        whenever(mockRepo.getAllMealsWithIngredients()).thenReturn(flowOf(emptyList()))
        
        val viewModel = PantryViewModel(mockRepo)
        // Explicitly set the planner date to the Monday we used
        viewModel._currentPlannerDate.value = monday

        composeTestRule.setContent {
            MealPlannerScreen(
                viewModel = viewModel,
                onNavigateBack = {},
                onAddMealToPlan = {}
            )
        }

        // Prüfe, ob die Mahlzeit angezeigt wird
        composeTestRule.onNodeWithText("Test Pasta").assertIsDisplayed()
        
        // Das Warn-Icon (ContentDescription "Missing ingredients") sollte existieren
        composeTestRule.onNodeWithContentDescription("Missing ingredients").assertIsDisplayed()
    }
}
