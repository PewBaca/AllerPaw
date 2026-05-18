package com.allerpaw.app.ui.tagebuch.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.allerpaw.app.R
import com.allerpaw.app.data.local.entity.*
import com.allerpaw.app.ui.tagebuch.TagebuchUiState
import com.allerpaw.app.ui.tagebuch.TagebuchViewModel

// ── Futter-Tab ────────────────────────────────────────────────────────────────

@Composable
fun FutterTab(state: TagebuchUiState, vm: TagebuchViewModel) {
    val gefiltert by vm.gefilterteFutter.collectAsState()
    val hatFilter = state.futterFilterErstgabe || state.futterFilterProvokation || state.futterFilterReaktion
    var filterOffen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value         = state.futterSuche,
                onValueChange = vm::setFutterSuche,
                modifier      = Modifier.weight(1f),
                placeholder   = { Text(stringResource(R.string.placeholder_suche)) },
                leadingIcon   = { Icon(Icons.Default.Search, null) },
                trailingIcon  = {
                    if (state.futterSuche.isNotBlank()) {
                        IconButton(onClick = { vm.setFutterSuche("") }) { Icon(Icons.Default.Clear, null) }
                    }
                },
                singleLine = true
            )
            BadgedBox(badge = { if (hatFilter) Badge() }) {
                IconButton(onClick = { filterOffen = !filterOffen }) {
                    Icon(
                        if (filterOffen) Icons.Default.FilterListOff else Icons.Default.FilterList,
                        stringResource(R.string.btn_filter),
                        tint = if (hatFilter) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
            }
        }

        AnimatedVisibility(visible = filterOffen) {
            ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, bottom = 6.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.btn_filter), style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = vm::clearFutterFilter) {
                            Text(stringResource(R.string.btn_filter_zuruecksetzen))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = state.futterFilterErstgabe, onClick = vm::toggleFutterFilterErstgabe,
                            label = { Text("Erstgabe") },
                            leadingIcon = if (state.futterFilterErstgabe) {{ Icon(Icons.Default.Check, null, Modifier.size(14.dp)) }} else null)
                        FilterChip(selected = state.futterFilterProvokation, onClick = vm::toggleFutterFilterProvokation,
                            label = { Text("Provokation") },
                            leadingIcon = if (state.futterFilterProvokation) {{ Icon(Icons.Default.Check, null, Modifier.size(14.dp)) }} else null)
                        FilterChip(selected = state.futterFilterReaktion, onClick = vm::toggleFutterFilterReaktion,
                            label = { Text("Reaktion") },
                            leadingIcon = if (state.futterFilterReaktion) {{ Icon(Icons.Default.Check, null, Modifier.size(14.dp)) }} else null)
                    }
                    if (hatFilter || state.futterSuche.isNotBlank()) {
                        Text("${gefiltert.size} Einträge gefunden",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        TabListe(
            eintraege = gefiltert,
            leerText  = if (hatFilter || state.futterSuche.isNotBlank()) "Keine Einträge gefunden" else "Noch kein Futter-Eintrag",
            itemContent = { e -> FutterCard(e, onEdit = { vm.editFutter(e) }, onDelete = { vm.deleteFutter(e.id) }) }
        )
    }

    state.editFutter?.let { e ->
        FutterEditDialog(eintrag = e, onDismiss = vm::dismissFutter, onSave = vm::saveFutter)
    }
}

@Composable
private fun FutterCard(e: TagebuchFutterEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(e.datum.toString(), style = MaterialTheme.typography.titleSmall)
                    if (e.erstgabe)    FutterBadge("Erstgabe",   MaterialTheme.colorScheme.primaryContainer)
                    if (e.provokation) FutterBadge("Provokation", MaterialTheme.colorScheme.tertiaryContainer)
                    if (e.reaktion)    FutterBadge("Reaktion",   MaterialTheme.colorScheme.errorContainer)
                }
                if (e.freitextErgaenzung.isNotBlank())
                    Text(e.freitextErgaenzung, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline, maxLines = 2)
            }
            IconButton(onClick = onEdit)   { Icon(Icons.Default.Edit, stringResource(R.string.cd_bearbeiten)) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, stringResource(R.string.cd_loeschen), tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun FutterBadge(label: String, containerColor: androidx.compose.ui.graphics.Color) {
    Surface(color = containerColor, shape = MaterialTheme.shapes.extraSmall) {
        Text(label, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun FutterEditDialog(eintrag: TagebuchFutterEntity, onDismiss: () -> Unit, onSave: (TagebuchFutterEntity) -> Unit) {
    var erstgabe    by remember { mutableStateOf(eintrag.erstgabe) }
    var provokation by remember { mutableStateOf(eintrag.provokation) }
    var reaktion    by remember { mutableStateOf(eintrag.reaktion) }
    var freitext    by remember { mutableStateOf(eintrag.freitextErgaenzung) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tagebuch_futter_eintrag)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Erstgabe (erste Gabe dieser Zutat)" to erstgabe to { erstgabe = !erstgabe },
                    "Provokations-Gabe" to provokation to { provokation = !provokation },
                    "Reaktion beobachtet" to reaktion to { reaktion = !reaktion }
                ).forEach { (pair, action) ->
                    val (label, checked) = pair
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checked, onCheckedChange = { action() })
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedTextField(freitext, { freitext = it },
                    label = { Text(stringResource(R.string.label_notizen_freitext)) },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(eintrag.copy(erstgabe = erstgabe, provokation = provokation, reaktion = reaktion, freitextErgaenzung = freitext))
            }) { Text(stringResource(R.string.btn_speichern)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_abbrechen)) } }
    )
}

