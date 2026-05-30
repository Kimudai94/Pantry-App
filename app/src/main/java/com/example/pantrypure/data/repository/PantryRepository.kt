package com.example.pantrypure.data.repository

import com.example.pantrypure.data.dao.PantryDao
import com.example.pantrypure.data.dao.ConsumptionDao
import com.example.pantrypure.data.dao.MealDao
import com.example.pantrypure.data.dao.MealIngredientDao
import com.example.pantrypure.data.model.AvailabilityCheck
import com.example.pantrypure.data.model.ConsumptionRecord
import com.example.pantrypure.data.model.Meal
import com.example.pantrypure.data.model.MealConsumptionResult
import com.example.pantrypure.data.model.MealIngredient
import com.example.pantrypure.data.model.MealIngredientDetail
import com.example.pantrypure.data.model.MealWithIngredients
import com.example.pantrypure.data.model.MissingIngredient
import com.example.pantrypure.data.model.PantryItem
import com.example.pantrypure.data.util.UnitConverter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
    suspend fun updateItem(item: PantryItem) {
        // LÖSCH-LOGIK: Wenn Menge <= 0, entferne das Item komplett
        // Optional: Prüfen, ob das Löschen den Schwellenwert triggert
        // Calculate total quantity across all items with the same name to check threshold
        if (item.quantity <= 0.0) {
            pantryDao.deleteItem(item)
            checkAndTriggerShoppingList(item)
            return
        }
        val oldItem = pantryDao.getItemById(item.id)
        val isPurchased = oldItem != null && item.quantity > oldItem.quantity
        val totalQuantity = (pantryDao.getTotalQuantityByName(item.name) ?: 0.0) - (oldItem?.quantity ?: 0.0) + item.quantity
        val isUnderThreshold = totalQuantity < item.quantityThreshold
        val updatedItem = item.copy(
            quantity = item.quantity,
            isOnShoppingList = if (isUnderThreshold) true else (if (isPurchased) false else item.isOnShoppingList),
            timesBought = if (isPurchased) (item.timesBought + 1) else item.timesBought,
            neededQuantity = if (isUnderThreshold) (item.quantityThreshold - totalQuantity).coerceAtLeast(0.0) else 0.0
        )

        pantryDao.updateItem(updatedItem)

        // If it's no longer under threshold and was a purchase, 
        // make sure all other items with the same name are also removed from the shopping list
        if (!isUnderThreshold && isPurchased) {
            pantryDao.updateShoppingListStatusByName(item.name, false)
        }
    }
    suspend fun deleteItem(item: PantryItem) = pantryDao.deleteItem(item)
    fun getShoppingListItems(): Flow<List<PantryItem>> = pantryDao.getShoppingListItems()
    suspend fun updateShoppingListStatusByName(name: String, isOnList: Boolean) =
        pantryDao.updateShoppingListStatusByName(name, isOnList)

    // Hilfsfunktion für den Fall, dass ein Item gelöscht wurde
    private suspend fun checkAndTriggerShoppingList(itemName: PantryItem) {
        val remainingItems = pantryDao.getItemsByNameSortedByExpiry(itemName.name)

        if (remainingItems.isEmpty()) {
            // Letzte Charge gelöscht ⇾ "Platzhalter"-Item anlegen für Einkaufsliste.
            val placeholder = PantryItem(
              name = itemName.name,
              quantity = 0.0,
              unit = itemName.unit,
              category = itemName.category,
              isOnShoppingList = true,
              quantityThreshold = itemName.quantityThreshold,
              neededQuantity = itemName.quantityThreshold,
              expiryThresholdDays = itemName.expiryThresholdDays,
              location = itemName.location,
              notes = itemName.notes
            )
            pantryDao.insertItem(placeholder)
        } else {
            // Es gibt andere Chargen. Prüfen, ob Gesamtmenge den Schwellenwert triggert.
            // Wir nehmen den Schwellenwert der ersten gefundenen Charge als Referenz
            val totalQuantity = pantryDao.getTotalQuantityByName(itemName.name) ?: 0.0
            val firstBatch = remainingItems.first()
            val threshold = firstBatch.quantityThreshold

            // Reset neededQuantity for all batches first to ensure consistency
            pantryDao.resetNeededQuantityByName(itemName.name)

            if (totalQuantity < threshold) {
                val deficit = (threshold - totalQuantity).coerceAtLeast(0.0)
                // Update all batches' shopping list status, but only the first gets the neededQuantity
                pantryDao.updateShoppingListStatusByName(itemName.name, true)
                pantryDao.updateItem(firstBatch.copy(neededQuantity = deficit, isOnShoppingList = true))
            } else {
                pantryDao.updateShoppingListStatusByName(itemName.name, false)
            }
        }
    }

    // Consumption history methods
    fun getConsumptionHistory(): Flow<List<ConsumptionRecord>> = consumptionDao.getAllHistory()
    suspend fun insertConsumptionRecord(record: ConsumptionRecord) = consumptionDao.insertRecord(record)
    suspend fun clearHistory() = consumptionDao.clearHistory()

    // Meal methods
    suspend fun deleteMeal(meal: Meal) = withContext(Dispatchers.IO) {
        mealDao.deleteMeal(meal)
    }
    suspend fun saveMealWithIngredients(meal: Meal, ingredients: List<MealIngredient>) = withContext(Dispatchers.IO) {
        val mealId = if (meal.id == 0L) {
            mealDao.insertMeal(meal)
        } else {
            mealDao.updateMeal(meal)
            mealIngredientDao.deleteAllIngredientsForMeal(meal.id)
            meal.id
        }
        ingredients.forEach { ingredient ->
            mealIngredientDao.insertIngredient(ingredient.copy(mealId = mealId))
        }
    }
    suspend fun getMealWithIngredients(id: Long): MealWithIngredients? = withContext(Dispatchers.IO) {
        val meal = mealDao.getMealById(id) ?: return@withContext null
        val ingredients = mealIngredientDao.getMealIngredientsWithNames(id)
        MealWithIngredients(meal, ingredients)
    }
    fun getAllMealsWithIngredients(): Flow<List<MealWithIngredients>> {
        return flow {
            val meals = mealDao.getAllMeals()
            val allIngredients = mealIngredientDao.getAllMealIngredientsWithNames()

            val mealsWithIngredients = meals.map { meal ->
                val ingredientsForMeal = allIngredients.filter { it.mealId == meal.id }
                MealWithIngredients(meal, ingredientsForMeal)
            }
            emit(mealsWithIngredients)
        }.flowOn(Dispatchers.IO)
    }

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
                val itemsToConsume = pantryDao.getItemsByNameSortedByExpiry(ingredient.pantryItemName)
                var remainingToConsume = ingredient.requiredQuantity

                for (pantryItem in itemsToConsume) {
                    if (remainingToConsume <= 0) break

                    try {
                        val availableInRequiredUnit = UnitConverter.convert(
                            pantryItem.quantity,
                            pantryItem.unit,
                            ingredient.requiredUnit
                        )

                        val consumeAmountInRequiredUnit = minOf(availableInRequiredUnit, remainingToConsume)
                        val consumeAmountInOriginalUnit = UnitConverter.convert(
                            consumeAmountInRequiredUnit,
                            ingredient.requiredUnit,
                            pantryItem.unit
                        )

                        val newQuantity = (pantryItem.quantity - consumeAmountInOriginalUnit).coerceAtLeast(0.0)
                        updateItem(pantryItem.copy(quantity = newQuantity))

                        // Record consumption
                        insertConsumptionRecord(
                            ConsumptionRecord(
                                itemId = pantryItem.id,
                                itemName = pantryItem.name,
                                quantityConsumed = consumeAmountInRequiredUnit,
                                unit = ingredient.requiredUnit
                            )
                        )

                        remainingToConsume -= consumeAmountInRequiredUnit
                    } catch (e: IllegalArgumentException) {
                        // Skip items with incompatible units
                        continue
                    }
                }
            }

            MealConsumptionResult.Success
        } catch (e: Exception) {
            MealConsumptionResult.Error(e.message ?: "Unknown error during meal consumption")
        }
    }
    private suspend fun checkIngredientsAvailable(ingredients: List<MealIngredientDetail>): AvailabilityCheck {
        val missingItems = mutableListOf<MissingIngredient>()

        for (ingredient in ingredients) {
            val identicalItems = pantryDao.getItemsByNameSortedByExpiry(ingredient.pantryItemName)
            
            var totalAvailable = 0.0
            for (item in identicalItems) {
                try {
                    totalAvailable += UnitConverter.convert(
                        item.quantity,
                        item.unit,
                        ingredient.requiredUnit
                    )
                } catch (e: IllegalArgumentException) {
                    // Skip items with incompatible units (e.g. Piece vs Liter for the same name)
                }
            }

            if (totalAvailable < ingredient.requiredQuantity) {
                missingItems.add(
                    MissingIngredient(
                        pantryItemId = ingredient.pantryItemId,
                        itemName = ingredient.pantryItemName,
                        required = ingredient.requiredQuantity,
                        available = totalAvailable,
                        unit = ingredient.requiredUnit,
                        deficit = ingredient.requiredQuantity - totalAvailable
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
