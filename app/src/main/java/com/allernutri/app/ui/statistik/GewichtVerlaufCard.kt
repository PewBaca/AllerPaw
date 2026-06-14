package com.allernutri.app.ui.statistik

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allernutri.app.R
import com.allernutri.app.data.local.entity.HundGewichtEntity
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Gewichtsverlauf-Karte mit:
 *   - Vico LineChart (letzte 15 Einträge, chronologisch)
 *   - Trend-Anzeige (↑↓→ mit Gewichtsdelta)
 *   - Scrollbare Chip-Liste aller Einträge mit Löschen-Option
 *   - FAB-artiger "Eintrag hinzufügen"-Button
 */
@Composable
fun GewichtVerlaufCard(
    eintraege: List<HundGewichtEntity>,
    onAddKlick: () -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Chronologisch sortiert (älteste zuerst) für Chart
    val chronologisch = eintraege.sortedBy { it.datum }
    val kgWerte       = chronologisch.map { it.gewichtKg }

    // Vico ModelProducer
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(kgWerte) {
        if (kgWerte.size >= 2) {
            modelProducer.runTransaction {
                lineSeries { series(kgWerte) }
            }
        }
    }

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // ── Header ─────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("⚖️", fontSize = 20.sp)
                    Text(
                        "Gewichtsverlauf",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                FilledTonalIconButton(onClick = onAddKlick) {
                    Icon(Icons.Default.Add, stringResource(R.string.cd_neuer_eintrag))
                }
            }

            // ── Leer-Zustand ───────────────────────────────────────────────
            if (eintraege.isEmpty()) {
                Box(
                    modifier          = Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment  = Alignment.Center
                ) {
                    Text(
                        "Noch kein Gewichtseintrag.\nTippe + um zu starten.",
                        style     = MaterialTheme.typography.bodySmall,
                        color     = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
                return@Column
            }

            // ── KPI-Zeile ──────────────────────────────────────────────────
            val aktuell = chronologisch.lastOrNull()?.gewichtKg
            val vorherig = if (chronologisch.size >= 2) chronologisch[chronologisch.size - 2].gewichtKg else null
            val delta    = if (aktuell != null && vorherig != null) aktuell - vorherig else null
            val min      = kgWerte.minOrNull()
            val max      = kgWerte.maxOrNull()

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GewichtKpi(
                    label = "Aktuell",
                    wert  = aktuell?.let { "%.1f kg".format(it) } ?: "–",
                    farbe = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                GewichtKpi(
                    label = "Trend",
                    wert  = when {
                        delta == null        -> "–"
                        delta > 0.05         -> "+%.1f kg ↑".format(delta)
                        delta < -0.05        -> "%.1f kg ↓".format(delta)
                        else                 -> "≈ stabil →"
                    },
                    farbe = when {
                        delta == null        -> MaterialTheme.colorScheme.outline
                        delta > 0.1          -> Color(0xFFFF9800)
                        delta < -0.1         -> MaterialTheme.colorScheme.error
                        else                 -> Color(0xFF4CAF50)
                    },
                    modifier = Modifier.weight(1f)
                )
                GewichtKpi(
                    label = "Spanne",
                    wert  = if (min != null && max != null && min != max)
                        "%.1f–%.1f kg".format(min, max) else "–",
                    farbe = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Vico LineChart ─────────────────────────────────────────────
            AnimatedVisibility(visible = kgWerte.size >= 2) {
                val datumFmt = DateTimeFormatter.ofPattern("dd.MM")
                val datumLabels = chronologisch.map { it.datum.format(datumFmt) }

                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(),
                        startAxis = VerticalAxis.rememberStart(
                            label = rememberTextComponent(
                                textSize = 10.sp,
                                color    = MaterialTheme.colorScheme.outline
                            )
                        ),
                        bottomAxis = HorizontalAxis.rememberBottom(
                            label = rememberTextComponent(
                                textSize = 9.sp,
                                color    = MaterialTheme.colorScheme.outline
                            ),

                        )
                    ),
                    modelProducer   = modelProducer,
                    scrollState     = rememberVicoScrollState(scrollEnabled = true),
                    modifier        = Modifier.fillMaxWidth().height(160.dp)
                )
            }

            // ── Eintrags-Liste (scrollbar, neueste zuerst) ────────────────
            if (eintraege.isNotEmpty()) {
                Text(
                    "Letzte ${eintraege.size} Einträge",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(eintraege, key = { it.id }) { eintrag ->
                        GewichtChip(
                            eintrag  = eintrag,
                            onDelete = { onDelete(eintrag.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GewichtKpi(label: String, wert: String, farbe: Color, modifier: Modifier) {
    OutlinedCard(modifier = modifier) {
        Column(
            Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(wert, style = MaterialTheme.typography.labelLarge, color = farbe)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun GewichtChip(eintrag: HundGewichtEntity, onDelete: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }
    val fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)

    InputChip(
        selected  = false,
        onClick   = { showDelete = !showDelete },
        label     = {
            Text(
                "${eintrag.datum.format(fmt)}  %.1f kg".format(eintrag.gewichtKg),
                style = MaterialTheme.typography.labelMedium
            )
        },
        trailingIcon = if (showDelete) {{
            IconButton(onClick = onDelete, modifier = Modifier.size(18.dp)) {
                Icon(Icons.Default.Close, stringResource(R.string.cd_loeschen),
                    modifier = Modifier.size(14.dp))
            }
        }} else null
    )
}

/**
 * Dialog zum Erfassen eines neuen Gewichtseintrags.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GewichtEingabeDialog(
    kg: String,
    datum: java.time.LocalDate,
    onKgChange: (String) -> Unit,
    onDatumChange: (java.time.LocalDate) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val istGueltig = com.allernutri.app.util.FloatParser.parse(kg) != null &&
            (com.allernutri.app.util.FloatParser.parse(kg) ?: 0.0) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gewicht eintragen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value         = kg,
                    onValueChange = onKgChange,
                    label         = { Text(stringResource(R.string.label_gewicht_kg)) },
                    placeholder   = { Text("z.B. 28,5") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    isError       = kg.isNotBlank() && !istGueltig,
                    supportingText = if (kg.isNotBlank() && !istGueltig) {{
                        Text("Ungültige Zahl (Komma oder Punkt erlaubt)")
                    }} else null
                )
                OutlinedTextField(
                    value         = datum.toString(),
                    onValueChange = { input ->
                        runCatching { java.time.LocalDate.parse(input) }
                            .onSuccess { onDatumChange(it) }
                    },
                    label      = { Text(stringResource(R.string.label_datum_start)) },
                    singleLine = true,
                    modifier   = Modifier.fillMaxWidth(),
                    placeholder = { Text("JJJJ-MM-TT") }
                )
                Text(
                    "Tipp: Komma oder Punkt als Dezimaltrenner möglich.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            TextButton(enabled = istGueltig, onClick = onSave) {
                Text(stringResource(R.string.btn_speichern))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_abbrechen)) }
        }
    )
}
