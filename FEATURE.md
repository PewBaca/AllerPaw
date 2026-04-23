# AllerPaw – Feature-Übersicht (v0.1)

> Letzte Aktualisierung: 2026-04-23  
> Status: Android-Migration – neue Datenbankarchitektur, lokale Datenspeicherung, IE-Konvertierung

---

## 🗄️ Datenbankarchitektur (Neu in v0.1)

AllerPaw verwendet eine **lokale SQLite-Datenbank** (via Room) als primäre Datenhaltung. Google Sheets ist nicht mehr die Datenbank, sondern ein optionaler Export-Kanal.

| Merkmal | Beschreibung |
|---------|-------------|
| Primärspeicher | SQLite lokal auf dem Android-Gerät (Room) |
| Datensouveränität | 100 % der Daten liegen beim Nutzer – kein Cloud-Zwang, kein Backend |
| Kein Fallback auf alte Datenstruktur | Neue Datenbankstruktur bricht bewusst mit dem alten Google-Sheets-Format |
| Backup / Export | Daten können als **CSV** oder **Google Sheets Spreadsheet** exportiert werden |
| Google Drive Sync | Optionaler automatischer Backup der SQLite-DB in den eigenen Google Drive |
| Offline-Betrieb | App funktioniert vollständig ohne Internetverbindung |

### Exportformate

| Format | Beschreibung |
|--------|-------------|
| CSV | Jede Tabelle als eigene CSV-Datei; wahlweise als ZIP-Archiv |
| Google Sheets | Export in ein neues oder vorhandenes Spreadsheet via Sheets API |
| SQLite-Backup | Rohe DB-Datei in Google Drive oder lokalen Speicher exportieren |

### Datenschutz & Kontrolle

- Kein Nutzerkonto bei AllerPaw erforderlich
- Google-Login nur für optionalen Drive-Backup / Sheets-Export
- Alle Daten werden ausschließlich lokal gespeichert; keine Telemetrie, keine Analytics
- Nutzer kann jederzeit alle Daten exportieren oder löschen

---

## 🌍 Mehrsprachigkeit (i18n)

| Feature | Beschreibung |
|---------|-------------|
| Sprachen | Deutsch (Standard), Englisch; weitere Sprachen erweiterbar |
| Sprachquelle | Android `strings.xml` (DE / EN) + datenbankbasierte Übersetzungen für Nährstoffnamen |
| Sprachschalter | In den Einstellungen; sofort wirksam ohne App-Neustart |
| Nährstoffnamen übersetzt | Alle 39 NRC-Nährstoffe in DE und EN verfügbar |
| Einheitenformatierung | Dezimaltrenner und Zahlenformat folgen Gerätesprache (DE: Komma, EN: Punkt) |

---

## 📦 Zutatendatenbank & Einheitensystem

### Mengeneinheiten je Zutat / Supplement

AllerPaw unterscheidet grundlegend zwischen **Lebensmittelzutaten** und **Supplements / Tabletten**:

| Typ | Eingabebasis | Beschreibung |
|-----|-------------|-------------|
| Lebensmittel | pro 100 g Frischgewicht | Fleisch, Gemüse, Fisch, Öle usw. |
| Supplement / Tablette | pro Einheit (Tablette / Kapsel / Softgel) | Vitaminpräparate, Mineralstofftabletten, Ölkapseln |
| Supplement / Tropfen | pro Einheit (1 Tropfen) | Flüssige Nahrungsergänzungen in Tropfenform |
| Supplement / Pulver | pro 100 g | Pulverförmige Nahrungsergänzungen |

Im Futterrechner wird die Dosierung entsprechend eingegeben:
- Lebensmittel: Gramm-Eingabe
- Tablette / Kapsel / Softgel: Stückzahl (Dezimalzahl, z. B. 0,5 / 1 / 2)
- Tropfen: Anzahl Tropfen
- Pulver: Gramm-Eingabe

### Nährstoffeinheiten

| Einheit | Verwendung |
|---------|-----------|
| g | Makronährstoffe (Protein, Fett, Kohlenhydrate, Rohfaser, Wasser) |
| mg | Mineralstoffe, einige B-Vitamine (z. B. Kalzium, Phosphor, Magnesium) |
| µg (mcg) | Spurenelemente, Vitamin B12, Folat, Vitamin K; auch interne Speichereinheit für IE-Vitamine |
| IE (IU) | Eingabe möglich für Vitamin A, D, E; wird vor Speicherung automatisch in µg konvertiert |
| kcal | Energiedichte |

### IE-Konvertierung (International Units)

