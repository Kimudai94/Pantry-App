package com.example.pantrypure.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.pantrypure.data.model.*
import com.example.pantrypure.ui.viewmodel.PantryViewModel
import com.example.pantrypure.ui.viewmodel.MealOperationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDetailScreen(
    viewModel: PantryViewModel,
    mealId: Long,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit
) {
    var mealWithIngredients by remember { mutableStateOf<MealWithIngredients?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInsufficientDialog by remember { mutableStateOf(false) }
    var insufficientIngredients by remember { mutableStateOf<List<MissingIngredient>>(emptyList()) }
    val operationState by viewModel.mealOperationState.collectAsState()
    var pantryItems by remember { mutableStateOf<Map<Long, PantryItem>>(emptyMap()) }

    // Load meal details
    LaunchedEffect(mealId) {
        mealWithIngredients = viewModel.getMealWithIngredients(mealId)
    }

    // Collect pantry items for availability check
    val allPantryItems by viewModel.pantryItems.collectAsState()
    LaunchedEffect(allPantryItems) {
        pantryItems = allPantryItems.associateBy { it.id }
    }

    // Handle operation feedback
    LaunchedEffect(operationState) {
        when (operationState) {
            is MealOperationState.Success -> {
                if ((operationState as MealOperationState.Success).message.contains("gelöscht")) {
                    onNavigateBack()
                }
                viewModel.clearMealOperationState()
            }
            is MealOperationState.InsufficientIngredients -> {
                insufficientIngredients = (operationState as MealOperationState.InsufficientIngredients).missingItems
                showInsufficientDialog = true
                viewModel.clearMealOperationState()
            }
            else -> {}
        }
    }

    val meal = mealWithIngredients?.meal ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mahlzeit") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Löschen")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Meal info
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(meal.name, style = MaterialTheme.typography.headlineMedium)
                        Text(meal.category.label, style = MaterialTheme.typography.bodyMedium)
                        if (meal.description.isNotEmpty()) {
                            Text(meal.description, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Ingredients section
            item {
                Text("Zutaten", style = MaterialTheme.typography.headlineSmall)
            }

            items(mealWithIngredients?.ingredients ?: emptyList()) { ingredient ->
                IngredientAvailabilityCard(
                    ingredient = ingredient,
                    availableQuantity = pantryItems[ingredient.pantryItemId]?.quantity ?: 0.0
                )
            }

            // Consume button
            item {
                Button(
                    onClick = { viewModel.consumeMeal(mealId) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Mahlzeit verbrauchen")
                }
            }

            if (meal.notes.isNotEmpty()) {
                item {
                    Text("Notizen", style = MaterialTheme.typography.headlineSmall)
                    Text(meal.notes, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Mahlzeit löschen?") },
            text = { Text("Möchten Sie die Mahlzeit \"${meal.name}\" wirklich löschen?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMeal(meal)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Insufficient ingredients dialog
    if (showInsufficientDialog && insufficientIngredients.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showInsufficientDialog = false },
            title = { Text("Unzureichende Zutaten") },
            text = {
                Column {
                    Text("Folgende Zutaten sind nicht ausreichend vorhanden:")
                    insufficientIngredients.forEach { missing ->
                        Text(
                            "❌ ${missing.itemName}: benötigt ${missing.required}${missing.unit.label}, verfügbar ${missing.available}${missing.unit.label}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showInsufficientDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun IngredientAvailabilityCard(
    ingredient: MealIngredientWithName,
    availableQuantity: Double
) {
    val isAvailable = availableQuantity >= ingredient.requiredQuantity
    val deficit = ingredient.requiredQuantity - availableQuantity

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isAvailable -> Color.Green.copy(alpha = 0.1f)
                deficit <= ingredient.requiredQuantity * 0.25 -> Color.Yellow.copy(alpha = 0.1f)
                else -> Color.Red.copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(ingredient.pantryItemName, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Benötigt: ${ingredient.requiredQuantity} ${ingredient.requiredUnit.label}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Verfügbar: $availableQuantity ${ingredient.requiredUnit.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isAvailable) Color.Green else Color.Red
                )
            }
            Text(
                if (isAvailable) "✓" else "✗",
                color = if (isAvailable) Color.Green else Color.Red,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}
