package com.example.pantrypure.ui.viewmodel

import com.example.pantrypure.data.repository.PantryRepository
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Calendar
import java.util.TimeZone

class TimeZoneStabilityTest {

    private val repository: PantryRepository = mock()

    @Test
    fun `getStartOfWeek is consistent across different time zones`() {
        // Wir testen drei extreme Zeitzonen
        val timeZones = listOf("UTC", "America/New_York", "Asia/Tokyo")
        
        // Mock default flows für ViewModel Init
        whenever(repository.getAllItems()).thenReturn(flowOf(emptyList()))
        whenever(repository.getConsumptionHistory()).thenReturn(flowOf(emptyList()))
        whenever(repository.getShoppingListItems()).thenReturn(flowOf(emptyList()))
        whenever(repository.getAllMealsWithIngredients()).thenReturn(flowOf(emptyList()))

        timeZones.forEach { zoneId ->
            TimeZone.setDefault(TimeZone.getTimeZone(zoneId))
            
            val viewModel = PantryViewModel(repository)
            val plannerDate = viewModel.currentPlannerDate.value
            
            val calendar = Calendar.getInstance(TimeZone.getTimeZone(zoneId))
            calendar.timeInMillis = plannerDate
            
            // In jeder Zeitzone muss der berechnete Start der Woche ein Montag um 00:00:00 sein
            assertEquals("Failed for zone $zoneId", Calendar.MONDAY, calendar.get(Calendar.DAY_OF_WEEK))
            assertEquals("Failed for zone $zoneId", 0, calendar.get(Calendar.HOUR_OF_DAY))
            assertEquals("Failed for zone $zoneId", 0, calendar.get(Calendar.MINUTE))
        }
        
        // Reset to system default
        TimeZone.setDefault(null)
    }
}
