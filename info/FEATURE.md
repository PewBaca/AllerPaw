# AllerPaw – Feature-Übersicht (v0.1)

> Letzte Aktualisierung: 2026-04-23  
> Basis: Web-App v2.3.1 (vollständig aus Code analysiert) + Android-Erweiterungen

---

## 🗄️ Datenbankarchitektur (Android-Neu)

| Merkmal | Beschreibung |
|---------|-------------|
| Primärspeicher | SQLite lokal auf dem Android-Gerät (Room) |
| Datensouveränität | 100 % der Daten beim Nutzer – kein Cloud-Zwang, kein Backend |
| Kein Fallback auf alte Struktur | Neue DB bricht bewusst mit dem alten Google-Sheets-Format |
| Backup / Export | CSV (je Tabelle als eigene Datei, wahlweise ZIP) oder Google Sheets |
| SQLite-Backup | Rohe DB-Datei in lokalen Speicher oder Google Drive exportierbar |
| Google Drive Sync | Optionaler automatischer Backup (aktivierbar in Einstellungen) |
| Offline-Betrieb | Alle Kernfunktionen ohne Internetverbindung |
| Google-Login | Nur für Drive-Backup / Sheets-Export; Kernfunktion ohne Login nutzbar |
| Keine Telemetrie | Keine Analytics, kein Server, keine externen Datenflüsse außer API-Calls |

---

## 🌍 Mehrsprachigkeit (i18n)

| Feature | Beschreibung |
|---------|-------------|
| Sprachen | Deutsch (Standard), Englisch; weitere Sprachen erweiterbar |
| Sprachquelle | `strings.xml` (DE / EN) + datenbankbasierte Übersetzungen für Nährstoffnamen |
| Sprachschalter | In den Einstellungen; sofort wirksam ohne App-Neustart |
| Nährstoffnamen übersetzt | Alle 39 NRC-Nährstoffe in DE und EN |
| Einheitenformat | Dezimaltrenner folgt Gerätesprache (DE: Komma, EN: Punkt) |

---

## 📦 Zutatendatenbank & Einheitensystem

### Mengeneinheiten je Zutat / Supplement

| Typ | Eingabebasis | Beschreibung |
|-----|-------------|-------------|
| Lebensmittel | pro 100 g Frischgewicht | Standard für Fleisch, Gemüse, Fisch, Öle |
| Supplement / Tablette | pro Einheit (Tablette / Kapsel / Softgel) | Gewicht pro Tablette in g hinterlegen |
| Supplement / Tropfen | pro Einheit (1 Tropfen) | Flüssige Nahrungsergänzungen |
| Supplement / Pulver | pro 100 g | Pulverförmige Nahrungsergänzungen |

Weitere Bezugsmengen (Web-App): 1 g, 1 kg, 1000 g — werden ebenfalls auf /100g normiert gespeichert.

**Interne Speicherung:** Alle Nährstoffe werden immer als Äquivalent pro 100 g gespeichert. Nährstoffwerte „pro Tablette" werden beim Speichern automatisch auf /100g umgerechnet (Basis: hinterlegtes Tablettengewicht in g).

### Nährstoffeinheiten

| Einheit | Verwendung |
|---------|-----------|
| g | Makronährstoffe (Protein, Fett, Kohlenhydrate, Rohfaser, Wasser) |
| mg | Mineralstoffe, einige B-Vitamine |
| µg (mcg) | Spurenelemente, Vitamin B12, Folat, Vitamin K |
| IE (IU) | Eingabe möglich für Vitamin A, D, E; interne Speicherung in µg/mg |
| kcal | Energiedichte |

### IE-Konvertierung (International Units)

IE misst biologische Aktivität, nicht Masse. Faktoren sind substanzspezifisch (WHO / NRC 2006):

