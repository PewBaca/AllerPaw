# AllerPaw – Validierungsplan (v0.2)

> Letzte Aktualisierung: 2026-04-24

---

## Phase 1 – Abgeschlossen ✅

| Test | Erwartetes Ergebnis | Status |
|------|---------------------|--------|
| App startet ohne Crash | LoginScreen erscheint | ✅ |
| Demo-Login | Direkt in App, Bottom Nav sichtbar | ✅ |
| Hund anlegen | Erscheint in Hunde-Liste | ✅ |
| Hund bearbeiten | Dialog öffnet mit bestehenden Werten | ✅ |
| Hund löschen | Soft-Delete, verschwindet aus Liste | ✅ |
| Hund mit Gewicht → Rechner | RER/MER wird berechnet und angezeigt | ✅ |
| Aktivitätsfaktor-Slider | MER aktualisiert sich live | ✅ |
| Dark Mode | Folgt Systemeinstellung | ✅ |
| Sprache EN | strings.xml en greift bei englischer Systemsprache | ✅ |

---

## Phase 2 – Geplant

| Test | Erwartetes Ergebnis |
|------|---------------------|
| Zutat anlegen (Lebensmittel) | Erscheint in Zutaten-Liste |
| Zutat anlegen (Tablette) | Stückzahl-Modus, Gewicht hinterlegt |
| Nährstoff-Import USDA | Nur leere Felder werden befüllt |
| Nährstoff-Import Open Food Facts | Parallel zu USDA; Zwei-Spalten-Vorschau |
| Tagebuch Umwelt-Tab | Eintrag anlegen, speichern, wieder öffnen |
| Tagebuch Symptom-Tab | Schweregrad 0–5, Körperstelle |
| Tagebuch Futter-Tab | Mehrere Rezepte + Gramm, Kcal-Summe |
| Soft-Delete + Undo | Banner 8 Sek., Undo stellt wieder her, max. 5 Stack |
| Phasentracker Elimination | Enddatum = Startdatum + 42 Tage (vorgeschlagen) |
| FloatParser | Komma und Punkt werden korrekt als Dezimaltrenner erkannt |

---

## Phase 3 – Geplant

| Test | Erwartetes Ergebnis |
|------|---------------------|
| Rezept erstellen | Zutaten + Gramm, NRC-Analyse korrekt |
| Kochverlust | Nur B-Vitamine um Faktor 0.70 reduziert (konfigurierbar) |
| Rezept-Mix | Verschachtelung max. 5 Ebenen; Zykluserkennung verhindert Endlosschleife |
| Skalierungsfaktor ×0.5 | Alle Grammwerte halbiert |
| Rezept-Vergleich A vs. B | Delta-Spalte in % des Tagesbedarfs |
| Zutat-Vergleich ⚖️ | Alle Nährstoffe nebeneinander, Ampelfarben, Delta-Pfeil |
| Statistik-Heatmap | Nur angezeigt ab 14 Symptomeinträgen |
| Korrelationsanalyse | Nur bei min. 3 Datenpunkten; Gruppen Ø > 2.0 orange |
| Reaktionsscore | 48-h-Fenster; min. 3 Beobachtungen pro Zutat |

---

## Phase 4 – Geplant

| Test | Erwartetes Ergebnis |
|------|---------------------|
| PDF-Export | Alle aktivierten Sektionen korrekt gerendert |
| PDF Schwarz-Weiß | Kein Farb-Druck-Overhead |
| CSV-Export | Alle Tabellen, korrekte Spaltenreihenfolge |
| SQLite-Backup | DB-Datei exportierbar + wiederherstellbar |
| Wetter-API | Außentemp, Feuchte, Niederschlag geladen |
| Pollen DWD | 8 Pollenarten für gewählte Region |

---

## Regressionsregeln (immer gültig)

- Soft-Delete darf Daten nie endgültig löschen (nur `deleted=1`, `deleted_at` gesetzt)
- Undo-Stack: max. 5 Einträge; Banner immer 8 Sekunden sichtbar
- Kochverlust nur für B1, B2, B3, B5, B6, B9, B12 — nie für andere Nährstoffe
- Rezept-Mix: Zykluserkennung muss Endlosschleife bei verschachtelten Rezepten verhindern
- Nährstoff-Werte intern immer als per-100g gespeichert
- IE immer sofort beim Speichern in µg/mg konvertiert — nie als IE in DB
- Symptom-Heatmap: ab 14 Einträgen — darunter keine Anzeige
