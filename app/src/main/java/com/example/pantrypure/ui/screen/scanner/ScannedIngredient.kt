package com.example.pantrypure.ui.screen.scanner

import com.example.pantrypure.data.model.PantryUnit

data class ScannedIngredient(
    val name: String,
    val quantity: Double = 1.0,
    val unit: PantryUnit = PantryUnit.PIECES
)
