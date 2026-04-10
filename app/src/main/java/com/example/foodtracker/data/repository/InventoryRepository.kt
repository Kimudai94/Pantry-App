package com.example.foodtracker.data.repository

import com.example.foodtracker.data.dao.InventoryDao
import com.example.foodtracker.data.db.entity.InventoryItemEntity
import com.example.foodtracker.ui.inventory.InventoryItemUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InventoryRepository(
    private val dao: InventoryDao
) {
    fun observeInventory(): Flow<List<InventoryItemUi>> =
      dao.observeInventory().map { list -> list.map { it.toUi() } }

    fun getById(id: Long): InventoryItemUi = dao.getById(id).toUi()

    suspend fun changeQuantity(id: Long, delta: Int) {
        val current = dao.getById(id)
        val newQty = (current.quantity + delta).coerceAtLeast(0)
        dao.update(current.copy(quantity = newQty))
    }

    private fun InventoryItemEntity.toUi() = InventoryItemUi(
      id = id,
      name = name,
      quantity = quantity,
      unit = unit,
      expiresAt = expiresAt
    )
}