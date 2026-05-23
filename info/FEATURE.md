# AllerNutri – Feature-Übersicht (v0.11.0012)

> Stand: 2026-05-22 · ✅ fertig · 🟡 teilweise · 🔲 geplant

---

## Navigation & App-Struktur

| Feature | Status |
|---------|--------|
| 6 Bottom-Nav-Tabs (Tagebuch, Rechner, Hunde, Statistik, Aufgaben, Export) | ✅ |
| Material You Dynamic Color + Dark Mode | ✅ |
| Sprachschalter DE/EN ohne Neustart (Android 13+ LocaleManager) | ✅ |
| Vollständige String-Extraktion (228 Strings, DE + EN) | ✅ |
| Erweiterbar für weitere Sprachen (nur values-XX/ + 1 Zeile) | ✅ |
| Adaptive App-Icon (alle 5 Dichten + anydpi-v26) | ✅ |
| Offline-First — alle Kernfunktionen ohne Internet | ✅ |

---

## Authentifizierung

| Feature | Status |
|---------|--------|
| Google Sign-In via Credential Manager | ✅ |
| Demo-Modus (kein Google-Account nötig) | ✅ |
| Session via DataStore (bleibt nach Neustart) | ✅ |
| Abmelden in Einstellungen | ✅ |

---

## Hundverwaltung (Tab: Hunde)

| Feature | Status |
|---------|--------|
| Hund anlegen/bearbeiten (Name, Rasse, Gewicht, Geschlecht, Kastration) | ✅ |
| Soft-Delete + Undo (8 Sek.) | ✅ |
| Gewichtsverlauf (letzte 15 Einträge) | ✅ |
| Manueller Kcal-Bedarf (überschreibt RER) | ✅ |
| Mehrere Hunde | ✅ |
| Zutaten-Button in TopBar → ZutatenScreen | ✅ |

---

## Zutatendatenbank (Tab: Hunde → Zutaten)

| Feature | Status |
|---------|--------|
| Zutat anlegen/bearbeiten (Name, Hersteller, Kategorie, Typ) | ✅ |
| Eingabemodi: pro 100 g · Tabletten · Tropfen · Pulver | ✅ |
| Tablettengewicht je Stück (g) | ✅ |
| Tropfengewicht (g) + Tropfenvolumen (ml) | ✅ |
| 30 NRC-Nährstoffe (inkl. kcal) — gruppiert, einklappbar | ✅ |
| IE-Eingabe für Vit. A, D, E — Konvertierung beim Speichern | ✅ |
| Vitamin-E-Formauswahl (natürlich/synthetisch/Acetat) | ✅ |
| Suche + Filter | ✅ |
| Soft-Delete + Undo-Banner | ✅ |
| USDA / Open Food Facts Import | ✅ (+ Edamam) |
| Zutat-zu-Zutat-Vergleich (⚖️) | ✅ |

---

## Futterrechner (Tab: Rechner)

### Rezeptverwaltung
| Feature | Status |
|---------|--------|
| Rezept anlegen/bearbeiten je Hund | ✅ |
| Zutaten mit Gramm hinzufügen | ✅ |
| Tabletten: Stückzahl → Gramm automatisch | ✅ |
| Tropfen: Anzahl → Gramm automatisch | ✅ |
| Gramm-Vorschau im Picker | ✅ |
| Freitext-Inhaltsstoffe je Komponente | ✅ Entity |
| Sub-Rezept (Rezept-Mix), max. 5 Ebenen | ✅ |
| Zykluserkennung bei verschachtelten Rezepten | ✅ |
| Skalierungsfaktor ×0.25/0.5/1/2 + Slider | ✅ |
| Portionen pro Tag | ✅ Entity |
| Soft-Delete Rezepte | ✅ |

