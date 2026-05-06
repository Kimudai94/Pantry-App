# PantryPure – Speisekammer-Verwaltungsapp

Ein Android-Projekt für die Verwaltung von Lagerbeständen, Mahlzeiten und Einkaufslisten mit Ablaufdatum-Tracking und Konsumhistorie.

## 🎯 Projektübersicht

PantryPure ist eine Android-App basierend auf Jetpack Compose, die es Benutzern ermöglicht:
- 📦 **Speisekammer-Inventar verwalten** – Items mit Menge, Einheit, Ablaufdatum, Kategorie
- 🍽️ **Mahlzeiten definieren** – Rezepte mit Zutaten-Zuordnung zu Pantry-Items
- 📋 **Einkaufsliste pflegen** – Verfolgung von zu kaufenden Items
- 📊 **Konsumhistorie nachverfolgen** – Aufzeichnung verbrauchter Mengen
- ⏰ **Automatische Expiry-Checks** – Benachrichtigungen bei bald ablaufenden Items

## 📋 Schnellstart

### Voraussetzungen
- Android SDK 28+ (minSdk)
- Android 36+ (targetSdk)
- Java 11+
- Gradle 8.x

### Build & Run
```bash
# App bauen
./gradlew build

# Debug-APK installieren
./gradlew installDebug

# Tests ausführen
./gradlew test
./gradlew connectedAndroidTest
```

### Projekt-Struktur
```
app/
├── src/main/java/com/example/pantrypure/
│   ├── data/                    # Datenebene (Room DB, Repositories)
│   │   ├── dao/                 # Data Access Objects
│   │   ├── database/            # PantryDatabase + TypeConverter
│   │   ├── model/               # Entity-Klassen (@Entity)
│   │   ├── repository/          # PantryRepository (Business Logic)
│   │   └── util/                # UnitConverter, Utilities
│   ├── ui/                      # UI-Ebene (Jetpack Compose)
│   │   ├── navigation/          # Navigation Routes + NavHost
│   │   ├── screen/              # Compose Screens (Add/Edit/List/Detail)
│   │   ├── theme/               # Theme, Colors, Typography
│   │   └── viewmodel/           # ViewModels + State Management
│   ├── util/                    # Globale Utilities
│   │   ├── NotificationHelper   # Notification Channels & Alerts
│   │   └── ExpiryCheckWorker    # Background Task (tägliche Checks)
│   └── MainActivity.kt          # Entry Point, NavHost Setup
├── res/                         # Android Resources
│   ├── values/                  # Strings, Colors, Themes
│   ├── drawable/                # App Icons
│   └── xml/                     # Backup/Data Extraction Rules
└── build.gradle.kts            # Dependencies + Build Config
```

## 🏗️ Architektur

**Layered Architecture** (Modern Android Best Practices):

```
┌─────────────────────────────────┐
│      UI Layer (Compose)         │
│  Screens ↔ ViewModels ↔ States  │
└────────────────┬────────────────┘
                 │
┌────────────────▼────────────────┐
│   Repository Layer              │
│  Business Logic, Data Fetching  │
└────────────────┬────────────────┘
                 │
┌────────────────▼────────────────┐
│   Data Layer                    │
│  Room DAO, Type Converters      │
└─────────────────────────────────┘
```

**Key Patterns**:
- **MVVM** – ViewModel mit StateFlow für reaktive State-Management
- **Repository Pattern** – Abstraktion der Datenzugriffe
- **Room** – Lokale SQLite-Datenbank mit Kotlin Coroutines
- **Jetpack Compose** – Deklarative UI, Recomposition Optimization
- **Flow/StateFlow** – Asynchrone Datenströme, keine Callbacks

## 📱 Hauptfeatures

### 1. Speisekammer (Inventory)
- **Entitäten**: `PantryItem` mit Menge, Einheit (`PantryUnit`), Ablaufdatum
- **Optionen**: Kategorie-Filter, Suchfunktion, Sortierung (Name/Ablaufdatum)
- **Aktionen**: Add/Edit/Delete, "Consume One", Duplicate, Shopping-List-Toggle
- **Alerts**: Overdue/Expiring Soon Status

