package com.example.pantrypure.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pantrypure.data.model.PantryItem
import com.example.pantrypure.ui.viewmodel.FilterOption
import com.example.pantrypure.ui.viewmodel.PantryViewModel
import com.example.pantrypure.ui.viewmodel.SortOption
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryListScreen(
    viewModel: PantryViewModel,
    onAddItemClick: () -> Unit,
    onItemClick: (Long) -> Unit,
    onShoppingListClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onMealsClick: () -> Unit = {}
) {
    val items by viewModel.pantryItems.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val filterOption by viewModel.filterOption.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var itemToDelete by remember { mutableStateOf<PantryItem?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PantryPure") },
                actions = {
                    IconButton(onClick = onMealsClick) {
                        Icon(Icons.Default.Restaurant, contentDescription = "Mahlzeiten")
                    }
                    IconButton(onClick = onHistoryClick) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = onShoppingListClick) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Shopping List")
                    }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                    }
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text("Sort by ${option.name.lowercase().replace("_", " ")}") },
                                onClick = {
                                    viewModel.setSortOption(option)
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOption == option) Icon(Icons.Default.Check, contentDescription = null)
                                }
                            )
                        }
                    }
                    
                    DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                        FilterOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text("Filter: ${option.name.lowercase().replace("_", " ")}") },
                                onClick = {
                                    viewModel.setFilterOption(option)
                                    showFilterMenu = false
                                },
                                leadingIcon = {
                                    if (filterOption == option) Icon(Icons.Default.Check, contentDescription = null)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItemClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search items...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    PantryItemCard(
                        item = item,
                        onClick = { onItemClick(item.id) },
                        onDeleteClick = { itemToDelete = item },
                        onConsumeClick = { viewModel.consumeOne(item) },
                        onDuplicateClick = { viewModel.duplicateItem(item) }
                    )
                }
            }
        }
    }

    if (itemToDelete != null) {
        val item = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Item") },
            text = { Text("Are you sure you want to delete '${item.name}' from your pantry?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteItem(item)
                    itemToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PantryItemCard(
    item: PantryItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onConsumeClick: () -> Unit,
    onDuplicateClick: () -> Unit
) {
    val now = System.currentTimeMillis()
    val thresholdMillis = item.expiryThresholdDays * 24 * 60 * 60 * 1000L

    val status = when {
        item.expiryDate == null -> null
        item.expiryDate < now -> "OVERDUE"
        item.expiryDate <= (now + thresholdMillis) -> "EXPIRING SOON"
        else -> null
    }

    val statusColor = when (status) {
        "OVERDUE" -> Color.Red
        "EXPIRING SOON" -> Color(0xFFFFA000) // Dark Amber
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.quantity} ${item.unit.label} • ${item.category}",
                    style = MaterialTheme.typography.bodyMedium
                )
                item.expiryDate?.let { expiry ->
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val expiryString = sdf.format(Date(expiry))
                    Text(
                        text = "Expires: $expiryString",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status == "OVERDUE") Color.Red else Color.Unspecified
                    )
                }
                status?.let {
                    Surface(
                        color = statusColor.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDuplicateClick) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Duplicate")
            }
            TextButton(onClick = onConsumeClick) {
                Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Consume 1")
            }
        }
    }
}