### NRC-Analyse
| Feature | Status |
|---------|--------|
| RER = 70 × kg^0.75 | ✅ |
| MER = RER × Aktivitätsfaktor (1.0–3.0) | ✅ |
| 30 NRC-Nährstoffe — Ist/Soll/Ampel | ✅ |
| Nährstoffbalken (Canvas, Ampelfarben, 80%/100%-Marker) | ✅ |
| Kochverlust: Faktor 0.70 — nur B-Vitamine — konfigurierbar | ✅ |
| Ca:P-Verhältnis als Badge (Ziel: 1.2–1.5:1) | ✅ |
| Omega 6:3-Verhältnis als Badge | ✅ |
| Rezept-Vergleich A vs. B (Delta-Balken) | ✅ |
| NRC Lebensphasen (Welpe, Senior, Trächtig, Laktierend) | ✅ |
| Toleranzbalken (Min/Max/Empfehlung) | ✅ vollständig |

### Futterumstellungsrechner
| Feature | Status |
|---------|--------|
| Rezept A → Rezept B wählen (Dropdown) | ✅ |
| Geschwindigkeit: Schnell (5T) / Normal (7T) / Sanft (10T) / Sehr sanft (14T) | ✅ |
| Eigene Tageanzahl (5–14) | ✅ |
| Linearer Tagesplan mit Gramm-Angaben | ✅ |
| Farbbalken Blau→Grün je Tag | ✅ |
| Tipps-Karte | ✅ |
| Einklappbar im Rechner-Tab | ✅ |

---

## Tagebuch (Tab: Tagebuch — 9 Tabs)

### Allgemein
| Feature | Status |
|---------|--------|
| Soft-Delete + Undo-Banner (8 Sek., max. 5) | ✅ |
| Hund-Auswahl bei mehreren Hunden | ✅ |
| FAB je Tab (außer Zustand-Tab) | ✅ |

### Tab: Zustand (Smiley)
| Feature | Status |
|---------|--------|
| 5 Stufen: 😄🙂😐😟😢 | ✅ |
| Optionale Notiz | ✅ |
| Tagesweise gespeichert (unique index) | ✅ |
| Verlauf-Liste | ✅ |
| Automatisch beim Hund-Wechsel geladen | ✅ |

### Tab: Umwelt
| Feature | Status |
|---------|--------|
| Temp min/max, Feuchte, Niederschlag | ✅ |
| Raumtemp + Raumfeuchte | ✅ |
| Bett: unverändert/gewechselt | ✅ |
| Pollen: Slider 0–5 je Art | ✅ |
| Eigene Pollenarten hinzufügen | ✅ |

### Tab: Symptom
| Feature | Status |
|---------|--------|
| 8 Kategorien + Freitext | ✅ |
| Schweregrad 0–5 (Slider) | ✅ |
| Körperstelle + Freitext | ✅ |

### Tab: Futter
| Feature | Status |
|---------|--------|
| Protokoll-Flags: Erstgabe, 2-Wochen, Provokation, Reaktion | ✅ |
| Freitext-Ergänzung | ✅ |
| Rezept-Positionen mit Gramm | ✅ Phase 3 vollständig |

### Tabs: Ausschluss · Allergen · Tierarzt · Medikament · Phasen
| Feature | Status |
|---------|--------|
| Alle Felder gemäß Spec | ✅ |
| Phasen-Fortschrittsbalken (aktive Phase) | ✅ |
| Phasen-Standarddauern: 42/14/7 Tage (vorgeschlagen) | ✅ |

---

## Wetter & Pollen

| Feature | Status |
|---------|--------|
| BrightSky (DWD): Temp, Feuchte, Niederschlag | ✅ Repository |
| Open-Meteo: 6 Pollenarten, Tagesmittel | ✅ Repository |
| Pollen → 0–5 Skala Konverter | ✅ |
| Standort via DataStore | ✅ |
| UI-Integration (Auto-Befüllung Umwelt-Tab) | ✅ |

---

## Statistik (Tab: Statistik)

