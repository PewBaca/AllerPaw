# AllerPaw – Feature-Übersicht (v0.2)

> Letzte Aktualisierung: 2026-04-24  
> Implementierungsstatus in der rechten Spalte: ✅ fertig · 🟡 teilweise · 🔲 geplant

---

## Datenbankarchitektur

| Merkmal | Beschreibung | Status |
|---------|-------------|--------|
| Primärspeicher | SQLite lokal auf dem Android-Gerät (Room) | ✅ |
| Datensouveränität | 100 % der Daten beim Nutzer – kein Cloud-Zwang | ✅ |
| Google Sheets | Nur noch als optionaler Export/Backup-Kanal | 🔲 Phase 4 |
| CSV-Export | Jede DB-Tabelle als CSV, optional als ZIP | 🔲 Phase 4 |
| SQLite-Backup | Rohe DB-Datei in lokalen Speicher oder Google Drive | 🔲 Phase 4 |
| Google Drive Sync | Optionaler automatischer Backup | 🔲 Phase 4 |
| Offline-Betrieb | Alle Kernfunktionen ohne Internet | ✅ |
| Google-Login | Nur für Drive/Sheets-Export; Kern ohne Login nutzbar | 🟡 Platzhalter |

---

## Mehrsprachigkeit (i18n)

| Feature | Beschreibung | Status |
|---------|-------------|--------|
| Sprachen | Deutsch (Standard), Englisch | 🟡 Grundstruktur |
| Sprachquelle | strings.xml (DE / EN) | ✅ |
| Sprachschalter | In den Einstellungen; sofort ohne Neustart | 🔲 Phase 5 |
| Nährstoffnamen | Alle 29+ NRC-Nährstoffe in DE und EN | 🟡 |
| Erststart-Wizard | Sprachauswahl beim ersten Start | 🔲 Phase 5 |
| Hinweise mehrsprachig | Pro Nährstoff / Futtermittel / Statistik | 🔲 Phase 5 |

---

## Zutatendatenbank & Einheitensystem

### Mengeneinheiten

| Typ | Eingabebasis | Status |
|-----|-------------|--------|
| Lebensmittel | pro 100 g Frischgewicht | 🔲 Phase 2 |
| Supplement / Tablette | pro Einheit; Gewicht je Tablette hinterlegen | 🔲 Phase 2 |
| Supplement / Tropfen | pro Einheit (1 Tropfen) | 🔲 Phase 2 |
| Supplement / Pulver | pro 100 g | 🔲 Phase 2 |

Alle Nährstoffe werden intern als Äquivalent pro 100 g gespeichert. Entity `ZutatNaehrstoffEntity` mit `wertPer100g` bereits implementiert. ✅

### IE-Konvertierung

| Nährstoff | Konversion | Status |
|-----------|-----------| -------|
| Vitamin A (Retinol) | 1 IE = 0,3 µg | 🔲 Phase 2 |
| Vitamin D3 | 1 IE = 0,025 µg | 🔲 Phase 2 |
| Vitamin E natürlich (d-Alpha-Tocopherol) | 1 IE = 0,67 mg | 🔲 Phase 2 |
| Vitamin E synthetisch (dl-Alpha-Tocopherol) | 1 IE = 0,45 mg | 🔲 Phase 2 |
| Vitamin E Acetat natürlich | 1 IE = 0,74 mg | 🔲 Phase 2 |
| Vitamin E Acetat synthetisch | 1 IE = 0,67 mg | 🔲 Phase 2 |

Vitamin-E-Form wird je Zutat in `ZutatEntity.vitaminEForm` gespeichert. ✅

---

## Hundverwaltung (Stammdaten → Hunde)

| Feature | Beschreibung | Status |
|---------|-------------|--------|
| Hund anlegen / bearbeiten | Name, Rasse, Geburtsdatum, Geschlecht, Kastration, Notizen | ✅ |
| Soft-Aktivierung | Hund deaktivieren statt löschen | ✅ |
| Kcal-Bedarf manuell | Überschreibt RER (Feld `kcalBedarfManuell` in Entity) | ✅ Entity, 🔲 UI |
| Gewichtserfassung | Dialog + Verlaufstabelle (letzte 15) | ✅ Entity+DAO, 🔲 UI |
| Gewichtsverlauf-Chart | In Statistik | 🔲 Phase 3 |
| Mehrere Hunde | Beliebig viele; alle Auswertungen pro Hund filterbar | ✅ |

---

## Zutaten & Supplements

