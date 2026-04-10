package com.example.foodtracker.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed interface InventoryEvent {
  data class ShowUndo(val id: Long, val deltaApplied: Int) : InventoryEvent
  data class ShowError(val message: String) : InventoryEvent
}

class InventoryViewModel(
  inventoryFlow: Flow<List<InventoryItemUi>>,
  private val changeQuantity: suspend (id: Long, delta: Int) -> Unit
) : ViewModel() {

  private val errorFlow = MutableStateFlow<String?>(null)
  private val _events = MutableSharedFlow<InventoryEvent>(extraBufferCapacity = 1)
  val events: SharedFlow<InventoryEvent> = _events.asSharedFlow()

  val state: StateFlow<InventoryUiState> =
    combine(inventoryFlow, errorFlow) { items, error ->
      InventoryUiState(
        items = items,
        isLoading = false,
        error = error
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = InventoryUiState(isLoading = true)
    )

  fun inc(id: Long) = update(id, +1)
  fun dec(id: Long) = update(id, -1)

  private fun update(id: Long, delta: Int) {
    viewModelScope.launch {
      runCatching { changeQuantity(id, delta) }
        .onSuccess {
          _events.tryEmit(InventoryEvent.ShowUndo(id, deltaApplied = delta))
          errorFlow.value = null
        }
        .onFailure { e ->
          val msg = e.message ?: "Unbekannter Fehler"
          errorFlow.value = msg
          _events.tryEmit(InventoryEvent.ShowError(msg))
        }
    }
  }

  fun undo(id: Long, deltaApplied: Int) {
    viewModelScope.launch {
      runCatching { changeQuantity(id, -deltaApplied) }
        .onFailure { e ->
          val msg = e.message ?: "Undo fehlgeschlagen"
          errorFlow.value = msg
          _events.tryEmit(InventoryEvent.ShowError(msg))
        }
    }
  }

  fun clearError() {
    errorFlow.value = null
  }

}