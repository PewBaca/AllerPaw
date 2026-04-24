# AllerPaw – Android Migration Plan (Web → Android)

> Version: 0.2  
> Stand: 2026-04-24  
> Ausgangsbasis: Web-App v2.3.1 (Vanilla HTML + ES Modules)  
> Ziel: Native Android App (Kotlin + Jetpack Compose + Room)

---

## Architektur-Entscheidung (geändert gegenüber v0.1)

```
Vorher (v0.1-Plan):  Android  →  Kotlin / Compose  →  Google Sheets API (Primär-DB)
Jetzt  (v0.2-Ist):   Android  →  Kotlin / Compose  →  Room SQLite (Primär-DB)
                                                    →  Google Sheets (nur Export/Backup)
```

**Begründung:** 100 % Offline-Betrieb, Datensouveränität, keine API-Rate-Limits, deutlich bessere Performance. Google Sheets bleibt als optionaler Export-Kanal für Nutzer, die ihre Daten in Sheets weiterführen möchten.

---

## Tech-Stack

| Bereich | Technologie | Status |
|---------|-------------|--------|
| Sprache | Kotlin | ✅ |
| UI | Jetpack Compose + Material You | ✅ |
| Lokale DB | Room (SQLite) – Primärspeicher | ✅ |
| DI | Hilt | ✅ |
| Async | Kotlin Coroutines + Flow | ✅ |
| HTTP | Retrofit 2 + OkHttp | ✅ (konfiguriert, noch nicht genutzt) |
| Auth | Google Credential Manager | 🔲 Platzhalter |
| Charts | Vico (Compose-nativ) | 🔲 konfiguriert, noch nicht genutzt |
| DataStore | Preferences DataStore | ✅ |
| Build | Gradle Kotlin DSL + Version Catalog | ✅ |
| Min SDK | API 26 (Android 8.0) | ✅ |
| Target SDK | API 35 | ✅ |

---

## Phasen

### Phase 1 – Projektaufbau & Datenschicht (v0.1–0.2) ✅ ABGESCHLOSSEN

- [x] Android-Projekt anlegen (Kotlin, Compose, Hilt)
- [x] Gradle Version Catalog (libs.versions.toml)
- [x] Room-Datenbank mit 19 Entities (Stammdaten + Tagebuch)
- [x] Alle 5 DAOs mit Soft-Delete-Unterstützung
- [x] HundRepository, SessionRepository
- [x] NRC-Nährstoff-Katalog (29 Nährstoffe), RER/MER-Rechner
- [x] RezeptAnalyseUseCase (Ampelstatus)
- [x] Navigation (Bottom Nav, NavHost, 5 Tabs)
- [x] Material You Theme + Dark Mode
- [x] LoginScreen (Platzhalter mit DataStore-Session)
- [x] StammdatenScreen (Hund-Liste + Edit-Dialog)
- [x] RechnerScreen (RER/MER-Anzeige, NRC-Tabelle Grundgerüst)
- [x] strings.xml DE + EN

**Verify:** App startet → Login → Bottom Nav → Hund anlegen → Gewicht erscheint im Rechner ✅

---

### Phase 2 – Stammdaten vollständig + Tagebuch (v0.3)

- [ ] ZutatenScreen: Liste, Anlegen, Bearbeiten, Soft-Delete + Undo
- [ ] NaehrstoffDialog: 29+ NRC-Nährstoffe, gruppiert, einklappbar
- [ ] IE-Eingabe: Vitamin A, D, E mit Formauswahl (natürlich / synthetisch / Acetat)
- [ ] Supplement-Modus: Tablette / Tropfen / Pulver
- [ ] USDA + Open Food Facts paralleler Import
- [ ] ToleranzenScreen: Min/Max/Empfehlung je Nährstoff und Hund
- [ ] ParameterScreen: Kochverlust, Portionen/Tag, RER-Faktor
- [ ] TagebuchScreen: Tab-Navigation (8 Tabs)
- [ ] UmweltTab: alle Felder inkl. Pollen-Toggle
- [ ] SymptomTab: Kategorie, Schweregrad 0–5, Körperstelle
- [ ] FutterTab: Multi-Rezept mit Gramm + Kcal
- [ ] AusschlussTab, AllergenTab, TierarztTab, MedikamentTab
- [ ] PhasenTab: Phasentracker + Fortschrittsbalken
- [ ] Eintrag-Cards mit Edit-Dialog + Soft-Delete + Undo-Banner (8 Sek.)
- [ ] FloatParser.kt: Komma/Punkt-Dezimaltrenner

**Verify:** Alle 8 Tagebuch-Tabs → Eintrag anlegen → bearbeiten → löschen → Undo

---

### Phase 3 – Futterrechner vollständig + Statistik (v0.4)

