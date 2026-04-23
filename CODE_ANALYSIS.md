# AllerPaw – Code-Analyse: Ist-Zustand vs. MD-Dokumentation

> Erstellt: 2026-04-23  
> Analysierte Dateien: `index.html`, alle `js/*.js` (v2.3.1) vs. `FEATURE.md`, `PROJECT.md`, `FAQ.md`

---

## Methodik

Vollständige Durchsicht von `index.html` (DOM-Struktur, Formulare, Event-Handler) und allen JS-Modulen (Funktionssignaturen, Implementierungsdetails). Abgleich gegen die aktuellen MD-Dokumente.

---

## ✅ Korrekt dokumentiert – Code stimmt mit MD überein

| Feature | Quelle |
|---------|--------|
| 8 Tagebuch-Tabs (Umwelt, Symptom, Futter, Ausschluss, Phasen, Allergen, Tierarzt, Medikament) | `index.html` Tab-Bar |
| Phasentracker (PHASEN_DEFAULTS: elimination=42, provokation=14, ergebnis=7) | `tagebuch.js:PHASEN_DEFAULTS` |
| Soft-Delete + Undo (max. 5 Stack, 8 Sek. Banner) | `stammdaten.js`, `tagebuch.js` |
| Rezept-Mix (max. 5 Ebenen, Zykluserkennung) | `rechner.js:resolveRezept()` |
| Nährstoff-Import USDA + Open Food Facts parallel | `stammdaten.js:_searchUSDA()`, `_searchOpenFoodFacts()` |
| Import überschreibt keine Werte | `stammdaten.js:applyImportToFields()` |
| Pollen-Log pro Pollenart eine Zeile | `wetter.js:_writePollenLog()` |
| Ca:P-Verhältnis + Omega 6:3 als Badge | `rechner.js:recalc()` |
| Rezept-Vergleich A vs. B | `rechner.js:calcVergleich()`, `initVergleich()` |
| Symptom-Muster-Heatmap (ab 14 Einträgen) | `statistik.js:_renderSymptomMuster()` |
| Korrelationsanalyse (min. 3 Datenpunkte) | `statistik.js:_renderKorrelation()` |
| Zutaten-Reaktionsscore | `statistik.js:_renderReaktionsscore()` |
| Hund-2-Vergleich in Statistik | `statistik.js:onHund2Changed()` |
| 9 Export-Sektionen (Tierarzt-PDF) | `export.js:SECTIONS[]` |
| BrightSky + DWD + Open-Meteo Pollen | `wetter.js:loadAll()` |
| Kochverlust nur B-Vitamine | `rechner.js:COOKING_LOSS_NUTR` (7 B-Vitamine + Langform-Aliase) |
| Tablette als Eingabemodus | `stammdaten.js:PER_OPTIONS[tabletteMode:true]` |
| IE-Konvertierung (Vitamin A, D, E) | `stammdaten.js:IE_FACTORS{}` |
| Sprache DE / EN via i18n-Sheet | `i18n.js:loadSheet('Translations')` |
| `saveWithFeedback()` Einstellungen-Button | `config.js`, `index.html` |
| Pollen-Popup-Dialog (Bottom Sheet) | `statistik.js:showPollenPopup()` |
| Phasen-Timeline in Statistik | `statistik.js:_renderPhasenTimeline()` |

---

## 🔴 In MD fehlend oder falsch – im Code vorhanden

### 1. Zutat-zu-Zutat-Vergleich (Stammdaten)

**Code:** `stammdaten.js` enthält `selectZutatForCompare()`, `showZutatVergleich()`, `_updateCmpBanner()`. In der Zutaten-Liste gibt es einen ⚖️-Button. Nutzer tippt bei zwei Zutaten auf ⚖️ → Vergleichs-Modal öffnet sich mit allen 39 Nährstoffen, Ampelfarben und Delta-Pfeil (▲/▼ in %).

**MD:** Nicht erwähnt. Weder in FEATURE.md noch in FAQ.md.

---

### 2. Kochverlust-Faktor ist ein konfigurierbarer Parameter — nicht fix 0.75

**Code:** `rechner.js:681`: `const cookFactor = 1 - (params['kochverlust_b_vitamine'] || 0.30)`

Der Standardfaktor beträgt **0.30** (= 30 % Verlust, Faktor 0.70), **nicht 0.75** wie in FEATURE.md und FAQ.md dokumentiert. Der Wert ist außerdem über den Parameter-Tab (`Stammdaten → Parameter`) konfigurierbar.

**MD (FEATURE.md):** „Kochverlustfaktor 0,75 für B-Vitamine" → **Falsch**. Korrekt: Verlustfaktor 0.30 → angewendeter Faktor 0.70. Außerdem: konfigurierbar.

**MD (FAQ.md):** „Der Kochverlust gilt ausschließlich für B-Vitamine" → korrekt, aber der Wert fehlt.

