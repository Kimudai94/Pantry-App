package com.example.pantrypure.ui.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.pantrypure.data.model.Meal
import com.example.pantrypure.data.model.MealIngredientWithName
import com.example.pantrypure.data.model.MealPlan
import com.example.pantrypure.data.model.MealPlanWithDetails
import com.example.pantrypure.data.model.PantryUnit
import com.example.pantrypure.data.repository.PantryRepository
import com.example.pantrypure.ui.viewmodel.PantryViewModel
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.whenever
import org.mockito.kotlin.mock
import org.mockito.ArgumentMatchers.anyLong

class ShoppingListUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shoppingList_displaysProjectedNeedsSection() {
        val mockRepo: PantryRepository = mock()
        // Mocking: Ein geplanter Bedarf an Mehl
        val mealPlan = MealPlanWithDetails(
            plan = MealPlan(id = 1, mealId = 1, plannedDate = System.currentTimeMillis()),
            meal = Meal(id = 1, name = "Kuchen"),
            ingredients = listOf(
                MealIngredientWithName(
                    id = 1,
                    mealId = 1,
                    ingredientName = "Mehl",
                    requiredQuantity = 0.5,
                    requiredUnit = PantryUnit.KILOGRAMS
                )
            )
        )

        whenever(mockRepo.getShoppingListItems()).thenReturn(flowOf(emptyList()))
        whenever(mockRepo.getAllItems()).thenReturn(flowOf(emptyList()))
        whenever(mockRepo.getMealPlansInRange(anyLong(), anyLong())).thenReturn(flowOf(listOf(mealPlan)))
        whenever(mockRepo.getConsumptionHistory()).thenReturn(flowOf(emptyList()))
        whenever(mockRepo.getAllMealsWithIngredients()).thenReturn(flowOf(emptyList()))

        val viewModel = PantryViewModel(mockRepo)

        composeTestRule.setContent {
            ShoppingListScreen(
                viewModel = viewModel,
                onItemClick = {},
                onNavigateBack = {}
            )
        }

        // Überprüfe die Sektions-Überschrift
        composeTestRule.onNodeWithText("Geplanter Bedarf", substring = true).assertIsDisplayed()
        
        // Überprüfe, ob das projizierte Item angezeigt wird
        composeTestRule.onNodeWithText("Mehl").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fehlend:", substring = true).assertExists()
    }
}
