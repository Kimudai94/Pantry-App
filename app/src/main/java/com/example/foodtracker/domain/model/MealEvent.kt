package com.example.foodtracker.domain.model

import java.time.LocalDateTime

data class MealEvent(
  val mealId: Long,
  val type: MealEventType,
  val portions: Int = 1,
  val occurredAt: LocalDateTime = LocalDateTime.now()
)

enum class MealEventType {
  EATEN,
  DISCARDED
}