IE (Internationale Einheit / IU) ist eine Einheit der **biologischen Aktivität**, nicht der Masse. Es gibt keine universelle Umrechnungsformel — der Faktor ist substanzspezifisch. AllerPaw rechnet IE-Eingaben automatisch in µg um und speichert intern immer in µg. Die Anzeige erfolgt wahlweise in IE oder µg/mg.

**Implementierte Konversionsfaktoren** (Quellen: WHO-Standard, NRC 2006, IOM / Office of Dietary Supplements):

| Nährstoff | Konversion | Hinweis |
|-----------|-----------|---------|
| Vitamin A (Retinol) | 1 IE = 0,3 µg Retinol | Standard für tierisches Vitamin A |
| Vitamin A (Beta-Carotin) | 1 IE = 0,6 µg Beta-Carotin | Carotin-Quelle; Hunde konvertieren effizienter als Katzen |
| Vitamin D3 (Cholecalciferol) | 1 IE = 0,025 µg Cholecalciferol (= 40 IE/µg) | Einzige für Hunde relevante D-Form |
| Vitamin E (d-Alpha-Tocopherol, natürlich) | 1 IE = 0,67 mg | Natürliche Form; höhere Bioverfügbarkeit |
| Vitamin E (dl-Alpha-Tocopherol, synthetisch) | 1 IE ≈ 0,45 mg | Synthetische Racemat-Mischung |
| Vitamin E (d-Alpha-Tocopheryl-Acetat) | 1 IE = 0,74 mg | Häufig in Supplements |
| Vitamin E (dl-Alpha-Tocopheryl-Acetat) | 1 IE = 0,67 mg | Synthetisches Acetat |

**NRC 2006 Referenzwerte für Hunde (adulte Erhaltung):**
- Vitamin A: Empfohlene Zufuhr 379 RE / 1.000 kcal (≈ 1.263 IE / 1.000 kcal)
- Vitamin D: Empfohlene Zufuhr 3,4 µg Cholecalciferol / 1.000 kcal (≈ 136 IE / 1.000 kcal)
- Vitamin E: Empfohlene Zufuhr 7,5 mg d-Alpha-Tocopherol / 1.000 kcal (≈ 123,5 IE / 1.000 kcal)

**Anzeige-Logik:**
- Eingabe in der vom Nutzer gewählten Einheit (IE oder µg/mg)
- Interne Berechnungen und Datenbankablage immer in µg
- Bei Vitamin E: Vitamin-E-Form (natürlich / synthetisch / Acetat) wird bei der Zutat hinterlegt
- Nährstoff-Popup zeigt IE-Wert und µg-Wert parallel an
- Standard-Anzeigeeinheit für IE-Vitamine in Einstellungen konfigurierbar

---

## 🐕 Hundverwaltung (Stammdaten → Hunde)

| Feature | Beschreibung |
|---------|-------------|
| Hund anlegen / bearbeiten | Name, Rasse, Geburtsdatum, Geschlecht, Kastration, Notizen |
| Soft-Aktivierung | Hund deaktivieren statt löschen |
| Kcal-Bedarf manuell | Überschreibt die automatische RER-Berechnung |
| Gewichtserfassung | Eigener Dialog mit Verlaufstabelle (letzte 15 Einträge) |
| Gewichtsverlauf-Chart | Kurve in der Statistik, wenn Gewichtsdaten vorhanden |
| Mehrere Hunde | Beliebig viele Hunde verwaltbar; alle Auswertungen per Hund filterbar |

---

## 🥩 Zutaten & Supplements (Stammdaten → Zutaten)

| Feature | Beschreibung |
|---------|-------------|
| Zutat anlegen / bearbeiten | Name, Hersteller, Kategorie, Typ, Status |
| Typ wählen | Lebensmittel / Supplement-Tablette / Supplement-Kapsel / Supplement-Softgel / Supplement-Tropfen / Supplement-Pulver |
| Eingabebasis | Pro 100 g (Lebensmittel + Pulver) **oder** pro Einheit (Tablette / Kapsel / Tropfen) |
| Einheit je Nährstoff wählbar | g / mg / µg / IE – IE wird automatisch in µg konvertiert |
| Vitamin-E-Form | Auswahl natürlich (d-Alpha) / synthetisch (dl-Alpha) / Acetat für korrekte IE-Umrechnung |
| 39 NRC-Nährstoffe | Alle Werte im Zutat-Dialog eingeben (einklappbar, gruppiert nach Makros / Aminosäuren / Fettsäuren / Mineralstoffe / Vitamine) |
| Gespeicherte Einheit | Immer in µg / mg / g – IE-Eingabe wird vor Speicherung konvertiert |
| Zutat soft-löschen | Mit Undo (8 Sek., bis zu 5 rückgängig) |
| Automatischer Futterrechner-Sync | Dropdown im Rechner sofort nach Speichern aktualisiert |
| Nährstoff-Import USDA + Open Food Facts parallel | Beide Quellen gleichzeitig; Zwei-Spalten-Ergebnisliste |
| Import überschreibt keine Werte | Nur leere Felder werden befüllt |

