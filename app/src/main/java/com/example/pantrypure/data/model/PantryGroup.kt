package com.example.pantrypure.data.model

data class PantryGroup(
  val name: String,
  val totalQuantity: Double,
  val unit: String,
  val minExpiryDate: Long?,
  val category: String,
  val anyIsOnShoppingList: Boolean,
  val totalNeeded: Double
)