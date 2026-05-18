package com.allerpaw.app.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class UserSession(
    val userId: String,
    val email: String,
    val displayName: String,
    val token: String
)

sealed class AuthResult {
    data class Success(val session: UserSession) : AuthResult()
    data class Error(val message: String)        : AuthResult()
    object Cancelled                             : AuthResult()
}

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val session: SessionRepository
) {
    val isLoggedIn: Flow<Boolean> = session.isLoggedIn
    val userEmail:  Flow<String?> = session.userEmail

    /**
     * Google Sign-In via Credential Manager (Android 14+ preferred).
     * activity muss eine ComponentActivity sein.
     *
     * Web-Client-ID aus Google Cloud Console:
     * https://console.cloud.google.com → APIs & Dienste → Anmeldedaten
     * → OAuth 2.0-Client-IDs → Web-Client → Client-ID kopieren
     */
    suspend fun signInWithGoogle(
        activity: android.app.Activity,
        webClientId: String = WEB_CLIENT_ID
    ): AuthResult {
        return try {
            val credentialManager = CredentialManager.create(ctx)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // auch neue Accounts erlauben
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activity
            )

            val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val idToken = googleCredential.idToken

            // TODO: Backend-Call mit idToken → JWT zurückbekommen
            // Für jetzt: lokale Demo-Session
            val userSession = UserSession(
                userId      = googleCredential.id,
                email       = googleCredential.id,
                displayName = googleCredential.displayName ?: "",
                token       = idToken
            )

            session.saveSession(
                userId = userSession.userId,
                email  = userSession.email,
                token  = userSession.token
            )

            AuthResult.Success(userSession)

        } catch (e: GetCredentialException) {
            if (e.message?.contains("cancel", ignoreCase = true) == true) {
                AuthResult.Cancelled
            } else {
                AuthResult.Error(e.message ?: "Unbekannter Fehler")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Unbekannter Fehler")
        }
    }

    suspend fun signOut() = session.clearSession()

    companion object {
        // HIER DEINE WEB-CLIENT-ID eintragen (Google Cloud Console)
        const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
    }
}