---

## 🧮 Futterrechner

| Feature | Beschreibung |
|---------|-------------|
| Rezepte erstellen & bearbeiten | Für jeden Hund separat |
| Zutatenmenge je Typ | Lebensmittel: Gramm; Tablette / Kapsel / Softgel: Stückzahl (Dezimal); Tropfen: Anzahl |
| Nährstoffanalyse | Alle 39 NRC-Nährstoffe; Ampelfarben (ok / low / high / zero) |
| IE-Anzeige | Vitamine A, D, E wahlweise in IE oder µg/mg; konfigurierbar in Einstellungen |
| Toleranzbalken | Individuelle Min / Max / Empfehlung je Nährstoff und Hund (in %) |
| Rezept-Mix | Verschachtelte Rezepte (max. 5 Ebenen, Zykluserkennung) |
| Kcal-Berechnung | Automatisch aus Gramm + Zutaten-Nährwerten |
| Gekocht-Flag | Kochverlustfaktor 0,75 für B-Vitamine (B1, B2, B3, B5, B6, B9, B12); andere Nährstoffe unverändert |
| Nährstoff-Popup | Detailinfos (NRC / AAFCO / FEDIAF); zeigt IE- und µg-Wert parallel |
| Soft-Delete Rezepte | Mit Undo-Banner |
| Rezept-Vergleich A vs. B | Delta-Spalte in % des Tagesbedarfs |
| Ca:P-Verhältnis & Omega 6:3 | Automatisch als Badge |
| Supplement-Übersicht | Separate Anzeige aller Supplements mit Stückzahl und IE-Tagesbeitrag |

---

## 📓 Tagebuch

Jeder Eintrag ist soft-löschbar mit Undo-Banner und per Edit-Dialog bearbeitbar.

### Tab-Übersicht

| Tab | Felder |
|-----|--------|
| 🌤 Umwelt | Außentemp min/max, Luftfeuchte außen, Niederschlag, Pollen (Typ + Stärke), Raumtemp, Raumfeuchte, Bett, Notizen |
| 🔍 Symptom | Kategorie, Beschreibung, Schweregrad (0–5), Körperstelle, Notizen |
| 🥩 Futter | Rezept-/Futtername, Produkt, Erstgabe, 2-Wochen-Phase, Provokation, Reaktion |
| 🚫 Ausschluss | Zutat, Verdachtsstufe 1–3, Kategorie, Status, Reaktion |
| ⚠️ Allergen | Allergen, Kategorie, Reaktionsstärke 1–5, Symptome |
| 🏥 Tierarzt | Datum, Praxis, Anlass, Untersuchungen, Ergebnis, Therapie, Folgebesuch |
| 💊 Medikament | Name, Typ, Dosierung, Häufigkeit, Von–Bis, Verordnet von |
| 📅 Phasen | Phasentyp, Zeitraum, Zutat (bei Provokation), Notizen |

### Ausschlussdiät-Phasentracker

| Feature | Beschreibung |
|---------|-------------|
| Phasentypen | Elimination (42 Tage Standard), Provokation (14 Tage), Ergebnis (7 Tage) |
| Enddatum | Per Vorschlag vorausgefüllt, manuell überschreibbar |
| Aktiver-Phasen-Banner | Fortschrittsbalken mit Tagen verbraucht / gesamt / verbleibend |
| Phasenliste | Alle Phasen mit Typ-Badge, Ergebnis-Badge, Datum, Notizen |
| Soft-Delete + Undo | Bis zu 5 Phasen rückgängig machbar |

---

## 🌿 Wetter & Pollen (Tagebuch → Umwelt-Tab)

| Feature | Beschreibung |
|---------|-------------|
| Wetter-Auto-Load | BrightSky API (DWD) – Temp, Feuchte, Niederschlag |
| Pollen DWD | 8 Pollenarten, 18 Regionen, DWD OpenData |
| Pollen Open-Meteo | Koordinatenbasiert, kein API-Key |
| Pollen-Auswahl UI | Toggle-Chips mit Stärke; Vorauswahl ab „mittel" |
| Eigene Pollenarten | Erweiterbar, lokal in SQLite gespeichert |
| Pollen-Log | Jede Pollenart als eigene DB-Zeile |
| Skala 0–5 | 0 = keine · 1 = gering · 2 = gering–mittel · 3 = mittel · 4 = mittel–stark · 5 = stark |

---

