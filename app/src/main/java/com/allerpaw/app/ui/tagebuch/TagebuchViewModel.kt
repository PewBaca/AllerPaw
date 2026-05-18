package com.allerpaw.app.ui.tagebuch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allerpaw.app.data.local.entity.*
import com.allerpaw.app.data.repository.HundRepository
import com.allerpaw.app.data.repository.HundZustandRepository
import com.allerpaw.app.data.repository.SettingsRepository
import com.allerpaw.app.data.repository.TagebuchRepository
import com.allerpaw.app.data.repository.WetterRepository
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

/** Zusammenfassung der Wetterdaten für den UmweltTab-Banner */
data class WetterBanner(
    val stadtName: String,
    val tempMin: Double?,
    val tempMax: Double?,
    val feuchte: Int?,
    val regenMm: Double?,
    val pollenMap: Map<String, Int>,  // Art → Stärke 0–5
    val autoBefuellt: Boolean = false  // true = heute bereits auto-befüllt
)

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
    val futterSuche: String = "",
    val futterFilterErstgabe: Boolean = false,
    val futterFilterProvokation: Boolean = false,
    val futterFilterReaktion: Boolean = false,

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

    // Wetter (UmweltTab Auto-Befüllung)
    val wetterBanner: WetterBanner? = null,
    val wetterFehler: String? = null,
    val wetterLaedt: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TagebuchViewModel @Inject constructor(
    private val hundRepo: HundRepository,
    private val repo: TagebuchRepository,
    private val zustandRepo: HundZustandRepository,
    private val wetterRepo: WetterRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TagebuchUiState())
    val state: StateFlow<TagebuchUiState> = _state.asStateFlow()

    val undoManager = UndoManager<Pair<TagebuchTab, Long>>(viewModelScope) { /* Soft-Delete reicht */ }

    private val selectedHundId = _state.map { it.selectedHundId }.distinctUntilChanged()

    init {
        // Hunde laden
        viewModelScope.launch {
            hundRepo.alleHunde().collect { hunde ->
                val bisherSelectedId = _state.value.selectedHundId
                val neuerSelectedId  = bisherSelectedId ?: hunde.firstOrNull()?.id
                _state.update { s ->
                    s.copy(
                        hunde          = hunde,
                        selectedHundId = neuerSelectedId
                    )
                }
                // Zustand für ersten Hund beim ersten Laden laden
                if (bisherSelectedId == null && neuerSelectedId != null) {
                    ladeZustand(neuerSelectedId)
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

    // ── Futter Suche + Filter ─────────────────────────────────────────────
    fun setFutterSuche(q: String) = _state.update { it.copy(futterSuche = q) }
    fun toggleFutterFilterErstgabe()   = _state.update { it.copy(futterFilterErstgabe   = !it.futterFilterErstgabe) }
    fun toggleFutterFilterProvokation() = _state.update { it.copy(futterFilterProvokation = !it.futterFilterProvokation) }
    fun toggleFutterFilterReaktion()   = _state.update { it.copy(futterFilterReaktion   = !it.futterFilterReaktion) }
    fun clearFutterFilter() = _state.update { it.copy(
        futterSuche           = "",
        futterFilterErstgabe  = false,
        futterFilterProvokation = false,
        futterFilterReaktion  = false
    ) }

    val gefilterteFutter = _state.map { s ->
        var result = s.futterEintraege
        if (s.futterSuche.isNotBlank()) {
            result = result.filter {
                it.freitextErgaenzung.contains(s.futterSuche, ignoreCase = true)
            }
        }
        if (s.futterFilterErstgabe)   result = result.filter { it.erstgabe }
        if (s.futterFilterProvokation) result = result.filter { it.provokation }
        if (s.futterFilterReaktion)   result = result.filter { it.reaktion }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000),
        emptyList())

    // ── Zustand (Smiley) ─────────────────────────────────────────────────
    private var zustandJob: kotlinx.coroutines.Job? = null

    private fun ladeZustand(hundId: Long) {
        // Heutigen Zustand einmalig laden
        viewModelScope.launch {
            val heute = zustandRepo.getHeute(hundId)
            _state.update { it.copy(
                heutigerZustand = heute?.zustand ?: 0,
                zustandNotiz    = heute?.notizen ?: ""
            ) }
        }
        // Verlauf als Flow — alten Job canceln um Memory Leak zu vermeiden
        zustandJob?.cancel()
        zustandJob = viewModelScope.launch {
            zustandRepo.verlauf(hundId).collect { verlauf ->
                _state.update { it.copy(zustandVerlauf = verlauf) }
            }
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

    // ── Wetter Auto-Befüllung ─────────────────────────────────────────────

    /**
     * Lädt Wetter + Pollen für den gespeicherten Standort und
     * zeigt ein Banner im UmweltTab.
     * Wird beim Tab-Wechsel zu UMWELT automatisch aufgerufen.
     */
    fun ladeWetter() = viewModelScope.launch {
        // Nicht neu laden wenn schon ein Banner da ist
        if (_state.value.wetterBanner != null) return@launch
        _state.update { it.copy(wetterLaedt = true, wetterFehler = null) }
        try {
            val lat  = settingsRepo.standortLat.first()
            val lon  = settingsRepo.standortLon.first()
            val wetter = wetterRepo.getWetter(LocalDate.now(), lat, lon)
            val pollen = wetterRepo.getPollen(lat, lon)

            if (wetter == null && pollen.isEmpty()) {
                _state.update { it.copy(
                    wetterLaedt = false,
                    wetterFehler = "Keine Wetterdaten verfügbar. Standort in Einstellungen prüfen."
                ) }
                return@launch
            }

            // Pollen: heutigen Tagesmittelwert je Art
            val pollenHeute = pollen.entries.associate { (art, tage) ->
                art to (tage.firstOrNull()?.staerke0bis5 ?: 0)
            }.filter { it.value > 0 }

            val stadtName = settingsRepo.getString("standort_name", "${lat}, ${lon}")

            _state.update { it.copy(
                wetterLaedt = false,
                wetterBanner = WetterBanner(
                    stadtName    = stadtName,
                    tempMin      = wetter?.tempMinC,
                    tempMax      = wetter?.tempMaxC,
                    feuchte      = wetter?.luftfeuchte,
                    regenMm      = wetter?.niederschlagMm,
                    pollenMap    = pollenHeute,
                    autoBefuellt = false
                )
            ) }
        } catch (e: Exception) {
            _state.update { it.copy(
                wetterLaedt = false,
                wetterFehler = "Wetter-Ladefehler: ${e.message}"
            ) }
        }
    }

    /**
     * Befüllt den heutigen Umwelt-Eintrag automatisch mit den Wetterdaten.
     * Legt einen neuen Eintrag an wenn noch keiner für heute existiert.
     */
    fun befuelleAktuelleUmweltAusWetter() = viewModelScope.launch {
        val banner = _state.value.wetterBanner ?: return@launch
        val hundId = _state.value.selectedHundId ?: return@launch
        val heute  = LocalDate.now()

        // Bestehenden Eintrag für heute finden oder neuen anlegen
        val bestehend = _state.value.umweltEintraege.firstOrNull { it.datum == heute }
        val eintrag   = bestehend ?: TagebuchUmweltEntity(hundId = hundId, datum = heute)

        val aktualisiert = eintrag.copy(
            tempMinC       = banner.tempMin ?: eintrag.tempMinC,
            tempMaxC       = banner.tempMax ?: eintrag.tempMaxC,
            luftfeuchte    = banner.feuchte ?: eintrag.luftfeuchte,
            niederschlagMm = banner.regenMm ?: eintrag.niederschlagMm
        )

        val umweltId = repo.saveUmwelt(aktualisiert)

        // Pollen als PollenLog-Einträge speichern
        if (banner.pollenMap.isNotEmpty()) {
            val pollenLogs = banner.pollenMap.map { (art, staerke) ->
                TagebuchPollenLogEntity(umweltId = umweltId, pollenart = art, staerke = staerke)
            }
            repo.savePollenLog(umweltId, pollenLogs)
        }

        // Banner als "auto-befüllt" markieren
        _state.update { it.copy(wetterBanner = banner.copy(autoBefuellt = true)) }
    }
}
