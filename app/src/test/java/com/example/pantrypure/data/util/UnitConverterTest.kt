package com.example.pantrypure.data.util

import com.example.pantrypure.data.model.PantryUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {

    @Test
    fun `convert same weight unit returns same value`() {
        val result = UnitConverter.convert(10.0, PantryUnit.GRAMS, PantryUnit.GRAMS)
        assertEquals(10.0, result, 0.001)
    }

    @Test
    fun `convert grams to kilograms`() {
        val result = UnitConverter.convert(1500.0, PantryUnit.GRAMS, PantryUnit.KILOGRAMS)
        assertEquals(1.5, result, 0.001)
    }

    @Test
    fun `convert kilograms to grams`() {
        val result = UnitConverter.convert(2.5, PantryUnit.KILOGRAMS, PantryUnit.GRAMS)
        assertEquals(2500.0, result, 0.001)
    }

    @Test
    fun `convert milliliters to liters`() {
        val result = UnitConverter.convert(750.0, PantryUnit.MILLILITERS, PantryUnit.LITERS)
        assertEquals(0.75, result, 0.001)
    }

    @Test
    fun `convert liters to milliliters`() {
        val result = UnitConverter.convert(1.2, PantryUnit.LITERS, PantryUnit.MILLILITERS)
        assertEquals(1200.0, result, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `convert incompatible units weight to volume throws exception`() {
        UnitConverter.convert(1.0, PantryUnit.KILOGRAMS, PantryUnit.LITERS)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `convert incompatible units volume to weight throws exception`() {
        UnitConverter.convert(1.0, PantryUnit.MILLILITERS, PantryUnit.GRAMS)
    }
    
    @Test
    fun `convert pieces to pieces`() {
        // Assuming pieces is not volume or weight, convert should return value if from == to
        val result = UnitConverter.convert(5.0, PantryUnit.PIECES, PantryUnit.PIECES)
        assertEquals(5.0, result, 0.001)
    }
}
