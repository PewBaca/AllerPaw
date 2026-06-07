package com.allernutri.app.ui.zutaten

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.allernutri.app.R
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allernutri.app.data.local.entity.ZutatEntity
import com.allernutri.app.util.UndoManager
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZutatenScreen(
    onNavigateToVergleich: () -> Unit = {},
    onNavigateToImport: () -> Unit = {},
    vm: ZutatenViewModel = hiltViewModel()
) {
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
                title = { Text(stringResource(R.string.cd_zutaten)) },
                actions = {
                    IconButton(onClick = onNavigateToImport) {
                        Icon(Icons.Default.CloudDownload, "Lebensmittel importieren")
                    }
                    IconButton(onClick = onNavigateToVergleich) {
                        Icon(Icons.Default.Balance, "Zutat-Vergleich")
                    }
                    IconButton(onClick = vm::editNew) {
                        Icon(Icons.Default.Add, stringResource(R.string.cd_neue_zutat))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = vm::editNew) {
                Icon(Icons.Default.Add, stringResource(R.string.cd_zutat_hinzufuegen))
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {

            // ── SearchBar + Filter-Toggle ─────────────────────────────────
            val kategorien by vm.verfuegbareKategorien.collectAsState()
            val hatAktivenFilter = state.filter.typ.isNotBlank() ||
                state.filter.kategorie.isNotBlank() ||
                state.filter.perMode.isNotBlank()

            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value         = state.suchbegriff,
                    onValueChange = vm::suche,
                    modifier      = Modifier.weight(1f),
                    placeholder   = { Text(stringResource(R.string.placeholder_suche)) },
                    leadingIcon   = { Icon(Icons.Default.Search, null) },
                    trailingIcon  = {
                        if (state.suchbegriff.isNotBlank()) {
                            IconButton(onClick = { vm.suche("") }) {
                                Icon(Icons.Default.Clear, stringResource(R.string.cd_loeschen))
                            }
                        }
                    },
                    singleLine = true
                )
                // Filter-Button
                BadgedBox(badge = {
                    if (hatAktivenFilter) Badge()
                }) {
                    IconButton(onClick = vm::toggleFilterPanel) {
                        Icon(
                            if (state.filterPanelOffen) Icons.Default.FilterListOff
                            else Icons.Default.FilterList,
                            "Filter",
                            tint = if (hatAktivenFilter) MaterialTheme.colorScheme.primary
                                   else LocalContentColor.current
                        )
                    }
                }
            }

            // ── Filter-Panel ──────────────────────────────────────────────
            androidx.compose.animation.AnimatedVisibility(
                visible = state.filterPanelOffen
            ) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                ) {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Filter", style = MaterialTheme.typography.titleSmall)
                            TextButton(onClick = vm::clearFilter) { Text("Zurücksetzen") }
                        }

                        // Typ-Filter
                        Text("Typ", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("" to "Alle", "lebensmittel" to "Lebensmittel",
                                "supplement" to "Supplement").forEach { (v, l) ->
                                FilterChip(
                                    selected = state.filter.typ == v,
                                    onClick  = {
                                        vm.setFilter(state.filter.copy(typ = v))
                                    },
                                    label    = { Text(l) }
                                )
                            }
                        }

                        // Eingabemodus-Filter
                        Text("Eingabemodus", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("" to "Alle", "100g" to "100g",
                                "tablette" to "Tablette", "tropfen" to "Tropfen",
                                "pulver" to "Pulver").forEach { (v, l) ->
                                FilterChip(
                                    selected = state.filter.perMode == v,
                                    onClick  = {
                                        vm.setFilter(state.filter.copy(perMode = v))
                                    },
                                    label    = { Text(l,
                                        style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        // Kategorie-Filter (dynamisch aus Daten)
                        if (kategorien.isNotEmpty()) {
                            Text("Kategorie", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline)
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = state.filter.kategorie.isBlank(),
                                        onClick  = { vm.setFilter(state.filter.copy(kategorie = "")) },
                                        label    = { Text("Alle") }
                                    )
                                }
                                items(kategorien) { kat ->
                                    FilterChip(
                                        selected = state.filter.kategorie == kat,
                                        onClick  = { vm.setFilter(state.filter.copy(kategorie = kat)) },
                                        label    = { Text(kat) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Ergebnis-Counter ──────────────────────────────────────────
            if (hatAktivenFilter || state.suchbegriff.isNotBlank()) {
                Text(
                    "${gefiltert.size} Zutaten gefunden",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            if (gefiltert.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SetMeal, null, Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (state.suchbegriff.isNotBlank() || hatAktivenFilter)
                                "Keine Zutaten gefunden"
                            else "Noch keine Zutaten",
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (state.suchbegriff.isNotBlank() || hatAktivenFilter) {
                            TextButton(onClick = vm::clearFilter) { Text("Filter zurücksetzen") }
                        } else {
                            TextButton(onClick = vm::editNew) { Text("Jetzt hinzufügen") }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(gefiltert, key = { it.id }) { zutat ->
                        ZutatCard(
                            zutat         = zutat,
                            onEdit        = { vm.editExisting(zutat) },
                            onNaehrstoffe = { vm.openNaehrstoffe(zutat) },
                            onDelete      = { vm.delete(zutat.id, zutat.name) }
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
                Icon(Icons.Default.Edit, stringResource(R.string.cd_bearbeiten))
            }
            IconButton(onClick = { showConfirm = true }) {
                Icon(Icons.Default.Delete, stringResource(R.string.cd_loeschen),
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title  = { Text(stringResource(R.string.dialog_zutat_loeschen)) },
            text   = { Text("${zutat.name}" wird gelöscht. Undo möglich.") },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; onDelete() }) { Text(stringResource(R.string.btn_loeschen)) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text(stringResource(R.string.btn_abbrechen)) }
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
    var name         by remember { mutableStateOf(zutat.name) }
    var hersteller   by remember { mutableStateOf(zutat.hersteller) }
    var kategorie    by remember { mutableStateOf(zutat.kategorie) }
    var typ          by remember { mutableStateOf(zutat.typ) }
    var perMode      by remember { mutableStateOf(zutat.perMode) }
    var tabGewicht   by remember { mutableStateOf(
        if (zutat.tabletteGewichtG > 0) zutat.tabletteGewichtG.toString() else "") }
    var tropfenG     by remember { mutableStateOf(
        if (zutat.tropfenGewichtG > 0) zutat.tropfenGewichtG.toString() else "") }
    var tropfenMl    by remember { mutableStateOf(
        if (zutat.tropfenVolumenMl > 0) zutat.tropfenVolumenMl.toString() else "") }
    var vitaminEForm by remember { mutableStateOf(zutat.vitaminEForm) }

    val perModes   = listOf("100g", "tablette", "tropfen", "pulver")
    val perLabels  = listOf("pro 100 g", "Tabletten", "Tropfen", "Pulver (g)")
    val vitEForms  = listOf("natuerlich", "synthetisch", "acetat_natuerlich", "acetat_synthetisch")
    val vitELabels = listOf("Natürlich (d-Alpha)", "Synthetisch (dl-Alpha)",
                            "Acetat natürlich", "Acetat synthetisch")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (zutat.id == 0L) stringResource(R.string.cd_neue_zutat) else "Zutat bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name *") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(hersteller, { hersteller = it }, label = { Text(stringResource(R.string.label_hersteller)) },
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
                    perModes.forEachIndexed { i, m ->
                        FilterChip(
                            selected = perMode == m,
                            onClick  = { perMode = m },
                            label    = { Text(perLabels[i]) }
                        )
                    }
                }

                // Tablettengewicht
                if (perMode == "tablette") {
                    OutlinedTextField(
                        tabGewicht, { tabGewicht = it },
                        label      = { Text("Gewicht je Tablette (g) *") },
                        supportingText = { Text("z.B. 0,5 für eine 500mg-Tablette") },
                        modifier   = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Tropfen-Felder
                if (perMode == "tropfen") {
                    OutlinedTextField(
                        tropfenG, { tropfenG = it },
                        label      = { Text("Gewicht je Tropfen (g) *") },
                        supportingText = { Text("z.B. 0,05 g pro Tropfen") },
                        modifier   = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        tropfenMl, { tropfenMl = it },
                        label      = { Text("Volumen je Tropfen (ml)") },
                        supportingText = { Text("Optional, z.B. 0,05 ml") },
                        modifier   = Modifier.fillMaxWidth(),
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
                        name               = name.trim(),
                        hersteller         = hersteller.trim(),
                        kategorie          = kategorie.trim(),
                        typ                = typ,
                        perMode            = perMode,
                        tabletteGewichtG   = tabGewicht.toDoubleOrNull() ?: 0.0,
                        tropfenGewichtG    = tropfenG.toDoubleOrNull() ?: 0.0,
                        tropfenVolumenMl   = tropfenMl.toDoubleOrNull() ?: 0.0,
                        vitaminEForm       = vitaminEForm
                    ))
                }
            ) { Text(stringResource(R.string.btn_speichern)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_abbrechen)) }
        }
    )
}
