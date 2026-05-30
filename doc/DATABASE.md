# Database Schema & Migrations Guide

Umfassende Dokumentation des Room-Datenbankschemas und der Migrationsprozesse für PantryPure.

---

## 1. Current Database Schema (Version 5)

### Entity Relationships

```
┌──────────────────┐
│   PANTRY_ITEM    │
├──────────────────┤
│ PK: id           │
│    name          │
│    quantity      │
│    unit          │◄──────┐
│    expiryDate    │       │
│    category      │       │ 1:N
│    notes         │       │
└──────────────────┘       │
         │                 │
         │ 1:N             │
         ▼                 │
┌──────────────────────┐   │
│ CONSUMPTION_RECORD   │   │
├──────────────────────┤   │
│ PK: id               │   │
│ FK: itemId ─────────►│   │
│    itemName (snap)   │   │
│    quantityConsumed  │   │
│    unit ─────────────┼───┤
│    timestamp         │   │
└──────────────────────┘   │
                           │
┌─────────────────┐        │
│      MEAL       │        │
├─────────────────┤        │
│ PK: id          │        │
│    name         │        │
│    category     │        │
│    instructions │        │
└─────────────────┘        │
         │                 │
         │ 1:N             │
         ▼                 │
┌──────────────────────┐   │
│  MEAL_INGREDIENT     │   │
├──────────────────────┤   │
│ PK: id               │   │
│ FK: mealId ───────►  │   │
│ FK: pantryItemId ────┴───┘
│    requiredQuantity  │
└──────────────────────┘
```

### Detailed Schema

#### PANTRY_ITEMS Table

| Column                | Type    | Constraints                | Description                                 |
|-----------------------|---------|----------------------------|---------------------------------------------|
| `id`                  | INTEGER | PRIMARY KEY, AUTOINCREMENT | Unique identifier                           |
| `name`                | TEXT    | NOT NULL                   | Item name (e.g., "Spaghetti")               |
| `quantity`            | REAL    | NOT NULL                   | Current amount in pantry                    |
| `unit`                | TEXT    | NOT NULL                   | PantryUnit as string (ENUM→converter)       |
| `expiryDate`          | INTEGER | NULLABLE                   | Milliseconds since epoch (null = no expiry) |
| `expiryThresholdDays` | INTEGER | NOT NULL, DEFAULT 3        | Days before expiry to warn                  |
| `isOnShoppingList`    | INTEGER | NOT NULL, DEFAULT 0        | Boolean (0=false, 1=true)                   |
| `category`            | TEXT    | NOT NULL                   | Item category (e.g., "Obst")                |
| `notes`               | TEXT    | DEFAULT ''                 | Optional user notes                         |

**Example Data**:
```sql
INSERT INTO pantry_items VALUES 
(1, 'Tomatoes', 5.0, 'PIECE', 1704067200000, 3, 0, 'Gemüse', 'In Kühlschrank'),
(2, 'Olive Oil', 500.0, 'MILLILITER', NULL, 3, 0, 'Öl', ''),
(3, 'Flour', 1000.0, 'GRAM', 1704153600000, 7, 1, 'Getreide', 'Nearly empty');
```

#### CONSUMPTION_RECORDS Table

| Column             | Type    | Constraints                   | Description                         |
|--------------------|---------|-------------------------------|-------------------------------------|
| `id`               | INTEGER | PRIMARY KEY, AUTOINCREMENT    | Record ID                           |
| `itemId`           | INTEGER | FOREIGN KEY → pantry_items.id | Which item was consumed             |
| `itemName`         | TEXT    | NOT NULL                      | Snapshot of item name (for history) |
| `quantityConsumed` | REAL    | NOT NULL                      | Amount consumed (e.g., 1.0)         |
| `unit`             | TEXT    | NOT NULL                      | PantryUnit as string                |
| `timestamp`        | INTEGER | NOT NULL, DEFAULT NOW         | Milliseconds since epoch            |

