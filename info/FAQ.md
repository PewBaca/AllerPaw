# AllerPaw – FAQ (v0.2)

> Letzte Aktualisierung: 2026-04-24

---

## Architektur & Datenspeicherung

**Warum Room statt Google Sheets als Primärdatenbank?**  
Google Sheets hat Rate-Limits, benötigt Internet und ist langsam. Room läuft komplett lokal, ist offline-fähig und deutlich performanter. Sheets bleibt als optionaler Export/Backup-Kanal erhalten.

**Werden bestehende Google-Sheets-Daten unterstützt?**  
Als Import: ja (geplant Phase 4). Als Primärspeicher: nein. Die neue App speichert alles in SQLite.

**Wie sind die Daten gesichert?**  
Automatisches SQLite-Backup optional via Google Drive (Phase 4). Manueller Export als SQLite-Datei oder CSV jederzeit möglich.

---

## Nährstoffberechnung

**Welchen Kochverlustfaktor verwendet der Rechner?**  
Standard: **30 % Verlust**, d.h. angewendeter Faktor **0.70**. Gilt **ausschließlich für B-Vitamine** (B1, B2, B3, B5, B6, B9, B12). Der Wert ist konfigurierbar unter Stammdaten → Parameter (`kochverlust_b_vitamine`).

**Wie wird der Energiebedarf berechnet?**  
- RER (Resting Energy Requirement) = 70 × Gewicht(kg)^0.75 [kcal/Tag]  
- MER (Maintenance Energy Requirement) = RER × Aktivitätsfaktor  
- Aktivitätsfaktor: 1.0 (inaktiv/Kastrat) bis 3.0 (Arbeitshund); Standard 1.6  
- Manueller Kcal-Bedarf überschreibt die RER-Berechnung (im Hund-Profil)

**Wie werden IE-Werte konvertiert?**  
- Vitamin A: 1 IE = 0,3 µg Retinol  
- Vitamin D3: 1 IE = 0,025 µg Cholecalciferol  
- Vitamin E (natürlich, d-Alpha-Tocopherol): 1 IE = 0,67 mg  
- Vitamin E (synthetisch, dl-Alpha-Tocopherol): 1 IE = 0,45 mg  
- Vitamin E (Acetat natürlich): 1 IE = 0,74 mg  
- Vitamin E (Acetat synthetisch): 1 IE = 0,67 mg  

Die Vitamin-E-Form wird je Zutat gespeichert. Intern immer µg/mg — nie IE.

**Wie viele NRC-Nährstoffe sind implementiert?**  
Aktuell 29 Kern-Nährstoffe (Makros, Mineralstoffe, Vitamine, Fettsäuren). Ausbau auf 39 (inkl. Aminosäuren) in Phase 2/3. NRC 2006 Bedarfswerte für adulte Hunde.

**Gibt es NRC-Werte für Welpen und Senioren?**  
Geplant für Phase 5. Die Tabellenstruktur erlaubt es, mehrere Bedarfsprofile je Nährstoff zu speichern.

**Was bedeuten die Ampelfarben im Rechner?**  
- OK: 80–150 % des Bedarfs  
- MANGEL: unter 80 %  
- UEBERSCHUSS: über 150 %  
- UEBERSCHRITTEN: über dem definierten UL (Maximaler sicherer Wert)

---

## Futterrechner

**Was ist der Skalierungsfaktor?**  
Schnell-Buttons ×0.25 / ×0.5 / ×1 / ×2 plus freies Eingabefeld. Skaliert alle Grammwerte des Rezepts proportional.

**Wie werden Portionen angezeigt?**  
Die Tagesration wird durch `portionen_pro_tag` (Standard: 2, konfigurierbar) geteilt und als „g je Portion" angezeigt.

**Was ist ein Rezept-Mix?**  
Rezepte können andere Rezepte als Zutat enthalten (verschachtelt). Maximum 5 Ebenen. Zykluserkennung verhindert Endlosschleifen. Keine Zwischenrundung bis zur finalen Anzeige.

**Wie werden Tabletten im Rechner eingegeben?**  
Stückzahl als Dezimalzahl (z.B. 0,5 Tabletten). Das Grammäquivalent wird automatisch angezeigt (Basis: hinterlegtes Tablettengewicht je Zutat).

---

## Tagebuch

**Wie funktioniert der Soft-Delete?**  
Einträge werden nicht gelöscht, sondern als `deleted=TRUE` markiert. Nach dem Löschen erscheint ein Undo-Banner für 8 Sekunden. Bis zu 5 Einträge können rückgängig gemacht werden.

**Was sind die Standard-Phasendauern?**  
- Elimination: 42 Tage  
- Provokation: 14 Tage  
- Ergebnis: 7 Tage  

Das Enddatum wird automatisch vorgeschlagen, ist aber manuell überschreibbar.

**Wie wird der Pollen-Log gespeichert?**  
Jede Pollenart bekommt eine eigene DB-Zeile in `TagebuchPollenLogEntity`. Keine Komma-getrennte Liste.

---

## Statistik

**Ab wann wird die Symptom-Heatmap angezeigt?**  
Ab 14 Symptomeinträgen. Darunter keine Anzeige.

**Was sind die Mindestanforderungen für Korrelationsanalyse und Reaktionsscore?**  
- Korrelationsanalyse: min. 3 Datenpunkte pro Gruppe  
- Reaktionsscore: min. 3 Beobachtungen pro Zutat; 48-h-Fenster nach Futtereintrag  
- Gruppen mit Ø-Schweregrad > 2.0 werden orange hervorgehoben

**Startet die Statistik mit vorausgewählten Parametern?**  
Nein. Der Nutzer wählt aktiv aus, welche Parameter im Chart angezeigt werden.

---

## Technisch

**Wie wird mit dem Komma als Dezimaltrenner umgegangen?**  
`FloatParser.kt` (TODO Phase 2) ersetzt das JS-Äquivalent `_float()` aus der Web-App. Komma (DE) und Punkt (EN) werden gleichermaßen korrekt verarbeitet.

**Warum Hilt als DI-Framework?**  
Standard für Android; gut mit ViewModel, Repository und Room integriert; gut testbar.

**Warum Vico für Charts?**  
Compose-nativ, aktiv maintained, unterstützt gefüllte Flächen und mehrere Serien — entspricht den Anforderungen aus statistik.js.

**Kann die App ohne Google-Login genutzt werden?**  
Ja. Google-Login ist nur für optionalen Drive-Backup / Sheets-Export nötig. Alle Kernfunktionen laufen vollständig offline ohne Account.

---

## Monetarisierung

**Wie soll die App monetarisiert werden?**  
Zwei Optionen geplant für Phase 5:  
1. Optionale Werbeanzeigen beim Login (vom Nutzer aktivierbar)  
2. Spendenoption innerhalb der App  

Keine Pflicht-Werbung, kein Abo-Modell.

**Was ist mit geteilten Datenbanken gemeint?**  
Nutzer sollen Zutaten- und Rezept-Datenbanken importieren, exportieren und teilen können. Geplant ist auch ein Marktplatz für fertige Datenbanken (Phase 5).
