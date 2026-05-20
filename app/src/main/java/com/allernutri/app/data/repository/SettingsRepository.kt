package com.allernutri.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        private val KEY_IE_ANZEIGE  = stringPreferencesKey("ie_anzeige")
        private val KEY_SPRACHE     = stringPreferencesKey("sprache")
        private val KEY_USDA_KEY    = stringPreferencesKey("usda_api_key")
        private val KEY_EDAMAM_ID   = stringPreferencesKey("edamam_app_id")
        private val KEY_EDAMAM_KEY  = stringPreferencesKey("edamam_app_key")

        // Standardwerte
        const val DEFAULT_LAT = 48.137154   // München
        const val DEFAULT_LON = 11.576124
    }

    val standortLat: Flow<Double>  = dataStore.data.map { it[KEY_LAT] ?: DEFAULT_LAT }
    val standortLon: Flow<Double>  = dataStore.data.map { it[KEY_LON] ?: DEFAULT_LON }
    val dwdRegion:   Flow<String>  = dataStore.data.map { it[KEY_DWD_REGION] ?: "" }
    val ieAnzeige:   Flow<String>  = dataStore.data.map { it[KEY_IE_ANZEIGE] ?: "metrisch" }
    val sprache:     Flow<String>  = dataStore.data.map { it[KEY_SPRACHE] ?: "de" }
    val usdaApiKey:  Flow<String>  = dataStore.data.map { it[KEY_USDA_KEY] ?: "" }
    val edamamAppId: Flow<String>  = dataStore.data.map { it[KEY_EDAMAM_ID] ?: "" }
    val edamamAppKey: Flow<String> = dataStore.data.map { it[KEY_EDAMAM_KEY] ?: "" }

    /** Liest einen String-Wert synchron (suspend) */
    suspend fun getString(key: String, default: String = ""): String {
        val prefKey = stringPreferencesKey(key)
        return dataStore.data.map { it[prefKey] ?: default }.first()
    }

    /** Schreibt einen String-Wert */
    suspend fun setString(key: String, value: String) {
        val prefKey = stringPreferencesKey(key)
        dataStore.edit { it[prefKey] = value }
    }

    suspend fun setStandort(lat: Double, lon: Double, name: String = "") {
        dataStore.edit { prefs ->
            prefs[KEY_LAT] = lat
            prefs[KEY_LON] = lon
        }
        if (name.isNotBlank()) setString("standort_name", name)
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
