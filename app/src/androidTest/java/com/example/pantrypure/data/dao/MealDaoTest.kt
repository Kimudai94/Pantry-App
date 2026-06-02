package com.example.pantrypure.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pantrypure.data.database.PantryDatabase
import com.example.pantrypure.data.model.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MealDaoTest {
    private lateinit var db: PantryDatabase
    private lateinit var mealDao: MealDao
    private lateinit var ingredientDao: MealIngredientDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, PantryDatabase::class.java
        ).build()
        mealDao = db.mealDao()
        ingredientDao = db.mealIngredientDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertMealAndIngredients_retrievesCorrectly() = runBlocking {
        val meal = Meal(name = "Pasta", category = MealCategory.DINNER)
        val mealId = mealDao.insertMeal(meal)
        
        val ingredient = MealIngredient(
            mealId = mealId,
            ingredientName = "Noodles",
            requiredQuantity = 500.0,
            requiredUnit = PantryUnit.GRAMS
        )
        ingredientDao.insertIngredient(ingredient)

        val retrievedIngredients = ingredientDao.getMealIngredientsWithNames(mealId)
        assertEquals(1, retrievedIngredients.size)
        assertEquals("Noodles", retrievedIngredients[0].ingredientName)
        assertEquals(500.0, retrievedIngredients[0].requiredQuantity, 0.0)
    }
}
