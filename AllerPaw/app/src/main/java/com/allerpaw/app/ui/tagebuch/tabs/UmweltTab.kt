package com.allerpaw.app.ui.tagebuch.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.allerpaw.app.data.local.entity.TagebuchPollenLogEntity
import com.allerpaw.app.data.local.entity.TagebuchUmweltEntity
import com.allerpaw.app.ui.tagebuch.TagebuchUiState
import com.allerpaw.app.ui.tagebuch.TagebuchViewModel
import com.allerpaw.app.util.FloatParser
import java.time.LocalDate

@Composable
fun UmweltTab(state: TagebuchUiState, vm: TagebuchViewModel) {
    TabListe(
        eintraege = state.umweltEintraege,
        leerText  = "Noch kein Umwelt-Eintrag",
        itemContent = { e ->
            UmweltCard(e,
                onEdit   = { vm.editUmwelt(e) },
                onDelete = { vm.deleteUmwelt(e.id) })
        }
    )
    state.editUmwelt?.let { e ->
        UmweltEditDialog(
            eintrag           = e,
            eigenePollenarten = state.eigenePollenarten.map { it.name },
            onDismiss         = vm::dismissUmwelt,
            onSave            = vm::saveUmwelt,
            onAddPollenart    = vm::addEigenePollenart
        )
    }
}

@Composable
private fun UmweltCard(e: TagebuchUmweltEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.datum.toString(), style = MaterialTheme.typography.titleSmall)
                if (e.tempMinC != null && e.tempMaxC != null)
                    Text("${e.tempMinC}°C – ${e.tempMaxC}°C",
                        style = MaterialTheme.typography.bodySmall)
                if (e.luftfeuchte != null)
                    Text("Feuchte: ${e.luftfeuchte}%", style = MaterialTheme.typography.bodySmall)
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
private fun UmweltEditDialog(
    eintrag: TagebuchUmweltEntity,
    eigenePollenarten: List<String>,
    onDismiss: () -> Unit,
    onSave: (TagebuchUmweltEntity) -> Unit,
    onAddPollenart: (String) -> Unit
) {
    var tempMin      by remember { mutableStateOf(FloatParser.formatOrEmpty(eintrag.tempMinC)) }
    var tempMax      by remember { mutableStateOf(FloatParser.formatOrEmpty(eintrag.tempMaxC)) }
    var feuchte      by remember { mutableStateOf(eintrag.luftfeuchte?.toString() ?: "") }
    var niederschlag by remember { mutableStateOf(FloatParser.formatOrEmpty(eintrag.niederschlagMm)) }
    var raumtemp     by remember { mutableStateOf(FloatParser.formatOrEmpty(eintrag.raumtempC)) }
    var raumfeuchte  by remember { mutableStateOf(eintrag.raumfeuchte?.toString() ?: "") }
    var bett         by remember { mutableStateOf(eintrag.bett) }
    var notizen      by remember { mutableStateOf(eintrag.notizen) }

    // Pollen: pollenart → stärke
    val pollenStaerken = remember {
        mutableStateMapOf<String, Int>().also { map ->
            eigenePollenarten.forEach { map.putIfAbsent(it, 0) }
        }
    }
    var neuePollenart by remember { mutableStateOf("") }

    val standardPollen = listOf("Birke", "Erle", "Hasel", "Gräser", "Roggen", "Beifuß", "Ambrosia", "Eiche")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (eintrag.id == 0L) "Umwelt-Eintrag" else "Umwelt bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(tempMin, { tempMin = it }, label = { Text("Temp Min (°C)") },
                        modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(tempMax, { tempMax = it }, label = { Text("Temp Max (°C)") },
                        modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(feuchte, { feuchte = it }, label = { Text("Feuchte (%)") },
                        modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(niederschlag, { niederschlag = it }, label = { Text("Regen (mm)") },
                        modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(raumtemp, { raumtemp = it }, label = { Text("Raumtemp (°C)") },
                        modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(raumfeuchte, { raumfeuchte = it }, label = { Text("Raumfeuchte (%)") },
                        modifier = Modifier.weight(1f), singleLine = true)
                }

                // Bett
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bett: ", style = MaterialTheme.typography.bodySmall)
                    listOf("unverändert", "gewechselt").forEach { v ->
                        FilterChip(selected = bett == v, onClick = { bett = v },
                            label = { Text(v) }, modifier = Modifier.padding(end = 4.dp))
                    }
                }

                // Pollen
                Text("Pollen (0–5)", style = MaterialTheme.typography.titleSmall)
                (standardPollen + eigenePollenarten).distinct().forEach { art ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(art, Modifier.width(100.dp), style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value         = (pollenStaerken[art] ?: 0).toFloat(),
                            onValueChange = { pollenStaerken[art] = it.toInt() },
                            valueRange    = 0f..5f,
                            steps         = 4,
                            modifier      = Modifier.weight(1f)
                        )
                        Text("${pollenStaerken[art] ?: 0}", modifier = Modifier.width(24.dp))
                    }
                }

                // Eigene Pollenart hinzufügen
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(neuePollenart, { neuePollenart = it },
                        label = { Text("Neue Pollenart") },
                        modifier = Modifier.weight(1f), singleLine = true)
                    IconButton(onClick = {
                        if (neuePollenart.isNotBlank()) {
                            onAddPollenart(neuePollenart.trim())
                            pollenStaerken[neuePollenart.trim()] = 0
                            neuePollenart = ""
                        }
                    }) { Icon(Icons.Default.Add, "Hinzufügen") }
                }

                OutlinedTextField(notizen, { notizen = it }, label = { Text("Notizen") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(eintrag.copy(
                    tempMinC      = FloatParser.parse(tempMin),
                    tempMaxC      = FloatParser.parse(tempMax),
                    luftfeuchte   = feuchte.toIntOrNull(),
                    niederschlagMm= FloatParser.parse(niederschlag),
                    raumtempC     = FloatParser.parse(raumtemp),
                    raumfeuchte   = raumfeuchte.toIntOrNull(),
                    bett          = bett,
                    notizen       = notizen
                ))
            }) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}
