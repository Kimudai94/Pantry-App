package com.example.pantrypure.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pantrypure.data.database.PantryDatabase
import com.example.pantrypure.data.model.ConsumptionRecord
import com.example.pantrypure.data.model.PantryUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ConsumptionDaoTest {
    private lateinit var db: PantryDatabase
    private lateinit var consumptionDao: ConsumptionDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, PantryDatabase::class.java
        ).build()
        consumptionDao = db.consumptionDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetAllHistory() = runBlocking {
        val record1 = ConsumptionRecord(
            itemId = 1,
            itemName = "Milk",
            quantityConsumed = 0.5,
            unit = PantryUnit.LITERS,
            consumptionDate = 1000L
        )
        val record2 = ConsumptionRecord(
            itemId = 2,
            itemName = "Eggs",
            quantityConsumed = 2.0,
            unit = PantryUnit.PIECES,
            consumptionDate = 2000L
        )
        
        consumptionDao.insertRecord(record1)
        consumptionDao.insertRecord(record2)

        val history = consumptionDao.getAllHistory().first()
        assertEquals(2, history.size)
        assertEquals("Eggs", history[0].itemName) // Assuming Descending order by timestamp
    }

    @Test
    fun clearHistory() = runBlocking {
        val record = ConsumptionRecord(
            itemId = 1,
            itemName = "Milk",
            quantityConsumed = 0.5,
            unit = PantryUnit.LITERS
        )
        consumptionDao.insertRecord(record)
        
        consumptionDao.clearHistory()
        
        val history = consumptionDao.getAllHistory().first()
        assertTrue(history.isEmpty())
    }
}
