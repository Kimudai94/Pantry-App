package com.example.pantrypure.ui.viewmodel

import com.example.pantrypure.data.model.*
import com.example.pantrypure.data.repository.PantryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.util.Calendar

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PantryViewModelTest {

    private val repository: PantryRepository = mock()
    private lateinit var viewModel: PantryViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock default flows
        whenever(repository.getAllItems()).thenReturn(flowOf(emptyList()))
        whenever(repository.getConsumptionHistory()).thenReturn(flowOf(emptyList()))
        whenever(repository.getShoppingListItems()).thenReturn(flowOf(emptyList()))
        whenever(repository.getAllMealsWithIngredients()).thenReturn(flowOf(emptyList()))
        whenever(repository.getMealPlansInRange(any(), any())).thenReturn(flowOf(emptyList()))
        
        viewModel = PantryViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `pantryItems filtering by search query works`() = runTest {
        val items = listOf(
            PantryItem(id = 1, name = "Apple", quantity = 5.0, unit = PantryUnit.PIECES, category = "Fruit"),
            PantryItem(id = 2, name = "Banana", quantity = 3.0, unit = PantryUnit.PIECES, category = "Fruit")
        )
        whenever(repository.getAllItems()).thenReturn(flowOf(items))
        
        viewModel = PantryViewModel(repository)
        // Start collecting to activate StateFlow
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.pantryItems.collect()
        }
        
        // Wait for flow to emit initial state
        runCurrent()

        viewModel.setSearchQuery("App")
        runCurrent()
        
        assertEquals(1, viewModel.pantryItems.value.size)
        assertEquals("Apple", viewModel.pantryItems.value[0].name)
        collectJob.cancel()
    }

    @Test
    fun `getStartOfWeek returns Monday at midnight`() {
        val vm = PantryViewModel(repository)
        val plannerDate = vm.currentPlannerDate.value
        
        val resultCal = Calendar.getInstance()
        resultCal.timeInMillis = plannerDate
        
        assertEquals(Calendar.MONDAY, resultCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(0, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(Calendar.MINUTE))
        assertEquals(0, resultCal.get(Calendar.SECOND))
        assertEquals(0, resultCal.get(Calendar.MILLISECOND))
    }

    @Test
    fun `plannedShoppingNeeds aggregates missing ingredients correctly`() = runTest {
        val monday = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        whenever(repository.getMealPlansInRange(any(), any())).thenReturn(flowOf(listOf(
            MealPlanWithDetails(
                plan = MealPlan(id = 1, mealId = 10, plannedDate = monday, servings = 2),
                meal = Meal(id = 10, name = "Pasta"),
                ingredients = listOf(
                    MealIngredientWithName(id = 1, mealId = 10, ingredientName = "Tomato", requiredQuantity = 100.0, requiredUnit = PantryUnit.GRAMS)
                )
            ),
            MealPlanWithDetails(
                plan = MealPlan(id = 2, mealId = 10, plannedDate = monday + 86400000L, servings = 1),
                meal = Meal(id = 10, name = "Pasta"),
                ingredients = listOf(
                    MealIngredientWithName(id = 2, mealId = 10, ingredientName = "Tomato", requiredQuantity = 100.0, requiredUnit = PantryUnit.GRAMS)
                )
            )
        )))
        
        whenever(repository.getAllItems()).thenReturn(flowOf(emptyList()))
        
        viewModel = PantryViewModel(repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.plannedShoppingNeeds.collect()
        }
        runCurrent()

        val needs = viewModel.plannedShoppingNeeds.value
        assertEquals(1, needs.size)
        assertEquals("Tomato", needs[0].itemName)
        assertEquals(300.0, needs[0].deficit, 0.001)
        collectJob.cancel()
    }

    @Test
    fun `simulation correctly handles existing virtual inventory`() = runTest {
        val monday = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
        }.timeInMillis

        val inventory = listOf(
            PantryItem(id = 1, name = "Tomato", quantity = 500.0, unit = PantryUnit.GRAMS)
        )
        whenever(repository.getAllItems()).thenReturn(flowOf(inventory))

        whenever(repository.getMealPlansInRange(any(), any())).thenReturn(flowOf(listOf(
            MealPlanWithDetails(
                plan = MealPlan(id = 1, mealId = 10, plannedDate = monday, servings = 3),
                meal = Meal(id = 10, name = "Soup"),
                ingredients = listOf(
                    MealIngredientWithName(id = 1, mealId = 10, ingredientName = "Tomato", requiredQuantity = 200.0, requiredUnit = PantryUnit.GRAMS)
                )
            )
        )))

        viewModel = PantryViewModel(repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.plannedShoppingNeeds.collect()
        }
        runCurrent()

        val needs = viewModel.plannedShoppingNeeds.value
        assertEquals(1, needs.size)
        assertEquals(100.0, needs[0].deficit, 0.001)
        collectJob.cancel()
    }

    @Test
    fun `simulation ignores consumed meal plans`() = runTest {
        val monday = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
        }.timeInMillis

        whenever(repository.getMealPlansInRange(any(), any())).thenReturn(flowOf(listOf(
            MealPlanWithDetails(
                plan = MealPlan(id = 1, mealId = 10, plannedDate = monday, servings = 1, isConsumed = true),
                meal = Meal(id = 10, name = "Soup"),
                ingredients = listOf(
                    MealIngredientWithName(id = 1, mealId = 10, ingredientName = "Tomato", requiredQuantity = 500.0, requiredUnit = PantryUnit.GRAMS)
                )
            )
        )))

        whenever(repository.getAllItems()).thenReturn(flowOf(emptyList()))

        viewModel = PantryViewModel(repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.plannedShoppingNeeds.collect()
        }
        runCurrent()

        assertTrue("Consumed meals should not generate shopping needs", viewModel.plannedShoppingNeeds.value.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun `simulation scales with servings`() = runTest {
        val monday = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
        }.timeInMillis

        whenever(repository.getMealPlansInRange(any(), any())).thenReturn(flowOf(listOf(
            MealPlanWithDetails(
                plan = MealPlan(id = 1, mealId = 10, plannedDate = monday, servings = 5),
                meal = Meal(id = 10, name = "Soup"),
                ingredients = listOf(
                    MealIngredientWithName(id = 1, mealId = 10, ingredientName = "Tomato", requiredQuantity = 200.0, requiredUnit = PantryUnit.GRAMS)
                )
            )
        )))

        whenever(repository.getAllItems()).thenReturn(flowOf(emptyList()))

        viewModel = PantryViewModel(repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.plannedShoppingNeeds.collect()
        }
        runCurrent()

        val needs = viewModel.plannedShoppingNeeds.value
        assertEquals(1, needs.size)
        assertEquals(1000.0, needs[0].deficit, 0.001)
        collectJob.cancel()
    }
}
