# AllerNutri – Migration & Entwicklungsplan (v0.11.0012)

> Stand: 2026-05-22 · Ausgangsbasis: Web-App v2.3.1 (Vanilla HTML + ES Modules)
> Aktuelle Version: **0.11.0013** · AGP 9.1.1 · Sicherheits-Phase D abgeschlossen

---

## Architektur-Entscheidung

```
Android  →  Kotlin / Compose  →  Room SQLite v3 (Primär-DB)
                               →  BrightSky + Open-Meteo (Wetter/Pollen)
                               →  Google Sheets / CSV / PDF (Export)
```

**Begründung Room als Primär-DB:** 100% Offline, keine Rate-Limits, volle Datensouveränität.

---

## Tech-Stack (vollständig implementiert)

| Bereich | Technologie | Status |
|---------|-------------|--------|
| Sprache | Kotlin 2.1.21 | ✅ |
| UI | Jetpack Compose + Material You | ✅ |
| Datenbank | Room 2.7.1 (Version 3, 22 Entities) | ✅ |
| DI | Hilt 2.56 | ✅ |
| Async | Coroutines + Flow | ✅ |
| HTTP | Retrofit 2 + OkHttp | ✅ |
| Auth | Google Credential Manager | ✅ |
| Push | WorkManager 2.10.0 + HiltWorkerFactory | ✅ |
| DataStore | Preferences DataStore | ✅ |
| Build | AGP 9.1.1 + Gradle KDS + Version Catalog | ✅ |
| CI | GitHub Actions (Debug + Release APK) | ✅ |
| ProGuard | R8 konfiguriert | ✅ |

---

## Modul-Mapping: JS → Kotlin (vollständig)

| Web-App Modul | Android-Äquivalent | Status |
|---------------|-------------------|--------|
| `auth.js` | `AuthRepository` + Credential Manager | ✅ |
| `config.js` | `SettingsRepository` + DataStore | ✅ |
| `store.js` | `HundRepository` + `ZutatenRepository` | ✅ |
| `cache.js` | `TagebuchRepository` | ✅ |
| `rechner.js` | `RezeptViewModel` + `NaehrstoffDomain` + `RezeptResolver` | ✅ |
| `tagebuch.js` | `TagebuchViewModel` + 9 Tab-Composables | ✅ |
| `stammdaten.js` | `StammdatenViewModel` + `ZutatenViewModel` | ✅ |
| `wetter.js` | `WetterRepository` (BrightSky + OpenMeteo) | ✅ |
| `statistik.js` | `StatistikViewModel` + Composables | ✅ |
| `export.js` | `ExportViewModel` + `PdfExporter` | ✅ |
| `i18n.js` | `strings.xml` (DE/EN) + `LocaleHelper` | ✅ |
| `sheets.js` | `SheetsRepository` | 🔲 optional |

---

## Phasen-Übersicht

### Phase 1 ✅ – Projektaufbau & Datenschicht
Room-DB, 19 Entities, 5 DAOs, Repositories, NRC-Katalog, RER/MER, Navigation, Login, Stammdaten-Grundgerüst

### Phase 2 ✅ – Stammdaten + Tagebuch
FloatParser, UndoManager, ZutatenScreen + NaehrstoffDialog, TagebuchScreen mit allen 8 Tabs, UmweltTab inkl. Pollen, SymptomTab, FutterTab, PhasenTab mit Fortschrittsbalken

### Phase 3 ✅ – Futterrechner + Statistik
RezeptResolver (rekursiv, Zykluserkennung), Kochverlust, NaehrstoffBalken (Canvas), Ca:P + Omega 6:3 Badges, Rezept-Vergleich, StatistikScreen mit Heatmap + Korrelation

### Phase 4 ✅ – Wetter/Pollen + Export + App-Icon
BrightSkyApi, OpenMeteoApi, WetterRepository, PdfExporter (8 Sektionen), ExportScreen, FileProvider, App-Icon in allen Dichten

### Phase 5 ✅ – Auth + Einstellungen + Tests + CI
AuthRepository (Google Credential Manager), LocaleHelper, NrcLebensphasen, SettingsScreen vollständig, 10 Unit-Tests, GitHub Actions CI, ProGuard

