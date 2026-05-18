package com.allerpaw.app.ui.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allerpaw.app.data.local.entity.HundEntity
import com.allerpaw.app.data.repository.*
import com.allerpaw.app.util.PdfExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import javax.inject.Inject

data class ExportUiState(
    val hunde: List<HundEntity> = emptyList(),
    val selectedHundId: Long? = null,
    val vonDatum: LocalDate = LocalDate.now().minusDays(90),
    val bisDatum: LocalDate = LocalDate.now(),

    // Sektions-Toggles
    val sektionen: Set<PdfExporter.Sektion> = PdfExporter.Sektion.STANDARD,

    val isExporting: Boolean = false,
    val exportFertig: Boolean = false,
    val exportDatei: String = "",
    val fehler: String? = null
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val hundRepo: HundRepository,
    private val tagebuchRepo: TagebuchRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ExportUiState())
    val state: StateFlow<ExportUiState> = _state.asStateFlow()

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

    fun selectHund(id: Long)            = _state.update { it.copy(selectedHundId = id) }
    fun setVonDatum(d: LocalDate)        = _state.update { it.copy(vonDatum = d) }
    fun setBisDatum(d: LocalDate)        = _state.update { it.copy(bisDatum = d) }

    fun toggleSektion(s: PdfExporter.Sektion) {
        val aktuell = _state.value.sektionen
        _state.update { it.copy(
            sektionen = if (s in aktuell) aktuell - s else aktuell + s
        ) }
    }

    // ── PDF-Export ────────────────────────────────────────────────────────
    fun exportPdf() = viewModelScope.launch {
        val hundId = _state.value.selectedHundId ?: return@launch
        val hund   = hundRepo.getById(hundId) ?: return@launch
        val von    = _state.value.vonDatum
        val bis    = _state.value.bisDatum

        _state.update { it.copy(isExporting = true, fehler = null) }

        try {
            val config = PdfExporter.ExportConfig(
                hund       = hund,
                vonDatum   = von,
                bisDatum   = bis,
                sektionen  = _state.value.sektionen,
                symptome   = tagebuchRepo.symptomeRange(hundId, von, bis),
                allergene  = tagebuchRepo.allergene(hundId).first(),
                phasen     = tagebuchRepo.phasenList(hundId),
                medikamente = tagebuchRepo.medikamente(hundId).first(),
                futter     = tagebuchRepo.futterRange(hundId, von, bis),
                tierarzt   = tagebuchRepo.tierarzt(hundId).first(),
                umwelt     = tagebuchRepo.umweltRange(hundId, von, bis)
            )
            val datei = PdfExporter.export(ctx, config)
            _state.update { it.copy(
                isExporting  = false,
                exportFertig = true,
                exportDatei  = datei.absolutePath
            ) }
            shareFile(datei, "application/pdf")
        } catch (e: Exception) {
            _state.update { it.copy(isExporting = false, fehler = e.message) }
        }
    }

    // ── CSV-Export ────────────────────────────────────────────────────────
    fun exportCsv() = viewModelScope.launch {
        val hundId = _state.value.selectedHundId ?: return@launch
        val von    = _state.value.vonDatum
        val bis    = _state.value.bisDatum

        _state.update { it.copy(isExporting = true, fehler = null) }

        try {
            val datei = File(ctx.cacheDir, "allerpaw_symptome_export.csv")
            FileWriter(datei).use { writer ->
                writer.write("Datum,Kategorie,Schweregrad,Körperstelle,Beschreibung,Notizen\n")
                tagebuchRepo.symptomeRange(hundId, von, bis).forEach { s ->
                    writer.write("${s.datum},${s.kategorie},${s.schweregrad}," +
                        "${s.koerperstelle},\"${s.beschreibung}\",\"${s.notizen}\"\n")
                }
            }
            _state.update { it.copy(isExporting = false, exportFertig = true,
                exportDatei = datei.absolutePath) }
            shareFile(datei, "text/csv")
        } catch (e: Exception) {
            _state.update { it.copy(isExporting = false, fehler = e.message) }
        }
    }

    // ── SQLite-Backup ─────────────────────────────────────────────────────
    fun exportBackup() = viewModelScope.launch {
        _state.update { it.copy(isExporting = true, fehler = null) }
        try {
            val dbPath = ctx.getDatabasePath("allerpaw.db")
            val backup = File(ctx.cacheDir, "allerpaw_backup_${LocalDate.now()}.db")
            dbPath.copyTo(backup, overwrite = true)
            _state.update { it.copy(isExporting = false, exportFertig = true,
                exportDatei = backup.absolutePath) }
            shareFile(backup, "application/octet-stream")
        } catch (e: Exception) {
            _state.update { it.copy(isExporting = false, fehler = e.message) }
        }
    }

    private fun shareFile(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            ctx, "${ctx.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(Intent.createChooser(intent, "Export teilen").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun resetExport() = _state.update { it.copy(exportFertig = false, fehler = null) }
}
