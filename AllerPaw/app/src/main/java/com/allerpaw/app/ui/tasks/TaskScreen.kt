package com.allerpaw.app.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allerpaw.app.data.local.entity.TaskEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(vm: TaskViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Aufgaben") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = vm::newTask) {
                Icon(Icons.Default.Add, "Neue Aufgabe")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            // ── Hund-Auswahl ─────────────────────────────────────────────
            if (state.hunde.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = state.hunde.indexOfFirst { it.id == state.selectedHundId }
                        .coerceAtLeast(0),
                    edgePadding = 12.dp
                ) {
                    state.hunde.forEach { hund ->
                        Tab(
                            selected = hund.id == state.selectedHundId,
                            onClick  = { vm.selectHund(hund.id) },
                            text     = { Text(hund.name) }
                        )
                    }
                }
            }

            if (state.tasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", fontSize = 56.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Noch keine Aufgaben", color = MaterialTheme.colorScheme.outline)
                        TextButton(onClick = vm::newTask) { Text("Erste Aufgabe erstellen") }
                    }
                }
            } else {
                // Datum-Header
                Text(
                    "Heute – ${state.heute}",
                    style    = MaterialTheme.typography.titleSmall,
                    color    = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )

                // Fortschritt
                val gesamt    = state.tasks.size
                val erledigt  = state.erledigungen.values.count { it?.erledigt == true }
                LinearProgressIndicator(
                    progress  = { if (gesamt > 0) erledigt.toFloat() / gesamt else 0f },
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                )
                Text(
                    "$erledigt / $gesamt erledigt",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 12.dp, bottom = 8.dp)
                )

                LazyColumn(
                    contentPadding      = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.tasks, key = { it.id }) { task ->
                        val erledigung = state.erledigungen[task.id]
                        val istErledigt = erledigung?.erledigt == true
                        TaskCard(
                            task       = task,
                            erledigt   = istErledigt,
                            onErledigen = {
                                if (istErledigt) vm.rueckgaengig(task.id)
                                else vm.erledigen(task.id)
                            },
                            onEdit     = { vm.editTask(task) },
                            onDelete   = { vm.deleteTask(task.id) }
                        )
                    }
                }
            }
        }
    }

    // Edit-Dialog
    state.editTask?.let { task ->
        TaskEditDialog(
            task      = task,
            onDismiss = vm::dismissEdit,
            onSave    = vm::saveTask
        )
    }
}

