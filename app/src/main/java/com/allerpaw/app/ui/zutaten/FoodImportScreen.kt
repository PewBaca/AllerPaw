package com.allerpaw.app.ui.zutaten

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allerpaw.app.R
import com.allerpaw.app.data.remote.FoodApiResult
import com.allerpaw.app.data.remote.FoodQuelle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodImportScreen(
    onNavigateUp: () -> Unit,
    onNavigateToApiKeys: () -> Unit,
    vm: FoodImportViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lebensmittel-Import") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.cd_zurueck))
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToApiKeys) {
                        Icon(Icons.Default.Key, "API-Keys")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.padding(padding).fillMaxSize(),
            contentPadding      = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ── Quellen-Chips ──────────────────────────────────────────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Quellen", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FoodQuelle.entries.forEach { quelle ->
                            val aktivierbar = when (quelle) {
                                FoodQuelle.USDA          -> state.usdaKeyVorhanden
                                FoodQuelle.EDAMAM        -> state.edamamKeyVorhanden
                                FoodQuelle.OPEN_FOOD_FACTS -> true
                            }
                            FilterChip(
                                selected = quelle in state.aktivQuellen,
                                onClick  = { vm.toggleQuelle(quelle) },
                                enabled  = aktivierbar,
                                label    = {
                                    Text(
                                        when (quelle) {
                                            FoodQuelle.USDA           -> "USDA"
                                            FoodQuelle.OPEN_FOOD_FACTS -> "Open Food"
                                            FoodQuelle.EDAMAM         -> "Edamam"
                                        },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = if (!aktivierbar) {{
                                    Icon(Icons.Default.LockOutline, null, Modifier.size(12.dp))
                                }} else null
                            )
                        }
                        if (!state.usdaKeyVorhanden || !state.edamamKeyVorhanden) {
                            TextButton(
                                onClick  = onNavigateToApiKeys,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Text("Keys konfigurieren",
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // ── Suchleiste ────────────────────────────────────────────────
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value         = state.sucheQuery,
                        onValueChange = vm::setQuery,
                        modifier      = Modifier.weight(1f),
                        placeholder   = { Text("z.B. Lachs, Hühnerbrust, Spinat…") },
                        singleLine    = true,
                        leadingIcon   = { Icon(Icons.Default.Search, null) },
                        trailingIcon  = if (state.sucheQuery.isNotBlank()) {{
                            IconButton(onClick = { vm.setQuery("") }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }} else null
                    )
                    Button(
                        onClick  = vm::suche,
                        enabled  = state.sucheQuery.isNotBlank() &&
                                   !state.isSearching &&
                                   state.aktivQuellen.isNotEmpty()
                    ) {
                        if (state.isSearching) CircularProgressIndicator(
                            Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Suche")
                    }
                }
            }

            // ── Fehler ────────────────────────────────────────────────────
            state.fehler?.let { fehler ->
                item {
                    Card(colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(Modifier.padding(12.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            Text(fehler, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f))
                            IconButton(onClick = vm::clearFehler, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    }
                }
            }

            // ── Import-Erfolg ─────────────────────────────────────────────
            state.importErfolgreich?.let { name ->
                item {
                    Card(colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Row(Modifier.padding(12.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = MaterialTheme.colorScheme.primary)
                            Text("✅ \"$name\" importiert!",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f))
                            IconButton(onClick = vm::clearErfolg, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    }
                }
            }

            // ── Detail-Ansicht der gewählten Zutat ───────────────────────
            state.selected?.let { selected ->
                item {
                    FoodDetailCard(
                        result      = selected,
                        isLoading   = state.isLoadingDetail,
                        isImporting = state.isImporting,
                        onImport    = vm::importiere,
                        onAbbruch   = vm::clearSelected
                    )
                }
                return@LazyColumn
            }

            // ── Ergebnis-Liste ────────────────────────────────────────────
            if (state.ergebnisse.isNotEmpty()) {
                item {
                    Text("${state.ergebnisse.size} Ergebnisse",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                }
                items(state.ergebnisse, key = { "${it.quelle}${it.fdcId}${it.barcodeOrId}${it.name}" }) { result ->
                    FoodErgebnisCard(result = result, onClick = { vm.selectErgebnis(result) })
                }
            }

            // ── Hinweis (leer) ────────────────────────────────────────────
            if (state.ergebnisse.isEmpty() && !state.isSearching && state.fehler == null) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🔍", fontSize = 48.sp)
                            Text("Suche nach Lebensmitteln in\nUSDA, Open Food Facts oder Edamam",
                                style     = MaterialTheme.typography.bodyMedium,
                                color     = MaterialTheme.colorScheme.outline,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

// ── Ergebnis-Karte (Listenansicht) ────────────────────────────────────────────

@Composable
private fun FoodErgebnisCard(result: FoodApiResult, onClick: () -> Unit) {
    val quelleFarbe = when (result.quelle) {
        FoodQuelle.USDA           -> Color(0xFF1565C0)
        FoodQuelle.OPEN_FOOD_FACTS -> Color(0xFF2E7D32)
        FoodQuelle.EDAMAM         -> Color(0xFFE65100)
    }
    ElevatedCard(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically) {
                    Text(result.name, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, maxLines = 2)
                }
                if (result.marke.isNotBlank())
                    Text(result.marke, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline, maxLines = 1)
                if (result.kategorie.isNotBlank())
                    Text(result.kategorie, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline, maxLines = 1)
                // Kompakte Nährwert-Vorschau
                val preview = buildList {
                    result.energieKcal?.let { add("${it.toInt()} kcal") }
                    result.proteinG?.let { add("P: ${String.format("%.1f", it)}g") }
                    result.fettG?.let { add("F: ${String.format("%.1f", it)}g") }
                }.joinToString("  ·  ")
                if (preview.isNotBlank())
                    Text(preview, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(color = quelleFarbe.copy(alpha = 0.12f), shape = MaterialTheme.shapes.extraSmall) {
                    Text(result.quelle.label.substringBefore(" "),
                        Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall, color = quelleFarbe)
                }
                Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

// ── Detail-Karte mit Nährstoff-Übersicht ─────────────────────────────────────

@Composable
private fun FoodDetailCard(
    result: FoodApiResult,
    isLoading: Boolean,
    isImporting: Boolean,
    onImport: () -> Unit,
    onAbbruch: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(result.name, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    if (result.marke.isNotBlank())
                        Text(result.marke, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    Text(result.quelle.label, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                }
                IconButton(onClick = onAbbruch, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, null)
                }
            }

            if (isLoading) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp))
                    Text("Lade vollständige Nährstoffe…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }

            HorizontalDivider()
            Text("Nährstoffe per 100 g", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline)

            // Makros
            NaehrstoffGruppe("Energie & Makros", listOf(
                "Energie"       to result.energieKcal?.let { "${it.toInt()} kcal" },
                "Protein"       to result.proteinG?.let { "${String.format("%.1f", it)} g" },
                "Fett"          to result.fettG?.let { "${String.format("%.1f", it)} g" },
                "Kohlenhydrate" to result.kohlenhydrateG?.let { "${String.format("%.1f", it)} g" },
                "Ballaststoffe" to result.ballaststoffeG?.let { "${String.format("%.1f", it)} g" }
            ))

            // Mineralstoffe
            NaehrstoffGruppe("Mineralstoffe", listOf(
                "Calcium"   to result.calciumMg?.let { "${String.format("%.1f", it)} mg" },
                "Phosphor"  to result.phosphorMg?.let { "${String.format("%.1f", it)} mg" },
                "Kalium"    to result.kaliumMg?.let { "${String.format("%.1f", it)} mg" },
                "Natrium"   to result.natriumMg?.let { "${String.format("%.1f", it)} mg" },
                "Magnesium" to result.magnesiumMg?.let { "${String.format("%.1f", it)} mg" },
                "Eisen"     to result.eisenMg?.let { "${String.format("%.2f", it)} mg" },
                "Zink"      to result.zinkMg?.let { "${String.format("%.2f", it)} mg" }
            ))

            // Vitamine
            NaehrstoffGruppe("Vitamine", listOf(
                "Vitamin A"   to result.vitaminAMcg?.let { "${String.format("%.1f", it)} µg" },
                "Vitamin D3"  to result.vitaminD3Mcg?.let { "${String.format("%.2f", it)} µg" },
                "Vitamin E"   to result.vitaminEMg?.let { "${String.format("%.2f", it)} mg" },
                "Vitamin B1"  to result.vitaminB1Mg?.let { "${String.format("%.3f", it)} mg" },
                "Vitamin B12" to result.vitaminB12Mcg?.let { "${String.format("%.3f", it)} µg" },
                "Vitamin C"   to result.vitaminCMg?.let { "${String.format("%.1f", it)} mg" },
                "Folsäure"    to result.folsaeureMcg?.let { "${String.format("%.1f", it)} µg" }
            ))

            HorizontalDivider()

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAbbruch, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.btn_abbrechen))
                }
                Button(
                    onClick  = onImport,
                    enabled  = !isImporting && !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isImporting) CircularProgressIndicator(
                        Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Als Zutat importieren")
                }
            }
        }
    }
}

@Composable
private fun NaehrstoffGruppe(titel: String, zeilen: List<Pair<String, String?>>) {
    val mitWert = zeilen.filter { it.second != null }
    if (mitWert.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(titel, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        mitWert.chunked(2).forEach { paar ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                paar.forEach { (name, wert) ->
                    Row(Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                        Text(wert ?: "", style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium)
                    }
                }
                if (paar.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
