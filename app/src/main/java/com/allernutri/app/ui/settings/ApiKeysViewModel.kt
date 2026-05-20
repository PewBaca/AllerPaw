package com.allernutri.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allernutri.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ApiKeysUiState(
    val usdaKey: String      = "",
    val edamamAppId: String  = "",
    val edamamAppKey: String = "",
    val savedMessage: String? = null
)

@HiltViewModel
class ApiKeysViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ApiKeysUiState())
    val state: StateFlow<ApiKeysUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(
                usdaKey      = settingsRepo.getString("usda_api_key"),
                edamamAppId  = settingsRepo.getString("edamam_app_id"),
                edamamAppKey = settingsRepo.getString("edamam_app_key")
            ) }
        }
    }

    fun saveUsdaKey(key: String) = viewModelScope.launch {
        settingsRepo.setString("usda_api_key", key.trim())
        _state.update { it.copy(usdaKey = key.trim(), savedMessage = "USDA API-Key gespeichert.") }
        clearMessage()
    }

    fun saveEdamamKeys(appId: String, appKey: String) = viewModelScope.launch {
        settingsRepo.setString("edamam_app_id",  appId.trim())
        settingsRepo.setString("edamam_app_key", appKey.trim())
        _state.update { it.copy(
            edamamAppId  = appId.trim(),
            edamamAppKey = appKey.trim(),
            savedMessage = "Edamam-Keys gespeichert."
        ) }
        clearMessage()
    }

    private fun clearMessage() = viewModelScope.launch {
        kotlinx.coroutines.delay(3000)
        _state.update { it.copy(savedMessage = null) }
    }
}
