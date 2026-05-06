# Architecture & Code Structure

Tiefgehende Dokumentation der PantryPure-Architektur für LLM-gesteuerte Entwicklung.

---

## 1. Übersicht: Layered Architecture

PantryPure folgt der **Modern Android Architecture** mit klaren Schichten:

```
┌─────────────────────────────────────────────────────┐
│              Presentation Layer (UI)                │
│  ├─ Composable Screens (Add/Edit/Detail/List)       │
│  ├─ ViewModels (State Management)                   │
│  └─ Navigation (Compose NavHost)                    │
├─────────────────────────────────────────────────────┤
│              Domain Layer (Business Logic)          │
│  ├─ Use Cases / Repositories                        │
│  ├─ Model Transformations                           │
│  └─ Validation & Rules                              │
├─────────────────────────────────────────────────────┤
│              Data Layer (Persistence)               │
│  ├─ Room Entities (@Entity)                         │
│  ├─ DAOs (@Dao)                                     │
│  ├─ Database (@Database)                            │
│  ├─ Type Converters                                 │
│  └─ Migrations                                      │
└─────────────────────────────────────────────────────┘
```

---

## 2. Data Layer (data/)

### 2.1 Entity Modelle

**PantryItem.kt** – Hauptentität für Speisekammer-Gegenstände
```kotlin
@Entity(tableName = "pantry_items")
data class PantryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                    // Display name
    val quantity: Double,                // Aktuell vorhandene Menge
    val unit: PantryUnit,                // Enum: PIECE, GRAM, LITER, etc.
    val expiryDate: Long?,               // Milliseconds timestamp (nullable)
    val expiryThresholdDays: Int = 3,    // Warnung bei N Tagen vor Ablauf
    val isOnShoppingList: Boolean = false, // Shopping-List-Marker
    val category: String,                // "Obst", "Gemüse", "Fleisch", etc.
    val notes: String = ""               // Optional: Lagerort, Notizen
)
```

**ConsumptionRecord.kt** – Verbrauchshistorie
```kotlin
@Entity(tableName = "consumption_records")
data class ConsumptionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,                    // FK zu PantryItem
    val itemName: String,                // Snapshot des Item-Namens (zur Verlauf-Lesbarkeit)
    val quantityConsumed: Double,        // Verbrauchte Menge
    val unit: PantryUnit,                // Einheit
    val timestamp: Long = System.currentTimeMillis() // Wann konsumiert?
)
```

**Meal.kt** – Rezept-Definition
```kotlin
@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                    // "Spaghetti Carbonara", etc.
    val category: MealCategory,          // BREAKFAST, LUNCH, DINNER, SNACK, OTHER
    val instructions: String             // "1. Wasser kochen...\n2. Nudeln..."
)

enum class MealCategory {
    BREAKFAST, LUNCH, DINNER, SNACK, OTHER
}
```

**MealIngredient.kt** – N:M Zuordnung Meals ↔ PantryItems
```kotlin
@Entity(
    tableName = "meal_ingredients",
    foreignKeys = [
        ForeignKey(entity = Meal::class, parentColumns = ["id"], childColumns = ["mealId"]),
        ForeignKey(entity = PantryItem::class, parentColumns = ["id"], childColumns = ["pantryItemId"])
    ]
)
data class MealIngredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,                    // Welches Rezept?
    val pantryItemId: Long,              // Welcher Pantry-Gegenstand?
    val requiredQuantity: Double         // Wie viel benötigt? (z.B. 500g)
)

// Companion Klasse für Queries
data class MealWithIngredients(
    val meal: Meal,
    val ingredients: List<MealIngredientWithName> // Ingredient + ItemName
)

data class MealIngredientWithName(
    val id: Long,
    val mealId: Long,
    val pantryItemId: Long,
    val requiredQuantity: Double,
    val itemName: String  // Für UI-Anzeige
)
```

