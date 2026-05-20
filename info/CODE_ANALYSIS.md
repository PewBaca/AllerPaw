# AllerPaw – Code-Analyse & Implementierungsabgleich (v0.2)

> Letzte Aktualisierung: 2026-04-24  
> Analysierte Basis: Web-App v2.3.1 + Android-Implementierung v0.2

---

## Android v0.2 – Implementierte Dateien

| Datei | Inhalt | Anmerkungen |
|-------|--------|-------------|
| `build.gradle.kts` (root + app) | AGP 8.5, Kotlin 2.0, KSP | Version Catalog |
| `libs.versions.toml` | Alle Abhängigkeiten zentral | Hilt, Room, Retrofit, Vico, Moshi |
| `AppDatabase.kt` | 19 Entities, Room Version 1 | TypeConverters für Instant + LocalDate |
| `StammdatenEntities.kt` | Hund, HundGewicht, Zutat, ZutatNaehrstoff, Rezept, RezeptZutat, Parameter, Toleranz | Soft-Delete überall |
| `TagebuchEntities.kt` | Umwelt, PollenLog, EigenePollenart, Symptom, Futter, FutterItem, Ausschluss, Allergen, Tierarzt, Medikament, AusschlussPhase | Alle 8 Tabs abgedeckt |
| `Daos.kt` | HundDao, ZutatenDao, RezeptDao, TagebuchDao, ParameterDao | Flow-basiert, Soft-Delete |
| `NaehrstoffDomain.kt` | 29 NRC-Nährstoffe, EnergieBedarf (RER/MER), RezeptAnalyseUseCase, NaehrstoffErgebnis | Ampelstatus: OK/MANGEL/UEBERSCHUSS/UEBERSCHRITTEN |
| `HundRepository.kt` | CRUD + Gewichtsverlauf | Flow-basiert |
| `SessionRepository.kt` | Login-State via DataStore | |
| `DatabaseModule.kt` | Hilt-Provider für DB + DAOs | |
| `NetworkModule.kt` | Retrofit, OkHttp, DataStore | Basis-URL Platzhalter |
| `DomainModule.kt` | RezeptAnalyseUseCase als Singleton | |
| `AllerPawApp.kt` | Root-Composable, Scaffold, NavHost, Bottom Nav | |
| `NavGraph.kt` | Screen-Routes, BottomNavItem-Enum | 5 Tabs + Settings |
| `Theme.kt` | Material You, Dynamic Color | Dark Mode via System |
| `LoginScreen.kt` + `LoginViewModel.kt` | Demo-Login, DataStore-Session | Google-Token = Platzhalter |
| `StammdatenScreen.kt` + `StammdatenViewModel.kt` | Hund-Liste, HundCard, Edit-Dialog | Geschlecht, Kastration, Gewicht |
| `RechnerScreen.kt` + `RechnerViewModel.kt` | RER/MER, Aktivitätsfaktor-Slider, NRC-Tabelle | Rezept-Analyse noch leer |
| `TagebuchScreen.kt` | Placeholder | Phase 2 |
| `StatistikScreen.kt` | Placeholder | Phase 3 |
| `ExportScreen.kt` | Placeholder | Phase 4 |
| `SettingsScreen.kt` | Grundgerüst | Ausbau Phase 2 |
| `strings.xml` (DE + EN) | App-Name, 5 Nav-Labels | Ausbau folgt |

---

## Korrekturen gegenüber Web-App-Dokumentation

### 1. Kochverlust-Faktor (kritisch)

**Web-App-Code:** `rechner.js:681`: `const cookFactor = 1 - (params['kochverlust_b_vitamine'] || 0.30)`

- Standardverlust: **30 %** → angewendeter Faktor: **0.70**
- Konfigurierbar via `ParameterEntity` (Schlüssel: `kochverlust_b_vitamine`)
- Gilt nur für: B1, B2, B3, B5, B6, B9, B12

**Alte MD (falsch):** „Kochverlustfaktor 0,75"  
**Neue MD (korrekt):** Verlust 0.30, Faktor 0.70, konfigurierbar ✅ korrigiert in FEATURE.md

---

### 2. Skalierungsfaktor (fehlte in MD)

**Web-App-Code:** Scale-Buttons ×0.25, ×0.5, ×1, ×2 + freies Eingabefeld  
**Jetzt dokumentiert** in FEATURE.md ✅

---

### 3. Portionen pro Tag (fehlte in MD)

**Web-App-Code:** `rechner.js`: `params['portionen_pro_tag'] || 2` → „g je Portion"  
**Jetzt dokumentiert** in FEATURE.md + Entity `ParameterEntity` vorhanden ✅

---

### 4. Zutat-zu-Zutat-Vergleich (fehlte in MD)

**Web-App-Code:** `selectZutatForCompare()`, `showZutatVergleich()`, ⚖️-Button in Zutaten-Liste  
**Jetzt dokumentiert** in FEATURE.md, geplant für Phase 3 ✅

---

### 5. Architektur-Entscheidung: Room statt Sheets als Primär-DB

Gegenüber dem ursprünglichen v0.1-Plan (Sheets als Primär-DB) wurde auf **Room SQLite als Primärspeicher** umgestellt. Sheets bleibt Export-Kanal. Alle MD-Dokumente aktualisiert ✅

---

## Offene Punkte / TODOs für Phase 2

| Punkt | Datei | Priorität |
|-------|-------|-----------|
| FloatParser.kt | util/ | Hoch – DE Komma-Dezimaltrenner |
| Google Credential Manager (echter Flow) | AuthRepository | Hoch |
| ZutatenRepository | data/repository/ | Hoch |
| TagebuchRepository | data/repository/ | Hoch |
| NRC Welpen/Senioren Bedarfswerte | NaehrstoffDomain | Mittel |
| IE-Konvertierung UI | NaehrstoffDialog | Mittel |
| LocaleHelper.kt | util/ | Niedrig |
| Kochverlust-Faktor in RezeptAnalyseUseCase | NaehrstoffDomain | Hoch (Phase 3) |
| resolveRezept() rekursiv | domain/ | Hoch (Phase 3) |

---

## NRC-Nährstoff-Abdeckung

Implementiert in `NaehrstoffKatalog` (29 Nährstoffe):

| Gruppe | Implementiert | Vollständig NRC 2006 |
|--------|--------------|---------------------|
| Makronährstoffe | Protein, Fett, LA, ALA, EPA+DHA | 5/5 Kern |
| Mineralstoffe | Ca, P, K, Na, Cl, Mg, Fe, Zn, Cu, Mn, Se, J | 12/12 |
| Vitamine | A, D, E, K, B1–B6, B9, B12, Biotin, Cholin | 12/12 |
| Aminosäuren | — | 0/10 geplant |
| Weitere Fettsäuren | — | 0/2 geplant |

Ausbau auf 39 Nährstoffe in Phase 2/3.
