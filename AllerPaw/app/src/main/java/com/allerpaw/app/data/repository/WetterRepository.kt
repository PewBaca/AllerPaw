package com.allerpaw.app.data.repository

import com.allerpaw.app.data.remote.api.BrightSkyApi
import com.allerpaw.app.data.remote.api.OpenMeteoApi
import com.allerpaw.app.data.remote.dto.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WetterRepository @Inject constructor(
    private val brightSkyApi: BrightSkyApi,
    private val openMeteoApi: OpenMeteoApi
) {
    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Lädt Wetterdaten für ein Datum via BrightSky (DWD).
     * lat/lon: Nutzerstandort aus DataStore/Settings.
     */
    suspend fun getWetter(datum: LocalDate, lat: Double, lon: Double): WetterDaten? {
        return try {
            val response = brightSkyApi.getWeather(
                date     = datum.format(fmt),
                lastDate = datum.plusDays(1).format(fmt),
                lat      = lat,
                lon      = lon
            )
            val eintraege = response.weather
            if (eintraege.isEmpty()) return null

            val temps  = eintraege.mapNotNull { it.temperature }
            val feuchte = eintraege.mapNotNull { it.relativeHumidity }
            val niederschlag = eintraege.mapNotNull { it.precipitation }.sum()

            WetterDaten(
                datum          = datum.format(fmt),
                tempMinC       = temps.minOrNull(),
                tempMaxC       = temps.maxOrNull(),
                luftfeuchte    = if (feuchte.isEmpty()) null
                                 else feuchte.average().toInt(),
                niederschlagMm = if (niederschlag == 0.0) null else niederschlag
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Lädt Pollen-Daten für heute + 3 Tage via Open-Meteo.
     * Gibt Tagesmittelwerte je Pollenart zurück.
     */
    suspend fun getPollen(lat: Double, lon: Double): Map<String, List<PollenDaten>> {
        return try {
            val response = openMeteoApi.getPollen(lat, lon)
            val hourly   = response.hourly ?: return emptyMap()

            val pollenArten = mapOf(
                "Erle"     to hourly.alderPollen,
                "Birke"    to hourly.birchPollen,
                "Gräser"   to hourly.grassPollen,
                "Beifuß"   to hourly.mugwortPollen,
                "Olive"    to hourly.olivePollen,
                "Ambrosia" to hourly.ragweedPollen
            )

            val result = mutableMapOf<String, List<PollenDaten>>()

            pollenArten.forEach { (name, werte) ->
                val apiKey = when (name) {
                    "Erle"   -> "alder_pollen"
                    "Birke"  -> "birch_pollen"
                    "Gräser" -> "grass_pollen"
                    "Beifuß" -> "mugwort_pollen"
                    "Olive"  -> "olive_pollen"
                    else     -> "ragweed_pollen"
                }
                // Tagesmittel berechnen (24 Stunden-Blöcke)
                val tagesmittel = werte
                    .chunked(24)
                    .map { block ->
                        val avg = block.filterNotNull().takeIf { it.isNotEmpty() }
                            ?.average() ?: 0.0
                        PollenDaten(
                            pollenart    = name,
                            staerkeRaw   = avg,
                            staerke0bis5 = avg.toPollenStaerke(apiKey)
                        )
                    }
                if (tagesmittel.any { it.staerkeRaw > 0 }) {
                    result[name] = tagesmittel
                }
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
