# AllerPaw – Projektbeschreibung (v0.11.0)

> **Dieses Dokument als Kontext in jeden Prompt einfügen.**
> Letzte Aktualisierung: 2026-05-19 · Status: v0.11.0 – **alle Features implementiert · API 37 Vorbereitung dokumentiert** 🎉

---

## Überblick

**AllerPaw** ist eine native Android-App zur Ernährungs- und Gesundheitsverwaltung für Hunde. Zielgruppe: BARF-Hundebesitzer mit Allergieverdacht oder aktiver Ausschlussdiät.

**Primärspeicher: SQLite (Room) — vollständig lokal, kein Backend-Zwang.**
Google Sheets, CSV, PDF: nur optionale Export-/Backup-Kanäle.

---

## Tech-Stack

| Bereich | Technologie | Version |
|---------|-------------|---------|
| Plattform | Android | API 26+ (Target 37 · Android 17) |
| Sprache | Kotlin | 2.2.10 |
| UI | Jetpack Compose + Material You | BOM 2025.05.00 |
| Datenbank | Room (SQLite) | 2.7.1 |
| DI | Hilt | 2.56 |
| Async | Coroutines + Flow | — |
| HTTP | Retrofit 2 + OkHttp | 2.11.0 |
| Auth | Google Credential Manager | 1.5.0 |
| Charts | Vico | 2.0.0-beta.3 |
| Adaptive UI | Material3 Adaptive + androidx.window | 1.1.0 / 1.4.0 |
| DataStore | Preferences DataStore | 1.1.4 |
| Build | Gradle KDS + Version Catalog | AGP 8.9.1 |
| CI | GitHub Actions | — |
| Push | WorkManager + HiltWorkerFactory | 2.10.0 |
| Wetter | BrightSky API (DWD) | kein Key |
| Pollen | Open-Meteo Air Quality | kein Key |
| PDF | Android PdfDocument | — |

---

## Implementierungsstand (v0.11)

### Datenschicht (Room DB Version 3 — 22 Entities)

| Gruppe | Entities |
|--------|---------|
| Stammdaten | HundEntity, HundGewichtEntity, ZutatEntity, ZutatNaehrstoffEntity, RezeptEntity, RezeptZutatEntity, ParameterEntity, ToleranzEntity |
| Tagebuch | TagebuchUmweltEntity, TagebuchPollenLogEntity, EigenePollenartEntity, TagebuchSymptomEntity, TagebuchFutterEntity, TagebuchFutterItemEntity, TagebuchAusschlussEntity, TagebuchAllergenEntity, TagebuchTierarztEntity, TagebuchMedikamentEntity, AusschlussPhasEntity |
| Neu v0.7 | TagebuchHundZustandEntity (Smiley), TaskEntity, TaskErledigung |

**DAOs:** HundDao, ZutatenDao, RezeptDao, TagebuchDao, ParameterDao, HundZustandDao, TaskDao

---

## Ordnerstruktur

