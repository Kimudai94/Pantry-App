# Features & API Reference

Detaillierte Dokumentation aller App-Features, Screens, ViewModels und deren API.

---

## 1. Feature Inventory (Master List)

### Core Features (MVP)

| Feature | Status | Location | Model |
|---------|--------|----------|-------|
| Pantry Item CRUD | ✅ Complete | `InventoryListScreen`, `AddEditItemScreen` | `PantryItem` |
| Consumption Tracking | ✅ Complete | `HistoryScreen` | `ConsumptionRecord` |
| Meal Management | ✅ Complete | `MealsListScreen`, `AddEditMealScreen` | `Meal`, `MealIngredient` |
| Shopping List | ✅ Complete | `ShoppingListScreen` | `PantryItem.isOnShoppingList` |
| Expiry Alerts | ✅ Complete | `ExpiryCheckWorker` | Notifications |
| Search & Filter | ✅ Complete | `InventoryListScreen` | StateFlow filtering |
| Sorting | ✅ Complete | `InventoryListScreen` | Enum: `SortOption` |

### Extended Features (Future)

- Barcode scanning
- Multi-location pantries
- Offline sync
- Cloud backup
- Recipe suggestions based on inventory

---

## 2. Screens & Navigation

### Navigation Map

```
┌─────────────────────────────────────────────────┐
│                   PantryPureApp                 │
│                 (NavHostController)             │
└──────────────┬──────────────────────────────────┘
               │
        ┌──────┴────────┬────────────┬──────────┐
        │               │            │          │
        ▼               ▼            ▼          ▼
   ┌─────────┐  ┌────────────┐  ┌────────┐  ┌────────┐
   │Inventory│  │   Meals    │  │Shopping│  │History │
   │  List   │  │   List     │  │  List  │  │        │
   └────┬────┘  └──────┬─────┘  └────────┘  └────────┘
        │               │
        ├──────────┬────┤
        │          │    │
        ▼          ▼    ▼
   ┌─────────┐ ┌────────┐ ┌──────────────┐
   │Add/Edit │ │ Meal   │ │  Ingredient  │
   │  Item   │ │Details │ │   Picker     │
   └─────────┘ └────────┘ └──────────────┘
```

### Screen Details

#### InventoryListScreen
- **Route**: `inventory_list` (Start Destination)
- **Purpose**: Browse all pantry items, search, filter, sort
- **ViewModel**: `PantryViewModel`
- **State Inputs**:
  - `pantryItems: StateFlow<List<PantryItem>>` – Filtered & sorted items
  - `sortOption: StateFlow<SortOption>` – Current sort mode
  - `filterOption: StateFlow<FilterOption>` – Current filter mode
  - `searchQuery: StateFlow<String>` – Search input
- **UI Elements**:
  - SearchBar (onChange → `setSearchQuery()`)
  - FilterDropdown (options: ALL, OVERDUE, EXPIRING_SOON)
  - SortDropdown (options: NAME, EXPIRY_DATE)
  - LazyColumn of `PantryItemRow`s
  - FAB to navigate to `AddEditItemScreen`
- **Actions**:
  - Tap Item → Navigate to `AddEditItemScreen` with itemId
  - "Consume" button → `consumeOne(item)`
  - Swipe/Delete → `deleteItem(item)`
  - Long press → show context menu (duplicate, edit, delete)

#### AddEditItemScreen
- **Route**: `add_edit_item/{itemId}` (itemId = 0 for new item)
- **Purpose**: Create or edit a pantry item
- **ViewModel**: `PantryViewModel`
- **Mode Detection**:
  ```kotlin
  val itemId = arguments?.getLong("itemId") ?: 0
  if (itemId > 0) {
      // Edit mode: load existing item
      val item = viewModel.getItemById(itemId)
  } else {
      // Create mode: start with empty form
      val item = PantryItem(name = "", ...)
  }
  ```
- **Form Fields**:
  - Name (TextField)
  - Quantity (TextField, type = Decimal)
  - Unit (Dropdown: PIECE, GRAM, KILOGRAM, etc.)
  - Expiry Date (DatePicker → Long timestamp)
  - Expiry Threshold Days (TextField)
  - Category (TextField or Dropdown)
  - Notes (TextField, multiline)
  - On Shopping List (Toggle)