### Phase 6 ✅ – Tabletten/Tropfen + Smiley + Tasks + Umstellungsrechner
- Tabletten: Stückzahl × Gewicht = g (Entity + DAO + UI vollständig)
- Tropfen: Anzahl × Gewicht = g (Entity + DAO + UI vollständig)
- `ZutatEntity`: `tropfenGewichtG` + `tropfenVolumenMl`
- `RezeptZutatEntity`: `anzahlTropfen` + `inhaltsstoffeFreitext`
- `ZutatPickerDialog`: perMode-Erkennung, Live-Gramm-Vorschau
- Zustand-Tab (9. Tagebuch-Tab): Smiley 1–5, Verlauf
- `TagebuchHundZustandEntity`: unique Index je Hund+Datum
- Task-System: `TaskEntity` + `TaskErledigung` + `TaskRepository`
- `TaskScreen`: Abhaken, Fortschrittsbalken, Kategorien-Emojis
- `TaskNotificationWorker`: Intervall-basierte Push-Notifications
- `AllerPawApplication`: HiltWorkerFactory + Notification Channel
- `MainActivity`: POST_NOTIFICATIONS Permission (Android 13+)
- `FutterUmstellungsRechner`: 4 Geschwindigkeiten + eigene Tageanzahl
- Room DB: Version 1 → 2 → 3
- 7 Bugs gefixt (Sub-Rezept Skalierung, Memory Leak, IE-Anzeige etc.)
- MD-Dateien: CODE_ANALYSIS.md gelöscht, VALIDATION in MIGRATION integriert

---

## Offene Punkte (Post-v0.7)

| Punkt | Priorität |
|-------|-----------|
| Gewichtsverlauf-Chart (Vico) in Statistik | Mittel |
| Reaktionsscore (48h-Fenster) | Mittel |
| Wetter-API Auto-Befüllung Umwelt-Tab | Mittel |
| USDA / Open Food Facts Import | Niedrig |
| Google Sheets Export | Niedrig |
| Backup wiederherstellen | Niedrig |
| Zutat-zu-Zutat-Vergleich (⚖️) | Niedrig |
| NRC Lebensphasen UI-Auswahl | Niedrig |
| Rezept-Positionen im Futter-Tab | Niedrig |
| Toleranzbalken UI | Niedrig |
| Hund-Vergleich in Statistik | Niedrig |
| Play Store Release | — |

---

## Validierungsregeln (immer gültig)

- Soft-Delete: `deleted=1` + `deletedAt` — nie echtes Löschen
- Undo: max. 5 Stack · Banner 8 Sekunden
- Kochverlust: **nur** B1,B2,B3,B5,B6,B9,B12 — Faktor 0.70
- Sub-Rezept: `mengeG / subRaw.gesamtGrammRoh` (kein `/ 1.0` Bug)
- IE: beim Speichern sofort in µg/mg konvertieren — nie als IE in DB
- Nährstoffe: intern immer als Wert per 100 g gespeichert
- Heatmap: erst ab 14 Symptomeinträgen anzeigen
- Korrelation: erst ab 3 Datenpunkten
- Phasen-Defaults: Elimination 42T · Provokation 14T · Ergebnis 7T
- Room DB Version: 3 · 22 Entities
- WorkManager: HiltWorkerFactory in AllerPawApplication Pflicht
- Notification Permission: POST_NOTIFICATIONS ab Android API 33

---

## Naming & Versionierung

- App-Name: **AllerPaw**
- Package: `com.allerpaw.app`
- versionCode: `10` · versionName: `"0.10.0"`
- compileSdk / targetSdk: `36` · minSdk: `26`

---

## Phase 7 ✅ – v0.8 Datenimport, Backup, Wetter

### Room Migrations (Store-ready)
- `DatabaseMigrations.kt` mit echten SQL-Migrations erstellt
- `MIGRATION_1_2`: `zutaten.tropfenGewichtG/Ml`, `rezept_zutaten.anzahlTropfen/freitext`
- `MIGRATION_2_3`: `tagebuch_hund_zustand`, `tasks`, `task_erledigungen` Tabellen
- `DatabaseModule`: `fallbackToDestructiveMigration()` → `addMigrations(*ALL)`

### Backup & Wiederherstellung
- `BackupRepository`: `exportBackup()` (WAL-Checkpoint + Share-Intent)
- `BackupRepository`: `importBackup(uri)` inkl. SQLite user_version Prüfung (Offset 60)
- `BackupScreen` + `BackupViewModel`: Filepicker, Versionscheck, Bestätigungs-Dialog, App-Neustart
- Navigation: Settings → Backup eigene Route

### Google Sheets Export + Import
- `SheetsApiClient`: OkHttp-basiert, Sheets v4 REST (create/read/write/append)
- `SheetsColumnMapper`: Auto-Mapping Kopfzeilen → Felder (exact + fuzzy match)
  - Felder für SYMPTOM, FUTTER, UMWELT, ALLERGEN
  - Sheet-Struktur: Zeile 1 = DE-Header, Zeile 2 = API-Keys, Daten ab Zeile 3
- `SheetsRepository`: exportToSheets() + previewImport() + importFromSheets()
- `SheetsImportScreen`: 3-Schritt-Wizard: Quelle → Mapping → Import
  - Auto-gemappte Spalten grün angezeigt
  - Unmapped Felder → Dropdown-Auswahl je Feld
