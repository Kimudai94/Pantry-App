package com.example.pantrypure.data.model

data class StockRecommendation(
    val offer: Offer,
    val reason: String,
    val suggestedQuantity: Double
)
