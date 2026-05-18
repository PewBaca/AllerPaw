package com.allerpaw.app.ui.zutaten

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allerpaw.app.R
import com.allerpaw.app.data.local.entity.ZutatEntity
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZutatVergleichScreen(
    onNavigateUp: () -> Unit,
    vm: ZutatVergleichViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zutat-Vergleich ⚖️") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.cd_zurueck))
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

            // ── Zutat-Picker A/B ──────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    ZutatPickerBox(
                        label      = "Zutat A",
                        zutat      = state.zutatA,
                        suche      = state.suchA,
                        alleZutaten = state.alleZutaten,
                        onSuche    = vm::setSuchA,
                        onSelect   = vm::selectZutatA,
                        onClear    = vm::clearZutatA,
                        farbe      = MaterialTheme.colorScheme.primaryContainer,
                        modifier   = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick  = vm::tausche,
                        enabled  = state.zutatA != null && state.zutatB != null
                    ) {
                        Icon(Icons.Default.SwapHoriz, "Tauschen",
                            tint = if (state.zutatA != null && state.zutatB != null)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline)
                    }

                    ZutatPickerBox(
                        label      = "Zutat B",
                        zutat      = state.zutatB,
                        suche      = state.suchB,
                        alleZutaten = state.alleZutaten,
                        onSuche    = vm::setSuchB,
                        onSelect   = vm::selectZutatB,
                        onClear    = vm::clearZutatB,
                        farbe      = MaterialTheme.colorScheme.secondaryContainer,
                        modifier   = Modifier.weight(1f)
                    )
                }
            }

            // ── Leer-Zustand ──────────────────────────────────────────────
            if (state.zutatA == null || state.zutatB == null) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⚖️", fontSize = 56.sp)
                            Text(
                                "Wähle zwei Zutaten aus um\nihre Nährstoffe zu vergleichen.",
                                style     = MaterialTheme.typography.bodyMedium,
                                color     = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                return@LazyColumn
            }

            // ── Filter-Chips ──────────────────────────────────────────────
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = state.nurMitWerten,
                        onClick  = vm::toggleNurMitWerten,
                        label    = { Text("Nur mit Werten", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = state.nurUnterschiede,
                        onClick  = vm::toggleNurUnterschiede,
                        label    = { Text("Nur Unterschiede", style = MaterialTheme.typography.labelSmall) }
                    )
                    val anzahl = state.vergleichsZeilen.size
                    Text("$anzahl Zeilen",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.align(Alignment.CenterVertically))
                }
            }

            // ── Tabellen-Header ───────────────────────────────────────────
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Nährstoff",
                            style    = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(2f))
                        Text(
                            state.zutatA!!.name.take(10),
                            style     = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color     = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.End,
                            modifier  = Modifier.weight(1.2f))
                        Text(
                            state.zutatB!!.name.take(10),
                            style     = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color     = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.End,
                            modifier  = Modifier.weight(1.2f))
                        Text("Δ B–A",
                            style     = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color     = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.End,
                            modifier  = Modifier.weight(1f))
                    }
                }
            }

            // ── Gruppen + Zeilen ──────────────────────────────────────────
            val gruppen = state.vergleichsZeilen.groupBy { it.gruppe }
            gruppen.forEach { (gruppe, zeilen) ->
                item(key = "header_$gruppe") {
                    Text(
                        gruppe,
                        style    = MaterialTheme.typography.titleSmall,
                        color    = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                items(zeilen, key = { it.key }) { zeile ->
                    VergleichsZeile(zeile)
                }
            }

            // ── Hinweis ───────────────────────────────────────────────────
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.Top
                    ) {
                        Icon(Icons.Default.Info, null, Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline)
                        Text(
                            "Alle Werte per 100 g Frischgewicht. " +
                            "Δ = B − A (grün = B hat mehr, rot = B hat weniger). " +
                            "Leere Felder = Wert nicht in der Datenbank.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

// ── Zutat-Picker Box ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZutatPickerBox(
    label: String,
    zutat: ZutatEntity?,
    suche: String,
    alleZutaten: List<ZutatEntity>,
    onSuche: (String) -> Unit,
    onSelect: (ZutatEntity) -> Unit,
    onClear: () -> Unit,
    farbe: Color,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline)

        if (zutat != null) {
            // Gewählte Zutat anzeigen
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(10.dp).fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(zutat.name,
                            style    = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2)
                        if (zutat.kategorie.isNotBlank())
                            Text(zutat.kategorie, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline)
                    }
                    IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, stringResource(R.string.cd_entfernen),
                            modifier = Modifier.size(16.dp))
                    }
                }
            }
        } else {
            // Suche + Dropdown
            OutlinedTextField(
                value         = suche,
                onValueChange = { onSuche(it); expanded = true },
                placeholder   = { Text("Zutat suchen…", style = MaterialTheme.typography.labelSmall) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                textStyle     = MaterialTheme.typography.bodySmall,
                leadingIcon   = { Icon(Icons.Default.Search, null, Modifier.size(16.dp)) }
            )
            AnimatedVisibility(visible = expanded && suche.isNotBlank()) {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    val gefiltert = alleZutaten.filter {
                        it.name.contains(suche, ignoreCase = true)
                    }.take(5)
                    Column {
                        gefiltert.forEach { z ->
                            TextButton(
                                onClick  = { onSelect(z); expanded = false; onSuche("") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(z.name, style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Start,
                                    modifier  = Modifier.fillMaxWidth())
                            }
                        }
                        if (gefiltert.isEmpty()) {
                            Text("Keine Treffer",
                                Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}

// ── Vergleichs-Zeile ──────────────────────────────────────────────────────────

@Composable
private fun VergleichsZeile(zeile: NaehrstoffZeile) {
    val delta     = zeile.delta
    val hatDelta  = delta != null && abs(delta) > 0.001
    val deltaFarbe = when {
        !hatDelta      -> MaterialTheme.colorScheme.outline
        delta!! > 0    -> Color(0xFF4CAF50)   // B hat mehr → grün
        else           -> MaterialTheme.colorScheme.error
    }

    val hintergrund = when {
        !hatDelta -> Color.Transparent
        delta!! > 0  -> Color(0xFF4CAF50).copy(alpha = 0.04f)
        else         -> MaterialTheme.colorScheme.error.copy(alpha = 0.04f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(hintergrund)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Nährstoff-Name
        Column(Modifier.weight(2f)) {
            Text(zeile.label,
                style    = MaterialTheme.typography.bodySmall,
                maxLines = 1)
            Text(zeile.einheit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline)
        }

        // Wert A
        Text(
            zeile.wertA?.let { fmtWert(it) } ?: "–",
            style     = MaterialTheme.typography.bodySmall,
            color     = if (zeile.wertA != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
            textAlign = TextAlign.End,
            modifier  = Modifier.weight(1.2f)
        )

        // Wert B
        Text(
            zeile.wertB?.let { fmtWert(it) } ?: "–",
            style     = MaterialTheme.typography.bodySmall,
            color     = if (zeile.wertB != null) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.outlineVariant,
            textAlign = TextAlign.End,
            modifier  = Modifier.weight(1.2f)
        )

        // Delta
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            if (hatDelta && delta != null) {
                Text(
                    (if (delta > 0) "+" else "") + fmtWert(delta),
                    style = MaterialTheme.typography.labelSmall,
                    color = deltaFarbe
                )
                zeile.deltaProzent?.let { pct ->
                    if (abs(pct) >= 1.0) {
                        Text(
                            "${if (pct > 0) "+" else ""}${pct.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = deltaFarbe.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Text("–", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    textAlign = TextAlign.End)
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}

/** Kompaktes Zahlenformat: groß=0 Nachkomma, klein=3 Nachkomma */
private fun fmtWert(v: Double): String = when {
    v >= 100  -> "%.0f".format(v)
    v >= 10   -> "%.1f".format(v)
    v >= 1    -> "%.2f".format(v)
    v >= 0.01 -> "%.3f".format(v)
    else      -> "%.4f".format(v)
}
