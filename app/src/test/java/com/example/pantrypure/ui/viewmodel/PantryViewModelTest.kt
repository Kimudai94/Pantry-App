package com.example.pantrypure.ui.viewmodel

import com.example.pantrypure.data.model.PantryItem
import com.example.pantrypure.data.model.PantryUnit
import com.example.pantrypure.data.repository.PantryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class PantryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    @Mock
    private lateinit var repository: PantryRepository
    
    private lateinit var viewModel: PantryViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        val items = listOf(
            PantryItem(id = 1, name = "Milk", quantity = 1.0, unit = PantryUnit.LITERS, expiryDate = null, category = "Dairy")
        )
        `when`(repository.getAllItems()).thenReturn(flowOf(items))
        
        viewModel = PantryViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun pantryItems_initialValue() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val items = viewModel.pantryItems.first { it.isNotEmpty() }
        assertEquals(1, items.size)
        assertEquals("Milk", items[0].name)
    }
}
