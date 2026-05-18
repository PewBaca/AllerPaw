package com.allerpaw.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─────────────────────────────────────────────
// BrightSky (DWD Wetter)
// ─────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class BrightSkyResponseDto(
    @Json(name = "weather") val weather: List<BrightSkyWeatherDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BrightSkyWeatherDto(
    @Json(name = "timestamp")          val timestamp: String,
    @Json(name = "temperature")        val temperature: Double?,
    @Json(name = "relative_humidity")  val relativeHumidity: Int?,
    @Json(name = "precipitation")      val precipitation: Double?,
    @Json(name = "condition")          val condition: String?
)

// ─────────────────────────────────────────────
// Open-Meteo Air Quality (Pollen)
// ─────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class OpenMeteoPollenResponseDto(
    @Json(name = "hourly")            val hourly: OpenMeteoHourlyDto?,
    @Json(name = "hourly_units")      val hourlyUnits: Map<String, String>?
)

@JsonClass(generateAdapter = true)
data class OpenMeteoHourlyDto(
    @Json(name = "time")              val time: List<String> = emptyList(),
    @Json(name = "alder_pollen")      val alderPollen: List<Double?> = emptyList(),
    @Json(name = "birch_pollen")      val birchPollen: List<Double?> = emptyList(),
    @Json(name = "grass_pollen")      val grassPollen: List<Double?> = emptyList(),
    @Json(name = "mugwort_pollen")    val mugwortPollen: List<Double?> = emptyList(),
    @Json(name = "olive_pollen")      val olivePollen: List<Double?> = emptyList(),
    @Json(name = "ragweed_pollen")    val ragweedPollen: List<Double?> = emptyList()
)

// ─────────────────────────────────────────────
// Domain-Modelle (API-agnostisch)
// ─────────────────────────────────────────────

data class WetterDaten(
    val datum: String,
    val tempMinC: Double?,
    val tempMaxC: Double?,
    val luftfeuchte: Int?,
    val niederschlagMm: Double?
)

data class PollenDaten(
    val pollenart: String,
    val staerkeRaw: Double,       // Konzentration in Grains/m³
    val staerke0bis5: Int         // Umgerechnet auf 0–5 Skala
)

/**
 * Konvertiert Grains/m³ → Skala 0–5 (angelehnt an DWD-Klassifizierung)
 */
fun Double.toPollenStaerke(art: String): Int = when (art) {
    "alder_pollen", "birch_pollen" -> when {
        this == 0.0  -> 0
        this < 10    -> 1
        this < 50    -> 2
        this < 100   -> 3
        this < 300   -> 4
        else         -> 5
    }
    "grass_pollen" -> when {
        this == 0.0  -> 0
        this < 5     -> 1
        this < 20    -> 2
        this < 50    -> 3
        this < 100   -> 4
        else         -> 5
    }
    else -> when {
        this == 0.0  -> 0
        this < 10    -> 1
        this < 30    -> 2
        this < 80    -> 3
        this < 200   -> 4
        else         -> 5
    }
}
