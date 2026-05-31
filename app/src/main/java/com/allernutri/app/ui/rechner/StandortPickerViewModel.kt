package com.allernutri.app.ui.rechner

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

data class StandortPickerState(
    val isLoading: Boolean = false,
    val fehler: String?    = null
)

@HiltViewModel
class StandortPickerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(StandortPickerState())
    val state: StateFlow<StandortPickerState> = _state.asStateFlow()

    fun geocodeStadt(
        stadtName: String,
        onResult: (lat: Double, lon: Double, name: String) -> Unit
    ) = viewModelScope.launch {
        if (stadtName.isBlank()) return@launch
        _state.update { it.copy(isLoading = true, fehler = null) }
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val results = withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(stadtName, 1)
            }
            if (results.isNullOrEmpty()) {
                _state.update { it.copy(isLoading = false, fehler = "Ort nicht gefunden") }
            } else {
                val addr = results[0]
                _state.update { it.copy(isLoading = false, fehler = null) }
                onResult(addr.latitude, addr.longitude, addr.locality ?: stadtName)
            }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, fehler = "Geocoding fehlgeschlagen: ${e.message}") }
        }
    }

    @SuppressLint("MissingPermission")
    fun holeAktuellenStandort(
        onResult: (lat: Double, lon: Double, name: String) -> Unit
    ) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, fehler = null) }
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE)
                    as android.location.LocationManager
            val provider = android.location.LocationManager.NETWORK_PROVIDER
            val location = withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                locationManager.getLastKnownLocation(provider)
            }
            if (location == null) {
                _state.update { it.copy(isLoading = false, fehler = "Standort nicht verfügbar") }
            } else {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addr = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        ?.firstOrNull()
                }
                val name = addr?.locality ?: "${location.latitude},${location.longitude}"
                _state.update { it.copy(isLoading = false) }
                onResult(location.latitude, location.longitude, name)
            }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, fehler = "GPS-Fehler: ${e.message}") }
        }
    }
}
