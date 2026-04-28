package com.allerpaw.app.ui.tagebuch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allerpaw.app.data.local.entity.*
import com.allerpaw.app.data.repository.HundRepository
import com.allerpaw.app.data.repository.HundZustandRepository
import com.allerpaw.app.data.repository.TagebuchRepository
import com.allerpaw.app.util.UndoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class TagebuchTab(val label: String) {
    ZUSTAND("Zustand"),
    UMWELT("Umwelt"),
    SYMPTOM("Symptom"),
    FUTTER("Futter"),
    AUSSCHLUSS("Ausschluss"),
    ALLERGEN("Allergen"),
    TIERARZT("Tierarzt"),
    MEDIKAMENT("Medikament"),
    PHASEN("Phasen")
}

data class TagebuchUiState(
    val hunde: List<HundEntity> = emptyList(),
    val selectedHundId: Long? = null,
    val aktuellerTab: TagebuchTab = TagebuchTab.ZUSTAND,

    // Zustand (Smiley)
    val heutigerZustand: Int = 0,        // 0 = noch nicht gesetzt heute
    val zustandNotiz: String = "",
    val zustandVerlauf: List<TagebuchHundZustandEntity> = emptyList(),
    val umweltEintraege: List<TagebuchUmweltEntity> = emptyList(),
    val eigenePollenarten: List<EigenePollenartEntity> = emptyList(),

    // Symptom
    val symptomEintraege: List<TagebuchSymptomEntity> = emptyList(),

    // Futter
    val futterEintraege: List<TagebuchFutterEntity> = emptyList(),

    // Ausschluss
    val ausschlussEintraege: List<TagebuchAusschlussEntity> = emptyList(),

    // Allergen
    val allergenEintraege: List<TagebuchAllergenEntity> = emptyList(),

    // Tierarzt
    val tierarztEintraege: List<TagebuchTierarztEntity> = emptyList(),

    // Medikament
    val medikamentEintraege: List<TagebuchMedikamentEntity> = emptyList(),

    // Phasen
    val phasenEintraege: List<AusschlussPhasEntity> = emptyList(),

    // Edit-Dialoge
    val editUmwelt: TagebuchUmweltEntity? = null,
    val editSymptom: TagebuchSymptomEntity? = null,
    val editFutter: TagebuchFutterEntity? = null,
    val editAusschluss: TagebuchAusschlussEntity? = null,
    val editAllergen: TagebuchAllergenEntity? = null,
    val editTierarzt: TagebuchTierarztEntity? = null,
    val editMedikament: TagebuchMedikamentEntity? = null,
    val editPhase: AusschlussPhasEntity? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TagebuchViewModel @Inject constructor(
    private val hundRepo: HundRepository,
    private val repo: TagebuchRepository,
    private val zustandRepo: HundZustandRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TagebuchUiState())
    val state: StateFlow<TagebuchUiState> = _state.asStateFlow()

    val undoManager = UndoManager<Pair<TagebuchTab, Long>>(viewModelScope) { /* Soft-Delete reicht */ }

    private val selectedHundId = _state.map { it.selectedHundId }.distinctUntilChanged()

    init {
        // Hunde laden
        viewModelScope.launch {
            hundRepo.alleHunde().collect { hunde ->
                _state.update { s ->
                    s.copy(
                        hunde = hunde,
                        selectedHundId = s.selectedHundId ?: hunde.firstOrNull()?.id
                    )
                }
            }
        }
        // Eigene Pollenarten
        viewModelScope.launch {
            repo.eigenePollenarten().collect { list ->
                _state.update { it.copy(eigenePollenarten = list) }
            }
        }
        // Tab-Daten reaktiv laden wenn Hund wechselt
        viewModelScope.launch {
            selectedHundId.filterNotNull().flatMapLatest { hundId ->
                combine(
                    repo.umwelt(hundId),
                    repo.symptome(hundId),
                    repo.futter(hundId),
                    repo.ausschluss(hundId),
                    repo.allergene(hundId)
                ) { u, s, f, a, al -> listOf(u, s, f, a, al) }
            }.collect { lists ->
                _state.update {
                    it.copy(
                        umweltEintraege    = lists[0] as List<TagebuchUmweltEntity>,
                        symptomEintraege   = lists[1] as List<TagebuchSymptomEntity>,
                        futterEintraege    = lists[2] as List<TagebuchFutterEntity>,
                        ausschlussEintraege= lists[3] as List<TagebuchAusschlussEntity>,
                        allergenEintraege  = lists[4] as List<TagebuchAllergenEntity>
                    )
                }
            }
        }
        viewModelScope.launch {
            selectedHundId.filterNotNull().flatMapLatest { hundId ->
                combine(
                    repo.tierarzt(hundId),
                    repo.medikamente(hundId),
                    repo.phasen(hundId)
                ) { t, m, p -> Triple(t, m, p) }
            }.collect { (t, m, p) ->
                _state.update {
                    it.copy(
                        tierarztEintraege  = t,
                        medikamentEintraege= m,
                        phasenEintraege    = p
                    )
                }
            }
        }
    }

    fun selectHund(id: Long) {
        _state.update { it.copy(selectedHundId = id) }
        ladeZustand(id)
    }
    fun selectTab(tab: TagebuchTab) = _state.update { it.copy(aktuellerTab = tab) }

    // ── Zustand (Smiley) ─────────────────────────────────────────────────
    private fun ladeZustand(hundId: Long) = viewModelScope.launch {
        val heute = zustandRepo.getHeute(hundId)
        _state.update { it.copy(
            heutigerZustand = heute?.zustand ?: 0,
            zustandNotiz    = heute?.notizen ?: ""
        ) }
        zustandRepo.verlauf(hundId).collect { verlauf ->
            _state.update { it.copy(zustandVerlauf = verlauf) }
        }
    }

    fun setZustand(wert: Int)        = _state.update { it.copy(heutigerZustand = wert) }
    fun setZustandNotiz(notiz: String) = _state.update { it.copy(zustandNotiz = notiz) }

    fun saveZustand() = viewModelScope.launch {
        val hundId  = _state.value.selectedHundId ?: return@launch
        val zustand = _state.value.heutigerZustand
        if (zustand > 0) {
            zustandRepo.speichern(hundId, zustand, _state.value.zustandNotiz)
        }
    }

    // ── Umwelt ───────────────────────────────────────────────────────────
    fun newUmwelt()  = _state.update { it.copy(editUmwelt = emptyUmwelt(it.selectedHundId)) }
    fun editUmwelt(e: TagebuchUmweltEntity) = _state.update { it.copy(editUmwelt = e) }
    fun dismissUmwelt() = _state.update { it.copy(editUmwelt = null) }
    fun saveUmwelt(e: TagebuchUmweltEntity) = viewModelScope.launch {
        repo.saveUmwelt(e); _state.update { it.copy(editUmwelt = null) }
    }
    fun deleteUmwelt(id: Long) {
        viewModelScope.launch { repo.deleteUmwelt(id) }
        undoManager.push(TagebuchTab.UMWELT to id, "Umwelt-Eintrag gelöscht")
    }

    // ── Symptom ───────────────────────────────────────────────────────────
    fun newSymptom() = _state.update { it.copy(editSymptom = emptySymptom(it.selectedHundId)) }
    fun editSymptom(e: TagebuchSymptomEntity) = _state.update { it.copy(editSymptom = e) }
    fun dismissSymptom() = _state.update { it.copy(editSymptom = null) }
    fun saveSymptom(e: TagebuchSymptomEntity) = viewModelScope.launch {
        repo.saveSymptom(e); _state.update { it.copy(editSymptom = null) }
    }
    fun deleteSymptom(id: Long) {
        viewModelScope.launch { repo.deleteSymptom(id) }
        undoManager.push(TagebuchTab.SYMPTOM to id, "Symptom gelöscht")
    }

    // ── Futter ────────────────────────────────────────────────────────────
    fun newFutter() = _state.update { it.copy(editFutter = emptyFutter(it.selectedHundId)) }
    fun editFutter(e: TagebuchFutterEntity) = _state.update { it.copy(editFutter = e) }
    fun dismissFutter() = _state.update { it.copy(editFutter = null) }
    fun saveFutter(e: TagebuchFutterEntity, items: List<TagebuchFutterItemEntity>) =
        viewModelScope.launch { repo.saveFutter(e, items); _state.update { it.copy(editFutter = null) } }
    fun deleteFutter(id: Long) {
        viewModelScope.launch { repo.deleteFutter(id) }
        undoManager.push(TagebuchTab.FUTTER to id, "Futter-Eintrag gelöscht")
    }

    // ── Ausschluss ────────────────────────────────────────────────────────
    fun newAusschluss() = _state.update { it.copy(editAusschluss = emptyAusschluss(it.selectedHundId)) }
    fun editAusschluss(e: TagebuchAusschlussEntity) = _state.update { it.copy(editAusschluss = e) }
    fun dismissAusschluss() = _state.update { it.copy(editAusschluss = null) }
    fun saveAusschluss(e: TagebuchAusschlussEntity) = viewModelScope.launch {
        repo.saveAusschluss(e); _state.update { it.copy(editAusschluss = null) }
    }
    fun deleteAusschluss(id: Long) {
        viewModelScope.launch { repo.deleteAusschluss(id) }
        undoManager.push(TagebuchTab.AUSSCHLUSS to id, "Ausschluss gelöscht")
    }

    // ── Allergen ──────────────────────────────────────────────────────────
    fun newAllergen() = _state.update { it.copy(editAllergen = emptyAllergen(it.selectedHundId)) }
    fun editAllergen(e: TagebuchAllergenEntity) = _state.update { it.copy(editAllergen = e) }
    fun dismissAllergen() = _state.update { it.copy(editAllergen = null) }
    fun saveAllergen(e: TagebuchAllergenEntity) = viewModelScope.launch {
        repo.saveAllergen(e); _state.update { it.copy(editAllergen = null) }
    }
    fun deleteAllergen(id: Long) {
        viewModelScope.launch { repo.deleteAllergen(id) }
        undoManager.push(TagebuchTab.ALLERGEN to id, "Allergen gelöscht")
    }

    // ── Tierarzt ──────────────────────────────────────────────────────────
    fun newTierarzt() = _state.update { it.copy(editTierarzt = emptyTierarzt(it.selectedHundId)) }
    fun editTierarzt(e: TagebuchTierarztEntity) = _state.update { it.copy(editTierarzt = e) }
    fun dismissTierarzt() = _state.update { it.copy(editTierarzt = null) }
    fun saveTierarzt(e: TagebuchTierarztEntity) = viewModelScope.launch {
        repo.saveTierarzt(e); _state.update { it.copy(editTierarzt = null) }
    }
    fun deleteTierarzt(id: Long) {
        viewModelScope.launch { repo.deleteTierarzt(id) }
        undoManager.push(TagebuchTab.TIERARZT to id, "Tierarzt-Eintrag gelöscht")
    }

    // ── Medikament ────────────────────────────────────────────────────────
    fun newMedikament() = _state.update { it.copy(editMedikament = emptyMedikament(it.selectedHundId)) }
    fun editMedikament(e: TagebuchMedikamentEntity) = _state.update { it.copy(editMedikament = e) }
    fun dismissMedikament() = _state.update { it.copy(editMedikament = null) }
    fun saveMedikament(e: TagebuchMedikamentEntity) = viewModelScope.launch {
        repo.saveMedikament(e); _state.update { it.copy(editMedikament = null) }
    }
    fun deleteMedikament(id: Long) {
        viewModelScope.launch { repo.deleteMedikament(id) }
        undoManager.push(TagebuchTab.MEDIKAMENT to id, "Medikament gelöscht")
    }

    // ── Phasen ────────────────────────────────────────────────────────────
    fun newPhase() = _state.update { it.copy(editPhase = emptyPhase(it.selectedHundId)) }
    fun editPhase(e: AusschlussPhasEntity) = _state.update { it.copy(editPhase = e) }
    fun dismissPhase() = _state.update { it.copy(editPhase = null) }
    fun savePhase(e: AusschlussPhasEntity) = viewModelScope.launch {
        repo.savePhase(e); _state.update { it.copy(editPhase = null) }
    }
    fun deletePhase(id: Long) {
        viewModelScope.launch { repo.deletePhase(id) }
        undoManager.push(TagebuchTab.PHASEN to id, "Phase gelöscht")
    }

    // ── Pollen ────────────────────────────────────────────────────────────
    fun addEigenePollenart(name: String) = viewModelScope.launch { repo.addEigenePollenart(name) }

    // ── Empty-Factories ───────────────────────────────────────────────────
    private fun emptyUmwelt(hundId: Long?) = TagebuchUmweltEntity(
        hundId = hundId ?: 0L, datum = LocalDate.now())
    private fun emptySymptom(hundId: Long?) = TagebuchSymptomEntity(
        hundId = hundId ?: 0L, datum = LocalDate.now(), kategorie = "", schweregrad = 0)
    private fun emptyFutter(hundId: Long?) = TagebuchFutterEntity(
        hundId = hundId ?: 0L, datum = LocalDate.now())
    private fun emptyAusschluss(hundId: Long?) = TagebuchAusschlussEntity(
        hundId = hundId ?: 0L, verdachtsstufe = 0)
    private fun emptyAllergen(hundId: Long?) = TagebuchAllergenEntity(
        hundId = hundId ?: 0L, allergen = "", reaktionsstaerke = 1)
    private fun emptyTierarzt(hundId: Long?) = TagebuchTierarztEntity(
        hundId = hundId ?: 0L, datum = LocalDate.now())
    private fun emptyMedikament(hundId: Long?) = TagebuchMedikamentEntity(
        hundId = hundId ?: 0L, name = "")
    private fun emptyPhase(hundId: Long?) = AusschlussPhasEntity(
        hundId = hundId ?: 0L, phasentyp = "elimination",
        startdatum = LocalDate.now(), enddatum = LocalDate.now().plusDays(42))
}
