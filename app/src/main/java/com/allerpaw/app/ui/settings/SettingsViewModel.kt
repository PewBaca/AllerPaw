package com.allerpaw.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allerpaw.app.data.repository.AuthRepository
import com.allerpaw.app.data.repository.SessionRepository
import com.allerpaw.app.data.repository.SettingsRepository
import com.allerpaw.app.util.LocaleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val userEmail: String?   = null,
    val sprache: String      = "de",
    val standortLat: Double  = SettingsRepository.DEFAULT_LAT,
    val standortLon: Double  = SettingsRepository.DEFAULT_LON,
    val latInput: String     = SettingsRepository.DEFAULT_LAT.toString(),
    val lonInput: String     = SettingsRepository.DEFAULT_LON.toString(),
    val ieAnzeige: String    = "metrisch"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val session: SessionRepository,
    private val settings: SettingsRepository,
    private val auth: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                session.userEmail,
                settings.sprache,
                settings.standortLat,
                settings.standortLon,
                settings.ieAnzeige
            ) { email, sprache, lat, lon, ie ->
                SettingsUiState(
                    userEmail   = email,
                    sprache     = sprache,
                    standortLat = lat,
                    standortLon = lon,
                    latInput    = lat.toString(),
                    lonInput    = lon.toString(),
                    ieAnzeige   = ie
                )
            }.collect { _state.value = it }
        }
    }

    fun signOut() = viewModelScope.launch { auth.signOut() }

    fun setSprache(tag: String, ctx: Context) {
        viewModelScope.launch {
            settings.setSprache(tag)
            LocaleHelper.setLocale(ctx, tag)
        }
    }

    fun setLatInput(v: String) = _state.update { it.copy(latInput = v) }
    fun setLonInput(v: String) = _state.update { it.copy(lonInput = v) }

    fun saveStandort() = viewModelScope.launch {
        val lat = _state.value.latInput.toDoubleOrNull() ?: return@launch
        val lon = _state.value.lonInput.toDoubleOrNull() ?: return@launch
        settings.setStandort(lat, lon)
    }

    fun setIeAnzeige(modus: String) = viewModelScope.launch {
        settings.setIeAnzeige(modus)
    }

    fun onBackupClick() { /* ExportViewModel.exportBackup() → via shared state */ }
    fun onRestoreClick() { /* TODO Phase 5+ */ }
}