**PantryUnit.kt** – Einheits-Enum
```kotlin
enum class PantryUnit {
    PIECE,        // Stück
    GRAM,         // g
    KILOGRAM,     // kg
    MILLILITER,   // ml
    LITER,        // l
    TABLESPOON,   // EL
    TEASPOON,     // TL
    CUP           // Tasse
}
```

### 2.2 DAOs (Data Access Objects)

**PantryDao.kt** – Speisekammer-Queries
```kotlin
@Dao
interface PantryDao {
    @Insert
    suspend fun insertItem(item: PantryItem)
    
    @Update
    suspend fun updateItem(item: PantryItem)
    
    @Delete
    suspend fun deleteItem(item: PantryItem)
    
    @Query("SELECT * FROM pantry_items ORDER BY name ASC")
    fun getAllItems(): Flow<List<PantryItem>>
    
    @Query("SELECT * FROM pantry_items WHERE id = :id")
    suspend fun getItemById(id: Long): PantryItem?
    
    @Query("SELECT * FROM pantry_items WHERE category = :category")
    fun getItemsByCategory(category: String): Flow<List<PantryItem>>
    
    @Query("SELECT * FROM pantry_items WHERE isOnShoppingList = 1")
    fun getShoppingListItems(): Flow<List<PantryItem>>
    
    @Query("UPDATE pantry_items SET isOnShoppingList = :isOnList WHERE id = :id")
    suspend fun updateShoppingListStatus(id: Long, isOnList: Boolean)
}
```

**ConsumptionDao.kt** – Historien-Queries
```kotlin
@Dao
interface ConsumptionDao {
    @Insert
    suspend fun insertRecord(record: ConsumptionRecord)
    
    @Query("SELECT * FROM consumption_records ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ConsumptionRecord>>
    
    @Query("DELETE FROM consumption_records")
    suspend fun clearHistory()
}
```

**MealDao.kt** – Mahlzeiten + Zutaten-Queries
```kotlin
@Dao
interface MealDao {
    @Insert
    suspend fun insertMeal(meal: Meal): Long
    
    @Update
    suspend fun updateMeal(meal: Meal)
    
    @Delete
    suspend fun deleteMeal(meal: Meal)
    
    @Query("SELECT * FROM meals")
    fun getAllMeals(): Flow<List<Meal>>
    
    @Query("SELECT * FROM meals WHERE category = :category")
    fun getMealsByCategory(category: MealCategory): Flow<List<Meal>>
    
    @Query("""
        SELECT m.*, 
               GROUP_CONCAT(mi.id) as ingredientIds
        FROM meals m
        LEFT JOIN meal_ingredients mi ON m.id = mi.mealId
        WHERE m.id = :id
        GROUP BY m.id
    """)
    suspend fun getMealByIdForDetail(id: Long): Meal?
    
    @Query("""
        SELECT mi.id, mi.mealId, mi.pantryItemId, mi.requiredQuantity,
               p.name as itemName
        FROM meal_ingredients mi
        LEFT JOIN pantry_items p ON mi.pantryItemId = p.id
        WHERE mi.mealId = :mealId
    """)
    suspend fun getMealIngredientsWithNames(mealId: Long): List<MealIngredientWithName>
    
    @Query("SELECT * FROM meals")
    suspend fun getAllMealsRaw(): List<Meal>
    
    @Query("""
        SELECT mi.id, mi.mealId, mi.pantryItemId, mi.requiredQuantity,
               p.name as itemName
        FROM meal_ingredients mi
        LEFT JOIN pantry_items p ON mi.pantryItemId = p.id
    """)
    suspend fun getAllMealIngredientsWithNames(): List<MealIngredientWithName>
}
```

**MealIngredientDao.kt** – Zutaten-Verwaltung
```kotlin
@Dao
interface MealIngredientDao {
    @Insert
    suspend fun insertIngredient(ingredient: MealIngredient): Long
    
    @Delete
    suspend fun deleteIngredient(ingredient: MealIngredient)
    
    @Query("SELECT * FROM meal_ingredients WHERE mealId = :mealId")
    suspend fun getIngredientsForMeal(mealId: Long): List<MealIngredient>
}
```

