package com.example.pantrypure.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PantryDatabase::class.java
    )

    @Test
    @Throws(IOException::class)
    fun migrate8To9() {
        // 1. Erstelle Datenbank in Version 8
        var db = helper.createDatabase(TEST_DB, 8)
        db.execSQL("""
            INSERT INTO pantry_items (
                name, quantity, quantityThreshold, unit, expiryThresholdDays, 
                isOnShoppingList, category, location, notes, timesBought, neededQuantity
            ) VALUES (
                'Migration Test', 1.0, 2.0, 'PIECES', 7, 
                0, 'Test', 'Test', '', 0, 0.0
            )
        """.trimIndent())
        db.close()

        // 2. Migriere auf Version 9 und validiere das Schema
        db = helper.runMigrationsAndValidate(TEST_DB, 9, true, PantryDatabase.MIGRATION_8_9)

        // 3. Prüfe, ob alte Daten noch da sind
        val cursor = db.query("SELECT * FROM pantry_items WHERE name = 'Migration Test'")
        assert(cursor.count == 1)
        cursor.close()

        // 4. Prüfe, ob die neue Tabelle 'meal_plans' existiert
        val planCursor = db.query("SELECT * FROM meal_plans")
        assert(planCursor.columnCount > 0)
        planCursor.close()
    }
}
