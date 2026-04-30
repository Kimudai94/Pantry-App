package com.example.pantrypure.data.model

enum class MealCategory(val label: String) {
    BREAKFAST("Frühstück"),
    LUNCH("Mittagessen"),
    DINNER("Abendessen"),
    SNACK("Snack"),
    OTHER("Sonstiges");

    companion object {
        fun fromString(value: String): MealCategory? {
            return entries.find { it.label.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) }
        }
    }
}
