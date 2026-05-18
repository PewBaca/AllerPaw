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
import com.allerpaw.app.domain.NrcLebensphasen
import androidx.compose.ui.res.stringResource
import com.allerpaw.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RezeptScreen(vm: RezeptViewModel = hiltViewModel()) {
    val state       by vm.state.collectAsState()
    val gefiltert   by vm.gefilterteRezepte.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.label_futterrechner)) },
                actions = {
                    IconButton(onClick = vm::neuesRezept) {
                        Icon(Icons.Default.Add, stringResource(R.string.cd_neues_rezept))
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

            // ── Rezept Suche + Kategorie-Filter ───────────────────────────
            if (state.hunde.isNotEmpty()) {
                item {
                    RezeptSucheUndFilter(
                        state    = state,
                        gefiltert = gefiltert,
                        vm       = vm
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

                // ── Manueller Kcal-Bedarf Hinweis ────────────────────────
                if (state.kcalBedarfManuellAktiv && state.effektiverKcal != null) {
                    item {
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text("🔥 Manuell",
                                        Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer)
                                }
                                Text(
                                    "Kcal-Bedarf: %.0f kcal/Tag (aus Hund-Profil)".format(state.effektiverKcal),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
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

                // ── Futterumstellungsrechner ───────────────────────────────
                item {
                    FutterUmstellungsCard(
                        alleRezepte         = state.alleRezepte,
                        tagesrationGramm    = state.gesamtGramm
                    )
                }
            }

            // ── Lebensphase-Picker ────────────────────────────────────────
            item {
                LebensphasePicker(
                    selected = state.lebensphase,
                    onSelect = vm::setLebensphase,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // ── NRC-Analyse ───────────────────────────────────────────────
            if (state.ergebnisse.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "NRC 2006 Analyse",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (state.lebensphase != NrcLebensphasen.Lebensphase.ADULT) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    state.lebensphase.label,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
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
                                ergebnis       = ergebnis,
                                toleranz       = state.toleranzMap[n.key],
                                vergleich      = vergleich,
                                onEditToleranz = { vm.editToleranz(n.key) },
                                modifier       = Modifier.padding(horizontal = 4.dp)
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
                rezept      = rezept,
                zutaten     = state.zutatenDraft,
                alleZutaten = state.alleZutaten,
                alleRezepte = state.alleRezepte,
                onDismiss   = { /* TODO: dismiss neues Rezept */ },
                onSave      = { r, z -> vm.saveRezept(r, z) }
            )
        }
    }

    // ── Toleranz-Edit-Dialog ──────────────────────────────────────────────
    state.editToleranz?.let { toleranz ->
        ToleranzEditDialog(
            toleranz        = toleranz,
            onDismiss       = vm::dismissToleranz,
            onSave          = vm::saveToleranz,
            onZuruecksetzen = { vm.toleranzZuruecksetzen(toleranz.naehrstoffKey) }
        )
    }
}

// ── Rezept Suche + Kategorie-Filter ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RezeptSucheUndFilter(
    state: RezeptUiState,
    gefiltert: List<com.allerpaw.app.data.local.entity.RezeptEntity>,
    vm: RezeptViewModel
) {
    var filterOffen by remember { mutableStateOf(false) }
    var gewählteHaupt by remember { mutableStateOf(state.filterKategorie) }
    val hatFilter = state.filterKategorie.isNotBlank() || state.rezeptSuche.isNotBlank()

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // Suche + Filter-Toggle
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value         = state.rezeptSuche,
                    onValueChange = vm::setRezeptSuche,
                    modifier      = Modifier.weight(1f),
                    placeholder   = { Text("Rezept suchen…") },
                    leadingIcon   = { Icon(Icons.Default.Search, null) },
                    trailingIcon  = {
                        if (state.rezeptSuche.isNotBlank())
                            IconButton(onClick = { vm.setRezeptSuche("") }) {
                                Icon(Icons.Default.Clear, null)
                            }
                    },
                    singleLine = true
                )
                BadgedBox(badge = { if (hatFilter) Badge() }) {
                    IconButton(onClick = { filterOffen = !filterOffen }) {
                        Icon(
                            if (filterOffen) Icons.Default.FilterListOff
                            else Icons.Default.FilterList,
                            "Filter",
                            tint = if (hatFilter) MaterialTheme.colorScheme.primary
                                   else LocalContentColor.current
                        )
                    }
                }
            }

            // Kategorie-Filter
            androidx.compose.animation.AnimatedVisibility(visible = filterOffen) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("Kategorie", style = MaterialTheme.typography.labelMedium)
                        if (hatFilter) {
                            TextButton(onClick = {
                                vm.clearRezeptFilter()
                                gewählteHaupt = ""
                            }) { Text(stringResource(R.string.btn_zuruecksetzen)) }
                        }
                    }

                    // Hauptkategorien
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = state.filterKategorie.isBlank(),
                                onClick  = {
                                    vm.setFilterKategorie("")
                                    gewählteHaupt = ""
                                },
                                label = { Text(stringResource(R.string.label_alle)) }
                            )
                        }
                        items(RezeptKategorien.alle) { haupt ->
                            FilterChip(
                                selected = state.filterKategorie == haupt.name,
                                onClick  = {
                                    gewählteHaupt = haupt.name
                                    vm.setFilterKategorie(haupt.name)
                                },
                                label = { Text("${haupt.emoji} ${haupt.name}") }
                            )
                        }
                    }

                    // Unterkategorien (wenn Hauptkategorie gewählt)
                    val unterkat = RezeptKategorien.unterkategorienFuer(gewählteHaupt)
                    if (unterkat.isNotEmpty()) {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = state.filterUnterkat.isBlank(),
                                    onClick  = { vm.setFilterKategorie(gewählteHaupt, "") },
                                    label    = { Text(stringResource(R.string.label_alle)) }
                                )
                            }
                            items(unterkat) { u ->
                                FilterChip(
                                    selected = state.filterUnterkat == u.name,
                                    onClick  = { vm.setFilterKategorie(gewählteHaupt, u.name) },
                                    label    = { Text("${u.emoji} ${u.name}") }
                                )
                            }
                        }
                    }
                }
            }

            // Rezept-Liste (gefiltert)
            Text("Rezept wählen", style = MaterialTheme.typography.labelMedium)
            if (gefiltert.isEmpty()) {
                Text(
                    if (hatFilter) "Kein Rezept in dieser Kategorie"
                    else "Noch kein Rezept für diesen Hund",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(gefiltert, key = { it.id }) { rezept ->
                        val kat = RezeptKategorien.hauptkategorie(rezept.kategorie)
                        FilterChip(
                            selected = rezept.id == state.rezept?.id,
                            onClick  = { vm.selectRezept(rezept) },
                            label    = {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    if (kat.isNotBlank()) {
                                        Text(RezeptKategorien.emoji(kat))
                                    }
                                    Text(rezept.name.ifBlank { "Rezept #${rezept.id}" })
                                }
                            }
                        )
                    }
                }
            }

            // Neues Rezept Button
            TextButton(onClick = vm::neuesRezept,
                modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.rezept_neu))
            }
        }
    }
}

