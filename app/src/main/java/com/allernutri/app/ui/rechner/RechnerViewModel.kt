package com.allernutri.app.ui.rechner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allernutri.app.data.local.entity.HundEntity
import com.allernutri.app.data.repository.HundRepository
import com.allernutri.app.domain.EnergieBedarf
import com.allernutri.app.domain.NaehrstoffErgebnis
import com.allernutri.app.domain.RezeptAnalyseUseCase
import com.allernutri.app.util.FloatParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RechnerUiState(
    val hunde: List<HundEntity>          = emptyList(),
    val selectedHundId: Long?            = null,

    // Energie
    val aktivitaetsFaktor: Float         = 1.6f,
    val rerKcal: Double?                 = null,
    val merKcal: Double?                 = null,

    // Manueller Kcal-Override
    val kcalManuellInput: String         = "",   // Eingabe-Puffer (leer = nicht aktiv)
    val kcalManuellAktiv: Boolean        = false, // true = Wert persistiert im Hund

    // Aktiver Kcal-Wert (MER oder Manuell)
    val effektiverKcal: Double?          = null,

    val ergebnisse: List<NaehrstoffErgebnis> = emptyList(),

    // Rezept-Tabs
    val rezepte: List<com.allernutri.app.data.local.entity.RezeptEntity> = emptyList(),
    val selectedRezeptId: Long?          = null,
    val aktuellerTab: RechnerTab         = RechnerTab.REZEPTE,
    val umstellungsplan: List<UmstellungsTag>? = null,
    val editRezept: com.allernutri.app.data.local.entity.RezeptEntity? = null
)

data class UmstellungsTag(val tagNr: Int, val anteilB: Float)

enum class RechnerTab(val label: String) {
    REZEPTE("Rezepte"), NRC("NRC"), UMSTELLUNG("Umstellung"), ENERGIEBEDARF("Energie")
}

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
        val kcalManuell = hund.kcalBedarfManuell
        _state.update { it.copy(
            kcalManuellInput = kcalManuell?.toLong()?.toString() ?: "",
            kcalManuellAktiv = kcalManuell != null
        ) }
        recalculate(hund, _state.value.aktivitaetsFaktor, kcalManuell)
    }

    fun selectTab(tab: RechnerTab) = _state.update { it.copy(aktuellerTab = tab) }

    fun selectRezept(id: Long) = _state.update { it.copy(selectedRezeptId = id) }

    fun newRezept() = _state.update {
        it.copy(editRezept = com.allernutri.app.data.local.entity.RezeptEntity(
            hundId = it.selectedHundId ?: return@update it, name = ""))
    }

    fun dismissEdit() = _state.update { it.copy(editRezept = null) }

    fun saveRezept(
        rezept: com.allernutri.app.data.local.entity.RezeptEntity,
        zutaten: List<com.allernutri.app.data.local.entity.RezeptZutatEntity>
    ) = viewModelScope.launch {
        _state.update { it.copy(editRezept = null) }
    }

    fun berechneUmstellungsplan(rezeptBId: Long?, tage: Int, schnell: Boolean) {
        if (rezeptBId == null) return
        val plan = (1..tage).map { tag ->
            val anteil = if (schnell) tag.toFloat() / tage
            else (tag.toFloat() / tage).let { t -> t * t }
            UmstellungsTag(tag, anteil.coerceIn(0f, 1f))
        }
        _state.update { it.copy(umstellungsplan = plan) }
    }

    // ── Aktivitätsfaktor ──────────────────────────────────────────────────
    fun setAktivitaetsFaktor(faktor: Float) {
        val hund = aktuellerHund() ?: return
        recalculate(hund, faktor, aktiverKcalManuell())
    }

    // ── Manueller Kcal-Bedarf ──────────────────────────────────────────────
    fun setKcalManuellInput(input: String) {
        _state.update { it.copy(kcalManuellInput = input) }
    }

    /**
     * Aktiviert den manuellen Kcal-Wert und speichert ihn im Hund-Profil.
     * Der Wert ersetzt den MER dauerhaft bis er zurückgesetzt wird.
     */
    fun aktiviereKcalManuell() = viewModelScope.launch {
        val hund  = aktuellerHund() ?: return@launch
        val kcal  = FloatParser.parse(_state.value.kcalManuellInput) ?: return@launch
        hundRepo.updateKcalBedarfManuell(hund.id, kcal)
        _state.update { it.copy(kcalManuellAktiv = true) }
        recalculate(hund, _state.value.aktivitaetsFaktor, kcal)
    }

    /**
     * Setzt manuellen Kcal-Wert zurück → RER/MER-Berechnung wird wieder verwendet.
     */
    fun resetKcalManuell() = viewModelScope.launch {
        val hund = aktuellerHund() ?: return@launch
        hundRepo.updateKcalBedarfManuell(hund.id, null)
        _state.update { it.copy(kcalManuellInput = "", kcalManuellAktiv = false) }
        recalculate(hund, _state.value.aktivitaetsFaktor, null)
    }

    // ── Intern ────────────────────────────────────────────────────────────
    private fun aktuellerHund(): HundEntity? =
        _state.value.hunde.find { it.id == _state.value.selectedHundId }

    private fun aktiverKcalManuell(): Double? =
        if (_state.value.kcalManuellAktiv)
            FloatParser.parse(_state.value.kcalManuellInput)
        else null

    private fun recalculate(hund: HundEntity, faktor: Float, kcalManuell: Double?) {
        val rer          = EnergieBedarf.rer(hund.gewichtKg)
        val mer          = EnergieBedarf.mer(hund.gewichtKg, faktor.toDouble())
        val effektivKcal = kcalManuell ?: mer
        _state.update {
            it.copy(
                selectedHundId    = hund.id,
                aktivitaetsFaktor = faktor,
                rerKcal           = rer,
                merKcal           = mer,
                effektiverKcal    = effektivKcal,
                ergebnisse        = emptyList()
            )
        }
    }
}
