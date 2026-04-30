package com.example.pantrypure.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pantrypure.data.model.Meal
import com.example.pantrypure.data.model.MealCategory
import com.example.pantrypure.ui.viewmodel.PantryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealsListScreen(
    viewModel: PantryViewModel,
    onAddMealClick: () -> Unit,
    onMealClick: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    val mealsByCategory by viewModel.mealsByCategory.collectAsState()
    val selectedCategory by viewModel.selectedMealCategory.collectAsState()
    var mealToDelete by remember { mutableStateOf<Meal?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mahlzeiten") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMealClick) {
                Icon(Icons.Default.Add, contentDescription = "Neue Mahlzeit")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Category Tabs
            ScrollableTabRow(
                selectedTabIndex = MealCategory.entries.indexOf(selectedCategory),
                modifier = Modifier.fillMaxWidth()
            ) {
                MealCategory.entries.forEach { category ->
                    Tab(
                        selected = category == selectedCategory,
                        onClick = { viewModel.setMealCategory(category) },
                        text = { Text(category.label, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            // Meals List
            if (mealsByCategory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("Keine Mahlzeiten in dieser Kategorie", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(mealsByCategory) { mealWithIngredients ->
                        MealListItem(
                            meal = mealWithIngredients.meal,
                            ingredientCount = mealWithIngredients.ingredients.size,
                            onMealClick = { onMealClick(mealWithIngredients.meal.id) },
                            onDeleteClick = { mealToDelete = mealWithIngredients.meal }
                        )
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (mealToDelete != null) {
            AlertDialog(
                onDismissRequest = { mealToDelete = null },
                title = { Text("Mahlzeit löschen?") },
                text = { Text("Möchten Sie die Mahlzeit \"${mealToDelete?.name}\" wirklich löschen?") },
                confirmButton = {
                    Button(
                        onClick = {
                            mealToDelete?.let { viewModel.deleteMeal(it) }
                            mealToDelete = null
                        }
                    ) {
                        Text("Löschen")
                    }
                },
                dismissButton = {
                    Button(onClick = { mealToDelete = null }) {
                        Text("Abbrechen")
                    }
                }
            )
        }
    }
}

@Composable
private fun MealListItem(
    meal: Meal,
    ingredientCount: Int,
    onMealClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onMealClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(meal.name, style = MaterialTheme.typography.headlineSmall)
                Text("$ingredientCount Zutaten", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Löschen")
            }
        }
    }
}
