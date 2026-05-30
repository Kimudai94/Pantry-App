package com.example.pantrypure.data.dao

import androidx.room.*
import com.example.pantrypure.data.model.MealIngredient
import com.example.pantrypure.data.model.MealIngredientWithName

@Dao
interface MealIngredientDao {

    @Query("""
        SELECT 
            id,
            mealId,
            pantryItemId,
            requiredQuantity,
            requiredUnit,
            ingredientName
        FROM meal_ingredients
        WHERE mealId = :mealId
    """)
    suspend fun getMealIngredientsWithNames(mealId: Long): List<MealIngredientWithName>

    @Query("""
        SELECT
            id,
            mealId,
            pantryItemId,
            requiredQuantity,
            requiredUnit,
            ingredientName
        FROM meal_ingredients
        ORDER BY mealId
    """)
    suspend fun getAllMealIngredientsWithNames(): List<MealIngredientWithName>

    @Query("DELETE FROM meal_ingredients WHERE mealId = :mealId")
    suspend fun deleteAllIngredientsForMeal(mealId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: MealIngredient): Long
}