### 2.3 Database & Type Converters

**PantryDatabase.kt**
```kotlin
@Database(
    entities = [
        PantryItem::class,
        ConsumptionRecord::class,
        Meal::class,
        MealIngredient::class
    ],
    version = 5,  // Erhöhe bei Schema-Änderungen
    exportSchema = false
)
@TypeConverters(PantryTypeConverters::class)
abstract class PantryDatabase : RoomDatabase() {
    abstract fun pantryDao(): PantryDao
    abstract fun consumptionDao(): ConsumptionDao
    abstract fun mealDao(): MealDao
    abstract fun mealIngredientDao(): MealIngredientDao
}
```

**PantryTypeConverters.kt** – Custom Serialisierung für Enums
```kotlin
class PantryTypeConverters {
    @TypeConverter
    fun pantryUnitToString(unit: PantryUnit?): String? = unit?.name
    
    @TypeConverter
    fun stringToPantryUnit(value: String?): PantryUnit? = 
        value?.let { PantryUnit.valueOf(it) }
    
    @TypeConverter
    fun mealCategoryToString(category: MealCategory?): String? = 
        category?.name
    
    @TypeConverter
    fun stringToMealCategory(value: String?): MealCategory? = 
        value?.let { MealCategory.valueOf(it) }
}
```

### 2.4 Repository Pattern

**PantryRepository.kt** – Single Source of Truth für Business Logic
```kotlin
class PantryRepository(
    private val pantryDao: PantryDao,
    private val consumptionDao: ConsumptionDao,
    private val mealDao: MealDao,
    private val mealIngredientDao: MealIngredientDao
) {
    // ========== PANTRY ITEMS ==========
    fun getAllItems(): Flow<List<PantryItem>> = pantryDao.getAllItems()
    
    suspend fun getItemById(id: Long): PantryItem? = pantryDao.getItemById(id)
    
    suspend fun insertItem(item: PantryItem) = pantryDao.insertItem(item)
    
    suspend fun updateItem(item: PantryItem) = pantryDao.updateItem(item)
    
    suspend fun deleteItem(item: PantryItem) = pantryDao.deleteItem(item)
    
    // ========== SHOPPING LIST ==========
    fun getShoppingListItems(): Flow<List<PantryItem>> = 
        pantryDao.getShoppingListItems()
    
    suspend fun updateShoppingListStatus(id: Long, isOnList: Boolean) = 
        pantryDao.updateShoppingListStatus(id, isOnList)
    
    // ========== CONSUMPTION HISTORY ==========
    fun getConsumptionHistory(): Flow<List<ConsumptionRecord>> = 
        consumptionDao.getAllHistory()
    
    suspend fun insertConsumptionRecord(record: ConsumptionRecord) = 
        consumptionDao.insertRecord(record)
    
    suspend fun clearHistory() = consumptionDao.clearHistory()
    
    // ========== MEALS ==========
    fun getAllMeals(): Flow<List<Meal>> = mealDao.getAllMeals()
    
    suspend fun getMealById(id: Long): Meal? = mealDao.getMealById(id)
    
    suspend fun insertMeal(meal: Meal): Long = mealDao.insertMeal(meal)
    
    suspend fun updateMeal(meal: Meal) = mealDao.updateMeal(meal)
    
    suspend fun deleteMeal(meal: Meal) = mealDao.deleteMeal(meal)
    
    suspend fun getMealWithIngredients(id: Long): MealWithIngredients? {
        val meal = mealDao.getMealByIdForDetail(id) ?: return null
        val ingredients = mealDao.getMealIngredientsWithNames(id)
        return MealWithIngredients(meal, ingredients)
    }
    
    // ========== MEAL INGREDIENTS ==========
    suspend fun addIngredientToMeal(ingredient: MealIngredient): Long = 
        mealIngredientDao.insertIngredient(ingredient)
    
    suspend fun removeIngredientFromMeal(ingredient: MealIngredient) = 
        mealIngredientDao.deleteIngredient(ingredient)
}
```

