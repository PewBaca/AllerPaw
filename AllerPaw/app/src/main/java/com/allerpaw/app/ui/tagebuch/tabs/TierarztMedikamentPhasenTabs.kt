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
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ─────────────────────────────────────────────
// Tierarzt-Tab
// ─────────────────────────────────────────────

@Composable
fun TierarztTab(state: TagebuchUiState, vm: TagebuchViewModel) {
    TabListe(
        eintraege = state.tierarztEintraege,
        leerText  = "Noch kein Tierarzt-Eintrag",
        itemContent = { e ->
            TierarztCard(e, onEdit = { vm.editTierarzt(e) }, onDelete = { vm.deleteTierarzt(e.id) })
        }
    )
    state.editTierarzt?.let { e ->
        TierarztEditDialog(e, onDismiss = vm::dismissTierarzt, onSave = vm::saveTierarzt)
    }
}

@Composable
private fun TierarztCard(e: TagebuchTierarztEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.datum.toString(), style = MaterialTheme.typography.titleSmall)
                if (e.praxis.isNotBlank()) Text(e.praxis, style = MaterialTheme.typography.bodyMedium)
                if (e.anlass.isNotBlank()) Text("Anlass: ${e.anlass}",
                    style = MaterialTheme.typography.bodySmall)
                if (e.folgebesuchDatum != null) Text("Folgebesuch: ${e.folgebesuchDatum}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onEdit)   { Icon(Icons.Default.Edit, "Bearbeiten") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Löschen",
                tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun TierarztEditDialog(
    e: TagebuchTierarztEntity,
    onDismiss: () -> Unit,
    onSave: (TagebuchTierarztEntity) -> Unit
) {
    var praxis          by remember { mutableStateOf(e.praxis) }
    var anlass          by remember { mutableStateOf(e.anlass) }
    var untersuchungen  by remember { mutableStateOf(e.untersuchungen) }
    var ergebnis        by remember { mutableStateOf(e.ergebnis) }
    var empfehlung      by remember { mutableStateOf(e.empfehlung) }
    var folgebesuch     by remember { mutableStateOf(e.folgebesuchDatum?.toString() ?: "") }
    var notizen         by remember { mutableStateOf(e.notizen) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (e.id == 0L) "Tierarzt-Besuch" else "Besuch bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(praxis, { praxis = it }, label = { Text("Praxis / Tierarzt") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(anlass, { anlass = it }, label = { Text("Anlass") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(untersuchungen, { untersuchungen = it },
                    label = { Text("Untersuchungen") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(ergebnis, { ergebnis = it }, label = { Text("Ergebnis") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(empfehlung, { empfehlung = it }, label = { Text("Empfehlung") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(folgebesuch, { folgebesuch = it },
                    label = { Text("Folgebesuch (JJJJ-MM-TT)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(notizen, { notizen = it }, label = { Text("Notizen") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(e.copy(
                    praxis           = praxis,
                    anlass           = anlass,
                    untersuchungen   = untersuchungen,
                    ergebnis         = ergebnis,
                    empfehlung       = empfehlung,
                    folgebesuchDatum = runCatching { LocalDate.parse(folgebesuch) }.getOrNull(),
                    notizen          = notizen
                ))
            }) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

// ─────────────────────────────────────────────
// Medikament-Tab
// ─────────────────────────────────────────────

@Composable
fun MedikamentTab(state: TagebuchUiState, vm: TagebuchViewModel) {
    TabListe(
        eintraege = state.medikamentEintraege,
        leerText  = "Noch kein Medikament-Eintrag",
        itemContent = { e ->
            MedikamentCard(e,
                onEdit   = { vm.editMedikament(e) },
                onDelete = { vm.deleteMedikament(e.id) })
        }
    )
    state.editMedikament?.let { e ->
        MedikamentEditDialog(e, onDismiss = vm::dismissMedikament, onSave = vm::saveMedikament)
    }
}

@Composable
private fun MedikamentCard(e: TagebuchMedikamentEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.name, style = MaterialTheme.typography.titleSmall)
                if (e.dosierung.isNotBlank()) Text(e.dosierung,
                    style = MaterialTheme.typography.bodySmall)
                val zeitraum = buildString {
                    e.vonDatum?.let { append("von $it") }
                    e.bisDatum?.let { append(" bis $it") }
                }
                if (zeitraum.isNotBlank()) Text(zeitraum,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = onEdit)   { Icon(Icons.Default.Edit, "Bearbeiten") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Löschen",
                tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun MedikamentEditDialog(
    e: TagebuchMedikamentEntity,
    onDismiss: () -> Unit,
    onSave: (TagebuchMedikamentEntity) -> Unit
) {
    var name        by remember { mutableStateOf(e.name) }
    var typ         by remember { mutableStateOf(e.typ) }
    var dosierung   by remember { mutableStateOf(e.dosierung) }
    var haeufigkeit by remember { mutableStateOf(e.haeufigkeit) }
    var von         by remember { mutableStateOf(e.vonDatum?.toString() ?: "") }
    var bis         by remember { mutableStateOf(e.bisDatum?.toString() ?: "") }
    var verordnet   by remember { mutableStateOf(e.verordnetVon) }
    var notizen     by remember { mutableStateOf(e.notizen) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (e.id == 0L) "Neues Medikament" else "Medikament bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name *") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(typ, { typ = it }, label = { Text("Typ (z.B. Antihistaminikum)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(dosierung, { dosierung = it }, label = { Text("Dosierung") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(haeufigkeit, { haeufigkeit = it },
                    label = { Text("Häufigkeit (z.B. täglich, 2× täglich)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(von, { von = it }, label = { Text("Von (JJJJ-MM-TT)") },
                        modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(bis, { bis = it }, label = { Text("Bis (JJJJ-MM-TT)") },
                        modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(verordnet, { verordnet = it },
                    label = { Text("Verordnet von") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(notizen, { notizen = it }, label = { Text("Notizen") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                onSave(e.copy(
                    name        = name,
                    typ         = typ,
                    dosierung   = dosierung,
                    haeufigkeit = haeufigkeit,
                    vonDatum    = runCatching { LocalDate.parse(von) }.getOrNull(),
                    bisDatum    = runCatching { LocalDate.parse(bis) }.getOrNull(),
                    verordnetVon= verordnet,
                    notizen     = notizen
                ))
            }) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

// ─────────────────────────────────────────────
// Phasen-Tab (Ausschlussdiät-Tracker)
// ─────────────────────────────────────────────

@Composable
fun PhasenTab(state: TagebuchUiState, vm: TagebuchViewModel) {
    TabListe(
        eintraege = state.phasenEintraege,
        leerText  = "Noch keine Phase angelegt",
        itemContent = { e ->
            PhasenCard(e, onEdit = { vm.editPhase(e) }, onDelete = { vm.deletePhase(e.id) })
        }
    )
    state.editPhase?.let { e ->
        PhasenEditDialog(e, onDismiss = vm::dismissPhase, onSave = vm::savePhase)
    }
}

@Composable
private fun PhasenCard(e: AusschlussPhasEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    val heute        = LocalDate.now()
    val gesamtTage   = ChronoUnit.DAYS.between(e.startdatum, e.enddatum).toInt().coerceAtLeast(1)
    val vergangen    = ChronoUnit.DAYS.between(e.startdatum, heute).toInt().coerceIn(0, gesamtTage)
    val fortschritt  = vergangen.toFloat() / gesamtTage.toFloat()
    val aktiv        = heute in e.startdatum..e.enddatum

    val phasenLabel = when (e.phasentyp) {
        "elimination" -> "Eliminationsphase"
        "provokation" -> "Provokationsphase"
        "ergebnis"    -> "Ergebnisphase"
        else          -> e.phasentyp
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(phasenLabel, style = MaterialTheme.typography.titleSmall)
                        if (aktiv) Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text("Aktiv", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    if (e.getesteZutatName.isNotBlank())
                        Text("Zutat: ${e.getesteZutatName}",
                            style = MaterialTheme.typography.bodySmall)
                    Text("${e.startdatum} – ${e.enddatum}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                }
                IconButton(onClick = onEdit)   { Icon(Icons.Default.Edit, "Bearbeiten") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Löschen",
                    tint = MaterialTheme.colorScheme.error) }
            }
            if (aktiv) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress    = { fortschritt },
                    modifier    = Modifier.fillMaxWidth(),
                    trackColor  = MaterialTheme.colorScheme.surfaceVariant
                )
                Text("Tag $vergangen von $gesamtTage · ${gesamtTage - vergangen} verbleibend",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline)
            }
            val ergebnisLabel = when (e.ergebnis) {
                "vertraeglich" -> "✅ Verträglich"
                "reaktion"     -> "❌ Reaktion"
                else           -> "⏳ Offen"
            }
            Text(ergebnisLabel, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PhasenEditDialog(
    e: AusschlussPhasEntity,
    onDismiss: () -> Unit,
    onSave: (AusschlussPhasEntity) -> Unit
) {
    var phasentyp    by remember { mutableStateOf(e.phasentyp) }
    var zutatName    by remember { mutableStateOf(e.getesteZutatName) }
    var startdatum   by remember { mutableStateOf(e.startdatum.toString()) }
    var enddatum     by remember { mutableStateOf(e.enddatum.toString()) }
    var ergebnis     by remember { mutableStateOf(e.ergebnis) }
    var notizen      by remember { mutableStateOf(e.notizen) }

    // Standarddauer je Phase
    val defaultDauer = mapOf("elimination" to 42L, "provokation" to 14L, "ergebnis" to 7L)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (e.id == 0L) "Neue Phase" else "Phase bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Phasentyp", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("elimination" to "Elimination", "provokation" to "Provokation",
                        "ergebnis" to "Ergebnis").forEach { (v, l) ->
                        FilterChip(selected = phasentyp == v, onClick = {
                            phasentyp = v
                            // Enddatum automatisch vorschlagen
                            runCatching { LocalDate.parse(startdatum) }.getOrNull()?.let { start ->
                                enddatum = start.plusDays(defaultDauer[v] ?: 14L).toString()
                            }
                        }, label = { Text(l) })
                    }
                }
                if (phasentyp == "provokation") {
                    OutlinedTextField(zutatName, { zutatName = it },
                        label = { Text("Getestete Zutat") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(startdatum, { startdatum = it },
                        label = { Text("Start (JJJJ-MM-TT)") },
                        modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(enddatum, { enddatum = it },
                        label = { Text("Ende (JJJJ-MM-TT)") },
                        modifier = Modifier.weight(1f), singleLine = true)
                }
                Text("Ergebnis", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("offen" to "Offen", "vertraeglich" to "Verträglich",
                        "reaktion" to "Reaktion").forEach { (v, l) ->
                        FilterChip(selected = ergebnis == v, onClick = { ergebnis = v },
                            label = { Text(l) })
                    }
                }
                OutlinedTextField(notizen, { notizen = it }, label = { Text("Notizen") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val start = runCatching { LocalDate.parse(startdatum) }.getOrElse { LocalDate.now() }
                val end   = runCatching { LocalDate.parse(enddatum) }.getOrElse { start.plusDays(42) }
                onSave(e.copy(
                    phasentyp        = phasentyp,
                    getesteZutatName = zutatName,
                    startdatum       = start,
                    enddatum         = end,
                    ergebnis         = ergebnis,
                    notizen          = notizen
                ))
            }) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}