**Example Data**:
```sql
INSERT INTO consumption_records VALUES 
(1, 1, 'Tomatoes', 1.0, 'PIECE', 1704067200000),
(2, 1, 'Tomatoes', 1.0, 'PIECE', 1704153600000),
(3, 2, 'Olive Oil', 50.0, 'MILLILITER', 1704240000000);
```

**Purpose**: Maintains consumption history independently of current pantry state (itemName is snapshot for immutable history).

#### MEALS Table

| Column         | Type    | Constraints                | Description                                     |
|----------------|---------|----------------------------|-------------------------------------------------|
| `id`           | INTEGER | PRIMARY KEY, AUTOINCREMENT | Meal ID                                         |
| `name`         | TEXT    | NOT NULL                   | Recipe name (e.g., "Spaghetti Carbonara")       |
| `category`     | TEXT    | NOT NULL                   | MealCategory as string (BREAKFAST, LUNCH, etc.) |
| `instructions` | TEXT    | NOT NULL                   | Cooking instructions                            |

**Example Data**:
```sql
INSERT INTO meals VALUES 
(1, 'Spaghetti Carbonara', 'LUNCH', '1. Boil water...\n2. Cook pasta...'),
(2, 'Omelet', 'BREAKFAST', '1. Beat eggs...\n2. Heat pan...');
```

#### MEAL_INGREDIENTS Table

| Column             | Type    | Constraints                   | Description                     |
|--------------------|---------|-------------------------------|---------------------------------|
| `id`               | INTEGER | PRIMARY KEY, AUTOINCREMENT    | Ingredient record ID            |
| `mealId`           | INTEGER | FOREIGN KEY → meals.id        | Which meal uses this ingredient |
| `pantryItemId`     | INTEGER | FOREIGN KEY → pantry_items.id | Which item from pantry          |
| `requiredQuantity` | REAL    | NOT NULL                      | Amount needed for recipe        |

**Example Data**:
```sql
INSERT INTO meal_ingredients VALUES 
(1, 1, 1, 400.0),  -- Carbonara needs 400g pasta
(2, 1, 2, 50.0),   -- Carbonara needs 50ml olive oil
(3, 2, 1, 2.0);    -- Omelet needs 2 tomatoes
```

**Purpose**: Links meals to pantry items; allows calculating if meal can be prepared based on available quantities.

---

## 2. Type Converters (Enum Serialization)

Since Room stores enums as their `.name` string, we have TypeConverters to serialize/deserialize:

### PantryUnit Converter
```kotlin
@TypeConverter
fun fromPantryUnit(value: PantryUnit?): String? = value?.name
// "GRAM" ↔ PantryUnit.GRAM

@TypeConverter
fun toPantryUnit(value: String?): PantryUnit? = 
    value?.let { PantryUnit.valueOf(it) }
```

**Valid Values in DB**: `"PIECE"`, `"GRAM"`, `"KILOGRAM"`, `"MILLILITER"`, `"LITER"`, `"TABLESPOON"`, `"TEASPOON"`, `"CUP"`

### MealCategory Converter
```kotlin
@TypeConverter
fun fromMealCategory(value: MealCategory?): String? = value?.name

@TypeConverter
fun toMealCategory(value: String?): MealCategory? = 
    value?.let { MealCategory.valueOf(it) }
```

**Valid Values in DB**: `"BREAKFAST"`, `"LUNCH"`, `"DINNER"`, `"SNACK"`, `"OTHER"`

---

## 3. Database Migration Guide

### Current Version: 5

The database currently uses `version = 5` in the `@Database` annotation.

### When to Migrate

Migrations are needed when:
- ✅ Adding a new column to an existing table
- ✅ Removing a column
- ✅ Changing column type
- ✅ Adding a new table
- ✅ Adding/modifying foreign keys or constraints

