package com.allernutri.app.ui.tagebuch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allernutri.app.data.local.entity.HundEntity
import com.allernutri.app.data.local.entity.TagebuchAllergenEntity
import com.allernutri.app.data.repository.HundRepository
import com.allernutri.app.data.repository.TagebuchRepository
import com.allernutri.app.domain.KreuzallergenAnalyse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class KreuzallergenUiState(
    val hunde: List<HundEntity>                         = emptyList(),
    val selectedHundId: Long?                           = null,
    val allergene: List<TagebuchAllergenEntity>         = emptyList(),
    val ergebnis: KreuzallergenAnalyse.AnalyseErgebnis? = null,
    val isLoading: Boolean                              = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class KreuzallergenViewModel @Inject constructor(
    private val hundRepo: HundRepository,
    private val tagebuchRepo: TagebuchRepository
) : ViewModel() {

    private val _state = MutableStateFlow(KreuzallergenUiState())
    val state: StateFlow<KreuzallergenUiState> = _state.asStateFlow()

    init {
        // Hunde laden
        hundRepo.alleHunde()
            .onEach { hunde ->
                val selectedId = _state.value.selectedHundId ?: hunde.firstOrNull()?.id
                _state.update { it.copy(hunde = hunde, selectedHundId = selectedId) }
            }
            .launchIn(viewModelScope)

        // Allergene beobachten und Analyse anstoßen
        _state
            .map { it.selectedHundId }
            .filterNotNull()
            .distinctUntilChanged()
            .flatMapLatest { hundId ->
                tagebuchRepo.allergene(hundId)
            }
            .onEach { allergene ->
                val ergebnis = KreuzallergenAnalyse.analysiere(allergene)
                _state.update { it.copy(allergene = allergene, ergebnis = ergebnis) }
            }
            .launchIn(viewModelScope)
    }

    fun selectHund(hundId: Long) = _state.update { it.copy(selectedHundId = hundId) }
}