- [ ] RezeptEditor: Zutaten + Gramm, Tabletten-Stückzahl
- [ ] resolveRezept(): rekursiv, max. 5 Ebenen, Zykluserkennung, ohne Zwischenrundung
- [ ] Nährstoffbalken: Compose Canvas, Ampelfarben, Toleranzbalken
- [ ] Gekocht-Flag: Kochverlust nur B-Vitamine (Faktor 0.70, konfigurierbar)
- [ ] Skalierungsfaktor: ×0.25 / ×0.5 / ×1 / ×2 + freies Eingabefeld
- [ ] Ca:P-Verhältnis + Omega 6:3 als Badge
- [ ] Rezept-Vergleich A vs. B (Delta-Spalte)
- [ ] Zutat-zu-Zutat-Vergleich (⚖️-Button, alle 39 Nährstoffe, Delta-Pfeil)
- [ ] StatistikScreen: KPI-Kacheln, konfigurierbarer Chart (Vico)
- [ ] Symptom-Heatmap (ab 14 Einträgen)
- [ ] Korrelationsanalyse (min. 3 Datenpunkte)
- [ ] Reaktionsscore (48-h-Fenster, min. 3 Beobachtungen)
- [ ] Phasen-Timeline
- [ ] Hund-Vergleich (zweites Hund-Dropdown)

---

### Phase 4 – Wetter, Export, Backup (v0.5)

- [ ] BrightSky API (DWD): Außentemp, Feuchte, Niederschlag
- [ ] Pollen DWD OpenData + Open-Meteo
- [ ] Eigene Pollenarten (Room-Tabelle bereits vorhanden)
- [ ] ExportScreen: PDF-Export via Android PrintManager / PdfDocument
- [ ] 9 Toggle-Sektionen (Deckblatt, Symptome, Allergene, Phasen …)
- [ ] CSV-Export je DB-Tabelle (ZIP)
- [ ] Google Sheets Export (Sheets API v4)
- [ ] SQLite-Backup (lokal oder Google Drive)

---

### Phase 5 – Finalisierung & Store (v0.6+)

- [ ] Google OAuth2 (Credential Manager) – echte Implementierung
- [ ] Sprachschalter in Einstellungen (sofort ohne Neustart)
- [ ] NRC-Werte für Welpen und Senioren (NRC 2006 Tabellen)
- [ ] Monetarisierung: optionale Werbung bei Login + Spendenoption
- [ ] Geteilte Zutaten-/Rezept-Datenbanken (Import/Export)
- [ ] Hinweise/Notizen pro Nährstoff, Futtermittel, Statistik (mehrsprachig)
- [ ] Erststart-Wizard: Sprachauswahl
- [ ] Unit-Tests: resolveRezept, Nährstoffberechnung, Kcal
- [ ] UI-Tests: Login, Eintrag anlegen, Soft-Delete + Undo
- [ ] GitHub Actions: APK-Build bei Push auf main
- [ ] ProGuard / R8
- [ ] Erste öffentliche Beta (GitHub Releases)

---

## Modul-Mapping: JS → Kotlin

| Web-App Modul | Android-Äquivalent | Status |
|---------------|-------------------|--------|
| `auth.js` | `AuthRepository.kt` + Credential Manager | 🔲 Platzhalter |
| `sheets.js` | `SheetsApiService.kt` + `SheetsRepository.kt` | 🔲 geplant Phase 4 |
| `config.js` | `SettingsRepository.kt` + DataStore | 🔲 Phase 2 |
| `store.js` | `StammdatenRepository.kt` + Room | 🔲 Phase 2 |
| `cache.js` | `TagebuchRepository.kt` + Room | 🔲 Phase 2 |
| `rechner.js` | `RechnerViewModel.kt` + `NaehrstoffDomain.kt` | 🟡 Teilweise (RER/MER) |
| `tagebuch.js` | `TagebuchViewModel.kt` je Tab | 🔲 Phase 2 |
| `ansicht.js` | `TagebuchListScreen.kt` + `EditDialog.kt` | 🔲 Phase 2 |
| `stammdaten.js` | `StammdatenViewModel.kt` + `ImportRepository.kt` | 🟡 Teilweise (Hund) |
| `wetter.js` | `WetterRepository.kt` + `PollenRepository.kt` | 🔲 Phase 4 |
| `statistik.js` | `StatistikViewModel.kt` + Chart-Composables | 🔲 Phase 3 |
| `export.js` | `ExportViewModel.kt` + `PdfExporter.kt` | 🔲 Phase 4 |
| `i18n.js` | `strings.xml` (DE/EN) + `LocaleHelper.kt` | 🟡 Grundstruktur |

---

## Risiken & Offene Punkte

| Risiko | Einschätzung | Maßnahme |
|--------|-------------|---------|
| Google OAuth2 auf Android | Mittel | SHA-1 in Cloud Console registrieren; Credential Manager |
| Vico Chart-Parität zu Chart.js | Mittel | Vico als erste Wahl; Fallback MPAndroidChart |
| PDF-Generierung | Mittel | Android PrintManager oder PdfDocument |
| NRC-Werte Welpen/Senioren | Niedrig | NRC 2006 Tabellen liegen vor |
| Performance bei vielen Einträgen | Niedrig | Room + Flow + Paging falls nötig |

---

## Naming & Versionierung

- App-Name: **AllerPaw**
- Package: `com.allerpaw.app`
- Versionierung: Semantic Versioning
- Aktuelle Version: **0.2.0** (Phase 1 abgeschlossen)
- `versionCode` inkrementell; `versionName` = 0.2.0
