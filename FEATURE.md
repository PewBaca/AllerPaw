# AllerPaw – Feature-Übersicht (v0.1)

> Letzte Aktualisierung: 2026-04-23  
> Status: Android-Migration – alle Funktionen aus der Web-App (v2.3.1) werden portiert

---

## 🐕 Hundverwaltung (Stammdaten → Hunde)

| Feature | Beschreibung |
|---------|-------------|
| Hund anlegen / bearbeiten | Name, Rasse, Geburtsdatum, Geschlecht, Kastration, Notizen |
| Soft-Aktivierung | Hund deaktivieren statt löschen – bleibt in der Datenbank erhalten |
| Kcal-Bedarf manuell | Überschreibt die automatische RER-Berechnung; einstellbar im Hund-Bearbeitungs-Screen |
| Gewichtserfassung | Eigener Dialog mit Verlaufstabelle (letzte 15 Einträge) |
| Gewichtsverlauf-Chart | Kurve in der Statistik, wenn Hund_Gewicht-Daten vorhanden |
| Mehrere Hunde | Beliebig viele Hunde verwaltbar; alle Auswertungen per Hund filterbar |

---

## 🥩 Zutaten & Nährstoffe (Stammdaten → Zutaten)

| Feature | Beschreibung |
|---------|-------------|
| Zutat anlegen / bearbeiten | Name, Hersteller, Kategorie, Status |
| 39 NRC-Nährstoffe | Alle Werte direkt im Zutat-Dialog eingeben (einklappbarer Bereich, gruppiert nach Makros / Aminosäuren / Fettsäuren / Mineralstoffe / Vitamine) |
| Nährstoffangaben pro 100 g Frischgewicht | Standard-Basis für alle Berechnungen |
| Zutat soft-löschen | Soft-Delete mit Undo-Möglichkeit (8 Sekunden, bis zu 5 rückgängig machbar) |
| Automatischer Futterrechner-Sync | Dropdown im Rechner wird nach Speichern sofort aktualisiert |
| Nährstoff-Import USDA + Open Food Facts parallel | Beide Quellen gleichzeitig abgerufen; Zwei-Spalten-Ergebnisliste; Werte je Quelle sichtbar |
| Import überschreibt keine Werte | Nur leere Felder werden befüllt – vorhandene Einträge bleiben erhalten |

---

## 🧮 Futterrechner

| Feature | Beschreibung |
|---------|-------------|
| Rezepte erstellen & bearbeiten | Für jeden Hund separat; Name, Zutaten, Gramm-Angaben |
| Nährstoffanalyse | Alle 39 NRC-Nährstoffe; Balken mit Ampelfarben (ok / low / high / zero) |
| Toleranzbalken | Individuelle Min / Max / Empfehlung je Nährstoff und Hund (in %) |
| Rezept-Mix | Verschachtelte Rezepte (max. 5 Ebenen, Zykluserkennung) |
| Kcal-Berechnung | Automatisch aus Gramm + Zutaten-Nährwerten |
| Gekocht-Flag | Kochverlustfaktor 0.75 für B-Vitamine je Zutat |
| Nährstoff-Popup | Detailinfos (NRC / AAFCO / FEDIAF) per Tap auf einen Nährstoffbalken |
| Soft-Delete Rezepte | Mit Undo-Banner |
| Rezept-Vergleich A vs. B | Zwei Rezepte nebeneinander vergleichen; Delta-Spalte zeigt Differenz in % des Tagesbedarfs |
| Ca:P-Verhältnis & Omega 6:3 | Automatisch berechnet und als Badge angezeigt |

---

## 📓 Tagebuch

Das Tagebuch ist in Tabs gegliedert. Jeder Eintrag ist soft-löschbar mit Undo-Banner und per Edit-Dialog nachträglich bearbeitbar.

### Tab-Übersicht

