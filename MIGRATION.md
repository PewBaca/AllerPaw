# AllerPaw – Android Migration Plan (Web → Android)

> Version: 0.1  
> Stand: 2026-04-23  
> Ausgangsbasis: Web-App v2.3.1 (Vanilla HTML + ES Modules + Google Sheets API)  
> Ziel: Native Android App (Kotlin + Jetpack Compose)

---

## Überblick

Die bisherige App läuft als statische Website auf GitHub Pages und kommuniziert direkt mit der Google Sheets API. Der Umbau zur Android-App behält die **Google Sheets API als Datenbank** vollständig bei — nur die Präsentationsschicht wird ersetzt.

```
Vorher:  Browser  →  Vanilla JS (ES Modules)  →  Google Sheets API
Nachher: Android  →  Kotlin / Compose          →  Google Sheets API
```

Der Google-Sheets-basierte Datenspeicher ist **ein bewusster Beibehalt**: Bestehende Nutzer verlieren keine Daten, und die vertraute Struktur (Dual-Spreadsheet, Soft-Delete, 2-Zeilen-Header) bleibt identisch.

---

## Tech-Stack (Android)

| Bereich | Technologie | Begründung |
|---------|-------------|-----------|
| Sprache | Kotlin | Standard für moderne Android-Entwicklung |
| UI | Jetpack Compose | Deklarativ, wartungsfreundlich, gut testbar |
| HTTP / API | Retrofit 2 + OkHttp | Typsichere REST-Calls gegen Sheets API |
| Auth | Google Sign-In for Android (Credential Manager) | Ersatz für `gsi/client` im Browser |
| Lokale DB / Cache | Room (SQLite) | Ersatz für `sessionStorage` und `localStorage` |
| Charts | Vico oder MPAndroidChart | Ersatz für Chart.js |
| Dependency Injection | Hilt | Standardisiert; testfreundlich |
| Async | Kotlin Coroutines + Flow | Ersatz für Promise-Ketten im JS-Code |
| Build | Gradle (Kotlin DSL) | |
| Min SDK | API 26 (Android 8.0) | Breite Kompatibilität |
| Target SDK | API 35+ | |

---

## Phasen

### Phase 1 – Projektaufbau & Auth (v0.1)

**Ziel:** Android-Projekt steht, Google-Login funktioniert, erste Sheets-Abfrage erfolgreich.

- [ ] Android-Projekt anlegen (Kotlin, Compose, Hilt)
- [ ] GitHub-Repository einrichten (`.github/workflows` für APK-Build via Actions)
- [ ] Google Cloud Project: OAuth2-Client-ID für Android registrieren (SHA-1)
- [ ] Google Sign-In mit Credential Manager implementieren (`auth.js` → `AuthRepository.kt`)
- [ ] Sheets API v4: `GET spreadsheets/{id}/values/{range}` via Retrofit
- [ ] Verbindungstest-Screen (ersetzt Einstellungs-Verbindungstest)
- [ ] Einstellungs-Screen: Spreadsheet-IDs, Standort, DWD-Region, USDA-Key
- [ ] Lokale Persistenz der Einstellungen via DataStore (ersetzt `localStorage`)

**Verify:** Login → Einstellungen → Verbindungstest → erste Sheet-Daten sichtbar

---

### Phase 2 – Stammdaten (Hunde & Zutaten) (v0.2)

**Ziel:** `stammdaten.js` vollständig portiert — CRUD für Hunde, Zutaten, Nährstoffe.

- [ ] `HundeScreen`: Liste, Anlegen, Bearbeiten, Soft-Deaktivieren
- [ ] `GewichtDialog`: Gewicht eintragen, Verlaufstabelle (letzte 15)
- [ ] `ZutatenScreen`: Liste, Anlegen, Bearbeiten, Soft-Delete + Undo
- [ ] `NaehrstoffDialog`: 39 NRC-Nährstoffe im einklappbaren Bereich
- [ ] USDA + Open Food Facts paralleler Import (`Promise.allSettled` → `async/await` mit `withContext`)
- [ ] Import-Vorschau: beide Quellen nebeneinander; nur leere Felder befüllen
- [ ] In-Memory-Cache Stammdaten (ersetzt `store.js` / `STORE.loadAll()`)
- [ ] Room-Tabellen für gecachte Stammdaten anlegen

**Verify:** Hund anlegen → Zutat anlegen → Nährstoff importieren → Werte korrekt in Sheet

---

### Phase 3 – Futterrechner (v0.3)

**Ziel:** `rechner.js` portiert — Rezepte, Nährstoffanalyse, Rezept-Mix.

