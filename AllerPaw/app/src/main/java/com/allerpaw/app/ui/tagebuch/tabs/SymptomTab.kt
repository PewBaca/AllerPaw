package com.allerpaw.app.ui.tagebuch.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.allerpaw.app.data.local.entity.TagebuchSymptomEntity
import com.allerpaw.app.ui.tagebuch.TagebuchUiState
import com.allerpaw.app.ui.tagebuch.TagebuchViewModel
import java.time.LocalDate

private val KATEGORIEN = listOf(
    "juckreiz" to "Juckreiz",
    "ohrentzuendung" to "Ohrentzündung",
    "hauroetung" to "Hautrötung",
    "pfoten_lecken" to "Pfoten lecken",
    "durchfall" to "Durchfall",
    "erbrechen" to "Erbrechen",
    "schuetteln" to "Schütteln",
    "sonstiges" to "Sonstiges"
)

private val KOERPERSTELLEN = listOf("ohren", "pfoten", "bauch", "rücken", "beine", "gesicht", "sonstiges")

@Composable
fun SymptomTab(state: TagebuchUiState, vm: TagebuchViewModel) {
    TabListe(
        eintraege = state.symptomEintraege,
        leerText  = "Noch kein Symptom-Eintrag",
        itemContent = { e ->
            SymptomCard(e, onEdit = { vm.editSymptom(e) }, onDelete = { vm.deleteSymptom(e.id) })
        }
    )
    state.editSymptom?.let { e ->
        SymptomEditDialog(e, onDismiss = vm::dismissSymptom, onSave = vm::saveSymptom)
    }
}

@Composable
private fun SymptomCard(e: TagebuchSymptomEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    val schweregradColor = when {
        e.schweregrad <= 1 -> Color(0xFF4CAF50)
        e.schweregrad <= 3 -> Color(0xFFFF9800)
        else               -> MaterialTheme.colorScheme.error
    }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(e.datum.toString(), style = MaterialTheme.typography.titleSmall)
                    Badge(containerColor = schweregradColor) {
                        Text("${e.schweregrad}/5", color = Color.White)
                    }
                }
                val katLabel = KATEGORIEN.find { it.first == e.kategorie }?.second ?: e.kategorie
                Text(katLabel, style = MaterialTheme.typography.bodyMedium)
                if (e.koerperstelle.isNotBlank())
                    Text(e.koerperstelle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                if (e.notizen.isNotBlank())
                    Text(e.notizen, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline, maxLines = 2)
            }
            IconButton(onClick = onEdit)   { Icon(Icons.Default.Edit, "Bearbeiten") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Löschen",
                tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SymptomEditDialog(
    e: TagebuchSymptomEntity,
    onDismiss: () -> Unit,
    onSave: (TagebuchSymptomEntity) -> Unit
) {
    var kategorie          by remember { mutableStateOf(e.kategorie.ifBlank { "juckreiz" }) }
    var kategorieFreitext  by remember { mutableStateOf(e.kategorieFreitext) }
    var beschreibung       by remember { mutableStateOf(e.beschreibung) }
    var schweregrad        by remember { mutableStateOf(e.schweregrad.toFloat()) }
    var koerperstelle      by remember { mutableStateOf(e.koerperstelle) }
    var koerperstelleFT    by remember { mutableStateOf(e.koerperstelleFreitext) }
    var notizen            by remember { mutableStateOf(e.notizen) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (e.id == 0L) "Neues Symptom" else "Symptom bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Kategorie
                Text("Kategorie", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    KATEGORIEN.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { (key, label) ->
                                FilterChip(
                                    selected = kategorie == key,
                                    onClick  = { kategorie = key },
                                    label    = { Text(label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                if (kategorie == "sonstiges") {
                    OutlinedTextField(kategorieFreitext, { kategorieFreitext = it },
                        label = { Text("Beschreibung Kategorie") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                }

                // Schweregrad
                Text("Schweregrad: ${schweregrad.toInt()}/5",
                    style = MaterialTheme.typography.labelMedium)
                Slider(
                    value         = schweregrad,
                    onValueChange = { schweregrad = it },
                    valueRange    = 0f..5f,
                    steps         = 4,
                    modifier      = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Kein", style = MaterialTheme.typography.labelSmall)
                    Text("Stark", style = MaterialTheme.typography.labelSmall)
                }

                // Körperstelle
                Text("Körperstelle", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    KOERPERSTELLEN.take(4).forEach { stelle ->
                        FilterChip(selected = koerperstelle == stelle,
                            onClick = { koerperstelle = stelle },
                            label = { Text(stelle) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    KOERPERSTELLEN.drop(4).forEach { stelle ->
                        FilterChip(selected = koerperstelle == stelle,
                            onClick = { koerperstelle = stelle },
                            label = { Text(stelle) })
                    }
                }
                if (koerperstelle == "sonstiges") {
                    OutlinedTextField(koerperstelleFT, { koerperstelleFT = it },
                        label = { Text("Körperstelle (Freitext)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                }

                OutlinedTextField(beschreibung, { beschreibung = it },
                    label = { Text("Beschreibung") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(notizen, { notizen = it },
                    label = { Text("Notizen") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(e.copy(
                    kategorie         = kategorie,
                    kategorieFreitext = kategorieFreitext,
                    beschreibung      = beschreibung,
                    schweregrad       = schweregrad.toInt(),
                    koerperstelle     = koerperstelle,
                    koerperstelleFreitext = koerperstelleFT,
                    notizen           = notizen
                ))
            }) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}