| Tab | Felder |
|-----|--------|
| 🌤 Umwelt | Außentemp min/max, Luftfeuchte außen, Niederschlag, Pollen (Typ + Stärke), Raumtemp, Raumfeuchte, Bett, Notizen |
| 🔍 Symptom | Kategorie, Beschreibung, Schweregrad (0–5), Körperstelle, Notizen |
| 🥩 Futter | Rezept-/Futternahme, Produkt, Erstgabe (ja/nein), 2-Wochen-Phase (ja/nein), Provokation (ja/nein), Reaktion |
| 🚫 Ausschluss | Zutat, Verdachtsstufe 1–3, Kategorie, Status, Reaktion |
| ⚠️ Allergen | Allergen, Kategorie, Reaktionsstärke 1–5, Symptome |
| 🏥 Tierarzt | Datum, Praxis, Anlass, Untersuchungen, Ergebnis, Therapie, Folgebesuch |
| 💊 Medikament | Name, Typ, Dosierung, Häufigkeit, Von–Bis, Verordnet von |
| 📅 Phasen | Phasentyp (Elimination / Provokation / Ergebnis), Zeitraum, Zutat (bei Provokation), Notizen |

### Ausschlussdiät-Phasentracker

| Feature | Beschreibung |
|---------|-------------|
| Phasentypen | Elimination (Standard 42 Tage), Provokation (14 Tage), Ergebnis (7 Tage) |
| Enddatum | Per Vorschlag vorausgefüllt, manuell überschreibbar |
| Aktiver-Phasen-Banner | Farbiger Banner mit Fortschrittsbalken (Tage verbraucht / gesamt / verbleibend) |
| Letzter Phasenstatus | Zuletzt abgeschlossene Phase wird im Banner angezeigt |
| Phasenliste | Alle Phasen mit Typ-Badge, Ergebnis-Badge, Datum, Notizen und Löschen-Option |
| Soft-Delete + Undo | Bis zu 5 Phasen rückgängig machbar |

---

## 🌿 Wetter & Pollen (Tagebuch → Umwelt-Tab)

| Feature | Beschreibung |
|---------|-------------|
| Wetter-Auto-Load | BrightSky API (DWD) – Außentemperatur min/max, Luftfeuchte, Niederschlag automatisch geladen |
| Pollen DWD | 8 Pollenarten, 18 deutsche Regionen, via DWD OpenData |
| Pollen Open-Meteo | Koordinatenbasiert, kein API-Key erforderlich |
| Pollen-Auswahl UI | Toggle-Buttons mit Stärke-Anzeige; Vorauswahl ab „mittel" |
| Eigene Pollenarten | Über „⚙️ Verwalten" beliebig erweiterbar (lokal gespeichert) |
| Pollen_Log | Jede Pollenart wird als eigene Zeile geschrieben → auswertbar in Statistik |
| Skala 0–5 | 0 = keine, 1 = gering, 2 = gering–mittel, 3 = mittel, 4 = mittel–stark, 5 = stark |

---

## 📊 Statistik

