package com.allernutri.app.data.remote

/**
 * Automatisches Mapping von Google Sheets Kopfzeilen zu AllerPaw-Feldern.
 *
 * Strategie:
 * 1. Exakter Match (case-insensitive)
 * 2. Enthält-Match (z.B. "Datum der Symptome" → "datum")
 * 3. Kein Match → Nutzer wählt manuell im SheetsImportScreen
 */
object SheetsColumnMapper {

    /** Bekannte Feldnamen je Tabellen-Typ */
    val SYMPTOM_FIELDS = listOf(
        ColumnDef("datum",        listOf("datum", "date", "tag", "day", "zeitpunkt")),
        ColumnDef("kategorie",    listOf("kategorie", "category", "typ", "type", "art")),
        ColumnDef("schweregrad",  listOf("schweregrad", "severity", "intensität", "stärke", "grad")),
        ColumnDef("koerperstelle",listOf("körperstelle", "koerperstelle", "body", "bereich", "stelle")),
        ColumnDef("beschreibung", listOf("beschreibung", "description", "details", "text", "anmerkung")),
        ColumnDef("notizen",      listOf("notizen", "notes", "kommentar", "comment"))
    )

    val FUTTER_FIELDS = listOf(
        ColumnDef("datum",        listOf("datum", "date", "tag")),
        ColumnDef("rezept",       listOf("rezept", "recipe", "futter", "food", "mahlzeit")),
        ColumnDef("menge",        listOf("menge", "amount", "gramm", "gram", "g")),
        ColumnDef("erstgabe",     listOf("erstgabe", "first", "neu", "new")),
        ColumnDef("notizen",      listOf("notizen", "notes", "kommentar"))
    )

    val UMWELT_FIELDS = listOf(
        ColumnDef("datum",         listOf("datum", "date", "tag")),
        ColumnDef("tempMinC",      listOf("temp_min", "min_temp", "temperatur_min", "temperature_min")),
        ColumnDef("tempMaxC",      listOf("temp_max", "max_temp", "temperatur_max", "temperature_max")),
        ColumnDef("luftfeuchte",   listOf("luftfeuchte", "humidity", "feuchte", "feuchtigkeit")),
        ColumnDef("niederschlag",  listOf("niederschlag", "precipitation", "regen", "rain")),
        ColumnDef("pollen",        listOf("pollen", "pollenbelastung", "allergen")),
        ColumnDef("notizen",       listOf("notizen", "notes"))
    )

    val ALLERGEN_FIELDS = listOf(
        ColumnDef("allergen",          listOf("allergen", "stoff", "substanz", "auslöser")),
        ColumnDef("reaktionsstaerke",  listOf("stärke", "reaktion", "severity", "grad")),
        ColumnDef("kategorie",         listOf("kategorie", "category", "typ")),
        ColumnDef("symptome",          listOf("symptome", "symptoms", "reaktionen")),
        ColumnDef("notizen",           listOf("notizen", "notes"))
    )

    /**
     * Mappt eine Liste von Kopfzeilen auf bekannte Felder.
     *
     * @param headers  Kopfzeilen aus dem Google Sheet
     * @param fields   Bekannte Felder (z.B. SYMPTOM_FIELDS)
     * @return Map von Spalten-Index → Feldname (nur gefundene Matches)
     */
    fun autoMap(
        headers: List<String>,
        fields: List<ColumnDef>
    ): Map<Int, String> {
        val result = mutableMapOf<Int, String>()

        headers.forEachIndexed { index, header ->
            val normalized = header.trim().lowercase()
                .replace(" ", "_")
                .replace("-", "_")
                .replace("ä", "a").replace("ö", "o")
                .replace("ü", "u").replace("ß", "ss")

            for (field in fields) {
                val matched = field.aliases.any { alias ->
                    normalized == alias ||
                    normalized.contains(alias) ||
                    alias.contains(normalized)
                }
                if (matched && !result.values.contains(field.key)) {
                    result[index] = field.key
                    break
                }
            }
        }
        return result
    }

    /**
     * Gibt alle Felder zurück die noch nicht gemappt wurden.
     * → Diese müssen manuell im UI zugewiesen werden.
     */
    fun unmappedFields(
        fields: List<ColumnDef>,
        mapping: Map<Int, String>
    ): List<ColumnDef> {
        val mappedKeys = mapping.values.toSet()
        return fields.filter { it.key !in mappedKeys }
    }
}

data class ColumnDef(
    val key: String,        // Interner Feldname
    val aliases: List<String>   // Mögliche Kopfzeilen-Bezeichnungen
) {
    val label: String get() = when (key) {
        "datum"           -> "Datum"
        "kategorie"       -> "Kategorie"
        "schweregrad"     -> "Schweregrad"
        "koerperstelle"   -> "Körperstelle"
        "beschreibung"    -> "Beschreibung"
        "notizen"         -> "Notizen"
        "rezept"          -> "Rezept"
        "menge"           -> "Menge (g)"
        "erstgabe"        -> "Erstgabe"
        "tempMinC"        -> "Temp. Min (°C)"
        "tempMaxC"        -> "Temp. Max (°C)"
        "luftfeuchte"     -> "Luftfeuchte (%)"
        "niederschlag"    -> "Niederschlag (mm)"
        "pollen"          -> "Pollen"
        "allergen"        -> "Allergen"
        "reaktionsstaerke"-> "Reaktionsstärke"
        "symptome"        -> "Symptome"
        else              -> key
    }
}
