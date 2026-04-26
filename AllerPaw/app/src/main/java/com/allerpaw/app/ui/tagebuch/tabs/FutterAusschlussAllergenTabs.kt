package com.allerpaw.app.ui.tagebuch.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.allerpaw.app.data.local.entity.*
import com.allerpaw.app.ui.tagebuch.TagebuchUiState
import com.allerpaw.app.ui.tagebuch.TagebuchViewModel

// ─────────────────────────────────────────────
// Futter-Tab
// ─────────────────────────────────────────────

@Composable
fun FutterTab(state: TagebuchUiState, vm: TagebuchViewModel) {
    TabListe(
        eintraege = state.futterEintraege,
        leerText  = "Noch kein Futter-Eintrag",
        itemContent = { e ->
            FutterCard(e, onEdit = { vm.editFutter(e) }, onDelete = { vm.deleteFutter(e.id) })
        }
    )
    state.editFutter?.let { e ->
        FutterEditDialog(e, onDismiss = vm::dismissFutter, onSave = { fe ->
            vm.saveFutter(fe, emptyList()) // Items: Rezept-Auswahl kommt in Phase 3
        })
    }
}

@Composable
private fun FutterCard(e: TagebuchFutterEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.datum.toString(), style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (e.erstgabe)        AssistChip(onClick = {}, label = { Text("Erstgabe") })
                    if (e.provokation)     AssistChip(onClick = {}, label = { Text("Provokation") })
                    if (e.reaktion)        AssistChip(onClick = {}, label = { Text("Reaktion") })
                }
                if (e.freitextErgaenzung.isNotBlank())
                    Text(e.freitextErgaenzung, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline, maxLines = 2)
            }
            IconButton(onClick = onEdit)   { Icon(Icons.Default.Edit, "Bearbeiten") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Löschen",
                tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun FutterEditDialog(
    e: TagebuchFutterEntity,
    onDismiss: () -> Unit,
    onSave: (TagebuchFutterEntity) -> Unit
) {
    var freitext    by remember { mutableStateOf(e.freitextErgaenzung) }
    var erstgabe    by remember { mutableStateOf(e.erstgabe) }
    var zweiWochen  by remember { mutableStateOf(e.zweiWochenPhase) }
    var provokation by remember { mutableStateOf(e.provokation) }
    var reaktion    by remember { mutableStateOf(e.reaktion) }
    var notizen     by remember { mutableStateOf(e.notizen) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (e.id == 0L) "Neuer Futter-Eintrag" else "Futter bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Protokoll-Flags", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(erstgabe, { erstgabe = it }, label = { Text("Erstgabe") })
                    FilterChip(zweiWochen, { zweiWochen = it }, label = { Text("2-Wochen") })
                    FilterChip(provokation, { provokation = it }, label = { Text("Provokation") })
                    FilterChip(reaktion, { reaktion = it }, label = { Text("Reaktion") })
                }
                OutlinedTextField(freitext, { freitext = it },
                    label = { Text("Ergänzung / Freitext") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(notizen, { notizen = it },
                    label = { Text("Notizen") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
                Text("Rezept-Positionen folgen in Phase 3",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(e.copy(
                    freitextErgaenzung = freitext,
                    erstgabe           = erstgabe,
                    zweiWochenPhase    = zweiWochen,
                    provokation        = provokation,
                    reaktion           = reaktion,
                    notizen            = notizen
                ))
            }) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

// ─────────────────────────────────────────────
// Ausschluss-Tab
// ─────────────────────────────────────────────

@Composable
fun AusschlussTab(state: TagebuchUiState, vm: TagebuchViewModel) {
    TabListe(
        eintraege = state.ausschlussEintraege,
        leerText  = "Noch kein Ausschluss-Eintrag",
        itemContent = { e ->
            AusschlussCard(e,
                onEdit   = { vm.editAusschluss(e) },
                onDelete = { vm.deleteAusschluss(e.id) })
        }
    )
    state.editAusschluss?.let { e ->
        AusschlussEditDialog(e, onDismiss = vm::dismissAusschluss, onSave = vm::saveAusschluss)
    }
}

@Composable
private fun AusschlussCard(e: TagebuchAusschlussEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    val stufeLabel = listOf("Sicher", "Leicht", "Mittel", "Stark").getOrElse(e.verdachtsstufe) { "?" }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.zutatName.ifBlank { "Zutat unbekannt" },
                    style = MaterialTheme.typography.titleSmall)
                Text("Verdacht: $stufeLabel", style = MaterialTheme.typography.bodySmall)
                if (e.reaktion.isNotBlank())
                    Text("Reaktion: ${e.reaktion}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
            }
            IconButton(onClick = onEdit)   { Icon(Icons.Default.Edit, "Bearbeiten") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Löschen",
                tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun AusschlussEditDialog(
    e: TagebuchAusschlussEntity,
    onDismiss: () -> Unit,
    onSave: (TagebuchAusschlussEntity) -> Unit
) {
    var zutatName     by remember { mutableStateOf(e.zutatName) }
    var verdacht      by remember { mutableStateOf(e.verdachtsstufe.toFloat()) }
    var kategorie     by remember { mutableStateOf(e.kategorie) }
    var reaktion      by remember { mutableStateOf(e.reaktion) }
    var notizen       by remember { mutableStateOf(e.notizen) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (e.id == 0L) "Neuer Ausschluss" else "Ausschluss bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(zutatName, { zutatName = it },
                    label = { Text("Zutat / Inhaltsstoff") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Verdachtsstufe: ${verdacht.toInt()}/3 – " +
                    listOf("Sicher verträglich", "Leichter Verdacht", "Mittlerer Verdacht", "Starker Verdacht")
                        .getOrElse(verdacht.toInt()) { "" },
                    style = MaterialTheme.typography.labelMedium)
                Slider(verdacht, { verdacht = it }, valueRange = 0f..3f,
                    steps = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(reaktion, { reaktion = it },
                    label = { Text("Beobachtete Reaktion") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(notizen, { notizen = it },
                    label = { Text("Notizen") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(enabled = zutatName.isNotBlank(), onClick = {
                onSave(e.copy(
                    zutatName      = zutatName,
                    verdachtsstufe = verdacht.toInt(),
                    kategorie      = kategorie,
                    reaktion       = reaktion,
                    notizen        = notizen
                ))
            }) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

// ─────────────────────────────────────────────
// Allergen-Tab
// ─────────────────────────────────────────────

@Composable
fun AllergenTab(state: TagebuchUiState, vm: TagebuchViewModel) {
    TabListe(
        eintraege = state.allergenEintraege,
        leerText  = "Noch kein Allergen-Eintrag",
        itemContent = { e ->
            AllergenCard(e, onEdit = { vm.editAllergen(e) }, onDelete = { vm.deleteAllergen(e.id) })
        }
    )
    state.editAllergen?.let { e ->
        AllergenEditDialog(e, onDismiss = vm::dismissAllergen, onSave = vm::saveAllergen)
    }
}

@Composable
private fun AllergenCard(e: TagebuchAllergenEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.allergen, style = MaterialTheme.typography.titleSmall)
                Text("Stärke: ${e.reaktionsstaerke}/5",
                    style = MaterialTheme.typography.bodySmall)
                if (e.symptome.isNotBlank())
                    Text(e.symptome, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline, maxLines = 2)
            }
            IconButton(onClick = onEdit)   { Icon(Icons.Default.Edit, "Bearbeiten") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Löschen",
                tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun AllergenEditDialog(
    e: TagebuchAllergenEntity,
    onDismiss: () -> Unit,
    onSave: (TagebuchAllergenEntity) -> Unit
) {
    var allergen  by remember { mutableStateOf(e.allergen) }
    var kategorie by remember { mutableStateOf(e.kategorie) }
    var staerke   by remember { mutableStateOf(e.reaktionsstaerke.toFloat()) }
    var symptome  by remember { mutableStateOf(e.symptome) }
    var notizen   by remember { mutableStateOf(e.notizen) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (e.id == 0L) "Neues Allergen" else "Allergen bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(allergen, { allergen = it },
                    label = { Text("Allergen *") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(kategorie, { kategorie = it },
                    label = { Text("Kategorie (z.B. Futtermittel, Umwelt)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Reaktionsstärke: ${staerke.toInt()}/5",
                    style = MaterialTheme.typography.labelMedium)
                Slider(staerke, { staerke = it }, valueRange = 1f..5f,
                    steps = 3, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(symptome, { symptome = it },
                    label = { Text("Symptome") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(notizen, { notizen = it },
                    label = { Text("Notizen") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(enabled = allergen.isNotBlank(), onClick = {
                onSave(e.copy(
                    allergen          = allergen,
                    kategorie         = kategorie,
                    reaktionsstaerke  = staerke.toInt(),
                    symptome          = symptome,
                    notizen           = notizen
                ))
            }) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}
