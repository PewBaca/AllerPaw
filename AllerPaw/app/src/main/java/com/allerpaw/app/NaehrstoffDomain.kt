package com.allerpaw.app.domain

import kotlin.math.pow

// ─────────────────────────────────────────────────────────────────────────────
// NRC-Nährstoff-Katalog
// ─────────────────────────────────────────────────────────────────────────────

data class Naehrstoff(
    val key: String,
    val label: String,
    val einheit: String,        // "g" | "mg" | "µg" | "kcal"
    val gruppe: String,         // "Makro" | "Mineral" | "Vitamin" | "Fettsäure"
    /** Bedarfswert je 1000 kcal ME (NRC 2006) */
    val bedarfPro1000kcal: Double,
    /** Maximaler sicherer Wert je 1000 kcal (null = kein bekannter UL) */
    val maxPro1000kcal: Double? = null
)

object NaehrstoffKatalog {

    val alle: List<Naehrstoff> = listOf(
        // ── Makronährstoffe ──────────────────────────────────────────────────
        Naehrstoff("protein",       "Protein",            "g",  "Makro",   45.0),
        Naehrstoff("fett",          "Fett",               "g",  "Makro",   13.75),
        Naehrstoff("linolsaeure",   "Linolsäure (LA)",    "g",  "Fettsäure", 2.8),
        Naehrstoff("alpha_linolen", "Alpha-Linolensäure (ALA)", "g", "Fettsäure", 0.11),
        Naehrstoff("epa_dha",       "EPA + DHA",          "mg", "Fettsäure", 130.0, 2800.0),

        // ── Mineralstoffe ────────────────────────────────────────────────────
        Naehrstoff("calcium",       "Calcium",            "g",  "Mineral", 1.0,   4.5),
        Naehrstoff("phosphor",      "Phosphor",           "g",  "Mineral", 0.75,  4.0),
        Naehrstoff("kalium",        "Kalium",             "g",  "Mineral", 1.0),
        Naehrstoff("natrium",       "Natrium",            "g",  "Mineral", 0.2),
        Naehrstoff("chlorid",       "Chlorid",            "g",  "Mineral", 0.3),
        Naehrstoff("magnesium",     "Magnesium",          "mg", "Mineral", 150.0),
        Naehrstoff("eisen",         "Eisen",              "mg", "Mineral", 7.5,   250.0),
        Naehrstoff("zink",          "Zink",               "mg", "Mineral", 15.0,  250.0),
        Naehrstoff("kupfer",        "Kupfer",             "mg", "Mineral", 1.5,   250.0),
        Naehrstoff("mangan",        "Mangan",             "mg", "Mineral", 1.2,   50.0),
        Naehrstoff("selen",         "Selen",              "µg", "Mineral", 87.5,  500.0),
        Naehrstoff("jod",           "Jod",                "µg", "Mineral", 218.0, 2190.0),

        // ── Vitamine ─────────────────────────────────────────────────────────
        Naehrstoff("vitamin_a",     "Vitamin A",          "µg", "Vitamin", 379.0, 15909.0),
        Naehrstoff("vitamin_d",     "Vitamin D",          "µg", "Vitamin", 3.4,   56.8),
        Naehrstoff("vitamin_e",     "Vitamin E",          "mg", "Vitamin", 7.5,   250.0),
        Naehrstoff("vitamin_k",     "Vitamin K",          "µg", "Vitamin", 0.45),
        Naehrstoff("vitamin_b1",    "Thiamin (B1)",       "mg", "Vitamin", 0.56),
        Naehrstoff("vitamin_b2",    "Riboflavin (B2)",    "mg", "Vitamin", 1.3),
        Naehrstoff("vitamin_b3",    "Niacin (B3)",        "mg", "Vitamin", 4.25),
        Naehrstoff("vitamin_b5",    "Pantothensäure (B5)","mg", "Vitamin", 3.0),
        Naehrstoff("vitamin_b6",    "Pyridoxin (B6)",     "mg", "Vitamin", 0.375),
        Naehrstoff("vitamin_b9",    "Folat (B9)",         "µg", "Vitamin", 68.0),
        Naehrstoff("vitamin_b12",   "Cobalamin (B12)",    "µg", "Vitamin", 8.75),
        Naehrstoff("biotin",        "Biotin",             "µg", "Vitamin", 19.0),
        Naehrstoff("cholin",        "Cholin",             "mg", "Vitamin", 425.0),
    )

