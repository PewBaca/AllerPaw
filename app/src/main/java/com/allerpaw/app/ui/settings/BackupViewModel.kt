package com.allerpaw.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allerpaw.app.data.repository.BackupRepository
import com.allerpaw.app.data.repository.BackupResult
import com.allerpaw.app.data.repository.RestoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val isLoading: Boolean              = false,
    val successMessage: String?         = null,
    val errorMessage: String?           = null,
    val showRestoreConfirm: Boolean     = false,
    val pendingRestoreUri: Uri?         = null,
    val showVersionMismatch: Boolean    = false,
    val backupDbVersion: Int            = 0,
    val appDbVersion: Int               = BackupRepository.CURRENT_DB_VERSION,
    val restoreErfolgreich: Boolean     = false   // → App-Neustart nötig
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val repo: BackupRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    // ── Export ────────────────────────────────────────────────────────────

    fun exportBackup() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        when (val result = repo.exportBackup()) {
            is BackupResult.Success -> {
                // Share-Intent
                val uri = FileProvider.getUriForFile(
                    ctx, "${ctx.packageName}.fileprovider", result.file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(Intent.createChooser(intent, "Backup teilen").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                _state.update { it.copy(isLoading = false,
                    successMessage = "Backup erstellt: ${result.file.name}") }
            }
            is BackupResult.Error -> {
                _state.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    // ── Import ────────────────────────────────────────────────────────────

    /** Wird aufgerufen wenn Nutzer eine .db-Datei aus dem Filepicker wählt */
    fun onRestoreFileSelected(uri: Uri) {
        _state.update { it.copy(
            pendingRestoreUri   = uri,
            showRestoreConfirm  = true,
            errorMessage        = null
        ) }
    }

    fun dismissRestoreConfirm() {
        _state.update { it.copy(showRestoreConfirm = false, pendingRestoreUri = null) }
    }

    fun confirmRestore() = viewModelScope.launch {
        val uri = _state.value.pendingRestoreUri ?: return@launch
        _state.update { it.copy(isLoading = true, showRestoreConfirm = false) }

        when (val result = repo.importBackup(uri)) {
            is RestoreResult.Success -> {
                _state.update { it.copy(isLoading = false, restoreErfolgreich = true) }
            }
            is RestoreResult.WrongVersion -> {
                _state.update { it.copy(
                    isLoading           = false,
                    showVersionMismatch  = true,
                    backupDbVersion      = result.dbVersion,
                    appDbVersion         = result.appVersion,
                    pendingRestoreUri    = null
                ) }
            }
            is RestoreResult.Error -> {
                _state.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    fun dismissVersionMismatch() = _state.update {
        it.copy(showVersionMismatch = false)
    }

    fun clearMessages() = _state.update {
        it.copy(successMessage = null, errorMessage = null)
    }
}
