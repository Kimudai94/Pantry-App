package com.example.foodtracker.data.dao

import androidx.room.*
import com.example.foodtracker.data.db.entity.MealEventEntity

@Dao
interface MealEventDao {
    @Query("SELECT SUM(portions) FROM meal_event WHERE mealId = :mealId AND type = 'EATEN'")
    suspend fun consumedPortions(mealId: Long): Int?

    @Insert
    suspend fun insert(event: MealEventEntity)
}
