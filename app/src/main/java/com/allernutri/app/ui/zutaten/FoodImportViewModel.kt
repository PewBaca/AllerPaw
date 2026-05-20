package com.allernutri.app.ui.zutaten

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allernutri.app.data.remote.FoodApiResult
import com.allernutri.app.data.remote.FoodApiResponse
import com.allernutri.app.data.remote.FoodQuelle
import com.allernutri.app.data.repository.FoodApiRepository
import com.allernutri.app.data.repository.FoodApiSettings
import com.allernutri.app.data.repository.ImportResult
import com.allernutri.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FoodImportUiState(
    val sucheQuery: String              = "",
    val isSearching: Boolean            = false,
    val ergebnisse: List<FoodApiResult> = emptyList(),
    val selected: FoodApiResult?        = null,
    val isImporting: Boolean            = false,
    val importErfolgreich: String?      = null,   // Zutat-Name nach Erfolg
    val fehler: String?                 = null,
    // Aktive Quellen
    val aktivQuellen: Set<FoodQuelle>   = FoodQuelle.entries.toSet(),
    // API-Key-Status
    val usdaKeyVorhanden: Boolean       = false,
    val edamamKeyVorhanden: Boolean     = false,
    // Detail laden
    val isLoadingDetail: Boolean        = false
)

@HiltViewModel
class FoodImportViewModel @Inject constructor(
    private val foodRepo: FoodApiRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FoodImportUiState())
    val state: StateFlow<FoodImportUiState> = _state.asStateFlow()

    private var settings = FoodApiSettings()

    init {
        viewModelScope.launch {
            val usdaKey    = settingsRepo.getString("usda_api_key", "")
            val edamamId   = settingsRepo.getString("edamam_app_id", "")
            val edamamKey  = settingsRepo.getString("edamam_app_key", "")
            settings = FoodApiSettings(usdaKey, edamamId, edamamKey)
            _state.update { it.copy(
                usdaKeyVorhanden   = usdaKey.isNotBlank(),
                edamamKeyVorhanden = edamamId.isNotBlank() && edamamKey.isNotBlank()
            ) }
        }
    }

    fun setQuery(q: String) = _state.update { it.copy(sucheQuery = q, fehler = null) }

    fun toggleQuelle(quelle: FoodQuelle) {
        val aktuell = _state.value.aktivQuellen.toMutableSet()
        if (quelle in aktuell) aktuell.remove(quelle) else aktuell.add(quelle)
        _state.update { it.copy(aktivQuellen = aktuell) }
    }

    fun suche() = viewModelScope.launch {
        val query = _state.value.sucheQuery.trim()
        if (query.isBlank()) return@launch
        _state.update { it.copy(isSearching = true, ergebnisse = emptyList(), fehler = null, selected = null) }
        when (val resp = foodRepo.sucheAlle(query, settings, _state.value.aktivQuellen)) {
            is FoodApiResponse.Success -> _state.update { it.copy(isSearching = false, ergebnisse = resp.ergebnisse) }
            is FoodApiResponse.Error   -> _state.update { it.copy(isSearching = false, fehler = resp.message) }
            FoodApiResponse.Empty      -> _state.update { it.copy(isSearching = false, fehler = "Keine Ergebnisse gefunden.") }
        }
    }

    fun sucheBarcode(barcode: String) = viewModelScope.launch {
        _state.update { it.copy(isSearching = true, ergebnisse = emptyList(), fehler = null, selected = null) }
        when (val resp = foodRepo.sucheBarcode(barcode)) {
            is FoodApiResponse.Success -> {
                val first = resp.ergebnisse.firstOrNull()
                _state.update { it.copy(isSearching = false, ergebnisse = resp.ergebnisse, selected = first) }
            }
            is FoodApiResponse.Error   -> _state.update { it.copy(isSearching = false, fehler = resp.message) }
            FoodApiResponse.Empty      -> _state.update { it.copy(isSearching = false, fehler = "Produkt nicht gefunden.") }
        }
    }

    fun selectErgebnis(result: FoodApiResult) {
        _state.update { it.copy(selected = result) }
        // Bei USDA Detail-Daten nachladen (mehr Nährstoffe)
        if (result.quelle == FoodQuelle.USDA && result.fdcId.isNotBlank()) {
            viewModelScope.launch {
                _state.update { it.copy(isLoadingDetail = true) }
                when (val detail = foodRepo.detailUsda(result.fdcId, settings)) {
                    is FoodApiResponse.Success ->
                        _state.update { it.copy(selected = detail.ergebnisse.first(), isLoadingDetail = false) }
                    else ->
                        _state.update { it.copy(isLoadingDetail = false) }
                }
            }
        }
    }

    fun importiere() = viewModelScope.launch {
        val result = _state.value.selected ?: return@launch
        _state.update { it.copy(isImporting = true, fehler = null) }
        when (val import = foodRepo.importiereAlsZutat(result)) {
            is ImportResult.Success ->
                _state.update { it.copy(isImporting = false, importErfolgreich = import.zutatName, selected = null) }
            is ImportResult.Error ->
                _state.update { it.copy(isImporting = false, fehler = import.message) }
        }
    }

    fun clearFehler()   = _state.update { it.copy(fehler = null) }
    fun clearErfolg()   = _state.update { it.copy(importErfolgreich = null) }
    fun clearSelected() = _state.update { it.copy(selected = null) }
}
