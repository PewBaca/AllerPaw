package com.allernutri.app.ui.rezept

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allernutri.app.data.local.entity.*
import com.allernutri.app.data.repository.*
import com.allernutri.app.domain.*
import com.allernutri.app.data.local.entity.ToleranzEntity
import com.allernutri.app.data.repository.ToleranzRepository
import com.allernutri.app.domain.NrcLebensphasen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RezeptZutatDraft(
    val id: Long = 0,
    val zutatId: Long? = null,
    val zutatName: String = "",
    val zutatPerMode: String = "100g",
    val zutatTabGewichtG: Double = 0.0,
    val zutatTropfenGewichtG: Double = 0.0,
    val subRezeptId: Long? = null,
    val subRezeptName: String = "",
    val mengeG: Double = 0.0,
    val anzahlTabletten: Double? = null,
    val anzahlTropfen: Double? = null,
    val inhaltsstoffeFreitext: String = ""   // Freitext: Charge, Hersteller, Hinweise
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

    // Kategorie-Filter
    val filterKategorie: String = "",    // "" = alle | "Fleisch" | "Gemüse" etc.
    val filterUnterkat: String  = "",    // "" = alle | "Rind" etc.
    val rezeptSuche: String     = "",

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

    // Lebensphase (NRC-Skalierung des Bedarfs)
    val lebensphase: NrcLebensphasen.Lebensphase = NrcLebensphasen.Lebensphase.ADULT,

    // Manueller Kcal-Override (aus Hund-Profil)
    val kcalBedarfManuellAktiv: Boolean = false,
    val effektiverKcal: Double? = null,

    // Toleranzen
    val toleranzMap: Map<String, ToleranzEntity> = emptyMap(),
    val editToleranz: ToleranzEntity? = null,

    val isLoading: Boolean = false
)

