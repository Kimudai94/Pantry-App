package com.example.pantrypure.data.repository

import com.example.pantrypure.data.dao.PantryDao
import com.example.pantrypure.data.dao.ConsumptionDao
import com.example.pantrypure.data.dao.MealDao
import com.example.pantrypure.data.dao.MealIngredientDao
import com.example.pantrypure.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PantryRepository(
    private val pantryDao: PantryDao,
    private val consumptionDao: ConsumptionDao,
    private val mealDao: MealDao,
    private val mealIngredientDao: MealIngredientDao
) {
    // Pantry Item methods
    fun getAllItems(): Flow<List<PantryItem>> = pantryDao.getAllItems()

    suspend fun getItemById(id: Long): PantryItem? = pantryDao.getItemById(id)

    suspend fun insertItem(item: PantryItem) = pantryDao.insertItem(item)

    suspend fun updateItem(item: PantryItem) = pantryDao.updateItem(item)

    suspend fun deleteItem(item: PantryItem) = pantryDao.deleteItem(item)

    fun getItemsByCategory(category: String): Flow<List<PantryItem>> = pantryDao.getItemsByCategory(category)

    fun getShoppingListItems(): Flow<List<PantryItem>> = pantryDao.getShoppingListItems()

    suspend fun updateShoppingListStatus(id: Long, isOnList: Boolean) = pantryDao.updateShoppingListStatus(id, isOnList)

    // Consumption history methods
    fun getConsumptionHistory(): Flow<List<ConsumptionRecord>> = consumptionDao.getAllHistory()

    suspend fun insertConsumptionRecord(record: ConsumptionRecord) = consumptionDao.insertRecord(record)

    suspend fun clearHistory() = consumptionDao.clearHistory()

    // Meal methods
    fun getAllMeals(): Flow<List<Meal>> = mealDao.getAllMeals()

    fun getMealsByCategory(category: MealCategory): Flow<List<Meal>> = mealDao.getMealsByCategory(category)

    suspend fun getMealById(id: Long): Meal? = mealDao.getMealById(id)

    suspend fun insertMeal(meal: Meal): Long = mealDao.insertMeal(meal)

    suspend fun updateMeal(meal: Meal) = mealDao.updateMeal(meal)

    suspend fun deleteMeal(meal: Meal) = mealDao.deleteMeal(meal)

    suspend fun getMealWithIngredients(id: Long): MealWithIngredients? {
        val meal = mealDao.getMealByIdForDetail(id) ?: return null
        val ingredients = mealDao.getMealIngredientsWithNames(id)
        return MealWithIngredients(meal, ingredients)
    }

    fun getAllMealsWithIngredients(): Flow<List<MealWithIngredients>> {
        return kotlinx.coroutines.flow.flow {
            val meals = mealDao.getAllMealsRaw()
            val allIngredients = mealDao.getAllMealIngredientsWithNames()

            val mealsWithIngredients = meals.map { meal ->
                val ingredientsForMeal = allIngredients.filter { it.mealId == meal.id }
                MealWithIngredients(meal, ingredientsForMeal)
            }
            emit(mealsWithIngredients)
        }
    }

    // Meal ingredient methods
    suspend fun addIngredientToMeal(ingredient: MealIngredient): Long = mealIngredientDao.insertIngredient(ingredient)

    suspend fun removeIngredientFromMeal(ingredient: MealIngredient) = mealIngredientDao.deleteIngredient(ingredient)

    suspend fun getIngredientsForMeal(mealId: Long): List<MealIngredient> = mealIngredientDao.getIngredientsForMeal(mealId)

    // Meal consumption with error handling
    suspend fun consumeMeal(mealId: Long): MealConsumptionResult = withContext(Dispatchers.Default) {
        try {
            val mealWithIngredients = getMealWithIngredients(mealId)
                ?: return@withContext MealConsumptionResult.NotFound

            // Phase 1: Check ingredient availability
            val availabilityCheck = checkIngredientsAvailable(mealWithIngredients.ingredients)
            if (availabilityCheck !is AvailabilityCheck.Success) {
                val missingItems = (availabilityCheck as? AvailabilityCheck.Failure)?.missingItems ?: emptyList()
                return@withContext MealConsumptionResult.InsufficientIngredients(missingItems)
            }

            // Phase 2: Atomic transaction - consume all ingredients
            mealWithIngredients.ingredients.forEach { ingredient ->
                val pantryItem = getItemById(ingredient.pantryItemId)
                    ?: return@forEach

                // Update quantity
                val newQuantity = (pantryItem.quantity - ingredient.requiredQuantity).coerceAtLeast(0.0)
                updateItem(pantryItem.copy(quantity = newQuantity))

                // Record consumption
                insertConsumptionRecord(
                    ConsumptionRecord(
                        itemId = pantryItem.id,
                        itemName = pantryItem.name,
                        quantityConsumed = ingredient.requiredQuantity,
                        unit = ingredient.requiredUnit
                    )
                )
            }

            MealConsumptionResult.Success
        } catch (e: Exception) {
            MealConsumptionResult.Error(e.message ?: "Unknown error during meal consumption")
        }
    }

    private suspend fun checkIngredientsAvailable(ingredients: List<MealIngredientDetail>): AvailabilityCheck {
        val missingItems = mutableListOf<MissingIngredient>()

        for (ingredient in ingredients) {
            val pantryItem = getItemById(ingredient.pantryItemId)
            if (pantryItem == null || pantryItem.quantity < ingredient.requiredQuantity) {
                val available = pantryItem?.quantity ?: 0.0
                missingItems.add(
                    MissingIngredient(
                        pantryItemId = ingredient.pantryItemId,
                        itemName = ingredient.pantryItemName,
                        required = ingredient.requiredQuantity,
                        available = available,
                        unit = ingredient.requiredUnit,
                        deficit = ingredient.requiredQuantity - available
                    )
                )
            }
        }

        return if (missingItems.isEmpty()) {
            AvailabilityCheck.Success
        } else {
            AvailabilityCheck.Failure(missingItems)
        }
    }
}