Migrations are **NOT** needed for:
- ❌ Changes to indexes only (handled automatically)
- ❌ Adding new Room DAO methods (no schema change)

### How to Create a Migration

**Step 1: Update Entity Classes**

Example: Add a `quantity_threshold` column to `PantryItem`:

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
    val notes: String = "",
    val quantityThreshold: Double = 5.0  // NEW FIELD
)
```

**Step 2: Increment DB Version**

In `PantryDatabase.kt`:
```kotlin
@Database(..., version = 6)  // Was 5, now 6
@TypeConverters(PantryTypeConverters::class)
abstract class PantryDatabase : RoomDatabase() {
    // ...
}
```

**Step 3: Create Migration**

Create a new Kotlin file `MigrationFrom5To6.kt`:

```kotlin
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add the new column with default value
        database.execSQL("""
            ALTER TABLE pantry_items 
            ADD COLUMN quantityThreshold REAL NOT NULL DEFAULT 5.0
        """)
    }
}
```

**Step 4: Register Migration in Room Builder**

Update `PantryPureApplication.kt`:

```kotlin
private val database by lazy {
    Room.databaseBuilder(this, PantryDatabase::class.java, "pantry_database")
        .addMigrations(MIGRATION_5_6)  // Add this line
        .fallbackToDestructiveMigration(false)
        .build()
}
```

### Common Migration Patterns

#### Add Column with Default
```kotlin
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            ALTER TABLE pantry_items 
            ADD COLUMN newColumn TEXT NOT NULL DEFAULT 'default_value'
        """)
    }
}
```

#### Rename Column
```kotlin
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // SQLite doesn't support direct column rename; use workaround:
        database.execSQL("ALTER TABLE pantry_items RENAME TO pantry_items_old")
        database.execSQL("""
            CREATE TABLE pantry_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                quantity REAL NOT NULL,
                unit TEXT NOT NULL,
                expiryDate INTEGER,
                expiryThresholdDays INTEGER NOT NULL,
                isOnShoppingList INTEGER NOT NULL,
                category TEXT NOT NULL,
                notes TEXT,
                quantityThreshold REAL NOT NULL DEFAULT 5.0
            )
        """)
        database.execSQL("""
            INSERT INTO pantry_items 
            SELECT id, name, quantity, unit, expiryDate, expiryThresholdDays, 
                   isOnShoppingList, category, notes, 5.0 
            FROM pantry_items_old
        """)
        database.execSQL("DROP TABLE pantry_items_old")
    }
}
```

#### Add New Table
```kotlin
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE shopping_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                isPurchased INTEGER NOT NULL DEFAULT 0,
                timestamp INTEGER NOT NULL
            )
        """)
    }
}
```

#### Add Foreign Key Constraint
```kotlin
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Note: Requires table recreation in SQLite
        database.execSQL("""
            CREATE TABLE meal_ingredients_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                mealId INTEGER NOT NULL,
                pantryItemId INTEGER NOT NULL,
                requiredQuantity REAL NOT NULL,
                FOREIGN KEY(mealId) REFERENCES meals(id) ON DELETE CASCADE,
                FOREIGN KEY(pantryItemId) REFERENCES pantry_items(id) ON DELETE CASCADE
            )
        """)
        database.execSQL("""
            INSERT INTO meal_ingredients_new 
            SELECT * FROM meal_ingredients
        """)
        database.execSQL("DROP TABLE meal_ingredients")
        database.execSQL("""
            ALTER TABLE meal_ingredients_new 
            RENAME TO meal_ingredients
        """)
    }
}
```

---

## 4. Fallback Strategy

Currently, `fallbackToDestructiveMigration(false)` is set, meaning:
- ✅ **Explicit migrations are required** – No auto-destruction
- ❌ **App crashes if migration is missing** – Good for production safety

### For Development Only (Temporary)

If you want to allow destructive migrations during development:
```kotlin
private val database by lazy {
    Room.databaseBuilder(this, PantryDatabase::class.java, "pantry_database")
        .fallbackToDestructiveMigration()  // Dev only!
        .build()
}
```

**Warning**: This clears all data on version mismatch. Never use in production.

---

## 5. Querying the Schema Programmatically

### Check Database Version at Runtime
```kotlin
val currentVersion = database.openHelper.readableDatabase.version
Log.d("PantryDB", "Current DB Version: $currentVersion")
```

### Inspect Table Schema
```kotlin
val cursor = database.query("PRAGMA table_info(pantry_items)", null)
while (cursor.moveToNext()) {
    val columnName = cursor.getString(1)
    val columnType = cursor.getString(2)
    Log.d("PantryDB", "$columnName: $columnType")
}
cursor.close()
```

---

## 6. Backup & Export

### Export Schema for Documentation
```bash
# After building, Room generates schema JSON files:
find . -name "*.json" -path "*/room_schema/*"

