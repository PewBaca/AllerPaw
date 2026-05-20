package com.allernutri.app.ui.tagebuch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allernutri.app.data.remote.dto.PollenDaten
import com.allernutri.app.data.remote.dto.WetterDaten
import com.allernutri.app.data.repository.SettingsRepository
import com.allernutri.app.data.repository.WetterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WetterUiState(
    val lat: Double                       = SettingsRepository.DEFAULT_LAT,
    val lon: Double                       = SettingsRepository.DEFAULT_LON,
    val standortName: String              = "",
    val wetter: WetterDaten?              = null,
    val pollen: Map<String, List<PollenDaten>> = emptyMap(),
    val isLoading: Boolean                = false,
    val fehler: String?                   = null
)

@HiltViewModel
class WetterViewModel @Inject constructor(
    private val wetterRepo: WetterRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WetterUiState())
    val state: StateFlow<WetterUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(settings.standortLat, settings.standortLon) { lat, lon -> lat to lon }
                .collect { (lat, lon) ->
                    _state.update { it.copy(lat = lat, lon = lon) }
                }
        }
    }

    fun setStandort(lat: Double, lon: Double, name: String) {
        viewModelScope.launch {
            settings.setStandort(lat, lon)
            _state.update { it.copy(lat = lat, lon = lon, standortName = name) }
        }
    }

    fun ladeWetterUndPollen() = viewModelScope.launch {
        val lat = _state.value.lat
        val lon = _state.value.lon
        _state.update { it.copy(isLoading = true, fehler = null) }

        val wetter = wetterRepo.getWetter(java.time.LocalDate.now(), lat, lon)
        val pollen = wetterRepo.getPollen(lat, lon)

        if (wetter == null && pollen.isEmpty()) {
            _state.update { it.copy(
                isLoading = false,
                fehler    = "Keine Wetterdaten verfügbar. Bitte Internetverbindung prüfen."
            ) }
        } else {
            _state.update { it.copy(
                isLoading = false,
                wetter    = wetter,
                pollen    = pollen
            ) }
        }
    }
}
