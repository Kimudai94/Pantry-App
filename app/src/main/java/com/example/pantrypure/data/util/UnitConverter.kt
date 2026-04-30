package com.example.pantrypure.data.util

import com.example.pantrypure.data.model.PantryUnit

object UnitConverter {

    fun convert(value: Double, from: PantryUnit, to: PantryUnit): Double {
        if (from == to) return value

        if (isVolume(from) && isVolume(to)) {
            val valueInMl = when (from) {
                PantryUnit.LITERS -> value * 1000.0
                PantryUnit.MILLILITERS -> value
                else -> throw IllegalArgumentException("Unsupported volume unit: $from")
            }
            return when (to) {
                PantryUnit.LITERS -> valueInMl / 1000.0
                PantryUnit.MILLILITERS -> valueInMl
                else -> throw IllegalArgumentException("Unsupported volume unit: $to")
            }
        }

        if (isWeight(from) && isWeight(to)) {
            val valueInG = when (from) {
                PantryUnit.KILOGRAMS -> value * 1000.0
                PantryUnit.GRAMS -> value
                else -> throw IllegalArgumentException("Unsupported weight unit: $from")
            }
            return when (to) {
                PantryUnit.KILOGRAMS -> valueInG / 1000.0
                PantryUnit.GRAMS -> valueInG
                else -> throw IllegalArgumentException("Unsupported weight unit: $to")
            }
        }

        throw IllegalArgumentException("Incompatible units: $from and $to")
    }

    private fun isVolume(unit: PantryUnit): Boolean {
        return unit == PantryUnit.LITERS || unit == PantryUnit.MILLILITERS
    }

    private fun isWeight(unit: PantryUnit): Boolean {
        return unit == PantryUnit.KILOGRAMS || unit == PantryUnit.GRAMS
    }
}
