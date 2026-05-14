# AllerPaw – FAQ (v0.10.0)

> Stand: 2026-05-07

---

## Architektur & Datenspeicherung

**Warum Room statt Google Sheets als Primärdatenbank?**
Google Sheets hat API Rate-Limits, benötigt Internet und ist langsam. Room läuft vollständig lokal, ist offline-fähig und deutlich performanter. Sheets bleibt als optionaler Export-Kanal.

**Welche Datenbank-Version ist aktuell?**
Room v3 mit 22 Entities. Version 1→2: Tabletten/Tropfen-Felder. Version 2→3: HundZustand, Task, TaskErledigung.

**Warum `fallbackToDestructiveMigration()` statt echten Migrations?**
Das Projekt ist noch nicht im Store — während der Entwicklung ist destructive Migration akzeptabel. Vor dem ersten öffentlichen Release müssen echte Room-Migrations geschrieben werden.

**Wie sind die Daten gesichert?**
SQLite-Backup via Share-Intent (Export-Tab). CSV-Export für Symptome. Google Drive Sync ist geplant, aber noch nicht implementiert.

---

## Futterkalkulation

**Wie wird der Energiebedarf berechnet?**
- RER (Ruheenergiebedarf) = 70 × Gewicht(kg)^0.75 [kcal/Tag]
- MER (Erhaltungsbedarf) = RER × Aktivitätsfaktor (1.0–3.0, Standard 1.6)
- Manueller Kcal-Bedarf im Hund-Profil überschreibt RER

**Welchen Kochverlustfaktor verwendet der Rechner?**
Standard: **30% Verlust → Faktor 0.70**. Gilt **ausschließlich** für B-Vitamine (B1, B2, B3, B5, B6, B9, B12). Konfigurierbar via `ParameterEntity` (Schlüssel: `kochverlust_b_vitamine`). Alle anderen Nährstoffe: kein Kochverlust.

**Wie werden IE-Werte konvertiert?**
Konvertierung passiert beim Speichern — in der DB stehen immer metrische Werte (µg/mg):
- Vitamin A: 1 IE = 0.3 µg Retinol
- Vitamin D3: 1 IE = 0.025 µg Cholecalciferol
- Vitamin E natürlich (d-Alpha): 1 IE = 0.67 mg
- Vitamin E synthetisch (dl-Alpha): 1 IE = 0.45 mg
- Vitamin E Acetat natürlich: 1 IE = 0.74 mg
- Vitamin E Acetat synthetisch: 1 IE = 0.67 mg

Öffnet man eine Zutat erneut, sieht man den bereits metrisch gespeicherten Wert — kein IE-Wert.

**Was bedeuten die Ampelfarben?**
- 🟢 OK: 80–150% des NRC-Bedarfs
- 🔴 MANGEL: unter 80%
- 🟠 ÜBERSCHUSS: über 150%
- 🔴 ÜBERSCHRITTEN: über dem UL (maximaler sicherer Wert laut NRC 2006)

**Wie funktioniert das Ca:P-Verhältnis?**
Ca:P = Calcium(g) / Phosphor(g) — Ziel: 1.2–1.5:1. Wird grün angezeigt wenn im Zielbereich, sonst rot.

**Wie werden Sub-Rezepte (Rezept-Mix) skaliert?**
`skalierung = mengeG / subRaw.gesamtGrammRoh` — das Sub-Rezept wird mit Faktor 1.0 aufgelöst, dann anteilig auf die gewünschte Menge skaliert. Max. 5 Ebenen. Zykluserkennung verhindert Endlosschleifen.

**Was ist der Unterschied zwischen Tabletten und Tropfen im Rezept?**
- **Tabletten**: Eingabe = Stückzahl (z.B. 0.5) × hinterlegtes Tablettengewicht (g) = Gramm intern
- **Tropfen**: Eingabe = Anzahl Tropfen × hinterlegtes Tropfengewicht (g) = Gramm intern
- Immer live-Vorschau der berechneten Gramm im Picker
- Intern wird immer mit Gramm gerechnet (Nährstoffe per 100g)