| Nährstoff | Konversion | Hinweis |
|-----------|-----------|---------|
| Vitamin A (Retinol) | 1 IE = 0,3 µg Retinol | |
| Vitamin D3 / D2 / D | 1 IE = 0,025 µg Cholecalciferol (= 40 IE/µg) | |
| Vitamin E (d-Alpha-Tocopherol, natürlich) | 1 IE = 0,67 mg | **Web-App-Ist: nur dieser Faktor** |
| Vitamin E (dl-Alpha-Tocopherol, synthetisch) | 1 IE ≈ 0,45 mg | **Neu in Android** |
| Vitamin E (d-Alpha-Tocopheryl-Acetat) | 1 IE = 0,74 mg | **Neu in Android** |
| Vitamin E (dl-Alpha-Tocopheryl-Acetat) | 1 IE = 0,67 mg | **Neu in Android** |

**Web-App-Ist:** Vitamin E hat nur einen Faktor (0,67 mg/IE). Keine Form-Auswahl.  
**Android-Soll:** Vitamin-E-Form (natürlich / synthetisch / Acetat) wird je Zutat hinterlegt; Faktor entsprechend gewählt.

NRC 2006 Referenzwerte adulte Hunde (Erhaltung):
- Vitamin A: 379 RE / 1.000 kcal (≈ 1.263 IE / 1.000 kcal)
- Vitamin D: ≈ 136 IE / 1.000 kcal
- Vitamin E: 123,5 IE / 1.000 kcal (≈ 7,5 mg d-Alpha-Tocopherol)

---

## 🐕 Hundverwaltung (Stammdaten → Hunde)

| Feature | Beschreibung |
|---------|-------------|
| Hund anlegen / bearbeiten | Name, Rasse, Geburtsdatum, Geschlecht, Kastration, Notizen |
| Soft-Aktivierung | Hund deaktivieren statt löschen |
| Kcal-Bedarf manuell | Überschreibt die automatische RER-Berechnung (Stammdaten → Hund bearbeiten) |
| Gewichtserfassung | Eigener Dialog mit Verlaufstabelle (letzte 15 Einträge) |
| Gewichtsverlauf-Chart | Kurve in der Statistik |
| Mehrere Hunde | Beliebig viele Hunde; alle Auswertungen per Hund filterbar |

---

## 🥩 Zutaten & Supplements (Stammdaten → Zutaten)

| Feature | Beschreibung |
|---------|-------------|
| Zutat anlegen / bearbeiten | Name, Hersteller, Kategorie, Typ, Status |
| Bezugsmenge wählen | pro 100 g / 1 g / 1 kg / 1000 g / Tablette |
| Tablettengewicht | Gewicht pro Tablette in g hinterlegen; Futterrechner zeigt dann Stückzahl statt Gramm |
| Einheit je Nährstoff wählbar | g / mg / µg / IE / kcal; IE wird automatisch in µg/mg konvertiert |
| Vitamin-E-Form (Android-Neu) | Auswahl natürlich / synthetisch / Acetat für korrekte IE-Umrechnung |
| 39 NRC-Nährstoffe | Alle Werte im Zutat-Dialog eingeben (einklappbar, gruppiert nach Makros / Aminosäuren / Fettsäuren / Mineralstoffe / Vitamine) |
| Zutat soft-löschen | Mit Undo-Banner (8 Sek., bis zu 5 rückgängig) |
| Automatischer Futterrechner-Sync | Dropdown im Rechner sofort nach Speichern aktualisiert |
| Nährstoff-Import USDA + Open Food Facts parallel | Beide Quellen gleichzeitig; Zwei-Spalten-Ergebnisliste |
| Import überschreibt keine Werte | Nur leere Felder werden befüllt |
| **Zutat-zu-Zutat-Vergleich** | ⚖️-Button in der Zutaten-Liste; zwei Zutaten auswählen → alle 39 Nährstoffe nebeneinander mit Ampelfarben und Delta-Pfeil (▲/▼ in %) |

---

## 🧮 Futterrechner