@Composable
private fun TaskCard(
    task: TaskEntity,
    erledigt: Boolean,
    onErledigen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val kategorieEmoji = when (task.kategorie) {
        "medikament" -> "💊"
        "pflege"     -> "🛁"
        "tierarzt"   -> "🏥"
        else         -> "📋"
    }

    val wiederholungLabel = when (task.wiederholung) {
        "taeglich"    -> "Täglich"
        "woechentlich" -> "Wöchentlich"
        "intervall"   -> "Alle ${task.intervallTage} Tage"
        "einmalig"    -> "Einmalig"
        else          -> task.wiederholung
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors   = if (erledigt)
            CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        else CardDefaults.elevatedCardColors()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked  = erledigt,
                onCheckedChange = { onErledigen() }
            )
            Spacer(Modifier.width(8.dp))

            // Kategorie-Emoji
            Text(kategorieEmoji, fontSize = 24.sp)
            Spacer(Modifier.width(8.dp))

            // Titel + Details
            Column(Modifier.weight(1f)) {
                Text(
                    task.titel,
                    style          = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (erledigt) TextDecoration.LineThrough else null,
                    color          = if (erledigt) MaterialTheme.colorScheme.outline
                                     else MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(wiederholungLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                    if (task.uhrzeit.isNotBlank()) {
                        Text("⏰ ${task.uhrzeit}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                    if (task.pushAktiv) {
                        Icon(Icons.Default.Notifications, null,
                            modifier = Modifier.size(12.dp),
                            tint     = MaterialTheme.colorScheme.primary)
                    }
                }
                if (task.beschreibung.isNotBlank()) {
                    Text(task.beschreibung,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 2)
                }
            }

            // Aktionen
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, "Bearbeiten", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, "Löschen",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit
) {
    var titel          by remember { mutableStateOf(task.titel) }
    var beschreibung   by remember { mutableStateOf(task.beschreibung) }
    var kategorie      by remember { mutableStateOf(task.kategorie) }
    var wiederholung   by remember { mutableStateOf(task.wiederholung) }
    var wochentage     by remember { mutableStateOf(task.wochentage) }
    var intervallTage  by remember { mutableStateOf(task.intervallTage.toString()) }
    var uhrzeit        by remember { mutableStateOf(task.uhrzeit) }
    var pushAktiv      by remember { mutableStateOf(task.pushAktiv) }

    val kategorien = listOf(
        "medikament" to "💊 Medikament",
        "pflege"     to "🛁 Pflege",
        "tierarzt"   to "🏥 Tierarzt",
        "sonstiges"  to "📋 Sonstiges"
    )

    val wochentageNamen = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    val gewaehlteWochentage = remember(wochentage) {
        mutableStateListOf<Int>().also { list ->
            wochentage.split(",").mapNotNull { it.trim().toIntOrNull() }.forEach { list.add(it) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (task.id == 0L) "Neue Aufgabe" else "Aufgabe bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                OutlinedTextField(titel, { titel = it },
                    label    = { Text("Titel *") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)

                OutlinedTextField(beschreibung, { beschreibung = it },
                    label    = { Text("Beschreibung") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)

                // Kategorie
                Text("Kategorie", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    kategorien.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { (v, l) ->
                                FilterChip(
                                    selected = kategorie == v,
                                    onClick  = { kategorie = v },
                                    label    = { Text(l) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Wiederholung
                Text("Wiederholung", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("taeglich" to "Täglich", "woechentlich" to "Wöchentlich",
                           "intervall" to "Intervall", "einmalig" to "Einmalig").forEach { (v, l) ->
                        FilterChip(selected = wiederholung == v,
                            onClick = { wiederholung = v },
                            label   = { Text(l, style = MaterialTheme.typography.labelSmall) })
                    }
                }

                // Wochentage (nur bei wöchentlich)
                if (wiederholung == "woechentlich") {
                    Text("Wochentage", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        wochentageNamen.forEachIndexed { i, name ->
                            val tag = i + 1
                            FilterChip(
                                selected = tag in gewaehlteWochentage,
                                onClick  = {
                                    if (tag in gewaehlteWochentage) gewaehlteWochentage.remove(tag)
                                    else gewaehlteWochentage.add(tag)
                                    wochentage = gewaehlteWochentage.sorted().joinToString(",")
                                },
                                label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Intervall-Tage
                if (wiederholung == "intervall") {
                    OutlinedTextField(intervallTage, { intervallTage = it },
                        label    = { Text("Alle N Tage") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                }

                // Uhrzeit für Push
                OutlinedTextField(uhrzeit, { uhrzeit = it },
                    label       = { Text("Erinnerungszeit (HH:MM)") },
                    placeholder = { Text("08:00") },
                    modifier    = Modifier.fillMaxWidth(), singleLine = true)

                // Push-Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = pushAktiv, onCheckedChange = { pushAktiv = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Push-Benachrichtigung aktivieren")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = titel.isNotBlank(),
                onClick = {
                    onSave(task.copy(
                        titel         = titel.trim(),
                        beschreibung  = beschreibung.trim(),
                        kategorie     = kategorie,
                        wiederholung  = wiederholung,
                        wochentage    = wochentage,
                        intervallTage = intervallTage.toIntOrNull() ?: 1,
                        uhrzeit       = uhrzeit.trim(),
                        pushAktiv     = pushAktiv
                    ))
                }
            ) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}
