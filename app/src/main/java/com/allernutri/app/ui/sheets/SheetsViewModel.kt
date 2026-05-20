package com.allernutri.app.ui.sheets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allernutri.app.data.local.entity.HundEntity
import com.allernutri.app.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SheetsUiState(
    // Gemeinsam
    val hunde: List<HundEntity>         = emptyList(),
    val selectedHundId: Long?           = null,
    val isLoading: Boolean              = false,
    val fehler: String?                 = null,
    val successMessage: String?         = null,

    // Import
    val spreadsheetId: String           = "",
    val sheetName: String               = "Symptome",
    val vorschauZeilen: List<Map<String, String>> = emptyList(),
    val mappings: Map<String, String?>  = emptyMap(),
    val autoMappings: Map<String, String?> = emptyMap(),
    val importResult: SheetsImportResult? = null,

    // Export
    val exportSpreadsheetId: String     = "",
    val exportVon: LocalDate            = LocalDate.now().minusDays(90),
    val exportBis: LocalDate            = LocalDate.now(),
    val exportTabs: Set<ExportTab>      = ExportTab.entries.toSet(),
    val exportResult: SheetsExportResult? = null,
    val exportedSheetUrl: String?       = null
)

@HiltViewModel
class SheetsViewModel @Inject constructor(
    private val hundRepo: HundRepository,
    private val sheetsRepo: SheetsRepository,
    private val session: SessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SheetsUiState())
    val state: StateFlow<SheetsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            hundRepo.alleHunde().collect { hunde ->
                _state.update { it.copy(
                    hunde          = hunde,
                    selectedHundId = it.selectedHundId ?: hunde.firstOrNull()?.id
                ) }
            }
        }
    }

    // ── Gemeinsam ─────────────────────────────────────────────────────────
    fun selectHund(id: Long)         = _state.update { it.copy(selectedHundId = id) }
    fun clearFehler()                = _state.update { it.copy(fehler = null) }
    fun clearResult()                = _state.update { it.copy(importResult = null, exportResult = null, successMessage = null) }

    // ── Import ────────────────────────────────────────────────────────────
    fun setSpreadsheetId(id: String) = _state.update { it.copy(spreadsheetId = id, vorschauZeilen = emptyList()) }
    fun setSheetName(name: String)   = _state.update { it.copy(sheetName = name, vorschauZeilen = emptyList()) }

    fun setMapping(feld: String, spalte: String?) {
        val neu = _state.value.mappings.toMutableMap()
        neu[feld] = spalte
        _state.update { it.copy(mappings = neu) }
    }

    fun ladeVorschau() = viewModelScope.launch {
        val token = getToken() ?: return@launch
        _state.update { it.copy(isLoading = true, fehler = null, vorschauZeilen = emptyList()) }

        val preview = sheetsRepo.previewImport(
            spreadsheetId = _state.value.spreadsheetId,
            sheetName     = _state.value.sheetName,
            token         = token
        )

        if (preview.fehler != null) {
            _state.update { it.copy(isLoading = false, fehler = preview.fehler) }
        } else {
            // Felder aus autoMapping als initiale mappings
            val initialMappings = preview.autoMapping.entries
                .associate { (idx, feld) -> feld to preview.headers.getOrNull(idx) }
            _state.update { it.copy(
                isLoading    = false,
                vorschauZeilen = preview.rawRows.map { row ->
                    preview.headers.zip(row).toMap()
                },
                autoMappings = initialMappings,
                mappings     = initialMappings
            ) }
        }
    }

    fun starteImport() = viewModelScope.launch {
        val hundId  = _state.value.selectedHundId ?: return@launch
        val preview = sheetsRepo.previewImport(
            spreadsheetId = _state.value.spreadsheetId,
            sheetName     = _state.value.sheetName,
            token         = getToken() ?: return@launch
        )
        _state.update { it.copy(isLoading = true, fehler = null) }
        val finalMapping = preview.autoMapping.toMutableMap().apply {
            _state.value.mappings.entries.forEachIndexed { idx, (_, spalte) ->
                if (spalte != null) this[idx] = spalte
            }
        }
        val result = sheetsRepo.importFromSheets(hundId, preview, finalMapping)
        _state.update { it.copy(isLoading = false, importResult = result,
            successMessage = if (result is SheetsImportResult.Success)
                "Import abgeschlossen: ${result.importiert} Einträge" else null) }
    }

    // ── Export ────────────────────────────────────────────────────────────
    fun setExportSpreadsheetId(id: String) = _state.update { it.copy(exportSpreadsheetId = id) }
    fun setExportVon(d: LocalDate)         = _state.update { it.copy(exportVon = d) }
    fun setExportBis(d: LocalDate)         = _state.update { it.copy(exportBis = d) }

    fun toggleExportTab(tab: ExportTab) {
        val tabs = _state.value.exportTabs.toMutableSet()
        if (tab in tabs) tabs.remove(tab) else tabs.add(tab)
        _state.update { it.copy(exportTabs = tabs) }
    }

    fun selectAllTabs()  = _state.update { it.copy(exportTabs = ExportTab.entries.toSet()) }
    fun clearAllTabs()   = _state.update { it.copy(exportTabs = emptySet()) }

    fun starteExport() = viewModelScope.launch {
        val hundId = _state.value.selectedHundId ?: return@launch
        val token  = getToken() ?: return@launch
        if (_state.value.exportTabs.isEmpty()) {
            _state.update { it.copy(fehler = "Bitte mindestens einen Tab auswählen.") }
            return@launch
        }
        _state.update { it.copy(isLoading = true, fehler = null, exportResult = null, exportedSheetUrl = null) }

        val sheetId = _state.value.exportSpreadsheetId.trim().ifBlank { null }
        val result  = sheetsRepo.exportToSheets(
            hundId        = hundId,
            spreadsheetId = sheetId,
            von           = _state.value.exportVon,
            bis           = _state.value.exportBis,
            tabs          = _state.value.exportTabs,
            token         = token
        )

        val url = if (result is SheetsExportResult.Success)
            "https://docs.google.com/spreadsheets/d/${result.spreadsheetId}/edit" else null

        _state.update { it.copy(
            isLoading        = false,
            exportResult     = result,
            exportedSheetUrl = url,
            successMessage   = if (result is SheetsExportResult.Success)
                "Export erfolgreich! ${_state.value.exportTabs.size} Tabs exportiert." else null,
            fehler           = if (result is SheetsExportResult.Error) result.message else null
        ) }
    }

    // ── Intern ────────────────────────────────────────────────────────────
    private suspend fun getToken(): String? {
        val loggedIn = session.isLoggedIn.first()
        if (!loggedIn) {
            _state.update { it.copy(
                fehler    = "Bitte zuerst mit Google anmelden (Einstellungen → Account)",
                isLoading = false
            ) }
            return null
        }
        // TODO: echten OAuth2-Token aus AuthRepository holen
        return "DEMO_TOKEN"
    }
}