- `SheetsViewModel`: kombiniert auto + user-Override Mapping
- Navigation: Settings → SheetsImport eigene Route

### Wetter Auto-Befüllung
- `StandortPicker`: 3 Modi (Stadtname via Geocoder, Koordinaten manuell, GPS)
- `WetterViewModel`: lädt Wetter + Pollen, Standort aus SettingsRepository
- `UmweltTab`: Wetter-Banner mit Auto-Befüllung via `LaunchedEffect`
  - Standort wechseln direkt im Dialog (einklappbarer StandortPicker)
  - Pollen-Stärken auto-befüllt aus Open-Meteo API

### versionCode 8 · versionName "0.8.0"

---

## Phase 8 ✅ – v0.10.0 API Level 36 Migration

### Android 16 (API 36) Compliance
- `compileSdk` + `targetSdk`: 35 → **36**
- `versionCode`: 9 → **10** · `versionName`: 0.9.0 → **0.10.0**
- **Edge-to-Edge**: `enableEdgeToEdge()` war bereits korrekt in `MainActivity`. `windowOptOutEdgeToEdgeEnforcement` nie verwendet → keine Aktion nötig.
- **Predictive Back Gesture**: `android:enableOnBackInvokedCallback="true"` in `<application>` gesetzt. Navigation Compose verwaltet Back-Stack über `NavController` korrekt → vollständig kompatibel.
- **Local Network Protection (LNP)**: `NEARBY_WIFI_DEVICES` im Manifest deklariert (noch kein Laufzeit-Request nötig). Wetter-APIs laufen über Internet, kein LAN-Zugriff. Erzwingung erwartet in Android 17.
- **elegantTextHeight**: nie auf `false` gesetzt → API 36-Standardverhalten aktiv, keine Layout-Anpassungen nötig (Compose-basiert).
- **scheduleAtFixedRate** / **MediaStore#getVersion()** / **Safer Intents**: nicht verwendet → keine Aktion nötig.
- **Adaptive Layouts (sw600dp)**: kein `screenOrientation`/`resizableActivity`/Aspect-Ratio-Lock in Manifest → automatisch compliant.

### Dependency-Updates
| Paket | Alt | Neu |
|-------|-----|-----|
| AGP | 8.5.2 | 8.9.1 |
| Kotlin | 2.0.21 | 2.1.21 |
| KSP | 2.0.21-1.0.27 | 2.1.21-1.0.32 |
| compose-bom | 2024.11.00 | 2025.05.00 |
| Room | 2.6.1 | 2.7.1 |
| androidx.core-ktx | 1.15.0 | 1.16.0 |
| lifecycle-* | 2.8.7 | 2.9.0 |
| activity-compose | 1.9.3 | 1.10.1 |
| navigation-compose | 2.8.4 | 2.9.0 |
| credentials | 1.3.0 | 1.5.0 |
| datastore | 1.1.1 | 1.1.4 |
| Coil | 2.6.0 | 2.7.0 |

### versionCode 10 · versionName "0.10.0"

---

## Phase 9 ✅ – v0.10.0 Toleranzbalken UI

### ToleranzDao + ToleranzRepository
- `ToleranzDao`: `beobachteToleranzFuerHund`, `upsert`, `loescheAlleForHund`
- `ToleranzRepository`: Flow mit NRC-Fallback für fehlende DB-Einträge
- `DatabaseModule` + `AppDatabase`: `toleranzDao()` registriert

### NaehrstoffBalken (komplett neu)
- Individuelle Min/Ziel/Max-Marker per Hund (statt hardcoded 80%/100%)
- Dynamische Skalierung (0–133% des Max-Werts)
- Long-Press öffnet Tooltip mit Ist/Bedarf/UL/Toleranzwerten
- Edit-Button (🖊) pro Balken öffnet `ToleranzEditDialog`
- Canvas-Überarbeitung: drei farbige Marker (blau/grün/rot)

### ToleranzEditDialog (neu)
- Drei Slider: Min (0–100%), Ziel (0–200%), Max (100–300%)
- Live-Vorschau-Balken mit aktuellen Markern
- Validierung: Min ≤ Ziel ≤ Max
- Zurücksetzen-Button setzt auf NRC-Standard (80/100/150%)
- NRC-UL-Hinweis wenn `maxPro1000kcal` vorhanden

### RezeptViewModel
- `ToleranzRepository` injiziert
- `toleranzMap: Map<String, ToleranzEntity>` im State
- `editToleranz`, `dismissToleranz`, `saveToleranz`, `toleranzZuruecksetzen`, `alleToleranzZuruecksetzen`
- Flow-Subscription wechselt bei `selectHund`

