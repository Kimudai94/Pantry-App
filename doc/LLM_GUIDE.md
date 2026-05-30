# LLM Development Guide (für OpenClaude & andere LLMs)

Spezifische Anleitung für LLM-gesteuerte Entwicklung in diesem Projekt.

---

## Inhaltsverzeichnis

1. [Quick Reference für LLMs](#quick-reference-für-llms)
2. [Projekt-Navigation](#projekt-navigation)
3. [Code-Patterns & Konventionen](#code-patterns--konventionen)
4. [Häufige Aufgaben & Lösungsvorlagen](#häufige-aufgaben--lösungsvorlagen)
5. [Debugging-Tipps für LLMs](#debugging-tipps-für-llms)
6. [Anti-Patterns zu vermeiden](#anti-patterns-zu-vermeiden)

---

## Quick Reference für LLMs

### Projekt-Struktur in 60 Sekunden

```
PantryPure = Android App für Speisekammer-Verwaltung
Tech Stack: Kotlin + Jetpack Compose + Room + MVVM

Architektur:
  UI (Compose Screens)
    ↓
  ViewModel (State Management)
    ↓
  Repository (Business Logic)
    ↓
  DAO (Database Access)
    ↓
  Room (SQLite)

Wenn ein LLM Code schreiben soll:
  → Zuerst DAO/Entity (Data Layer)
  → Dann Repository (Business Logic)
  → Dann ViewModel (State)
  → Zuletzt UI (Screens)
```

### File Locations (Schnell-Zugriff)

| Was | Wo |
|-----|-----|
| **Models/Entities** | `app/src/main/java/com/example/pantrypure/data/model/` |
| **Database & DAO** | `app/src/main/java/com/example/pantrypure/data/dao/` + `data/database/` |
| **Repository** | `app/src/main/java/com/example/pantrypure/data/repository/PantryRepository.kt` |
| **ViewModel** | `app/src/main/java/com/example/pantrypure/ui/viewmodel/PantryViewModel.kt` |
| **Screens** | `app/src/main/java/com/example/pantrypure/ui/screen/` |
| **Navigation** | `app/src/main/java/com/example/pantrypure/ui/navigation/PantryNavigation.kt` |
| **Build Config** | `app/build.gradle.kts` |
| **Database Version** | `app/src/main/java/com/example/pantrypure/data/database/PantryDatabase.kt` (line: `@Database(version = X)`) |

### Database Entities (Current Schema)

```
PantryItem (Lager-Gegenstände)
  - id: Long (PK)
  - name: String
  - quantity: Double
  - unit: PantryUnit (Enum → TEXT in DB)
  - expiryDate: Long? (Milliseconds timestamp)
  - expiryThresholdDays: Int (Default: 3)
  - isOnShoppingList: Boolean
  - category: String
  - notes: String

ConsumptionRecord (Verbrauchshistorie)
  - id: Long (PK)
  - itemId: Long (FK → PantryItem)
  - itemName: String (Snapshot)
  - quantityConsumed: Double
  - unit: PantryUnit
  - timestamp: Long

Meal (Rezepte)
  - id: Long (PK)
  - name: String
  - category: MealCategory (Enum)
  - instructions: String

MealIngredient (Zutaten-Zuordnung)
  - id: Long (PK)
  - mealId: Long (FK → Meal)
  - pantryItemId: Long (FK → PantryItem)
  - requiredQuantity: Double
```

---

## Projekt-Navigation

### Wie LLMs die Codebase erforschen sollten

#### Schritt 1: Verstehe die Entry Points
```
→ MainActivity.kt          # App starts here
  → PantryPureApplication  # Initializes DB & Repository
  → PantryNavHost          # Sets up navigation
```

#### Schritt 2: Verstehe den Data Flow
```
Screen (UI)
  ↓ (Collects State)
ViewModel (Holds StateFlow<T>)
  ↓ (Calls)
Repository (Business Logic)
  ↓ (Calls)
DAO (Database Queries)
  ↓ (Reads/Writes)
Room Database (SQLite)
```

#### Schritt 3: Finde die relevante Feature
```
Beispiel: "Ich möchte Artikel löschen können"

1. Find the Model:
   app/src/main/java/.../data/model/PantryItem.kt
   
2. Find the DAO:
   app/src/main/java/.../data/dao/PantryDao.kt
   → Look for: @Delete fun deleteItem(item: PantryItem)
   
3. Find the Repository:
   .../data/repository/PantryRepository.kt
   → Look for: suspend fun deleteItem(item: PantryItem)
   
4. Find the ViewModel:
   .../ui/viewmodel/PantryViewModel.kt
   → Look for: fun deleteItem(item: PantryItem)
   
5. Find the Screen:
   .../ui/screen/InventoryListScreen.kt
   → Look for: onDelete = { viewModel.deleteItem(item) }
```

---

## Code-Patterns & Konventionen

### Wichtigste Muster

#### 1. Asynchrone Operationen (Coroutines)
```kotlin
// ❌ FALSCH: Blocking operation in ViewModel
fun addItem(item: PantryItem) {
    val result = repository.insertItem(item)  // BLOCKS!
}

// ✅ RICHTIG: Launch in viewModelScope
fun addItem(item: PantryItem) {
    viewModelScope.launch {
        repository.insertItem(item)  // Non-blocking suspend
    }
}
```

#### 2. State Management (StateFlow)
```kotlin
// ❌ FALSCH: Mutable state exposed to UI
val items: MutableList<PantryItem> = mutableListOf()

// ✅ RICHTIG: Immutable StateFlow
private val _items = MutableStateFlow<List<PantryItem>>(emptyList())
val items: StateFlow<List<PantryItem>> = _items.asStateFlow()

// In UI: Collect as State
val items by viewModel.items.collectAsState()
```

#### 3. Reactive Filtering (Combine)
```kotlin
// ✅ RICHTIG: Combine multiple flows
val filteredItems: StateFlow<List<PantryItem>> = combine(
    repository.getAllItems(),      // Flow from DB
    _sortOption,                   // ViewModel filter
    _searchQuery                   // ViewModel search
) { items, sort, query ->
    items.filter { it.name.contains(query, ignoreCase = true) }
        .sortedBy { if (sort == SortOption.NAME) it.name else it.expiryDate }
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

#### 4. Navigation Args
```kotlin
// ❌ FALSCH: Passing objects through arguments
navController.navigate("detail?item=$item")  // Won't work

// ✅ RICHTIG: Pass only IDs, load data in screen
sealed class Screen(val route: String) {
    object ItemDetail : Screen("item_detail/{itemId}") {
        fun createRoute(itemId: Long) = "item_detail/$itemId"
    }
}

// In screen: Load from ViewModel
LaunchedEffect(itemId) {
    item = viewModel.getItemById(itemId)
}
```

#### 5. Database Entities
```kotlin
// ✅ RICHTIG: Full Entity with all Room annotations
@Entity(tableName = "pantry_items")
data class PantryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: Double,
    val unit: PantryUnit,  // Enum → TypeConverter needed
    val expiryDate: Long?,
    val expiryThresholdDays: Int = 3,
    val isOnShoppingList: Boolean = false,
    val category: String,
    val notes: String = ""
)

// Wenn Enum: Braucht TypeConverter!
class PantryTypeConverters {
    @TypeConverter
    fun pantryUnitToString(unit: PantryUnit?): String? = unit?.name
    
    @TypeConverter
    fun stringToPantryUnit(value: String?): PantryUnit? = 
        value?.let { PantryUnit.valueOf(it) }
}
```

#### 6. Suspend Functions (for database operations)
```kotlin
// ❌ FALSCH: Regular function
fun insertItem(item: PantryItem) {
    // Won't work with Room
}

// ✅ RICHTIG: Suspend function
suspend fun insertItem(item: PantryItem) {
    repository.insertItem(item)  // Can be awaited
}
```

#### 7. Flow vs StateFlow
```kotlin
// ❌ FALSCH: Regular Flow
fun getAllItems(): Flow<List<PantryItem>> = 
    pantryDao.getAllItems()

// ✅ RICHTIG: StateFlow für State, Flow für Events
private val _items = MutableStateFlow<List<PantryItem>>(emptyList())
val items: StateFlow<List<PantryItem>> = _items.asStateFlow()

// Aber: Repository returns Flow
fun getAllItems(): Flow<List<PantryItem>> = pantryDao.getAllItems()
```

---

## Häufige Aufgaben & Lösungsvorlagen

### Task 1: Neue Entity hinzufügen

**Aufgabe**: "Add a 'PreparedDate' field to Meal"

**Schritt 1**: Update Entity
```kotlin
// File: app/src/main/java/.../data/model/Meal.kt
@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: MealCategory,
    val instructions: String,
    val preparedDate: Long? = null  // NEW
)
```

**Schritt 2**: Create Migration
```kotlin
// File: app/src/main/java/.../data/database/Migration_6_To_7.kt
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            ALTER TABLE meals 
            ADD COLUMN preparedDate INTEGER DEFAULT NULL
        """)
    }
}
```

**Schritt 3**: Update Database Version
```kotlin
// File: app/src/main/java/.../data/database/PantryDatabase.kt
@Database(..., version = 7)  // Was 6
```

**Schritt 4**: Register Migration
```kotlin
// File: PantryPureApplication.kt
private val database by lazy {
    Room.databaseBuilder(this, PantryDatabase::class.java, "pantry_database")
        .addMigrations(MIGRATION_5_6, MIGRATION_6_7)  // ADD HERE
        .fallbackToDestructiveMigration(false)
        .build()
}
```

**Schritt 5**: Test
```bash
./gradlew build
./gradlew installDebug
# Test in app that data persists correctly
```

### Task 2: Neue DAO Query hinzufügen

**Aufgabe**: "Add query to get items expiring today"

```kotlin
// File: app/src/main/java/.../data/dao/PantryDao.kt
@Dao
interface PantryDao {
    // ... existing methods ...
    
    @Query("""
        SELECT * FROM pantry_items 
        WHERE expiryDate IS NOT NULL
        AND expiryDate BETWEEN :startOfDay AND :endOfDay
        ORDER BY expiryDate ASC
    """)
    fun getItemsExpiringToday(
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<PantryItem>>
}
```

**Usage im Repository**:
```kotlin
// File: data/repository/PantryRepository.kt
fun getItemsExpiringToday(): Flow<List<PantryItem>> {
    val now = System.currentTimeMillis()
    val startOfDay = now - (now % (24 * 60 * 60 * 1000))
    val endOfDay = startOfDay + (24 * 60 * 60 * 1000)
    return pantryDao.getItemsExpiringToday(startOfDay, endOfDay)
}
```

### Task 3: Neue ViewModel Methode hinzufügen

**Aufgabe**: "Add a method to get expiring items with a warning level"

```kotlin
// File: ui/viewmodel/PantryViewModel.kt
data class ExpiryWarning(
    val item: PantryItem,
    val level: WarningLevel  // EXPIRED, URGENT (< 1 day), SOON (< 3 days)
)

enum class WarningLevel { EXPIRED, URGENT, SOON }

class PantryViewModel(...) {
    val expiryWarnings: StateFlow<List<ExpiryWarning>> = 
        repository.getAllItems()
            .map { items ->
                val now = System.currentTimeMillis()
                items.mapNotNull { item ->
                    if (item.expiryDate == null) return@mapNotNull null
                    
                    val daysUntilExpiry = (item.expiryDate - now) / (24 * 60 * 60 * 1000)
                    
                    val level = when {
                        daysUntilExpiry < 0 -> WarningLevel.EXPIRED
                        daysUntilExpiry < 1 -> WarningLevel.URGENT
                        daysUntilExpiry < item.expiryThresholdDays -> WarningLevel.SOON
                        else -> return@mapNotNull null
                    }
                    
                    ExpiryWarning(item, level)
                }.sortedBy { it.item.expiryDate }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

### Task 4: Neuer Screen hinzufügen

**Aufgabe**: "Add ExpiryWarningsScreen to show items expiring soon"

**Schritt 1**: Add Route
```kotlin
// ui/navigation/Screen.kt
sealed class Screen(val route: String) {
    // ... existing ...
    object ExpiryWarnings : Screen("expiry_warnings")
}
```

**Schritt 2**: Create Screen
```kotlin
// ui/screen/ExpiryWarningsScreen.kt
@Composable
fun ExpiryWarningsScreen(
    viewModel: PantryViewModel,
    navController: NavHostController
) {
    val warnings by viewModel.expiryWarnings.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Items Expiring Soon", style = MaterialTheme.typography.headlineSmall)
        
        LazyColumn {
            items(warnings) { warning ->
                WarningCard(
                    warning = warning,
                    onEdit = { 
                        navController.navigate(
                            Screen.AddEditItem.createRoute(warning.item.id)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun WarningCard(warning: ExpiryWarning, onEdit: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Row(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(warning.item.name, fontWeight = FontWeight.Bold)
                Text("Level: ${warning.level}", color = Color.Gray)
            }
            Button(onClick = onEdit) { Text("Edit") }
        }
    }
}
```

**Schritt 3**: Add to NavHost
```kotlin
// ui/navigation/PantryNavigation.kt
composable(Screen.ExpiryWarnings.route) {
    ExpiryWarningsScreen(viewModel, navController)
}
```

**Schritt 4**: Link from another screen
```kotlin
// z.B. in InventoryListScreen
Button(onClick = { navController.navigate(Screen.ExpiryWarnings.route) }) {
    Text("View Expiry Warnings")
}
```

---

## Debugging-Tipps für LLMs

### Problem: Room Migration Error

**Symptom**: App crashes with "Migration not found" or "Cannot find entity"

**Debugging**:
```
1. Check database version in @Database annotation
2. Verify all migrations are registered in Room.databaseBuilder()
3. If adding new column, ensure ALTER TABLE SQL is correct
4. Check TypeConverter for any Enum fields
5. Look in logcat: "adb logcat Room:D"
```

**Häufigste Fehler**:
- [ ] Version nicht erhöht
- [ ] Migration nicht registriert
- [ ] Falsche SQL Syntax in Migration
- [ ] Entity definition passt nicht zur DB

### Problem: Compose UI Not Updating

**Symptom**: User taps button, but UI doesn't change

**Debugging**:
```
1. Check if StateFlow is being collected: val items by viewModel.items.collectAsState()
2. Is ViewModel function actually launching in viewModelScope?
3. Check Logcat for exceptions
4. Verify Repository.insertItem() is being called
5. Check Database with Android Studio Database Inspector
```

**Häufigste Fehler**:
- [ ] StateFlow nicht collected (missing `.collectAsState()`)
- [ ] Operation nicht in `viewModelScope.launch`
- [ ] Async operation result not propagated back to StateFlow

### Problem: Database Constraints Error

**Symptom**: "UNIQUE constraint failed" or "FOREIGN KEY constraint failed"

**Debugging**:
```
1. Check if PK is already in DB: SELECT MAX(id) FROM table
2. Verify FK relationship exists in foreign table
3. Check unique constraints in Entity
4. Look at INSERT/UPDATE statement in DAO
```

**Häufigste Fehler**:
- [ ] PrimaryKey not unique
- [ ] Foreign Key doesn't exist
- [ ] Duplicate INSERT without checking existing IDs

### Problem: TypeConverter Serialization

**Symptom**: "Cannot convert enum from database" or empty value in DB

**Debugging**:
```
1. Is @TypeConverter added to Entity fields?
2. Check if TypeConverter is added to @Database @TypeConverters(...)
3. Verify enum name matches string in DB
4. Check .name vs .toString() in converter
```

**Häufigste Fehler**:
- [ ] Enum TypeConverter nicht registriert
- [ ] Enum Wert im DB nicht gültig (Case-sensitive!)
- [ ] TypeConverter nicht in Database Klasse referenziert

---

## Anti-Patterns zu vermeiden

### ❌ Anti-Pattern 1: Blocking on Main Thread
```kotlin
// FALSCH
fun loadData() {
    val data = repository.getAllItems()  // Blocks!
}

// RICHTIG
fun loadData() {
    viewModelScope.launch {
        val data = repository.getAllItems()  // Non-blocking
    }
}
```

### ❌ Anti-Pattern 2: Exposing Mutable State
```kotlin
// FALSCH
val items: MutableList<PantryItem> = mutableListOf()

// RICHTIG
private val _items = MutableStateFlow<List<PantryItem>>(emptyList())
val items: StateFlow<List<PantryItem>> = _items
```

### ❌ Anti-Pattern 3: Passing Objects through Navigation
```kotlin
// FALSCH
navController.navigate("detail?item=$item")

// RICHTIG
navController.navigate(Screen.Detail.createRoute(item.id))
```

### ❌ Anti-Pattern 4: Not Using TypeConverters for Enums
```kotlin
// FALSCH
@Entity
data class Meal(
    val category: MealCategory  // Won't serialize!
)

// RICHTIG
@Entity
data class Meal(
    val category: MealCategory  // with TypeConverter
)

@Database(..., @TypeConverters(PantryTypeConverters::class))
```

### ❌ Anti-Pattern 5: Creating Database Instance Multiple Times
```kotlin
// FALSCH
val db1 = Room.databaseBuilder(...).build()
val db2 = Room.databaseBuilder(...).build()  // Two separate DBs!

// RICHTIG
val database by lazy {
    Room.databaseBuilder(...).build()  // Singleton
}
```

### ❌ Anti-Pattern 6: Forgetting suspend Keyword
```kotlin
// FALSCH
fun insertItem(item: PantryItem) {
    // Won't work with Room DAO
}

// RICHTIG
suspend fun insertItem(item: PantryItem) {
    // Can be awaited in coroutine
}
```

### ❌ Anti-Pattern 7: Modifying Entity in UI
```kotlin
// FALSCH
item.quantity = 5.0  // Direct modification
// Now need to manually call updateItem()

// RICHTIG
val newItem = item.copy(quantity = 5.0)
viewModel.updateItem(newItem)
```

---

## Checkliste für neue Features

Bevor ein LLM einen Pull Request macht:

- [ ] **Entity**: Ist die neue Entity oder modifizierte Entity korrekt definiert?
- [ ] **Migration**: Wenn DB-Schema geändert, ist Migration erstellt und Version erhöht?
- [ ] **TypeConverter**: Alle Enums mit @TypeConverter versehen?
- [ ] **DAO**: Alle benötigten Queries definiert?
- [ ] **Repository**: Business Logic in Repository, nicht in ViewModel?
- [ ] **ViewModel**: State in StateFlow, Operations in viewModelScope?
- [ ] **Screen**: UI sammelt State mit `.collectAsState()`?
- [ ] **Navigation**: Routes in `Screen.kt` defined, NavHost composable added?
- [ ] **Tests**: Unit Tests für kritische Logik?
- [ ] **Build**: `./gradlew build` erfolgreich?
- [ ] **Install**: `./gradlew installDebug` ohne Fehler?
- [ ] **Manual Test**: Feature manuell in App getestet?

---

## Für OpenClaude (oder andere multimodale LLMs)

### Wenn du Code Review machst:
1. Check **Architektur** (Data → Repository → ViewModel → UI)
2. Check **Asynchronie** (coroutines, suspend functions)
3. Check **State Management** (StateFlow, MutableStateFlow, Flow.combine)
4. Check **Navigation** (Routes, Args, createRoute methods)
5. Check **Database** (Entities, DAOs, TypeConverters, Migrations)

### Wenn du Code schreibst:
1. Schreib **Von unten nach oben** (Data Layer → UI Layer)
2. Schreib **Suspend Functions** für DB Ops
3. Benutze **StateFlow.combine** für reaktive Filtering
4. Schreib **Keine Custom Exceptions** (nur aussagekräftige Error Messages)
5. **Teste lokal** vor Push

### Wenn du Fehler debuggst:
1. Prüfe **Logcat** zuerst
2. Prüfe **Android Studio Database Inspector**
3. Prüfe **Room Migrations** gegen Entity Definitionen
4. Prüfe **StateFlow Collection** im UI
5. Prüfe **Coroutine Scopes** (viewModelScope vs GlobalScope)

---

## Quick Links

| Frage | Antwort |
|-------|--------|
| Wo finde ich Screens? | `ui/screen/` |
| Wo definiere ich neue Routes? | `ui/navigation/Screen.kt` |
| Wo ändere ich die Database-Version? | `data/database/PantryDatabase.kt` |
| Wo schreibe ich Business-Logik? | `data/repository/PantryRepository.kt` |
| Wo schreibe ich State-Management? | `ui/viewmodel/PantryViewModel.kt` |
| Wie teste ich? | `./gradlew test` (unit), `./gradlew connectedAndroidTest` (instrumented) |
| Wie debugge ich? | Logcat: `adb logcat com.example.pantrypure:V` |

---

**Dieser Guide wurde für LLM-optimierte Entwicklung konzipiert. Alle Patterns sind konsistent im Projekt angewendet.**