### 2.5 Utilities

**UnitConverter.kt** – Einheitskonvertierung zwischen Units
```kotlin
object UnitConverter {
    // Konvertiert z.B. 1000 GRAM zu 1 KILOGRAM
    fun convertQuantity(
        quantity: Double,
        fromUnit: PantryUnit,
        toUnit: PantryUnit
    ): Double {
        // Implementierung: Basis-Einheiten (z.B. Milliliter, Gram)
        // und Konversionsfaktoren definieren
    }
}
```

---

## 3. Domain Layer (Repository)

Das Repository (`PantryRepository`) ist die **Single Source of Truth**:

- **Abstraktion**: Versteckt DAO-Details vor UI
- **Business Logic**: Filtering, Sorting, Combining Data
- **Async Management**: Coroutines + Flow für non-blocking Operations

### Typischer Workflow:
```
ViewModel.addItem(item)
    └─> viewModelScope.launch
        └─> repository.insertItem(item)
            └─> pantryDao.insertItem(item)  // DB Write
```

---

## 4. Presentation Layer (UI)

### 4.1 Navigation Structure

**Screen.kt** – Sealed Class für Route-Definitionen
```kotlin
sealed class Screen(val route: String) {
    object InventoryList : Screen("inventory_list")
    object AddEditItem : Screen("add_edit_item")
    object ItemDetail : Screen("item_detail/{itemId}") {
        fun createRoute(itemId: Long) = "item_detail/$itemId"
    }
    object MealsList : Screen("meals_list")
    object AddEditMeal : Screen("add_edit_meal")
    object MealDetail : Screen("meal_detail/{mealId}") {
        fun createRoute(mealId: Long) = "meal_detail/$mealId"
    }
    // ... weitere Screens
}
```

**PantryNavigation.kt** – NavHost Setup
```kotlin
@Composable
fun PantryNavHost(
    navController: NavHostController,
    viewModel: PantryViewModel
) {
    NavHost(navController, startDestination = Screen.InventoryList.route) {
        composable(Screen.InventoryList.route) {
            InventoryListScreen(viewModel, navController)
        }
        composable(
            Screen.AddEditItem.route,
            arguments = listOf(
                navArgument("itemId") {
                    type = NavType.LongType
                    defaultValue = 0L  // 0 = neues Item
                }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0
            AddEditItemScreen(viewModel, navController, itemId)
        }
        // ... weitere Screens
    }
}
```

### 4.2 ViewModel State Management

**PantryViewModel.kt** – Zentrale State Management
```kotlin
class PantryViewModel(private val repository: PantryRepository) : ViewModel() {
    
    // ===== PANTRY STATE =====
    private val _sortOption = MutableStateFlow(SortOption.EXPIRY_DATE)
    val sortOption: StateFlow<SortOption> = _sortOption
    
    private val _filterOption = MutableStateFlow(FilterOption.ALL)
    val filterOption: StateFlow<FilterOption> = _filterOption
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    // Kombiniert Repo-Daten mit UI-State
    val pantryItems: StateFlow<List<PantryItem>> = combine(
        repository.getAllItems(),
        _sortOption,
        _filterOption,
        _searchQuery
    ) { items, sort, filter, query ->
        // Filtering & Sorting Logic
        items
            .filter { it.name.contains(query, ignoreCase = true) }
            .filter { matchesFilter(it, filter) }
            .sortedWith { a, b -> compareBy(sort, a, b) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // ===== OPERATIONS =====
    fun addItem(item: PantryItem) {
        viewModelScope.launch {
            repository.insertItem(item)
        }
    }
    
    fun consumeOne(item: PantryItem) {
        if (item.quantity > 0) {
            viewModelScope.launch {
                val newQuantity = (item.quantity - 1.0).coerceAtLeast(0.0)
                repository.updateItem(item.copy(quantity = newQuantity))
                
                // Auch in Konsumhistorie aufzeichnen
                repository.insertConsumptionRecord(
                    ConsumptionRecord(
                        itemId = item.id,
                        itemName = item.name,
                        quantityConsumed = 1.0,
                        unit = item.unit
                    )
                )
            }
        }
    }
    
    fun setSortOption(option: SortOption) { _sortOption.value = option }
    fun setFilterOption(option: FilterOption) { _filterOption.value = option }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
}

enum class SortOption { NAME, EXPIRY_DATE }
enum class FilterOption { ALL, OVERDUE, EXPIRING_SOON }
```

