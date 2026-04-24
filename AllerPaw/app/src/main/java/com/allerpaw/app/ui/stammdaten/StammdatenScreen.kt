package com.allerpaw.app.ui.stammdaten

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
import androidx.hilt.navigation.compose.hiltViewModel
import com.allerpaw.app.data.local.entity.HundEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StammdatenScreen(vm: StammdatenViewModel = hiltViewModel()) {
    val hunde by vm.hunde.collectAsState()
    val editHund by vm.editHund.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Meine Hunde") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = vm::editNew) {
                Icon(Icons.Default.Add, contentDescription = "Hund hinzufügen")
            }
        }
    ) { padding ->
        if (hunde.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Pets, null, modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(12.dp))
                    Text("Noch kein Hund angelegt", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline)
                    TextButton(onClick = vm::editNew) { Text("Jetzt hinzufügen") }
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
            IconButton(onClick = onEdit)  { Icon(Icons.Default.Edit, "Bearbeiten") }
            IconButton(onClick = { showConfirmDelete = true }) {
                Icon(Icons.Default.Delete, "Löschen",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title  = { Text("Hund löschen?") },
            text   = { Text("${hund.name} und alle zugehörigen Daten werden gelöscht.") },
            confirmButton = {
                TextButton(onClick = { showConfirmDelete = false; onDelete() }) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) { Text("Abbrechen") }
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
                    label = { Text("Name *") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rasse, onValueChange = { rasse = it },
                    label = { Text("Rasse") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = gewicht, onValueChange = { gewicht = it },
                    label = { Text("Gewicht (kg)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = kastriert, onCheckedChange = { kastriert = it })
                    Text("Kastriert / sterilisiert")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Geschlecht: ", style = MaterialTheme.typography.bodySmall)
                    FilterChip(
                        selected = geschlecht == "m",
                        onClick = { geschlecht = "m" },
                        label = { Text("Rüde") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    FilterChip(
                        selected = geschlecht == "w",
                        onClick = { geschlecht = "w" },
                        label = { Text("Hündin") }
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
            ) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}
