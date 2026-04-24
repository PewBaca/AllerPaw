package com.allerpaw.app.ui.rechner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allerpaw.app.data.local.entity.HundEntity
import com.allerpaw.app.data.repository.HundRepository
import com.allerpaw.app.domain.EnergieBedarf
import com.allerpaw.app.domain.NaehrstoffErgebnis
import com.allerpaw.app.domain.RezeptAnalyseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RechnerUiState(
    val hunde: List<HundEntity> = emptyList(),
    val selectedHundId: Long? = null,
    val aktivitaetsFaktor: Float = 1.6f,
    val rerKcal: Double? = null,
    val merKcal: Double? = null,
    val ergebnisse: List<NaehrstoffErgebnis> = emptyList()
)

@HiltViewModel
class RechnerViewModel @Inject constructor(
    private val hundRepo: HundRepository,
    private val analyseUseCase: RezeptAnalyseUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RechnerUiState())
    val state: StateFlow<RechnerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            hundRepo.alleHunde().collect { hunde ->
                _state.update { it.copy(hunde = hunde) }
            }
        }
    }

    fun selectHund(hundId: Long) {
        val hund = _state.value.hunde.find { it.id == hundId } ?: return
        recalculate(hund, _state.value.aktivitaetsFaktor)
    }

    fun setAktivitaetsFaktor(faktor: Float) {
        val hundId = _state.value.selectedHundId ?: return
        val hund   = _state.value.hunde.find { it.id == hundId } ?: return
        recalculate(hund, faktor)
    }

    private fun recalculate(hund: HundEntity, faktor: Float) {
        val rer = EnergieBedarf.rer(hund.gewichtKg)
        val mer = EnergieBedarf.mer(hund.gewichtKg, faktor.toDouble())
        // TODO: Rezepte laden und echte Analyse fahren
        _state.update {
            it.copy(
                selectedHundId    = hund.id,
                aktivitaetsFaktor = faktor,
                rerKcal           = rer,
                merKcal           = mer,
                ergebnisse        = emptyList()  // befüllt sobald Rezepte geladen
            )
        }
    }
}
