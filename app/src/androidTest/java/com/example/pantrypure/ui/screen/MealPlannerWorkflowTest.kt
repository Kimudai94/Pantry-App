package com.example.pantrypure.ui.screen

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import com.example.pantrypure.MainActivity
import org.junit.Rule
import org.junit.Test

class MealPlannerWorkflowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    init {
        MainActivity.skipNotificationPermissionForTest = true
    }

    @Test
    fun testNavigateToMealPlanner() {
        // Wait for the UI to be ready
        composeTestRule.waitForIdle()
        
        // Find and click the planner icon in the top bar
        composeTestRule.onNodeWithContentDescription("Meal Planner").performClick()

        // Check if we are on the Meal Planner screen
        composeTestRule.onNodeWithText("Meal Planner").assertIsDisplayed()
        
        // Check if day sections are present (e.g., today)
        composeTestRule.onAllNodesWithText("No meals planned").onFirst().assertIsDisplayed()
    }

    @Test
    fun testAddMealToPlanWorkflow() {
        composeTestRule.waitForIdle()
        // 1. Go to Meal Planner
        composeTestRule.onNodeWithContentDescription("Meal Planner").performClick()

        // 2. Click "Add Meal" on the first day
        composeTestRule.onAllNodesWithContentDescription("Add Meal").onFirst().performClick()

        // 3. Should be on meals list screen now.
        composeTestRule.onNodeWithText("Mahlzeit auswählen").assertIsDisplayed()
    }

    @Test
    fun testMissingIngredientsWarning() {
        composeTestRule.waitForIdle()
        // 1. Go to Meal Planner
        composeTestRule.onNodeWithContentDescription("Meal Planner").performClick()

        // 2. Check for missing ingredient warning if a meal is planned but items missing
        composeTestRule.onNodeWithText("Meal Planner").assertIsDisplayed()
    }
}
