# CLAUDE.md – AllerPaw Android

Behavioral guidelines für alle Code-Sessions in diesem Projekt.

**Tradeoff:** Diese Richtlinien bevorzugen Sorgfalt gegenüber Geschwindigkeit. Bei trivialen Aufgaben nach eigenem Ermessen handeln.

### Lies alle md dateien im Info Ordner

## 1. Vor dem Coden nachdenken

**Nicht raten. Unklarheiten benennen. Tradeoffs aufzeigen.**

Vor der Implementierung:
- Annahmen explizit benennen. Bei Unsicherheit nachfragen.
- Wenn mehrere Interpretationen möglich sind, alle nennen — nicht still eine wählen.
- Wenn ein einfacherer Ansatz existiert, diesen nennen und begründen.
- Wenn etwas unklar ist: stoppen, benennen, fragen.

## 2. Einfachheit zuerst

**Minimaler Code, der das Problem löst. Nichts Spekulatives.**

- Keine Features über das Verlangte hinaus.
- Keine Abstraktionen für einmalig genutzten Code.
- Keine „Flexibilität", die nicht verlangt wurde.
- Keine Fehlerbehandlung für unmögliche Szenarien.

Frage: „Würde ein erfahrener Android-Entwickler das als überkomplex einschätzen?" — Wenn ja: vereinfachen.

## 3. Chirurgische Änderungen

**Nur das Notwendige anfassen.**

- Keinen angrenzenden Code „verbessern".
- Kein Refactoring nicht-defekter Stellen.
- Vorhandenen Stil beibehalten.
- Ungenutzten Code durch eigene Änderungen bereinigen; vorhandenen toten Code nur auf Anfrage entfernen.

## 4. Zielorientierte Ausführung

**Erfolgskriterien definieren. Verifizieren.**

Aufgaben in prüfbare Ziele übersetzen:
- „Validierung hinzufügen" → „Tests für ungültige Inputs schreiben, dann grün machen"
- „Bug fixen" → „Test schreiben der den Bug reproduziert, dann grün machen"

Bei Mehrschritt-Aufgaben kurzen Plan benennen:
```
1. [Schritt] → verify: [Prüfung]
2. [Schritt] → verify: [Prüfung]
```

---

## 5. ✅ Pflicht nach jeder Änderung

### App-Info aktualisieren

In `build.gradle.kts`:
- `versionName` erhöhen
- `versionCode` inkrementieren

In der About-/Einstellungs-Ansicht (Compose):
- Versionsnummer aktualisieren
- Neue Hauptfeatures knapp ergänzen (max. 5 Zeilen)

### PROJECT.md aktualisieren

- Versionsnummer erhöhen
- Datum auf aktuelles Datum setzen
- Modulstruktur anpassen falls neue Dateien hinzukommen
- Implementierungsstand: neue Version als Block `**vX.X:**` mit Bullet-Points ergänzen
- „Wichtige Hinweise für neue Prompts" aktualisieren

### FEATURE.md aktualisieren

- Neue Features in der passenden Kategorie ergänzen
- Geänderte Features anpassen (Label-Änderungen, Verhalten)
- Vor dem Entfernen eines Features nachfragen
- Features möglichst erhalten, wenn nicht explizit gestrichen

### FAQ.md aktualisieren

- Neue FAQs für neue Features hinzufügen
- Bestehende Antworten anpassen wenn sich Verhalten ändert
- Neue Fehlerfälle oder typische Nutzer-Fragen ergänzen

### Sheet-Änderungen melden

Wenn neue Spalten, Sheets oder Spalten-Reihenfolgen geändert werden → explizit im Chat mitteilen mit genauer Anleitung, was in Google Sheets manuell geändert werden muss.

### MIGRATION.md aktualisieren

- Abgehakte Punkte (`[x]`) bei abgeschlossenen Schritten setzen
- Neue Risiken oder Erkenntnisse in der Risikotabelle ergänzen

### Änderungsübersicht auf Englisch im Chat

Nach jeder Änderungs-Session eine kompakte Übersicht posten:

```
## Changes in vX.X
**Modified:** [file] – [what changed]
**Added:** [file/feature] – [description]
**Removed:** [feature] – [reason]
**Sheet changes required:** [yes/no + details]
**Migration checklist:** [items checked off]
```

### VALIDATION.md aktualisieren

- Alte Validierungen nicht löschen
- Prüfen ob neue Testszenarien erforderlich sind

---

## Android-spezifische Regeln

- **Kein `runBlocking` im Main Thread** — nur `viewModelScope.launch` oder `lifecycleScope`
- **FloatParser.kt verwenden** für alle Dezimalzahlen aus Sheets (Komma-Dezimaltrenner!)
- **Soft-Delete immer** — nie Zeilen in Sheets löschen; `deleted = TRUE` setzen
- **Neue Spalten ans Ende** — Spaltenindex-basiertes Lesen darf nicht brechen
- **Room-Cache TTL** prüfen bevor API-Call — kein unnötiger Netzwerkverkehr
- **Compose State heben** — kein lokaler State in tiefen Composables wenn ViewModel-State ausreicht
- **Hilt für DI** — kein manuelles Service Locator Pattern
