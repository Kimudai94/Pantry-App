package com.example.pantrypure.data.dao

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
class MealPlanDaoTest {
    private lateinit var db: PantryDatabase
    private lateinit var mealDao: MealDao
    private lateinit var mealPlanDao: MealPlanDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, PantryDatabase::class.java
        ).build()
        mealDao = db.mealDao()
        mealPlanDao = db.mealPlanDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetMealPlanInRange() = runBlocking {
        // 1. Insert a meal
        val meal = Meal(name = "Spaghetti", category = MealCategory.DINNER)
        val mealId = mealDao.insertMeal(meal)

        // 2. Create meal plans
        val today = 1000000L
        val plan1 = MealPlan(mealId = mealId, plannedDate = today, servings = 2)
        val plan2 = MealPlan(mealId = mealId, plannedDate = today + 86400000L, servings = 4) // Tomorrow
        
        mealPlanDao.insertMealPlan(plan1)
        mealPlanDao.insertMealPlan(plan2)

        // 3. Query range
        val rangePlans = mealPlanDao.getMealPlansInRange(today, today + 1000L).first()
        
        assertEquals(1, rangePlans.size)
        assertEquals("Spaghetti", rangePlans[0].meal.name)
        assertEquals(2, rangePlans[0].plan.servings)
    }

    @Test
    @Throws(Exception::class)
    fun updateConsumedStatus() = runBlocking {
        val meal = Meal(name = "Salad")
        val mealId = mealDao.insertMeal(meal)
        val plan = MealPlan(mealId = mealId, plannedDate = 1000L)
        val planId = mealPlanDao.insertMealPlan(plan)

        mealPlanDao.updateConsumedStatus(planId, true)

        val plans = mealPlanDao.getUpcomingMealPlans(0L).first()
        // isConsumed = 1 should be excluded from getUpcomingMealPlans based on the query
        assertTrue(plans.isEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun deleteMealPlan() = runBlocking {
        val meal = Meal(name = "Pizza")
        val mealId = mealDao.insertMeal(meal)
        val plan = MealPlan(mealId = mealId, plannedDate = 2000L)
        val planId = mealPlanDao.insertMealPlan(plan)
        
        val planToDelete = mealPlanDao.getUpcomingMealPlans(0L).first()[0].plan
        mealPlanDao.deleteMealPlan(planToDelete)

        val plans = mealPlanDao.getUpcomingMealPlans(0L).first()
        assertTrue(plans.isEmpty())
    }
}