## 📊 Statistik

| Feature | Beschreibung |
|---------|-------------|
| Hund-Filter | Auswahl per Dropdown |
| Zeitraum-Filter | 30 / 90 / 180 Tage / 1 Jahr / Alles |
| Hund-Vergleich | Zweites Hund-Dropdown; Symptomverläufe beider Hunde im selben Chart |
| KPI-Kacheln | Symptomtage, Ø Schweregrad, Pollentage |
| Konfigurierbarer Chart | Temperaturband, Schweregrad-Fläche (rot), Pollen, Gewicht |
| Symptom-Muster-Heatmap | Wochentag + Monat; Ø Schweregrad; ab 14 Einträgen |
| Korrelationsanalyse | Pollen / Temp / Feuchte vs. Ø Schweregrad; min. 3 Datenpunkte |
| Zutaten-Reaktionsscore | 48-h-Fenster nach Futtereintrag; min. 3 Beobachtungen; Ampelfarben |
| Supplement-Verlauf | IE-Zufuhr für Vitamin A, D, E über Zeitraum |
| Phasen-Timeline | Chronologische Übersicht aller Ausschlussdiät-Phasen |
| Futter-Reaktionen | Liste nur mit Reaktion oder Provokation |
| Medikamente | Liste mit Zeitraum |

---

## 📄 Tierarzt-Export (PDF)

| Feature | Beschreibung |
|---------|-------------|
| Zeitraum wählbar | Von/Bis-Datum + Schnellauswahl (30 / 60 / 90 / 180 Tage) |
| 9 wählbare Sektionen | Deckblatt, Symptome, Allergene, Ausschlussdiät, Phasen, Medikamente, Futter, Reaktionsscore, Korrelationsanalyse |
| PDF via Android | `PrintManager` / `PdfDocument`; Share-Intent |
| Druckoptimiert | Schwarz-Weiß; Disclaimer + Exportdatum |

---

## 💾 Datenexport & Backup

| Feature | Beschreibung |
|---------|-------------|
| CSV-Export | Jede DB-Tabelle als eigene CSV; wahlweise als ZIP |
| Google Sheets Export | Alle Tabellen in ein Spreadsheet via Sheets API; Nutzer wählt Ziel |
| SQLite-Backup | Rohe DB-Datei in lokalen Speicher oder Google Drive exportieren |
| Google Drive Sync | Optionaler automatischer täglicher Backup (aktivierbar in Einstellungen) |
| Import aus Backup | SQLite-Backup wiederherstellen |
| Offene Formate | CSV und SQLite sind dokumentierte Offene Standards; keine Vendor-Lock-in |

---

## ⚙️ Einstellungen

| Feature | Beschreibung |
|---------|-------------|
| Datenspeicherort | Lokal (Standard); Google Drive Backup optional aktivierbar |
| Google-Login | Nur für Drive-Backup / Sheets-Export; Kernfunktion ohne Login nutzbar |
| Standort | Breitengrad / Längengrad für Wetter-API |
| DWD-Region | Pollen-Region aus 18 deutschen Regionen |
| USDA API-Key | Für Nährstoff-Import aus USDA FoodData Central |
| IE-Anzeige | Standard-Anzeigeeinheit für Vitamine A, D, E (IE oder µg/mg) |
| Sprache | Deutsch / Englisch |
| Dark Mode | Folgt Android-Systemeinstellung |
| Verbindungstest | Prüft optionalen Google Drive / Sheets API Zugang |

---

## 🏗️ Technische Features

| Feature | Beschreibung |
|---------|-------------|
| Lokale SQLite-DB | Room (Android); keine Abhängigkeit von Drittdiensten |
| Kein Google Sheets als DB | Sheets nur noch als Export-Ziel; nicht mehr als Primärspeicher |
| Kein Backend | 100 % client-seitig |
| IE-Konvertierung | Feste WHO / NRC 2006 Faktoren je Nährstoff und Vitamin-E-Form; Ablage intern in µg |
| Supplement-Stückzahl | Dezimalzahl (z. B. 0,5 Tabletten); Nährstoffe linear skaliert |
| Soft-Delete überall | `deleted` / `deleted_at` statt hartem Löschen |
| FloatParser | Komma (DE) und Punkt (EN) korrekt verarbeitet |
| Rezept-Präzision | Ohne Zwischenrundung bis zur Anzeige; max. 5 Ebenen Rezept-Mix |
| Mehrsprachigkeit | DE / EN via `strings.xml`; Nährstoffnamen in DB mehrsprachig |
| Dark Mode | Material You; folgt Android-Systemeinstellung |
| Offline-First | Alle Kernfunktionen ohne Internet; Sync nur bei Backup / Export |
