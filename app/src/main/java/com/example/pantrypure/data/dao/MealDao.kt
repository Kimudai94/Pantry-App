package com.example.pantrypure.data.dao

import androidx.room.*
import com.example.pantrypure.data.model.Meal
import com.example.pantrypure.data.model.MealCategory
import com.example.pantrypure.data.model.MealIngredient
import com.example.pantrypure.data.model.MealIngredientWithName
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meals ORDER BY category, name ASC")
    fun getAllMeals(): Flow<List<Meal>>

    @Query("SELECT * FROM meals WHERE category = :category ORDER BY name ASC")
    fun getMealsByCategory(category: MealCategory): Flow<List<Meal>>

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun getMealById(id: Long): Meal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: Meal): Long

    @Update
    suspend fun updateMeal(meal: Meal)

    @Delete
    suspend fun deleteMeal(meal: Meal)

    @Query("SELECT * FROM meal_ingredients WHERE mealId = :mealId")
    suspend fun getMealIngredientsRaw(mealId: Long): List<MealIngredient>

    @Query("""
        SELECT
            mi.id,
            mi.mealId,
            mi.pantryItemId,
            mi.requiredQuantity,
            mi.requiredUnit,
            pi.name as pantryItemName
        FROM meal_ingredients mi
        JOIN pantry_items pi ON mi.pantryItemId = pi.id
        WHERE mi.mealId = :mealId
    """)
    suspend fun getMealIngredientsWithNames(mealId: Long): List<MealIngredientWithName>

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun getMealByIdForDetail(id: Long): Meal?

    @Query("SELECT * FROM meals ORDER BY category, name ASC")
    suspend fun getAllMealsRaw(): List<Meal>

    @Query("""
        SELECT
            mi.id,
            mi.mealId,
            mi.pantryItemId,
            mi.requiredQuantity,
            mi.requiredUnit,
            pi.name as pantryItemName
        FROM meal_ingredients mi
        JOIN pantry_items pi ON mi.pantryItemId = pi.id
        ORDER BY mi.mealId
    """)
    suspend fun getAllMealIngredientsWithNames(): List<MealIngredientWithName>
}
