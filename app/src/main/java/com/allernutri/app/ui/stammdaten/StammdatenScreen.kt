package com.allernutri.app.ui.stammdaten

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allernutri.app.R
import com.allernutri.app.data.local.entity.HundEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StammdatenScreen(
    onNavigateToZutaten: () -> Unit = {},
    vm: StammdatenViewModel = hiltViewModel()
) {
    val hunde by vm.hunde.collectAsState()
    val editHund by vm.editHund.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stammdaten_title)) },
                actions = {
                    IconButton(onClick = onNavigateToZutaten) {
                        Icon(Icons.Default.SetMeal, stringResource(R.string.cd_zutaten))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = vm::editNew) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_hund_hinzufuegen))
            }
        }
    ) { padding ->
        if (hunde.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Pets, null, modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.empty_hunde), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline)
                    TextButton(onClick = vm::editNew) { Text(stringResource(R.string.btn_jetzt_hinzufuegen)) }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(hunde, key = { it.id }) { hund ->
                    HundCard(hund = hund, onEdit = { vm.editExisting(hund) }, onDelete = { vm.delete(hund.id) })
                }
            }
        }
    }

    editHund?.let { hund ->
        HundEditDialog(
            hund      = hund,
            onDismiss = vm::dismissEdit,
            onSave    = vm::save
        )
    }
}

@Composable
private fun HundCard(hund: HundEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showConfirmDelete by remember { mutableStateOf(false) }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Pets, null,
                modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(hund.name, style = MaterialTheme.typography.titleMedium)
                if (hund.rasse.isNotBlank())
                    Text(hund.rasse, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                if (hund.gewichtKg > 0)
                    Text("${hund.gewichtKg} kg", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onEdit)  { Icon(Icons.Default.Edit, stringResource(R.string.cd_bearbeiten)) }
            IconButton(onClick = { showConfirmDelete = true }) {
                Icon(Icons.Default.Delete, stringResource(R.string.cd_loeschen),
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title  = { Text(stringResource(R.string.dialog_hund_loeschen_title)) },
            text   = { Text(stringResource(R.string.dialog_hund_loeschen_text, hund.name)) },
            confirmButton = {
                TextButton(onClick = { showConfirmDelete = false; onDelete() }) {
                    Text(stringResource(R.string.btn_loeschen))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text(stringResource(R.string.btn_abbrechen))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HundEditDialog(hund: HundEntity, onDismiss: () -> Unit, onSave: (HundEntity) -> Unit) {
    var name       by remember { mutableStateOf(hund.name) }
    var rasse      by remember { mutableStateOf(hund.rasse) }
    var gewicht    by remember { mutableStateOf(if (hund.gewichtKg > 0) hund.gewichtKg.toString() else "") }
    var kastriert  by remember { mutableStateOf(hund.kastriert) }
    var geschlecht by remember { mutableStateOf(hund.geschlecht) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hund.id == 0L) "Neuer Hund" else "Hund bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_name)) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rasse, onValueChange = { rasse = it },
                    label = { Text(stringResource(R.string.label_rasse)) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = gewicht, onValueChange = { gewicht = it },
                    label = { Text(stringResource(R.string.label_gewicht_kg)) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = kastriert, onCheckedChange = { kastriert = it })
                    Text(stringResource(R.string.label_kastriert))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.label_geschlecht), style = MaterialTheme.typography.bodySmall)
                    FilterChip(
                        selected = geschlecht == "m",
                        onClick = { geschlecht = "m" },
                        label = { Text(stringResource(R.string.label_ruede)) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    FilterChip(
                        selected = geschlecht == "w",
                        onClick = { geschlecht = "w" },
                        label = { Text(stringResource(R.string.label_huendin)) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(hund.copy(
                        name       = name.trim(),
                        rasse      = rasse.trim(),
                        gewichtKg  = gewicht.toDoubleOrNull() ?: hund.gewichtKg,
                        kastriert  = kastriert,
                        geschlecht = geschlecht
                    ))
                }
            ) { Text(stringResource(R.string.btn_speichern)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_abbrechen)) }
        }
    )
}
