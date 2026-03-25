# Pantry App 🥬

Eine native Android-Anwendung zum Tracken deines Lebensmittelbestands zuhause. Nutze die App offline auf deinem Smartphone und behalte stets den Überblick über Menge, Mindesthaltbarkeitsdatum und Preis deiner Produkte.

## Features

- ✅ **Offline-First** - Funktioniert vollständig ohne Internetverbindung
- ✅ **Native Android App** - Optimiert für Performance auf Smartphones
- ✅ **Produkte verwalten** - Name, Menge, Einheit, MHD und Preis erfassen
- ✅ **CRUD-Operationen** - Erstellen, Lesen, Aktualisieren, Löschen
- ✅ **MHD-Warnung** - Automatische Warnung bei ablaufenden Produkten
- ✅ **Suche & Filter** - Schnelle Produktsuche und Filterfunktion
- ✅ **Sortieren** - Nach Name, MHD oder Preis sortieren
- ✅ **Lokale Datenspeicherung** - SQLite Datenbank auf dem Gerät

## Technologie Stack

- **Framework**: React Native
- **Datenbank**: SQLite3 (offline)
- **Sprache**: TypeScript
- **QA**: Jest, ESLint
- **CI/CD**: GitHub Actions (automatischer APK-Build)

## Installation

### Voraussetzungen
- Node.js (v18 oder höher)
- Android SDK / Android Studio
- Java Development Kit (JDK 11+)
- npm oder yarn

### Schritt-für-Schritt

1. **Repository klonen**
   ```bash
   git clone https://github.com/Kimudai94/Pantry-App.git
   cd Pantry-App
   ```

2. **Abhängigkeiten installieren**
   ```bash
   npm install
   ```

3. **App auf Android-Emulator starten**
   ```bash
   npm run android
   ```

   Oder auf deinem echten Android-Gerät (mit USB-Debugging):
   ```bash
   npm run android
   ```

### Manuellen APK bauen

**Debug APK:**
```bash
npm run build:android:debug
```

**Release APK:**
```bash
npm run build:android
```

Die APK-Dateien findest du in:
- Debug: `android/app/build/outputs/apk/debug/app-debug.apk`
- Release: `android/app/build/outputs/apk/release/app-release.apk`

## Verwendung

### Neues Produkt hinzufügen
1. Tippe auf die **+** Schaltfläche in der App
2. Fülle die Produktinformationen aus:
   - **Produktname** (erforderlich)
   - **Menge** (erforderlich)
   - **Einheit** (g, ml, l, kg, Stück, Packung)
   - **MHD** (optional, Format: YYYY-MM-DD)
   - **Letzter Preis** (optional)
3. Tippe "Hinzufügen"

### Produkt bearbeiten
1. Tippe auf das 📝 Symbol bei einem Produkt
2. Passe die Informationen an
3. Tippe "Speichern"

### Produkt löschen
1. Tippe auf das 🗑️ Symbol bei einem Produkt
2. Bestätige das Löschen

### Produkte suchen und sortieren
- **Suche**: Gib einen Produktnamen in das Suchfeld ein
- **Sortierung**: Wähle zwischen Name, MHD oder Preis

## Projektstruktur

```
Pantry-App/
├── src/
│   ├── database/
│   │   └── db.ts              # SQLite CRUD Operationen
│   ├── screens/
│   │   ├── HomeScreen.tsx      # Hauptscreen mit Produktliste
│   │   ├── AddProductScreen.tsx   # Neues Produkt hinzufügen
│   │   └── EditProductScreen.tsx  # Produkt bearbeiten
│   ├── components/
│   │   └── ProductCard.tsx     # Wiederverwendbare Produktkarte
│   ├── types/
│   │   └── product.ts          # TypeScript Interfaces
├── __tests__/
│   ├── database.test.ts        # Database Tests
│   └── app.test.ts             # App Tests
├── App.tsx                      # Hauptkomponente
├── index.js                     # Entry Point
├── .github/workflows/
│   └── build.yml               # GitHub Actions Workflow
├── package.json
├── tsconfig.json
├── jest.config.js
└── README.md
```

## Development

### Tests ausführen
```bash
npm test
```

### Tests im Watch-Modus
```bash
npm run test:watch
```

### Code-Qualität prüfen
```bash
npm run lint
```

## GitHub Actions Workflow

Der automatische **CI/CD Workflow** läuft bei jedem Push und Pull Request:

1. **Linting & Formatierung** - ESLint überprüft Code-Qualität
2. **Tests** - Jest führt Tests aus
3. **APK Build** - Bei erfolgreicher Prüfung wird die APK gebaut
4. **Artifact Upload** - APK-Dateien als Artifacts verfügbar

**APK-Dateien** findest du unter "Actions → Build → Artifacts" im GitHub Repository.

## Anforderungen für Release

- ✅ Alle Tests bestanden
- ✅ ESLint ohne kritische Fehler
- ✅ APK erfolgreich gebaut

## Browser-Unterstützung

- Android 6.0+ (API Level 23+)

## Lizenz

MIT License

## Support

Bei Fehlern oder Fragen bitte ein Issue erstellen:
https://github.com/Kimudai94/Pantry-App/issues

## Autor

Erstellt mit ❤️ für besseren Lebensmittelbestand auf dem Smartphone!

---

**Hinweis**: Diese App speichert alle Daten lokal auf deinem Gerät. Keine Cloud-Synchronisierung erforderlich!
