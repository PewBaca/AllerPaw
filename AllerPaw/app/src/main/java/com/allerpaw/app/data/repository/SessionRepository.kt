package com.allerpaw.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_USER_ID    = stringPreferencesKey("user_id")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
    }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { it[KEY_AUTH_TOKEN] != null }
    val userId:     Flow<String?> = dataStore.data.map { it[KEY_USER_ID] }
    val userEmail:  Flow<String?> = dataStore.data.map { it[KEY_USER_EMAIL] }

    suspend fun saveSession(userId: String, email: String, token: String) {
        dataStore.edit { prefs ->
            prefs[KEY_USER_ID]    = userId
            prefs[KEY_USER_EMAIL] = email
            prefs[KEY_AUTH_TOKEN] = token
        }
    }

    suspend fun clearSession() {
        dataStore.edit { it.clear() }
    }
}