### 2. Mahlzeiten (Meals)
- **Entitäten**: `Meal` (Name, Kategorie, Anleitung), `MealIngredient` (Verknüpfung zu PantryItems)
- **Kategorien**: Breakfast, Lunch, Dinner, Snack, Other (Enum: `MealCategory`)
- **Funktionen**: Create/Edit/Delete Meal, Add/Remove Ingredients, Browse by Category

### 3. Einkaufsliste (Shopping List)
- **Toggle-Flag**: `isOnShoppingList` auf PantryItem
- **Sicht**: Gefilterte Ansicht aller Items mit aktiviertem Flag
- **Workflow**: Add to List → Buy → Remove from List

### 4. Konsumhistorie (Consumption History)
- **Tracking**: Jede "Consume One" Aktion erzeugt `ConsumptionRecord`
- **Speichert**: ItemId, ItemName, Quantity, Unit, Timestamp
- **Ansicht**: Zeitgestempel Liste
- **Verwaltung**: Clear History Funktion

### 5. Background Expiry Checks
- **Worker**: `ExpiryCheckWorker` (täglich, via WorkManager)
- **Notification**: Benachrichtigung für abgelaufene/bald ablaufende Items
- **Bedingungen**: Läuft nur bei ausreichend Batterie

## 🗄️ Datenbankmodell

Vier Hauptentitäten (Room @Database):

```
PANTRY_ITEM (Master Inventory)
├─ id (PK, Auto-Increment)
├─ name: String
├─ quantity: Double
├─ unit: PantryUnit (Enum-Converter)
├─ expiryDate: Long (Milliseconds Timestamp)
├─ expiryThresholdDays: Int (Default: 3)
├─ isOnShoppingList: Boolean
├─ category: String
└─ notes: String

CONSUMPTION_RECORD (History)
├─ id (PK)
├─ itemId (FK → PANTRY_ITEM)
├─ itemName: String (Snapshot)
├─ quantityConsumed: Double
├─ unit: PantryUnit
└─ timestamp: Long

MEAL (Rezepte)
├─ id (PK)
├─ name: String
├─ category: MealCategory (Enum)
└─ instructions: String

MEAL_INGREDIENT (N:M Zuordnung)
├─ id (PK)
├─ mealId (FK → MEAL)
├─ pantryItemId (FK → PANTRY_ITEM)
└─ requiredQuantity: Double
```

**Wichtige TypeConverter** (in `PantryTypeConverters.kt`):
- `PantryUnit` (Enum) ↔ String
- `MealCategory` (Enum) ↔ String
- `Long` (Timestamps) ↔ Date-Format

## 🔧 Dependencies & Libraries

| Kategorie | Library | Version |
|---|---|---|
| **Core** | androidx.core:core-ktx | Latest |
| **Compose** | androidx.compose.* | BOM managed |
| **Navigation** | androidx.navigation:navigation-compose | Latest |
| **Database** | androidx.room:* | Latest |
| **Async** | kotlinx.coroutines.* | Latest |
| **Background** | androidx.work:work-runtime-ktx | Latest |
| **Camera** | androidx.camera.* | Latest |
| **Location** | com.google.android.gms:play-services-location | Latest |
| **Image Loading** | io.coil-kt:coil-compose | Latest |
| **Networking** | retrofit2, okhttp3, moshi | Latest |
| **UI Material** | androidx.compose.material3.* | Latest |
| **Permissions** | com.google.accompanist:accompanist-permissions | Latest |

**Build Plugins**:
- Kotlin Compose Compiler
- Google Devtools KSP (für Room + Moshi Code-Gen)

## 📐 Wichtige Klassen & Patterns

