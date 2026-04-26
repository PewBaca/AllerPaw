package com.allerpaw.app.util

/**
 * Parst Dezimalzahlen mit Komma (DE) oder Punkt (EN) als Trennzeichen.
 * Entspricht _float() aus der Web-App (rechner.js).
 */
object FloatParser {

    fun parse(input: String): Double? {
        if (input.isBlank()) return null
        // Tausendertrennzeichen entfernen, dann Komma → Punkt
        val normalized = input.trim()
            .replace(" ", "")
            .replace(".", "")   // Tausenderpunkt entfernen (DE: 1.000,5)
            .replace(",", ".")  // Komma → Punkt
        return normalized.toDoubleOrNull()
            ?: input.trim().replace(",", ".").toDoubleOrNull() // Fallback
    }

    fun format(value: Double, decimals: Int = 2): String {
        return "%.${decimals}f".format(value).replace(".", ",")
    }

    fun formatOrEmpty(value: Double?, decimals: Int = 2): String {
        return if (value == null || value == 0.0) "" else format(value, decimals)
    }
}
