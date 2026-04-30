package com.example.pantrypure.ui.screen

import android.R.attr.enabled
import android.R.attr.type
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pantrypure.data.model.PantryItem
import com.example.pantrypure.data.model.PantryUnit
import com.example.pantrypure.ui.viewmodel.PantryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealIngredientPickerSheet(
    viewModel: PantryViewModel,
    onIngredientSelected: (String, Double, PantryUnit) -> Unit,
    onDismiss: () -> Unit
) {
    val pantryItems by viewModel.pantryItems.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedItemId by remember { mutableStateOf<Long?>(null) }
    var quantity by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf<PantryUnit?>(null) }

    val filteredItems = pantryItems.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Zutat auswählen", style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Schließen")
                }
            }

            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Nach Zutat suchen") },
                modifier = Modifier.fillMaxWidth()
            )

            // Items list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredItems) { item ->
                    IngredientSelectionCard(
                        item = item,
                        isSelected = selectedItemId == item.id,
                        onSelect = {
                            selectedItemId = item.id
                            selectedUnit = item.unit
                        }
                    )
                }
            }

            // Quantity input (if item selected)
            if (selectedItemId != null) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Menge") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Unit selection
                var unitMenuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = unitMenuExpanded,
                    onExpandedChange = { unitMenuExpanded = !unitMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedUnit?.label ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Einheit") },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = unitMenuExpanded,
                        onDismissRequest = { unitMenuExpanded = false }
                    ) {
                        PantryUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.label) },
                                onClick = {
                                    selectedUnit = unit
                                    unitMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Add button
                Button(
                    onClick = {
                        val item = pantryItems.find { it.id == selectedItemId }
                        if (item != null && quantity.toDoubleOrNull() != null && selectedUnit != null) {
                            onIngredientSelected(item.name, quantity.toDouble(), selectedUnit!!)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedItemId != null && quantity.toDoubleOrNull() != null && selectedUnit != null
                ) {
                    Text("Zutat hinzufügen")
                }
            }
        }
    }
}

@Composable
private fun IngredientSelectionCard(
    item: PantryItem,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect() }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Verfügbar: ${item.quantity} ${item.unit.label}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (isSelected) {
                Text("✓", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}
