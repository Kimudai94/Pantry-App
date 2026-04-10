package com.example.foodtracker.data.repository

import com.example.foodtracker.data.dao.MealAggRow
import com.example.foodtracker.data.dao.MealDao
import com.example.foodtracker.data.dao.MealEventDao
import com.example.foodtracker.data.db.entity.MealEntity
import com.example.foodtracker.data.db.entity.MealEventEntity
import com.example.foodtracker.domain.model.Meal
import com.example.foodtracker.domain.model.StorageLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class MealRepository(
    private val mealDao: MealDao,
    private val eventDao: MealEventDao
) {
    fun observeAll(): Flow<List<Meal>> =
        mealDao.observeMealsAgg().map { rows -> rows.map { it.toDomain() } }

    fun observeById(id: Long): Flow<Meal> =
        mealDao.observeMealAggById(id).map { it.toDomain() }

    suspend fun save(meal: Meal) {
        mealDao.insert(meal.toEntity())
    }

    suspend fun consumePortion(mealId: Long) {
        eventDao.insert(
            MealEventEntity(
                mealId = mealId,
                type = "EATEN",
                portions = 1,
                occurredAt = LocalDateTime.now()
            )
        )
    }

    private fun MealAggRow.toDomain(): Meal = Meal(
        id = id,
        name = name,
        prepDate = prepDate,
        storage = StorageLocation.valueOf(storage),
        totalPortions = totalPortions - eatenPortions,
        shelfLifeDays = shelfLifeDays,
        notes = notes
    )

    private fun Meal.toEntity(): MealEntity = MealEntity(
        id = id,
        name = name,
        prepDate = prepDate,
        storage = storage.name,
        totalPortions = totalPortions + 0,
        shelfLifeDays = shelfLifeDays,
        notes = notes
    )
}