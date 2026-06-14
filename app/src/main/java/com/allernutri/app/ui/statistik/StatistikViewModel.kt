package com.allernutri.app.ui.statistik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allernutri.app.data.local.entity.*
import com.allernutri.app.data.repository.HundRepository
import com.allernutri.app.data.repository.TagebuchRepository
import com.allernutri.app.domain.ReaktionsScoreAnalyse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.abs

data class KpiState(
    val symptomTage: Int = 0,
    val durchschnittSchweregrad: Double = 0.0,
    val pollenTage: Int = 0,
    val anzahlAllergene: Int = 0
)

data class HeatmapZelle(
    val wochentag: Int,  // 1=Mo … 7=So
    val monat: Int,      // 1–12
    val durchschnittSchweregrad: Double,
    val anzahl: Int
)

data class KorrelationsEintrag(
    val gruppe: String,   // Pollenart oder Zutat
    val durchschnittSchweregrad: Double,
    val anzahlBeobachtungen: Int,
    val istSignifikant: Boolean  // Ø > 2.0 und min. 3 Einträge
)

data class ReaktionsScore(
    val zutatName: String,
    val score: Double,              // 0–5
    val anzahlBeobachtungen: Int,
    val durchschnittSchweregrad: Double = 0.0,
    val haeufigkeit: Double = 0.0,
    val istSignifikant: Boolean = false,
    val beispielDaten: List<java.time.LocalDate> = emptyList()
)

