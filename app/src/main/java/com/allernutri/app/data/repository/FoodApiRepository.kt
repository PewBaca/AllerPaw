package com.allernutri.app.data.repository

import com.allernutri.app.data.local.entity.ZutatEntity
import com.allernutri.app.data.local.entity.ZutatNaehrstoffEntity
import com.allernutri.app.data.remote.FoodApiClient
import com.allernutri.app.data.remote.FoodApiResponse
import com.allernutri.app.data.remote.FoodApiResult
import com.allernutri.app.data.remote.FoodQuelle
import com.allernutri.app.data.repository.ZutatenRepository
import javax.inject.Inject
import javax.inject.Singleton

data class FoodApiSettings(
    val usdaApiKey: String = "",
    val edamamAppId: String = "",
    val edamamAppKey: String = ""
)

sealed class ImportResult {
    data class Success(val zutatId: Long, val zutatName: String) : ImportResult()
    data class Error(val message: String)                         : ImportResult()
}

@Singleton
class FoodApiRepository @Inject constructor(
    private val client: FoodApiClient,
    private val zutatenRepo: ZutatenRepository
) {
    /**
     * Sucht in allen konfigurierten APIs gleichzeitig.
     * Gibt kombinierte, deduplizierte Ergebnisse zurück.
     */
    suspend fun sucheAlle(
        query: String,
        settings: FoodApiSettings,
        quellen: Set<FoodQuelle> = FoodQuelle.entries.toSet()
    ): FoodApiResponse {
        val ergebnisse = mutableListOf<FoodApiResult>()
        val fehler     = mutableListOf<String>()

        if (FoodQuelle.USDA in quellen && settings.usdaApiKey.isNotBlank()) {
            when (val r = client.sucheUsda(query, settings.usdaApiKey)) {
                is FoodApiResponse.Success -> ergebnisse.addAll(r.ergebnisse)
                is FoodApiResponse.Error   -> fehler.add("USDA: ${r.message}")
                else -> {}
            }
        }

        if (FoodQuelle.OPEN_FOOD_FACTS in quellen) {
            when (val r = client.sucheOpenFoodFacts(query)) {
                is FoodApiResponse.Success -> ergebnisse.addAll(r.ergebnisse)
                is FoodApiResponse.Error   -> fehler.add("OFF: ${r.message}")
                else -> {}
            }
        }

        if (FoodQuelle.EDAMAM in quellen && settings.edamamAppId.isNotBlank()) {
            when (val r = client.sucheEdamam(query, settings.edamamAppId, settings.edamamAppKey)) {
                is FoodApiResponse.Success -> ergebnisse.addAll(r.ergebnisse)
                is FoodApiResponse.Error   -> fehler.add("Edamam: ${r.message}")
                else -> {}
            }
        }

        return when {
            ergebnisse.isNotEmpty() -> FoodApiResponse.Success(ergebnisse)
            fehler.isNotEmpty()     -> FoodApiResponse.Error(fehler.joinToString("\n"))
            else                    -> FoodApiResponse.Empty
        }
    }

    /** Barcode-Suche (nur Open Food Facts) */
    suspend fun sucheBarcode(barcode: String): FoodApiResponse =
        client.barcodeOpenFoodFacts(barcode)

    /** USDA Detail-Laden (vollständige Nährstoffe) */
    suspend fun detailUsda(fdcId: String, settings: FoodApiSettings): FoodApiResponse =
        client.detailUsda(fdcId, settings.usdaApiKey)

    /**
     * Importiert ein FoodApiResult als neue Zutat in die lokale DB.
     * Mappt alle verfügbaren Nährstoffe auf die passenden NRC-Keys.
     */
    suspend fun importiereAlsZutat(
        result: FoodApiResult,
        hundId: Long? = null
    ): ImportResult {
        return try {
            val zutat = ZutatEntity(
                name       = result.name.trim(),
                hersteller = result.marke.trim(),
                kategorie  = mapKategorie(result),
                perMode    = "100g",
                quelle     = result.quelle.label,
                quelleId   = result.fdcId.ifBlank { result.barcodeOrId }
            )
            val zutatId = zutatenRepo.upsert(zutat)

            // Nährstoffe mappen und speichern
            val naehrstoffe = resultToNaehrstoffe(result, zutatId)
            if (naehrstoffe.isNotEmpty()) {
                zutatenRepo.saveNaehrstoffe(zutatId, naehrstoffe)
            }

            ImportResult.Success(zutatId, zutat.name)
        } catch (e: Exception) {
            ImportResult.Error("Import fehlgeschlagen: ${e.message}")
        }
    }

    // ── Kategorie-Mapping ─────────────────────────────────────────────────

    private fun mapKategorie(result: FoodApiResult): String {
        val kat = result.kategorie.lowercase()
        return when {
            kat.contains("fish") || kat.contains("fisch") || kat.contains("seafood") -> "Fisch"
            kat.contains("poultry") || kat.contains("chicken") || kat.contains("geflügel") -> "Geflügel"
            kat.contains("beef") || kat.contains("rind") || kat.contains("veal") -> "Fleisch"
            kat.contains("dairy") || kat.contains("milch") || kat.contains("milk") -> "Milchprodukte"
            kat.contains("vegetable") || kat.contains("gemüse") -> "Gemüse"
            kat.contains("fruit") || kat.contains("obst") -> "Obst"
            kat.contains("grain") || kat.contains("getreide") || kat.contains("cereal") -> "Getreide"
            kat.contains("legume") || kat.contains("hülsen") || kat.contains("bean") -> "Hülsenfrüchte"
            kat.contains("oil") || kat.contains("öl") || kat.contains("fat") -> "Öle & Fette"
            kat.contains("supplement") || kat.contains("vitamin") -> "Supplement"
            else -> result.kategorie.ifBlank { "Sonstiges" }
        }
    }

    // ── NRC-Nährstoff-Mapping ─────────────────────────────────────────────

    /**
     * Mappt FoodApiResult auf ZutatNaehrstoffEntity-Liste.
     * Keys entsprechen den NaehrstoffKatalog-Keys.
     */
    private fun resultToNaehrstoffe(result: FoodApiResult, zutatId: Long): List<ZutatNaehrstoffEntity> {
        val eintraege = mutableListOf<ZutatNaehrstoffEntity>()

        fun add(key: String, wert: Double?) {
            if (wert != null && wert > 0) {
                eintraege.add(ZutatNaehrstoffEntity(
                    zutatId       = zutatId,
                    naehrstoffKey = key,
                    wertPer100g   = wert,
                    einheit       = "g"
                ))
            }
        }

        // Energie & Makros
        add("energie_kcal",    result.energieKcal)
        add("protein",         result.proteinG)
        add("fett",            result.fettG)
        add("kohlenhydrate",   result.kohlenhydrateG)
        add("ballaststoffe",   result.ballaststoffeG)
        add("wasser",          result.wasserG)

        // Mineralstoffe
        add("calcium",         result.calciumMg)
        add("phosphor",        result.phosphorMg)
        add("kalium",          result.kaliumMg)
        add("natrium",         result.natriumMg)
        add("magnesium",       result.magnesiumMg)
        add("eisen",           result.eisenMg)
        add("zink",            result.zinkMg)
        add("kupfer",          result.kupferMg)
        add("mangan",          result.mangan)
        add("selen",           result.selenMcg)
        add("jod",             result.jodMcg)

        // Vitamine
        add("vitamin_a",       result.vitaminAMcg)
        add("vitamin_d3",      result.vitaminD3Mcg)
        add("vitamin_e",       result.vitaminEMg)
        add("vitamin_k",       result.vitaminKMcg)
        add("vitamin_b1",      result.vitaminB1Mg)
        add("vitamin_b2",      result.vitaminB2Mg)
        add("vitamin_b6",      result.vitaminB6Mg)
        add("vitamin_b12",     result.vitaminB12Mcg)
        add("vitamin_c",       result.vitaminCMg)
        add("folsaeure",       result.folsaeureMcg)
        add("biotin",          result.biotinMcg)
        add("cholin",          result.cholinMg)

        // Fettsäuren
        add("omega6_linolsaeure",      result.linolsaeurePct)
        add("omega3_alphalinolensaeure", result.alphaLinolensaeurePct)
        add("epa",             result.epaG)
        add("dha",             result.dhaG)

        return eintraege
    }
}
