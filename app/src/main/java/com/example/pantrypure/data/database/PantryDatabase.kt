package com.example.pantrypure.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pantrypure.data.dao.PantryDao
import com.example.pantrypure.data.dao.ConsumptionDao
import com.example.pantrypure.data.dao.MealDao
import com.example.pantrypure.data.dao.MealIngredientDao
import com.example.pantrypure.data.dao.MealPlanDao
import com.example.pantrypure.data.dao.OfferDao
import com.example.pantrypure.data.model.PantryItem
import com.example.pantrypure.data.model.ConsumptionRecord
import com.example.pantrypure.data.model.Meal
import com.example.pantrypure.data.model.MealIngredient
import com.example.pantrypure.data.model.MealPlan
import com.example.pantrypure.data.model.Offer

@Database(
    entities = [
        PantryItem::class,
        ConsumptionRecord::class,
        Meal::class,
        MealIngredient::class,
        MealPlan::class,
        Offer::class
    ],
    version = 10,
    exportSchema = true
)
@TypeConverters(PantryTypeConverters::class)
abstract class PantryDatabase : RoomDatabase() {
    abstract fun pantryDao(): PantryDao
    abstract fun consumptionDao(): ConsumptionDao
    abstract fun mealDao(): MealDao
    abstract fun mealIngredientDao(): MealIngredientDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun offerDao(): OfferDao

    companion object {
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `offers` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `itemName` TEXT NOT NULL, 
                        `store` TEXT NOT NULL, 
                        `price` REAL NOT NULL, 
                        `originalPrice` REAL, 
                        `offerQuantity` REAL NOT NULL, 
                        `offerUnit` TEXT NOT NULL, 
                        `validFrom` INTEGER NOT NULL, 
                        `validUntil` INTEGER NOT NULL, 
                        `isAddedToShoppingList` INTEGER NOT NULL DEFAULT 0, 
                        `category` TEXT NOT NULL DEFAULT 'Prospekt'
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `meal_plans` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `mealId` INTEGER NOT NULL, 
                        `plannedDate` INTEGER NOT NULL, 
                        `servings` INTEGER NOT NULL DEFAULT 1, 
                        `isConsumed` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`mealId`) REFERENCES `meals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_plans_mealId` ON `meal_plans` (`mealId`)")
            }
        }
    }
}
