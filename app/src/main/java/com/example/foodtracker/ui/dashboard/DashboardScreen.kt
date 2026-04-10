package com.example.foodtracker.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onMealClick: (Long) -> Unit
) {
    val state by viewModel.state.collectAsState()

    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.error != null -> Text("Fehler: ${state.error}")
        else -> DashboardContent(state, onMealClick)
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onMealClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        section("Jetzt essen", state.nowEating, onMealClick)
        section("Diese Woche", state.thisWeek, onMealClick)
        section("Tiefkühl‑Reserve", state.freezerReserve, onMealClick)
    }
}

private fun LazyListScope.section(
    title: String,
    items: List<DashboardMealItem>,
    onMealClick: (Long) -> Unit
) {
    item { Text(title, style = MaterialTheme.typography.titleLarge) }
    if (items.isEmpty()) {
        item { Text("– nichts –") }
    } else {
        items(items) { m -> DashboardMealRow(m, onMealClick) }
    }
}

@Composable
private fun DashboardMealRow(
    item: DashboardMealItem,
    onMealClick: (Long) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onMealClick(item.mealId) }) {
        Column(Modifier.padding(12.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium)
            Text("Portionen: ${item.remainingPortions}")
            Text("Status: ${item.status} • Ablauf in ${item.expiresInDays} Tagen")
        }
    }
}