| Feature | Beschreibung | Status |
|---------|-------------|--------|
| Zutat anlegen / bearbeiten | Name, Hersteller, Kategorie, Typ, Status | 🔲 Phase 2 |
| 29 NRC-Nährstoffe | Im NaehrstoffKatalog implementiert | ✅ |
| NaehrstoffDialog | Einklappbar, gruppiert (Makro / Mineral / Vitamin / Fettsäure) | 🔲 Phase 2 |
| Vitamin-E-Form | Auswahl natürlich / synthetisch / Acetat | ✅ Entity, 🔲 UI |
| Soft-Delete + Undo | Banner 8 Sek., max. 5 Stack | ✅ DAO, 🔲 UI |
| USDA + Open Food Facts Import | Parallel, nur leere Felder befüllen | 🔲 Phase 2 |
| Zutat-zu-Zutat-Vergleich | ⚖️-Button → alle Nährstoffe nebeneinander, Delta-Pfeil | 🔲 Phase 3 |

---

## Futterrechner

| Feature | Beschreibung | Status |
|---------|-------------|--------|
| Rezepte anlegen / bearbeiten | Je Hund | ✅ Entity+DAO, 🔲 UI |
| Lebensmittel: Gramm | Standardeingabe | 🔲 Phase 3 |
| Tablette: Stückzahl | Dezimalzahl (z.B. 0,5); Grammäquivalent wird angezeigt | 🔲 Phase 3 |
| Skalierungsfaktor | ×0,25 / ×0,5 / ×1 / ×2 + freies Eingabefeld | 🔲 Phase 3 |
| Portionen pro Tag | Tagesration / Portionswert = g je Portion | 🔲 Phase 3 |
| Nährstoffanalyse | 29 NRC-Nährstoffe, Ampelfarben (OK / MANGEL / UEBERSCHUSS / UEBERSCHRITTEN) | ✅ Domain, 🔲 Balken-UI |
| RER/MER-Anzeige | RER = 70 × kg^0.75; MER = RER × Aktivitätsfaktor | ✅ |
| Aktivitätsfaktor-Slider | 1.0–3.0, Schritte 0.1 | ✅ |
| Toleranzbalken | Min / Max / Empfehlung je Nährstoff; Entity vorhanden | ✅ Entity, 🔲 UI |
| Rezept-Mix | Max. 5 Ebenen, Zykluserkennung, ohne Zwischenrundung | 🔲 Phase 3 |
| Kcal-Berechnung | Aus Gramm + Zutaten-Nährwerten | 🔲 Phase 3 |
| Gekocht-Flag | Kochverlust Faktor **0.70** (= 30 % Verlust); konfigurierbar; nur B-Vitamine | 🔲 Phase 3 |
| Ca:P-Verhältnis + Omega 6:3 | Automatisch als Badge; Ca:P Ziel 1,2–1,5:1 | 🔲 Phase 3 |
| Rezept-Vergleich A vs. B | Delta-Spalte in % des Tagesbedarfs | 🔲 Phase 3 |
| Soft-Delete Rezepte + Undo | | ✅ DAO, 🔲 UI |

> **Wichtig:** Kochverlustfaktor ist 0.30 (= 30% Verlust → angewendeter Faktor 0.70), nicht 0.75. Konfigurierbar via `ParameterEntity`. Gilt nur für B1, B2, B3, B5, B6, B9, B12.

---

## Tagebuch

Alle Tabs: Soft-Delete + Undo-Banner (8 Sek.) + Edit-Dialog. Entities und DAOs vollständig implementiert ✅. UI folgt in Phase 2.

### Eingabe-Tabs (alle 8 vorhanden als Entity/DAO ✅)

| Tab | Felder | UI-Status |
|-----|--------|-----------|
| Umwelt | Datum, Temp min/max, Luftfeuchte, Niederschlag, Pollen (Typ+Stärke), Raumtemp, Raumfeuchte, Bett, Notizen | 🔲 |
| Symptom | Datum, Kategorie (8+Freitext), Beschreibung, Schweregrad 0–5, Körperstelle (+Freitext), Notizen | 🔲 |
| Futter | Datum, mehrere Rezept-Positionen+Gramm, Freitext, Protokollfelder, Notizen | 🔲 |
| Ausschluss | Zutat, Verdachtsstufe 0–3, Kategorie, Status, Erstgabe-Datum, Reaktion, Notizen | 🔲 |
| Allergen | Allergen, Kategorie, Reaktionsstärke 1–5, Symptome, Notizen | 🔲 |
| Tierarzt | Datum, Praxis, Anlass, Untersuchungen, Ergebnis, Empfehlung, Folgebesuch | 🔲 |
| Medikament | Name, Typ, Dosierung, Häufigkeit, Von–Bis, Verordnet von, Notizen | 🔲 |
| Phasen | Phasentyp, Zutat (bei Provokation), Start, Ende, Ergebnis, Notizen | 🔲 |

### Ausschlussdiät-Phasentracker

