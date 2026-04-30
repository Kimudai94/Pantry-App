package com.example.pantrypure.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation

@Entity(
    tableName = "meal_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = Meal::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PantryItem::class,
            parentColumns = ["id"],
            childColumns = ["pantryItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("mealId"),
        Index("pantryItemId")
    ]
)
data class MealIngredient(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mealId: Long,
    val pantryItemId: Long,
    val requiredQuantity: Double,
    val requiredUnit: PantryUnit
)

/**
 * Data Transfer Object for a meal with all its ingredients
 */
data class MealWithIngredients(
    @Embedded
    val meal: Meal,
    @Relation(
        parentColumn = "id",
        entityColumn = "mealId",
        entity = MealIngredient::class
    )
    val ingredients: List<MealIngredientWithName>
)

/**
 * Detail view of a meal ingredient with pantry item information
 * Room will populate this from a custom query
 */
data class MealIngredientWithName(
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "mealId")
    val mealId: Long = 0,
    @ColumnInfo(name = "pantryItemId")
    val pantryItemId: Long = 0,
    @ColumnInfo(name = "requiredQuantity")
    val requiredQuantity: Double = 0.0,
    @ColumnInfo(name = "requiredUnit")
    val requiredUnit: PantryUnit = PantryUnit.PIECES,
    @ColumnInfo(name = "pantryItemName")
    val pantryItemName: String = ""
)

/**
 * Alias for backward compatibility
 */
typealias MealIngredientDetail = MealIngredientWithName