- **Actions**:
  - Save → `viewModel.addItem()` or `updateItem()` → back to InventoryListScreen
  - Cancel → back without saving

#### MealsListScreen
- **Route**: `meals_list`
- **Purpose**: Browse all recipes, filter by category
- **ViewModel**: `PantryViewModel`
- **State Inputs**:
  - `allMeals: StateFlow<List<Meal>>`
  - `selectedMealCategory: StateFlow<MealCategory>`
- **UI Elements**:
  - CategoryTabs (BREAKFAST, LUNCH, DINNER, SNACK, OTHER)
  - LazyColumn of `MealRow`s
  - FAB to `AddEditMealScreen`
- **Actions**:
  - Tap Meal → Navigate to `MealDetailScreen`
  - Swipe/Delete → `deleteMeal(meal)`

#### MealDetailScreen
- **Route**: `meal_detail/{mealId}`
- **Purpose**: View meal recipe and ingredients, check if can be prepared
- **ViewModel**: `PantryViewModel`
- **Load Data**:
  ```kotlin
  val mealWithIngredients = viewModel.getMealWithIngredients(mealId)
  ```
- **Display**:
  - Meal name & category
  - Instructions (scrollable text)
  - Ingredients list (with required quantity, available quantity)
  - "Can Prepare" indicator (all ingredients available? ✓ or ✗)
  - Buttons: Edit, Delete, Add to Shopping List (missing ingredients)
- **Actions**:
  - Edit → Navigate to `AddEditMealScreen` with mealId
  - Delete → `deleteMeal(meal)`
  - "Add Missing to Shopping List" → Mark pantry items as `isOnShoppingList=true`

#### AddEditMealScreen
- **Route**: `add_edit_meal/{mealId}`
- **Purpose**: Create or edit meal recipe
- **Form Fields**:
  - Name (TextField)
  - Category (Dropdown: MealCategory enum)
  - Instructions (TextField, multiline)
  - Add Ingredients → Navigate to `MealIngredientPickerScreen`
  - Ingredients List (editable, delete button per ingredient)
- **Actions**:
  - Add Ingredient → Shows `MealIngredientPickerScreen`
  - Save → `addMeal()` or `updateMeal()`
  - Remove Ingredient → `removeIngredientFromMeal()`

#### MealIngredientPickerScreen
- **Route**: `meal_ingredient_picker/{mealId}`
- **Purpose**: Select pantry items to add as meal ingredients
- **Display**:
  - SearchBar to filter pantry items
  - LazyColumn of pantry items (with quantity input field)
  - "Add Selected" button
- **Actions**:
  - Select item + enter quantity → `addIngredientToMeal(MealIngredient)`
  - Back without adding → discard selections

#### ShoppingListScreen
- **Route**: `shopping_list`
- **Purpose**: View and manage shopping list
- **ViewModel**: `PantryViewModel`
- **State Input**:
  - `shoppingListItems: StateFlow<List<PantryItem>>`
- **UI Elements**:
  - LazyColumn of items with `isOnShoppingList=true`
  - Checkbox per item (toggle on/off)
  - SwipeToDelete per item
  - "Clear Purchased" button
- **Actions**:
  - Toggle checkbox → `toggleShoppingListStatus(item)`
  - Swipe delete → Remove from shopping list

#### HistoryScreen
- **Route**: `history`
- **Purpose**: View consumption history (time-series)
- **ViewModel**: `PantryViewModel`
- **State Input**:
  - `consumptionHistory: StateFlow<List<ConsumptionRecord>>`
- **Display**:
  - LazyColumn of records (newest first)
  - Each row: `${itemName}: ${quantityConsumed}${unit} - ${timestamp}`
  - Summary stats (total items consumed this week/month)
- **Actions**:
  - "Clear All" → `clearHistory()` (with confirmation)
  - Tap record → Show detail modal

---

## 3. ViewModel API Reference

### PantryViewModel

Location: `ui/viewmodel/PantryViewModel.kt`

#### State (Immutable Outputs)

