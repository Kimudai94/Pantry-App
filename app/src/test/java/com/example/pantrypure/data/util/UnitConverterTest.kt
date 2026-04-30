package com.example.pantrypure.data.util

import com.example.pantrypure.data.model.PantryUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {

    @Test
    fun convert_litersToMilliliters() {
        val result = UnitConverter.convert(1.0, PantryUnit.LITERS, PantryUnit.MILLILITERS)
        assertEquals(1000.0, result, 0.001)
    }

    @Test
    fun convert_millilitersToLiters() {
        val result = UnitConverter.convert(500.0, PantryUnit.MILLILITERS, PantryUnit.LITERS)
        assertEquals(0.5, result, 0.001)
    }

    @Test
    fun convert_kilogramsToGrams() {
        val result = UnitConverter.convert(2.5, PantryUnit.KILOGRAMS, PantryUnit.GRAMS)
        assertEquals(2500.0, result, 0.001)
    }

    @Test
    fun convert_gramsToKilograms() {
        val result = UnitConverter.convert(750.0, PantryUnit.GRAMS, PantryUnit.KILOGRAMS)
        assertEquals(0.75, result, 0.001)
    }

    @Test
    fun convert_sameUnit() {
        val result = UnitConverter.convert(10.0, PantryUnit.PIECES, PantryUnit.PIECES)
        assertEquals(10.0, result, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun convert_incompatibleUnits_volumeToWeight() {
        UnitConverter.convert(1.0, PantryUnit.LITERS, PantryUnit.GRAMS)
    }

    @Test(expected = IllegalArgumentException::class)
    fun convert_incompatibleUnits_weightToPieces() {
        UnitConverter.convert(1.0, PantryUnit.KILOGRAMS, PantryUnit.PIECES)
    }
}