data class StatistikUiState(
    val hunde: List<HundEntity> = emptyList(),
    val selectedHundId: Long? = null,
    val vergleichsHundId: Long? = null,          // null = kein Vergleich
    val zeitraumTage: Int = 90,

    val kpi: KpiState = KpiState(),
    val vergleichsKpi: KpiState? = null,         // KPI des Vergleichshunds
    val heatmap: List<HeatmapZelle> = emptyList(),
    val korrelationen: List<KorrelationsEintrag> = emptyList(),
    val reaktionsScores: List<ReaktionsScore> = emptyList(),
    val phasen: List<AusschlussPhasEntity> = emptyList(),

    // Rohdaten für Chart (Haupthund)
    val symptomVerlauf: List<Pair<LocalDate, Double>> = emptyList(),
    val pollenVerlauf: List<Pair<LocalDate, Int>> = emptyList(),
    // Rohdaten für Chart (Vergleichshund)
    val vergleichsSymptomVerlauf: List<Pair<LocalDate, Double>> = emptyList(),

    val isLoading: Boolean = false,
    val heatmapVerfuegbar: Boolean = false,
    val korrelationVerfuegbar: Boolean = false,
    val reaktionsScoreVerfuegbar: Boolean = false,

    // Gewichtsverlauf
    val gewichtVerlauf: List<HundGewichtEntity> = emptyList(),
    val gewichtNeuDatum: LocalDate = LocalDate.now(),
    val gewichtNeuKg: String = "",
    val showGewichtDialog: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatistikViewModel @Inject constructor(
    private val hundRepo: HundRepository,
    private val tagebuchRepo: TagebuchRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatistikUiState())
    val state: StateFlow<StatistikUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            hundRepo.alleHunde().collect { hunde ->
                _state.update { it.copy(hunde = hunde) }
                // Ersten Hund automatisch auswählen
                val firstId = hunde.firstOrNull()?.id
                if (firstId != null && _state.value.selectedHundId == null) {
                    selectHund(firstId)
                }
            }
        }
    }

    fun selectHund(id: Long) {
        _state.update { it.copy(selectedHundId = id) }
        ladeStatistik()
    }

    fun selectVergleichsHund(id: Long?) {
        _state.update { it.copy(
            vergleichsHundId        = id,
            vergleichsKpi           = null,
            vergleichsSymptomVerlauf = emptyList()
        ) }
        ladeStatistik()
    }

    fun setZeitraum(tage: Int) {
        _state.update { it.copy(zeitraumTage = tage) }
        ladeStatistik()
    }

    private fun ladeStatistik() = viewModelScope.launch {
        val hundId = _state.value.selectedHundId ?: return@launch
        _state.update { it.copy(isLoading = true) }

        val heute = LocalDate.now()
        val von = if (_state.value.zeitraumTage == 0) LocalDate.of(2000, 1, 1)
                  else heute.minusDays(_state.value.zeitraumTage.toLong())

        // Rohdaten laden
        val symptome  = tagebuchRepo.symptomeRange(hundId, von, heute)
        val umwelt    = tagebuchRepo.umweltRange(hundId, von, heute)
        val pollenLog = tagebuchRepo.pollenRange(hundId, von, heute)
        val phasen    = tagebuchRepo.phasenList(hundId)

        // ── KPIs ─────────────────────────────────────────────────────────
        val symptomTage = symptome.map { it.datum }.distinct().size
        val durchschnitt = if (symptome.isEmpty()) 0.0
                           else symptome.map { it.schweregrad.toDouble() }.average()
        val pollenTage = umwelt.count { eintrag ->
            pollenLog.any { it.umweltId == eintrag.id && it.staerke > 0 }
        }

        // ── Symptom-Verlauf (für Chart) ───────────────────────────────────
        val symptomNachDatum = symptome.groupBy { it.datum }
            .map { (datum, list) -> datum to list.map { it.schweregrad.toDouble() }.average() }
            .sortedBy { it.first }

        // ── Pollen-Verlauf ────────────────────────────────────────────────
        val pollenNachDatum = umwelt.map { eintrag ->
            val maxStaerke = pollenLog
                .filter { it.umweltId == eintrag.id }
                .maxOfOrNull { it.staerke } ?: 0
            eintrag.datum to maxStaerke
        }.sortedBy { it.first }

        // ── Heatmap (ab 14 Symptomeinträgen) ─────────────────────────────
        val heatmapVerfuegbar = symptome.size >= 14
        val heatmap = if (heatmapVerfuegbar) {
            symptome.groupBy { it.datum.dayOfWeek.value to it.datum.monthValue }
                .map { (key, list) ->
                    HeatmapZelle(
                        wochentag = key.first,
                        monat     = key.second,
                        durchschnittSchweregrad = list.map { it.schweregrad.toDouble() }.average(),
                        anzahl    = list.size
                    )
                }
        } else emptyList()

        // ── Korrelation Pollen ↔ Symptom ──────────────────────────────────
        val pollenArten = pollenLog.map { it.pollenart }.distinct()
        val korrelationen = mutableListOf<KorrelationsEintrag>()

        pollenArten.forEach { art ->
            // Tage mit dieser Pollenart
            val tageeMitPollen = pollenLog
                .filter { it.pollenart == art && it.staerke > 1 }
                .mapNotNull { pl -> umwelt.find { it.id == pl.umweltId }?.datum }
                .toSet()

            // Symptome in 48h-Fenster nach Pollentag
            val symptomNachPollen = symptome.filter { s ->
                tageeMitPollen.any { pollentag ->
                    val diff = ChronoUnit.DAYS.between(pollentag, s.datum)
                    diff in 0..2
                }
            }

            if (symptomNachPollen.size >= 3) {
                val avg = symptomNachPollen.map { it.schweregrad.toDouble() }.average()
                korrelationen.add(KorrelationsEintrag(
                    gruppe                  = art,
                    durchschnittSchweregrad = avg,
                    anzahlBeobachtungen     = symptomNachPollen.size,
                    istSignifikant          = avg > 2.0
                ))
            }
        }

        val korrelationVerfuegbar = korrelationen.isNotEmpty()

        // ── Reaktionsscore (48h-Fenster) ──────────────────────────────────
        val futterImZeitraum        = tagebuchRepo.futterRange(hundId, von, bis)
        val scoreEintraege          = ReaktionsScoreAnalyse.analysiere(futterImZeitraum, symptome)
        val reaktionsScores         = scoreEintraege.map { e ->
            ReaktionsScore(
                zutatName               = e.zutatOderRezept,
                score                   = e.score,
                anzahlBeobachtungen     = e.eintraege,
                durchschnittSchweregrad = e.durchschnittSchweregrad,
                haeufigkeit             = e.haeufigkeit,
                istSignifikant          = e.istSignifikant,
                beispielDaten           = e.beispielDaten
            )
        }
        val reaktionsScoreVerfuegbar = reaktionsScores.isNotEmpty()

        _state.update {
            it.copy(
                kpi = KpiState(
                    symptomTage              = symptomTage,
                    durchschnittSchweregrad  = durchschnitt,
                    pollenTage               = pollenTage,
                    anzahlAllergene          = tagebuchRepo.allergenCount(hundId)
                ),
                symptomVerlauf               = symptomNachDatum,
                pollenVerlauf                = pollenNachDatum,
                heatmap                      = heatmap,
                heatmapVerfuegbar            = heatmapVerfuegbar,
                korrelationen                = korrelationen.sortedByDescending { k -> k.durchschnittSchweregrad },
                korrelationVerfuegbar        = korrelationVerfuegbar,
                reaktionsScores              = reaktionsScores,
                reaktionsScoreVerfuegbar     = reaktionsScoreVerfuegbar,
                phasen                       = phasen,
                gewichtVerlauf               = hundRepo.letzteGewichte(hundId),
                isLoading               = false
            )
        }

        // ── Vergleichshund parallel laden ──────────────────────────────────
        val vergleichsId = _state.value.vergleichsHundId
        if (vergleichsId != null && vergleichsId != hundId) {
            val vSymptome = tagebuchRepo.symptomeRange(vergleichsId, von, heute)
            val vSymptomNachDatum = vSymptome
                .groupBy { it.datum }
                .map { (datum, liste) -> datum to liste.map { it.schweregrad.toDouble() }.average() }
                .sortedBy { it.first }
            val vSymptomTage   = vSymptome.map { it.datum }.distinct().size
            val vDurchschnitt  = if (vSymptome.isNotEmpty())
                vSymptome.map { it.schweregrad.toDouble() }.average() else 0.0
            val vPollenLog     = tagebuchRepo.pollenRange(vergleichsId, von, heute)
            val vUmwelt        = tagebuchRepo.umweltRange(vergleichsId, von, heute)
            val vPollenTage    = vUmwelt.count { eintrag ->
                vPollenLog.any { it.umweltId == eintrag.id && it.staerke > 0 }
            }
            val vAllergenCount = tagebuchRepo.allergenCount(vergleichsId)
            _state.update { it.copy(
                vergleichsKpi = KpiState(
                    symptomTage             = vSymptomTage,
                    durchschnittSchweregrad = vDurchschnitt,
                    pollenTage              = vPollenTage,
                    anzahlAllergene         = vAllergenCount
                ),
                vergleichsSymptomVerlauf = vSymptomNachDatum
            ) }
        }
    }

    // ── Gewichtsverlauf ───────────────────────────────────────────────────
    fun openGewichtDialog()  = _state.update { it.copy(showGewichtDialog = true,
        gewichtNeuDatum = LocalDate.now(), gewichtNeuKg = "") }
    fun dismissGewichtDialog() = _state.update { it.copy(showGewichtDialog = false) }
    fun setGewichtKg(kg: String)      = _state.update { it.copy(gewichtNeuKg = kg) }
    fun setGewichtDatum(d: LocalDate) = _state.update { it.copy(gewichtNeuDatum = d) }

    fun saveGewicht() = viewModelScope.launch {
        val hundId = _state.value.selectedHundId ?: return@launch
        val kg     = com.allernutri.app.util.FloatParser.parse(_state.value.gewichtNeuKg) ?: return@launch
        hundRepo.addGewicht(hundId, kg, _state.value.gewichtNeuDatum)
        _state.update { it.copy(
            showGewichtDialog = false,
            gewichtVerlauf    = hundRepo.letzteGewichte(hundId)
        ) }
        // Aktuelles Gewicht im Hund-Profil aktualisieren
        hundRepo.updateGewicht(hundId, kg)
    }

    fun deleteGewicht(id: Long) = viewModelScope.launch {
        hundRepo.deleteGewicht(id)
        val hundId = _state.value.selectedHundId ?: return@launch
        _state.update { it.copy(gewichtVerlauf = hundRepo.letzteGewichte(hundId)) }
    }
}