### 4.3 Compose Screens

**InventoryListScreen.kt** – Hauptansicht
```kotlin
@Composable
fun InventoryListScreen(
    viewModel: PantryViewModel,
    navController: NavHostController
) {
    val items by viewModel.pantryItems.collectAsState()
    val filterOption by viewModel.filterOption.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    
    Column {
        // Top Bar mit Filter/Sort Controls
        FilterSortBar(
            sortOption = sortOption,
            filterOption = filterOption,
            onSortChange = viewModel::setSortOption,
            onFilterChange = viewModel::setFilterOption
        )
        
        // Items List
        LazyColumn {
            items(items) { item ->
                PantryItemRow(
                    item = item,
                    onEdit = { navController.navigate(Screen.AddEditItem.createRoute(item.id)) },
                    onDelete = { viewModel.deleteItem(item) },
                    onConsume = { viewModel.consumeOne(item) }
                )
            }
        }
        
        // FAB zum Hinzufügen
        FloatingActionButton(
            onClick = { navController.navigate(Screen.AddEditItem.route) }
        ) {
            Icon(Icons.Default.Add, "Add Item")
        }
    }
}
```

**AddEditItemScreen.kt** – Form für Add/Edit
```kotlin
@Composable
fun AddEditItemScreen(
    viewModel: PantryViewModel,
    navController: NavHostController,
    itemId: Long
) {
    var item by remember { mutableStateOf<PantryItem?>(null) }
    
    // Lade Item wenn itemId > 0 (Edit-Mode)
    LaunchedEffect(itemId) {
        if (itemId > 0) {
            item = viewModel.getItemById(itemId)
        }
    }
    
    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = item?.name ?: "",
            onValueChange = { item = item?.copy(name = it) ?: PantryItem(name = it, ...) },
            label = { Text("Name") }
        )
        
        // Menge, Einheit, Ablaufdatum, Kategorie Felder...
        
        Button(onClick = {
            item?.let {
                if (it.id == 0L) {
                    viewModel.addItem(it)
                } else {
                    viewModel.updateItem(it)
                }
                navController.popBackStack()
            }
        }) {
            Text("Save")
        }
    }
}
```

### 4.4 Theme & Styling