@HiltViewModel
class RezeptViewModel @Inject constructor(
    private val hundRepo: HundRepository,
    private val rezeptRepo: RezeptRepository,
    private val analyseUseCase: RezeptAnalyseUseCase,
    private val toleranzRepo: ToleranzRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RezeptEditorState())
    val state: StateFlow<RezeptEditorState> = _state.asStateFlow()

    private var toleranzJob: kotlinx.coroutines.Job? = null

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
                // Toleranzen für ersten Hund laden
                _state.value.selectedHundId?.let { beobachteToleranzFuerHund(it) }
            }
        }
        viewModelScope.launch {
            val kochverlust = rezeptRepo.getParameter("kochverlust_b_vitamine", "0.30").toDoubleOrNull() ?: 0.30
            _state.update { it.copy(kochverlustFaktor = 1.0 - kochverlust) }
        }
    }

    private fun beobachteToleranzFuerHund(hundId: Long) {
        toleranzJob?.cancel()
        toleranzJob = viewModelScope.launch {
            toleranzRepo.beobachteToleranzMap(hundId).collect { map ->
                _state.update { it.copy(toleranzMap = map) }
            }
        }
    }

    // ── Kategorie-Filter + Suche ──────────────────────────────────────────
    fun setRezeptSuche(q: String) = _state.update { it.copy(rezeptSuche = q) }

    fun setFilterKategorie(haupt: String, unter: String = "") =
        _state.update { it.copy(filterKategorie = haupt, filterUnterkat = unter) }

    fun clearRezeptFilter() = _state.update {
        it.copy(filterKategorie = "", filterUnterkat = "", rezeptSuche = "")
    }

    val gefilterteRezepte: StateFlow<List<RezeptEntity>> = _state.map { s ->
        var result = s.alleRezepte
        if (s.rezeptSuche.isNotBlank()) {
            result = result.filter {
                it.name.contains(s.rezeptSuche, ignoreCase = true) ||
                it.notizen.contains(s.rezeptSuche, ignoreCase = true) ||
                it.kategorie.contains(s.rezeptSuche, ignoreCase = true)
            }
        }
        if (s.filterKategorie.isNotBlank()) {
            result = result.filter {
                RezeptKategorien.hauptkategorie(it.kategorie) == s.filterKategorie
            }
        }
        if (s.filterUnterkat.isNotBlank()) {
            result = result.filter {
                RezeptKategorien.unterkategorie(it.kategorie) == s.filterUnterkat
            }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectHund(id: Long) {
        val hund = _state.value.hunde.find { it.id == id }
        _state.update { it.copy(
            selectedHundId         = id,
            kcalBedarfManuellAktiv = hund?.kcalBedarfManuell != null,
            effektiverKcal         = hund?.kcalBedarfManuell
                ?: hund?.let { h -> EnergieBedarf.mer(h.gewichtKg) }
        ) }
        loadRezepte(id)
        beobachteToleranzFuerHund(id)
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
                id                   = pos.id,
                zutatId              = pos.zutatId,
                zutatName            = zutat?.name ?: "",
                zutatPerMode         = zutat?.perMode ?: "100g",
                zutatTabGewichtG     = zutat?.tabletteGewichtG ?: 0.0,
                zutatTropfenGewichtG = zutat?.tropfenGewichtG ?: 0.0,
                subRezeptId          = pos.subRezeptId,
                subRezeptName        = pos.subRezeptId?.let {
                    rezeptRepo.getById(it)?.name ?: "Rezept #$it"
                } ?: "",
                mengeG               = pos.mengeG,
                anzahlTabletten      = pos.anzahlTabletten,
                anzahlTropfen        = pos.anzahlTropfen,
                inhaltsstoffeFreitext = pos.inhaltsstoffeFreitext
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
        val mer  = hund.kcalBedarfManuell ?: EnergieBedarf.mer(hund.gewichtKg)
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
        _state.update { it.copy(vergleichsErgebnisse = analyseUseCase.analyse(inputs, mer, _state.value.lebensphase)) }
    }

    private fun analyseRezept(rezept: RezeptEntity) = viewModelScope.launch {
        val hund = _state.value.hunde.find { it.id == _state.value.selectedHundId } ?: return@launch
        val mer  = hund.kcalBedarfManuell ?: EnergieBedarf.mer(hund.gewichtKg)
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
        val ergebnisse = analyseUseCase.analyse(inputs, mer, _state.value.lebensphase)

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
                rezeptId               = id,
                zutatId                = d.zutatId,
                subRezeptId            = d.subRezeptId,
                mengeG                 = d.mengeG,
                anzahlTabletten        = d.anzahlTabletten,
                anzahlTropfen          = d.anzahlTropfen,
                inhaltsstoffeFreitext  = d.inhaltsstoffeFreitext,
                reihenfolge            = i
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

    // ── Toleranz-Verwaltung ───────────────────────────────────────────────
    fun editToleranz(naehrstoffKey: String) {
        val hundId    = _state.value.selectedHundId ?: return
        val toleranz  = _state.value.toleranzMap[naehrstoffKey] ?: ToleranzEntity(
            hundId            = hundId,
            naehrstoffKey     = naehrstoffKey,
            minProzent        = 80.0,
            empfehlungProzent = 100.0,
            maxProzent        = 150.0
        )
        _state.update { it.copy(editToleranz = toleranz) }
    }

    fun dismissToleranz() = _state.update { it.copy(editToleranz = null) }

    // ── Lebensphase ────────────────────────────────────────────────────────
    fun setLebensphase(phase: NrcLebensphasen.Lebensphase) {
        _state.update { it.copy(lebensphase = phase) }
        // Neuberechnung anstoßen damit Balken sofort aktualisiert werden
        _state.value.rezept?.let { analyseRezept(it) }
    }

    fun saveToleranz(toleranz: ToleranzEntity) = viewModelScope.launch {
        toleranzRepo.upsert(toleranz)
        _state.update { it.copy(editToleranz = null) }
    }

    fun toleranzZuruecksetzen(naehrstoffKey: String) = viewModelScope.launch {
        val hundId = _state.value.selectedHundId ?: return@launch
        toleranzRepo.upsert(ToleranzEntity(
            hundId            = hundId,
            naehrstoffKey     = naehrstoffKey,
            minProzent        = 80.0,
            empfehlungProzent = 100.0,
            maxProzent        = 150.0
        ))
    }

    fun alleToleranzZuruecksetzen() = viewModelScope.launch {
        val hundId = _state.value.selectedHundId ?: return@launch
        toleranzRepo.zuruecksetzenAufNrc(hundId)
    }
}
