package com.allernutri.app.data.repository

import com.allernutri.app.data.local.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verwaltet die Nutzer-Session.
 *
 * Auth-Token, User-ID und E-Mail werden verschlüsselt in
 * EncryptedSharedPreferences gespeichert (SecureStorage).
 * Kein Klartext im DataStore.
 *
 * Die API (isLoggedIn, userEmail als Flow) ist identisch zur
 * vorherigen Implementierung – Aufrufer müssen nicht geändert werden.
 *
 * Migrationshinweis: Nutzer, die auf diese Version updaten, werden
 * ausgeloggt, da der alte DataStore-Token nicht migriert wird.
 * Eine einmalige Neu-Anmeldung ist erforderlich.
 */
@Singleton
class SessionRepository @Inject constructor(
    private val secureStorage: SecureStorage
) {
    // Initial-Wert aus SecureStorage beim App-Start lesen
    private val _isLoggedIn = MutableStateFlow(
        secureStorage.contains(SecureStorage.KEY_AUTH_TOKEN)
    )
    val isLoggedIn: Flow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(
        secureStorage.getString(SecureStorage.KEY_USER_EMAIL).ifBlank { null }
    )
    val userEmail: Flow<String?> = _userEmail.asStateFlow()

    val userId: String?
        get() = secureStorage.getString(SecureStorage.KEY_USER_ID).ifBlank { null }

    suspend fun saveSession(userId: String, email: String, token: String) =
        withContext(Dispatchers.IO) {
            secureStorage.setString(SecureStorage.KEY_USER_ID,    userId)
            secureStorage.setString(SecureStorage.KEY_USER_EMAIL, email)
            secureStorage.setString(SecureStorage.KEY_AUTH_TOKEN, token)
            _isLoggedIn.value = true
            _userEmail.value  = email
        }

    suspend fun clearSession() = withContext(Dispatchers.IO) {
        secureStorage.remove(SecureStorage.KEY_AUTH_TOKEN)
        secureStorage.remove(SecureStorage.KEY_USER_ID)
        secureStorage.remove(SecureStorage.KEY_USER_EMAIL)
        _isLoggedIn.value = false
        _userEmail.value  = null
    }
}
