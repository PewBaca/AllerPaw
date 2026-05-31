package com.allernutri.app.ui.tagebuch.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.allernutri.app.R
import com.allernutri.app.data.local.entity.TagebuchSymptomEntity
import com.allernutri.app.ui.tagebuch.TagebuchUiState
import com.allernutri.app.ui.tagebuch.TagebuchViewModel

@Composable
fun SymptomTab(state: TagebuchUiState, vm: TagebuchViewModel) {
    TabListe(
        eintraege   = state.symptomEintraege,
        leerText    = "Noch kein Symptom-Eintrag",
        itemContent = { e ->
            SymptomCard(
                e        = e,
                onEdit   = { vm.editSymptom(e) },
                onDelete = { vm.deleteSymptom(e.id) }
            )
        }
    )
    state.editSymptom?.let { e ->
        SymptomEditDialog(eintrag = e, onDismiss = vm::dismissSymptom, onSave = vm::saveSymptom)
    }
}

@Composable
private fun SymptomCard(
    e: TagebuchSymptomEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(e.datum.toString(), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.statistik_schweregrad_wert, e.schweregrad.toString()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )

                }
                if (e.koerperstelle.isNotBlank())
                    Text(e.koerperstelle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                if (e.beschreibung.isNotBlank())
                    Text(e.beschreibung, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            IconButton(onClick = onEdit)   { Icon(Icons.Default.Edit, stringResource(R.string.cd_bearbeiten)) }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, stringResource(R.string.cd_loeschen),
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SymptomEditDialog(
    eintrag: TagebuchSymptomEntity,
    onDismiss: () -> Unit,
    onSave: (TagebuchSymptomEntity) -> Unit
) {
    var koerperstelle      by remember { mutableStateOf(eintrag.koerperstelle) }
    var koerperstelleFrei  by remember { mutableStateOf(eintrag.koerperstelleFreitext) }
    var schweregrad        by remember { mutableStateOf(eintrag.schweregrad.toFloat()) }
    var beschreibung       by remember { mutableStateOf(eintrag.beschreibung) }

    val koerperstelleOptionen = listOf(
        "Haut", "Pfoten", "Ohren", "Augen", "Magen/Darm",
        "Atemwege", "Fell", "Allgemeinbefinden", "Sonstiges"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Symptom") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.label_koerperstelle),
                    style = MaterialTheme.typography.labelMedium)

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    koerperstelleOptionen.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { option ->
                                FilterChip(
                                    selected = koerperstelle == option,
                                    onClick  = { koerperstelle = option },
                                    label    = {
                                        Text(option, style = MaterialTheme.typography.labelSmall)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                if (koerperstelle == "Sonstiges") {
                    OutlinedTextField(
                        value         = koerperstelleFrei,
                        onValueChange = { koerperstelleFrei = it },
                        label         = { Text(stringResource(R.string.label_koerperstelle_freitext)) },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true
                    )
                }

                Text(
                    stringResource(R.string.label_schweregrad_wert, schweregrad.toInt()),
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value        = schweregrad,
                    onValueChange = { schweregrad = it },
                    valueRange   = 1f..5f,
                    steps        = 3
                )

                OutlinedTextField(
                    value         = beschreibung,
                    onValueChange = { beschreibung = it },
                    label         = { Text(stringResource(R.string.label_beschreibung)) },
                    modifier      = Modifier.fillMaxWidth(),
                    minLines      = 2
                )


            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(eintrag.copy(
                    koerperstelle        = koerperstelle,
                    koerperstelleFreitext = koerperstelleFrei,
                    schweregrad          = schweregrad.toInt(),
                    beschreibung         = beschreibung.trim()
                ))
            }) { Text(stringResource(R.string.btn_speichern)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_abbrechen)) }
        }
    )
}