| Feature | Status |
|---------|--------|
| Hund-Filter | ✅ |
| Zeitraum: 30/90/180 Tage/1J/Alles | ✅ |
| KPI-Kacheln: Symptomtage, Ø Schweregrad, Pollentage, Allergene | ✅ |
| Symptom-Verlaufsbalken | ✅ |
| Wochentag-Heatmap (ab 14 Einträgen) | ✅ |
| Pollen-Korrelation (ab 3 Datenpunkten) | ✅ |
| Phasen-Timeline | ✅ |
| Gewichtsverlauf-Chart (Vico) | ✅ |
| Reaktionsscore (48h-Fenster, min. 3) | ✅ |
| Hund-Vergleich (zweites Dropdown) | ✅ |
| Kreuzallergie-Analyse: statische Protein-Matrix + Risikogruppen-Markierung | ✅ |
| Kreuzallergie-Hinweise im Tagebuch (Kandidaten aus bestätigten Allergenen) | ✅ |

---

## Aufgaben / Task-System (Tab: Aufgaben)

| Feature | Status |
|---------|--------|
| Task anlegen (Titel, Beschreibung, Kategorie) | ✅ |
| Kategorien: 💊 Medikament · 🛁 Pflege · 🏥 Tierarzt · 📋 Sonstiges | ✅ |
| Wiederholung: täglich / wöchentlich / Intervall (N Tage) / einmalig | ✅ |
| Wochentage wählbar (Mo–So) | ✅ |
| Abhaken per Checkbox | ✅ |
| Tages-Fortschrittsbalken | ✅ |
| Erledigungs-Protokoll je Task | ✅ |
| Hund-Zuordnung | ✅ |
| Push-Notification (Intervall-basiert) | ✅ |
| Notification Permission (Android 13+) | ✅ |
| WorkManager täglicher Check um Mitternacht | ✅ |

---

## Export (Tab: Export)

| Feature | Status |
|---------|--------|
| PDF-Bericht (8 Sektionen, A4, Share-Intent) | ✅ |
| Sektions-Toggles (Checkboxen) | ✅ |
| Zeitraum wählbar | ✅ |
| CSV-Export (Symptome) | ✅ |
| SQLite-Backup (.db Datei) | ✅ |
| FileProvider für Share-Intent | ✅ |
| Google Sheets Export | ✅ |

---

## Einstellungen

| Feature | Status |
|---------|--------|
| Abmelden | ✅ |
| Sprachschalter DE/EN | ✅ |
| Standort (Lat/Lon) für Wetter-APIs | ✅ |
| Vitaminanzeige: metrisch (µg/mg) oder IE | ✅ |
| Datenbank-Backup erstellen | ✅ |
| Backup wiederherstellen | ✅ |

---

## Technisch

| Feature | Status |
|---------|--------|
| Room SQLite v3 · 22 Entities | ✅ |
| Soft-Delete überall (`deleted` + `deletedAt`) | ✅ |
| FloatParser (DE Komma + EN Punkt) | ✅ |
| HiltWorkerFactory in Application-Klasse | ✅ |
| ProGuard/R8 konfiguriert (Paketname allernutri korrekt) | ✅ |
| GitHub Actions CI (Debug + Release APK) | ✅ |
| Unit-Tests: RER, MER, Analyse, FloatParser | ✅ |
| **Android 16 (API 36) target** | ✅ |
| AGP 9.1.1 Upgrade | ✅ |
| HTTP-Logging nur im Debug-Build | ✅ |
| WEB_CLIENT_ID aus BuildConfig (local.properties) | ✅ |
| Edge-to-Edge (`enableEdgeToEdge()`) | ✅ |
| Predictive Back Gesture (`enableOnBackInvokedCallback=true`) | ✅ |
| NEARBY_WIFI_DEVICES (LNP-Vorbereitung) | ✅ deklariert |
| **Android 17 (API 37) target** | ✅ |
| Adaptive Navigation (NavigationRail auf Medium/Expanded Screens) | ✅ |
| LoginScreen `widthIn(max=480dp)` auf Tablets | ✅ |
| State-Preservation bei Konfigurations-Änderungen | ✅ ViewModel vorhanden |
| `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` nicht verwendet | ✅ nie nötig |
| Adaptive Layouts Audit (restliche Screens) | 🔲 folgt in v0.12 |
