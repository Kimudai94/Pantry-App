# Documentation Index

Vollständiger Überblick über die PantryPure-Dokumentation. Finde schnell die richtige Datei für deine Frage.

---

## 📚 Dokumentationsdateien

### 1. **README.md** – Start Here!
   - 📍 Projektübersicht und Quick Start
   - 🎯 Hauptfeatures auf einen Blick
   - 🚀 Build & Run Anweisungen
   - 🏗️ Architektur-Überblick (High-Level)
   - 📱 Feature-Zusammenfassung
   
   **Wann lesen**:
   - Ich bin neu im Projekt
   - Ich brauche einen schnellen Überblick
   - Ich möchte das Projekt lokal starten

### 2. **ARCHITECTURE.md** – Deep Dive into Code Structure
   - 🔍 Detaillierte Architektur (7 Schichten)
   - 📦 Data Layer (Entities, DAOs, Repository)
   - 🎨 Domain & Presentation Layer
   - 🔧 Utilities & Initialization
   - 📊 Workflow-Beispiele (Add Item, Consume, etc.)
   - 💡 Best Practices für LLM-Entwicklung
   
   **Wann lesen**:
   - Ich muss Code verstehen oder schreiben
   - Ich möchte das Projektlayout kennen
   - Ich bin ein LLM, das Code analysiert

### 3. **DATABASE.md** – Database Schema & Migrations
   - 📋 Entity-Relationship-Diagramm
   - 🗄️ Detailliertes Schema (alle 4 Tabellen)
   - 🔄 Type Converters (Enum-Serialisierung)
   - ⚙️ Migration Guide (Step-by-Step)
   - 🧪 Backup & Debugging
   - ✅ Migrations-Checkliste
   
   **Wann lesen**:
   - Ich muss eine neue Entity hinzufügen
   - Ich brauche eine Database-Migration
   - Ich debugge DB-Fehler
   - Ich möchte das Schema verstehen

### 4. **DEVELOPMENT.md** – Local Setup & Debugging
   - 💻 Voraussetzungen & Environment-Setup
   - 🔧 Build Varianten & Installation
   - 🔄 Daily Development Loop
   - 🐛 Debugging-Techniken (Logcat, Breakpoints)
   - ✨ Hot Reload & Compose Preview
   - 📝 Code Quality & Linting
   - 🔑 Signing für Release
   - 🎬 Deployment-Anweisungen
   
   **Wann lesen**:
   - Ich richte das Projekt zum ersten Mal auf
   - Ich brauche Debugging-Tipps
   - Ich möchte die App deployen
   - Ich führe Tests aus

### 5. **FEATURES.md** – Feature & API Reference
   - 🎯 Feature-Inventory (alle Features)
   - 🗺️ Navigation Map (alle Screens)
   - 📱 Screen Details (UI Komponenten, Actions)
   - 🔌 ViewModel API (alle public Methoden)
   - 📊 Repository API (alle Operationen)
   - 📦 Data Models & Enums
   - ⏱️ Background Jobs (WorkManager)
   - 🔔 Notifications
   - 🛠️ Utility Functions
   - 💻 Common Use Cases (mit Code)
   
   **Wann lesen**:
   - Ich muss eine neue UI-Komponente erstellen
   - Ich brauche die ViewModel/Repository API
   - Ich möchte verstehen, wie Screens funktionieren
   - Ich suche Code-Beispiele für Features

### 6. **LLM_GUIDE.md** – Spezifisch für KI-Assistenten
   - ⚡ Quick Reference (60 Sekunden)
   - 📂 File Locations (Schnell-Zugriff)
   - 📋 Database Entities (Überblick)
   - 🧭 Projekt-Navigation (wie man Code findet)
   - 💻 Code-Patterns & Konventionen
   - ✅ Häufige Aufgaben & Lösungsvorlagen
   - 🐛 Debugging-Tipps für LLMs
   - ❌ Anti-Patterns zu vermeiden
   - ✓ Checkliste für neue Features
   - 🔗 Quick Links
   
   **Wann lesen**:
   - Ich bin ein LLM (Claude, OpenClaude, etc.)
   - Ich muss schnell Features hinzufügen
   - Ich debugge Probleme
   - Ich brauche Code-Vorlagen

---

## 🎯 Navigiere nach deiner Frage

### 🚀 "Ich starte gerade mit diesem Projekt"
1. Lese **README.md** (5 Min)
2. Lese **DEVELOPMENT.md** Sections 1-3 (10 Min)
3. Führe aus: `./gradlew build && ./gradlew installDebug` (10 Min)
4. Öffne App und probiere es aus (5 Min)
5. Lese **ARCHITECTURE.md** (20 Min)