### Strings (7 neu)
- DE + EN: `toleranz_title`, `toleranz_min`, `toleranz_ziel`, `toleranz_max`, `toleranz_vorschau`, `toleranz_fehler`, `toleranz_ul_hinweis`

---

## Phase 10 ✅ – v0.10.0 Google Sheets Export

### SheetsRepository – Export komplett überarbeitet
- `exportToSheets` Bug gefixt (Flow-collect Abbruch entfernt)
- 6 Export-Tabs: Symptome, Umwelt, Allergene, Phasen, Futter, Medikamente
- Vollständige Header-Definitionen DE + API-Keys für alle Tabs
- Alle Row-Builder implementiert (`buildAllergenRows`, `buildPhasenRows`, `buildFutterRows`, `buildMedikamentRows`)
- `ExportTab`-Enum ergänzt
- `SheetsApiClient`-Import ergänzt

### TagebuchRepository – Export-Methoden ergänzt
- `allergenList(hundId)`, `medikamentList(hundId)`, `ausschlussList(hundId)`
- `tierarztList(hundId)`, `futterRange(hundId, von, bis)`

### SheetsViewModel – Export-State + Aktionen
- `exportSpreadsheetId`, `exportVon/Bis`, `exportTabs`, `exportResult`, `exportedSheetUrl`
- `starteExport()`, `toggleExportTab()`, `selectAllTabs()`, `clearAllTabs()`
- `setExportVon/Bis/SpreadsheetId()`
- Import-State auf neue `vorschauZeilen`/`mappings`/`autoMappings`-Struktur migriert

### SheetsExportScreen (neu)
- Hund-Auswahl (Multi-Hund)
- Zeitraum-Picker (30T/90T/6M/1J)
- Tab-Auswahl mit Checkbox + Alle/Keine-Buttons + Beschreibung + Emoji
- Optionale Ziel-Spreadsheet-ID
- Export-Button mit Loading-State
- Ergebnis-Card: direkt-Link zum Sheet (Intent → Browser), URL
- Fehler-Card mit Dismiss
- Hinweis-Card

### Navigation
- `Screen.SheetsExport` in `NavGraph` ergänzt
- Route in `AllerPawApp` registriert
- `SettingsScreen`: neuer "Google Sheets Export"-ListItem
- `SettingsScreen` + `AllerPawApp`: `onNavigateSheetsExport` Parameter ergänzt

---

## Phase 11 ✅ – v0.10.0 Gewichtsverlauf UI + Chart

### HundDao
- `updateHundGewicht(hundId, kg)` — aktualisiert Gewicht im Hund-Profil

### HundRepository
- `updateGewicht(hundId, kg)` — Wrapper für DAO

### StatistikUiState
- `gewichtVerlauf: List<HundGewichtEntity>` (letzte 15, neuste zuerst)
- `gewichtNeuDatum`, `gewichtNeuKg`, `showGewichtDialog`

### StatistikViewModel
- `gewichtVerlauf` wird in `ladeStatistik()` geladen
- `openGewichtDialog()`, `dismissGewichtDialog()`, `setGewichtKg()`, `setGewichtDatum()`
- `saveGewicht()`: FloatParser + addGewicht + updateGewicht + State-Refresh
- `deleteGewicht(id)`: Soft-Delete + State-Refresh

### GewichtVerlaufCard (neu)
- Vico `CartesianChartHost` LineChart (chronologisch, scrollbar)
- KPI-Zeile: Aktuell / Trend (↑↓→) / Min-Max-Spanne
- `LazyRow` mit `InputChip` je Eintrag (Tap → Löschen-X anzeigen)
- Leer-Zustand mit Hinweistext
- `FilledTonalIconButton` für neuen Eintrag

### GewichtEingabeDialog (neu)
- `OutlinedTextField` für kg (FloatParser: Komma + Punkt)
- `OutlinedTextField` für Datum (ISO-Format, Validierung via runCatching)
- Validierung: Zahl > 0 erforderlich

### StatistikScreen
- `GewichtVerlaufCard` als letzter Abschnitt in `LazyColumn`
- `GewichtEingabeDialog` außerhalb Scaffold

---

## Phase 12 ✅ – v0.10.0 NRC Lebensphasen UI

### RezeptAnalyseUseCase
- `analyse(zutaten, kcalME, lebensphase)` — Lebensphase-Parameter ergänzt
- Bedarf wird per `NrcLebensphasen.bedarfPro1000kcal(naehrstoff, lebensphase)` skaliert
- Default: `ADULT` (Faktor 1,0 überall) → kein Breaking Change

### RezeptEditorState
- `lebensphase: NrcLebensphasen.Lebensphase = ADULT`