| Feature | Beschreibung |
|---------|-------------|
| Hund-Filter | Auswahl per Dropdown |
| Zeitraum-Filter | 30 / 90 / 180 Tage / 1 Jahr / Alles |
| Hund-Vergleich | Zweites Hund-Dropdown; Symptomverlauf beider Hunde im selben Chart |
| KPI-Kacheln | Symptomtage, Ø Schweregrad, Pollentage |
| Konfigurierbarer Chart | Beliebige Kombination aus verfügbaren Parametern |
| Temperaturband | Gefülltes oranges Band zwischen Tagesminimum und -maximum |
| Innentemperatur | Linie |
| Luftfeuchte außen/innen | Gestrichelte Linien |
| Schweregrad Symptome | Rotes gefülltes Flächenband (fill from 0) – deutliche visuelle Hervorhebung |
| Schweregrad Hund 2 | Blaues Flächenband, nur wenn zweiter Hund gewählt |
| Gewicht | Verlaufslinie (nur wenn Gewichtsdaten vorhanden) |
| Pollen-Popup | Popup-Dialog mit allen Pollen-Typen aus Pollen_Log + eigenen Pollen; alle standardmäßig aktiv |
| Symptom-Muster-Heatmap | Wochentag (Mo–So) + Monat (Jan–Dez); Ø Schweregrad als farbige Kacheln; ab 14 Einträgen |
| Korrelationsanalyse | Pollen / Außentemperatur / Luftfeuchte vs. Ø Schweregrad; gruppiert; Gruppen mit Ø > 2.0 orange; min. 3 Datenpunkte |
| Zutaten-Reaktionsscore | Score = Anteil der Futtertage mit Symptom-Schweregrad > 2 in den folgenden 48 h; Mindestens 3 Beobachtungen; Ampelfarben |
| Futter-Reaktionen | Liste (nur Einträge mit Reaktion oder Provokation) |
| Phasen-Timeline | Chronologische Übersicht aller Ausschlussdiät-Phasen mit Ergebnis-Badges |
| Medikamente | Liste mit Zeitraum |
| Cache-Status | Anzeige ob Daten aus Cache oder frisch geladen |

---

## 📄 Tierarzt-Export (PDF)

| Feature | Beschreibung |
|---------|-------------|
| Zeitraum wählbar | Von/Bis-Datumseingabe + Schnellauswahl (30 / 60 / 90 / 180 Tage) |
| 9 wählbare Sektionen | Deckblatt, Symptome, Allergene, Ausschlussdiät, Phasen, Medikamente, Futter, Reaktionsscore, Korrelationsanalyse |
| Alle/Keine-Auswahl | Schnell alle Sektionen aktivieren oder deaktivieren |
| Druckoptimiert | Schwarz-Weiß-kompatibel; Disclaimer und Exportdatum im Footer |
| Kein Backend | Export via Android-Druckdialog / PDF-Speichern |

---

## ⚙️ Einstellungen

| Feature | Beschreibung |
|---------|-------------|
| Google OAuth2 | Login / Logout |
| Spreadsheet IDs | Stammdaten-ID und Tagebuch-ID konfigurierbar |
| Standort | Breitengrad / Längengrad für automatischen Wetter-Abruf |
| DWD-Region | Pollen-Region aus 18 deutschen Regionen wählbar |
| Neue Sheets anlegen | Automatisch via Button (Rezept_Komponenten, Translations, Hund_Gewicht, Pollen_Log, Ausschluss_Phasen) |
| USDA API-Key | Key für automatischen Nährstoff-Import aus USDA FoodData Central |
| Verbindungstest | Prüft Sheets-API-Zugang |
| Sprache | Deutsch / Englisch (i18n) |
| Dark Mode | Folgt System-Einstellung (Android) |

---

## 🏗️ Technische Features

| Feature | Beschreibung |
|---------|-------------|
| Kein eigenes Backend | Datenhaltung vollständig in Google Sheets |
| Google Sheets als DB | Dual-Spreadsheet (Stammdaten + Tagebuch) |
| Lokaler Cache | Stammdaten im Speicher; Tagebuch-Daten mit TTL 10 Min (Room / SharedPreferences) |
| Soft-Delete überall | `deleted` / `deleted_at` statt hartem Löschen |
| NaN-Schutz | Alle Float-Parsing-Stellen mit Fallback; Komma-Dezimaltrenner (DE) wird korrekt verarbeitet |
| Rezept-Präzision | `resolveRezept()` ohne Zwischenrundung bis zur Anzeige |
| Dark Mode | Folgt `prefers-color-scheme` / Android-Systemeinstellung |
| Offline-Anzeige | Gecachte Daten bleiben sichtbar; schreibende Operationen erfordern Verbindung |
| Mehrsprachigkeit | Deutsch / Englisch via i18n-Modul |
