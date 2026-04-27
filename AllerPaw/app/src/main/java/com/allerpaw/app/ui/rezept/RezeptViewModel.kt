package com.allerpaw.app.ui.rezept

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allerpaw.app.data.local.entity.*
import com.allerpaw.app.data.repository.*
import com.allerpaw.app.domain.*
import com.allerpaw.app.util.FloatParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RezeptZutatDraft(
    val id: Long = 0,
    val zutatId: Long? = null,
    val zutatName: String = "",
    val zutatPerMode: String = "100g",       // perMode der Zutat
    val zutatTabGewichtG: Double = 0.0,      // Tablettengewicht in g
    val zutatTropfenGewichtG: Double = 0.0,  // Tropfengewicht in g
    val subRezeptId: Long? = null,
    val subRezeptName: String = "",
    val mengeG: Double = 0.0,                // Immer in Gramm intern
    val anzahlTabletten: Double? = null,
    val anzahlTropfen: Double? = null
) {
    /** Anzeige-String je nach perMode */
    fun anzeigeText(): String = when (zutatPerMode) {
        "tablette" -> "${anzahlTabletten ?: 0.0} Tbl. (${String.format("%.1f", mengeG)} g)"
        "tropfen"  -> "${anzahlTropfen ?: 0.0} Tr. (${String.format("%.2f", mengeG)} g)"
        "pulver"   -> "${String.format("%.1f", mengeG)} g"
        else       -> "${String.format("%.1f", mengeG)} g"
    }
}

data class RezeptEditorState(
    val hunde: List<HundEntity> = emptyList(),
    val selectedHundId: Long? = null,
    val alleZutaten: List<ZutatEntity> = emptyList(),
    val alleRezepte: List<RezeptEntity> = emptyList(),

    // Aktives Rezept
    val rezept: RezeptEntity? = null,
    val zutatenDraft: List<RezeptZutatDraft> = emptyList(),
    val skalierung: Float = 1.0f,

    // Analyse
    val ergebnisse: List<NaehrstoffErgebnis> = emptyList(),
    val kcalGesamt: Double = 0.0,
    val gesamtGramm: Double = 0.0,
    val caPVerhaeltnis: Double? = null,
    val omega63: Double? = null,
    val kochverlustFaktor: Double = 0.70,

    // Vergleichs-Rezept
    val vergleichsRezeptId: Long? = null,
    val vergleichsErgebnisse: List<NaehrstoffErgebnis> = emptyList(),

    val isLoading: Boolean = false
)