- [ ] `RechnerScreen`: Rezeptliste je Hund
- [ ] Rezept anlegen / bearbeiten: Zutaten + Gramm
- [ ] `resolveRezept()` → Kotlin: rekursive Auflösung (max. 5 Ebenen, Zykluserkennung), ohne Zwischenrundung
- [ ] Nährstoffanalyse: Balkenanzeige mit Ampelfarben (Compose Canvas oder LinearProgressIndicator)
- [ ] Toleranzbalken: Min / Max / Empfehlung je Nährstoff
- [ ] Kcal-Berechnung + Gekocht-Flag (Kochverlust nur B-Vitamine)
- [ ] Nährstoff-Detail-Popup (NRC / AAFCO / FEDIAF)
- [ ] Rezept-Vergleich A vs. B (Delta-Spalte)
- [ ] Soft-Delete Rezepte + Undo

**Verify:** Rezept erstellen → Nährstoffe korrekt → Mix testen → Vergleich prüfen

---

### Phase 4 – Tagebuch (v0.4)

**Ziel:** `tagebuch.js` + `ansicht.js` portiert — alle 8 Tabs, Edit, Soft-Delete.

- [ ] `TagebuchScreen` mit Tab-Navigation (Compose TabRow oder BottomNavigation)
- [ ] Umwelt-Tab: Felder + Pollen-Toggle-Buttons
- [ ] Symptom-Tab
- [ ] Futter-Tab: Multi-Futter mit Kcal-Berechnung
- [ ] Ausschluss-Tab
- [ ] Allergen-Tab
- [ ] Tierarzt-Tab
- [ ] Medikament-Tab
- [ ] Phasen-Tab: Phasentracker inkl. Fortschritts-Banner
- [ ] Eintrag-Cards: Anzeige, Edit-Dialog, Soft-Delete + Undo-Banner (8 Sek.)
- [ ] Session-Cache für Tagebuch-Daten (Room, TTL 10 Min)

**Verify:** Eintrag anlegen → bearbeiten → löschen → Undo → korrekt in Sheet

---

### Phase 5 – Wetter & Pollen (v0.5)

**Ziel:** `wetter.js` portiert — automatischer Wetter- und Pollen-Abruf.

- [ ] BrightSky API (DWD): Außentemp, Feuchte, Niederschlag
- [ ] Pollen DWD OpenData: 8 Pollenarten, 18 Regionen
- [ ] Open-Meteo Air Quality API: koordinatenbasiert
- [ ] Pollen-Auswahl UI: Toggle-Chips mit Stärke; Vorauswahl ab „mittel"
- [ ] Eigene Pollenarten: Room-Tabelle (ersetzt `localStorage`)
- [ ] Pollen_Log: je Pollenart eine eigene Sheet-Zeile
- [ ] Standort: GPS-Permission anfragen oder manuell aus Einstellungen

**Verify:** Wetter geladen → Pollen angezeigt → Eintrag speichert Pollen_Log korrekt

---

### Phase 6 – Statistik (v0.6)

**Ziel:** `statistik.js` portiert — Chart, Heatmap, Korrelation, Reaktionsscore.

- [ ] `StatistikScreen`: Hund-Filter, Zeitraum-Filter, Hund-Vergleich
- [ ] KPI-Kacheln: Symptomtage, Ø Schweregrad, Pollentage
- [ ] Konfigurierbarer Chart (Vico / MPAndroidChart): Temperaturband, Symptom-Fläche, Pollen, Gewicht
- [ ] Pollen-Popup-Dialog (Bottom Sheet)
- [ ] Symptom-Muster-Heatmap (Wochentag + Monat)
- [ ] Korrelationsanalyse (Pollen / Temp / Feuchte vs. Schweregrad)
- [ ] Zutaten-Reaktionsscore mit Chip-Filter
- [ ] Phasen-Timeline
- [ ] Futter-Reaktionen + Medikamente-Liste
- [ ] Cache-Status-Anzeige

**Verify:** Chart zeigt korrekte Daten → Heatmap ab 14 Einträgen → Korrelation min. 3 Datenpunkte

---

### Phase 7 – Tierarzt-Export (v0.7)

**Ziel:** `export.js` portiert — konfigurierbarer PDF-Bericht.

- [ ] Export-Dialog: Von/Bis-Datum + Schnellauswahl
- [ ] 9 Toggle-Sektionen (Deckblatt, Symptome, Allergene, Ausschlussdiät, Phasen, Medikamente, Futter, Reaktionsscore, Korrelation)
- [ ] PDF-Generierung via Android `PrintManager` oder `PdfDocument`
- [ ] Schwarz-Weiß-Darstellung; Disclaimer + Exportdatum

**Verify:** Export mit allen Sektionen → PDF korrekt gerendert → Share-Intent funktioniert