### 💻 "Ich muss einen neuen Screen bauen"
1. Öffne **LLM_GUIDE.md** → Task 4: Neuer Screen
2. Lese **FEATURES.md** → Screens & Navigation (um andere Screens zu sehen)
3. Lese **ARCHITECTURE.md** → Presentation Layer (für Patterns)
4. Schreibe den Code
5. Prüfe **LLM_GUIDE.md** → Checkliste

### 📦 "Ich muss eine neue Entity & Migration erstellen"
1. Öffne **LLM_GUIDE.md** → Task 1: Neue Entity
2. Lese **DATABASE.md** → Schema & Migrations
3. Schreibe Entity, DAO, Migration
4. Erhöhe DB-Version in `PantryDatabase.kt`
5. Registriere Migration in `PantryPureApplication.kt`
6. Teste: `./gradlew build && ./gradlew installDebug`

### 🐛 "Die App crasht!"
1. Öffne Logcat: `adb logcat com.example.pantrypure:V`
2. Suche nach Exception
3. Je nach Fehler:
   - **Room Migration Error** → **DATABASE.md**
   - **UI Not Updating** → **LLM_GUIDE.md** Debugging
   - **Navigation Error** → **FEATURES.md** Navigation Map
   - **Kotlin Compilation Error** → **ARCHITECTURE.md** Code Patterns

### 📊 "Ich muss eine DAO Query hinzufügen"
1. Öffne **LLM_GUIDE.md** → Task 2: Neue DAO Query
2. Schreibe Query in DAO Interface
3. Schreibe Repository Methode
4. Schreibe ViewModel State/Methode
5. Verwende im Screen

### 🔄 "Ich muss Async Operationen verstehen"
1. Lese **ARCHITECTURE.md** → Presentation Layer (ViewModel Pattern)
2. Lese **LLM_GUIDE.md** → Code-Patterns 1 & 2 (Coroutines & StateFlow)
3. Lese **DEVELOPMENT.md** → Compose Preview & Hot Reload

### 🧪 "Wie schreibe ich Tests?"
1. Lese **DEVELOPMENT.md** Section 7: Testing
2. Lese **ARCHITECTURE.md** → ExampleInstrumentedTest
3. Schreibe Test
4. Führe aus: `./gradlew test` oder `./gradlew connectedAndroidTest`

### 🚀 "Ich deploye zum ersten Mal"
1. Lese **DEVELOPMENT.md** Section 9: Signing for Release
2. Lese **DEVELOPMENT.md** Section 10: Deployment
3. Lese **README.md** Troubleshooting

---

## 📚 By Topic

### Data Layer
- **Entities definieren** → DATABASE.md Section 1
- **DAOs schreiben** → ARCHITECTURE.md Section 2.2
- **Migrations erstellen** → DATABASE.md Section 3
- **Type Converters** → DATABASE.md Section 2

### Business Logic
- **Repository Pattern** → ARCHITECTURE.md Section 2.4
- **Operations hinzufügen** → LLM_GUIDE.md Task 2

### Presentation Layer
- **Screens bauen** → FEATURES.md Section 2, ARCHITECTURE.md Section 4.3
- **ViewModel State** → ARCHITECTURE.md Section 4.2, LLM_GUIDE.md Code Patterns 2
- **Navigation** → FEATURES.md Section 2, ARCHITECTURE.md Section 4.1

### State Management
- **StateFlow/MutableStateFlow** → LLM_GUIDE.md Code Pattern 2
- **Reactive Filtering** → LLM_GUIDE.md Code Pattern 3
- **Coroutines** → LLM_GUIDE.md Code Pattern 1

### Background Jobs
- **WorkManager** → FEATURES.md Section 6
- **ExpiryCheckWorker** → ARCHITECTURE.md Section 5

### Testing
- **Unit Tests** → DEVELOPMENT.md Section 7
- **Instrumented Tests** → DEVELOPMENT.md Section 7

### Debugging
- **Logcat** → DEVELOPMENT.md Section 5
- **Database Inspector** → DATABASE.md Section 8
- **Common Issues** → LLM_GUIDE.md Debugging Tipps

### Patterns & Best Practices
- **Code Patterns** → LLM_GUIDE.md Section 3
- **Anti-Patterns** → LLM_GUIDE.md Section 4
- **Best Practices** → ARCHITECTURE.md Section 8

---

## 🔗 Quick Navigation

