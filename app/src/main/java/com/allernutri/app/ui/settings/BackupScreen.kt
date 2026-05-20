package com.allernutri.app.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allernutri.app.R
import com.allernutri.app.data.repository.BackupRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateUp: () -> Unit,
    vm: BackupViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val ctx   = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.onRestoreFileSelected(it) } }

    if (state.restoreErfolgreich) {
        AlertDialog(
            onDismissRequest = {},
            icon    = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
            title   = { Text(stringResource(R.string.backup_wiederherstellung_erfolgreich)) },
            text    = { Text(stringResource(R.string.backup_db_wiederhergestellt)) },
            confirmButton = {
                TextButton(onClick = {
                    val intent = ctx.packageManager
                        .getLaunchIntentForPackage(ctx.packageName)
                        ?.apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) }
                    ctx.startActivity(intent)
                    (ctx as? Activity)?.finish()
                }) { Text(stringResource(R.string.btn_neu_starten)) }
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.cd_zurueck))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier            = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment     = Alignment.Top) {
                    Icon(Icons.Default.Info, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(
                        "Das Backup enthält alle deine Daten: Hunde, Rezepte, Tagebuch, " +
                        "Aufgaben und Einstellungen. Die Datei ist eine SQLite-Datenbank " +
                        "(.db) und kann in Google Drive, iCloud oder lokal gespeichert werden.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text(stringResource(R.string.backup_erstellen),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Erstellt eine vollständige Kopie deiner Datenbank " +
                        "(allernutri_backup_[Datum]_v${BackupRepository.CURRENT_DB_VERSION}.db).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Button(
                        onClick  = vm::exportBackup,
                        enabled  = !state.isLoading,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Default.Backup, null, Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.backup_erstellen_teilen))
                    }
                }
            }

            Text(stringResource(R.string.backup_wiederherstellen),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedCard(colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment     = Alignment.Top) {
                            Icon(Icons.Default.Warning, null,
                                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Text(
                                "Achtung: Alle aktuellen Daten werden durch das Backup " +
                                "überschrieben. Diese Aktion kann nicht rückgängig gemacht werden.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    Text(
                        "Wähle eine .db-Backup-Datei aus. Die App prüft automatisch, " +
                        "ob die Version kompatibel ist (aktuell: v${BackupRepository.CURRENT_DB_VERSION}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    OutlinedButton(
                        onClick  = { filePicker.launch(arrayOf("*/*")) },
                        enabled  = !state.isLoading,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.backup_datei_auswaehlen))
                    }
                }
            }

            state.successMessage?.let { msg ->
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        Text(msg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = vm::clearMessages, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                }
            }

            state.errorMessage?.let { msg ->
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Text(msg, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f))
                        IconButton(onClick = vm::clearMessages, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                }
            }
        }
    }

    if (state.showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = vm::dismissRestoreConfirm,
            icon    = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title   = { Text(stringResource(R.string.dialog_backup_wiederherstellen_title)) },
            text    = {
                Text("Alle aktuellen Daten werden durch das ausgewählte Backup ersetzt. " +
                     "Die App wird danach neu gestartet.\n\nDiese Aktion kann nicht rückgängig gemacht werden.")
            },
            confirmButton = {
                TextButton(
                    onClick = vm::confirmRestore,
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.btn_wiederherstellen)) }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissRestoreConfirm) {
                    Text(stringResource(R.string.btn_abbrechen))
                }
            }
        )
    }

    if (state.showVersionMismatch) {
        AlertDialog(
            onDismissRequest = vm::dismissVersionMismatch,
            icon    = { Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error) },
            title   = { Text(stringResource(R.string.error_inkompatible_version)) },
            text    = {
                Text("Das Backup hat Datenbankversion ${state.backupDbVersion}, " +
                     "die App erwartet Version ${state.appDbVersion}.\n\n" +
                     "Bitte stelle sicher, dass du ein Backup aus der aktuellen App-Version verwendest.")
            },
            confirmButton = {
                TextButton(onClick = vm::dismissVersionMismatch) { Text(stringResource(R.string.btn_ok)) }
            }
        )
    }
}
