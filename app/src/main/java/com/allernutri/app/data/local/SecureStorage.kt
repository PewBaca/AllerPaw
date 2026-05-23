package com.allernutri.app.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verschlüsselter Schlüssel-Wert-Speicher für sensible App-Daten.
 *
 * Verwendet AES256-GCM (Schlüssel im Android Keystore) für die
 * Datenverschlüsselung und AES256-SIV für die Key-Verschlüsselung.
 * API: security-crypto 1.0.0 (stabile Version, MasterKeys-API).
 *
 * Gespeicherte Daten:
 *  - Google Auth-Token (JWT)
 *  - User-ID und E-Mail
 *  - USDA API-Key
 *  - Edamam App-ID und App-Key
 *
 * Nicht sensible Einstellungen (Standort, Sprache, IE-Modus) verbleiben
 * im normalen DataStore (allernutri_prefs).
 *
 * Hinweis: Die Datei allernutri_secure.xml ist in backup_rules.xml
 * vom Backup ausgeschlossen, da Android-Keystore-Schlüssel
 * gerätespezifisch und nicht portierbar sind.
 */
@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SecureStorage"
        private const val PREFS_NAME = "allernutri_secure"

        const val KEY_AUTH_TOKEN  = "auth_token"
        const val KEY_USER_ID     = "user_id"
        const val KEY_USER_EMAIL  = "user_email"
        const val KEY_USDA_KEY    = "usda_api_key"
        const val KEY_EDAMAM_ID   = "edamam_app_id"
        const val KEY_EDAMAM_KEY  = "edamam_app_key"
    }

    /**
     * Lazy-Initialisierung: EncryptedSharedPreferences werden erst beim
     * ersten Zugriff angelegt. Schlägt die Initialisierung fehl
     * (z.B. beschädigter Keystore), wird die Datei gelöscht und neu
     * angelegt — der Nutzer muss sich neu anmelden.
     */
    private val prefs: SharedPreferences by lazy { createPrefs() }

    private fun createPrefs(): SharedPreferences {
        return try {
            // security-crypto 1.0.0: MasterKeys statt MasterKey.Builder
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Keystore-Fehler: beschädigte Datei löschen und neu erstellen.
            // Tritt auf nach: Factory Reset mit Backup-Restore, oder korruptem
            // Keystore. Ergebnis: Nutzer muss sich neu anmelden.
            Log.w(TAG, "EncryptedSharedPreferences init fehlgeschlagen – Reset: ${e.message}")
            context.deleteSharedPreferences(PREFS_NAME)

            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    fun getString(key: String, default: String = ""): String =
        prefs.getString(key, default) ?: default

    fun setString(key: String, value: String) =
        prefs.edit().putString(key, value).apply()

    fun remove(key: String) =
        prefs.edit().remove(key).apply()

    fun clear() =
        prefs.edit().clear().apply()

    fun contains(key: String): Boolean =
        prefs.contains(key)
}
