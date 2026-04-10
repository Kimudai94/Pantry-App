package com.example.foodtracker.domain.usecase
import com.example.foodtracker.domain.model.Meal 
import com.example.foodtracker.domain.model.MealStatus 
import com.example.foodtracker.domain.model.StorageLocation 
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate

class ComputeMealStatusTest {
    private val useCase = ComputeMealStatus()
    @Test fun fresh when far from expiry() { 
        val today = LocalDate.of(2026, 4, 8) 
        val meal = Meal(1, "Pasta", today.minusDays(1), StorageLocation.FRIDGE, 2, 5) 
        assertEquals(MealStatus.FRESH, useCase(meal, today))
    }
    @Test fun soon when one day before expiry() { 
        val today = LocalDate.of(2026, 4, 8) 
        val meal = Meal(1, "Pasta", today.minusDays(3), StorageLocation.FRIDGE, 2, 4) // expiry: today+1 
        assertEquals(MealStatus.SOON, useCase(meal, today))
    }
    @Test fun expired when after expiry() { 
        val today = LocalDate.of(2026, 4, 8) 
        val meal = Meal(1, "Pasta", today.minusDays(3), StorageLocation.FRIDGE, 2, 2) // expiry: today-1
        assertEquals(MealStatus.EXPIRED, useCase(meal, today))
    }
}