```kotlin
// Pantry Items with applied filters & sort
val pantryItems: StateFlow<List<PantryItem>>

// Consumption history
val consumptionHistory: StateFlow<List<ConsumptionRecord>>

// Shopping list items
val shoppingListItems: StateFlow<List<PantryItem>>

// Meals
val allMeals: StateFlow<List<Meal>>
val mealsInCategory: StateFlow<List<Meal>>

// Filter/Sort/Search controls
val sortOption: StateFlow<SortOption>
val filterOption: StateFlow<FilterOption>
val searchQuery: StateFlow<String>
val selectedMealCategory: StateFlow<MealCategory>
```

#### Pantry Item Operations

```kotlin
/**
 * Add new pantry item to database.
 * Launches in viewModelScope.
 */
fun addItem(item: PantryItem)

/**
 * Update existing pantry item.
 */
fun updateItem(item: PantryItem)

/**
 * Delete pantry item.
 */
fun deleteItem(item: PantryItem)

/**
 * Fetch single item by ID (suspend function).
 */
suspend fun getItemById(id: Long): PantryItem?

/**
 * Duplicate item with "(Copy)" suffix.
 */
fun duplicateItem(item: PantryItem)

/**
 * Reduce quantity by 1.0 and record consumption.
 */
fun consumeOne(item: PantryItem)
```

#### Filter/Sort/Search Controls

```kotlin
/**
 * Set sort option: NAME or EXPIRY_DATE.
 * Triggers pantryItems StateFlow re-emit.
 */
fun setSortOption(option: SortOption)

/**
 * Set filter option: ALL, OVERDUE, or EXPIRING_SOON.
 * Filters are applied at ViewModel level.
 */
fun setFilterOption(option: FilterOption)

/**
 * Set search query string (case-insensitive name matching).
 */
fun setSearchQuery(query: String)
```

#### Shopping List Operations

```kotlin
/**
 * Toggle item on/off shopping list.
 * Updates isOnShoppingList flag.
 */
fun toggleShoppingListStatus(item: PantryItem)
```

#### Consumption History

```kotlin
/**
 * Clear all consumption records from database.
 */
fun clearHistory()
```

#### Meal Operations

```kotlin
/**
 * Fetch all meals (or filtered by category).
 */
fun getAllMeals()

/**
 * Fetch single meal with ingredients.
 */
suspend fun getMealWithIngredients(id: Long): MealWithIngredients?

/**
 * Add new meal.
 */
fun addMeal(meal: Meal): Long

/**
 * Update existing meal.
 */
fun updateMeal(meal: Meal)

/**
 * Delete meal.
 */
fun deleteMeal(meal: Meal)

/**
 * Add ingredient to meal.
 */
fun addIngredientToMeal(ingredient: MealIngredient): Long

/**
 * Remove ingredient from meal.
 */
fun removeIngredientFromMeal(ingredient: MealIngredient)
```

#### Category Selection

```kotlin
/**
 * Set selected meal category (for MealsListScreen filtering).
 */
fun setSelectedMealCategory(category: MealCategory)
```

### PantryViewModelFactory

Location: `ui/viewmodel/PantryViewModelFactory.kt`

```kotlin
class PantryViewModelFactory(
    private val repository: PantryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PantryViewModel(repository) as T
    }
}
```

**Usage in MainActivity**:
```kotlin
private val viewModel: PantryViewModel by viewModels {
    PantryViewModelFactory((application as PantryPureApplication).repository)
}
```

---

## 4. Repository API Reference

Location: `data/repository/PantryRepository.kt`

### Pantry Item Operations

```kotlin
fun getAllItems(): Flow<List<PantryItem>>
suspend fun getItemById(id: Long): PantryItem?
suspend fun insertItem(item: PantryItem)
suspend fun updateItem(item: PantryItem)
suspend fun deleteItem(item: PantryItem)
fun getItemsByCategory(category: String): Flow<List<PantryItem>>
```

### Shopping List

```kotlin
fun getShoppingListItems(): Flow<List<PantryItem>>
suspend fun updateShoppingListStatus(id: Long, isOnList: Boolean)
```

### Consumption History

```kotlin
fun getConsumptionHistory(): Flow<List<ConsumptionRecord>>
suspend fun insertConsumptionRecord(record: ConsumptionRecord)
suspend fun clearHistory()
```

### Meals

