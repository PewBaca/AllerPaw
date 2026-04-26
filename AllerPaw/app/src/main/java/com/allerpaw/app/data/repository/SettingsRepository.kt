package com.allerpaw.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_LAT         = doublePreferencesKey("standort_lat")
        private val KEY_LON         = doublePreferencesKey("standort_lon")
        private val KEY_DWD_REGION  = stringPreferencesKey("dwd_region")
        private val KEY_IE_ANZEIGE  = stringPreferencesKey("ie_anzeige")   // "ie" | "metrisch"
        private val KEY_SPRACHE     = stringPreferencesKey("sprache")       // "de" | "en"

        // Standardwerte
        const val DEFAULT_LAT = 48.137154   // München
        const val DEFAULT_LON = 11.576124
    }

    val standortLat: Flow<Double>  = dataStore.data.map { it[KEY_LAT] ?: DEFAULT_LAT }
    val standortLon: Flow<Double>  = dataStore.data.map { it[KEY_LON] ?: DEFAULT_LON }
    val dwdRegion:   Flow<String>  = dataStore.data.map { it[KEY_DWD_REGION] ?: "" }
    val ieAnzeige:   Flow<String>  = dataStore.data.map { it[KEY_IE_ANZEIGE] ?: "metrisch" }
    val sprache:     Flow<String>  = dataStore.data.map { it[KEY_SPRACHE] ?: "de" }

    suspend fun setStandort(lat: Double, lon: Double) {
        dataStore.edit { prefs ->
            prefs[KEY_LAT] = lat
            prefs[KEY_LON] = lon
        }
    }

    suspend fun setDwdRegion(region: String) {
        dataStore.edit { it[KEY_DWD_REGION] = region }
    }

    suspend fun setIeAnzeige(modus: String) {
        dataStore.edit { it[KEY_IE_ANZEIGE] = modus }
    }

    suspend fun setSprache(sprache: String) {
        dataStore.edit { it[KEY_SPRACHE] = sprache }
    }
}