// ── Ausschluss-Tab ────────────────────────────────────────────────────────────

@Composable
fun AusschlussTab(state: TagebuchUiState, vm: TagebuchViewModel) {
    TabListe(
        eintraege = state.ausschlussEintraege,
        leerText  = "Noch kein Ausschluss-Eintrag",
        itemContent = { e -> AusschlussCard(e, onEdit = { vm.editAusschluss(e) }, onDelete = { vm.deleteAusschluss(e.id) }) }
    )
    state.editAusschluss?.let { e -> AusschlussEditDialog(e, vm::dismissAusschluss, vm::saveAusschluss) }
}

@Composable
private fun AusschlussCard(e: TagebuchAusschlussEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.zutatName, style = MaterialTheme.typography.titleSmall)
                Text("${e.vonDatum} – ${e.bisDatum}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                if (e.grund.isNotBlank()) Text(e.grund, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            IconButton(onClick = onEdit)   { Icon(Icons.Default.Edit, stringResource(R.string.cd_bearbeiten)) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, stringResource(R.string.cd_loeschen), tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun AusschlussEditDialog(e: TagebuchAusschlussEntity, onDismiss: () -> Unit, onSave: (TagebuchAusschlussEntity) -> Unit) {
    var zutatName by remember { mutableStateOf(e.zutatName) }
    var grund     by remember { mutableStateOf(e.grund) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text(stringResource(R.string.tagebuch_ausschluss)) },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(zutatName, { zutatName = it }, label = { Text(stringResource(R.string.label_zutat_stoff)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(grund, { grund = it }, label = { Text(stringResource(R.string.label_grund)) },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(enabled = zutatName.isNotBlank(),
                onClick = { onSave(e.copy(zutatName = zutatName.trim(), grund = grund)) }) {
                Text(stringResource(R.string.btn_speichern))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_abbrechen)) } }
    )
}

// ── Allergen-Tab ──────────────────────────────────────────────────────────────

@Composable
fun AllergenTab(state: TagebuchUiState, vm: TagebuchViewModel) {
    TabListe(
        eintraege = state.allergenEintraege,
        leerText  = "Noch kein Allergen-Eintrag",
        itemContent = { e -> AllergenCard(e, onEdit = { vm.editAllergen(e) }, onDelete = { vm.deleteAllergen(e.id) }) }
    )
    state.editAllergen?.let { e -> AllergenEditDialog(e, vm::dismissAllergen, vm::saveAllergen) }
}

@Composable
private fun AllergenCard(e: TagebuchAllergenEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.allergen, style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.statistik_reaktionsstaerke, e.reaktionsstaerke),
                    style = MaterialTheme.typography.bodySmall)
                if (e.symptome.isNotBlank())
                    Text(e.symptome, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline, maxLines = 2)
            }
            IconButton(onClick = onEdit)   { Icon(Icons.Default.Edit, stringResource(R.string.cd_bearbeiten)) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, stringResource(R.string.cd_loeschen), tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun AllergenEditDialog(e: TagebuchAllergenEntity, onDismiss: () -> Unit, onSave: (TagebuchAllergenEntity) -> Unit) {
    var allergen  by remember { mutableStateOf(e.allergen) }
    var staerke   by remember { mutableStateOf(e.reaktionsstaerke.toFloat()) }
    var kategorie by remember { mutableStateOf(e.kategorie) }
    var symptome  by remember { mutableStateOf(e.symptome) }
    var notizen   by remember { mutableStateOf(e.notizen) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.label_allergen)) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(allergen, { allergen = it }, label = { Text(stringResource(R.string.label_allergen_stoff)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(kategorie, { kategorie = it }, label = { Text(stringResource(R.string.label_kategorie)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text(stringResource(R.string.label_schweregrad_wert, staerke.toInt()),
                    style = MaterialTheme.typography.bodySmall)
                Slider(staerke, { staerke = it }, valueRange = 1f..5f, steps = 3)
                OutlinedTextField(symptome, { symptome = it }, label = { Text(stringResource(R.string.tagebuch_symptome_beobachtet)) },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(notizen, { notizen = it }, label = { Text(stringResource(R.string.label_notizen)) },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(enabled = allergen.isNotBlank(), onClick = {
                onSave(e.copy(allergen = allergen.trim(), kategorie = kategorie.trim(),
                    reaktionsstaerke = staerke.toInt(), symptome = symptome, notizen = notizen))
            }) { Text(stringResource(R.string.btn_speichern)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_abbrechen)) } }
    )
}
