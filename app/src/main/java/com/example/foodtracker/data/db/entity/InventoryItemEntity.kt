package com.example.foodtracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "inventory_item")
data class InventoryItemEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val quantity: Int,
  val unit: String,
  val expiresAt: LocalDate? = null
)