package com.example.pantrypure.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pantrypure.data.database.PantryDatabase
import com.example.pantrypure.data.model.PantryItem
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
class PantryDaoTest {
    private lateinit var pantryDao: PantryDao
    private lateinit var db: PantryDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, PantryDatabase::class.java
        ).build()
        pantryDao = db.pantryDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetItem() = runBlocking {
        val item = PantryItem(
            name = "Milk",
            quantity = 1.0,
            unit = PantryUnit.LITERS,
            expiryDate = System.currentTimeMillis(),
            category = "Dairy",
            notes = "For breakfast"
        )
        val id = pantryDao.insertItem(item)
        val retrievedItem = pantryDao.getItemById(id)
        assertEquals(item.name, retrievedItem?.name)
        assertEquals(item.unit, retrievedItem?.unit)
    }

    @Test
    @Throws(Exception::class)
    fun getAllItemsOrderedByExpiry() = runBlocking {
        val now = System.currentTimeMillis()
        val item1 = PantryItem(name = "Apples", quantity = 5.0, unit = PantryUnit.PIECES, expiryDate = now + 10000, category = "Fruit")
        val item2 = PantryItem(name = "Bananas", quantity = 3.0, unit = PantryUnit.PIECES, expiryDate = now + 5000, category = "Fruit")
        
        pantryDao.insertItem(item1)
        pantryDao.insertItem(item2)

        val allItems = pantryDao.getAllItems().first()
        assertEquals(2, allItems.size)
        assertEquals("Bananas", allItems[0].name) // Earlier expiry first
        assertEquals("Apples", allItems[1].name)
    }

    @Test
    @Throws(Exception::class)
    fun updateItem() = runBlocking {
        val item = PantryItem(name = "Eggs", quantity = 12.0, unit = PantryUnit.PIECES, expiryDate = null, category = "Dairy")
        val id = pantryDao.insertItem(item)
        val updatedItem = item.copy(id = id, quantity = 10.0)
        pantryDao.updateItem(updatedItem)
        
        val retrievedItem = pantryDao.getItemById(id)
        assertEquals(10.0, retrievedItem?.quantity ?: 0.0, 0.0)
    }

    @Test
    @Throws(Exception::class)
    fun deleteItem() = runBlocking {
        val item = PantryItem(name = "Bread", quantity = 1.0, unit = PantryUnit.PIECES, expiryDate = null, category = "Bakery")
        val id = pantryDao.insertItem(item)
        val retrievedItem = pantryDao.getItemById(id)
        pantryDao.deleteItem(retrievedItem!!)
        
        val allItems = pantryDao.getAllItems().first()
        assertTrue(allItems.isEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun getItemsByCategory() = runBlocking {
        val item1 = PantryItem(name = "Milk", quantity = 1.0, unit = PantryUnit.LITERS, expiryDate = null, category = "Dairy")
        val item2 = PantryItem(name = "Cheese", quantity = 500.0, unit = PantryUnit.GRAMS, expiryDate = null, category = "Dairy")
        val item3 = PantryItem(name = "Apples", quantity = 5.0, unit = PantryUnit.PIECES, expiryDate = null, category = "Fruit")
        
        pantryDao.insertItem(item1)
        pantryDao.insertItem(item2)
        pantryDao.insertItem(item3)

        val dairyItems = pantryDao.getItemsByCategory("Dairy").first()
        assertEquals(2, dairyItems.size)
        assertTrue(dairyItems.all { it.category == "Dairy" })
    }
}
