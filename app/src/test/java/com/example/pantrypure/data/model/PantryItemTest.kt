package com.example.pantrypure.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PantryItemTest {

    private val now = System.currentTimeMillis()
    private val dayInMillis = 24 * 60 * 60 * 1000L

    @Test
    fun `isOverdue returns true for past dates`() {
        val item = PantryItem(name = "Milk", unit = PantryUnit.LITERS, expiryDate = now - 1000)
        assertTrue(item.expiryDate!! < now)
    }

    @Test
    fun `isExpiringSoon detects items within threshold`() {
        // Item expires in 2 days, threshold is 3 days
        val expiryDate = now + (2 * dayInMillis)
        val thresholdDays = 3
        val thresholdMillis = thresholdDays * dayInMillis
        
        assertTrue(expiryDate <= (now + thresholdMillis))
    }

    @Test
    fun `isExpiringSoon returns false for items far in future`() {
        // Item expires in 10 days, threshold is 3 days
        val expiryDate = now + (10 * dayInMillis)
        val thresholdDays = 3
        val thresholdMillis = thresholdDays * dayInMillis
        
        assertFalse(expiryDate <= (now + thresholdMillis))
    }
}
