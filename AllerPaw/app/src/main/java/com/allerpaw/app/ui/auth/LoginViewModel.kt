package com.allerpaw.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allerpaw.app.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val session: SessionRepository
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = session.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Simulated Google Sign-In – replace with Credential Manager flow */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            // TODO: call backend /auth/google with idToken → receive JWT
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
