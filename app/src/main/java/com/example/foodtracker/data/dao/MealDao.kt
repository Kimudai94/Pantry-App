package com.example.foodtracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.foodtracker.data.db.entity.MealEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface MealDao {
    @Query("SELECT * FROM meal ORDER BY prepDate DESC")
    fun observeAllEntities(): Flow<List<MealEntity>>

    @Query("SELECT * FROM meal WHERE id = :id")
    fun observeEntityById(id: Long): Flow<MealEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meal: MealEntity): Long

    @Query(
        """
        SELECT m.id, m.name, m.prepDate, m.storage, m.totalPortions, m.shelfLifeDays, m.notes,
               COALESCE(SUM(CASE WHEN e.type = 'EATEN' THEN e.portions ELSE 0 END), 0) AS eatenPortions
        FROM meal m
        LEFT JOIN meal_event e ON e.mealId = m.id
        GROUP BY m.id
        ORDER BY m.prepDate DESC
        """
    )
    fun observeMealsAgg(): Flow<List<MealAggRow>>

    @Query(
        """
        SELECT m.id, m.name, m.prepDate, m.storage, m.totalPortions, m.shelfLifeDays, m.notes,
               COALESCE(SUM(CASE WHEN e.type = 'EATEN' THEN e.portions ELSE 0 END), 0) AS eatenPortions
        FROM meal m
        LEFT JOIN meal_event e ON e.mealId = m.id
        WHERE m.id = :id
        GROUP BY m.id
        """
    )
    fun observeMealAggById(id: Long): Flow<MealAggRow>
}
