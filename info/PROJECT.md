# AllerPaw – Projektbeschreibung (v0.1)

> **Dieses Dokument als Kontext in jeden Prompt einfügen, wenn nur einzelne Module geteilt werden.**  
> Letzte Aktualisierung: 2026-04-23 · Status: v0.1 – Android-Migration begonnen

---

## Überblick

**AllerPaw** ist eine native Android-App zur Ernährungs- und Gesundheitsverwaltung für Hunde. Sie richtet sich an Hundebesitzer, die BARF-Ernährung betreiben und Symptome sowie Allergien systematisch dokumentieren wollen.

Die App nutzt **Google Sheets als Datenbank** (Sheets API v4) — ein Backend gibt es nicht. Die bisherige Web-App (v2.3.1, GitHub Pages) wird vollständig als Android-App neu implementiert. Bestehende Spreadsheets bleiben ohne Änderung kompatibel.

---

## Tech-Stack

| Bereich | Technologie |
|---------|-------------|
| Plattform | Android (API 26+) |
| Sprache | Kotlin |
| UI | Jetpack Compose |
| HTTP / API | Retrofit 2 + OkHttp |
| Auth | Google Sign-In / Credential Manager |
| Lokale DB / Cache | Room (SQLite) |
| Settings | DataStore |
| Charts | Vico (Compose-nativ) oder MPAndroidChart |
| DI | Hilt |
| Async | Kotlin Coroutines + Flow |
| Datenbank (extern) | Google Sheets API v4 (REST) |
| Wetter | BrightSky API (DWD-Daten) |
| Pollen | DWD OpenData + Open-Meteo Air Quality API |
| Nährstoffberechnung | NRC 2006 (39 Nährstoffe), ergänzt durch AAFCO / FEDIAF |
| Build | Gradle (Kotlin DSL) |
| CI | GitHub Actions (APK-Build) |

---

## Modulstruktur (Ziel-Architektur)

```
app/
├── data/
│   ├── remote/
│   │   ├── SheetsApiService.kt       ← Retrofit-Interface, alle Sheets-API-Calls
│   │   ├── WetterApiService.kt       ← BrightSky API
│   │   └── PollenApiService.kt       ← DWD OpenData + Open-Meteo
│   ├── local/
│   │   ├── AppDatabase.kt            ← Room-Datenbank
│   │   ├── StammdatenDao.kt          ← Hunde, Zutaten, Rezepte (Cache)
│   │   └── TagebuchDao.kt            ← Tagebuch-Einträge (Cache, TTL 10 Min)
│   └── repository/
│       ├── AuthRepository.kt         ← Google OAuth2 (war: auth.js)
│       ├── SheetsRepository.kt       ← Alle Sheet-Operationen (war: sheets.js)
│       ├── StammdatenRepository.kt   ← In-Memory-Cache Stammdaten (war: store.js)
│       ├── TagebuchRepository.kt     ← Session-Cache Tagebuch (war: cache.js)
│       ├── SettingsRepository.kt     ← DataStore (war: config.js / localStorage)
│       ├── WetterRepository.kt       ← Wetter + Pollen (war: wetter.js)
│       └── ImportRepository.kt       ← USDA + Open Food Facts (war: stammdaten.js Import)
├── domain/
│   ├── NutritionCalculator.kt        ← resolveRezept(), Kcal, Kochverlust (war: rechner.js)
│   ├── ReaktionsscoreCalculator.kt   ← Zutaten-Reaktionsscore (war: statistik.js)
│   └── KorrelationsAnalyse.kt        ← Korrelationsanalyse (war: statistik.js)
├── ui/
│   ├── auth/
│   │   └── LoginScreen.kt
│   ├── settings/
│   │   └── SettingsScreen.kt         ← war: config.js + Einstellungen-Panel
│   ├── stammdaten/
│   │   ├── HundeScreen.kt            ← war: stammdaten.js Hunde-Tab
│   │   ├── GewichtDialog.kt
│   │   ├── ZutatenScreen.kt          ← war: stammdaten.js Zutaten-Tab
│   │   ├── NaehrstoffDialog.kt       ← 39 NRC-Nährstoffe
│   │   └── ImportDialog.kt           ← USDA + OFF paralleler Import
│   ├── rechner/
│   │   ├── RechnerScreen.kt          ← war: rechner.js
│   │   ├── NaehrstoffBalken.kt       ← Compose Canvas / ProgressIndicator
│   │   └── VergleichScreen.kt        ← Rezept A vs. B
│   ├── tagebuch/
│   │   ├── TagebuchScreen.kt         ← Tab-Navigation (war: tagebuch.js)
│   │   ├── UmweltTab.kt
│   │   ├── SymptomTab.kt
│   │   ├── FutterTab.kt
│   │   ├── AusschlussTab.kt
│   │   ├── AllergenTab.kt
│   │   ├── TierarztTab.kt
│   │   ├── MedikamentTab.kt
│   │   ├── PhasenTab.kt              ← Phasentracker (war: tagebuch.js submitPhase etc.)
│   │   ├── EintragCard.kt            ← war: ansicht.js Entry-Cards
│   │   └── EditDialog.kt             ← war: ansicht.js Edit-Modal
│   ├── statistik/
│   │   ├── StatistikScreen.kt        ← war: statistik.js
│   │   ├── KpiKacheln.kt
│   │   ├── KonfigurierbarerChart.kt
│   │   ├── HeatmapView.kt            ← Symptom-Muster
│   │   └── PollenPopupDialog.kt
│   └── export/
│       └── ExportScreen.kt           ← war: export.js
├── util/
│   ├── FloatParser.kt                ← _float() mit Komma-Dezimaltrenner
│   └── LocaleHelper.kt              ← i18n (war: i18n.js)
└── res/
    ├── values/strings.xml            ← DE Strings
    └── values-en/strings.xml         ← EN Strings
```

