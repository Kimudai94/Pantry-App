# Development Guide & Setup

Setup-Anweisungen, Development Workflow und Troubleshooting für PantryPure-Entwicklung.
**Note**: In allen Code-Beispielen unten bedeutet `<PROJECT_ROOT>` das Wurzelverzeichnis des Projekts `c:\Tools\Pantry-App`.

---

## 1. Prerequisites

### System Requirements
- **OS**: Windows, macOS, or Linux
- **JDK**: Java 11+ (OpenJDK oder Oracle)
- **Android SDK**: API Level 36+ (targetSdk)
- **Gradle**: 8.x (automatically managed by wrapper)
- **Android Studio**: 2023.1.x or newer (recommended)

### Check Environment
```bash
# Java Version
java -version
# Should output Java 11+

# Gradle (via wrapper)
cd <PROJECT_ROOT>
./gradlew --version
```

---

## 2. Initial Setup

### Clone / Open Project

```bash
# If fresh clone from git
git clone <repo-url> <PROJECT_ROOT>
cd <PROJECT_ROOT>

# Or open existing project in Android Studio
# File → Open → Select <PROJECT_ROOT> folder
```

### Verify Gradle Sync

1. Open project in Android Studio
2. **File → Sync Now** (or Ctrl+Shift+F5)
3. Wait for Gradle sync to complete
4. Verify `libs.versions.toml` is correctly resolved

### Check Dependencies

```bash
./gradlew dependencies --configuration debugRuntimeClasspath | grep -E "androidx|room|compose"
```

---

## 3. Build Variants

### Debug Build (Development)
```bash
# Build debug APK
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Release Build (Production)
```bash
# Build release APK (unsigned)
./gradlew assembleRelease

# Note: Requires signing configuration (see Signing section)
```

### Install to Device/Emulator
```bash
# Install debug build
./gradlew installDebug

# Or manually with adb
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Run App Directly
```bash
# Build, install, and launch (with Logcat)
./gradlew runDebug

# Alternative: Run from Android Studio
# Device Manager → Select Emulator/Device → Play button
```

---

## 4. Development Workflow

### Daily Development Loop

#### 1. Make Code Changes
```
Edit files in app/src/main/java/com/example/pantrypure/
```

#### 2. Rebuild and Test
```bash
# Incremental build (fast)
./gradlew build

# Full rebuild (if incremental fails)
./gradlew clean build
```

#### 3. Run on Device
```bash
./gradlew installDebug

# View logs in real-time
adb logcat com.example.pantrypure:V
```

#### 4. Debug Issues
- Android Studio: **Run → Debug** (F5)
- Set breakpoints, inspect variables
- View Logcat output in **Logcat** view

#### 5. Run Tests
```bash
# Unit tests (JVM, fast)
./gradlew test

# Instrumented tests (Device/Emulator, slow)
./gradlew connectedAndroidTest

# Specific test class
./gradlew test --tests "*.PantryDaoTest"
```

### Hot Reload / Compose Preview

**Android Studio Compose Preview**:
- Edit a `@Composable` function
- Right-click function name → **Compose Preview**
- Preview updates live as you edit (if using stable compose version)

**Live Edit (Limited)**:
- Modify UI code, Save
- App may hot-reload changes without full rebuild
- If stuck, use **Build → Rebuild Project**

---

## 5. Debugging

### Logcat Filtering
```bash
# All logs from app
adb logcat com.example.pantrypure:V

# Filter by tag
adb logcat | grep "PantryViewModel"

# Real-time logs with timestamps
adb logcat -v time com.example.pantrypure:V
```

### Common Log Tags to Monitor

| Tag | Source | Purpose |
|-----|--------|---------|
| `Room` | Database | SQL queries, migrations |
| `PantryViewModel` | ViewModel | State changes, operations |
| `WorkManager` | Background Jobs | ExpiryCheckWorker logs |
| `Compose` | UI | Recomposition logs |

### Android Studio Debugger

1. **Set Breakpoint**: Click line number in editor
2. **Run → Debug 'app'** (Shift+F9)
3. **Variables Panel**: Inspect object state
4. **Evaluate Expression**: Execute code mid-breakpoint
5. **Step Over/Into**: Navigate code execution

### Device Logs

```bash
# Capture crash logs
adb logcat > crash_log.txt
# Trigger crash, then Ctrl+C to save

# View last 100 lines
adb logcat -t 100
```

---

## 6. Adding New Features

### Step-by-Step Guide

#### Example: Add "Last Used Date" to PantryItem

##### 1. Update Entity
```kotlin
// data/model/PantryItem.kt
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
    val lastUsedDate: Long? = null  // NEW FIELD
)
```

##### 2. Create Database Migration
```kotlin
// data/database/Migration6To7.kt
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            ALTER TABLE pantry_items 
            ADD COLUMN lastUsedDate INTEGER DEFAULT NULL
        """)
    }
}
```