| Feature | Beschreibung |
|---------|-------------|
| Rezepte erstellen & bearbeiten | Für jeden Hund separat |
| Zutatenmenge je Typ | Lebensmittel: Gramm; Tablette: Stückzahl (Dezimalzahl, z. B. 0,5); Anzeige zeigt Grammäquivalent |
| Skalierungsfaktor | Schnellwahl ×0,25 / ×0,5 / ×1 / ×2 + eigenes Eingabefeld für beliebigen Faktor |
| Portionen pro Tag | Tagesration wird durch konfigurierbaren Portionswert geteilt und als „g je Portion" angezeigt |
| Nährstoffanalyse | Alle 39 NRC-Nährstoffe; Ampelfarben (ok / low / high / zero) |
| IE-Anzeige | Vitamine A, D, E wahlweise in IE oder µg/mg |
| Toleranzbalken | Individuelle Min / Max / Empfehlung je Nährstoff und Hund (in %) |
| Rezept-Mix | Verschachtelte Rezepte (max. 5 Ebenen, Zykluserkennung) |
| Kcal-Berechnung | Automatisch aus Gramm + Zutaten-Nährwerten |
| Gekocht-Flag | Kochverlustfaktor für B-Vitamine konfigurierbar (Standard: 30 % Verlust = Faktor 0,70); gilt für B1, B2, B3, B5, B6, B9, B12 |
| Nährstoff-Popup | Detailinfos (NRC / AAFCO / FEDIAF) per Tap |
| Soft-Delete Rezepte | Mit Undo-Banner |
| Rezept-Vergleich A vs. B | Zwei Rezepte, optionale Gramm-Eingabe je Rezept; Delta-Spalte in % des Tagesbedarfs |
| Ca:P-Verhältnis & Omega 6:3 | Automatisch als Badge; Ziel Ca:P = 1,2–1,5:1; Omega = max. 6:1 |
| Multi-Futter im Tagebuch | Mehrere Rezepte mit Gramm-Angabe in einem Futtereintrag; automatische Kcal-Berechnung + Komponentenaufschlüsselung |

---

## 📓 Tagebuch

Alle Tabs haben Soft-Delete mit Undo-Banner und Edit-Dialog für bestehende Einträge.

### Eingabe-Tabs

| Tab | Felder |
|-----|--------|
| 🌤 Umwelt | Datum, Außentemp min/max, Luftfeuchte, Niederschlag, Pollen (Typ + Stärke), Raumtemp, Raumfeuchte, Bett (Unverändert / Gewechselt), Notizen |
| 🔍 Symptom | Datum, Kategorie (Juckreiz / Ohrentzündung / Hautrötung / Pfoten lecken / Durchfall / Erbrechen / Schütteln / Sonstiges + Freitext), Beschreibung, Schweregrad 0–5, Körperstelle (Ohren / Pfoten / Bauch / Rücken / Beine / Gesicht + Freitext), Notizen |
| 🥩 Futter | Datum, mehrere Rezept-Positionen (Rezept + Gramm, je Portion), Freitext-Ergänzung, Protokoll-Felder (Produkt, Erstgabe, 2-Wochen-Phase, Provokation, Reaktion), Notizen |
| 🚫 Ausschluss | Zutat, Verdachtsstufe **0–3** (0 = Sicher, 1 = leicht, 2 = mittel, 3 = stark), Kategorie, Status, Erstmals gegeben (Datum), Reaktion, Notizen |
| ⚠️ Allergen | Allergen, Kategorie, Reaktionsstärke 1–5, Symptome, Notizen |
| 🏥 Tierarzt | Datum, Praxis, Anlass/Diagnose, Untersuchungen, Ergebnis/Befund, Empfehlung/Therapie, Folgebesuch (Datum) |
| 💊 Medikament | Name, Typ, Dosierung, Häufigkeit, Von–Bis, Verordnet von, Notizen/Wirkung |
| 📅 Phasen | Phasentyp, Getestete Zutat (nur bei Provokation), Startdatum, Enddatum, Ergebnis (offen / verträglich / Reaktion), Notizen |

### Ausschlussdiät-Phasentracker

