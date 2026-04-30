package com.example.pantrypure.data.dao

import androidx.room.*
import com.example.pantrypure.data.model.MealIngredient

@Dao
interface MealIngredientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: MealIngredient): Long

    @Delete
    suspend fun deleteIngredient(ingredient: MealIngredient)

    @Query("DELETE FROM meal_ingredients WHERE mealId = :mealId")
    suspend fun deleteAllIngredientsForMeal(mealId: Long)

    @Query("SELECT * FROM meal_ingredients WHERE mealId = :mealId")
    suspend fun getIngredientsForMeal(mealId: Long): List<MealIngredient>
}
