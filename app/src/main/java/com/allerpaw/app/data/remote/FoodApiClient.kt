package com.allerpaw.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Einheitliche Datenstuktur für Lebensmittel-Suchergebnisse.
 * Alle Nährstoffe per 100g Frischgewicht.
 */
data class FoodApiResult(
    val quelle: FoodQuelle,
    val fdcId: String = "",          // USDA FDC ID
    val barcodeOrId: String = "",    // Open Food Facts Barcode / Edamam foodId
    val name: String,
    val marke: String = "",
    val kategorie: String = "",
    val energieKcal: Double? = null,
    val proteinG: Double? = null,
    val fettG: Double? = null,
    val kohlenhydrateG: Double? = null,
    val ballaststoffeG: Double? = null,
    val wasserG: Double? = null,
    val calciumMg: Double? = null,
    val phosphorMg: Double? = null,
    val kaliumMg: Double? = null,
    val natriumMg: Double? = null,
    val magnesiumMg: Double? = null,
    val eisenMg: Double? = null,
    val zinkMg: Double? = null,
    val kupferMg: Double? = null,
    val mangan: Double? = null,
    val selenMcg: Double? = null,
    val jodMcg: Double? = null,
    val vitaminAMcg: Double? = null,
    val vitaminD3Mcg: Double? = null,
    val vitaminEMg: Double? = null,
    val vitaminKMcg: Double? = null,
    val vitaminB1Mg: Double? = null,
    val vitaminB2Mg: Double? = null,
    val vitaminB6Mg: Double? = null,
    val vitaminB12Mcg: Double? = null,
    val vitaminCMg: Double? = null,
    val folsaeureMcg: Double? = null,
    val biotinMcg: Double? = null,
    val cholinMg: Double? = null,
    val linolsaeurePct: Double? = null,  // % der Fette (Omega-6)
    val alphaLinolensaeurePct: Double? = null,  // % der Fette (Omega-3)
    val epaG: Double? = null,
    val dhaG: Double? = null,
    val rohproteinPct: Double? = null,  // für Rohfutter
    val rohfettPct: Double? = null
)

enum class FoodQuelle(val label: String) {
    USDA("USDA FoodData Central"),
    OPEN_FOOD_FACTS("Open Food Facts"),
    EDAMAM("Edamam Food Database")
}

sealed class FoodApiResponse {
    data class Success(val ergebnisse: List<FoodApiResult>) : FoodApiResponse()
    data class Error(val message: String)                   : FoodApiResponse()
    object Empty                                            : FoodApiResponse()
}

/**
 * Verbindet mit drei Food-APIs und normalisiert die Ergebnisse
 * auf ein einheitliches Format (per 100g Frischgewicht).
 *
 * API-Keys werden von außen übergeben (aus DataStore).
 */
@Singleton
class FoodApiClient @Inject constructor() {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    // ── USDA FoodData Central ─────────────────────────────────────────────

    /**
     * Sucht in USDA FoodData Central.
     * API-Key kostenlos unter https://fdc.nal.usda.gov/api-key-signup.html
     * Basis-URL: https://api.nal.usda.gov/fdc/v1/foods/search
     */
    suspend fun sucheUsda(query: String, apiKey: String, maxErgebnisse: Int = 10): FoodApiResponse =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext FoodApiResponse.Error("Kein USDA API-Key konfiguriert")
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = "https://api.nal.usda.gov/fdc/v1/foods/search" +
                    "?query=$encoded&api_key=$apiKey&pageSize=$maxErgebnisse" +
                    "&dataType=Foundation,SR%20Legacy,Branded"
                val resp = http.newCall(Request.Builder().url(url).build()).execute()
                val body = resp.body?.string() ?: return@withContext FoodApiResponse.Empty
                val json = JSONObject(body)
                val foods = json.optJSONArray("foods") ?: return@withContext FoodApiResponse.Empty