**theme/** – Material Design 3 Integration
- `Color.kt` – App-Farben (Primary, Secondary, etc.)
- `Typography.kt` – Text-Stile (Headline, Body, etc.)
- `Theme.kt` – Material3Theme Setup mit Dark Mode Support

---

## 5. Util Layer

### NotificationHelper.kt
```kotlin
class NotificationHelper(private val context: Context) {
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "expiry_alerts",
                "Ablauf-Benachrichtigungen",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }
    
    fun notifyExpiredItems(items: List<PantryItem>) {
        val notification = NotificationCompat.Builder(context, "expiry_alerts")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Items abgelaufen!")
            .setContentText("${items.size} Items in der Speisekammer abgelaufen")
            .build()
        
        context.getSystemService(NotificationManager::class.java)
            ?.notify(1, notification)
    }
}
```

### ExpiryCheckWorker.kt – Background Job
```kotlin
class ExpiryCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val repository = (applicationContext as PantryPureApplication).repository
            val items = repository.getAllItems().first()
            val now = System.currentTimeMillis()
            
            val expiredItems = items.filter { item ->
                item.expiryDate != null && item.expiryDate < now
            }
            
            if (expiredItems.isNotEmpty()) {
                NotificationHelper(applicationContext).notifyExpiredItems(expiredItems)
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

---

## 6. Application Initialization

**PantryPureApplication.kt**
```kotlin
class PantryPureApplication : Application() {
    // Lazy-initialized Database (singleton)
    private val database by lazy {
        Room.databaseBuilder(
            this,
            PantryDatabase::class.java,
            "pantry_database"
        )
        .fallbackToDestructiveMigration(false)  // Erfordert explizite Migrations
        .build()
    }
    
    // Lazy-initialized Repository (singleton)
    val repository by lazy {
        PantryRepository(
            database.pantryDao(),
            database.consumptionDao(),
            database.mealDao(),
            database.mealIngredientDao()
        )
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Setup Notification Channels
        NotificationHelper(this).createNotificationChannel()
        
        // Schedule Background Work
        scheduleExpiryCheck()
    }
    
    private fun scheduleExpiryCheck() {
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
    }
}
```

**MainActivity.kt**
```kotlin
class MainActivity : ComponentActivity() {
    private val viewModel: PantryViewModel by viewModels {
        PantryViewModelFactory(
            (application as PantryPureApplication).repository
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PantryPureTheme {
                NotificationPermissionEffect()
                PantryPureApp(viewModel)
            }
        }
    }
}

@Composable
fun PantryPureApp(viewModel: PantryViewModel) {
    val navController = rememberNavController()
    PantryNavHost(navController, viewModel)
}
```

---

## 7. Workflow-Beispiele

### Add Item to Pantry
```
UI: User klickt FAB → AddEditItemScreen
    ↓
UI: User füllt Form (Name, Menge, Unit, Ablauf) → klickt Save
    ↓
ViewModel: viewModel.addItem(item) aufgerufen
    ↓
Repository: repository.insertItem(item)
    ↓
DAO: pantryDao.insertItem(item)
    ↓
DB: INSERT into pantry_items VALUES (...)
    ↓
Flow: AllItems Flow emittiert neue Liste
    ↓
StateFlow: pantryItems.value wird aktualisiert
    ↓
UI: Recompose mit neuem Item in der Liste
```

### Consume One Item
```
UI: User klickt "Consume" Button auf Item
    ↓
ViewModel: viewModel.consumeOne(item) aufgerufen
    ↓
ViewModel: 
  1. newQuantity = item.quantity - 1
  2. repository.updateItem(item.copy(quantity = newQuantity))
  3. repository.insertConsumptionRecord(ConsumptionRecord(...))
    ↓
DB: 
  UPDATE pantry_items SET quantity = ... WHERE id = ?
  INSERT INTO consumption_records VALUES (...)
    ↓
Flows: Beide pantryItems & consumptionHistory emittieren neue Daten
    ↓
UI: Recompose mit aktualisierten Werten
```

---

## 8. Best Practices für LLM-Entwicklung

### Code Patterns, die einzuhalten sind:

1. **ViewModel Creation**: Immer `PantryViewModelFactory` nutzen
   ```kotlin
   private val viewModel: PantryViewModel by viewModels {
       PantryViewModelFactory((application as PantryPureApplication).repository)
   }
   ```

2. **Async Operations**: Koroutinen im `viewModelScope`
   ```kotlin
   fun addItem(item: PantryItem) {
       viewModelScope.launch {
           repository.insertItem(item)  // suspend function
       }
   }
   ```

3. **State Collection**: Immer `.collectAsState()` in Compose
   ```kotlin
   val items by viewModel.pantryItems.collectAsState()
   ```

4. **Navigation Args**: Immer `sealed class Screen` Pattern
   ```kotlin
   navController.navigate(Screen.AddEditItem.createRoute(itemId))
   ```

5. **DB Versioning**: Version erhöhen + Migration bereitstellen
   ```kotlin
   @Database(..., version = 6)  // War 5, jetzt 6
   ```

6. **Type Converters**: Immer explizit für Enums definieren
   ```kotlin
   @TypeConverter
   fun pantryUnitToString(unit: PantryUnit?): String? = unit?.name
   ```

---

**Alle Dateipfade sind relativ zu** `app/src/main/java/com/example/pantrypure/`