| Feature | Standard | Status |
|---------|---------|--------|
| Elimination | 42 Tage | ✅ Entity |
| Provokation | 14 Tage | ✅ Entity |
| Ergebnis | 7 Tage | ✅ Entity |
| Fortschritts-Banner | Tage verbraucht / gesamt / verbleibend | 🔲 Phase 2 |

---

## Wetter & Pollen

| Feature | Beschreibung | Status |
|---------|-------------|--------|
| BrightSky API | Außentemp min/max, Luftfeuchte, Niederschlag | 🔲 Phase 4 |
| Pollen DWD | 8 Pollenarten, 18 Regionen | 🔲 Phase 4 |
| Pollen Open-Meteo | Koordinatenbasiert, kein API-Key | 🔲 Phase 4 |
| Eigene Pollenarten | Room-Tabelle `EigenePollenartEntity` | ✅ Entity+DAO |
| Pollen-Log | Jede Pollenart eigene DB-Zeile | ✅ Entity+DAO |
| Skala 0–5 | 0=keine · 1=gering · … · 5=stark | ✅ Entity |

---

## Statistik

| Feature | Beschreibung | Status |
|---------|-------------|--------|
| Hund-Filter | Dropdown | 🔲 Phase 3 |
| Zeitraum-Filter | 30/90/180 Tage/1 Jahr/Alles | 🔲 Phase 3 |
| KPI-Kacheln | Symptomtage, Ø Schweregrad, Pollentage | 🔲 Phase 3 |
| Chart (Vico) | Temperaturband, Symptom-Fläche, Pollen, Gewicht | 🔲 Phase 3 |
| Symptom-Heatmap | Wochentag + Monat; ab 14 Einträgen | 🔲 Phase 3 |
| Korrelationsanalyse | min. 3 Datenpunkte; Gruppen Ø > 2.0 orange | 🔲 Phase 3 |
| Reaktionsscore | 48-h-Fenster; min. 3 Beobachtungen | 🔲 Phase 3 |
| Hund-Vergleich | Zweites Hund-Dropdown; blaues Band | 🔲 Phase 3 |
| Pollen-Popup-Dialog | Bottom-Sheet, alle Pollenarten | 🔲 Phase 3 |
| Phasen-Timeline | Alle Phasen chronologisch | 🔲 Phase 3 |

---

## Tierarzt-Export (PDF)

| Feature | Beschreibung | Status |
|---------|-------------|--------|
| Zeitraum wählbar | Von/Bis + Schnellauswahl | 🔲 Phase 4 |
| 9 Sektionen | Deckblatt, Symptome, Allergene, Ausschlussdiät, Phasen-Timeline, Medikamente, Futter, Reaktionsscore, Korrelation | 🔲 Phase 4 |
| Standard-Auswahl | Reaktionsscore + Korrelation deaktiviert; Rest aktiv | 🔲 Phase 4 |
| PDF via Android | PrintManager / PdfDocument; Share-Intent | 🔲 Phase 4 |

---

## Technische Features

| Feature | Beschreibung | Status |
|---------|-------------|--------|
| Room SQLite | 19 Entities, Version 1, TypeConverters | ✅ |
| Soft-Delete | deleted / deleted_at überall; max. 5 Undo-Stack | ✅ DAO, 🔲 UI |
| FloatParser | Komma (DE) + Punkt (EN) als Dezimaltrenner | 🔲 Phase 2 |
| Material You | Dynamic Color, Dark Mode folgt System | ✅ |
| Offline-First | Alle Kernfunktionen ohne Internet | ✅ |
| IE-Konvertierung | 4 Vitamin-E-Formen; intern µg/mg | ✅ Entity, 🔲 UI |
| Monetarisierung | Optionale Werbung bei Login + Spendenoption | 🔲 Phase 5 |
| Geteilte Datenbanken | Zutaten-/Rezept-DBs importieren/exportieren/kaufen | 🔲 Phase 5 |
| NRC Welpen/Senioren | Separate NRC-Bedarfswerte | 🔲 Phase 5 |

---

## Einstellungen

| Feature | Beschreibung | Status |
|---------|-------------|--------|
| Google OAuth2 | Login/Logout (nur für Drive/Sheets-Export) | 🔲 |
| Standort | Breitengrad / Längengrad für Wetter-API | 🔲 |
| DWD-Region | Pollen-Region (18 Regionen) | 🔲 |
| USDA API-Key | Für Nährstoff-Import | 🔲 |
| IE-Anzeige | Vitamine A, D, E in IE oder µg/mg | 🔲 |
| Sprache | Deutsch / Englisch | 🔲 |
| Dark Mode | Folgt Systemeinstellung | ✅ |
| Kochverlust-Faktor | Standard 0.30; konfigurierbar | ✅ Entity, 🔲 UI |
| Portionen/Tag | Standard 2; konfigurierbar | ✅ Entity, 🔲 UI |
