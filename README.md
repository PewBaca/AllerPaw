# AllerPaw

Native Android-App zur Ernährungs- und Allergieverwaltung für Hunde.

> Version: 0.2.0 (Phase 1 abgeschlossen) · Stand: 2026-04-24

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

**Phase 1 ✅** – Projektgerüst, Room-DB (19 Entities), Hilt, Navigation, Hunde-CRUD, RER/MER-Rechner, NRC-Domänenlogik  
**Phase 2 🔲** – Zutaten, Tagebuch vollständig  
**Phase 3 🔲** – Rezept-Editor, Statistik  
**Phase 4 🔲** – Wetter/Pollen, PDF-Export, Backup  
**Phase 5 🔲** – Auth, i18n, Tests, Store-Release  

---

## Lizenz

Privates Projekt – alle Rechte vorbehalten.
