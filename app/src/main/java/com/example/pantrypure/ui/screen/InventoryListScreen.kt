package com.example.pantrypure.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pantrypure.data.model.ExpiryStatus
import com.example.pantrypure.data.model.PantryItem
import com.example.pantrypure.data.model.getExpiryStatus
import com.example.pantrypure.ui.viewmodel.FilterOption
import com.example.pantrypure.ui.viewmodel.PantryViewModel
import com.example.pantrypure.ui.viewmodel.SortOption
import kotlinx.coroutines.launch
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
    onMealsClick: () -> Unit = {},
    onPlannerClick: () -> Unit = {},
    onScannerClick: () -> Unit = {}
) {
    val items by viewModel.pantryItems.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val filterOption by viewModel.filterOption.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    var itemToDelete by remember { mutableStateOf<PantryItem?>(null) }

    val emptyStateMessage by remember {
        derivedStateOf {
            if (searchQuery.isEmpty()) "Your pantry is empty" else "No items found for '$searchQuery'"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PantryPure") },
                actions = {
                    IconButton(onClick = onScannerClick) { Icon(Icons.Default.QrCodeScanner, "Bon scannen") }
                    IconButton(onClick = onPlannerClick) { Icon(Icons.Default.CalendarMonth, "Meal Planner") }
                    IconButton(onClick = onMealsClick) { Icon(Icons.Default.Restaurant, "Mahlzeiten") }
                    IconButton(onClick = onHistoryClick) { Icon(Icons.Default.History, "History") }
                    IconButton(onClick = onShoppingListClick) { Icon(Icons.Default.ShoppingCart, "Shopping List") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItemClick) { Icon(Icons.Default.Add, "Add Item") }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // 1. Suche und Filter in eigene Funktion
            SearchAndFilterSection(
                searchQuery = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                sortOption = sortOption,
                onSortChange = { viewModel.setSortOption(it) },
                filterOption = filterOption,
                onFilterChange = { viewModel.setFilterOption(it) }
            )

            // 2. Liste in eigene Funktion
            PantryListContent(
                items = items,
                emptyMessage = emptyStateMessage,
                onItemClick = onItemClick,
                onDeleteRequest = { itemToDelete = it },
                onConsume = { viewModel.consumeOne(it) },
                onDuplicate = { viewModel.duplicateItem(it) }
            )
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
                    scope.launch {
                        viewModel.deleteItem(item)
                        itemToDelete = null
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { itemToDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun SearchAndFilterSection(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    sortOption: SortOption,
    onSortChange: (SortOption) -> Unit,
    filterOption: FilterOption,
    onFilterChange: (FilterOption) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search items...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, "Clear")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            item {
                FilterChip(
                    selected = true,
                    onClick = { showSortMenu = true },
                    label = { Text("Sort: ${sortOption.name.lowercase().replace("_", " ")}") },
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Filled.Sort,
                            null,
                            Modifier.size(18.dp)
                        )
                    }
                )
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.name.lowercase().replace("_", " ")) },
                            onClick = { onSortChange(option); showSortMenu = false }
                        )
                    }
                }
            }
            item {
                FilterChip(
                    selected = filterOption != FilterOption.ALL,
                    onClick = { showFilterMenu = true },
                    label = { Text("Filter: ${filterOption.name.lowercase().replace("_", " ")}") },
                    leadingIcon = { Icon(Icons.Default.FilterList, null, Modifier.size(18.dp)) }
                )
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false }) {
                    FilterOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.name.lowercase().replace("_", " ")) },
                            onClick = { onFilterChange(option); showFilterMenu = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PantryListContent(
    items: List<PantryItem>,
    emptyMessage: String,
    onItemClick: (Long) -> Unit,
    onDeleteRequest: (PantryItem) -> Unit,
    onConsume: (PantryItem) -> Unit,
    onDuplicate: (PantryItem) -> Unit
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = emptyMessage, style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.id }) { item ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart) {
                            onDeleteRequest(item)
                            false // Warte auf Dialog-Bestätigung
                        } else false
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        val color =
                            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                                MaterialTheme.colorScheme.errorContainer else Color.Transparent
                        Box(
                            Modifier.fillMaxSize().padding(8.dp).padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    enableDismissFromStartToEnd = false
                ) {
                    PantryItemCard(
                        item = item,
                        onClick = { onItemClick(item.id) },
                        onDeleteClick = { onDeleteRequest(item) },
                        onConsumeClick = { onConsume(item) },
                        onDuplicateClick = { onDuplicate(item) }
                    )
                }
            }
        }
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
    val status = remember(item) { item.getExpiryStatus() }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val cardContainerColor = when (status) {
        ExpiryStatus.OVERDUE -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
        ExpiryStatus.EXPIRING_SOON -> Color(0xFFFFA000).copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surfaceVariant   // Standardfarbe
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        border = if (status != ExpiryStatus.NORMAL) BorderStroke(1.dp, cardContainerColor.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = "${item.quantity} ${item.unit.label} • ${item.category} • ${item.location}",
                    style = MaterialTheme.typography.bodyMedium
                )
                item.expiryDate?.let { expiry ->
                    val expiryString = dateFormatter.format(Date(expiry))
                    Text(text = "Expires: $expiryString", style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDuplicateClick) {
                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Duplicate")
            }
            TextButton(onClick = onConsumeClick) {
                Icon(Icons.Default.RemoveCircleOutline, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Consume 1")
            }
        }
    }
}
