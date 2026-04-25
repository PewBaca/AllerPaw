package com.allerpaw.app.ui.zutaten

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allerpaw.app.data.local.entity.ZutatEntity
import com.allerpaw.app.data.local.entity.ZutatNaehrstoffEntity
import com.allerpaw.app.data.repository.ZutatenRepository
import com.allerpaw.app.util.UndoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ZutatenUiState(
    val zutaten: List<ZutatEntity> = emptyList(),
    val editZutat: ZutatEntity? = null,
    val naehrstoffDialogZutat: ZutatEntity? = null,
    val naehrstoffe: List<ZutatNaehrstoffEntity> = emptyList(),
    val suchbegriff: String = ""
)

@HiltViewModel
class ZutatenViewModel @Inject constructor(
    private val repo: ZutatenRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ZutatenUiState())
    val state: StateFlow<ZutatenUiState> = _state.asStateFlow()

    val undoManager = UndoManager<Long>(viewModelScope) { /* Soft-Delete reicht */ }

    init {
        viewModelScope.launch {
            repo.alleZutaten().collect { list ->
                _state.update { it.copy(zutaten = list) }
            }
        }
    }

    val gefilterteZutaten: StateFlow<List<ZutatEntity>> = state.map { s ->
        if (s.suchbegriff.isBlank()) s.zutaten
        else s.zutaten.filter { it.name.contains(s.suchbegriff, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun suche(q: String) = _state.update { it.copy(suchbegriff = q) }

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
        undoManager.push(id, "„$name“ gelöscht")
    }

    fun undoDelete(id: Long) = viewModelScope.launch {
        undoManager.undo(id)
        // Soft-Delete zurücksetzen: Entity mit deleted=false wieder speichern
        repo.getById(id)?.let { repo.upsert(it.copy(deleted = false, deletedAt = null)) }
    }

    // Nährstoff-Dialog
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