### ViewModel Pattern (PantryViewModel)
```kotlin
class PantryViewModel(private val repository: PantryRepository) {
    // State Management via MutableStateFlow/StateFlow
    val pantryItems: StateFlow<List<PantryItem>>
    val consumptionHistory: StateFlow<List<ConsumptionRecord>>
    val selectedMealCategory: StateFlow<MealCategory>
    
    // Operations (launch in viewModelScope)
    fun addItem(item: PantryItem)
    fun consumeOne(item: PantryItem)
    fun toggleShoppingListStatus(item: PantryItem)
    // ...
}
```

### Repository Pattern (PantryRepository)
```kotlin
class PantryRepository(
    private val pantryDao: PantryDao,
    private val consumptionDao: ConsumptionDao,
    private val mealDao: MealDao,
    private val mealIngredientDao: MealIngredientDao
) {
    // Public API für Data Access & Business Logic
    fun getAllItems(): Flow<List<PantryItem>>
    suspend fun insertItem(item: PantryItem)
    suspend fun getMealWithIngredients(id: Long): MealWithIngredients?
    // ...
}
```

### Entity + DAO Pattern (Room)
```kotlin
@Entity(tableName = "pantry_items")
data class PantryItem(...)

@Dao
interface PantryDao {
    @Insert
    suspend fun insertItem(item: PantryItem)
    
    @Query("SELECT * FROM pantry_items")
    fun getAllItems(): Flow<List<PantryItem>>
}
```

## 🚀 Entwicklung & Anpassungen

### Neue Feature hinzufügen (Checklist)
1. **Modell**: Neue @Entity Klasse in `data/model/`
2. **DAO**: Interface in `data/dao/` mit @Query/@Insert/@Update/@Delete
3. **Repository**: Public API Methoden hinzufügen
4. **ViewModel**: State (StateFlow/MutableStateFlow) + Methods
5. **UI Screens**: Compose-Funktionen in `ui/screen/`
6. **Navigation**: Route in `ui/navigation/Screen.kt` + NavHost `composable{}`
7. **Tests**: Unit Tests in `test/`, Instrumented Tests in `androidTest/`

### Datenbankmigrationen
- Room-Versionen in `PantryDatabase.kt` Annotation: `version = X`
- Verwende `fallbackToDestructiveMigration(false)` → erfordert explizite Migration
- Migration-Logik in `@Migration` Klasse oder manuelle SQL-Scripts

### Testing
```bash
# Unit Tests (JVM)
./gradlew test

# Instrumented Tests (Device/Emulator)
./gradlew connectedAndroidTest

# Test Coverage Report
./gradlew testDebugUnitTest --tests "*Test"
```

## ⚙️ Konfiguration

### Notification Channels
Wird in `PantryPureApplication.onCreate()` initialisiert via `NotificationHelper.createNotificationChannel()`

### WorkManager Configuration
- `ExpiryCheckWorker` läuft täglich
- Bedingung: Batterie nicht niedrig (requiresBatteryNotLow)
- Policy: `KEEP` (keine Duplikate)

### Gradle Properties
```gradle.properties
org.gradle.jvmargs=-Xmx2048m
android.useAndroidX=true
```

## 🔍 Debugging & Troubleshooting

### Common Issues
| Problem | Lösung |
|---|---|
| **Room Migration Error** | Erhöhe DB Version + schreibe Migration |
| **Compose Recomposition Loop** | Prüfe StateFlow Updates, nutze `.distinctUntilChanged()` |
| **Background Work nicht triggert** | Prüfe WorkManager-Constraints + Device-Power-State |
| **Permission Denial** | Nutze `rememberPermissionState()`, explizite Requests in UI |

### Logs
```bash
# Logcat filtern
adb logcat com.example.pantrypure:V *:S

# Room SQL Logs
adb logcat Room:D
```

---

**Weitere Dokumentation**: Siehe `doc/` Folder für Mermaid-Diagramme:
- `ERM.mermaid` – Entity-Relationship-Diagramm
- `PlanAndCookMeal.mermaid` – Mahlzeiten-Workflow
- `UpdatePantry.mermaid` – Speisekammer-Update-Flow
- `UpdateShoppingList.mermaid` – Einkaufslisten-Flow
