package com.example.pantrypure.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pantrypure.data.model.PantryItem
import com.example.pantrypure.data.model.PantryUnit
import com.example.pantrypure.ui.viewmodel.PantryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemScreen(
    viewModel: PantryViewModel,
    itemId: Long?,
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(PantryUnit.PIECES) }
    var category by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf<Long?>(null) }
    var expiryThresholdDays by remember { mutableStateOf("3") }
    
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        if (itemId != null && itemId != -1L) {
            val item = viewModel.getItemById(itemId)
            item?.let {
                name = it.name
                quantity = it.quantity.toString()
                unit = it.unit
                category = it.category
                notes = it.notes
                expiryDate = it.expiryDate
                expiryThresholdDays = it.expiryThresholdDays.toString()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (itemId == null || itemId == -1L) "Add Item" else "Edit Item") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    modifier = Modifier.weight(1f)
                )

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = unit.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        PantryUnit.entries.forEach { pantryUnit ->
                            DropdownMenuItem(
                                text = { Text(pantryUnit.label) },
                                onClick = {
                                    unit = pantryUnit
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth()
            )

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            OutlinedTextField(
                value = expiryDate?.let { sdf.format(Date(it)) } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Expiry Date") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = expiryThresholdDays,
                onValueChange = { if (it.all { char -> char.isDigit() }) expiryThresholdDays = it },
                label = { Text("Expiry Warning Threshold (Days)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
            )

            Button(
                onClick = {
                    val item = PantryItem(
                        id = if (itemId == null || itemId == -1L) 0 else itemId,
                        name = name,
                        quantity = quantity.toDoubleOrNull() ?: 0.0,
                        unit = unit,
                        category = category,
                        notes = notes,
                        expiryDate = expiryDate,
                        expiryThresholdDays = expiryThresholdDays.toIntOrNull() ?: 3
                    )
                    if (item.id == 0L) {
                        viewModel.addItem(item)
                    } else {
                        viewModel.updateItem(item)
                    }
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && quantity.toDoubleOrNull() != null
            ) {
                Text("Save")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = expiryDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    expiryDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