```kotlin
fun getAllMeals(): Flow<List<Meal>>
fun getMealsByCategory(category: MealCategory): Flow<List<Meal>>
suspend fun getMealById(id: Long): Meal?
suspend fun insertMeal(meal: Meal): Long
suspend fun updateMeal(meal: Meal)
suspend fun deleteMeal(meal: Meal)
suspend fun getMealWithIngredients(id: Long): MealWithIngredients?
fun getAllMealsWithIngredients(): Flow<List<MealWithIngredients>>
```

### Meal Ingredients

```kotlin
suspend fun addIngredientToMeal(ingredient: MealIngredient): Long
suspend fun removeIngredientFromMeal(ingredient: MealIngredient)
suspend fun getIngredientsForMeal(mealId: Long): List<MealIngredient>
```

---

## 5. Data Models & Enums

### PantryItem
```kotlin
@Entity(tableName = "pantry_items")
data class PantryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: Double,
    val unit: PantryUnit,
    val expiryDate: Long?,
    val expiryThresholdDays: Int = 3,
    val isOnShoppingList: Boolean = false,
    val category: String,
    val notes: String = ""
)
```

### ConsumptionRecord
```kotlin
@Entity(tableName = "consumption_records")
data class ConsumptionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val itemName: String,
    val quantityConsumed: Double,
    val unit: PantryUnit,
    val timestamp: Long = System.currentTimeMillis()
)
```

### Meal
```kotlin
@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: MealCategory,
    val instructions: String
)

enum class MealCategory {
    BREAKFAST, LUNCH, DINNER, SNACK, OTHER
}
```

### MealIngredient
```kotlin
@Entity(tableName = "meal_ingredients")
data class MealIngredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,
    val pantryItemId: Long,
    val requiredQuantity: Double
)

data class MealWithIngredients(
    val meal: Meal,
    val ingredients: List<MealIngredientWithName>
)

data class MealIngredientWithName(
    val id: Long,
    val mealId: Long,
    val pantryItemId: Long,
    val requiredQuantity: Double,
    val itemName: String
)
```

### PantryUnit
```kotlin
enum class PantryUnit {
    PIECE, GRAM, KILOGRAM, MILLILITER, LITER, TABLESPOON, TEASPOON, CUP
}
```

### SortOption & FilterOption (ViewModel)
```kotlin
enum class SortOption { NAME, EXPIRY_DATE }
enum class FilterOption { ALL, OVERDUE, EXPIRING_SOON }
```

---

## 6. Background Jobs

### ExpiryCheckWorker

Location: `worker/ExpiryCheckWorker.kt`

**Purpose**: Daily background job to check for expired items and send notifications.

**Schedule**:
- Triggers: Once per day
- Constraints: Battery not low
- Policy: Unique (no duplicates)

**Implementation**:
```kotlin
class ExpiryCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        // 1. Get all pantry items
        val items = repository.getAllItems().first()
        
        // 2. Filter expired items
        val now = System.currentTimeMillis()
        val expiredItems = items.filter { item ->
            item.expiryDate != null && item.expiryDate < now
        }
        
        // 3. Send notification if any
        if (expiredItems.isNotEmpty()) {
            NotificationHelper(applicationContext).notifyExpiredItems(expiredItems)
        }
        
        return Result.success()
    }
}
```

**Scheduling** (in `PantryPureApplication.onCreate()`):
```kotlin
val expiryCheckRequest = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(
    1, TimeUnit.DAYS
).setConstraints(
    Constraints.Builder()
        .setRequiresBatteryNotLow(true)
        .build()
).build()

WorkManager.getInstance(this).enqueueUniquePeriodicWork(
    "ExpiryCheckWork",
    ExistingPeriodicWorkPolicy.KEEP,
    expiryCheckRequest
)
```

---

## 7. Notifications

Location: `util/NotificationHelper.kt`

### Notification Channel Setup
```kotlin
fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "expiry_alerts",
            "Ablauf-Benachrichtigungen",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }
}
```

### Send Expiry Alert
```kotlin
fun notifyExpiredItems(items: List<PantryItem>) {
    val notification = NotificationCompat.Builder(context, "expiry_alerts")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Items abgelaufen!")
        .setContentText("${items.size} Items in der Speisekammer abgelaufen")
        .setAutoCancel(true)
        .build()
    
    notificationManager.notify(1, notification)
}
```