### RezeptViewModel
- `NrcLebensphasen`-Import ergänzt
- Beide `analyseUseCase.analyse()`-Aufrufe übergeben `_state.value.lebensphase`
- `setLebensphase(phase)`: State-Update + Neuberechnung des aktiven Rezepts

### LebensphasePicker (neu)
- Eingeklappt: aktive Phase als Badge + Expand-Pfeil
- Ausgeklappt: `FilterChip` je Phase mit Emoji + Bedarfs-Hinweis (DE)
- Inline-Dokumentation der NRC-Faktoren im Hinweistext
- NRC-Quellen-Info am Ende
- Schließt sich nach Auswahl automatisch

### RezeptScreen
- `LebensphasePicker` als eigener `item {}` vor der NRC-Analyse-Sektion
- Aktive Lebensphase (wenn ≠ ADULT) als Badge neben "NRC 2006 Analyse"-Titel
- Import `NrcLebensphasen` ergänzt

---

## Phase 13 ✅ – v0.10.0 Manueller Kcal-Bedarf UI

### HundDao
- `updateKcalBedarfManuell(hundId, kcal: Double?)` — setzt oder löscht den manuellen Wert

### HundRepository
- `updateKcalBedarfManuell(hundId, kcal)` — Wrapper

### RechnerViewModel (komplett überarbeitet)
- `RechnerUiState`: `kcalManuellInput`, `kcalManuellAktiv`, `effektiverKcal`, `rerKcal`, `merKcal`
- `selectHund()`: lädt `kcalBedarfManuell` aus Entity in State
- `setAktivitaetsFaktor()`: Neuberechnung mit aktuellem Manuell-Wert
- `setKcalManuellInput()`: Eingabe-Puffer
- `aktiviereKcalManuell()`: FloatParser-Validierung + persistiert in DB + Neuberechnung
- `resetKcalManuell()`: DB-Update auf null + RER/MER reaktivieren
- Hilfsfunktionen `aktuellerHund()`, `aktiverKcalManuell()`, `recalculate()`

### RezeptViewModel
- Beide `analyseRezept()`-Aufrufe: `hund.kcalBedarfManuell ?: EnergieBedarf.mer(...)`
- `selectHund()`: `kcalBedarfManuellAktiv` + `effektiverKcal` im State setzen
- `RezeptEditorState`: `kcalBedarfManuellAktiv`, `effektiverKcal` Felder

### KcalBedarfCard (neu)
- RER/MER/Manuell KPI-Chips
- Aktivitätsfaktor-Slider (1,0–3,0) mit 4 Vorauswahl-Chips
- Slider deaktiviert wenn Manuell aktiv
- Manuell-Eingabe: ausgeklappt/eingeklappt, FloatParser, Delta vs. MER
- Persistenz-Hinweis + Zurücksetzen-Button
- Warnfarbe wenn Manuell >20% von MER abweicht

### Energiebedarf.kt (neu)
- Energiebedarf-Tab im Rechner mit `KcalBedarfCard` + NRC-Erklärungskarte

### RezeptScreen
- Manuell-Badge unter SkalierungsCard wenn `kcalBedarfManuellAktiv`

### Strings (6 neu, DE + EN)
- `kcal_manuell_title/aktivieren/aktualisieren/aktiv/hinweis/ueberschreibt`

---

## Phase 14 ✅ – v0.10.0 Kreuzallergie-Analyse

### KreuzallergenMatrix (neu, Domain)
- 8 Protein-Gruppen: Geflügel, Rind/Milch, Schwein/Wild, Fisch, Schalentiere/Milben, Gluten-Getreide, Hülsenfrüchte, Gräser/Pollen
- Je Gruppe: Name, Protein, Beschreibung, Mitglieder-Liste (lowercase Keywords), Quellen
- `findeGruppe(allergenName)`: Teilstring-Match (lowercase)
- `kreuzreaktionsKandidaten(allergenName)`: Mitglieder ohne das Allergen selbst
- Keyword-Index als lazy Map für O(n) Lookup

### KreuzallergenAnalyse (neu, Domain UseCase)
- `analysiere(allergene)` → `AnalyseErgebnis`
- `AllergenMitGruppe`, `Risikogruppe`, `AnalyseErgebnis` Data-Classes
- Risikogruppen sortiert nach max. Reaktionsstärke
- Kandidaten: Gruppen-Mitglieder die noch nicht als Allergen erfasst

### KreuzallergenViewModel (neu)
- Hunde-Flow + Allergen-Flow via `flatMapLatest`
- Analyse wird bei Allergen-Änderung automatisch neu berechnet

### KreuzallergenScreen (neu)
- Hund-Auswahl (Multi-Hund)
- Leer-Zustand mit Hinweis
- Ergebnis-Header (Anzahl Gruppen + nicht zugeordnete Allergene)
- `RisikoGruppeCard`: eingeklappt/ausgeklappt, bestätigte Allergene, Kandidaten als `SuggestionChip`
- Ampelfarbe nach max. Reaktionsstärke (Hoch/Mittel/Niedrig)
- Wissenschaftliche Grundlage + Quellen je Gruppe
- Disclaimer-Karte (kein Diagnoseersatz)

