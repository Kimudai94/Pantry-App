package com.example.pantrypure.data.model

enum class PantryUnit(val label: String) {
    LITERS("L"),
    MILLILITERS("ml"),
    GRAMS("g"),
    KILOGRAMS("kg"),
    PIECES("pcs");

    companion object {
        fun fromString(value: String): PantryUnit? {
            return entries.find { it.label.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) }
        }
    }
}
