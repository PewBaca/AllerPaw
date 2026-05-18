package com.allerpaw.app.ui.rezept

/**
 * Hierarchische Rezept-Kategorien für AllerPaw.
 *
 * Aufbau: Hauptkategorie → Unterkategorien
 * Nutzer kann eigene Kategorien frei eingeben — diese Liste ist die Vorschlagsliste.
 *
 * Format in DB: "Fleisch/Rind" | "Gemüse/Wurzelgemüse" | "Supplement/Vitamin"
 */
object RezeptKategorien {

    data class Unterkategorie(val name: String, val emoji: String = "")

    data class Hauptkategorie(
        val name: String,
        val emoji: String,
        val unterkategorien: List<Unterkategorie>
    )

    val alle: List<Hauptkategorie> = listOf(

        Hauptkategorie("Fleisch", "🥩", listOf(
            Unterkategorie("Rind",       "🐄"),
            Unterkategorie("Huhn",       "🐔"),
            Unterkategorie("Pute",       "🦃"),
            Unterkategorie("Lamm",       "🐑"),
            Unterkategorie("Schwein",    "🐷"),
            Unterkategorie("Pferd",      "🐴"),
            Unterkategorie("Wild",       "🦌"),
            Unterkategorie("Kaninchen",  "🐰"),
            Unterkategorie("Ente",       "🦆"),
            Unterkategorie("Innereien",  "🫀"),
            Unterkategorie("Knochen",    "🦴"),
        )),

        Hauptkategorie("Fisch & Meeresfrüchte", "🐟", listOf(
            Unterkategorie("Lachs",      "🐟"),
            Unterkategorie("Hering",     "🐟"),
            Unterkategorie("Makrele",    "🐟"),
            Unterkategorie("Thunfisch",  "🐟"),
            Unterkategorie("Sardine",    "🐟"),
            Unterkategorie("Garnelen",   "🦐"),
            Unterkategorie("Muscheln",   "🦪"),
        )),

        Hauptkategorie("Gemüse", "🥦", listOf(
            Unterkategorie("Wurzelgemüse",  "🥕"),
            Unterkategorie("Blattgemüse",   "🥬"),
            Unterkategorie("Kreuzblütler",  "🥦"),
            Unterkategorie("Kürbisgewächse","🎃"),
            Unterkategorie("Hülsenfrüchte", "🫘"),
            Unterkategorie("Zucchini",      "🥒"),
            Unterkategorie("Spinat",        "🌿"),
            Unterkategorie("Karotte",       "🥕"),
        )),

        Hauptkategorie("Obst", "🍎", listOf(
            Unterkategorie("Beeren",     "🫐"),
            Unterkategorie("Äpfel",      "🍎"),
            Unterkategorie("Banane",     "🍌"),
            Unterkategorie("Melone",     "🍉"),
        )),

        Hauptkategorie("Eier & Milchprodukte", "🥚", listOf(
            Unterkategorie("Eier",       "🥚"),
            Unterkategorie("Quark",      "🫙"),
            Unterkategorie("Joghurt",    "🥛"),
            Unterkategorie("Käse",       "🧀"),
        )),

        Hauptkategorie("Öle & Fette", "🫙", listOf(
            Unterkategorie("Fischöl",        "🐟"),
            Unterkategorie("Leinöl",         "🌿"),
            Unterkategorie("Hanföl",         "🌿"),
            Unterkategorie("Kokosöl",        "🥥"),
            Unterkategorie("Schwarzkümmelöl","🌿"),
        )),

        Hauptkategorie("Supplemente", "💊", listOf(
            Unterkategorie("Vitamine",       "💊"),
            Unterkategorie("Mineralien",     "💊"),
            Unterkategorie("Spurenelemente", "💊"),
            Unterkategorie("Probiotika",     "🦠"),
            Unterkategorie("Omega-3",        "🐟"),
            Unterkategorie("Kräuter",        "🌿"),
            Unterkategorie("Algen",          "🌊"),
        )),

        Hauptkategorie("Sonstiges", "📦", listOf(
            Unterkategorie("Getreide",   "🌾"),
            Unterkategorie("Samen",      "🌱"),
            Unterkategorie("Mischung",   "🥣"),
        ))
    )

    /** Flache Liste aller Hauptkategorien */
    val hauptkategorien: List<String> get() = alle.map { it.name }

    /** Gibt Unterkategorien für eine Hauptkategorie zurück */
    fun unterkategorienFuer(hauptkategorie: String): List<Unterkategorie> =
        alle.find { it.name == hauptkategorie }?.unterkategorien ?: emptyList()

    /** Vollständige Kategorie aus Haupt + Unter: "Fleisch/Rind" */
    fun vollstaendig(haupt: String, unter: String): String =
        if (unter.isBlank()) haupt else "$haupt/$unter"

    /** Extrahiert Hauptkategorie aus "Fleisch/Rind" → "Fleisch" */
    fun hauptkategorie(vollstaendig: String): String =
        vollstaendig.substringBefore("/")

    /** Extrahiert Unterkategorie aus "Fleisch/Rind" → "Rind" */
    fun unterkategorie(vollstaendig: String): String =
        if ("/" in vollstaendig) vollstaendig.substringAfter("/") else ""

    /** Emoji für Hauptkategorie */
    fun emoji(hauptkategorie: String): String =
        alle.find { it.name == hauptkategorie }?.emoji ?: "📦"
}