### FlowRow (neu, ui/common)
- Thin wrapper um `androidx.compose.foundation.layout.FlowRow`

### Navigation
- `Screen.Kreuzallergen` in `NavGraph` ergänzt
- Route in `AllerPawApp` registriert
- `TagebuchScreen`: `onNavigateToKreuzallergen` Parameter + BubbleChart-Icon im TopAppBar (nur im Allergen-Tab sichtbar)

---

## Phase 15 ✅ – v0.10.0 Rezept-Positionen Phase 3 vollständig

### RezeptZutatDraft
- `inhaltsstoffeFreitext: String = ""` ergänzt

### RezeptViewModel
- Draft-Mapping: `inhaltsstoffeFreitext = pos.inhaltsstoffeFreitext`
- `saveRezept()`: `inhaltsstoffeFreitext = d.inhaltsstoffeFreitext` (statt leer)

### RezeptEditDialog (überarbeitet)
- Positionen-Liste als `ElevatedCard` je Position mit:
  - Emoji-Icon (🍖 Zutat / 📋 Sub-Rezept)
  - Name + `anzeigeText()` + optionaler Freitext (1 Zeile)
  - Bearbeiten-Button → `PositionEditDialog`
  - Hoch/Runter-Pfeile für Reihenfolge
  - Entfernen-Button (rot)
  - Gesamtgewicht-Summe im Header
- Zwei Hinzufügen-Buttons: Zutat + Rezept-Mix (nur wenn alleRezepte nicht leer)
- `alleRezepte` Parameter ergänzt

### PositionEditDialog (neu)
- Nachträgliche Gramm-Änderung (FloatParser, Direkteingabe)
- Tabletten/Tropfen-Rückrechnung zur Anzeige
- Freitext-Feld (Charge, Hersteller, Hinweis)

### SubRezeptPickerDialog (neu)
- Auswahl aus bestehenden Rezepten (außer dem aktuellen)
- Gesamtmenge-Eingabe → interne Zutaten werden anteilig skaliert (via RezeptResolver)
- Kategorien zur Orientierung angezeigt

### ZutatPickerDialog (überarbeitet)
- Freitext-Feld ergänzt
- Suche mit Leading-Icon
- Emoji-Icons je perMode (💊 💧 🧂 🍖)
- Callback um `freitext: String` erweitert

---

## Phase 16 ✅ – v0.10.0 Reaktionsscore (48h-Fenster)

### ReaktionsScoreAnalyse (neu, Domain)
- `analysiere(futterEintraege, symptome, minBeobachtungen)` → `List<ScoreEintrag>`
- 48h-Fenster: Symptome in 0–2 Tagen nach Erstgabe/Provokation
- Score = `durchschnittSchweregrad × haeufigkeit` (0–5)
- Häufigkeit = Anteil Erstgaben mit mind. 1 Symptom im Fenster
- Sortierung: höchster Score zuerst
- `risikoLabel(score)`: Sehr hoch / Hoch / Mittel / Niedrig / Kein Signal

### StatistikViewModel
- `ReaktionsScore` Data-Class: +`durchschnittSchweregrad`, `haeufigkeit`, `istSignifikant`, `beispielDaten`
- `StatistikUiState`: `reaktionsScoreVerfuegbar` Boolean
- `ladeStatistik()`: `futterRange` + `ReaktionsScoreAnalyse.analysiere()` → State
- Import `ReaktionsScoreAnalyse` ergänzt

### ReaktionsScoreCard (neu)
- Header mit Gesamtzahl
- `ScoreZeile` je Produkt: Score-Balken (LinearProgressIndicator), Risiko-Badge
- Farbe nach Score: Rot ≥3,5 / Orange ≥2,5 / Gelb ≥1,5 / Grün <1,5
- Klickbar → ausgeklappt: Einführungsdaten, Score-Erläuterung, Signifikanz-Warnung
- Meta-Chips: Anzahl Erstgaben, Ø Schweregrad, Häufigkeit %
- Methodik-Hinweis am Ende

### StatistikScreen
- `ReaktionsScoreCard` vor Gewichtsverlauf (nur wenn `reaktionsScoreVerfuegbar`)

---

## Phase 17 ✅ – v0.10.0 Hund-Vergleich (Statistik)

### StatistikUiState
- `vergleichsHundId: Long?` — null = kein Vergleich
- `vergleichsKpi: KpiState?` — KPIs des Vergleichshunds
- `vergleichsSymptomVerlauf: List<Pair<LocalDate, Double>>` — für zukünftigen Chart-Overlay

