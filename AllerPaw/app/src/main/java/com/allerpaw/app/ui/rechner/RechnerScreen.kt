package com.allerpaw.app.ui.rechner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allerpaw.app.domain.NaehrstoffErgebnis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RechnerScreen(vm: RechnerViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Futterrechner") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Hund-Auswahl ─────────────────────────────────────────────
            item {
                HundSelectorCard(
                    hunde         = state.hunde,
                    selectedHund  = state.selectedHundId,
                    onSelect      = vm::selectHund
                )
            }

            // ── Energie ───────────────────────────────────────────────────
            item {
                EnergieBannerCard(
                    rerKcal = state.rerKcal,
                    merKcal = state.merKcal,
                    faktor  = state.aktivitaetsFaktor,
                    onFaktorChange = vm::setAktivitaetsFaktor
                )
            }

            // ── Analyse-Ergebnisse ────────────────────────────────────────
            if (state.ergebnisse.isNotEmpty()) {
                item { Text("NRC-Analyse", style = MaterialTheme.typography.titleMedium) }
                items(state.ergebnisse, key = { it.naehrstoff.key }) { ergebnis ->
                    NaehrstoffRow(ergebnis)
                }
            } else {
                item {
                    Text(
                        "Wähle einen Hund und ordne Rezepte zu, um die NRC-Analyse zu sehen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun HundSelectorCard(
    hunde: List<com.allerpaw.app.data.local.entity.HundEntity>,
    selectedHund: Long?,
    onSelect: (Long) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Hund auswählen", style = MaterialTheme.typography.titleSmall)
            if (hunde.isEmpty()) {
                Text("Noch kein Hund angelegt.", color = MaterialTheme.colorScheme.outline)
            } else {
                hunde.forEach { hund ->
                    FilterChip(
                        selected = hund.id == selectedHund,
                        onClick  = { onSelect(hund.id) },
                        label    = { Text(hund.name) },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EnergieBannerCard(
    rerKcal: Double?,
    merKcal: Double?,
    faktor: Float,
    onFaktorChange: (Float) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Energiebedarf", style = MaterialTheme.typography.titleSmall)
            if (rerKcal != null && merKcal != null) {
                Text("RER: ${rerKcal.toInt()} kcal/Tag")
                Text("MER (Faktor ${String.format("%.1f", faktor)}): ${merKcal.toInt()} kcal/Tag")
            } else {
                Text("Kein Hund ausgewählt.", color = MaterialTheme.colorScheme.outline)
            }
            Text("Aktivitätsfaktor", style = MaterialTheme.typography.labelSmall)
            Slider(
                value = faktor,
                onValueChange = onFaktorChange,
                valueRange = 1.0f..3.0f,
                steps = 19
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Inaktiv (1.0)", style = MaterialTheme.typography.labelSmall)
                Text("Arbeitshund (3.0)", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun NaehrstoffRow(e: NaehrstoffErgebnis) {
    val color = when (e.status) {
        NaehrstoffErgebnis.Status.OK              -> MaterialTheme.colorScheme.primary
        NaehrstoffErgebnis.Status.MANGEL          -> MaterialTheme.colorScheme.error
        NaehrstoffErgebnis.Status.UEBERSCHUSS     -> Color(0xFFFF8C00)
        NaehrstoffErgebnis.Status.UEBERSCHRITTEN  -> MaterialTheme.colorScheme.error
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(e.naehrstoff.label, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${String.format("%.2f", e.istWert)} ${e.naehrstoff.einheit} / " +
                "Bedarf ${String.format("%.2f", e.bedarfswert)} ${e.naehrstoff.einheit}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
            Text(
                "${e.prozent.toInt()} %",
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
            Text(
                e.status.name,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}