##### 3. Update DAO if Needed
```kotlin
// data/dao/PantryDao.kt
@Query("SELECT * FROM pantry_items ORDER BY lastUsedDate DESC")
fun getItemsByLastUsed(): Flow<List<PantryItem>>
```

##### 4. Update Repository
```kotlin
// data/repository/PantryRepository.kt
fun getItemsByLastUsed(): Flow<List<PantryItem>> = 
    pantryDao.getItemsByLastUsed()
```

##### 5. Update ViewModel
```kotlin
// ui/viewmodel/PantryViewModel.kt
fun markItemAsUsed(item: PantryItem) {
    viewModelScope.launch {
        repository.updateItem(
            item.copy(lastUsedDate = System.currentTimeMillis())
        )
    }
}
```

##### 6. Update UI Screen
```kotlin
// ui/screen/InventoryListScreen.kt
PantryItemRow(
    item = item,
    onConsume = {
        viewModel.consumeOne(item)
        viewModel.markItemAsUsed(item)
    }
)
```

##### 7. Register Migration
```kotlin
// PantryPureApplication.kt
private val database by lazy {
    Room.databaseBuilder(this, PantryDatabase::class.java, "pantry_database")
        .addMigrations(MIGRATION_5_6, MIGRATION_6_7)  // Add here
        .fallbackToDestructiveMigration(false)
        .build()
}
```

##### 8. Update Database Version
```kotlin
// data/database/PantryDatabase.kt
@Database(..., version = 7)  // Was 6
```

##### 9. Test
```bash
./gradlew build
./gradlew installDebug
# Test feature in app
```

---

## 7. Testing

### Unit Tests (JVM)

Located in: `app/src/test/java/com/example/pantrypure/`

```kotlin
// ExampleUnitTest.kt
class PantryRepositoryTest {
    private lateinit var repository: PantryRepository
    
    @Before
    fun setup() {
        val mockPantryDao = mock<PantryDao>()
        // ... setup mocks
        repository = PantryRepository(mockPantryDao, ...)
    }
    
    @Test
    fun testAddItem() = runBlocking {
        val item = PantryItem(name = "Test", ...)
        repository.insertItem(item)
        verify(mockPantryDao).insertItem(item)
    }
}
```

Run tests:
```bash
./gradlew test
# Results: app/build/reports/tests/testDebugUnitTest/index.html
```

### Instrumented Tests (Device/Emulator)

Located in: `app/src/androidTest/java/com/example/pantrypure/`

```kotlin
// PantryDaoTest.kt
@RunWith(AndroidJUnit4::class)
class PantryDaoTest {
    private lateinit var database: PantryDatabase
    
    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            PantryDatabase::class.java
        ).build()
    }
    
    @Test
    fun testInsertRetrieve() = runBlocking {
        val item = PantryItem(name = "Tomato", ...)
        database.pantryDao().insertItem(item)
        
        val result = database.pantryDao().getItemById(1)
        assertNotNull(result)
    }
}
```

Run tests:
```bash
# Ensure device/emulator is running
./gradlew connectedAndroidTest
# Results: app/build/reports/androidTests/connected/
```

### Test Coverage Report

```bash
# Generate coverage for unit tests
./gradlew testDebugUnitTestCoverage

# View report
open app/build/reports/coverage/debug/index.html
```

---

## 8. Code Quality & Linting

### Kotlin Linter (Built-in)

Android Studio includes built-in Kotlin linting. View issues:

**View → Tool Windows → Problems** (or Alt+6)

Common issues:
- ❌ Unused imports
- ❌ Unused variables
- ⚠️ Naming conventions
- ⚠️ Potential null pointer dereferences

### Manual Lint Check via Gradle

```bash
./gradlew lint

# View HTML report
open app/build/reports/lint-results.html
```

### Code Formatting (Android Studio)

```bash
# Format entire project
Code → Reformat Code (Ctrl+Alt+L)

# Or via Gradle (requires additional setup)
./gradlew spotlessApply
```

---

## 9. Signing for Release

### Generate Signing Key

```bash
# Windows PowerShell
keytool -genkey -v -keystore pantry-release-key.jks `
  -keyalg RSA -keysize 2048 -validity 10000 `
  -alias pantry_release_key

# macOS / Linux
keytool -genkey -v -keystore pantry-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias pantry_release_key
```

### Configure Signing in Gradle

Edit `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "pantry-release-key.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

### Build Release APK

```bash
# Set environment variables
$env:KEYSTORE_PATH = "C:\path\to\pantry-release-key.jks"
$env:KEYSTORE_PASSWORD = "your_keystore_password"
$env:KEY_ALIAS = "pantry_release_key"
$env:KEY_PASSWORD = "your_key_password"

