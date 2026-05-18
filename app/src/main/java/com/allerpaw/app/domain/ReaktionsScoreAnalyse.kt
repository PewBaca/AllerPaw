package com.allerpaw.app.domain

import com.allerpaw.app.data.local.entity.TagebuchFutterEntity
import com.allerpaw.app.data.local.entity.TagebuchSymptomEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Reaktionsscore-Analyse nach dem 48h-Fenster-Prinzip.
 *
 * Methode (basiert auf Eliminationsdiät-Protokoll):
 *   1. Futter-Einträge mit `erstgabe = true` oder `provokation = true` identifizieren
 *   2. Für jeden Eintrag: Symptome in einem 48h-Fenster nach dem Einführungstag sammeln
 *   3. Wenn mind. [MIN_BEOBACHTUNGEN] Datenpunkte vorhanden: Score berechnen
 *      Score = Durchschnittsschweregrad × Häufigkeitsfaktor (0–5)
 *   4. Ergebnisse nach Score sortieren (höchstes Risiko zuerst)
 *
 * Wichtig: Ohne Futter-Item-Entity kann keine Zutat-Zuordnung erfolgen —
 * der Score wird dann dem Futter-Eintrag-Freitext / produkt-Feld zugeordnet.
 */
object ReaktionsScoreAnalyse {

    const val MIN_BEOBACHTUNGEN = 2   // mind. 2 Symptom-Ereignisse für validen Score
    const val FENSTER_STUNDEN   = 48  // Beobachtungsfenster in Stunden

    data class ScoreEintrag(
        val zutatOderRezept: String,  // Name der Zutat/des Produkts
        val eintraege: Int,           // Anzahl Erstgaben/Provokationen
        val symptomEreignisse: Int,   // Anzahl Symptome im 48h-Fenster
        val score: Double,            // 0–5 (Schweregrad × Häufigkeit)
        val durchschnittSchweregrad: Double,
        val haeufigkeit: Double,      // Symptome pro Erstgabe (0–1+)
        val istSignifikant: Boolean,  // score > Schwelle
        val beispielDaten: List<LocalDate>  // Erstgabe-Daten für Timeline
    )

    /**
     * Analysiert Futter-Einträge und Symptome im 48h-Fenster.
     *
     * @param futterEintraege  Alle Futter-Einträge des Hundes im Zeitraum
     * @param symptome         Alle Symptome des Hundes im Zeitraum
     * @param minBeobachtungen Minimale Datenpunkte für validen Score
     */
    fun analysiere(
        futterEintraege: List<TagebuchFutterEntity>,
        symptome: List<TagebuchSymptomEntity>,
        minBeobachtungen: Int = MIN_BEOBACHTUNGEN
    ): List<ScoreEintrag> {

        // Nur Erstgaben + Provokationen betrachten
        val relevanteEintraege = futterEintraege.filter { it.erstgabe || it.provokation }
        if (relevanteEintraege.isEmpty()) return emptyList()

        // Gruppieren nach Produkt/Freitext (Name des Lebensmittels)
        val nachProdukt = relevanteEintraege.groupBy { e ->
            e.produkt.ifBlank { e.freitextErgaenzung.ifBlank { "Unbekannt" } }.trim()
        }

        return nachProdukt.mapNotNull { (produkt, eintraege) ->
            val datumSet = eintraege.map { it.datum }.toSet()

            // Symptome in 48h-Fenster nach jedem Einführungstag sammeln
            val symptomeImFenster = symptome.filter { symptom ->
                datumSet.any { tag ->
                    val diff = ChronoUnit.DAYS.between(tag, symptom.datum)
                    diff in 0..2  // 0 = gleicher Tag, 1 = nächster Tag, 2 = übernächster Tag
                }
            }

            val anzahlSymptomEreignisse = symptomeImFenster.size

            // Zu wenig Datenpunkte → überspringen
            if (eintraege.size < minBeobachtungen && anzahlSymptomEreignisse < minBeobachtungen) {
                return@mapNotNull null
            }

            val durchschnitt = if (symptomeImFenster.isNotEmpty())
                symptomeImFenster.map { it.schweregrad.toDouble() }.average()
            else 0.0

            // Häufigkeit: Anteil der Erstgaben mit mindestens einem Symptom
            val eintragenMitSymptom = eintraege.count { e ->
                symptome.any { s ->
                    val diff = ChronoUnit.DAYS.between(e.datum, s.datum)
                    diff in 0..2
                }
            }
            val haeufigkeit = eintragenMitSymptom.toDouble() / eintraege.size.coerceAtLeast(1)

            // Score: Schweregrad × Häufigkeit × 5 (normalisiert auf 0–5)
            val score = (durchschnitt * haeufigkeit).coerceIn(0.0, 5.0)

            ScoreEintrag(
                zutatOderRezept        = produkt,
                eintraege              = eintraege.size,
                symptomEreignisse      = anzahlSymptomEreignisse,
                score                  = score,
                durchschnittSchweregrad = durchschnitt,
                haeufigkeit            = haeufigkeit,
                istSignifikant         = score >= 1.5 && anzahlSymptomEreignisse >= minBeobachtungen,
                beispielDaten          = eintraege.map { it.datum }.sortedDescending().take(3)
            )
        }.sortedByDescending { it.score }
    }

    /** Risikostufe als Label */
    fun risikoLabel(score: Double): String = when {
        score >= 3.5 -> "Sehr hoch"
        score >= 2.5 -> "Hoch"
        score >= 1.5 -> "Mittel"
        score >= 0.5 -> "Niedrig"
        else         -> "Kein Signal"
    }
}