| Feature | Beschreibung |
|---------|-------------|
| Phasentypen | Elimination (42 Tage Standard), Provokation (14 Tage), Ergebnis (7 Tage) |
| Enddatum-Vorschlag | Automatisch aus Startdatum + Standarddauer berechnet; manuell überschreibbar |
| Aktiver-Phasen-Banner | Fortschrittsbalken (Tage verbraucht / gesamt / verbleibend); letzter Phasenstatus |
| Phasenliste | Alle Phasen mit Typ-Badge, Ergebnis-Badge, Datum, Notizen, Löschen |
| Soft-Delete + Undo | Bis zu 5 Phasen im Undo-Stack |

---

## 🌿 Wetter & Pollen (Tagebuch → Umwelt-Tab)

| Feature | Beschreibung |
|---------|-------------|
| Wetter-Auto-Load | BrightSky API (DWD) – Außentemp min/max, Luftfeuchte, Niederschlag |
| Pollen DWD | 8 Pollenarten, 18 deutsche Regionen, via DWD OpenData |
| Pollen Open-Meteo | Koordinatenbasiert, kein API-Key |
| Pollen-Auswahl UI | Toggle-Buttons mit Stärke-Anzeige; Vorauswahl ab „mittel" |
| Eigene Pollenarten | Beliebig erweiterbar; lokal gespeichert |
| Pollen-Log | Jede Pollenart als eigene Zeile in der DB → auswertbar in Statistik |
| Skala 0–5 | 0 = keine · 1 = gering · 2 = gering–mittel · 3 = mittel · 4 = mittel–stark · 5 = stark |

---

## 📊 Statistik

| Feature | Beschreibung |
|---------|-------------|
| Hund-Filter | Auswahl per Dropdown |
| Zeitraum-Filter | 30 / 90 / 180 Tage / 1 Jahr / Alles |
| Hund-Vergleich | Zweites Hund-Dropdown; Symptomverlauf beider Hunde im selben Chart; blaues Flächenband für Hund 2 |
| KPI-Kacheln | Symptomtage, Ø Schweregrad, Pollentage |
| Konfigurierbarer Chart | Parameter-Auswahl; startet ohne Vorauswahl (Nutzer wählt aktiv aus) |
| Temperaturband | Gefülltes oranges Band Min–Max |
| Schweregrad Symptome | Rotes gefülltes Flächenband (fill from 0) |
| Pollen-Popup-Dialog | Bottom-Sheet mit allen Pollenarten aus Log + eigenen Pollen; alle standardmäßig aktiv; Alle/Keine-Button |
| Symptom-Muster-Heatmap | Wochentag (Mo–So) + Monat (Jan–Dez); Ø Schweregrad als farbige Kacheln; ab 14 Einträgen |
| Korrelationsanalyse | Pollen / Temp / Feuchte vs. Ø Schweregrad; gruppiert; Gruppen mit Ø > 2,0 orange; min. 3 Datenpunkte |
| Zutaten-Reaktionsscore | Anteil Futtertage mit Symptom-Schweregrad > 2 in 48-h-Fenster; min. 3 Beobachtungen; Ampelfarben; Chip-Filter |
| Futter-Reaktionen | Liste nur mit Reaktion oder Provokation |
| Phasen-Timeline | Chronologische Übersicht aller Ausschlussdiät-Phasen mit Typ- und Ergebnis-Badge |
| Medikamente | Liste mit Zeitraum |
| Cache-Status | Anzeige ob Daten aus Cache oder frisch geladen |

---

## 📄 Tierarzt-Export (PDF)

| Feature | Beschreibung |
|---------|-------------|
| Zeitraum wählbar | Von/Bis-Datum + Schnellauswahl (30 / 60 / 90 / 180 Tage) |
| Sektionen (9 gesamt) | Deckblatt ✓ · Symptomverlauf ✓ · Bekannte Allergene ✓ · Ausschlussdiät ✓ · Phasen-Timeline ✓ · Medikamente ✓ · Letzte Futtereinträge ✓ · Reaktionsscore ✗ · Korrelationsanalyse ✗ |
| Standard-Auswahl | Reaktionsscore und Korrelationsanalyse standardmäßig **deaktiviert**; alle anderen aktiv |
| Alle/Keine-Buttons | Schnell-Toggle für alle Sektionen |
| PDF via Android | `PrintManager` / `PdfDocument`; Share-Intent |
| Druckoptimiert | Schwarz-Weiß; Disclaimer + Exportdatum |