---

### 3. Skalierungsfaktor im Futterrechner (×0.25 / ×0.5 / ×1 / ×2 + eigener Wert)

**Code:** `index.html` zeigt Scale-Buttons `×0.25`, `×0.5`, `×1`, `×2` plus ein freies Eingabefeld für beliebige Faktoren. `rechner.js:setScale()`, `applyScale()`.

**MD:** Nicht erwähnt in FEATURE.md.

---

### 4. Portionen pro Tag (Parameter)

**Code:** `rechner.js:707-710` liest `params['portionen_pro_tag'] || 2` und zeigt im Rechner „g je Portion" an. Konfigurierbar im Parameter-Tab.

**MD:** Nicht erwähnt. Im Rechner-Panel wird die Tagesration automatisch durch die Portionszahl geteilt.

---

### 5. IE-Konvertierung: Vitamin D2 wird gleich wie D3 behandelt — und Vitamin E hat keine Form-Auswahl

**Code:** `IE_FACTORS` hat nur einen einzigen Eintrag für `Vitamin E` (0.67 mg/IE). Es gibt **keine** Unterscheidung zwischen natürlicher (d-Alpha) und synthetischer Form (dl-Alpha) oder Acetat-Form.

**MD (FEATURE.md v0.1):** Beschreibt 4 verschiedene Faktoren für Vitamin E und eine Auswahl der E-Form. Das ist **noch nicht implementiert** — in der Web-App gibt es diese Ausdifferenzierung nicht.

**Handlungsbedarf für Android:** Die FEATURE.md beschreibt hier bereits den Soll-Zustand der Android-App, der über die Web-App hinausgeht. Das ist richtig so — muss aber als „neu in Android, nicht in Web vorhanden" markiert werden.

---

### 6. `PER_OPTIONS`: Eingabe auch per 1g, 1kg, 1000g — nicht nur 100g und Tablette

**Code:** `stammdaten.js:PER_OPTIONS`:
- `/ 100g` (Faktor 1)
- `/ 1kg` (Faktor 0.1)
- `/ 1g` (Faktor 100)
- `/ 1000g` (Faktor 0.1)
- `/ Tablette` (tabletteMode: true)

**MD:** Nur „pro 100 g" und „pro Einheit (Tablette)" erwähnt. Die anderen Bezugsmengen fehlen.

---

### 7. Tablette: Nährstoffwert-Eingabe „pro Tablette" — interne Speicherung als /100g-Äquivalent

**Code:** Wenn `tabletteMode: true` und Tablettengewicht > 0, berechnet `_tablettePerFactor(gewichtProTabl)` den Umrechnungsfaktor. Die Nährstoffe werden in der DB immer als Äquivalent pro 100g gespeichert, nicht als Rohwert pro Tablette.

Im Futterrechner (`renderIngredients()`): Zeigt Anzahl Tabletten an (`anzahl = ing.grams / tabG`), darunter den äquivalenten Grammwert.

**MD:** FEATURE.md sagt „Nährstoffangaben immer in der Speichereinheit (µg/mg/g) abgelegt" — das ist für die Android-Version korrekt als Ziel, aber die Web-App speichert **als /100g-Äquivalent** (nicht als Rohwert pro Tablette). Dieser Unterschied sollte in der Migrations-Entscheidung beachtet werden.

---

### 8. Export: Reaktionsscore und Korrelation sind standardmäßig DEAKTIVIERT

**Code:** `export.js:SECTIONS[]`:
- `reaktion`: `default: false`
- `korrelation`: `default: false`
- Alle anderen 7 Sektionen: `default: true`

**MD (FEATURE.md):** „9 Toggle-Sektionen" ohne Hinweis auf Standardzustand.

---

### 9. Symptom-Kategorien sind fest im HTML kodiert (nicht aus Sheets)

**Code:** `index.html` Tab-Symptom: Die Kategorie-Buttons (Juckreiz, Ohrentzündung, Hautrötung, Pfoten lecken, Durchfall, Erbrechen, Schütteln, Sonstiges) sind direkt im HTML. Es gibt ein zusätzliches Freitext-Feld „Eigene Kategorie…".

**MD:** Nicht erwähnt. Für die Android-App relevant: Sollen die Kategorien konfigurierbar sein?

---

### 10. Körperstellen sind fest im HTML kodiert

**Code:** `index.html`: Ohren, Pfoten, Bauch, Rücken, Beine, Gesicht + Freitext-Feld.

**MD:** Nicht erwähnt. Gleiche Frage wie oben.

---

### 11. Futter-Tab: Multi-Futter mit dynamischen Rezept-Items + Freitext-Ergänzung

