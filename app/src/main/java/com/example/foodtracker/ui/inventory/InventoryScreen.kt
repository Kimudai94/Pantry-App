package com.example.foodtracker.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InventoryScreen(viewModel: InventoryViewModel) {
  val state by viewModel.state.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) {
    viewModel.events.collect { ev ->
      when (ev) {
        is InventoryEvent.ShowUndo -> {
          val res = snackbarHostState.showSnackbar(
            message = "Menge geändert",
            actionLabel = "Rückgängig"
          )
          if (res == SnackbarResult.ActionPerformed) {
            viewModel.undo(ev.id, ev.deltaApplied)
          }
        }
        is InventoryEvent.ShowError -> {
          snackbarHostState.showSnackbar(ev.message)
          // optional: viewModel.clearError()
        }
      }
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { padding ->
    when {
      state.isLoading -> Box(
        Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator()
      }
      else -> InventoryContent(
        modifier = Modifier.fillMaxSize().padding(padding),
        state = state,
        viewModel = viewModel
      )
    }
  }
}

@Composable
private fun InventoryContent(
  modifier: Modifier = Modifier,
  state: InventoryUiState,
  viewModel: InventoryViewModel
) {
  LazyColumn(
    modifier = Modifier.padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    items(state.items) { item ->
      Card(Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(item.name, style = MaterialTheme.typography.titleMedium)
            Text("Menge: ${item.quantity} ${item.unit}")
            item.expiresAt?.let { Text("Ablauf: $it") }
          }
          Row {
            IconButton(onClick = { viewModel.dec(item.id) }) { Text("–") }
            IconButton(onClick = { viewModel.inc(item.id) }) { Text("+") }
          }
        }
      }
    }
  }
}