# These files are stored in:
app/schemas/com.example.pantrypure.data.database.PantryDatabase/
```

### Manual Backup (adb)
```bash
# Pull database from device
adb pull /data/data/com.example.pantrypure/databases/pantry_database

# Push back to device (if needed)
adb push pantry_database /data/data/com.example.pantrypure/databases/
```

---

## 7. Testing Database Logic

### Unit Test Example (PantryDao)
```kotlin
@RunWith(AndroidJUnit4::class)
class PantryDaoTest {
    private lateinit var database: PantryDatabase
    private lateinit var pantryDao: PantryDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().context,
            PantryDatabase::class.java
        ).build()
        
        pantryDao = database.pantryDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertAndRetrieveItem() = runBlocking {
        val item = PantryItem(
            name = "Test Item",
            quantity = 10.0,
            unit = PantryUnit.PIECE,
            expiryDate = null,
            category = "Test"
        )
        
        pantryDao.insertItem(item)
        val retrieved = pantryDao.getItemById(1)
        
        assertNotNull(retrieved)
        assertEquals("Test Item", retrieved?.name)
    }
}
```

---

## 8. Debugging with Database Inspector

**Android Studio Built-in Database Inspector**:

1. Run app on device/emulator
2. In Android Studio: **View → Tool Windows → App Inspection**
3. Select "Database Inspector"
4. Browse tables, execute queries, view schema

---

## 9. Migration Checklist

When upgrading database version:

- [ ] Update `@Database(version = X)` in `PantryDatabase.kt`
- [ ] Update Entity classes (`@Entity` classes in `data/model/`)
- [ ] Create `MIGRATION_X_Y` object if needed
- [ ] Register migration in `Room.databaseBuilder(...).addMigrations(...)`
- [ ] Test migration with old app → new app upgrade
- [ ] Test fresh install (DB creation from scratch)
- [ ] Verify Room code generation (rebuild project)
- [ ] Update this documentation

---

## 10. Production Deployment Guidelines

For production releases:

1. **Test migrations thoroughly**
   - Install old app version, create some data
   - Upgrade to new version, verify all data intact

2. **Include multiple migration paths**
   ```kotlin
   .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
   ```

3. **Never use `fallbackToDestructiveMigration()`**

4. **Monitor app crashes in production** (Firebase Crashlytics, etc.)

5. **Document breaking changes** in release notes

---

## Summary: Database File Locations

| Component             | File                                              |
|-----------------------|---------------------------------------------------|
| Entity Definitions    | `data/model/*.kt`                                 |
| DAO Definitions       | `data/dao/*.kt`                                   |
| Database Setup        | `data/database/PantryDatabase.kt`                 |
| Type Converters       | `data/database/PantryTypeConverters.kt`           |
| Migrations            | `data/database/migrations/*.kt` (create new file) |
| Application Bootstrap | `PantryPureApplication.kt`                        |