---

### Phase 8 – Mehrsprachigkeit & Finalisierung (v0.8)

**Ziel:** i18n, Dark Mode, Tests, Store-Release-Vorbereitung.

- [ ] `i18n.js` → Android `strings.xml` (DE + EN)
- [ ] Sprachschalter in Einstellungen
- [ ] Dark Mode: Material You Theme
- [ ] Unit-Tests für `resolveRezept`, Nährstoffberechnung, Kcal
- [ ] UI-Tests für kritische Flows (Login, Eintrag anlegen, Soft-Delete + Undo)
- [ ] GitHub Actions: APK-Build bei Push auf `main`
- [ ] ProGuard / R8 konfigurieren
- [ ] Erste öffentliche Beta (GitHub Releases)

---

## Datenbank-Kompatibilität

Die Google-Sheets-Struktur bleibt **unverändert**. Alle Konventionen aus der Web-App gelten weiter:

- Zeile 1: Anzeige-Header (Deutsch)
- Zeile 2: API-Header (Englisch, snake_case)
- Daten ab Zeile 3
- Neue Spalten immer ans Ende anhängen
- Pflichtfelder: `created_at`, `deleted`, `deleted_at`
- Spalten werden per Positionsindex gelesen (nicht per Name)
- Soft-Delete: `deleted = TRUE`, `deleted_at = ISO 8601`

Bestehende Spreadsheets von Web-App-Nutzern sind **ohne Anpassung** kompatibel.

---

## Modul-Mapping: JS → Kotlin

| Web-App Modul | Android-Äquivalent |
|---------------|-------------------|
| `auth.js` | `AuthRepository.kt` + Google Credential Manager |
| `sheets.js` | `SheetsApiService.kt` (Retrofit) + `SheetsRepository.kt` |
| `config.js` | `SettingsRepository.kt` + DataStore |
| `store.js` | `StammdatenRepository.kt` + In-Memory-Cache |
| `cache.js` | `TagebuchRepository.kt` + Room (TTL-Logik) |
| `ui.js` | Compose-Komponenten + ViewModel |
| `form.js` | Compose State (remember, mutableStateOf) |
| `wetter.js` | `WetterRepository.kt` + `PollenRepository.kt` |
| `rechner.js` | `RechnerViewModel.kt` + `NutritionCalculator.kt` |
| `tagebuch.js` | `TagebuchViewModel.kt` je Tab |
| `ansicht.js` | `TagebuchListScreen.kt` + `EditDialog.kt` |
| `stammdaten.js` | `StammdatenViewModel.kt` + `ImportRepository.kt` |
| `statistik.js` | `StatistikViewModel.kt` + Chart-Composables |
| `export.js` | `ExportViewModel.kt` + `PdfExporter.kt` |
| `i18n.js` | `strings.xml` (DE/EN) + `LocaleHelper.kt` |

---

## Risiken & Offene Punkte

| Risiko | Einschätzung | Maßnahme |
|--------|-------------|---------|
| Google OAuth2 auf Android vs. Web | Mittel – andere Client-ID und Flow | SHA-1 im Cloud Console registrieren; Credential Manager verwenden |
| Chart-Bibliothek Parität | Mittel – Compose-native Charts weniger ausgereift | Vico als erste Wahl; MPAndroidChart als Fallback |
| Pollen-CORS-Proxy nicht nötig | Niedrig – Android hat keinen CORS | Direkte HTTP-Requests möglich |
| Drucken / PDF | Mittel – `window.print()` nicht verfügbar | Android `PrintManager` oder `PdfDocument` |
| Performance bei großen Sheets | Mittel – viele Zeilen möglich | Pagination implementieren; Room als Cache |
| Mehrere Hunde gleichzeitig | Niedrig | Bereits in JS vorhanden; 1:1 portierbar |

---

## Naming & Versionierung

- App-Name: **AllerPaw**
- Package: `com.allerpaw.app` (vorgeschlagen)
- Versionierung: Semantic Versioning (`MAJOR.MINOR.PATCH`)
- Aktuelle Version: **0.1** (Pre-Alpha, kein Feature-Complete)
- GitHub Releases: APK je Tag
- `versionCode` = inkrementell (1, 2, 3 …)
- `versionName` = `0.1.0`, `0.2.0` etc.

---

## Nicht portiert (vorerst)

| Feature | Begründung |
|---------|-----------|
| PWA / Offline-Installation | Entfällt; Android-App ist die native Lösung |
| GitHub Pages Hosting | Entfällt; APK auf GitHub Releases |
| `window.print()` für Export | Wird durch Android PrintManager ersetzt |
| CORS-Proxy für DWD-Pollen | Entfällt; Android hat kein CORS-Problem |
