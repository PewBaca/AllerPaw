package com.allerpaw.app.ui.rezept

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
import com.allerpaw.app.domain.NaehrstoffErgebnis
import com.allerpaw.app.domain.NaehrstoffKatalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RezeptScreen(vm: RezeptViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Futterrechner") },
                actions = {
                    IconButton(onClick = vm::neuesRezept) {
                        Icon(Icons.Default.Add, "Neues Rezept")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.padding(padding).fillMaxSize(),
            contentPadding      = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Hund-Auswahl ─────────────────────────────────────────────
            item {
                HundSelectorCard(
                    hunde        = state.hunde,
                    selectedHund = state.selectedHundId,
                    onSelect     = vm::selectHund
                )
            }

            // ── Rezept-Auswahl ────────────────────────────────────────────
            if (state.hunde.isNotEmpty()) {
                item {
                    RezeptSelectorCard(
                        rezepte        = state.alleRezepte,
                        selectedRezept = state.rezept,
                        onSelect       = vm::selectRezept,
                        onNeu          = vm::neuesRezept
                    )
                }
            }

            // ── Skalierung ────────────────────────────────────────────────
            if (state.rezept != null) {
                item {
                    SkalierungsCard(
                        faktor         = state.skalierung,
                        onFaktorChange = vm::setSkalierung,
                        gesamtGramm    = state.gesamtGramm,
                        kcalGesamt     = state.kcalGesamt
                    )
                }

                // ── Ca:P + Omega 6:3 Badges ───────────────────────────────
                item {
                    VerhaeltnisRow(
                        caPVerhaeltnis = state.caPVerhaeltnis,
                        omega63        = state.omega63
                    )
                }

                // ── Vergleichsrezept ──────────────────────────────────────
                item {
                    VergleichsCard(
                        rezepte               = state.alleRezepte,
                        vergleichsRezeptId    = state.vergleichsRezeptId,
                        onSelectVergleich     = vm::setVergleichsRezept
                    )
                }
            }

            // ── NRC-Analyse ───────────────────────────────────────────────
            if (state.ergebnisse.isNotEmpty()) {
                item {
                    Text(
                        "NRC 2006 Analyse",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Gruppiert nach: Makro, Fettsäure, Mineral, Vitamin
                val gruppen = NaehrstoffKatalog.alle.groupBy { it.gruppe }
                gruppen.forEach { (gruppe, naehrstoffe) ->
                    item {
                        Text(
                            gruppe,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(naehrstoffe, key = { it.key }) { n ->
                        val ergebnis = state.ergebnisse.find { it.naehrstoff.key == n.key }
                        val vergleich = state.vergleichsErgebnisse.find { it.naehrstoff.key == n.key }
                        if (ergebnis != null) {
                            NaehrstoffBalken(
                                ergebnis  = ergebnis,
                                vergleich = vergleich,
                                modifier  = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            } else if (state.rezept != null && state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.rezept != null) {
                item {
                    Text(
                        "Noch keine Zutaten im Rezept. Zutaten unter Stammdaten → Zutaten anlegen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }

    // ── Rezept-Edit-Dialog ────────────────────────────────────────────────
    state.rezept?.let { rezept ->
        if (rezept.id == 0L) {
            RezeptEditDialog(
                rezept    = rezept,
                zutaten   = state.zutatenDraft,
                alleZutaten = state.alleZutaten,
                onDismiss = { /* TODO: dismiss neues Rezept */ },
                onSave    = { r, z -> vm.saveRezept(r, z) }
            )
        }
    }
}

// ── Sub-Composables ───────────────────────────────────────────────────────────

@Composable
private fun HundSelectorCard(
    hunde: List<com.allerpaw.app.data.local.entity.HundEntity>,
    selectedHund: Long?,
    onSelect: (Long) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Hund", style = MaterialTheme.typography.titleSmall)
            if (hunde.isEmpty()) {
                Text("Erst einen Hund unter dem Tab \"Hunde\" anlegen.",
                    color = MaterialTheme.colorScheme.outline)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    hunde.forEach { hund ->
                        FilterChip(
                            selected = hund.id == selectedHund,
                            onClick  = { onSelect(hund.id) },
                            label    = { Text(hund.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RezeptSelectorCard(
    rezepte: List<com.allerpaw.app.data.local.entity.RezeptEntity>,
    selectedRezept: com.allerpaw.app.data.local.entity.RezeptEntity?,
    onSelect: (com.allerpaw.app.data.local.entity.RezeptEntity) -> Unit,
    onNeu: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Rezept", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onNeu) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    Text("Neu")
                }
            }
            if (rezepte.isEmpty()) {
                Text("Noch kein Rezept für diesen Hund.",
                    color = MaterialTheme.colorScheme.outline)
            } else {
                rezepte.forEach { rezept ->
                    FilterChip(
                        selected = rezept.id == selectedRezept?.id,
                        onClick  = { onSelect(rezept) },
                        label    = { Text(rezept.name.ifBlank { "Rezept #${rezept.id}" }) },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SkalierungsCard(
    faktor: Float,
    onFaktorChange: (Float) -> Unit,
    gesamtGramm: Double,
    kcalGesamt: Double
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Skalierung & Energie", style = MaterialTheme.typography.titleSmall)

            // Schnell-Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0.25f, 0.5f, 1.0f, 2.0f).forEach { f ->
                    FilterChip(
                        selected = faktor == f,
                        onClick  = { onFaktorChange(f) },
                        label    = { Text("×${if (f < 1) f else f.toInt()}") }
                    )
                }
            }

            Slider(
                value         = faktor,
                onValueChange = onFaktorChange,
                valueRange    = 0.25f..3.0f,
                steps         = 22,
                modifier      = Modifier.fillMaxWidth()
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Gesamt: ${String.format("%.0f", gesamtGramm)} g",
                    style = MaterialTheme.typography.bodySmall)
                Text("${String.format("%.0f", kcalGesamt)} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun VerhaeltnisRow(caPVerhaeltnis: Double?, omega63: Double?) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Ca:P
        caPVerhaeltnis?.let { cap ->
            val ok = cap in 1.2..1.5
            AssistChip(
                onClick = {},
                label   = { Text("Ca:P = ${String.format("%.2f", cap)}:1") },
                colors  = AssistChipDefaults.assistChipColors(
                    containerColor = if (ok) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                )
            )
        }
        // Omega 6:3
        omega63?.let { o ->
            val ok = o in 5.0..10.0
            AssistChip(
                onClick = {},
                label   = { Text("Ω6:3 = ${String.format("%.1f", o)}:1") },
                colors  = AssistChipDefaults.assistChipColors(
                    containerColor = if (ok) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                )
            )
        }
    }
}

@Composable
private fun VergleichsCard(
    rezepte: List<com.allerpaw.app.data.local.entity.RezeptEntity>,
    vergleichsRezeptId: Long?,
    onSelectVergleich: (Long?) -> Unit
) {
    if (rezepte.size < 2) return
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Vergleich mit Rezept B", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = vergleichsRezeptId == null,
                    onClick  = { onSelectVergleich(null) },
                    label    = { Text("Kein Vergleich") }
                )
                rezepte.forEach { r ->
                    FilterChip(
                        selected = r.id == vergleichsRezeptId,
                        onClick  = { onSelectVergleich(r.id) },
                        label    = { Text(r.name.ifBlank { "#${r.id}" }) }
                    )
                }
            }
        }
    }
}

// ── Rezept-Edit-Dialog ────────────────────────────────────────────────────────

@Composable
private fun RezeptEditDialog(
    rezept: com.allerpaw.app.data.local.entity.RezeptEntity,
    zutaten: List<RezeptZutatDraft>,
    alleZutaten: List<com.allerpaw.app.data.local.entity.ZutatEntity>,
    onDismiss: () -> Unit,
    onSave: (com.allerpaw.app.data.local.entity.RezeptEntity, List<RezeptZutatDraft>) -> Unit
) {
    var name      by remember { mutableStateOf(rezept.name) }
    var gekocht   by remember { mutableStateOf(rezept.gekocht) }
    var portionen by remember { mutableStateOf(rezept.portionenProTag.toString()) }
    val positionen = remember { mutableStateListOf<RezeptZutatDraft>().also { it.addAll(zutaten) } }
    var showZutatPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rezept anlegen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name *") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(gekocht, { gekocht = it })
                    Text("Gekocht (B-Vitamine -30%)")
                }
                OutlinedTextField(portionen, { portionen = it },
                    label = { Text("Portionen/Tag") },
                    modifier = Modifier.width(120.dp), singleLine = true)

                Text("Zutaten", style = MaterialTheme.typography.titleSmall)
                positionen.forEachIndexed { i, pos ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(pos.zutatName.ifBlank { "Zutat #${pos.zutatId}" },
                            Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text("${String.format("%.0f", pos.mengeG)} g",
                            style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { positionen.removeAt(i) }) {
                            Icon(Icons.Default.Close, "Entfernen",
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
                TextButton(onClick = { showZutatPicker = true }) {
                    Icon(Icons.Default.Add, null)
                    Text("Zutat hinzufügen")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(rezept.copy(
                        name           = name.trim(),
                        gekocht        = gekocht,
                        portionenProTag = portionen.toIntOrNull() ?: 2
                    ), positionen.toList())
                }
            ) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )

    if (showZutatPicker) {
        ZutatPickerDialog(
            alleZutaten = alleZutaten,
            onDismiss   = { showZutatPicker = false },
            onAdd       = { zutat, mengeG ->
                positionen.add(RezeptZutatDraft(
                    zutatId  = zutat.id,
                    zutatName = zutat.name,
                    mengeG   = mengeG
                ))
                showZutatPicker = false
            }
        )
    }
}

@Composable
private fun ZutatPickerDialog(
    alleZutaten: List<com.allerpaw.app.data.local.entity.ZutatEntity>,
    onDismiss: () -> Unit,
    onAdd: (com.allerpaw.app.data.local.entity.ZutatEntity, Double) -> Unit
) {
    var selected by remember { mutableStateOf<com.allerpaw.app.data.local.entity.ZutatEntity?>(null) }
    var eingabe  by remember { mutableStateOf("") }
    var suche    by remember { mutableStateOf("") }

    val gefiltert = alleZutaten.filter {
        suche.isBlank() || it.name.contains(suche, ignoreCase = true)
    }

    // Label und Hinweistext je nach perMode der gewählten Zutat
    val (eingabeLabel, eingabeHinweis, berechneG) = remember(selected) {
        when (selected?.perMode) {
            "tablette" -> Triple(
                "Anzahl Tabletten",
                "z.B. 0,5 = halbe Tablette · " +
                "1 Tbl. = ${selected?.tabletteGewichtG ?: 0.0} g",
                { anzahl: Double -> anzahl * (selected?.tabletteGewichtG ?: 0.0) }
            )
            "tropfen" -> Triple(
                "Anzahl Tropfen",
                "z.B. 5 Tropfen · " +
                "1 Tropfen = ${selected?.tropfenGewichtG ?: 0.0} g",
                { anzahl: Double -> anzahl * (selected?.tropfenGewichtG ?: 0.0) }
            )
            "pulver" -> Triple(
                "Menge (g)",
                "Pulver – Eingabe in Gramm",
                { g: Double -> g }
            )
            else -> Triple(
                "Menge (g)",
                "Frischgewicht in Gramm",
                { g: Double -> g }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zutat wählen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = suche, onValueChange = { suche = it },
                    label = { Text("Suchen…") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(gefiltert, key = { it.id }) { zutat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected?.id == zutat.id,
                                onClick  = { selected = zutat; eingabe = "" }
                            )
                            Column {
                                Text(zutat.name, style = MaterialTheme.typography.bodyMedium)
                                // Modus-Badge
                                val modusLabel = when (zutat.perMode) {
                                    "tablette" -> "Tablette · ${zutat.tabletteGewichtG} g/Stk"
                                    "tropfen"  -> "Tropfen · ${zutat.tropfenGewichtG} g/Tr"
                                    "pulver"   -> "Pulver"
                                    else       -> "Lebensmittel"
                                }
                                Text(modusLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }

                if (selected != null) {
                    OutlinedTextField(
                        value         = eingabe,
                        onValueChange = { eingabe = it },
                        label         = { Text(eingabeLabel) },
                        supportingText = { Text(eingabeHinweis) },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true
                    )

                    // Vorschau: berechnete Gramm
                    val anzahl = com.allerpaw.app.util.FloatParser.parse(eingabe) ?: 0.0
                    val gramm  = berechneG(anzahl)
                    if (gramm > 0) {
                        Text(
                            "= ${String.format("%.2f", gramm)} g",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected != null &&
                    (com.allerpaw.app.util.FloatParser.parse(eingabe) ?: 0.0) > 0,
                onClick = {
                    val z     = selected ?: return@TextButton
                    val anzahl = com.allerpaw.app.util.FloatParser.parse(eingabe) ?: return@TextButton
                    val gramm  = berechneG(anzahl)
                    onAdd(z, gramm)
                }
            ) { Text("Hinzufügen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}