### StatistikViewModel
- `selectVergleichsHund(id?)`: State-Update + `ladeStatistik()` neu
- `ladeStatistik()`: nach Haupt-Update Vergleichshund-Daten parallel laden
  (symptomeRange, pollenRange, allergenCount)
- Nur wenn `vergleichsId != null && vergleichsId != hundId`

### HundVergleichCard (neu)
- Picker: eingeklappt/ausgeklappt, RadioButtons, X-Button zum Beenden
- KPI-Tabelle: Haupthund vs. Vergleichshund nebeneinander
  - Symptomtage, Ø Schweregrad, Pollentage, Allergene
  - Δ-Spalte mit Farb-Kodierung (grün = besser, rot = schlechter)
  - ✓-Icon beim besseren Wert
  - `lowerIsBetter`-Flag je KPI

### StatistikScreen
- `HundVergleichCard` nach Zeitraum-Filter, vor Lade-Indikator
- Nur sichtbar wenn `state.hunde.size > 1`

---

## Phase 18 ✅ – v0.10.0 Zutat-zu-Zutat-Vergleich

### ZutatVergleichViewModel (neu)
- `NaehrstoffZeile`: key, label, gruppe, einheit, wertA, wertB, delta, deltaProzent
- `ZutatVergleichUiState`: zutatA/B, naehrstoffeA/B, vergleichsZeilen, suchA/B, Filter
- `selectZutatA/B()`: lädt Nährstoffe via `ZutatenRepository.getNaehrstoffe()`
- `tausche()`: A ↔ B wechseln
- `clearZutatA/B()`: Auswahl zurücksetzen
- `toggleNurUnterschiede/NurMitWerten()`: Live-Filter
- `berechne()`: privat, NaehrstoffKatalog-basiert, alle Gruppen, Filter angewandt

### ZutatVergleichScreen (neu)
- Zwei `ZutatPickerBox`-Komponenten nebeneinander mit ⇄-Tausch-Button
- `ZutatPickerBox`: Suchfeld → Dropdown (max. 5 Treffer), gewählte Zutat als Card mit ×
- Filter-Chips: "Nur mit Werten" / "Nur Unterschiede" + Zeilen-Anzahl
- Tabellen-Header: Nährstoff | A-Name | B-Name | Δ B–A
- `VergleichsZeile`: farbiger Hintergrund je Delta-Richtung, Δ % wenn ≥1%
- `fmtWert()`: adaptives Format (0–4 Dezimalstellen je Größe)
- Hinweis-Karte (per 100g, Farb-Erklärung)
- Leer-Zustand wenn noch keine zwei Zutaten gewählt

### Navigation
- `Screen.ZutatVergleich` in NavGraph
- Route in AllerPawApp + Import
- `ZutatenScreen`: Balance-Icon ⚖️ in TopAppBar → ZutatVergleich
- `ZutatenScreen`: `onNavigateToVergleich` Parameter ergänzt

---

## Phase 19 ✅ – v0.10.0 Food-API Import (USDA + Open Food Facts + Edamam)

### Neue Felder
- `ZutatEntity`: `quelle: String = "manuell"`, `quelleId: String = ""`
- `SettingsRepository`: `usdaApiKey`, `edamamAppId/Key` Flows + `getString/setString`
- DB-Version: 4 → 5 (MIGRATION_4_5: ALTER TABLE zutaten + 2 Spalten)

### FoodApiClient (neu)
- **USDA FoodData Central**: Suche + Detail (Nährstoff-ID-Map, 30+ Nährstoffe)
- **Open Food Facts**: Textsuche + Barcode (kein Key, User-Agent gesetzt)
- **Edamam**: Textsuche (App-ID + App-Key, Edamam-Kürzel-Map)
- Einheitliches `FoodApiResult` (40+ Felder, alle per 100g)
- `FoodApiResponse` Sealed Class

### FoodApiRepository (neu)
- `sucheAlle()`: alle aktiven Quellen parallel, dedupliziert
- `sucheBarcode()`: nur OFF
- `detailUsda()`: Detail-Abruf für mehr Nährstoffe
- `importiereAlsZutat()`: → `ZutatEntity` + `ZutatNaehrstoffEntity` speichern
- Kategorie-Mapping (englisch/deutsch → DE-Kategorie)
- NRC-Key-Mapping (40 Felder)

### FoodImportViewModel (neu)
- Lädt API-Keys aus DataStore bei Init
- `suche()`, `sucheBarcode()`, `selectErgebnis()` (USDA Detail nachladen)
- `importiere()`, Filter-Toggle

