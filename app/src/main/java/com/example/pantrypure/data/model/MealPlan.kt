package com.example.pantrypure.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation

@Entity(
    tableName = "meal_plans",
    foreignKeys = [
        ForeignKey(
            entity = Meal::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mealId")]
)
data class MealPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mealId: Long,
    val plannedDate: Long, // Midnight timestamp
    val servings: Int = 1,
    val isConsumed: Boolean = false
)

/**
 * Detailed view of a meal plan with its meal and ingredients
 */
data class MealPlanWithDetails(
    @Embedded
    val plan: MealPlan,
    @Relation(
        parentColumn = "mealId",
        entityColumn = "id"
    )
    val meal: Meal,
    @Relation(
        parentColumn = "mealId",
        entityColumn = "mealId",
        entity = MealIngredient::class
    )
    val ingredients: List<MealIngredientWithName>
)
