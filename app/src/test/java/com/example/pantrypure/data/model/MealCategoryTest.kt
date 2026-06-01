package com.example.pantrypure.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MealCategoryTest {

    @Test
    fun `fromString matches labels correctly`() {
        assertEquals(MealCategory.BREAKFAST, MealCategory.fromString("Frühstück"))
        assertEquals(MealCategory.LUNCH, MealCategory.fromString("Mittagessen"))
        assertEquals(MealCategory.DINNER, MealCategory.fromString("Abendessen"))
    }

    @Test
    fun `fromString is case insensitive for names`() {
        assertEquals(MealCategory.BREAKFAST, MealCategory.fromString("breakfast"))
        assertEquals(MealCategory.BREAKFAST, MealCategory.fromString("BREAKFAST"))
    }

    @Test
    fun `fromString returns null for unknown values`() {
        assertNull(MealCategory.fromString("Pizza"))
        assertNull(MealCategory.fromString(""))
    }
}
