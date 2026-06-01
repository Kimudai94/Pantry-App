package com.example.pantrypure.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pantrypure.data.database.PantryDatabase
import com.example.pantrypure.data.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class PantryRepositoryTest {

    private lateinit var db: PantryDatabase
    private lateinit var repository: PantryRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, PantryDatabase::class.java
        ).allowMainThreadQueries().build()
        
        repository = PantryRepository(
            db.pantryDao(),
            db.consumptionDao(),
            db.mealDao(),
            db.mealIngredientDao(),
            db.mealPlanDao()
        )
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun consumeMeal_deductsFromMultipleBatchesCorrectly() = runBlocking {
        // 1. Setup inventory: Two batches of Milk
        val batch1 = PantryItem(
            name = "Milk",
            quantity = 0.5,
            unit = PantryUnit.LITERS,
            expiryDate = 1000L,
            quantityThreshold = 2.0
        )
        val batch2 = PantryItem(
            name = "Milk",
            quantity = 1.0,
            unit = PantryUnit.LITERS,
            expiryDate = 2000L,
            quantityThreshold = 2.0
        )
        repository.insertItem(batch1)
        repository.insertItem(batch2)

        // 2. Setup meal: Needs 800ml Milk
        val meal = Meal(name = "Cereal", category = MealCategory.BREAKFAST)
        repository.saveMealWithIngredients(
            meal,
            listOf(
                MealIngredient(
                    mealId = 0, // Will be updated by repository
                    ingredientName = "Milk",
                    requiredQuantity = 800.0,
                    requiredUnit = PantryUnit.MILLILITERS
                )
            )
        )
        val savedMeal = repository.getAllMealsWithIngredients().first().first { it.meal.name == "Cereal" }

        // 3. Consume
        val result = repository.consumeMeal(savedMeal.meal.id)
        assertTrue(result is MealConsumptionResult.Success)

        // 4. Verify quantities
        val items = repository.getAllItems().first().filter { it.name == "Milk" }
        // Batch 1 (earlier expiry) should be fully consumed: 500ml
        // Batch 2 should provide remaining 300ml -> 1.0L - 0.3L = 0.7L
        
        // Wait, repository.updateItem deletes if quantity <= 0
        assertEquals(1, items.size)
        assertEquals(0.7, items[0].quantity, 0.001)
        
        // Check consumption history
        val history = repository.getConsumptionHistory().first()
        assertEquals(2, history.size) // One record per batch
    }

    @Test
    fun updateItem_triggersShoppingListBelowThreshold() = runBlocking {
        val item = PantryItem(
            name = "Sugar",
            quantity = 5.0,
            unit = PantryUnit.KILOGRAMS,
            quantityThreshold = 2.0
        )
        val id = db.pantryDao().insertItem(item)
        val inserted = repository.getItemById(id)!!

        // Update quantity to below threshold
        repository.updateItem(inserted.copy(quantity = 1.5))

        val updated = repository.getItemById(id)!!
        assertTrue("Should be on shopping list", updated.isOnShoppingList)
        assertEquals(0.5, updated.neededQuantity, 0.001)
    }

    @Test
    fun deleteItem_createsPlaceholderIfLastItem() = runBlocking {
        val item = PantryItem(
            name = "Eggs",
            quantity = 6.0,
            unit = PantryUnit.PIECES,
            quantityThreshold = 10.0,
            category = "Test",
            location = "Test",
            notes = "",
            expiryThresholdDays = 7
        )
        val id = db.pantryDao().insertItem(item)
        val inserted = repository.getItemById(id)!!

        repository.deleteItem(inserted)

        // Wait a bit or use a more robust way to check for the placeholder
        // Since getAllItems() filters quantity > 0, we need to check the DAO directly or change the query
        val allItems = db.pantryDao().getShoppingListItems().first()
        assertEquals(1, allItems.size)
        val placeholder = allItems[0]
        assertEquals("Eggs", placeholder.name)
        assertEquals(0.0, placeholder.quantity, 0.0)
        assertTrue(placeholder.isOnShoppingList)
        assertEquals(10.0, placeholder.neededQuantity, 0.0)
    }
}