**Code:** `tagebuch.js` exportiert `addFutterItem()`, `removeFutterItem()`, `futterItemRezeptChanged()`, `futterItemPortionenChanged()`, `futterItemGrammChanged()`, `renderFutterItems()`. Es können mehrere Rezepte mit je Gramm-Angabe eingetragen werden. Zusätzlich gibt es ein Freitext-Ergänzungsfeld.

**MD:** FEATURE.md Tagebuch-Tab Futter beschreibt nur „Rezept-/Futtername, Produkt, Erstgabe, 2-Wochen-Phase, Provokation, Reaktion". Die Multi-Futter-Struktur und Kcal-Berechnung aus mehreren Rezepten fehlt.

---

### 12. Stammdaten: 4 Tabs (Hunde, Zutaten, Toleranzen, Parameter)

**Code:** `index.html` Stammdaten-Panel hat 4 Tabs: `Hunde`, `Zutaten`, `Toleranzen`, `Parameter`.

**MD:** FEATURE.md erwähnt nur Hunde und Zutaten explizit. Toleranzen sind als Feature beschrieben, aber nicht als eigener Tab. Parameter-Tab fehlt ganz.

---

### 13. Ausschluss-Tab: Verdachtsstufe geht von 0–3, nicht 1–3

**Code:** `index.html` Ausschluss-Tab: Toggle-Grid mit `0 – Sicher`, `1 – Leichter Verdacht`, `2 – Mittlerer Verdacht`, `3 – Starker Verdacht`.

**MD (FEATURE.md):** „Verdachtsstufe 1–3" → **Falsch**. Korrekt: 0–3 (0 = Sicher getestet).

---

### 14. Bett-Feld im Umwelt-Tab: Toggle „Unverändert / Gewechselt"

**Code:** `index.html` Umwelt-Tab: Toggle-Grid mit „Unverändert" / „Gewechselt".

**MD:** FEATURE.md erwähnt „Bett" als Feld ohne Beschreibung der Auswahloptionen.

---

### 15. Statistik startet ohne vorausgewählte Parameter

**Code:** `statistik.js`: `_selected` startet als leeres Set.

**MD:** In PROJECT.md korrekt dokumentiert. In FEATURE.md nicht erwähnt.

---

## 🟡 Dokumentiert aber im Code noch nicht / anders implementiert (Android-Soll vs. Web-Ist)

| Feature (MD-Beschreibung) | Status im Web-Code | Handlung |
|---------------------------|-------------------|---------|
| SQLite als Primärspeicher | ❌ Nicht vorhanden (Web nutzt Sheets) | Android-Neu-Implementation |
| CSV / SQLite Export | ❌ Nicht vorhanden | Android-Neu-Implementation |
| Google Drive Sync | ❌ Nicht vorhanden | Android-Neu-Implementation |
| Kein Google-Login für Grundfunktion | ❌ Web erfordert Login | Android-Änderung |
| Vitamin-E-Form-Auswahl (d-Alpha / dl-Alpha / Acetat) | ❌ Nur 1 Faktor in Web | Android-Erweiterung |
| Tablet: Speicherung pro Tablette (Rohwert) | ⚠️ Web speichert als /100g-Äquivalent | Android-Entscheidung nötig |
| Supplement-Tagesübersicht im Rechner | ❌ Nicht vorhanden | Android-Erweiterung |
| Supplement-Verlauf in Statistik (IE-Kurve) | ❌ Nicht vorhanden | Android-Erweiterung |

---

## 📋 Empfohlene MD-Korrekturen

| Datei | Korrektur |
|-------|-----------|
| FEATURE.md | Zutat-zu-Zutat-Vergleich in Stammdaten ergänzen |
| FEATURE.md | Kochverlustfaktor: 0.75 → 0.30 (= 70 % angewendet), konfigurierbar |
| FEATURE.md | Skalierungsfaktor im Futterrechner (×0.25 / ×0.5 / ×1 / ×2 / eigener Wert) |
| FEATURE.md | Portionen pro Tag als Parameter |
| FEATURE.md | PER_OPTIONS: alle Bezugsmengen (100g, 1g, 1kg, 1000g, Tablette) |
| FEATURE.md | Multi-Futter im Tagebuch-Futter-Tab + Freitext-Ergänzung |
| FEATURE.md | Stammdaten: 4 Tabs inkl. Toleranzen und Parameter beschreiben |
| FEATURE.md | Ausschluss-Verdachtsstufe: 0–3 (nicht 1–3) |
| FEATURE.md | Export: Reaktionsscore und Korrelation standardmäßig deaktiviert |
| FEATURE.md | Symptom-Kategorien: fest + Freitext-Ergänzung |
| FAQ.md | Kochverlust-Faktor-Wert ergänzen |
| FAQ.md | Zutat-Vergleich erklären |
| MIGRATION.md | Tablet-Speicherentscheidung: Rohwert/Tablette vs. /100g-Äquivalent klären |
