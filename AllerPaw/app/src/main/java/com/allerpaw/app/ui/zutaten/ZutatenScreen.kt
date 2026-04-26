package com.allerpaw.app.ui.zutaten

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
import com.allerpaw.app.data.local.entity.ZutatEntity
import com.allerpaw.app.util.UndoManager
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZutatenScreen(vm: ZutatenViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val gefiltert by vm.gefilterteZutaten.collectAsState()
    val undoStack by vm.undoManager.stack.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Undo-Banner
    LaunchedEffect(undoStack) {
        undoStack.lastOrNull()?.let { item ->
            val result = snackbarHostState.showSnackbar(
                message     = item.label,
                actionLabel = "Rückgängig",
                duration    = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                vm.undoDelete(item.item)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zutaten") },
                actions = {
                    IconButton(onClick = vm::editNew) {
                        Icon(Icons.Default.Add, "Neue Zutat")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = vm::editNew) {
                Icon(Icons.Default.Add, "Zutat hinzufügen")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // Suchfeld
            OutlinedTextField(
                value         = state.suchbegriff,
                onValueChange = vm::suche,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder   = { Text("Suchen…") },
                leadingIcon   = { Icon(Icons.Default.Search, null) },
                singleLine    = true
            )

            if (gefiltert.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SetMeal, null, Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(12.dp))
                        Text("Noch keine Zutaten", color = MaterialTheme.colorScheme.outline)
                        TextButton(onClick = vm::editNew) { Text("Jetzt hinzufügen") }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(gefiltert, key = { it.id }) { zutat ->
                        ZutatCard(
                            zutat      = zutat,
                            onEdit     = { vm.editExisting(zutat) },
                            onNaehrstoffe = { vm.openNaehrstoffe(zutat) },
                            onDelete   = { vm.delete(zutat.id, zutat.name) }
                        )
                    }
                }
            }
        }
    }

    // Edit-Dialog
    state.editZutat?.let { zutat ->
        ZutatEditDialog(
            zutat     = zutat,
            onDismiss = vm::dismissEdit,
            onSave    = vm::save
        )
    }

    // Nährstoff-Dialog
    state.naehrstoffDialogZutat?.let { zutat ->
        NaehrstoffDialog(
            zutatId      = zutat.id,
            vitaminEForm = zutat.vitaminEForm,
            bestehend    = state.naehrstoffe,
            onDismiss    = vm::dismissNaehrstoffe,
            onSave       = { vm.saveNaehrstoffe(zutat.id, it) }
        )
    }
}

@Composable
private fun ZutatCard(
    zutat: ZutatEntity,
    onEdit: () -> Unit,
    onNaehrstoffe: () -> Unit,
    onDelete: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(zutat.name, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (zutat.kategorie.isNotBlank())
                        AssistChip(onClick = {}, label = { Text(zutat.kategorie) })
                    AssistChip(
                        onClick = {},
                        label   = { Text(if (zutat.typ == "supplement") "Supplement" else "Lebensmittel") }
                    )
                }
            }
            IconButton(onClick = onNaehrstoffe) {
                Icon(Icons.Default.Biotech, "Nährstoffe",
                    tint = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Bearbeiten")
            }
            IconButton(onClick = { showConfirm = true }) {
                Icon(Icons.Default.Delete, "Löschen",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title  = { Text("Zutat löschen?") },
            text   = { Text("„${zutat.name}“ wird gelöscht. Undo möglich.") },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; onDelete() }) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Abbrechen") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZutatEditDialog(
    zutat: ZutatEntity,
    onDismiss: () -> Unit,
    onSave: (ZutatEntity) -> Unit
) {
    var name        by remember { mutableStateOf(zutat.name) }
    var hersteller  by remember { mutableStateOf(zutat.hersteller) }
    var kategorie   by remember { mutableStateOf(zutat.kategorie) }
    var typ         by remember { mutableStateOf(zutat.typ) }
    var perMode     by remember { mutableStateOf(zutat.perMode) }
    var tabGewicht  by remember { mutableStateOf(
        if (zutat.tabletteGewichtG > 0) zutat.tabletteGewichtG.toString() else "") }
    var vitaminEForm by remember { mutableStateOf(zutat.vitaminEForm) }

    val perModes    = listOf("100g", "tablette", "tropfen", "pulver")
    val vitEForms   = listOf("natuerlich", "synthetisch", "acetat_natuerlich", "acetat_synthetisch")
    val vitELabels  = listOf("Natürlich", "Synthetisch", "Acetat natürlich", "Acetat synthetisch")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (zutat.id == 0L) "Neue Zutat" else "Zutat bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name *") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(hersteller, { hersteller = it }, label = { Text("Hersteller") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(kategorie, { kategorie = it }, label = { Text("Kategorie") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)

                // Typ
                Text("Typ", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("lebensmittel" to "Lebensmittel", "supplement" to "Supplement").forEach { (v, l) ->
                        FilterChip(selected = typ == v, onClick = { typ = v }, label = { Text(l) })
                    }
                }

                // Per-Mode
                Text("Eingabemodus", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    perModes.forEach { m ->
                        FilterChip(selected = perMode == m, onClick = { perMode = m },
                            label = { Text(m) })
                    }
                }

                // Tablettengewicht
                if (perMode == "tablette") {
                    OutlinedTextField(
                        tabGewicht, { tabGewicht = it },
                        label    = { Text("Gewicht je Tablette (g)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Vitamin-E-Form
                if (typ == "supplement") {
                    Text("Vitamin-E-Form", style = MaterialTheme.typography.labelMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        vitEForms.forEachIndexed { i, form ->
                            FilterChip(
                                selected = vitaminEForm == form,
                                onClick  = { vitaminEForm = form },
                                label    = { Text(vitELabels[i]) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(zutat.copy(
                        name              = name.trim(),
                        hersteller        = hersteller.trim(),
                        kategorie         = kategorie.trim(),
                        typ               = typ,
                        perMode           = perMode,
                        tabletteGewichtG  = tabGewicht.toDoubleOrNull() ?: 0.0,
                        vitaminEForm      = vitaminEForm
                    ))
                }
            ) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}
