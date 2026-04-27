package com.allerpaw.app.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allerpaw.app.data.repository.AuthRepository
import com.allerpaw.app.data.repository.AuthResult
import com.allerpaw.app.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean   = false,
    val fehler: String?      = null,
    val emailInput: String   = "",
    val passwortInput: String = ""
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val session: SessionRepository,
    private val auth: AuthRepository
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = session.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // true solange DataStore noch lädt → verhindert schwarzen Bildschirm
    val isLoading: StateFlow<Boolean> = session.isLoggedIn
        .map { false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signInWithGoogle(activity: Activity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, fehler = null) }
            when (val result = auth.signInWithGoogle(activity)) {
                is AuthResult.Success   -> { _uiState.update { it.copy(isLoading = false) }; onSuccess() }
                is AuthResult.Error     -> _uiState.update { it.copy(isLoading = false, fehler = result.message) }
                is AuthResult.Cancelled -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /** Demo-Login ohne echten Google-Account (für Tests) */
    fun signInDemo(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            session.saveSession("demo", "demo@allerpaw.app", "demo-token")
            _uiState.update { it.copy(isLoading = false) }
            onSuccess()
        }
    }

    fun signOut() = viewModelScope.launch { auth.signOut() }

    fun setEmail(v: String)    = _uiState.update { it.copy(emailInput = v) }
    fun setPasswort(v: String) = _uiState.update { it.copy(passwortInput = v) }
    fun clearFehler()          = _uiState.update { it.copy(fehler = null) }
}