    val byKey: Map<String, Naehrstoff> = alle.associateBy { it.key }
}

// ─────────────────────────────────────────────────────────────────────────────
// Energieberechnung (RER / MER)
// ─────────────────────────────────────────────────────────────────────────────

object EnergieBedarf {

    /**
     * Resting Energy Requirement (NRC 2006)
     *   RER = 70 × (Gewicht_kg ^ 0.75)  [kcal/Tag]
     */
    fun rer(gewichtKg: Double): Double = 70.0 * gewichtKg.pow(0.75)

    /**
     * Maintenance Energy Requirement = RER × Aktivitätsfaktor
     * faktor: 1.0 = inaktiv / Kastrat ; 1.6 = normal ; 2.5 = Arbeitshund
     */
    fun mer(gewichtKg: Double, faktor: Double = 1.6): Double = rer(gewichtKg) * faktor

    /** Bedarfswert eines Nährstoffs (g/mg/µg) bei gegebener ME-Menge */
    fun naehrstoffBedarf(naehrstoff: Naehrstoff, kcalME: Double): Double =
        naehrstoff.bedarfPro1000kcal * kcalME / 1000.0
}

// ─────────────────────────────────────────────────────────────────────────────
// Naehrstoff-Analyse-Ergebnis
// ─────────────────────────────────────────────────────────────────────────────

data class NaehrstoffErgebnis(
    val naehrstoff: Naehrstoff,
    val istWert: Double,
    val bedarfswert: Double,    // Empfehlung
    val maxWert: Double?,       // UL
    val prozent: Double         // istWert / bedarfswert * 100
) {
    val status: Status get() = when {
        maxWert != null && istWert > maxWert -> Status.UEBERSCHRITTEN
        prozent < 80.0  -> Status.MANGEL
        prozent > 150.0 -> Status.UEBERSCHUSS
        else            -> Status.OK
    }

    enum class Status { OK, MANGEL, UEBERSCHUSS, UEBERSCHRITTEN }
}

// ─────────────────────────────────────────────────────────────────────────────
// RezeptAnalyse – Domain use case
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Berechnet den Nährstoff-Ist-Wert für eine Liste von (zutatId → mengeG, nährstoffe)
 * und vergleicht mit dem Bedarf bei [kcalME].
 */
class RezeptAnalyseUseCase {

    data class ZutatInput(
        val mengeG: Double,
        val naehrstoffe: Map<String, Double>   // key → wert per 100g
    )

    fun analyse(
        zutaten: List<ZutatInput>,
        kcalME: Double
    ): List<NaehrstoffErgebnis> {
        val gesamtWerte = mutableMapOf<String, Double>()
        for (z in zutaten) {
            val faktor = z.mengeG / 100.0
            for ((key, wertPer100g) in z.naehrstoffe) {
                gesamtWerte[key] = (gesamtWerte[key] ?: 0.0) + wertPer100g * faktor
            }
        }
        return NaehrstoffKatalog.alle.map { naehrstoff ->
            val ist     = gesamtWerte[naehrstoff.key] ?: 0.0
            val bedarf  = EnergieBedarf.naehrstoffBedarf(naehrstoff, kcalME)
            val prozent = if (bedarf > 0) ist / bedarf * 100.0 else 0.0
            NaehrstoffErgebnis(
                naehrstoff  = naehrstoff,
                istWert     = ist,
                bedarfswert = bedarf,
                maxWert     = naehrstoff.maxPro1000kcal?.let { EnergieBedarf.naehrstoffBedarf(
                    naehrstoff.copy(bedarfPro1000kcal = it), kcalME) },
                prozent     = prozent
            )
        }
    }
}