# Build
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
```

---

## 10. Deployment

### Local Testing Before Release

```bash
# Install release APK
adb install -r app/build/outputs/apk/release/app-release.apk

# Test thoroughly
# - All features work
# - No crashes
# - Performance acceptable
# - Database migrations work
```

### Upload to Google Play Store

1. Create developer account: https://play.google.com/console
2. Create new app
3. Fill metadata (screenshots, description, etc.)
4. Upload signed APK / AAB
5. Review and publish

### Staged Rollout

For production releases:
1. Publish to 10% of users first
2. Monitor crash rates (Firebase Crashlytics)
3. If stable, expand to 50%, then 100%

---

## 11. Version Management

### Semantic Versioning

PantryPure uses `MAJOR.MINOR.PATCH`:

- `1.0.0` – Initial release
- `1.1.0` – New feature (shopping list filtering)
- `1.1.1` – Bug fix (crash on old Android)
- `2.0.0` – Breaking changes (new database schema)

### Update Version Code & Name

Edit `app/build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        versionCode = 2        // Increment for each release
        versionName = "1.1.0"  // User-facing version
    }
}
```

**versionCode rules**:
- Must be an integer
- Must increase with each release
- Google Play enforces this

---

## 12. Troubleshooting

### Common Build Issues

| Issue | Solution |
|-------|----------|
| **Gradle sync fails** | `File → Invalidate Caches → Restart` |
| **Unresolved imports** | `Build → Rebuild Project` |
| **Room compilation error** | Ensure KSP plugin applied, check `@Database` version |
| **OutOfMemory during build** | Increase heap: `org.gradle.jvmargs=-Xmx4096m` in `gradle.properties` |

### Common Runtime Issues

| Issue | Solution |
|-------|----------|
| **App crashes on startup** | Check Logcat for exceptions, verify DB migration |
| **Navigation crashes** | Verify route names match `Screen.kt` definitions |
| **UI not updating** | Check if `StateFlow` collection is active (`.collectAsState()`) |
| **Database locked** | Multiple writes conflicting, add coroutine serialization |

### Performance Issues

#### Slow Database Queries
```kotlin
// Use @Transaction for complex multi-table queries
@Transaction
@Query("""...""")
suspend fun complexQuery(): List<ComplexResult>
```

#### Recomposition Loop
```kotlin
// Debug: Add logging to see what's triggering recomposition
LaunchedEffect(key1 = items) {
    Log.d("Compose", "Items changed: ${items.size}")
}
```

#### Memory Leaks
- Avoid holding references to Context in ViewModels
- Use `lifecycle-aware` components
- Cancel Coroutines in `onCleared()`

---

## 13. IDE Shortcuts (Android Studio)

| Shortcut | Action |
|----------|--------|
| **Ctrl+Space** | Autocomplete |
| **Ctrl+Alt+L** | Format code |
| **Ctrl+/** | Comment/uncomment |
| **Ctrl+B** | Go to definition |
| **Ctrl+Alt+B** | Go to implementation |
| **Ctrl+Shift+F** | Find in files |
| **Shift+F9** | Debug app |
| **Shift+F5** | Stop debug |
| **Ctrl+Shift+F5** | Gradle sync |

---

## 14. Git Workflow

### Commit Convention

Use Conventional Commits:

```bash
# Feature
git commit -m "feat: add last used date to pantry items"

# Bug fix
git commit -m "fix: prevent negative quantities"

# Refactor
git commit -m "refactor: extract ItemRow composable"

# Docs
git commit -m "docs: update database schema documentation"

# Tests
git commit -m "test: add PantryViewModel tests"
```

### Branching Strategy

```bash
# Feature branch
git checkout -b feature/shopping-list-filters
# ... make changes
git push origin feature/shopping-list-filters
# Create Pull Request

# Hotfix
git checkout -b hotfix/crash-on-delete
# ... fix critical bug
git push origin hotfix/crash-on-delete
```

---

## 15. Documentation

### Inline Documentation

Add docstrings to public functions:

```kotlin
/**
 * Marks a pantry item as consumed and records it in consumption history.
 * 
 * @param item The pantry item to consume
 * @throws IllegalArgumentException if item quantity is already zero
 */
fun consumeOne(item: PantryItem) {
    // Implementation
}
```

### Updating Project Documentation

- **README.md** – Overview, quick start, features
- **ARCHITECTURE.md** – Code structure, patterns
- **DATABASE.md** – Schema, migrations
- **DEVELOPMENT.md** (this file) – Setup, debugging, deployment

---

## Next Steps

1. **Get familiar with the codebase**: Read ARCHITECTURE.md
2. **Setup local environment**: Follow section 2-3 above
3. **Run tests**: `./gradlew test`
4. **Make first change**: Add a small feature following section 6
5. **Deploy**: Test locally (section 10), then to device

For questions, check the **Troubleshooting** section or open an issue in the project repository.
