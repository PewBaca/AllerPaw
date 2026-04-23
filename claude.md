# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

## Instruktionen für Claude bei jedem Code-Update

> Diese Regeln gelten für **jede** Session in der Code-Änderungen vorgenommen werden.

## 5. ✅ Pflicht nach jeder Änderung

1. **App-Info in `index.html` aktualisieren:**
   - Versionsnummer im HTML-Header-Kommentar (Zeile ~5: `Version X.Y.Z | ES Modules | …`)
   - Versionsnummer + Feature-Zeilen in der `ℹ️ App-Info`-Box im Einstellungen-Panel
   - Neue Hauptfeatures knapp in der App-Info-Box ergänzen (max. 5 Zeilen)

2. **PROJECT.md aktualisieren:**
   - Versionsnummer erhöhen 
   - Project.md immer aktualisieren
   - Datum auf aktuelles Datum setzen
   - Modul-Beschreibung in der Dateistruktur anpassen
   - UI-Struktur aktualisieren falls sich Panels/Tabs ändern
   - Implementierungsstand: neue Version als Block `**vX.X:**` mit Bullet-Points hinzufügen
   - „Wichtige Hinweise für neue Prompts" aktualisieren
    

2. **FEATURE.md aktualisieren:**
   - Neue Features in der passenden Kategorie ergänzen
   - Feature.md aktualiseren
   - Geänderte Features anpassen (z.B. Label-Änderungen, Verhalten)
   - Frage nach bevor Entfernte Features aus der Liste streichen
   - Versuche immer features zu erhalten wenn diese nicht explizit gestrichen wurden

3. **FAQ.md aktualisieren:**
   - Neue FAQs für neue Features hinzufügen
   - Antworten auf bestehende Fragen anpassen wenn sich Verhalten ändert
   - Neue Fehlerfälle oder typische Nutzer-Fragen ergänzen

4. **Sheet-Änderungen melden:**
   - Wenn neue Spalten, Sheets oder Spalten-Reihenfolgen geändert werden → explizit im Chat mitteilen mit genauer Anleitung was in Google Sheets manuell geändert werden muss

5. **Änderungsübersicht auf Englisch im Chat:**
   - Nach jeder Änderungssession eine kompakte Übersicht **auf Englisch** posten:
   ```
   ## Changes in vX.X
   **Modified:** [file] – [what changed]
   **Added:** [file/feature] – [description]
   **Removed:** [feature] – [reason]
   **Sheet changes required:** [yes/no + details]
   ```

 6. **Validation.md aktualisieren:**
  **Alte Validierungen nicht löschen
  **prüfen ob neue Validierungen und Evaluierungen erforderlich sind


