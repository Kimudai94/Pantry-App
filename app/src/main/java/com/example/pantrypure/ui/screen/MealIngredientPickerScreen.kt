package com.example.pantrypure.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pantrypure.data.model.PantryItem
import com.example.pantrypure.data.model.PantryUnit
import com.example.pantrypure.ui.viewmodel.PantryViewModel
import com.example.pantrypure.util.NumberMode
import com.example.pantrypure.util.NumberSpinnerFlexible
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealIngredientPickerSheet(
    viewModel: PantryViewModel,
    onIngredientSelected: (Long, String, Double, PantryUnit) -> Unit,
    onDismiss: () -> Unit
) {
    val pantryItems by viewModel.pantryItems.collectAsState()
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedItemId by remember { mutableStateOf<Long?>(null) }
    var quantity by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf<PantryUnit?>(null) }
    var isCreatingNewItem by remember { mutableStateOf(false) }

    val filteredItems = pantryItems.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = { WindowInsets.ime }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
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
            Box(modifier = Modifier.heightIn(max = 250.dp)) {
                if (filteredItems.isEmpty() && searchQuery.isNotBlank()) {
                    OutlinedCard(
                        onClick = {
                            isCreatingNewItem = true
                            selectedItemId = -1L // Markierung für "Neu"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isCreatingNewItem)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Text("'$searchQuery' als neue Zutat anlegen")
                        }
                    }
                } else {
                    LazyColumn(
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
                }
            }

            // Quantity input (if item selected)
            if (selectedItemId != null || isCreatingNewItem) {
                val displayName = if (isCreatingNewItem) searchQuery else
                    pantryItems.find { it.id == selectedItemId }?.name ?: ""

                Text("Details für: $displayName", style = MaterialTheme.typography.labelLarge)

                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                NumberSpinnerFlexible(
                    NumberMode.DECIMAL,
                    label = "Quantity",
                    initial = quantity.toDoubleOrNull() ?: 1.0,
                    onValueChange = { quantity = it.toString() },
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
                        scope.launch {
                            val item: PantryItem?
                            if (isCreatingNewItem) {
                                val newItem = PantryItem(
                                    id = 0,
                                    name = searchQuery,
                                    quantity = 0.0,
                                    unit = selectedUnit ?: PantryUnit.PIECES,
                                    isOnShoppingList = true
                                )
                                val generatedId = viewModel.addItem(newItem)
                                item = newItem.copy(id = generatedId)
                            } else {
                                item = pantryItems.find { it.id == selectedItemId }
                            }

                            if (item != null && item.id != 0L && quantity.toDoubleOrNull() != null && selectedUnit != null) {
                                onIngredientSelected(
                                    item.id,
                                    item.name,
                                    quantity.toDoubleOrNull() ?: 1.0,
                                    selectedUnit!!
                                )
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = (selectedItemId != null || (isCreatingNewItem && searchQuery.isNotBlank()))
                            && quantity.toDoubleOrNull() != null
                            && selectedUnit != null
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
