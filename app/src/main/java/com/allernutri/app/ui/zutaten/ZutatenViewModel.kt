package com.allernutri.app.ui.zutaten

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allernutri.app.data.local.entity.ZutatEntity
import com.allernutri.app.data.local.entity.ZutatNaehrstoffEntity
import com.allernutri.app.data.repository.ZutatenRepository
import com.allernutri.app.util.UndoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ZutatenFilter(
    val typ: String      = "",      // "" = alle | "lebensmittel" | "supplement"
    val kategorie: String = "",     // "" = alle | z.B. "Fleisch"
    val perMode: String  = ""       // "" = alle | "tablette" | "tropfen" | "100g" | "pulver"
)

data class ZutatenUiState(
    val zutaten: List<ZutatEntity>               = emptyList(),
    val editZutat: ZutatEntity?                  = null,
    val naehrstoffDialogZutat: ZutatEntity?      = null,
    val naehrstoffe: List<ZutatNaehrstoffEntity> = emptyList(),
    val suchbegriff: String                      = "",
    val filter: ZutatenFilter                    = ZutatenFilter(),
    val filterPanelOffen: Boolean                = false
)

@HiltViewModel
class ZutatenViewModel @Inject constructor(
    private val repo: ZutatenRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ZutatenUiState())
    val state: StateFlow<ZutatenUiState> = _state.asStateFlow()

    val undoManager = UndoManager<Long>(viewModelScope) { }

    init {
        viewModelScope.launch {
            repo.alleZutaten().collect { list ->
                _state.update { it.copy(zutaten = list) }
            }
        }
    }

    val gefilterteZutaten: StateFlow<List<ZutatEntity>> = state.map { s ->
        var result = s.zutaten

        // Suche
        if (s.suchbegriff.isNotBlank()) {
            result = result.filter {
                it.name.contains(s.suchbegriff, ignoreCase = true) ||
                it.hersteller.contains(s.suchbegriff, ignoreCase = true) ||
                it.kategorie.contains(s.suchbegriff, ignoreCase = true)
            }
        }

        // Filter: Typ
        if (s.filter.typ.isNotBlank()) {
            result = result.filter { it.typ == s.filter.typ }
        }

        // Filter: Kategorie
        if (s.filter.kategorie.isNotBlank()) {
            result = result.filter {
                it.kategorie.contains(s.filter.kategorie, ignoreCase = true)
            }
        }

        // Filter: perMode
        if (s.filter.perMode.isNotBlank()) {
            result = result.filter { it.perMode == s.filter.perMode }
        }

        result.sortedBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Alle vorhandenen Kategorien für Filter-Chips */
    val verfuegbareKategorien: StateFlow<List<String>> = state.map { s ->
        s.zutaten.map { it.kategorie }.filter { it.isNotBlank() }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun suche(q: String) = _state.update { it.copy(suchbegriff = q) }

    fun setFilter(filter: ZutatenFilter) = _state.update { it.copy(filter = filter) }

    fun toggleFilterPanel() = _state.update { it.copy(filterPanelOffen = !it.filterPanelOffen) }

    fun clearFilter() = _state.update {
        it.copy(filter = ZutatenFilter(), suchbegriff = "", filterPanelOffen = false)
    }

    fun editNew() = _state.update {
        it.copy(editZutat = ZutatEntity(name = "", typ = "lebensmittel"))
    }

    fun editExisting(z: ZutatEntity) = _state.update { it.copy(editZutat = z) }
    fun dismissEdit() = _state.update { it.copy(editZutat = null) }

    fun save(zutat: ZutatEntity) = viewModelScope.launch {
        repo.upsert(zutat)
        _state.update { it.copy(editZutat = null) }
    }

    fun delete(id: Long, name: String) {
        viewModelScope.launch { repo.delete(id) }
        undoManager.push(id, "„$name" gelöscht")
    }

    fun undoDelete(id: Long) = viewModelScope.launch {
        repo.getById(id)?.let { repo.upsert(it.copy(deleted = false, deletedAt = null)) }
    }

    fun openNaehrstoffe(zutat: ZutatEntity) = viewModelScope.launch {
        val naehrstoffe = repo.getNaehrstoffe(zutat.id)
        _state.update { it.copy(naehrstoffDialogZutat = zutat, naehrstoffe = naehrstoffe) }
    }

    fun dismissNaehrstoffe() = _state.update {
        it.copy(naehrstoffDialogZutat = null, naehrstoffe = emptyList())
    }

    fun saveNaehrstoffe(zutatId: Long, list: List<ZutatNaehrstoffEntity>) = viewModelScope.launch {
        repo.saveNaehrstoffe(zutatId, list)
        _state.update { it.copy(naehrstoffDialogZutat = null, naehrstoffe = emptyList()) }
    }
}