```
app/src/main/java/com/allerpaw/app/
├── AllerPawApplication.kt       ← @HiltAndroidApp + HiltWorkerFactory
├── MainActivity.kt              ← Notification Permission + WorkManager
├── di/
│   ├── DatabaseModule.kt        ← Room DB + 7 DAOs
│   ├── NetworkModule.kt         ← BrightSky + OpenMeteo Retrofit
│   └── DomainModule.kt          ← RezeptAnalyseUseCase
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt       ← Room v3, 22 Entities
│   │   ├── Converters.kt        ← Instant ↔ Long, LocalDate ↔ String
│   │   ├── entity/
│   │   │   ├── StammdatenEntities.kt
│   │   │   └── TagebuchEntities.kt  ← inkl. HundZustand, Task, TaskErledigung
│   │   └── dao/Daos.kt
│   ├── remote/
│   │   ├── api/BrightSkyApi.kt
│   │   ├── api/OpenMeteoApi.kt
│   │   └── dto/ApiDtos.kt
│   └── repository/
│       ├── HundRepository.kt
│       ├── ZutatenRepository.kt
│       ├── TagebuchRepository.kt    ← inkl. Range-Methoden für Statistik
│       ├── RezeptRepository.kt
│       ├── SessionRepository.kt
│       ├── AuthRepository.kt        ← Google Credential Manager
│       ├── SettingsRepository.kt    ← Standort, Sprache, IE-Modus
│       ├── WetterRepository.kt      ← BrightSky + OpenMeteo kombiniert
│       ├── HundZustandRepository.kt
│       └── TaskRepository.kt
├── domain/
│   ├── NaehrstoffDomain.kt      ← 30 NRC-Einträge (inkl. kcal), RER/MER, Analyse
│   ├── RezeptResolver.kt        ← rekursiv, max. 5 Ebenen, Zykluserkennung
│   └── NrcLebensphasen.kt       ← Welpe, Senior, Trächtig, Laktierend
├── util/
│   ├── FloatParser.kt           ← DE/EN Dezimaltrenner
│   ├── UndoManager.kt           ← Soft-Delete Stack, 8 Sek., max. 5
│   ├── PdfExporter.kt           ← 8 Sektionen, A4, Android PdfDocument
│   ├── LocaleHelper.kt          ← Sprachschalter ohne Neustart
│   └── TaskNotificationService.kt ← WorkManager, Intervall-Push
└── ui/
    ├── AllerPawApp.kt           ← Root, 6 Bottom-Nav-Tabs
    ├── nav/NavGraph.kt          ← Screens + BottomNavItem
    ├── theme/
    ├── auth/                    ← LoginScreen, LoginViewModel
    ├── stammdaten/              ← StammdatenScreen + ViewModel
    ├── zutaten/                 ← ZutatenScreen, NaehrstoffDialog, ViewModel
    ├── rezept/                  ← RezeptScreen, NaehrstoffBalken,
    │                               FutterUmstellungsRechner, ViewModel
    ├── tagebuch/
    │   ├── TagebuchScreen.kt    ← 9 Tabs inkl. Zustand
    │   ├── TagebuchViewModel.kt ← inkl. HundZustandRepository
    │   └── tabs/                ← ZustandTab, UmweltTab, SymptomTab,
    │                               FutterTab, AusschlussTab, AllergenTab,
    │                               TierarztTab, MedikamentTab, PhasenTab
    ├── statistik/               ← StatistikScreen + ViewModel
    ├── tasks/                   ← TaskScreen + TaskViewModel
    ├── export/                  ← ExportScreen + ExportViewModel
    └── settings/                ← SettingsScreen + SettingsViewModel
```

---

## Navigation

**Bottom Navigation (6 Tabs):**
Tagebuch · Rechner · Hunde · Statistik · Aufgaben · Export

**Tagebuch-Tabs (9):**
Zustand · Umwelt · Symptom · Futter · Ausschluss · Allergen · Tierarzt · Medikament · Phasen

---

## Wichtige Implementierungsregeln

| Regel | Wert |
|-------|------|
| Kochverlust | 0.30 (Faktor 0.70) — nur B1,B2,B3,B5,B6,B9,B12 |
| Sub-Rezept Skalierung | `mengeG / subRaw.gesamtGrammRoh` |
| Soft-Delete | `deleted=1` + `deletedAt` — max. 5 Undo, 8 Sek. Banner |
| IE-Konvertierung | Vit.A: 1IE=0.3µg · Vit.D: 1IE=0.025µg · Vit.E: je Form |
| Pollen-Log | Jede Pollenart = eigene DB-Zeile |
| Heatmap | Ab 14 Symptomeinträgen |
| Korrelation | Ab 3 Datenpunkten · Ø>2.0 = orange |
| Reaktionsscore | 48h-Fenster · min. 3 Beobachtungen |
| NRC | 30 Einträge (inkl. kcal) · Bedarfswerte je 1000 kcal ME |
| Room DB | Version 3 · 22 Entities · Wildcard-Import `entity.*` |
| WorkManager | HiltWorkerFactory in AllerPawApplication pflichtmäßig |
| Notification | POST_NOTIFICATIONS Permission ab Android 13 |
| Phasen-Defaults | Elimination 42T · Provokation 14T · Ergebnis 7T |
| Umstellung | 5–14 Tage · linear · Gramm = Anteil% × Tagesration |
| API 37 Opt-out | `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` nie verwendet — nicht nötig |
| Adaptive Layouts | `NavigationRail` auf sw≥Medium, `widthIn(max=480dp)` auf Login |

---

## Typischer Prompt bei Einzelmodul-Arbeit

```
Kontext: AllerPaw v0.11 – Android, Kotlin + Compose, Room v3, AGP 9.1.1.
[PROJECT.md als Kontext einfügen]

Aufgabe: [Beschreibung]
Betroffene Dateien: [z.B. RezeptScreen, RezeptViewModel]
```