### FoodImportScreen (neu)
- Quellen-Chips (gesperrt wenn kein Key)
- Suchleiste + Button, Ergebnis-Liste (`FoodErgebnisCard`)
- Detail-Ansicht (`FoodDetailCard`): Makros, Mineralien, Vitamine in Rasterform
- Import-Button → direkter DB-Import
- Erfolgs/Fehler-Cards

### ApiKeysViewModel + ApiKeysScreen (neu)
- USDA Key, Edamam App-ID + App-Key
- PasswordVisualTransformation + Toggle-Sichtbarkeit
- Registrierungs-Link per `LocalUriHandler`
- 3s-Auto-Dismiss Erfolgs-Meldung

### Navigation
- `Screen.FoodImport`, `Screen.ApiKeys` in NavGraph
- `ZutatenScreen`: CloudDownload-Icon → FoodImport
- `SettingsScreen`: "API-Keys (USDA, Edamam)" ListItem → ApiKeys
- Alle Routen in AllerPawApp registriert

---

## Phase 20 ✅ – v0.10.0 UI-Integration Auto-Befüllung Umwelt-Tab

### WetterBanner (neu, in TagebuchViewModel.kt)
- `WetterBanner(stadtName, tempMin, tempMax, feuchte, regenMm, pollenMap, autoBefuellt)`
- Repräsentiert den heutigen Wetter-Snapshot für den UmweltTab-Header

### TagebuchUiState
- `wetterBanner: WetterBanner?`
- `wetterFehler: String?`
- `wetterLaedt: Boolean`

### TagebuchViewModel
- `WetterRepository` + `SettingsRepository` injiziert
- `ladeWetter()`:
  - Standort aus `settingsRepo.standortLat/Lon`
  - `wetterRepo.getWetter()` (BrightSky/DWD)
  - `wetterRepo.getPollen()` (Open-Meteo)
  - Pollen: Tages-Stärke 0–5 je Art, nur > 0 anzeigen
  - Stadtname aus `settingsRepo.getString("standort_name")`
  - Kein Reload wenn Banner bereits da
- `befuelleAktuelleUmweltAusWetter()`:
  - Bestehenden Umwelt-Eintrag heute finden oder neu anlegen
  - `repo.saveUmwelt()` mit Wetter-Feldern (`tempMinC/MaxC`, `luftfeuchte`, `niederschlagMm`)
  - `repo.savePollenLog()` für alle Pollen-Arten
  - `autoBefuellt = true` im Banner setzen

### SettingsRepository
- `setStandort(lat, lon, name)` — `name`-Parameter ergänzt
- Setzt `standort_name` via `setString()`

### UmweltTab
- `LaunchedEffect(state.selectedHundId)` → `vm.ladeWetter()` (bereits vorhanden)
- `LinearProgressIndicator` wenn `state.wetterLaedt`
- Fehler-Card wenn `state.wetterFehler` gesetzt
- `WetterBanner`-Card: Temp/Feuchte/Niederschlag + Pollen-Stärken + Befüllen-Button
- Nach Befüllung: ✅-Badge statt Button
- Manueller Laden-Button wenn kein Banner

### FEATURE.md
- UI-Integration Auto-Befüllung Umwelt-Tab: 🔲 → ✅

---

## Phase 21 ✅ – v0.10.0 Backup Wiederherstellen (Bugfix + Abschluss)

### BackupRepository
- `CURRENT_DB_VERSION`: 3 → **5** (DB wurde in Phase 19 auf v5 angehoben)
- Versionscheck prüft jetzt korrekt ob Backup mit aktueller DB kompatibel ist

### Status
- `BackupViewModel` war bereits vollständig: `exportBackup()`, `onRestoreFileSelected()`,
  `confirmRestore()`, `dismissRestoreConfirm()`, `dismissVersionMismatch()`, `clearMessages()`
- `BackupScreen` war bereits vollständig: Filepicker (`OpenDocument`), Bestätigungs-Dialog,
  Versions-Mismatch-Dialog, App-Neustart nach Erfolg

### 🎉 Alle Features vollständig
Sämtliche geplanten Features aus FEATURE.md sind implementiert:
- API Level 36 (v0.10.0)
- i18n (228 Strings, DE + EN, erweiterbar)
- Toleranzbalken UI
- Google Sheets Import + Export
- Gewichtsverlauf + Vico Chart
- NRC Lebensphasen UI
- Manueller Kcal-Bedarf
- Kreuzallergie-Analyse (Protein-Matrix)
- Reaktionsscore (48h-Fenster)
- Hund-Vergleich (Statistik)
- Zutat-zu-Zutat-Vergleich
- Rezept-Positionen Phase 3 (Gramm, Sub-Rezept, Freitext, Reihenfolge)
- USDA + Open Food Facts + Edamam Import
- UI-Integration Auto-Befüllung Umwelt-Tab
- Backup erstellen + wiederherstellen
