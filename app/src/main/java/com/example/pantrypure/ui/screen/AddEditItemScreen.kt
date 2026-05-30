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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pantrypure.ui.theme.PantryPureTheme
import com.example.pantrypure.data.model.PantryItem
import com.example.pantrypure.data.model.PantryUnit
import com.example.pantrypure.ui.viewmodel.PantryViewModel
import com.example.pantrypure.util.NumberMode
import com.example.pantrypure.util.NumberSpinnerFlexible
import com.example.pantrypure.util.PantryDropdownSelector
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

val CATEGORIES = listOf("Food", "Beverage", "Cleaning", "Hygiene", "Other")
val LOCATIONS = listOf("Fridge", "Pantry", "Freezer", "Cellar", "Shelf")
@Composable
fun AddEditItemScreen(
    viewModel: PantryViewModel,
    itemId: Long?,
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(PantryUnit.PIECES) }
    var category by remember { mutableStateOf(CATEGORIES[0]) }
    var location by remember { mutableStateOf(LOCATIONS[0]) }
    var notes by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf<Long?>(null) }
    var expiryThresholdDays by remember { mutableIntStateOf(3) }
    var quantityThreshold by remember { mutableDoubleStateOf(1.0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(itemId) {
        if (itemId != null && itemId != -1L) {
            val item = viewModel.getItemById(itemId)
            item?.let {
                name = it.name
                quantity = it.quantity.toString()
                unit = it.unit
                category = it.category
                location = it.location
                notes = it.notes
                expiryDate = it.expiryDate
                expiryThresholdDays = it.expiryThresholdDays
                quantityThreshold = it.quantityThreshold
            }
        }
    }

    AddEditItemContent(
        name = name,
        onNameChange = { name = it },
        quantity = quantity,
        onQuantityChange = { quantity = it },
        unit = unit,
        onUnitChange = { unit = it },
        category = category,
        onCategoryChange = { category = it },
        location = location,
        onLocationChange = { location = it },
        notes = notes,
        onNotesChange = { notes = it },
        expiryDate = expiryDate,
        onExpiryDateChange = { expiryDate = it },
        expiryThresholdDays = expiryThresholdDays,
        onExpiryThresholdDaysChange = { expiryThresholdDays = it },
        quantityThreshold = quantityThreshold,
        onQuantityThresholdChange = { quantityThreshold = it },
        onSave = {
            val newQuantity = ((quantity.toDoubleOrNull() ?: 0.0) * 100.0).roundToInt() / 100.0
            val item = PantryItem(
                id = if (itemId == null || itemId == -1L) 0 else itemId,
                name = name,
                quantity = newQuantity,
                unit = unit,
                category = category,
                location = location,
                notes = notes,
                expiryDate = expiryDate,
                expiryThresholdDays = expiryThresholdDays,
                quantityThreshold = quantityThreshold
            )
            scope.launch {
                if (item.id == 0L) {
                    viewModel.addItem(item)
                } else {
                    viewModel.updateItem(item)
                }
                onNavigateBack()
            }
        },
        onNavigateBack = onNavigateBack,
        isEditMode = itemId != null && itemId != -1L
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemContent(
    name: String,
    onNameChange: (String) -> Unit,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    unit: PantryUnit,
    onUnitChange: (PantryUnit) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    expiryDate: Long?,
    onExpiryDateChange: (Long?) -> Unit,
    expiryThresholdDays: Int,
    onExpiryThresholdDaysChange: (Int) -> Unit,
    quantityThreshold: Double,
    onQuantityThresholdChange: (Double) -> Unit,
    onSave: () -> Unit,
    onNavigateBack: () -> Unit,
    isEditMode: Boolean
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (!isEditMode) "Add Item" else "Edit Item") },
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
                onValueChange = onNameChange,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberSpinnerFlexible(
                    NumberMode.DECIMAL,
                    label = "Quantity",
                    initial = quantity.toDoubleOrNull() ?: 1.0,
                    onValueChange = { onQuantityChange(it.toString()) }
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
                                    onUnitChange(pantryUnit)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            PantryDropdownSelector(
                label = "Category",
                options = CATEGORIES,
                selectedOption = category,
                onOptionSelected = onCategoryChange
            )

            PantryDropdownSelector(
                label = "Location",
                options = LOCATIONS,
                selectedOption = location,
                onOptionSelected = onLocationChange
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
                onValueChange = onNotesChange,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            NumberSpinnerFlexible(
                mode = NumberMode.INTEGER,
                label = "Expiry Threshold (days)",
                initial = expiryThresholdDays.toDouble(),
                onValueChange = { onExpiryThresholdDaysChange(it.toInt()) }
            )

            NumberSpinnerFlexible(
                mode = NumberMode.DECIMAL,
                label = "Low Quantity Threshold",
                initial = quantityThreshold,
                onValueChange = onQuantityThresholdChange
            )

            Button(
                onClick = onSave,
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
                    onExpiryDateChange(datePickerState.selectedDateMillis)
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

@Preview(showBackground = true)
@Composable
fun AddEditItemPreview() {
    PantryPureTheme {
        AddEditItemContent(
            name = "Milk",
            onNameChange = {},
            quantity = "2.0",
            onQuantityChange = {},
            unit = PantryUnit.LITERS,
            onUnitChange = {},
            category = "Dairy",
            onCategoryChange = {},
            location = "Fridge",
            onLocationChange = {},
            notes = "Organic milk",
            onNotesChange = {},
            expiryDate = System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7,
            onExpiryDateChange = {},
            expiryThresholdDays = 3,
            onExpiryThresholdDaysChange = {},
            quantityThreshold = 1.0,
            onQuantityThresholdChange = {},
            onSave = {},
            onNavigateBack = {},
            isEditMode = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddEditItemEditPreview() {
    PantryPureTheme {
        AddEditItemContent(
            name = "Milk",
            onNameChange = {},
            quantity = "2.0",
            onQuantityChange = {},
            unit = PantryUnit.LITERS,
            onUnitChange = {},
            category = "Dairy",
            onCategoryChange = {},
            location = "Fridge",
            onLocationChange = {},
            notes = "Organic milk",
            onNotesChange = {},
            expiryDate = System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7,
            onExpiryDateChange = {},
            expiryThresholdDays = 3,
            onExpiryThresholdDaysChange = {},
            quantityThreshold = 1.0,
            onQuantityThresholdChange = {},
            onSave = {},
            onNavigateBack = {},
            isEditMode = true
        )
    }
}
