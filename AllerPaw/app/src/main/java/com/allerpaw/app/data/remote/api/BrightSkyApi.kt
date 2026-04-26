package com.allerpaw.app.data.remote.api

import com.allerpaw.app.data.remote.dto.BrightSkyResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * BrightSky API – Wrapper um den DWD Open Data.
 * Kein API-Key nötig. Basis-URL: https://api.brightsky.dev/
 */
interface BrightSkyApi {

    @GET("weather")
    suspend fun getWeather(
        @Query("date")      date: String,   // ISO 8601: 2024-04-26
        @Query("last_date") lastDate: String,
        @Query("lat")       lat: Double,
        @Query("lon")       lon: Double,
        @Query("units")     units: String = "dwd"
    ): BrightSkyResponseDto
}
