package com.allernutri.app.data.repository

import android.content.Context
import com.allernutri.app.data.local.entity.*
import com.allernutri.app.data.remote.SheetsApiClient
import com.allernutri.app.data.remote.SheetsColumnMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Sheets v4 Export + Import.
 *
 * Authentifizierung: Google OAuth2 via AuthRepository (JWT aus Credential Manager).
 * API-Basis: https://sheets.googleapis.com/v4/spreadsheets
 *
 * Sheet-Struktur:
 * - Zeile 1: Deutsche Anzeige-Header
 * - Zeile 2: API-Schlüssel (intern, für Import-Mapping)
 * - Daten ab Zeile 3
 *
 * Unterstützte Tabs:
 *   Symptome | Futter | Umwelt | Allergene | Phasen | Medikamente
 */
@Singleton
class SheetsRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val session: SessionRepository,
    private val tagebuchRepo: TagebuchRepository
) {
    private val datumFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val baseUrl  = "https://sheets.googleapis.com/v4/spreadsheets"

    // ── Sheet-Struktur Definitionen ───────────────────────────────────────

    private val SYMPTOM_HEADERS_DE  = listOf(
        "Datum", "Kategorie", "Schweregrad", "Körperstelle", "Beschreibung", "Notizen"
    )
    private val SYMPTOM_HEADERS_API = listOf(
        "datum", "kategorie", "schweregrad", "koerperstelle", "beschreibung", "notizen"
    )

    private val UMWELT_HEADERS_DE  = listOf(
        "Datum", "Temp Min (°C)", "Temp Max (°C)", "Luftfeuchte (%)",
        "Niederschlag (mm)", "Raumtemp (°C)", "Bett", "Notizen"
    )
    private val UMWELT_HEADERS_API = listOf(
        "datum", "tempMinC", "tempMaxC", "luftfeuchte",
        "niederschlagMm", "raumtempC", "bett", "notizen"
    )

    private val ALLERGEN_HEADERS_DE  = listOf(
        "Allergen", "Kategorie", "Reaktionsstärke", "Symptome", "Notizen"
    )
    private val ALLERGEN_HEADERS_API = listOf(
        "allergen", "kategorie", "reaktionsstaerke", "symptome", "notizen"
    )

    private val PHASEN_HEADERS_DE  = listOf(
        "Typ", "Von", "Bis", "Beschreibung"
    )
    private val PHASEN_HEADERS_API = listOf(
        "typ", "vonDatum", "bisDatum", "beschreibung"
    )

    private val FUTTER_HEADERS_DE  = listOf(
        "Datum", "Rezept", "Erstgabe", "Provokation", "Reaktion", "Notizen"
    )
    private val FUTTER_HEADERS_API = listOf(
        "datum", "rezept", "erstgabe", "provokation", "reaktion", "notizen"
    )

    private val MEDIKAMENT_HEADERS_DE  = listOf(
        "Name", "Typ", "Dosierung", "Von", "Bis", "Verordnet von", "Notizen"
    )
    private val MEDIKAMENT_HEADERS_API = listOf(
        "name", "typ", "dosierung", "vonDatum", "bisDatum", "verordnetVon", "notizen"
    )

    // ── Export ────────────────────────────────────────────────────────────

    /**
     * Exportiert Tagebuch-Daten eines Hundes in ein Google Sheet.
     * Unterstützte Tabs: Symptome | Umwelt | Allergene | Phasen | Futter | Medikamente
     *
     * @param hundId        Hund dessen Daten exportiert werden
     * @param spreadsheetId Bestehende Sheet-ID oder null → neues Sheet wird erstellt
     * @param von           Zeitraum von (für zeitbegrenzte Tabs)
     * @param bis           Zeitraum bis
     * @param tabs          Welche Tabs exportiert werden sollen
     * @param token         OAuth2-Token
     */
    suspend fun exportToSheets(
        hundId: Long,
        spreadsheetId: String?,
        von: LocalDate,
        bis: LocalDate,
        tabs: Set<ExportTab> = ExportTab.entries.toSet(),
        token: String
    ): SheetsExportResult = withContext(Dispatchers.IO) {
        try {
            val api     = SheetsApiClient(token)
            val sheetId = spreadsheetId ?: api.createSpreadsheet(
                "AllerPaw Export ${LocalDate.now()}"
            )

            if (ExportTab.SYMPTOME in tabs) {
                val rows = buildSymptomRows(tagebuchRepo.symptomeRange(hundId, von, bis))
                api.writeSheet(sheetId, "Symptome",
                    SYMPTOM_HEADERS_DE, SYMPTOM_HEADERS_API, rows)
            }

            if (ExportTab.UMWELT in tabs) {
                val rows = buildUmweltRows(tagebuchRepo.umweltRange(hundId, von, bis))
                api.writeSheet(sheetId, "Umwelt",
                    UMWELT_HEADERS_DE, UMWELT_HEADERS_API, rows)
            }

            if (ExportTab.ALLERGENE in tabs) {
                val rows = buildAllergenRows(tagebuchRepo.allergenList(hundId))
                api.writeSheet(sheetId, "Allergene",
                    ALLERGEN_HEADERS_DE, ALLERGEN_HEADERS_API, rows)
            }

            if (ExportTab.PHASEN in tabs) {
                val rows = buildPhasenRows(tagebuchRepo.phasenList(hundId))
                api.writeSheet(sheetId, "Phasen",
                    PHASEN_HEADERS_DE, PHASEN_HEADERS_API, rows)
            }

            if (ExportTab.FUTTER in tabs) {
                val rows = buildFutterRows(tagebuchRepo.futterRange(hundId, von, bis))
                api.writeSheet(sheetId, "Futter",
                    FUTTER_HEADERS_DE, FUTTER_HEADERS_API, rows)
            }

            if (ExportTab.MEDIKAMENTE in tabs) {
                val rows = buildMedikamentRows(tagebuchRepo.medikamentList(hundId))
                api.writeSheet(sheetId, "Medikamente",
                    MEDIKAMENT_HEADERS_DE, MEDIKAMENT_HEADERS_API, rows)
            }

            SheetsExportResult.Success(sheetId)

        } catch (e: Exception) {
            SheetsExportResult.Error(e.message ?: "Unbekannter Fehler")
        }
    }

    // ── Import ────────────────────────────────────────────────────────────

    /**
     * Liest die Kopfzeilen eines Sheets und erstellt automatisches Spalten-Mapping.
     *
     * @return SheetsImportPreview mit auto-gemappten und ungemappten Spalten
     */
    suspend fun previewImport(
        spreadsheetId: String,
        sheetName: String,
        token: String
    ): SheetsImportPreview = withContext(Dispatchers.IO) {
        try {
            val api = SheetsApiClient(token)

            // Kopfzeilen laden (Zeile 1 = DE-Header, Zeile 2 = API-Keys)
            val rows = api.readSheet(spreadsheetId, sheetName, "A1:Z2")
            if (rows.isEmpty()) return@withContext SheetsImportPreview(
                sheetName   = sheetName,
                headers     = emptyList(),
                autoMapping = emptyMap(),
                rawRows     = emptyList()
            )

            val headers  = rows[0]
            val apiKeys  = if (rows.size > 1) rows[1] else emptyList()

            // Felder je Typ bestimmen
            val fields = when (sheetName.lowercase()) {
                "symptome", "symptoms"   -> SheetsColumnMapper.SYMPTOM_FIELDS
                "umwelt", "environment"  -> SheetsColumnMapper.UMWELT_FIELDS
                "allergene", "allergens" -> SheetsColumnMapper.ALLERGEN_FIELDS
                "futter", "food"         -> SheetsColumnMapper.FUTTER_FIELDS
                else                     -> SheetsColumnMapper.SYMPTOM_FIELDS
            }

            // Auto-Mapping: erst API-Keys (Zeile 2), dann DE-Header (Zeile 1)
            val autoMapping = if (apiKeys.isNotEmpty()) {
                // Zeile 2 enthält API-Keys → direktes Mapping
                apiKeys.mapIndexedNotNull { i, key ->
                    if (key.isNotBlank()) i to key else null
                }.toMap()
            } else {
                // Nur DE-Header → Fuzzy-Matching
                SheetsColumnMapper.autoMap(headers, fields)
            }

            // Alle Datenzeilen laden (ab Zeile 3)
            val dataRows = api.readSheet(spreadsheetId, sheetName, "A3:Z")

            SheetsImportPreview(
                sheetName   = sheetName,
                headers     = headers,
                autoMapping = autoMapping,
                rawRows     = dataRows,
                unmapped    = SheetsColumnMapper.unmappedFields(fields, autoMapping)
            )

        } catch (e: Exception) {
            SheetsImportPreview(
                sheetName   = sheetName,
                headers     = emptyList(),
                autoMapping = emptyMap(),
                rawRows     = emptyList(),
                fehler      = e.message
            )
        }
    }

    /**
     * Importiert Daten mit dem bestätigten Spalten-Mapping.
     *
     * @param hundId      Ziel-Hund
     * @param preview     Vorschau mit Roh-Daten
     * @param mapping     Finales Spalten-Mapping (Index → Feldname)
     */
    suspend fun importFromSheets(
        hundId: Long,
        preview: SheetsImportPreview,
        mapping: Map<Int, String>
    ): SheetsImportResult = withContext(Dispatchers.IO) {
        try {
            var importiert = 0
            var fehler     = 0

            preview.rawRows.forEach { row ->
                try {
                    val felder = mapping.entries.associate { (colIdx, feldName) ->
                        feldName to (row.getOrNull(colIdx) ?: "")
                    }

                    when (preview.sheetName.lowercase()) {
                        "symptome", "symptoms" -> {
                            val entity = parseSymptomRow(hundId, felder) ?: return@forEach
                            tagebuchRepo.saveSymptom(entity)
                            importiert++
                        }
                        "umwelt", "environment" -> {
                            val entity = parseUmweltRow(hundId, felder) ?: return@forEach
                            tagebuchRepo.saveUmwelt(entity)
                            importiert++
                        }
                        "allergene", "allergens" -> {
                            val entity = parseAllergenRow(hundId, felder) ?: return@forEach
                            tagebuchRepo.saveAllergen(entity)
                            importiert++
                        }
                    }
                } catch (e: Exception) {
                    fehler++
                }
            }

            SheetsImportResult.Success(importiert = importiert, fehler = fehler)

        } catch (e: Exception) {
            SheetsImportResult.Error(e.message ?: "Import fehlgeschlagen")
        }
    }

    // ── Row-Builder (Export) ──────────────────────────────────────────────

    private fun buildSymptomRows(symptome: List<TagebuchSymptomEntity>): List<List<String>> =
        symptome.map { s ->
            listOf(
                s.datum.format(datumFmt),
                s.kategorie,
                s.schweregrad.toString(),
                s.koerperstelle,
                s.beschreibung,
                s.notizen
            )
        }

    private fun buildUmweltRows(umwelt: List<TagebuchUmweltEntity>): List<List<String>> =
        umwelt.map { u ->
            listOf(
                u.datum.format(datumFmt),
                u.tempMinC?.toString() ?: "",
                u.tempMaxC?.toString() ?: "",
                u.luftfeuchte?.toString() ?: "",
                u.niederschlagMm?.toString() ?: "",
                u.raumtempC?.toString() ?: "",
                u.bett,
                ""  // Pollen: separate Entity, nicht in Umwelt-Zeile
            )
        }

    private fun buildAllergenRows(allergene: List<TagebuchAllergenEntity>): List<List<String>> =
        allergene.map { a ->
            listOf(
                a.allergen,
                a.kategorie,
                a.reaktionsstaerke.toString(),
                a.symptome,
                a.notizen
            )
        }

    private fun buildPhasenRows(phasen: List<AusschlussPhasEntity>): List<List<String>> =
        phasen.map { p ->
            listOf(
                p.phasentyp,
                p.startdatum.format(datumFmt),
                p.enddatum.format(datumFmt),
                p.ergebnis
            )
        }

    private fun buildFutterRows(futter: List<TagebuchFutterEntity>): List<List<String>> =
        futter.map { f ->
            listOf(
                f.datum.format(datumFmt),
                "",   // Rezeptname – wird nicht in Entity gespeichert
                if (f.erstgabe) "Ja" else "Nein",
                if (f.provokation) "Ja" else "Nein",
                if (f.reaktion) "Ja" else "Nein",
                f.freitextErgaenzung
            )
        }

    private fun buildMedikamentRows(medikamente: List<TagebuchMedikamentEntity>): List<List<String>> =
        medikamente.map { m ->
            listOf(
                m.name,
                m.typ,
                m.dosierung,
                m.vonDatum?.format(datumFmt) ?: "",
                m.bisDatum?.format(datumFmt) ?: "",
                m.verordnetVon,
                m.notizen
            )
        }

    // ── Row-Parser (Import) ───────────────────────────────────────────────

    private fun parseSymptomRow(
        hundId: Long,
        felder: Map<String, String>
    ): TagebuchSymptomEntity? {
        val datumStr = felder["datum"] ?: return null
        val datum = parseDatum(datumStr) ?: return null
        return TagebuchSymptomEntity(
            hundId       = hundId,
            datum        = datum,
            kategorie    = felder["kategorie"] ?: "sonstiges",
            schweregrad  = felder["schweregrad"]?.toIntOrNull() ?: 0,
            koerperstelle = felder["koerperstelle"] ?: "",
            beschreibung = felder["beschreibung"] ?: "",
            notizen      = felder["notizen"] ?: ""
        )
    }

    private fun parseUmweltRow(
        hundId: Long,
        felder: Map<String, String>
    ): TagebuchUmweltEntity? {
        val datumStr = felder["datum"] ?: return null
        val datum = parseDatum(datumStr) ?: return null
        return TagebuchUmweltEntity(
            hundId         = hundId,
            datum          = datum,
            tempMinC       = felder["tempMinC"]?.toDoubleOrNull(),
            tempMaxC       = felder["tempMaxC"]?.toDoubleOrNull(),
            luftfeuchte    = felder["luftfeuchte"]?.toIntOrNull(),
            niederschlagMm = felder["niederschlagMm"]?.toDoubleOrNull(),
            raumtempC      = felder["raumtempC"]?.toDoubleOrNull(),
            bett           = felder["bett"] ?: "unverändert",
            notizen        = felder["notizen"] ?: ""
        )
    }

    private fun parseAllergenRow(
        hundId: Long,
        felder: Map<String, String>
    ): TagebuchAllergenEntity? {
        val allergen = felder["allergen"]?.takeIf { it.isNotBlank() } ?: return null
        return TagebuchAllergenEntity(
            hundId           = hundId,
            allergen         = allergen,
            kategorie        = felder["kategorie"] ?: "",
            reaktionsstaerke = felder["reaktionsstaerke"]?.toIntOrNull() ?: 1,
            symptome         = felder["symptome"] ?: "",
            notizen          = felder["notizen"] ?: ""
        )
    }

    private fun parseDatum(s: String): LocalDate? {
        val formats = listOf(
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy")
        )
        for (fmt in formats) {
            try { return LocalDate.parse(s.trim(), fmt) } catch (_: Exception) {}
        }
        return null
    }
}

// ── Daten-Klassen ─────────────────────────────────────────────────────────────

sealed class SheetsExportResult {
    data class Success(val spreadsheetId: String) : SheetsExportResult()
    data class Error(val message: String)         : SheetsExportResult()
}

data class SheetsImportPreview(
    val sheetName:   String,
    val headers:     List<String>,
    val autoMapping: Map<Int, String>,           // Spalten-Index → Feldname
    val rawRows:     List<List<String>>,
    val unmapped:    List<com.allernutri.app.data.remote.ColumnDef> = emptyList(),
    val fehler:      String? = null
)

sealed class SheetsImportResult {
    data class Success(val importiert: Int, val fehler: Int) : SheetsImportResult()
    data class Error(val message: String)                    : SheetsImportResult()
}

enum class ExportTab(val label: String) {
    SYMPTOME   ("Symptome"),
    UMWELT     ("Umwelt"),
    ALLERGENE  ("Allergene"),
    PHASEN     ("Phasen"),
    FUTTER     ("Futter"),
    MEDIKAMENTE("Medikamente")
}