                val ergebnisse = (0 until foods.length()).mapNotNull { i ->
                    usdaToResult(foods.getJSONObject(i))
                }
                if (ergebnisse.isEmpty()) FoodApiResponse.Empty else FoodApiResponse.Success(ergebnisse)
            } catch (e: Exception) {
                FoodApiResponse.Error("USDA Fehler: ${e.message}")
            }
        }

    suspend fun detailUsda(fdcId: String, apiKey: String): FoodApiResponse =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext FoodApiResponse.Error("Kein USDA API-Key")
            try {
                val url = "https://api.nal.usda.gov/fdc/v1/food/$fdcId?api_key=$apiKey"
                val resp = http.newCall(Request.Builder().url(url).build()).execute()
                val body = resp.body?.string() ?: return@withContext FoodApiResponse.Empty
                val json = JSONObject(body)
                val result = usdaDetailToResult(json)
                if (result != null) FoodApiResponse.Success(listOf(result)) else FoodApiResponse.Empty
            } catch (e: Exception) {
                FoodApiResponse.Error("USDA Detail Fehler: ${e.message}")
            }
        }

    private fun usdaToResult(food: JSONObject): FoodApiResult? {
        val name = food.optString("description").ifBlank { return null }
        val fdcId = food.optInt("fdcId").toString()
        val marke = food.optString("brandName", "")
        val kat   = food.optString("foodCategory", "")
        val nutrients = food.optJSONArray("foodNutrients") ?: JSONArray()
        val map = usdaNutrientMap(nutrients)
        return FoodApiResult(
            quelle        = FoodQuelle.USDA,
            fdcId         = fdcId,
            name          = name,
            marke         = marke,
            kategorie     = kat,
            energieKcal   = map[1008],
            proteinG      = map[1003],
            fettG         = map[1004],
            kohlenhydrateG = map[1005],
            ballaststoffeG = map[1079],
            wasserG       = map[1051],
            calciumMg     = map[1087],
            phosphorMg    = map[1091],
            kaliumMg      = map[1092],
            natriumMg     = map[1093],
            magnesiumMg   = map[1090],
            eisenMg       = map[1089],
            zinkMg        = map[1095],
            kupferMg      = map[1098],
            selenMcg      = map[1103],
            vitaminAMcg   = map[1106],
            vitaminD3Mcg  = map[1114],
            vitaminEMg    = map[1109],
            vitaminKMcg   = map[1185],
            vitaminB1Mg   = map[1165],
            vitaminB2Mg   = map[1166],
            vitaminB6Mg   = map[1175],
            vitaminB12Mcg = map[1178],
            vitaminCMg    = map[1162],
            folsaeureMcg  = map[1177],
            cholinMg      = map[1180],
            epaG          = map[1278]?.div(1000),
            dhaG          = map[1272]?.div(1000)
        )
    }

    private fun usdaDetailToResult(food: JSONObject): FoodApiResult? {
        val name = food.optString("description").ifBlank { return null }
        val fdcId = food.optInt("fdcId").toString()
        val nutrients = food.optJSONArray("foodNutrients") ?: JSONArray()
        val map = usdaDetailNutrientMap(nutrients)
        return FoodApiResult(
            quelle        = FoodQuelle.USDA,
            fdcId         = fdcId,
            name          = name,
            energieKcal   = map[1008],
            proteinG      = map[1003],
            fettG         = map[1004],
            kohlenhydrateG = map[1005],
            ballaststoffeG = map[1079],
            wasserG       = map[1051],
            calciumMg     = map[1087],
            phosphorMg    = map[1091],
            kaliumMg      = map[1092],
            natriumMg     = map[1093],
            magnesiumMg   = map[1090],
            eisenMg       = map[1089],
            zinkMg        = map[1095],
            kupferMg      = map[1098],
            selenMcg      = map[1103],
            vitaminAMcg   = map[1106],
            vitaminD3Mcg  = map[1114],
            vitaminEMg    = map[1109],
            vitaminKMcg   = map[1185],
            vitaminB1Mg   = map[1165],
            vitaminB2Mg   = map[1166],
            vitaminB6Mg   = map[1175],
            vitaminB12Mcg = map[1178],
            vitaminCMg    = map[1162],
            folsaeureMcg  = map[1177],
            cholinMg      = map[1180],
            epaG          = map[1278]?.div(1000),
            dhaG          = map[1272]?.div(1000)
        )
    }

    /** Suche-Response: Nährstoffe im flachen Array */
    private fun usdaNutrientMap(nutrients: JSONArray): Map<Int, Double> {
        val map = mutableMapOf<Int, Double>()
        for (i in 0 until nutrients.length()) {
            val n = nutrients.getJSONObject(i)
            val id = n.optInt("nutrientId", -1)
            val value = n.optDouble("value", Double.NaN)
            if (id > 0 && !value.isNaN()) map[id] = value
        }
        return map
    }

    /** Detail-Response: Nährstoffe verschachtelt */
    private fun usdaDetailNutrientMap(nutrients: JSONArray): Map<Int, Double> {
        val map = mutableMapOf<Int, Double>()
        for (i in 0 until nutrients.length()) {
            val n = nutrients.getJSONObject(i)
            val nutrient = n.optJSONObject("nutrient") ?: continue
            val id = nutrient.optInt("id", -1)
            val amount = n.optDouble("amount", Double.NaN)
            if (id > 0 && !amount.isNaN()) map[id] = amount
        }
        return map
    }

    // ── Open Food Facts ───────────────────────────────────────────────────

    /**
     * Sucht in Open Food Facts (kein API-Key nötig).
     * Basis-URL: https://world.openfoodfacts.org/cgi/search.pl
     */
    suspend fun sucheOpenFoodFacts(query: String, maxErgebnisse: Int = 10): FoodApiResponse =
        withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = "https://world.openfoodfacts.org/cgi/search.pl" +
                    "?search_terms=$encoded&search_simple=1&action=process" +
                    "&json=1&page_size=$maxErgebnisse" +
                    "&fields=product_name,brands,categories_tags,nutriments,code"
                val req  = Request.Builder().url(url)
                    .addHeader("User-Agent", "AllerPaw/1.0 (Android; allerpaw@example.com)")
                    .build()
                val resp = http.newCall(req).execute()
                val body = resp.body?.string() ?: return@withContext FoodApiResponse.Empty
                val json = JSONObject(body)
                val products = json.optJSONArray("products") ?: return@withContext FoodApiResponse.Empty

                val ergebnisse = (0 until products.length()).mapNotNull { i ->
                    offToResult(products.getJSONObject(i))
                }
                if (ergebnisse.isEmpty()) FoodApiResponse.Empty else FoodApiResponse.Success(ergebnisse)
            } catch (e: Exception) {
                FoodApiResponse.Error("Open Food Facts Fehler: ${e.message}")
            }
        }

    suspend fun barcodeOpenFoodFacts(barcode: String): FoodApiResponse =
        withContext(Dispatchers.IO) {
            try {
                val url  = "https://world.openfoodfacts.org/api/v2/product/$barcode.json" +
                    "?fields=product_name,brands,categories_tags,nutriments,code"
                val resp = http.newCall(Request.Builder().url(url).build()).execute()
                val body = resp.body?.string() ?: return@withContext FoodApiResponse.Empty
                val json = JSONObject(body)
                val status = json.optInt("status", 0)
                if (status != 1) return@withContext FoodApiResponse.Empty
                val product = json.optJSONObject("product") ?: return@withContext FoodApiResponse.Empty
                val result = offToResult(product)
                if (result != null) FoodApiResponse.Success(listOf(result)) else FoodApiResponse.Empty
            } catch (e: Exception) {
                FoodApiResponse.Error("Barcode Fehler: ${e.message}")
            }
        }

    private fun offToResult(product: JSONObject): FoodApiResult? {
        val name = product.optString("product_name").ifBlank { return null }
        val barcode  = product.optString("code", "")
        val marke    = product.optString("brands", "")
        val kategorie = product.optJSONArray("categories_tags")
            ?.let { (0 until it.length()).map { i -> it.getString(i) } }
            ?.firstOrNull { !it.startsWith("en:") }?.removePrefix("de:") ?: ""
        val n = product.optJSONObject("nutriments") ?: JSONObject()

        // OFF gibt Werte per 100g in Feldern wie "energy-kcal_100g"
        fun d(key: String): Double? = n.optDouble("${key}_100g", Double.NaN)
            .takeIf { !it.isNaN() && it >= 0 }

        return FoodApiResult(
            quelle         = FoodQuelle.OPEN_FOOD_FACTS,
            barcodeOrId    = barcode,
            name           = name.trim(),
            marke          = marke.trim(),
            kategorie      = kategorie,
            energieKcal    = d("energy-kcal"),
            proteinG       = d("proteins"),
            fettG          = d("fat"),
            kohlenhydrateG = d("carbohydrates"),
            ballaststoffeG = d("fiber"),
            calciumMg      = d("calcium")?.times(1000),   // OFF gibt in g, umrechnen
            phosphorMg     = d("phosphorus")?.times(1000),
            kaliumMg       = d("potassium")?.times(1000),
            natriumMg      = d("sodium")?.times(1000),
            magnesiumMg    = d("magnesium")?.times(1000),
            eisenMg        = d("iron")?.times(1000),
            zinkMg         = d("zinc")?.times(1000),
            vitaminAMcg    = d("vitamin-a")?.times(1_000_000),
            vitaminD3Mcg   = d("vitamin-d")?.times(1_000_000),
            vitaminEMg     = d("vitamin-e")?.times(1000),
            vitaminCMg     = d("vitamin-c")?.times(1000),
            vitaminB1Mg    = d("vitamin-b1")?.times(1000),
            vitaminB2Mg    = d("vitamin-b2")?.times(1000),
            vitaminB6Mg    = d("vitamin-b6")?.times(1000),
            vitaminB12Mcg  = d("vitamin-b12")?.times(1_000_000)
        )
    }

    // ── Edamam Food Database ──────────────────────────────────────────────

    /**
     * Sucht in Edamam Food Database.
     * API-Key: https://developer.edamam.com/food-database-api
     * Kostenloser Tier: 1000 Anfragen/Monat
     */
    suspend fun sucheEdamam(
        query: String,
        appId: String,
        appKey: String,
        maxErgebnisse: Int = 10
    ): FoodApiResponse = withContext(Dispatchers.IO) {
        if (appId.isBlank() || appKey.isBlank())
            return@withContext FoodApiResponse.Error("Kein Edamam App-ID/Key konfiguriert")
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.edamam.com/api/food-database/v2/parser" +
                "?app_id=$appId&app_key=$appKey" +
                "&ingr=$encoded&nutrition-type=logging"
            val resp = http.newCall(Request.Builder().url(url).build()).execute()
            val body = resp.body?.string() ?: return@withContext FoodApiResponse.Empty
            val json = JSONObject(body)
            val hints = json.optJSONArray("hints") ?: return@withContext FoodApiResponse.Empty

            val ergebnisse = (0 until minOf(hints.length(), maxErgebnisse)).mapNotNull { i ->
                edamamToResult(hints.getJSONObject(i).optJSONObject("food") ?: return@mapNotNull null)
            }
            if (ergebnisse.isEmpty()) FoodApiResponse.Empty else FoodApiResponse.Success(ergebnisse)
        } catch (e: Exception) {
            FoodApiResponse.Error("Edamam Fehler: ${e.message}")
        }
    }

    private fun edamamToResult(food: JSONObject): FoodApiResult? {
        val name  = food.optString("label").ifBlank { return null }
        val foodId = food.optString("foodId", "")
        val marke  = food.optString("brand", "")
        val kat    = food.optString("category", "")
        val n      = food.optJSONObject("nutrients") ?: JSONObject()

        fun d(key: String): Double? = n.optDouble(key, Double.NaN).takeIf { !it.isNaN() && it >= 0 }

        // Edamam gibt Nährstoffe per 100g, Schlüssel sind Kürzel
        return FoodApiResult(
            quelle         = FoodQuelle.EDAMAM,
            barcodeOrId    = foodId,
            name           = name.trim(),
            marke          = marke.trim(),
            kategorie      = kat,
            energieKcal    = d("ENERC_KCAL"),
            proteinG       = d("PROCNT"),
            fettG          = d("FAT"),
            kohlenhydrateG = d("CHOCDF"),
            ballaststoffeG = d("FIBTG"),
            calciumMg      = d("CA"),
            phosphorMg     = d("P"),
            kaliumMg       = d("K"),
            natriumMg      = d("NA"),
            magnesiumMg    = d("MG"),
            eisenMg        = d("FE"),
            zinkMg         = d("ZN"),
            vitaminAMcg    = d("VITA_RAE"),
            vitaminD3Mcg   = d("VITD"),
            vitaminEMg     = d("TOCPHA"),
            vitaminKMcg    = d("VITK1"),
            vitaminB1Mg    = d("THIA"),
            vitaminB2Mg    = d("RIBF"),
            vitaminB6Mg    = d("VITB6A"),
            vitaminB12Mcg  = d("VITB12"),
            vitaminCMg     = d("VITC"),
            folsaeureMcg   = d("FOLDFE"),
            cholinMg       = d("CHOLN"),
            epaG           = d("EPA"),
            dhaG           = d("DHA")
        )
    }
}
