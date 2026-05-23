# AllerPaw

Native Android-App zur Ernährungs- und Allergieverwaltung für Hunde.

> Version: 0.11.0012 · Stand: 2026-05-22

---

## Features

- **Futterrechner** – NRC 2006 Nährstoffanalyse für 29+ Nährstoffe, RER/MER-Berechnung, Rezept-Mix
- **Tagebuch** – 8 Tabs: Umwelt, Symptom, Futter, Ausschluss, Allergen, Tierarzt, Medikament, Phasen
- **Ausschlussdiät-Phasentracker** – Elimination / Provokation / Ergebnis mit Fortschrittsbalken
- **Statistik** – Konfigurierbarer Chart, Symptom-Heatmap, Korrelationsanalyse, Reaktionsscore
- **Wetter & Pollen** – BrightSky (DWD) + Open-Meteo, 18 DWD-Regionen
- **Export** – PDF-Tierarztbericht, CSV, SQLite-Backup, Google Sheets (optional)
- **Mehrsprachig** – Deutsch + Englisch
- **Offline-First** – Alle Kernfunktionen ohne Internet

---

## Tech-Stack

| Bereich | Technologie |
|---------|-------------|
| Sprache | Kotlin |
| UI | Jetpack Compose + Material You |
| Datenbank | Room (SQLite) |
| DI | Hilt |
| Charts | Vico |
| Async | Coroutines + Flow |
| Min SDK | API 26 (Android 8.0) |
| Build | AGP 9.1.1 |

---

## Projekt öffnen

1. ZIP entpacken
2. In Android Studio öffnen (`File → Open → AllerPaw/`)
3. Gradle sync abwarten
4. Emulator starten oder Gerät verbinden
5. Run ▶

---

## Projektdokumentation

| Datei | Inhalt |
|-------|--------|
| `PROJECT.md` | Architektur, Implementierungsstand, Konventionen |
| `FEATURE.md` | Feature-Liste mit Implementierungsstatus |
| `MIGRATION.md` | Phasenplan, Modul-Mapping JS → Kotlin |
| `CODE_ANALYSIS.md` | Abgleich Web-App vs. Android, Korrekturen |
| `VALIDATION.md` | Testplan je Phase |
| `FAQ.md` | Häufige Fragen zu Implementierungsdetails |

---

## Implementierungsstand

**Phase 1 ✅** – Projektgerüst, Room-DB, Hilt, Navigation, Hunde-CRUD, RER/MER-Rechner  
**Phase 2 ✅** – Zutaten, Tagebuch vollständig (9 Tabs)  
**Phase 3 ✅** – Rezept-Editor, Statistik, Kreuzallergen-Analyse  
**Phase 4 ✅** – Wetter/Pollen, PDF-Export, Backup, Google Sheets  
**Phase 5 ✅** – Auth, i18n (DE/EN), Tests, WorkManager, targetSdk 37  
**v0.11 ✅** – Adaptive UI (NavigationRail), AGP 9.1.1, Sicherheits-Baseline  

---

## Lizenz

Privates Projekt – alle Rechte vorbehalten.
