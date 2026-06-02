package com.example.pantrypure.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.pantrypure.PantryPureApplication
import com.example.pantrypure.data.model.PantryItem
import com.example.pantrypure.data.model.PantryUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ExpiryCheckWorkerTest {
    private lateinit var context: Context
    private lateinit var application: PantryPureApplication

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        application = context as PantryPureApplication
    }

    @Test
    fun testExpiryCheckWorker_Success() = runBlocking {
        // Prepare data in the real database (cleaned up after test if necessary, 
        // but here we just want to see if it runs)
        val repository = application.repository
        
        // Clear existing items to have a clean state
        // In a real scenario, we might want to use a separate test database
        // but for this task I will just add a specific item.
        
        val expiringItem = PantryItem(
            name = "Test Milk",
            quantity = 1.0,
            unit = PantryUnit.LITERS,
            category = "Dairy",
            location = "Fridge",
            expiryDate = System.currentTimeMillis() + (1 * 24 * 60 * 60 * 1000L), // Expires in 1 day
            expiryThresholdDays = 3 // Threshold is 3 days, so it should be detected
        )
        repository.insertItem(expiringItem)

        val worker = TestListenableWorkerBuilder<ExpiryCheckWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        // Note: Testing actual notifications is harder as it involves UI/System state,
        // but we've verified the worker completes successfully with the logic.
    }
}
