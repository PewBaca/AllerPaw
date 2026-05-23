package com.allernutri.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.allernutri.app.data.local.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Einstellungen der App.
 *
 * Nicht-sensible Werte (Standort, Sprache, IE-Modus, DWD-Region)
 * bleiben im normalen DataStore (allernutri_prefs).
 *
 * Sensible API-Keys (USDA, Edamam) werden verschlüsselt in
 * SecureStorage (EncryptedSharedPreferences) gespeichert.
 *
 * Die public API ist abwärtskompatibel: usdaApiKey, edamamAppId,
 * edamamAppKey bleiben als Flow<String> verfügbar.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val secureStorage: SecureStorage
) {
    companion object {
        // Nicht-sensible Keys im DataStore
        private val KEY_LAT        = doublePreferencesKey("standort_lat")
        private val KEY_LON        = doublePreferencesKey("standort_lon")
        private val KEY_DWD_REGION = stringPreferencesKey("dwd_region")
        private val KEY_IE_ANZEIGE = stringPreferencesKey("ie_anzeige")
        private val KEY_SPRACHE    = stringPreferencesKey("sprache")

        // Sensible Keys nur in SecureStorage – Namen stimmen mit SecureStorage überein
        private val SECURE_KEYS = setOf(
            SecureStorage.KEY_USDA_KEY,
            SecureStorage.KEY_EDAMAM_ID,
            SecureStorage.KEY_EDAMAM_KEY
        )

        const val DEFAULT_LAT = 48.137154   // München
        const val DEFAULT_LON = 11.576124
    }

    // ── Nicht-sensible Flows (DataStore) ───────────────────────────
    val standortLat: Flow<Double> = dataStore.data.map { it[KEY_LAT] ?: DEFAULT_LAT }
    val standortLon: Flow<Double> = dataStore.data.map { it[KEY_LON] ?: DEFAULT_LON }
    val dwdRegion:   Flow<String> = dataStore.data.map { it[KEY_DWD_REGION] ?: "" }
    val ieAnzeige:   Flow<String> = dataStore.data.map { it[KEY_IE_ANZEIGE] ?: "metrisch" }
    val sprache:     Flow<String> = dataStore.data.map { it[KEY_SPRACHE] ?: "de" }

    // ── Sensible API-Key-Flows (SecureStorage) ──────────────────────
    // MutableStateFlow: Initial aus SecureStorage lesen, bei setString aktualisieren
    private val _usdaApiKey = MutableStateFlow(
        secureStorage.getString(SecureStorage.KEY_USDA_KEY)
    )
    val usdaApiKey: Flow<String> = _usdaApiKey.asStateFlow()

    private val _edamamAppId = MutableStateFlow(
        secureStorage.getString(SecureStorage.KEY_EDAMAM_ID)
    )
    val edamamAppId: Flow<String> = _edamamAppId.asStateFlow()

    private val _edamamAppKey = MutableStateFlow(
        secureStorage.getString(SecureStorage.KEY_EDAMAM_KEY)
    )
    val edamamAppKey: Flow<String> = _edamamAppKey.asStateFlow()

    // ── Generische Lese-/Schreibmethoden ───────────────────────────

    /**
     * Liest einen String-Wert.
     * API-Keys werden aus SecureStorage gelesen, alle anderen aus DataStore.
     */
    suspend fun getString(key: String, default: String = ""): String =
        if (key in SECURE_KEYS) {
            withContext(Dispatchers.IO) { secureStorage.getString(key, default) }
        } else {
            val prefKey = stringPreferencesKey(key)
            dataStore.data.map { it[prefKey] ?: default }.first()
        }

    /**
     * Schreibt einen String-Wert.
     * API-Keys gehen verschlüsselt in SecureStorage, alle anderen in DataStore.
     */
    suspend fun setString(key: String, value: String) {
        if (key in SECURE_KEYS) {
            withContext(Dispatchers.IO) {
                secureStorage.setString(key, value)
                // StateFlow aktualisieren damit Flows sofort reagieren
                when (key) {
                    SecureStorage.KEY_USDA_KEY    -> _usdaApiKey.value    = value
                    SecureStorage.KEY_EDAMAM_ID   -> _edamamAppId.value   = value
                    SecureStorage.KEY_EDAMAM_KEY  -> _edamamAppKey.value  = value
                }
            }
        } else {
            val prefKey = stringPreferencesKey(key)
            dataStore.edit { it[prefKey] = value }
        }
    }

    // ── Standort & sonstige Einstellungen ──────────────────────────

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
