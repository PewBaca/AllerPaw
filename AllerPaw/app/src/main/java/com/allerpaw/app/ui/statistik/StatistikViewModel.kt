package com.allerpaw.app.ui.statistik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allerpaw.app.data.local.entity.*
import com.allerpaw.app.data.repository.HundRepository
import com.allerpaw.app.data.repository.TagebuchRepository
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
    val score: Double,        // 0–5
    val anzahlBeobachtungen: Int
)

data class StatistikUiState(
    val hunde: List<HundEntity> = emptyList(),
    val selectedHundId: Long? = null,
    val zeitraumTage: Int = 90,  // 30 / 90 / 180 / 365 / 0=Alles

    val kpi: KpiState = KpiState(),
    val heatmap: List<HeatmapZelle> = emptyList(),
    val korrelationen: List<KorrelationsEintrag> = emptyList(),
    val reaktionsScores: List<ReaktionsScore> = emptyList(),
    val phasen: List<AusschlussPhasEntity> = emptyList(),

    // Rohdaten für Chart
    val symptomVerlauf: List<Pair<LocalDate, Double>> = emptyList(),  // Datum → Ø-Schweregrad
    val pollenVerlauf: List<Pair<LocalDate, Int>> = emptyList(),       // Datum → max. Stärke

    val isLoading: Boolean = false,
    val heatmapVerfuegbar: Boolean = false,   // erst ab 14 Symptomeinträgen
    val korrelationVerfuegbar: Boolean = false // erst ab 3 Datenpunkten
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

        _state.update {
            it.copy(
                kpi = KpiState(
                    symptomTage              = symptomTage,
                    durchschnittSchweregrad  = durchschnitt,
                    pollenTage               = pollenTage,
                    anzahlAllergene          = tagebuchRepo.allergenCount(hundId)
                ),
                symptomVerlauf          = symptomNachDatum,
                pollenVerlauf           = pollenNachDatum,
                heatmap                 = heatmap,
                heatmapVerfuegbar       = heatmapVerfuegbar,
                korrelationen           = korrelationen.sortedByDescending { k -> k.durchschnittSchweregrad },
                korrelationVerfuegbar   = korrelationVerfuegbar,
                phasen                  = phasen,
                isLoading               = false
            )
        }
    }
}