**Gibt es NRC-Werte für Welpen und Senioren?**
Ja — `NrcLebensphasen.kt` implementiert Faktoren für Welpe, Senior, Trächtig, Laktierend basierend auf NRC 2006. Die UI-Auswahl (Dropdown je Hund) ist noch nicht implementiert.

---

## Futterumstellungsrechner

**Wie funktioniert der Umstellungsrechner?**
Linearer Übergang von Rezept A (100%) zu Rezept B (0%) über den gewählten Zeitraum. Tag 1 = 100% A + 0% B, letzter Tag = 0% A + 100% B. Gramm-Angaben basieren auf der Tagesration des aktiven Hundes.

**Welche Geschwindigkeiten gibt es?**
- 🐕 Schnell: 5 Tage (gesunder Magen)
- 🐶 Normal: 7 Tage (Standard)
- 🐩 Sanft: 10 Tage (empfindlicher Magen)
- 🐾 Sehr sanft: 14 Tage (Allergiker, Welpen)
- Eigene Tageanzahl: 5–14 frei einstellbar

**Was tun bei Durchfall während der Umstellung?**
Tempo reduzieren (längerer Zeitraum wählen) oder 2 Tage Pause einlegen und aktuelles Verhältnis beibehalten. Im Tagebuch dokumentieren.

---

## Tagebuch

**Wie funktioniert der Zustand-Tab (Smiley)?**
Täglich einen Gesamtzustand eingeben: 😄(1) bis 😢(5). Wird pro Hund und Datum gespeichert (unique Index). Überschreibt den bestehenden Wert bei erneutem Speichern am selben Tag.

**Wie funktioniert der Soft-Delete?**
Einträge werden als `deleted=1` markiert, nicht gelöscht. Undo-Banner erscheint für 8 Sekunden. Max. 5 Einträge im Undo-Stack. Ältester Eintrag verfällt automatisch wenn der Stack voll ist.

**Was sind die Standarddauern für Ausschlussdiät-Phasen?**
- Elimination: 42 Tage
- Provokation: 14 Tage
- Ergebnis: 7 Tage
Enddatum wird automatisch vorgeschlagen, ist manuell überschreibbar.

**Wie wird der Pollen-Log gespeichert?**
Jede Pollenart = eigene DB-Zeile in `TagebuchPollenLogEntity`. Keine Komma-getrennte Liste.

---

## Task-System

**Welche Wiederholungstypen gibt es?**
- **Täglich**: jeden Tag
- **Wöchentlich**: an gewählten Wochentagen (Mo–So, Mehrfachauswahl)
- **Intervall**: alle N Tage (berechnet ab Erstellungsdatum des Tasks)
- **Einmalig**: kein Push, manuell abhaken

**Wie funktionieren Push-Notifications?**
WorkManager prüft täglich um Mitternacht welche Tasks fällig sind. Intervall-Berechnung: `(heute - Erstellungsdatum) % intervallTage == 0`. Nur wenn Task noch nicht erledigt. Notification Permission (POST_NOTIFICATIONS) wird beim App-Start angefragt (Android 13+).

**Werden Tasks pro Hund gespeichert?**
Ja — jeder Task ist einem Hund zugeordnet. Erledigungs-Protokoll wird je Task und Datum gespeichert.

---

## Statistik

**Ab wann wird die Symptom-Heatmap angezeigt?**
Ab 14 Symptomeinträgen im gewählten Zeitraum. Darunter erscheint ein Hinweis.

**Was ist die Pollen-Korrelationsanalyse?**
Für jede Pollenart wird geprüft: Wie viele Symptome traten in einem 48h-Fenster nach Pollen-Belastung (Stärke > 1) auf? Mindestens 3 Datenpunkte erforderlich. Gruppen mit Ø-Schweregrad > 2.0 werden orange markiert.

**Startet die Statistik mit vorausgewählten Parametern?**
Nein — Zeitraum und Hund werden beim ersten Laden gesetzt, aber keine Parameter im Chart vorausgewählt.

---

## Technisch

**Warum AGP 9.1.1 und nicht 8.x?**
Android Studio hat automatisch AGP 9.1.1 eingetragen. AGP 9.x bringt Built-in Kotlin (2.2.10), neues `kotlin { compilerOptions }` DSL (statt `kotlinOptions`), und `compileSdk 36`. Alle Breaking Changes wurden migriert.

