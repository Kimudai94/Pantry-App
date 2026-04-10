package com.example.foodtracker.ui.meal

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MealScreen(viewModel: MealViewModel) {
    val state by viewModel.state.collectAsState()

    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.error != null -> Text("Fehler: ${state.error}")
        else -> MealContent(state, onEat = { viewModel.eatOnePortion() })
    }
}

@Composable
private fun MealContent(state: MealUiState, onEat: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(state.name, style = MaterialTheme.typography.headlineSmall)
        Text("Portionen gesamt: ${state.totalPortions}")
        Text("Status: ${state.status}")
        Text("Ablaufdatum: ${state.expiresAt}")
        state.notes?.let { Text("Notiz: $it") }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onEat, enabled = state.remainingPortions > 0) {
            Text("1 Portion gegessen")
        }
    }
}