---

## Google Sheets Struktur

Die Sheets-Struktur ist identisch zur Web-App v2.3.1. Bestehende Spreadsheets sind ohne Änderung nutzbar.

### WICHTIGE KONVENTIONEN (unverändert)

- **Zeile 1:** Anzeige-Header (Deutsch, für menschliche Lesbarkeit)
- **Zeile 2:** API-Header (Englisch, snake_case)
- **Daten ab Zeile 3**
- **Pflichtfelder** am Ende jeder Tabelle: `created_at`, `deleted`, `deleted_at`
- Kotlin-Code liest per **Spaltenindex** (positional); neue Spalten IMMER ANS ENDE anfügen
- Soft-Delete: `deleted = TRUE`, `deleted_at = ISO 8601`

### Spreadsheet 1: Stammdaten (`Hund_Stammdaten`)

Sheets: `Hunde`, `Parameter`, `Naehrstoffe`, `Bedarf`, `Zutaten`, `Zutaten_Naehrstoffe`, `Toleranzen`, `Rezepte`, `Rezept_Zutaten`, `Rezept_Komponenten`, `Translations`, `Hund_Gewicht`

### Spreadsheet 2: Tagebuch (`Hund_Tagebuch`)

Sheets: `Umwelt`, `Symptom`, `Futter`, `Ausschluss`, `Allergen`, `Tierarzt`, `Medikament`, `Pollen_Log`, `Ausschluss_Phasen`

---

## Modul-Abhängigkeiten (Kotlin)

```
AuthRepository         → (kein Import)
SheetsRepository       → AuthRepository
SettingsRepository     → DataStore
StammdatenRepository   → SheetsRepository, SettingsRepository
TagebuchRepository     → SheetsRepository, SettingsRepository
WetterRepository       → SettingsRepository (Standort, DWD-Region)
ImportRepository       → SettingsRepository (USDA-Key)
NutritionCalculator    → StammdatenRepository
StatistikViewModel     → TagebuchRepository, StammdatenRepository
ExportViewModel        → TagebuchRepository, StammdatenRepository, NutritionCalculator
```

---

## Implementierungsstand

**v0.1 (aktuell):**
- Projektstruktur definiert
- Android-Migration begonnen
- Alle MD-Dokumente auf AllerPaw / Android aktualisiert
- Ausgangsbasis: Web-App v2.3.1 vollständig dokumentiert

---

## Konvention: Rückfragen vor der Implementierung

> Claude soll vor jeder Implementierung Unklarheiten aktiv ansprechen.  
> Lieber 5 Fragen stellen als falsch implementieren.

## Typischer Prompt bei Einzelmodul-Arbeit

```
Kontext: AllerPaw v0.1 – Android-App, Kotlin + Compose, Google Sheets als Datenbank.
Vollständige Projektbeschreibung: [PROJECT.md]

Ich teile jetzt [Modul]. Bitte [Aufgabe].
Andere relevante Module: [z.B. SheetsRepository, StammdatenRepository]
Aktualisiere PROJECT.md, FEATURE.md und FAQ.md als zukünftige Referenz.
Bei Änderungen an der Sheets-Struktur bitte explizit mitteilen.
```

---

## Wichtige Hinweise für neue Prompts

- `FloatParser.kt` übernimmt `_float()` aus store.js: Komma als Dezimaltrenner (Google-Sheets-DE) wird korrekt verarbeitet
- `NutritionCalculator.resolveRezept()` arbeitet ohne Zwischenrundung bis zur Anzeige (max. 5 Ebenen, Zykluserkennung)
- Kochverlustfaktor 0.75 gilt **ausschließlich für B-Vitamine** (B1, B2, B3, B5, B6, B9, B12)
- Nährstoff `EPA + DHA` muss exakt so benannt sein (kombiniert, nicht als getrennte Einträge)
- Pollen_Log: jede Pollenart = eigene Sheet-Zeile (nicht als komma-getrennte Liste)
- Cache TTL Tagebuch: 10 Minuten (Room-Timestamp-Vergleich)
- Soft-Delete Stack: max. 5 Einträge; Undo-Banner sichtbar für 8 Sekunden
- PHASEN_DEFAULTS: Elimination = 42 Tage, Provokation = 14 Tage, Ergebnis = 7 Tage
- Statistik Symptom-Heatmap: nur ab 14 Symptomeinträgen anzeigen
- Korrelationsanalyse: min. 3 Datenpunkte pro Gruppe; Gruppen mit Ø > 2.0 orange hervorheben
- Reaktionsscore: min. 3 Beobachtungen pro Zutat; 48-h-Fenster nach Futtereintrag
- Statistik startet ohne vorausgewählte Parameter (Nutzer wählt aktiv aus)
