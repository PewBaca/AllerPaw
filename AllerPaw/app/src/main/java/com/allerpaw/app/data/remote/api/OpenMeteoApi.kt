package com.allerpaw.app.data.remote.api

import com.allerpaw.app.data.remote.dto.OpenMeteoPollenResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo Air Quality API – Pollen-Daten koordinatenbasiert.
 * Kein API-Key nötig. Basis-URL: https://air-quality-api.open-meteo.com/
 */
interface OpenMeteoApi {

    @GET("v1/air-quality")
    suspend fun getPollen(
        @Query("latitude")       lat: Double,
        @Query("longitude")      lon: Double,
        @Query("hourly")         hourly: String =
            "alder_pollen,birch_pollen,grass_pollen,mugwort_pollen,olive_pollen,ragweed_pollen",
        @Query("forecast_days")  forecastDays: Int = 3,
        @Query("timezone")       timezone: String = "Europe/Berlin"
    ): OpenMeteoPollenResponseDto
}
