package com.allerpaw.app.domain

/**
 * NRC 2006 Bedarfswerte für verschiedene Lebensphasen.
 * Alle Werte je 1000 kcal ME.
 *
 * Quelle: National Research Council (2006). Nutrient Requirements of Dogs and Cats.
 */
object NrcLebensphasen {

    enum class Lebensphase(val label: String) {
        ADULT("Erwachsen"),
        WELPE("Welpe (< 1 Jahr)"),
        SENIOR("Senior (> 7 Jahre)"),
        TRÄCHTIG("Trächtig"),
        LAKTIEREND("Laktierend")
    }

    /**
     * Bedarfsfaktoren relativ zum Adult-Bedarf.
     * Basierend auf NRC 2006 Tabellen 15-1 bis 15-5.
     */
    private val faktoren: Map<Lebensphase, Map<String, Double>> = mapOf(

        Lebensphase.ADULT to emptyMap(), // 1.0× = Standard im NaehrstoffKatalog

        Lebensphase.WELPE to mapOf(
            "protein"       to 1.56,  // 25g vs 45g pro 1000kcal (Welpen: min 56.3g)
            "calcium"       to 3.0,   // 3.0g vs 1.0g
            "phosphor"      to 2.5,   // 2.5g vs 1.0g
            "vitamin_a"     to 0.84,  // etwas weniger als Adult
            "vitamin_d"     to 1.47,  // 5.0µg vs 3.4µg
            "eisen"         to 1.87,  // 14mg vs 7.5mg
            "zink"          to 1.47,  // 22mg vs 15mg
            "kupfer"        to 1.53,  // 2.3mg vs 1.5mg
            "jod"           to 0.64,  // 140µg vs 218µg
            "linolsaeure"   to 1.07   // 3.0g vs 2.8g
        ),

        Lebensphase.SENIOR to mapOf(
            "protein"       to 1.11,  // etwas mehr Protein für Muskelerhalt
            "phosphor"      to 0.67,  // weniger wegen Nierengesundheit
            "natrium"       to 0.75,  // weniger wegen Blutdruck
            "vitamin_e"     to 1.33,  // mehr Antioxidantien
            "vitamin_c"     to 1.5    // mehr Antioxidantien (nicht im NRC Pflicht)
        ),

        Lebensphase.TRÄCHTIG to mapOf(
            "protein"       to 1.44,
            "calcium"       to 2.6,
            "phosphor"      to 2.1,
            "vitamin_d"     to 1.47,
            "eisen"         to 2.27,
            "zink"          to 1.6,
            "linolsaeure"   to 1.25
        ),

        Lebensphase.LAKTIEREND to mapOf(
            "protein"       to 2.0,
            "calcium"       to 4.8,
            "phosphor"      to 3.9,
            "eisen"         to 2.67,
            "zink"          to 2.47,
            "linolsaeure"   to 2.14
        )
    )

    /**
     * Gibt den skalierten Bedarfswert je 1000 kcal zurück.
     * Falls kein spezifischer Faktor vorhanden → Adult-Wert (Faktor 1.0).
     */
    fun bedarfPro1000kcal(
        naehrstoff: Naehrstoff,
        lebensphase: Lebensphase
    ): Double {
        val faktor = faktoren[lebensphase]?.get(naehrstoff.key) ?: 1.0
        return naehrstoff.bedarfPro1000kcal * faktor
    }

    /**
     * Gibt den maximal sicheren Wert (UL) zurück — unverändert über alle Phasen,
     * da NRC 2006 hier keine lebensphasen-spezifischen Werte ausweist.
     */
    fun maxPro1000kcal(naehrstoff: Naehrstoff): Double? =
        naehrstoff.maxPro1000kcal
}