---

## 8. Utility Functions

### UnitConverter
Location: `data/util/UnitConverter.kt`

```kotlin
object UnitConverter {
    /**
     * Convert quantity from one unit to another.
     * Example: 1000 GRAM → 1 KILOGRAM
     */
    fun convertQuantity(
        quantity: Double,
        fromUnit: PantryUnit,
        toUnit: PantryUnit
    ): Double
}
```

---

## 9. Permission Model

### Required Permissions
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" /> <!-- API 33+ -->
<uses-permission android:name="android.permission.CAMERA" /> <!-- For barcode scanning (future) -->
```

### Runtime Permission Handling
```kotlin
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionEffect() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberPermissionState(
            Manifest.permission.POST_NOTIFICATIONS
        )
        LaunchedEffect(Unit) {
            if (!permissionState.status.isGranted) {
                permissionState.launchPermissionRequest()
            }
        }
    }
}
```

---

## 10. Common Use Cases

### Use Case: Search and Filter Items

```kotlin
// In InventoryListScreen
TextField(
    value = searchQuery,
    onValueChange = { viewModel.setSearchQuery(it) },
    label = { Text("Search") }
)

Dropdown(
    selected = filterOption,
    onSelect = { viewModel.setFilterOption(it) },
    items = listOf(FilterOption.ALL, FilterOption.OVERDUE, FilterOption.EXPIRING_SOON)
)

// ViewModel automatically re-filters & re-sorts
val items by viewModel.pantryItems.collectAsState()
```

**Result**: Items list updates reactively as user types/filters.

### Use Case: Add Item to Pantry

1. User taps FAB on InventoryListScreen
2. Navigate to `AddEditItemScreen` (itemId = 0)
3. User fills form, taps "Save"
4. Call `viewModel.addItem(item)`
5. ViewModel launches coroutine → `repository.insertItem(item)`
6. Room inserts into database
7. Flow re-emits updated list
8. InventoryListScreen recomposes with new item
9. Auto-navigate back (or show toast)

### Use Case: Create Meal & Add Ingredients

1. User taps FAB on MealsListScreen
2. Navigate to `AddEditMealScreen` (mealId = 0)
3. Fill name, category, instructions
4. Tap "Add Ingredient"
5. Navigate to `MealIngredientPickerScreen`
6. User searches, selects items, enters quantities
7. Tap "Add Selected"
8. Back to AddEditMealScreen with ingredients list populated
9. Tap "Save Meal"
10. ViewModel calls `insertMeal()` → returns mealId
11. For each ingredient, call `addIngredientToMeal()`
12. All saved to database

---

## 11. Error Handling

### Error Patterns

**No custom exceptions** – Use meaningful error messages:

```kotlin
// In DAO
@Query("SELECT * FROM pantry_items WHERE id = :id")
suspend fun getItemById(id: Long): PantryItem?

// In ViewModel
if (item == null) {
    // Handle gracefully – item not found
    Log.e("PantryViewModel", "Item $id not found in database")
    return@LaunchedEffect
}
```

### Coroutine Error Handling

```kotlin
fun addItem(item: PantryItem) {
    viewModelScope.launch {
        try {
            repository.insertItem(item)
        } catch (e: Exception) {
            Log.e("PantryViewModel", "Failed to add item: ${e.message}")
            // Emit error state (if needed)
        }
    }
}
```

---

## 12. Future API Extensions

Placeholder for planned features:

```kotlin
// Barcode Scanner (Future)
suspend fun recognizeItemFromBarcode(barcodeData: String): PantryItem?

// Recipe Suggestions (Future)
fun getSuggestedRecipes(availableItems: List<Long>): List<Meal>

// Multi-Pantry Support (Future)
data class Pantry(val id: Long, val name: String)
fun switchPantry(pantryId: Long)

// Cloud Sync (Future)
suspend fun syncToCloud(): Boolean
suspend fun restoreFromCloud(): Boolean
```

---

**Key Takeaway for LLMs**: All features flow through the ViewModel → Repository → DAO → Database pattern. Modify data at the Model layer, business logic in Repository, UI state in ViewModel.
