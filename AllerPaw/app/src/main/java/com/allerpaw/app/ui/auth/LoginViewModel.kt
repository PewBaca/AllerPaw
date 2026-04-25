package com.allerpaw.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allerpaw.app.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val session: SessionRepository
) : ViewModel() {

    // isLoggedIn: false solange DataStore noch nicht gelesen hat
    val isLoggedIn: StateFlow<Boolean> = session.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // isLoading: true bis der erste DataStore-Wert gelesen wurde
    // verhindert schwarzen Bildschirm / falschen Start-Screen
    val isLoading: StateFlow<Boolean> = session.isLoggedIn
        .map { false }                          // sobald ein Wert kommt → fertig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            session.saveSession(
                userId = "demo-user",
                email  = "demo@allerpaw.app",
                token  = "jwt-placeholder"
            )
        }
    }

    fun signOut() {
        viewModelScope.launch { session.clearSession() }
    }
}
