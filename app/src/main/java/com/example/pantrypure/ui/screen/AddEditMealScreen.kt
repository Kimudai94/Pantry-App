package com.example.pantrypure.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pantrypure.data.model.Meal
import com.example.pantrypure.data.model.MealCategory
import com.example.pantrypure.data.model.MealIngredient
import com.example.pantrypure.data.model.MealIngredientWithName
import com.example.pantrypure.ui.viewmodel.PantryViewModel
import com.example.pantrypure.ui.viewmodel.MealOperationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMealScreen(
    viewModel: PantryViewModel,
    mealId: Long?,
    onNavigateBack: () -> Unit
) {
    var mealName by remember { mutableStateOf("") }
    var mealDescription by remember { mutableStateOf("") }
    var mealNotes by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(MealCategory.OTHER) }
    var ingredients by remember { mutableStateOf<List<MealIngredientWithName>>(emptyList()) }
    var showIngredientPicker by remember { mutableStateOf(false) }
    val operationState by viewModel.mealOperationState.collectAsState()

    // Load meal data if editing
    LaunchedEffect(mealId) {
        if (mealId != null && mealId > 0) {
            val meal = viewModel.getMealWithIngredients(mealId)
            if (meal != null) {
                mealName = meal.meal.name
                mealDescription = meal.meal.description
                mealNotes = meal.meal.notes
                selectedCategory = meal.meal.category
                ingredients = meal.ingredients
            }
        }
    }

    // Handle operation feedback
    LaunchedEffect(operationState) {
        when (operationState) {
            is MealOperationState.Success -> {
                onNavigateBack()
                viewModel.clearMealOperationState()
            }
            else -> {}
        }
    }

    if (showIngredientPicker) {
        MealIngredientPickerSheet(
            viewModel = viewModel,
            onIngredientSelected = { itemId, name, qty, unit ->
                ingredients = ingredients + MealIngredientWithName(
                    pantryItemId = itemId,
                    ingredientName = name,
                    requiredQuantity = qty,
                    requiredUnit = unit
                )
                showIngredientPicker = false
            },
            onDismiss = { showIngredientPicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mealId != null && mealId > 0) "Mahlzeit bearbeiten" else "Neue Mahlzeit") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
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
            // Error messages
            item {
                when (operationState) {
                    is MealOperationState.Error -> {
                        Text(
                            (operationState as MealOperationState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    else -> {}
                }
            }

            // Name field
            item {
                OutlinedTextField(
                    value = mealName,
                    onValueChange = { mealName = it },
                    label = { Text("Mahlzeitenname") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = mealName.isBlank()
                )
            }

            // Category dropdown
            item {
                var categoryMenuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategorie") },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false }
                    ) {
                        MealCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.label) },
                                onClick = {
                                    selectedCategory = category
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Description field
            item {
                OutlinedTextField(
                    value = mealDescription,
                    onValueChange = { mealDescription = it },
                    label = { Text("Beschreibung") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }

            // Notes field
            item {
                OutlinedTextField(
                    value = mealNotes,
                    onValueChange = { mealNotes = it },
                    label = { Text("Notizen") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }

            // Ingredients section
            item {
                Text("Zutaten", style = MaterialTheme.typography.headlineSmall)
            }

            items(ingredients) { ingredient ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ingredient.pantryItemName, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${ingredient.requiredQuantity} ${ingredient.requiredUnit.label}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = {
                        ingredients = ingredients.filter { it.ingredientName != ingredient.ingredientName }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Zutat entfernen")
                    }
                }
            }

            // Add ingredient button
            item {
                Button(
                    onClick = { showIngredientPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Zutat hinzufügen")
                }
            }

            // Save button
            item {
                Button(
                    onClick = {
                        if (mealName.isNotBlank() && ingredients.isNotEmpty()) {
                            val meal = Meal(
                                id = mealId ?: 0,
                                name = mealName,
                                description = mealDescription,
                                category = selectedCategory,
                                notes = mealNotes
                            )
                            val mealIngredients = ingredients.map {
                                MealIngredient(
                                    mealId = meal.id,
                                    ingredientName = it.ingredientName,
                                    pantryItemId = it.pantryItemId,
                                    requiredQuantity = it.requiredQuantity,
                                    requiredUnit = it.requiredUnit
                                )
                            }
                            viewModel.saveMeal(meal, mealIngredients)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = mealName.isNotBlank() && ingredients.isNotEmpty()
                ) {
                    Text(if (mealId != null && mealId > 0) "Aktualisieren" else "Erstellen")
                }
            }
        }
    }
}
