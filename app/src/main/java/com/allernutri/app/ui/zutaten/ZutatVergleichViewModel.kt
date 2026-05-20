package com.allernutri.app.ui.zutaten

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allernutri.app.data.local.entity.ZutatEntity
import com.allernutri.app.data.local.entity.ZutatNaehrstoffEntity
import com.allernutri.app.data.repository.ZutatenRepository
import com.allernutri.app.domain.NaehrstoffKatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NaehrstoffZeile(
    val key: String,
    val label: String,
    val gruppe: String,
    val einheit: String,
    val wertA: Double?,    // Zutat A (per 100g)
    val wertB: Double?,    // Zutat B (per 100g)
    val delta: Double?,    // B - A
    val deltaProzent: Double?  // (B - A) / A × 100
)

data class ZutatVergleichUiState(
    val alleZutaten: List<ZutatEntity> = emptyList(),
    val zutatA: ZutatEntity? = null,
    val zutatB: ZutatEntity? = null,
    val naehrstoffeA: Map<String, Double> = emptyMap(),
    val naehrstoffeB: Map<String, Double> = emptyMap(),
    val vergleichsZeilen: List<NaehrstoffZeile> = emptyList(),
    val suchA: String = "",
    val suchB: String = "",
    val isLoading: Boolean = false,
    // Filter: nur Zeilen mit Unterschied anzeigen
    val nurUnterschiede: Boolean = false,
    val nurMitWerten: Boolean = true
)

@HiltViewModel
class ZutatVergleichViewModel @Inject constructor(
    private val zutatenRepo: ZutatenRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ZutatVergleichUiState())
    val state: StateFlow<ZutatVergleichUiState> = _state.asStateFlow()

    init {
        zutatenRepo.alleZutaten()
            .onEach { zutaten -> _state.update { it.copy(alleZutaten = zutaten) } }
            .launchIn(viewModelScope)
    }

    fun setSuchA(q: String) = _state.update { it.copy(suchA = q) }
    fun setSuchB(q: String) = _state.update { it.copy(suchB = q) }
    fun toggleNurUnterschiede() = _state.update {
        it.copy(nurUnterschiede = !it.nurUnterschiede).also { s -> berechne(s) }
    }
    fun toggleNurMitWerten() = _state.update {
        it.copy(nurMitWerten = !it.nurMitWerten).also { s -> berechne(s) }
    }

    fun selectZutatA(zutat: ZutatEntity) = viewModelScope.launch {
        val naehrstoffe = ladeNaehrstoffe(zutat.id)
        _state.update { s ->
            val neu = s.copy(zutatA = zutat, naehrstoffeA = naehrstoffe, suchA = "")
            berechne(neu)
        }
    }

    fun selectZutatB(zutat: ZutatEntity) = viewModelScope.launch {
        val naehrstoffe = ladeNaehrstoffe(zutat.id)
        _state.update { s ->
            val neu = s.copy(zutatB = zutat, naehrstoffeB = naehrstoffe, suchB = "")
            berechne(neu)
        }
    }

    fun tausche() {
        _state.update { s ->
            val neu = s.copy(
                zutatA       = s.zutatB,
                zutatB       = s.zutatA,
                naehrstoffeA = s.naehrstoffeB,
                naehrstoffeB = s.naehrstoffeA
            )
            berechne(neu)
        }
    }

    fun clearZutatA() = _state.update { s ->
        val neu = s.copy(zutatA = null, naehrstoffeA = emptyMap())
        berechne(neu)
    }

    fun clearZutatB() = _state.update { s ->
        val neu = s.copy(zutatB = null, naehrstoffeB = emptyMap())
        berechne(neu)
    }

    private suspend fun ladeNaehrstoffe(zutatId: Long): Map<String, Double> =
        zutatenRepo.getNaehrstoffe(zutatId).associate { it.naehrstoffKey to it.wertPer100g }

    private fun berechne(state: ZutatVergleichUiState): ZutatVergleichUiState {
        val zeilen = NaehrstoffKatalog.alle
            .groupBy { it.gruppe }
            .flatMap { (_, naehrstoffe) ->
                naehrstoffe.map { naehrstoff ->
                    val a     = state.naehrstoffeA[naehrstoff.key]
                    val b     = state.naehrstoffeB[naehrstoff.key]
                    val delta = if (a != null && b != null) b - a else null
                    val pct   = if (a != null && b != null && a > 0)
                        (b - a) / a * 100.0 else null

                    NaehrstoffZeile(
                        key          = naehrstoff.key,
                        label        = naehrstoff.label,
                        gruppe       = naehrstoff.gruppe,
                        einheit      = naehrstoff.einheit,
                        wertA        = a,
                        wertB        = b,
                        delta        = delta,
                        deltaProzent = pct
                    )
                }
            }
            .filter { zeile ->
                val hatWert = zeile.wertA != null || zeile.wertB != null
                val hatUnterschied = zeile.delta != null && kotlin.math.abs(zeile.delta) > 0.001
                val nurMitWertenOk = !state.nurMitWerten || hatWert
                val nurUnterschiedeOk = !state.nurUnterschiede || hatUnterschied
                nurMitWertenOk && nurUnterschiedeOk
            }

        return state.copy(vergleichsZeilen = zeilen)
    }
}
