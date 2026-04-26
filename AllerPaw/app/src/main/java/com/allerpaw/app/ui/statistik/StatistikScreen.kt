package com.allerpaw.app.ui.statistik

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatistikScreen(vm: StatistikViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Statistik") }) }
    ) { padding ->
        if (state.hunde.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.BarChart, null, Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(12.dp))
                    Text("Erst einen Hund anlegen um Statistiken zu sehen.",
                        color = MaterialTheme.colorScheme.outline)
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Hund-Auswahl ─────────────────────────────────────────────
            item {
                HundFilterCard(
                    hunde        = state.hunde,
                    selectedId   = state.selectedHundId,
                    onSelect     = vm::selectHund
                )
            }

            // ── Zeitraum-Filter ───────────────────────────────────────────
            item {
                ZeitraumCard(
                    aktuellerZeitraum = state.zeitraumTage,
                    onSelect          = vm::setZeitraum
                )
            }

            // ── Ladeindikator ─────────────────────────────────────────────
            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                return@LazyColumn
            }

            // ── KPI-Kacheln ───────────────────────────────────────────────
            item { KpiRow(state.kpi) }

            // ── Symptom-Verlauf ───────────────────────────────────────────
            if (state.symptomVerlauf.isNotEmpty()) {
                item {
                    Text("Symptom-Verlauf", style = MaterialTheme.typography.titleMedium)
                }
                item { SymptomVerlaufChart(state.symptomVerlauf) }
            }

            // ── Heatmap (ab 14 Einträgen) ─────────────────────────────────
            item {
                if (!state.heatmapVerfuegbar) {
                    InfoCard(
                        icon = Icons.Default.Info,
                        text = "Symptom-Heatmap verfügbar ab 14 Einträgen " +
                               "(aktuell ${state.kpi.symptomTage})"
                    )
                } else {
                    Text("Symptom-Heatmap", style = MaterialTheme.typography.titleMedium)
                }
            }

            if (state.heatmapVerfuegbar) {
                item { HeatmapCard(state.heatmap) }
            }

            // ── Korrelation Pollen ↔ Symptom ──────────────────────────────
            if (state.korrelationVerfuegbar) {
                item {
                    Text("Pollen-Korrelation", style = MaterialTheme.typography.titleMedium)
                }
                items(state.korrelationen, key = { it.gruppe }) { k ->
                    KorrelationsCard(k)
                }
            } else {
                item {
                    InfoCard(
                        icon = Icons.Default.ScatterPlot,
                        text = "Korrelationsanalyse verfügbar ab 3 Datenpunkten " +
                               "mit Pollenbelastung > 1"
                    )
                }
            }

            // ── Phasen-Timeline ───────────────────────────────────────────
            if (state.phasen.isNotEmpty()) {
                item {
                    Text("Phasen-Timeline", style = MaterialTheme.typography.titleMedium)
                }
                items(state.phasen, key = { it.id }) { phase ->
                    PhasenTimelineCard(phase)
                }
            }
        }
    }
}

// ── Sub-Composables ───────────────────────────────────────────────────────────

@Composable
private fun HundFilterCard(
    hunde: List<com.allerpaw.app.data.local.entity.HundEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            hunde.forEach { hund ->
                FilterChip(
                    selected = hund.id == selectedId,
                    onClick  = { onSelect(hund.id) },
                    label    = { Text(hund.name) }
                )
            }
        }
    }
}