---

## 💾 Datenexport & Backup (Android-Neu)

| Feature | Beschreibung |
|---------|-------------|
| CSV-Export | Jede DB-Tabelle als eigene CSV; wahlweise als ZIP |
| Google Sheets Export | Alle Tabellen in ein Spreadsheet via Sheets API; Nutzer wählt Ziel |
| SQLite-Backup | Rohe DB-Datei exportieren (lokal oder Google Drive) |
| Google Drive Sync | Optionaler automatischer täglicher Backup |
| Import aus Backup | SQLite-Backup wiederherstellen |
| Offene Formate | CSV und SQLite sind dokumentierte offene Standards |

---

## ⚙️ Stammdaten – Alle Tabs

| Tab | Inhalt |
|-----|--------|
| 🐕 Hunde | Hunde verwalten, Gewicht, Kcal-Bedarf |
| 🥩 Zutaten | Zutaten / Supplements verwalten, Nährstoffe, Import, Vergleich |
| 📊 Toleranzen | Individuelle Min / Max / Empfehlung je Nährstoff und Hund (in %) |
| ⚙️ Parameter | Globale Rechenwerte: Kochverlust-Faktor (Standard 0,30), Portionen/Tag (Standard 2), RER-Faktor, metabolischer KG-Exponent u. a. |

---

## ⚙️ Einstellungen

| Feature | Beschreibung |
|---------|-------------|
| Google OAuth2 | Login / Logout (nur für Drive/Sheets-Export benötigt) |
| Spreadsheet-IDs | Stammdaten-ID und Tagebuch-ID konfigurierbar |
| Standort | Breitengrad / Längengrad für Wetter-API |
| DWD-Region | Pollen-Region aus 18 deutschen Regionen (Berlin=50, Hamburg=10 …) |
| USDA API-Key | Für Nährstoff-Import aus USDA FoodData Central |
| IE-Anzeige | Standard-Anzeigeeinheit für Vitamine A, D, E (IE oder µg/mg) |
| Sprache | Deutsch / Englisch |
| Dark Mode | Folgt Android-Systemeinstellung |
| Verbindungstest | Prüft Google Drive / Sheets API Zugang |
| Neue Sheets anlegen | Fehlende Sheets automatisch erstellen (Android: fehlende DB-Tabellen migrieren) |

---

## 🏗️ Technische Features

| Feature | Beschreibung |
|---------|-------------|
| Lokale SQLite-DB | Room (Android); keine Abhängigkeit von Drittdiensten |
| Kein Google Sheets als DB | Sheets nur noch als Export-Ziel |
| Kein Backend | 100 % client-seitig |
| IE-Konvertierung | WHO / NRC 2006 Faktoren je Nährstoff; intern immer in µg/mg gespeichert |
| Supplement-Stückzahl | Dezimalzahl (z. B. 0,5 Tabletten); Grammäquivalent wird angezeigt |
| Skalierungsfaktor | ×0,25 / ×0,5 / ×1 / ×2 / eigener Wert für Tagesration |
| Soft-Delete überall | `deleted` / `deleted_at`; bis zu 5 Einträge im Undo-Stack; Banner 8 Sek. |
| FloatParser | Komma (DE) und Punkt (EN) korrekt verarbeitet |
| Rezept-Präzision | Ohne Zwischenrundung bis zur Anzeige; max. 5 Ebenen Rezept-Mix |
| Kochverlust | Nur B-Vitamine (B1–B12); Faktor konfigurierbar (Standard: 30 % Verlust) |
| Mehrsprachigkeit | DE / EN; Nährstoffnamen in DB mehrsprachig |
| Dark Mode | Material You; folgt Android-Systemeinstellung |
| Offline-First | Alle Kernfunktionen ohne Internet; Sync nur bei Backup / Export |