// ── Bestehende Sub-Composables ────────────────────────────────────────────────

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
                    Text(stringResource(R.string.btn_neu))
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
                    label    = { Text(stringResource(R.string.rezept_kein_vergleich)) }
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
    alleRezepte: List<RezeptEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (com.allerpaw.app.data.local.entity.RezeptEntity, List<RezeptZutatDraft>) -> Unit
) {
    var name      by remember { mutableStateOf(rezept.name) }
    var gekocht   by remember { mutableStateOf(rezept.gekocht) }
    var portionen by remember { mutableStateOf(rezept.portionenProTag.toString()) }
    var kategorie by remember { mutableStateOf(rezept.kategorie) }
    var katHaupt  by remember { mutableStateOf(RezeptKategorien.hauptkategorie(rezept.kategorie)) }
    var katUnter  by remember { mutableStateOf(RezeptKategorien.unterkategorie(rezept.kategorie)) }
    val positionen = remember { mutableStateListOf<RezeptZutatDraft>().also { it.addAll(zutaten) } }

    var showZutatPicker    by remember { mutableStateOf(false) }
    var showSubRezeptPicker by remember { mutableStateOf(false) }
    var editPosition       by remember { mutableStateOf<Pair<Int, RezeptZutatDraft>?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rezept_anlegen)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // ── Rezept-Felder ─────────────────────────────────────────
                OutlinedTextField(name, { name = it },
                    label    = { Text(stringResource(R.string.label_name)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(gekocht, { gekocht = it })
                    Text(stringResource(R.string.label_gekocht))
                    Spacer(Modifier.width(16.dp))
                    OutlinedTextField(portionen, { portionen = it },
                        label    = { Text(stringResource(R.string.label_portionen_tag)) },
                        modifier = Modifier.width(110.dp), singleLine = true)
                }

                // ── Kategorie ─────────────────────────────────────────────
                Text(stringResource(R.string.label_kategorie), style = MaterialTheme.typography.labelMedium)
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    item {
                        FilterChip(selected = katHaupt.isBlank(),
                            onClick = { katHaupt = ""; katUnter = ""; kategorie = "" },
                            label   = { Text(stringResource(R.string.label_keine)) })
                    }
                    items(RezeptKategorien.alle) { haupt ->
                        FilterChip(
                            selected = katHaupt == haupt.name,
                            onClick  = { katHaupt = haupt.name; katUnter = ""; kategorie = haupt.name },
                            label    = { Text("${haupt.emoji} ${haupt.name}") })
                    }
                }
                if (katHaupt.isNotBlank()) {
                    val unterkat = RezeptKategorien.unterkategorienFuer(katHaupt)
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(unterkat) { u ->
                            FilterChip(
                                selected = katUnter == u.name,
                                onClick  = { katUnter = u.name; kategorie = RezeptKategorien.vollstaendig(katHaupt, u.name) },
                                label    = { Text("${u.emoji} ${u.name}") })
                        }
                    }
                    if (kategorie.isNotBlank()) {
                        Text("📁 $kategorie", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }

                HorizontalDivider()

                // ── Positionen ────────────────────────────────────────────
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically) {
                    Text(stringResource(R.string.label_zutaten),
                        style = MaterialTheme.typography.titleSmall)
                    // Gesamtgewicht
                    if (positionen.isNotEmpty()) {
                        Text("∑ ${String.format("%.0f", positionen.sumOf { it.mengeG })} g",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }

                positionen.forEachIndexed { i, pos ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                            // Positions-Emoji
                            Text(if (pos.subRezeptId != null) "📋" else "🍖",
                                style = MaterialTheme.typography.bodyMedium)

                            Column(Modifier.weight(1f)) {
                                Text(
                                    pos.zutatName.ifBlank { pos.subRezeptName.ifBlank { "?" } },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                                Text(
                                    pos.anzeigeText(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (pos.inhaltsstoffeFreitext.isNotBlank()) {
                                    Text(pos.inhaltsstoffeFreitext,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 1)
                                }
                            }

                            // Bearbeiten-Button
                            IconButton(onClick = { editPosition = i to pos },
                                modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, stringResource(R.string.cd_bearbeiten),
                                    modifier = Modifier.size(16.dp))
                            }

                            // Hoch/Runter
                            Column {
                                IconButton(onClick = {
                                    if (i > 0) { val tmp = positionen[i-1]; positionen[i-1] = pos; positionen[i] = tmp }
                                }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.KeyboardArrowUp, null, Modifier.size(16.dp))
                                }
                                IconButton(onClick = {
                                    if (i < positionen.size - 1) { val tmp = positionen[i+1]; positionen[i+1] = pos; positionen[i] = tmp }
                                }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(16.dp))
                                }
                            }

                            // Entfernen
                            IconButton(onClick = { positionen.removeAt(i) },
                                modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, stringResource(R.string.cd_entfernen),
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                // ── Hinzufügen-Buttons ─────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick  = { showZutatPicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.label_zutat_hinzufuegen),
                            style = MaterialTheme.typography.labelMedium)
                    }
                    if (alleRezepte.isNotEmpty()) {
                        OutlinedButton(
                            onClick  = { showSubRezeptPicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AddCircleOutline, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("+ Rezept-Mix", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(rezept.copy(
                        name            = name.trim(),
                        gekocht         = gekocht,
                        portionenProTag = portionen.toIntOrNull() ?: 2,
                        kategorie       = kategorie
                    ), positionen.toList())
                }
            ) { Text(stringResource(R.string.btn_speichern)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_abbrechen)) } }
    )

    // ── Zutat-Picker ──────────────────────────────────────────────────────
    if (showZutatPicker) {
        ZutatPickerDialog(
            alleZutaten = alleZutaten,
            onDismiss   = { showZutatPicker = false },
            onAdd       = { zutat, mengeG, anzahlTab, anzahlTrop, freitext ->
                positionen.add(RezeptZutatDraft(
                    zutatId              = zutat.id,
                    zutatName            = zutat.name,
                    zutatPerMode         = zutat.perMode,
                    zutatTabGewichtG     = zutat.tabletteGewichtG,
                    zutatTropfenGewichtG = zutat.tropfenGewichtG,
                    mengeG               = mengeG,
                    anzahlTabletten      = anzahlTab,
                    anzahlTropfen        = anzahlTrop,
                    inhaltsstoffeFreitext = freitext
                ))
                showZutatPicker = false
            }
        )
    }

    // ── Sub-Rezept-Picker ─────────────────────────────────────────────────
    if (showSubRezeptPicker) {
        SubRezeptPickerDialog(
            alleRezepte    = alleRezepte.filter { it.id != rezept.id },
            onDismiss      = { showSubRezeptPicker = false },
            onAdd          = { subRezept, mengeG ->
                positionen.add(RezeptZutatDraft(
                    subRezeptId   = subRezept.id,
                    subRezeptName = subRezept.name,
                    mengeG        = mengeG
                ))
                showSubRezeptPicker = false
            }
        )
    }

    // ── Position bearbeiten ───────────────────────────────────────────────
    editPosition?.let { (index, pos) ->
        PositionEditDialog(
            position  = pos,
            onDismiss = { editPosition = null },
            onSave    = { updated ->
                positionen[index] = updated
                editPosition = null
            }
        )
    }
}

// ── Position nachträglich bearbeiten ──────────────────────────────────────────

@Composable
private fun PositionEditDialog(
    position: RezeptZutatDraft,
    onDismiss: () -> Unit,
    onSave: (RezeptZutatDraft) -> Unit
) {
    var mengeGInput  by remember { mutableStateOf(String.format("%.1f", position.mengeG)) }
    var freitext     by remember { mutableStateOf(position.inhaltsstoffeFreitext) }

    val menge = com.allerpaw.app.util.FloatParser.parse(mengeGInput)
    val istGueltig = menge != null && menge > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(position.zutatName.ifBlank { position.subRezeptName.ifBlank { "Position" } })
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                OutlinedTextField(
                    value         = mengeGInput,
                    onValueChange = { mengeGInput = it },
                    label         = { Text("Menge (g)") },
                    placeholder   = { Text("Frischgewicht in g") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    isError       = mengeGInput.isNotBlank() && !istGueltig,
                    supportingText = if (istGueltig) {{
                        Text("= ${String.format("%.2f", menge)} g",
                            color = MaterialTheme.colorScheme.primary)
                    }} else null
                )

                // Tabletten/Tropfen-Anzeige
                if (position.zutatPerMode == "tablette" && menge != null) {
                    val anzahl = menge / position.zutatTabGewichtG.coerceAtLeast(0.001)
                    Text("≈ ${String.format("%.1f", anzahl)} Tabletten",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                }
                if (position.zutatPerMode == "tropfen" && menge != null) {
                    val anzahl = menge / position.zutatTropfenGewichtG.coerceAtLeast(0.001)
                    Text("≈ ${String.format("%.0f", anzahl)} Tropfen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                }

                OutlinedTextField(
                    value         = freitext,
                    onValueChange = { freitext = it },
                    label         = { Text(stringResource(R.string.label_notizen_freitext)) },
                    placeholder   = { Text("z.B. Inhaltsstoffe, Charge, Hinweis") },
                    modifier      = Modifier.fillMaxWidth(),
                    minLines      = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = istGueltig,
                onClick = {
                    val neuesMenge = menge ?: return@TextButton
                    val anzTab = if (position.zutatPerMode == "tablette")
                        neuesMenge / position.zutatTabGewichtG.coerceAtLeast(0.001) else null
                    val anzTrop = if (position.zutatPerMode == "tropfen")
                        neuesMenge / position.zutatTropfenGewichtG.coerceAtLeast(0.001) else null
                    onSave(position.copy(
                        mengeG                = neuesMenge,
                        anzahlTabletten       = anzTab,
                        anzahlTropfen         = anzTrop,
                        inhaltsstoffeFreitext = freitext.trim()
                    ))
                }
            ) { Text(stringResource(R.string.btn_speichern)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_abbrechen)) } }
    )
}

// ── Sub-Rezept-Picker ─────────────────────────────────────────────────────────

@Composable
private fun SubRezeptPickerDialog(
    alleRezepte: List<RezeptEntity>,
    onDismiss: () -> Unit,
    onAdd: (RezeptEntity, Double) -> Unit
) {
    var selected by remember { mutableStateOf<RezeptEntity?>(null) }
    var mengeG   by remember { mutableStateOf("") }

    val menge      = com.allerpaw.app.util.FloatParser.parse(mengeG)
    val istGueltig = selected != null && menge != null && menge > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rezept-Mix hinzufügen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Wähle ein Rezept als Mix-Komponente:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)

                LazyColumn(Modifier.heightIn(max = 200.dp)) {
                    items(alleRezepte, key = { it.id }) { rezept ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected?.id == rezept.id,
                                onClick  = { selected = rezept; mengeG = "" }
                            )
                            Column {
                                Text(rezept.name, style = MaterialTheme.typography.bodyMedium)
                                if (rezept.kategorie.isNotBlank()) {
                                    Text(rezept.kategorie,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }

                if (selected != null) {
                    OutlinedTextField(
                        value         = mengeG,
                        onValueChange = { mengeG = it },
                        label         = { Text("Gesamtmenge des Sub-Rezepts (g)") },
                        placeholder   = { Text("z.B. 200") },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        supportingText = { Text("Interne Zutaten werden anteilig skaliert.") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = istGueltig,
                onClick = {
                    val r = selected ?: return@TextButton
                    val g = menge ?: return@TextButton
                    onAdd(r, g)
                }
            ) { Text(stringResource(R.string.btn_hinzufuegen)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_abbrechen)) } }
    )
}

// ── Zutat-Picker ──────────────────────────────────────────────────────────────

@Composable
private fun ZutatPickerDialog(
    alleZutaten: List<com.allerpaw.app.data.local.entity.ZutatEntity>,
    onDismiss: () -> Unit,
    onAdd: (com.allerpaw.app.data.local.entity.ZutatEntity, Double, Double?, Double?, String) -> Unit
) {
    var selected by remember { mutableStateOf<com.allerpaw.app.data.local.entity.ZutatEntity?>(null) }
    var eingabe  by remember { mutableStateOf("") }
    var suche    by remember { mutableStateOf("") }
    var freitext by remember { mutableStateOf("") }

    val gefiltert = alleZutaten.filter {
        suche.isBlank() || it.name.contains(suche, ignoreCase = true)
    }

    val (eingabeLabel, eingabeHinweis, berechneG) = remember(selected) {
        when (selected?.perMode) {
            "tablette" -> Triple(
                "Anzahl Tabletten",
                "z.B. 0,5 = halbe Tablette · 1 Tbl. = ${selected?.tabletteGewichtG ?: 0.0} g",
                { anzahl: Double -> anzahl * (selected?.tabletteGewichtG ?: 0.0) }
            )
            "tropfen" -> Triple(
                "Anzahl Tropfen",
                "z.B. 5 Tropfen · 1 Tr. = ${selected?.tropfenGewichtG ?: 0.0} g",
                { anzahl: Double -> anzahl * (selected?.tropfenGewichtG ?: 0.0) }
            )
            "pulver" -> Triple("Menge (g)", "Pulver – Eingabe in Gramm", { g: Double -> g })
            else     -> Triple("Menge (g)", "Frischgewicht in Gramm", { g: Double -> g })
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cd_zutat_hinzufuegen)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = suche, onValueChange = { suche = it },
                    label       = { Text(stringResource(R.string.placeholder_suche)) },
                    modifier    = Modifier.fillMaxWidth(),
                    singleLine  = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )

                LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                    items(gefiltert, key = { it.id }) { zutat ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected?.id == zutat.id,
                                onClick  = { selected = zutat; eingabe = "" }
                            )
                            Column {
                                Text(zutat.name, style = MaterialTheme.typography.bodyMedium)
                                val modusLabel = when (zutat.perMode) {
                                    "tablette" -> "💊 ${zutat.tabletteGewichtG} g/Stk"
                                    "tropfen"  -> "💧 ${zutat.tropfenGewichtG} g/Tr"
                                    "pulver"   -> "🧂 Pulver"
                                    else       -> "🍖 ${zutat.kategorie}"
                                }
                                Text(modusLabel, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }

                if (selected != null) {
                    OutlinedTextField(
                        value          = eingabe,
                        onValueChange  = { eingabe = it },
                        label          = { Text(eingabeLabel) },
                        supportingText = { Text(eingabeHinweis) },
                        modifier       = Modifier.fillMaxWidth(),
                        singleLine     = true
                    )

                    val anzahl = com.allerpaw.app.util.FloatParser.parse(eingabe) ?: 0.0
                    val gramm  = berechneG(anzahl)
                    if (gramm > 0) {
                        Text("= ${String.format("%.2f", gramm)} g",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }

                    OutlinedTextField(
                        value         = freitext,
                        onValueChange = { freitext = it },
                        label         = { Text(stringResource(R.string.label_notizen_freitext) + " (optional)") },
                        placeholder   = { Text("z.B. Charge, Hersteller, Hinweis") },
                        modifier      = Modifier.fillMaxWidth(),
                        minLines      = 2
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected != null &&
                    (com.allerpaw.app.util.FloatParser.parse(eingabe) ?: 0.0) > 0,
                onClick = {
                    val z      = selected ?: return@TextButton
                    val anzahl = com.allerpaw.app.util.FloatParser.parse(eingabe) ?: return@TextButton
                    val gramm  = berechneG(anzahl)
                    val anzTab = if (z.perMode == "tablette") anzahl else null
                    val anzTr  = if (z.perMode == "tropfen") anzahl else null
                    onAdd(z, gramm, anzTab, anzTr, freitext.trim())
                }
            ) { Text(stringResource(R.string.btn_hinzufuegen)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_abbrechen)) } }
    )
}
 {
    var name      by remember { mutableStateOf(rezept.name) }
    var gekocht   by remember { mutableStateOf(rezept.gekocht) }
    var portionen by remember { mutableStateOf(rezept.portionenProTag.toString()) }
    var kategorie by remember { mutableStateOf(rezept.kategorie) }
    var katHaupt  by remember {
        mutableStateOf(RezeptKategorien.hauptkategorie(rezept.kategorie))
    }
    var katUnter  by remember {
        mutableStateOf(RezeptKategorien.unterkategorie(rezept.kategorie))
    }
    val positionen = remember { mutableStateListOf<RezeptZutatDraft>().also { it.addAll(zutaten) } }
    var showZutatPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rezept_anlegen)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.label_name)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(gekocht, { gekocht = it })
                    Text(stringResource(R.string.label_gekocht))
                }
                OutlinedTextField(portionen, { portionen = it },
                    label = { Text(stringResource(R.string.label_portionen_tag)) },
                    modifier = Modifier.width(120.dp), singleLine = true)

                // Kategorie-Auswahl
                Text("Kategorie", style = MaterialTheme.typography.labelMedium)
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = katHaupt.isBlank(),
                            onClick  = { katHaupt = ""; katUnter = ""; kategorie = "" },
                            label    = { Text(stringResource(R.string.label_keine)) }
                        )
                    }
                    items(RezeptKategorien.alle) { haupt ->
                        FilterChip(
                            selected = katHaupt == haupt.name,
                            onClick  = {
                                katHaupt  = haupt.name
                                katUnter  = ""
                                kategorie = haupt.name
                            },
                            label = { Text("${haupt.emoji} ${haupt.name}") }
                        )
                    }
                }

                // Unterkategorien
                if (katHaupt.isNotBlank()) {
                    val unterkat = RezeptKategorien.unterkategorienFuer(katHaupt)
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(unterkat) { u ->
                            FilterChip(
                                selected = katUnter == u.name,
                                onClick  = {
                                    katUnter  = u.name
                                    kategorie = RezeptKategorien.vollstaendig(katHaupt, u.name)
                                },
                                label = { Text("${u.emoji} ${u.name}") }
                            )
                        }
                    }
                    if (kategorie.isNotBlank()) {
                        Text("📁 $kategorie",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }

                Text("Zutaten", style = MaterialTheme.typography.titleSmall)
                positionen.forEachIndexed { i, pos ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(pos.zutatName.ifBlank { "Zutat #${pos.zutatId}" },
                            Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text("${String.format("%.0f", pos.mengeG)} g",
                            style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { positionen.removeAt(i) }) {
                            Icon(Icons.Default.Close, stringResource(R.string.cd_entfernen),
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
                TextButton(onClick = { showZutatPicker = true }) {
                    Icon(Icons.Default.Add, null)
                    Text(stringResource(R.string.label_zutat_hinzufuegen))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(rezept.copy(
                        name            = name.trim(),
                        gekocht         = gekocht,
                        portionenProTag = portionen.toIntOrNull() ?: 2,
                        kategorie       = kategorie
                    ), positionen.toList())
                }
            ) { Text(stringResource(R.string.btn_speichern)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_abbrechen)) } }
    )

    if (showZutatPicker) {
        ZutatPickerDialog(
            alleZutaten = alleZutaten,
            onDismiss   = { showZutatPicker = false },
            onAdd       = { zutat, mengeG, anzahlTab, anzahlTrop ->
                positionen.add(RezeptZutatDraft(
                    zutatId              = zutat.id,
                    zutatName            = zutat.name,
                    zutatPerMode         = zutat.perMode,
                    zutatTabGewichtG     = zutat.tabletteGewichtG,
                    zutatTropfenGewichtG = zutat.tropfenGewichtG,
                    mengeG               = mengeG,
                    anzahlTabletten      = anzahlTab,
                    anzahlTropfen        = anzahlTrop
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
    // zutat, gramm, anzahlTabletten, anzahlTropfen
    onAdd: (com.allerpaw.app.data.local.entity.ZutatEntity, Double, Double?, Double?) -> Unit
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
                    label = { Text(stringResource(R.string.placeholder_suche)) },
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
                    val z      = selected ?: return@TextButton
                    val anzahl = com.allerpaw.app.util.FloatParser.parse(eingabe) ?: return@TextButton
                    val gramm  = berechneG(anzahl)
                    val anzTab = if (z.perMode == "tablette") anzahl else null
                    val anzTr  = if (z.perMode == "tropfen") anzahl else null
                    onAdd(z, gramm, anzTab, anzTr)
                }
            ) { Text(stringResource(R.string.btn_hinzufuegen)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_abbrechen)) } }
    )
}
