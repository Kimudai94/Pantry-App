package com.example.pantrypure.data.dao

import androidx.room.*
import com.example.pantrypure.data.model.MealPlan
import com.example.pantrypure.data.model.MealPlanWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlan(mealPlan: MealPlan): Long

    @Update
    suspend fun updateMealPlan(mealPlan: MealPlan)

    @Delete
    suspend fun deleteMealPlan(mealPlan: MealPlan)

    @Transaction
    @Query("SELECT * FROM meal_plans WHERE plannedDate BETWEEN :startDate AND :endDate ORDER BY plannedDate ASC")
    fun getMealPlansInRange(startDate: Long, endDate: Long): Flow<List<MealPlanWithDetails>>

    @Transaction
    @Query("SELECT * FROM meal_plans WHERE isConsumed = 0 AND plannedDate >= :currentTime ORDER BY plannedDate ASC")
    fun getUpcomingMealPlans(currentTime: Long): Flow<List<MealPlanWithDetails>>

    @Query("UPDATE meal_plans SET isConsumed = :consumed WHERE id = :planId")
    suspend fun updateConsumedStatus(planId: Long, consumed: Boolean)
}
