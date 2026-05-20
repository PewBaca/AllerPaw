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
import com.allernutri.app.data.local.entity.*
import com.allernutri.app.ui.tagebuch.TagebuchUiState
import com.allernutri.app.ui.tagebuch.TagebuchViewModel

// ── Tierarzt ──────────────────────────────────────────────────────────────────

@Composable
fun TierarztTab(state: TagebuchUiState, vm: TagebuchViewModel) {
    TabListe(
        eintraege   = state.tierarztEintraege,
        leerText    = "Noch kein Tierarzt-Eintrag",
        itemContent = { e ->
            TierarztCard(e, onEdit = { vm.editTierarzt(e) }, onDelete = { vm.deleteTierarzt(e.id) })
        }
    )
    state.editTierarzt?.let { e ->
        TierarztEditDialog(e, vm::dismissTierarzt, vm::saveTierarzt)
    }
}

@Composable
private fun TierarztCard(e: TagebuchTierarztEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.datum.toString(), style = MaterialTheme.typography.titleSmall)
                if (e.praxisTierarzt.isNotBlank())
                    Text("🏥 ${e.praxisTierarzt}", style = MaterialTheme.typography.bodySmall)
                if (e.anlass.isNotBlank())
                    Text(e.anlass, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline, maxLines = 2)
                if (e.folgebesuch != null)
                    Text("📅 Folgebesuch: ${e.folgebesuch}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onEdit)   { Icon(Icons.Default.Edit, stringResource(R.string.cd_bearbeiten)) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, stringResource(R.string.cd_loeschen), tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun TierarztEditDialog(e: TagebuchTierarztEntity, onDismiss: () -> Unit, onSave: (TagebuchTierarztEntity) -> Unit) {
    var praxis      by remember { mutableStateOf(e.praxisTierarzt) }
    var anlass      by remember { mutableStateOf(e.anlass) }
    var ergebnis    by remember { mutableStateOf(e.ergebnis) }
    var folgebesuch by remember { mutableStateOf(e.folgebesuch?.toString() ?: "") }
    var notizen     by remember { mutableStateOf(e.notizen) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.label_untersuchungen)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(praxis, { praxis = it },
                    label = { Text(stringResource(R.string.label_praxis_tierarzt)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(anlass, { anlass = it },
                    label = { Text(stringResource(R.string.label_anlass)) },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(ergebnis, { ergebnis = it },
                    label = { Text(stringResource(R.string.label_ergebnis)) },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(folgebesuch, { folgebesuch = it },
                    label = { Text(stringResource(R.string.label_folgebesuch)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("2026-06-15") })
                OutlinedTextField(notizen, { notizen = it },
                    label = { Text(stringResource(R.string.label_notizen)) },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(e.copy(
                    praxisTierarzt = praxis.trim(),
                    anlass         = anlass.trim(),
                    ergebnis       = ergebnis.trim(),
                    folgebesuch    = folgebesuch.trim().takeIf { it.isNotBlank() }
                        ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
                    notizen        = notizen.trim()
                ))
            }) { Text(stringResource(R.string.btn_speichern)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_abbrechen)) } }
    )
}

// ── Medikament ────────────────────────────────────────────────────────────────

@Composable
fun MedikamentTab(state: TagebuchUiState, vm: TagebuchViewModel) {
    TabListe(
        eintraege   = state.medikamentEintraege,
        leerText    = "Noch kein Medikament-Eintrag",
        itemContent = { e ->
            MedikamentCard(e, onEdit = { vm.editMedikament(e) }, onDelete = { vm.deleteMedikament(e.id) })
        }
    )
    state.editMedikament?.let { e ->
        MedikamentEditDialog(e, vm::dismissMedikament, vm::saveMedikament)
    }
}

@Composable
private fun MedikamentCard(e: TagebuchMedikamentEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("💊", style = MaterialTheme.typography.bodyLarge)
                    Text(e.name, style = MaterialTheme.typography.titleSmall)
                }
                if (e.dosierung.isNotBlank())
                    Text(e.dosierung, style = MaterialTheme.typography.bodySmall)
                Text("${e.vonDatum} – ${e.bisDatum ?: "laufend"}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                if (e.verordnetVon.isNotBlank())
                    Text(stringResource(R.string.label_verordnet_von) + ": ${e.verordnetVon}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = onEdit)   { Icon(Icons.Default.Edit, stringResource(R.string.cd_bearbeiten)) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, stringResource(R.string.cd_loeschen), tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun MedikamentEditDialog(e: TagebuchMedikamentEntity, onDismiss: () -> Unit, onSave: (TagebuchMedikamentEntity) -> Unit) {
    var name        by remember { mutableStateOf(e.name) }
    var typ         by remember { mutableStateOf(e.typ) }
    var dosierung   by remember { mutableStateOf(e.dosierung) }
    var vonDatum    by remember { mutableStateOf(e.vonDatum.toString()) }
    var bisDatum    by remember { mutableStateOf(e.bisDatum?.toString() ?: "") }
    var verordnet   by remember { mutableStateOf(e.verordnetVon) }
    var notizen     by remember { mutableStateOf(e.notizen) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("💊 Medikament") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it },
                    label = { Text(stringResource(R.string.label_name)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(typ, { typ = it },
                    label = { Text(stringResource(R.string.label_typ_medikament)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(dosierung, { dosierung = it },
                    label = { Text(stringResource(R.string.label_dosierung)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(vonDatum, { vonDatum = it },
                        label = { Text(stringResource(R.string.label_datum_von)) },
                        modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(bisDatum, { bisDatum = it },
                        label = { Text(stringResource(R.string.label_datum_bis)) },
                        modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(verordnet, { verordnet = it },
                    label = { Text(stringResource(R.string.label_verordnet_von)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(notizen, { notizen = it },
                    label = { Text(stringResource(R.string.label_notizen)) },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                onSave(e.copy(
                    name         = name.trim(),
                    typ          = typ.trim(),
                    dosierung    = dosierung.trim(),
                    vonDatum     = runCatching { java.time.LocalDate.parse(vonDatum) }.getOrElse { e.vonDatum },
                    bisDatum     = bisDatum.trim().takeIf { it.isNotBlank() }
                        ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
                    verordnetVon = verordnet.trim(),
                    notizen      = notizen.trim()
                ))
            }) { Text(stringResource(R.string.btn_speichern)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_abbrechen)) } }
    )
}

// ── Phasen ────────────────────────────────────────────────────────────────────

@Composable
fun PhasenTab(state: TagebuchUiState, vm: TagebuchViewModel) {
    TabListe(
        eintraege   = state.phasenEintraege,
        leerText    = "Noch keine Phase",
        itemContent = { e ->
            PhasenCard(e, onEdit = { vm.editPhase(e) }, onDelete = { vm.deletePhase(e.id) })
        }
    )
    state.editPhase?.let { e ->
        PhasenEditDialog(e, vm::dismissPhase, vm::savePhase)
    }
}

@Composable
private fun PhasenCard(e: TagebuchPhaseEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    val heute         = java.time.LocalDate.now()
    val vonDate       = e.vonDatum
    val bisDate       = e.bisDatum
    val gesamtTage    = if (bisDate != null) (bisDate.toEpochDay() - vonDate.toEpochDay()).toInt() + 1 else null
    val vergangeneTage = if (heute.isAfter(vonDate)) (heute.toEpochDay() - vonDate.toEpochDay()).toInt().coerceAtMost(gesamtTage ?: Int.MAX_VALUE) else 0
    val istAktiv      = bisDate == null || (heute >= vonDate && heute <= bisDate)

    val phasenEmoji = when (e.typ) {
        "ausschluss"  -> "🚫"
        "provokation" -> "⚡"
        "reaktion"    -> "🔴"
        else          -> "📋"
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors   = if (istAktiv)
            CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else CardDefaults.elevatedCardColors()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(phasenEmoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically) {
                    Text(e.typ.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleSmall)
                    if (istAktiv) {
                        Surface(color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.extraSmall) {
                            Text(stringResource(R.string.label_aktiv),
                                Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
                if (e.beschreibung.isNotBlank())
                    Text(e.beschreibung, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline, maxLines = 2)
                if (gesamtTage != null) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress  = { vergangeneTage.toFloat() / gesamtTage },
                        modifier  = Modifier.fillMaxWidth()
                    )
                    Text(
                        stringResource(R.string.tagebuch_phase_fortschritt,
                            vergangeneTage, gesamtTage, (gesamtTage - vergangeneTage).coerceAtLeast(0)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Column {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, stringResource(R.string.cd_bearbeiten), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, stringResource(R.string.cd_loeschen),
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun PhasenEditDialog(e: TagebuchPhaseEntity, onDismiss: () -> Unit, onSave: (TagebuchPhaseEntity) -> Unit) {
    var typ          by remember { mutableStateOf(e.typ) }
    var beschreibung by remember { mutableStateOf(e.beschreibung) }
    var vonDatum     by remember { mutableStateOf(e.vonDatum.toString()) }
    var bisDatum     by remember { mutableStateOf(e.bisDatum?.toString() ?: "") }

    val typen = listOf(
        "ausschluss"  to "${stringResource(R.string.tagebuch_ausschluss)} 🚫",
        "provokation" to "${stringResource(R.string.tagebuch_provokation)} ⚡",
        "reaktion"    to "${stringResource(R.string.tagebuch_reaktion)} 🔴"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tagebuch_phasentyp)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.tagebuch_phasentyp), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    typen.forEach { (v, l) ->
                        FilterChip(selected = typ == v, onClick = { typ = v },
                            label = { Text(l, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                OutlinedTextField(beschreibung, { beschreibung = it },
                    label = { Text(stringResource(R.string.label_beschreibung)) },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(vonDatum, { vonDatum = it },
                        label = { Text(stringResource(R.string.label_datum_start)) },
                        modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(bisDatum, { bisDatum = it },
                        label = { Text(stringResource(R.string.label_datum_ende)) },
                        modifier = Modifier.weight(1f), singleLine = true)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(e.copy(
                    typ          = typ,
                    beschreibung = beschreibung.trim(),
                    vonDatum     = runCatching { java.time.LocalDate.parse(vonDatum) }.getOrElse { e.vonDatum },
                    bisDatum     = bisDatum.trim().takeIf { it.isNotBlank() }
                        ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
                ))
            }) { Text(stringResource(R.string.btn_speichern)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_abbrechen)) } }
    )
}