| Frage | Antwort | Datei |
|-------|--------|-------|
| Wo sind die Screens? | `ui/screen/` | ARCHITECTURE.md, FEATURES.md |
| Wo ist die Navigation? | `ui/navigation/Screen.kt` | ARCHITECTURE.md 4.1, FEATURES.md 2 |
| Wo sind die Models? | `data/model/` | ARCHITECTURE.md 2.1, DATABASE.md 1 |
| Wo sind die DAOs? | `data/dao/` | ARCHITECTURE.md 2.2, DATABASE.md 1 |
| Wo ist das Repository? | `data/repository/PantryRepository.kt` | ARCHITECTURE.md 2.4, FEATURES.md 4 |
| Wo ist der ViewModel? | `ui/viewmodel/PantryViewModel.kt` | ARCHITECTURE.md 4.2, FEATURES.md 3 |
| Wie teste ich lokal? | `./gradlew build && ./gradlew installDebug` | DEVELOPMENT.md 3 |
| Wie debugge ich? | `adb logcat` + Android Studio | DEVELOPMENT.md 5 |
| Wie deploye ich? | Signing + Upload | DEVELOPMENT.md 9-10 |
| Wie füge ich eine Entity hinzu? | Siehe Checkliste | LLM_GUIDE.md Task 1, DATABASE.md 3 |

---

## 📖 Reading Paths

### Path 1: For New Developers (2-3 hours)
1. README.md (10 min)
2. DEVELOPMENT.md: Sections 1-4 (30 min)
3. ARCHITECTURE.md: Full (60 min)
4. LLM_GUIDE.md: Sections 1-3 (30 min)

### Path 2: For LLM Assistants (1 hour)
1. LLM_GUIDE.md: Full (30 min)
2. ARCHITECTURE.md: Quick reference (20 min)
3. FEATURES.md: API sections (10 min)

### Path 3: For Database Work (30 min)
1. DATABASE.md: Full (20 min)
2. ARCHITECTURE.md: Section 2 (10 min)

### Path 4: For Feature Implementation (1-2 hours)
1. LLM_GUIDE.md: Relevant Task (15 min)
2. FEATURES.md: Related Screens/API (20 min)
3. ARCHITECTURE.md: Related Sections (20 min)
4. Code Review & Implement (30+ min)

### Path 5: For Debugging (15-30 min)
1. DEVELOPMENT.md: Section 5 (10 min)
2. LLM_GUIDE.md: Debugging Tips (5 min)
3. Search relevant docs based on error

---

## 🎯 Common Workflows

### Workflow 1: Add a new field to PantryItem
```
1. LLM_GUIDE.md → Task 1
2. DATABASE.md → Schema section
3. DATABASE.md → Migration section
4. DATABASE.md → Testing section
5. DEVELOPMENT.md → Build & Run
```

### Workflow 2: Create a new Screen
```
1. LLM_GUIDE.md → Task 4
2. FEATURES.md → Screen Details section
3. ARCHITECTURE.md → Presentation Layer section
4. DEVELOPMENT.md → Build & Run
```

### Workflow 3: Add a new ViewModel method
```
1. LLM_GUIDE.md → Task 3
2. FEATURES.md → ViewModel API section
3. DEVELOPMENT.md → Build & Run
```

### Workflow 4: Debug a crash
```
1. DEVELOPMENT.md → Debugging section
2. Check Logcat
3. Search relevant docs (DATABASE/ARCHITECTURE/LLM_GUIDE)
4. Fix and test
```

---

## 🤖 For LLMs (OpenClaude, Claude, etc.)

**Diese Dateien lesen in dieser Reihenfolge**:
1. **LLM_GUIDE.md** (30 min) – Alles was du wissen musst
2. **ARCHITECTURE.md** (30 min) – Code-Struktur verstehen
3. **DATABASE.md** (15 min) – Schema referenzieren
4. **FEATURES.md** (als Referenz) – Screens & API nachschlagen

**Immer prüfen vor Code-Änderungen**:
- Ist mein Code ein erwartetes Pattern? (LLM_GUIDE.md Patterns)
- Vermeidet es Anti-Patterns? (LLM_GUIDE.md Anti-Patterns)
- Passt es zur Architektur? (ARCHITECTURE.md)
- Ist die Checkliste erfüllt? (LLM_GUIDE.md Checkliste)

---

## 📞 When In Doubt

- **Crashes or Errors?** → DEVELOPMENT.md Section 5
- **Database Issues?** → DATABASE.md Sections 7-8
- **Architecture Questions?** → ARCHITECTURE.md Full
- **How to do X?** → LLM_GUIDE.md Tasks
- **Where is Y?** → LLM_GUIDE.md File Locations
- **API Reference?** → FEATURES.md Full

---

## 📝 Keep This Index Updated

Wenn neue Dokumentationsdateien hinzugefügt werden:
1. Trage sie in den Abschnitt 📚 Dokumentationsdateien ein
2. Trage sie in die Navigations-Tabelle ein
3. Trage sie in relevante Topic-Gruppen ein

---

**Last Updated**: May 5, 2026  
**Projekt**: PantryPure Android App  
**Tech Stack**: Kotlin, Jetpack Compose, Room, MVVM
