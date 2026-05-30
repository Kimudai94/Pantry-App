package com.example.pantrypure.data.dao

import androidx.room.*
import com.example.pantrypure.data.model.Meal
import com.example.pantrypure.data.model.MealCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meals ORDER BY category, name ASC")
    suspend fun getAllMeals(): List<Meal>

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
}