@HiltViewModel
class RezeptViewModel @Inject constructor(
    private val hundRepo: HundRepository,
    private val rezeptRepo: RezeptRepository,
    private val analyseUseCase: RezeptAnalyseUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RezeptEditorState())
    val state: StateFlow<RezeptEditorState> = _state.asStateFlow()

    private val loader = object : RezeptResolver.Loader {
        override suspend fun loadZutaten(rezeptId: Long) = rezeptRepo.getZutaten(rezeptId)
        override suspend fun loadZutat(zutatId: Long)    = rezeptRepo.getZutatById(zutatId)
        override suspend fun loadNaehrstoffe(zutatId: Long) = rezeptRepo.getNaehrstoffeForZutat(zutatId)
        override suspend fun loadRezept(rezeptId: Long)  = rezeptRepo.getById(rezeptId)
    }

    init {
        viewModelScope.launch {
            hundRepo.alleHunde().collect { hunde ->
                _state.update { it.copy(hunde = hunde,
                    selectedHundId = it.selectedHundId ?: hunde.firstOrNull()?.id) }
            }
        }
        viewModelScope.launch {
            val kochverlust = rezeptRepo.getParameter("kochverlust_b_vitamine", "0.30").toDoubleOrNull() ?: 0.30
            _state.update { it.copy(kochverlustFaktor = 1.0 - kochverlust) }
        }
    }

    fun selectHund(id: Long) {
        _state.update { it.copy(selectedHundId = id) }
        loadRezepte(id)
    }

    private fun loadRezepte(hundId: Long) = viewModelScope.launch {
        val rezepte = rezeptRepo.alleRezepte().filter { it.hundId == hundId }
        _state.update { it.copy(alleRezepte = rezepte) }
    }

    fun selectRezept(rezept: RezeptEntity) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, rezept = rezept) }
        val positionen = rezeptRepo.getZutaten(rezept.id)
        val draft = positionen.map { pos ->
            val zutat = pos.zutatId?.let { rezeptRepo.getZutatById(it) }
            RezeptZutatDraft(
                id             = pos.id,
                zutatId        = pos.zutatId,
                zutatName      = zutat?.name ?: "",
                subRezeptId    = pos.subRezeptId,
                mengeG         = pos.mengeG,
                anzahlTabletten = pos.anzahlTabletten
            )
        }
        _state.update { it.copy(zutatenDraft = draft, isLoading = false) }
        analyseRezept(rezept)
    }

    fun setSkalierung(faktor: Float) {
        _state.update { it.copy(skalierung = faktor) }
        _state.value.rezept?.let { analyseRezept(it) }
    }

    fun setVergleichsRezept(rezeptId: Long?) = viewModelScope.launch {
        _state.update { it.copy(vergleichsRezeptId = rezeptId) }
        if (rezeptId == null) {
            _state.update { it.copy(vergleichsErgebnisse = emptyList()) }
            return@launch
        }
        val vergleichsRezept = rezeptRepo.getById(rezeptId) ?: return@launch
        val hund = _state.value.hunde.find { it.id == _state.value.selectedHundId } ?: return@launch
        val mer  = EnergieBedarf.mer(hund.gewichtKg)
        val result = RezeptResolver.resolve(
            rezeptId          = rezeptId,
            skalierung        = _state.value.skalierung.toDouble(),
            gekocht           = vergleichsRezept.gekocht,
            kochverlustFaktor = _state.value.kochverlustFaktor,
            loader            = loader
        )
        val inputs = result.zutaten.map { z ->
            RezeptAnalyseUseCase.ZutatInput(z.mengeG, z.naehrstoffe)
        }
        _state.update { it.copy(vergleichsErgebnisse = analyseUseCase.analyse(inputs, mer)) }
    }

    private fun analyseRezept(rezept: RezeptEntity) = viewModelScope.launch {
        val hund = _state.value.hunde.find { it.id == _state.value.selectedHundId } ?: return@launch
        val mer  = EnergieBedarf.mer(hund.gewichtKg)
        val result = RezeptResolver.resolve(
            rezeptId          = rezept.id,
            skalierung        = _state.value.skalierung.toDouble(),
            gekocht           = rezept.gekocht,
            kochverlustFaktor = _state.value.kochverlustFaktor,
            loader            = loader
        )
        val inputs = result.zutaten.map { z ->
            RezeptAnalyseUseCase.ZutatInput(z.mengeG, z.naehrstoffe)
        }
        val ergebnisse = analyseUseCase.analyse(inputs, mer)

        // Ca:P und Omega 6:3
        val calcium   = result.zutaten.sumOf { z -> (z.naehrstoffe["calcium"] ?: 0.0) * z.mengeG / 100.0 }
        val phosphor  = result.zutaten.sumOf { z -> (z.naehrstoffe["phosphor"] ?: 0.0) * z.mengeG / 100.0 }
        val la        = result.zutaten.sumOf { z -> (z.naehrstoffe["linolsaeure"] ?: 0.0) * z.mengeG / 100.0 }
        val ala       = result.zutaten.sumOf { z -> (z.naehrstoffe["alpha_linolen"] ?: 0.0) * z.mengeG / 100.0 }
        val epaDha    = result.zutaten.sumOf { z -> (z.naehrstoffe["epa_dha"] ?: 0.0) * z.mengeG / 100.0 }
        val omega3    = ala + epaDha / 1000.0  // EPA+DHA in mg → g

        _state.update { it.copy(
            ergebnisse     = ergebnisse,
            kcalGesamt     = result.kcalGesamt,
            gesamtGramm    = result.gesamtGrammRoh,
            caPVerhaeltnis = if (phosphor > 0) calcium / phosphor else null,
            omega63        = if (omega3 > 0) la / omega3 else null,
            isLoading      = false
        ) }
    }

    // ── Rezept CRUD ───────────────────────────────────────────────────────
    fun neuesRezept() {
        val hundId = _state.value.selectedHundId ?: return
        _state.update { it.copy(
            rezept       = RezeptEntity(hundId = hundId, name = ""),
            zutatenDraft = emptyList(),
            ergebnisse   = emptyList()
        ) }
    }

    fun saveRezept(rezept: RezeptEntity, zutaten: List<RezeptZutatDraft>) = viewModelScope.launch {
        val id = rezeptRepo.upsert(rezept)
        val entities = zutaten.mapIndexed { i, d ->
            RezeptZutatEntity(
                rezeptId        = id,
                zutatId         = d.zutatId,
                subRezeptId     = d.subRezeptId,
                mengeG          = d.mengeG,
                anzahlTabletten = d.anzahlTabletten,
                reihenfolge     = i
            )
        }
        rezeptRepo.saveZutaten(id, entities)
        _state.value.selectedHundId?.let { loadRezepte(it) }
    }

    fun deleteRezept(id: Long) = viewModelScope.launch {
        rezeptRepo.delete(id)
        _state.value.selectedHundId?.let { loadRezepte(it) }
        if (_state.value.rezept?.id == id) _state.update { it.copy(rezept = null, ergebnisse = emptyList()) }
    }
}