**Was ist `android.disallowKotlinSourceSets=false` in gradle.properties?**
KSP registriert generierte Quellen noch via `kotlin.sourceSets` DSL — das ist mit AGP 9.x Built-in Kotlin nicht erlaubt. Diese Flag ist der offizielle Workaround bis KSP vollständig AGP 9.x kompatibel ist.

**Warum HiltWorkerFactory in AllerPawApplication?**
WorkManager mit `@HiltWorker`-annotierten Workern benötigt eine custom `WorkerFactory`. Ohne diese Factory wirft WorkManager zur Laufzeit eine Exception. Die Factory wird via `Configuration.Provider` registriert, was die automatische WorkManager-Initialisierung in `InitializationProvider` deaktiviert (via `tools:node="remove"` im Manifest).

**Wie wird FloatParser verwendet?**
```kotlin
FloatParser.parse("3,14")  // → 3.14
FloatParser.parse("3.14")  // → 3.14
FloatParser.format(3.14)   // → "3,14" (DE)
```
Ersetzt `_float()` aus der Web-App. Komma (DE) und Punkt (EN) werden gleichermaßen akzeptiert.

**Warum `fallbackToDestructiveMigration()` in DatabaseModule?**
Während der Entwicklung akzeptabel. Vor erstem Store-Release: echte Room-Migrations schreiben (ALTER TABLE für neue Felder, keine `fallback`-Strategie).

**Was muss vor dem Play Store Release noch gemacht werden?**
1. Echte Room-Migrations (v1→2, v2→3) implementieren
2. SHA-1 in Google Cloud Console für Google Sign-In registrieren
3. `WEB_CLIENT_ID` in `AuthRepository` eintragen
4. ProGuard auf Release-Build testen
5. `android.disallowKotlinSourceSets` auf KSP-Update warten
6. `HttpLoggingInterceptor.Level.BODY` auf `BASIC` oder `NONE` für Release

---

## Monetarisierung

**Wie soll die App monetarisiert werden?**
Zwei Optionen (geplant):
1. Optionale Werbung beim Login (vom Nutzer aktivierbar)
2. Spendenoption innerhalb der App

Kein Abo-Modell, keine Pflicht-Werbung.

**Was sind geteilte Datenbanken?**
Geplant: Nutzer können Zutaten- und Rezept-Datenbanken importieren, exportieren und teilen. Marktplatz für fertige BARF-Datenbanken als optionales Feature.

---

## Android 16 / API Level 36

**Warum wurde auf targetSdk 36 angehoben?**
Ab 31. August 2025 müssen neue Google Play Apps auf API 35+ ausgerichtet sein. API 36 (Android 16) ist die aktuelle Plattformversion mit wichtigen Verhaltensänderungen.

**Was ändert sich durch die Edge-to-Edge-Pflicht in Android 16?**
`windowOptOutEdgeToEdgeEnforcement` ist ab API 36 deaktiviert. AllerPaw verwendet bereits `enableEdgeToEdge()` in `MainActivity` und `Scaffold` mit `innerPadding` → kein Breaking Change.

**Was ist die Predictive Back Gesture und was wurde geändert?**
Ab API 36 sind System-Animationen für die Zurück-Geste standardmäßig aktiv. `onBackPressed()` wird nicht mehr aufgerufen. AllerPaw setzt `android:enableOnBackInvokedCallback="true"` im Manifest. Navigation Compose verwaltet den Back-Stack über `NavController` korrekt → vollständig kompatibel.

**Was bedeutet Local Network Protection (LNP)?**
Ab Android 16 schützt Android den Zugriff aufs lokale Netzwerk. AllerPaw kommuniziert ausschließlich über Internet (BrightSky, Open-Meteo, Google Sheets) — kein LAN-Zugriff. `NEARBY_WIFI_DEVICES` wurde vorsorglich deklariert; Laufzeit-Request kommt in v0.11.0 wenn LNP erzwungen wird.

**Muss ich wegen `scheduleAtFixedRate`-Änderungen etwas tun?**
Nein. AllerPaw verwendet `scheduleAtFixedRate` nicht direkt. WorkManager ist davon nicht betroffen.