@Composable
private fun ZeitraumCard(aktuellerZeitraum: Int, onSelect: (Int) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Zeitraum", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(30 to "30 T", 90 to "90 T", 180 to "6 M",
                       365 to "1 J", 0 to "Alles").forEach { (tage, label) ->
                    FilterChip(
                        selected = aktuellerZeitraum == tage,
                        onClick  = { onSelect(tage) },
                        label    = { Text(label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiRow(kpi: KpiState) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KpiKachel(
            label = "Symptomtage",
            wert  = "${kpi.symptomTage}",
            icon  = Icons.Default.Sick,
            modifier = Modifier.weight(1f)
        )
        KpiKachel(
            label = "Ø Schweregrad",
            wert  = String.format("%.1f", kpi.durchschnittSchweregrad),
            icon  = Icons.Default.TrendingUp,
            modifier = Modifier.weight(1f)
        )
        KpiKachel(
            label = "Pollentage",
            wert  = "${kpi.pollenTage}",
            icon  = Icons.Default.FilterVintage,
            modifier = Modifier.weight(1f)
        )
        KpiKachel(
            label = "Allergene",
            wert  = "${kpi.anzahlAllergene}",
            icon  = Icons.Default.Warning,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun KpiKachel(
    label: String,
    wert: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier) {
        Column(
            Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary)
            Text(wert, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun SymptomVerlaufChart(verlauf: List<Pair<java.time.LocalDate, Double>>) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Schweregrad-Verlauf", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            // Einfacher Balken-Chart ohne externe Library
            val maxWert = verlauf.maxOfOrNull { it.second } ?: 5.0
            Row(
                Modifier.fillMaxWidth().height(80.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                verlauf.takeLast(30).forEach { (_, wert) ->
                    val hoehe = (wert / maxWert).toFloat()
                    val farbe = when {
                        wert <= 1.5 -> Color(0xFF4CAF50)
                        wert <= 3.0 -> Color(0xFFFF9800)
                        else        -> MaterialTheme.colorScheme.error
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(hoehe.coerceAtLeast(0.05f))
                            .background(farbe, shape = MaterialTheme.shapes.extraSmall)
                    )
                }
            }
            Text(
                "Letzte ${min(verlauf.size, 30)} Einträge · Max: ${
                    String.format("%.1f", maxWert)}/5",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun HeatmapCard(heatmap: List<HeatmapZelle>) {
    val wochentage = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Wochentag-Muster", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                wochentage.forEachIndexed { i, tag ->
                    val zelle = heatmap.filter { it.wochentag == i + 1 }
                    val avg = if (zelle.isEmpty()) 0.0
                              else zelle.map { it.durchschnittSchweregrad }.average()
                    val farbe = when {
                        avg == 0.0  -> MaterialTheme.colorScheme.surfaceVariant
                        avg <= 1.5  -> Color(0xFF81C784)
                        avg <= 3.0  -> Color(0xFFFFB74D)
                        else        -> Color(0xFFE57373)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .background(farbe, shape = MaterialTheme.shapes.small)
                        )
                        Text(tag, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Color(0xFF81C784) to "Niedrig",
                       Color(0xFFFFB74D) to "Mittel",
                       Color(0xFFE57373) to "Hoch").forEach { (farbe, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(10.dp).background(farbe,
                            shape = MaterialTheme.shapes.extraSmall))
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun KorrelationsCard(k: KorrelationsEintrag) {
    val farbe = if (k.istSignifikant) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceVariant
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (k.istSignifikant) Icons.Default.Warning else Icons.Default.FilterVintage,
                null,
                tint = if (k.istSignifikant) MaterialTheme.colorScheme.error
                       else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(k.gruppe, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${k.anzahlBeobachtungen} Beobachtungen · " +
                    "Ø ${String.format("%.1f", k.durchschnittSchweregrad)}/5",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            if (k.istSignifikant) {
                Badge(containerColor = MaterialTheme.colorScheme.error) {
                    Text("Auffällig")
                }
            }
        }
    }
}

@Composable
private fun PhasenTimelineCard(phase: com.allerpaw.app.data.local.entity.AusschlussPhasEntity) {
    val label = when (phase.phasentyp) {
        "elimination" -> "Elimination"
        "provokation" -> "Provokation"
        "ergebnis"    -> "Ergebnis"
        else          -> phase.phasentyp
    }
    val ergebnisIcon = when (phase.ergebnis) {
        "vertraeglich" -> Icons.Default.CheckCircle
        "reaktion"     -> Icons.Default.Cancel
        else           -> Icons.Default.HourglassEmpty
    }
    val ergebnisFarbe = when (phase.ergebnis) {
        "vertraeglich" -> Color(0xFF4CAF50)
        "reaktion"     -> MaterialTheme.colorScheme.error
        else           -> MaterialTheme.colorScheme.outline
    }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(ergebnisIcon, null, tint = ergebnisFarbe, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                if (phase.getesteZutatName.isNotBlank())
                    Text(phase.getesteZutatName, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                Text("${phase.startdatum} – ${phase.enddatum}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)
        }
    }
}
