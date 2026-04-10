package com.example.foodtracker.ui.inventory

import java.time.LocalDate

data class InventoryUiState(
  val items: List<InventoryItemUi> = emptyList(),
  val isLoading: Boolean = true,
  val error: String? = null
)

data class InventoryItemUi(
  val id: Long,
  val name: String,
  val quantity: Int,
  val unit: String,
  val expiresAt: LocalDate?
)
