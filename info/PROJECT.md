# AllerPaw – Projektbeschreibung (v0.2)

> **Dieses Dokument als Kontext in jeden Prompt einfügen, wenn nur einzelne Module geteilt werden.**  
> Letzte Aktualisierung: 2026-04-24 · Status: v0.2 – Phase 1 implementiert (lokale Room-DB)

---

## Überblick

**AllerPaw** ist eine native Android-App zur Ernährungs- und Gesundheitsverwaltung für Hunde. Sie richtet sich an Hundebesitzer, die BARF-Ernährung betreiben und Symptome sowie Allergien systematisch dokumentieren wollen.

Die App nutzt **SQLite (Room) als primäre Datenbank** — vollständig lokal auf dem Gerät, kein Backend-Zwang. Google Sheets dient ausschließlich als optionaler Export/Backup-Kanal.

---

## Tech-Stack

| Bereich | Technologie |
|---------|-------------|
| Plattform | Android (API 26+) |
| Sprache | Kotlin |
| UI | Jetpack Compose + Material You |
| HTTP / API | Retrofit 2 + OkHttp |
| Auth | Google Sign-In / Credential Manager (optional, nur für Backup) |
| Lokale DB | Room (SQLite) – Primärspeicher |
| Settings | DataStore Preferences |
| Charts | Vico (Compose-nativ) |
| DI | Hilt |
| Async | Kotlin Coroutines + Flow |
| Build | Gradle (Kotlin DSL + Version Catalog) |
| CI | GitHub Actions (APK-Build) |
| Wetter | BrightSky API (DWD-Daten) |
| Pollen | DWD OpenData + Open-Meteo Air Quality API |
| Nährstoffberechnung | NRC 2006 (29 Kern-Nährstoffe implementiert) |

---

## Implementierungsstand (v0.2 – Stand 2026-04-24)

### Fertig implementiert

| Bereich | Dateien | Details |
|---------|---------|---------|
| Projektstruktur | build.gradle.kts, settings.gradle.kts, libs.versions.toml | AGP 8.5, Kotlin 2.0, KSP |
| Hilt DI | DatabaseModule, NetworkModule, DomainModule | Alle Provider konfiguriert |
| Room DB | AppDatabase (Version 1) | 19 Entities, TypeConverters |
| Stammdaten-Entities | StammdatenEntities.kt | Hund, HundGewicht, Zutat, ZutatNaehrstoff, Rezept, RezeptZutat, Parameter, Toleranz |
| Tagebuch-Entities | TagebuchEntities.kt | Umwelt, PollenLog, EigenePollenart, Symptom, Futter, FutterItem, Ausschluss, Allergen, Tierarzt, Medikament, AusschlussPhase |
| DAOs | Daos.kt | HundDao, ZutatenDao, RezeptDao, TagebuchDao, ParameterDao – alle mit Soft-Delete |
| Repositories | HundRepository, SessionRepository | Flow-basiert |
| Domain-Logik | NaehrstoffDomain.kt | 29 NRC-Nährstoffe, RER/MER, RezeptAnalyseUseCase |
| Navigation | NavGraph.kt, AllerPawApp.kt | Bottom Nav, 5 Tabs + Settings |
| Theme | Theme.kt, Typography.kt | Material You, Dynamic Color, Dark Mode |
| Auth | LoginScreen, LoginViewModel | DataStore-basiert; Google-Token-Platzhalter |
| Stammdaten UI | StammdatenScreen, StammdatenViewModel | Hund-Liste, Edit-Dialog |
| Rechner UI | RechnerScreen, RechnerViewModel | Energie-Banner, Aktivitätsfaktor-Slider, NRC-Tabelle |
| Strings | values/strings.xml, values-en/strings.xml | DE + EN |

### Placeholder (nächste Phasen)

| Screen | Phase |
|--------|-------|
| TagebuchScreen vollständig | Phase 2 |
| ZutatenScreen + NaehrstoffDialog | Phase 2 |
| RezeptEditor | Phase 3 |
| StatistikScreen | Phase 3 |
| ExportScreen (PDF) | Phase 4 |

---

## Architektur

```
ui  →  domain  ←  data
```

Drei Schichten, unidirektionale Abhängigkeit. `domain` kennt weder Android noch Room.

### Ordnerstruktur

```
app/src/main/java/com/allerpaw/app/
├── AllerPawApplication.kt
├── MainActivity.kt
├── di/
│   ├── DatabaseModule.kt
│   ├── NetworkModule.kt
│   └── DomainModule.kt
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── Converters.kt
│   │   ├── entity/
│   │   │   ├── StammdatenEntities.kt
│   │   │   └── TagebuchEntities.kt
│   │   └── dao/Daos.kt
│   └── repository/
│       ├── HundRepository.kt
│       └── SessionRepository.kt
├── domain/
│   └── NaehrstoffDomain.kt
└── ui/
    ├── AllerPawApp.kt
    ├── nav/NavGraph.kt
    ├── theme/
    ├── auth/
    ├── stammdaten/
    ├── rechner/
    ├── tagebuch/      ← Placeholder
    ├── statistik/     ← Placeholder
    ├── export/        ← Placeholder
    └── settings/
```

---

## Wichtige Implementierungsregeln

- Kochverlustfaktor: **0.30** (30 % Verlust → Faktor 0.70); nur B-Vitamine; konfigurierbar
- RezeptAnalyse: Ohne Zwischenrundung; max. 5 Ebenen; Zykluserkennung
- Pollen_Log: jede Pollenart = eigene DB-Zeile
- Soft-Delete: max. 5 Undo-Stack; Banner 8 Sekunden
- PHASEN_DEFAULTS: Elimination 42 Tage, Provokation 14 Tage, Ergebnis 7 Tage
- Symptom-Heatmap: ab 14 Einträgen
- Reaktionsscore: min. 3 Beobachtungen; 48-h-Fenster
- Statistik: startet ohne vorausgewählte Parameter
