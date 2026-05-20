package com.allernutri.app.ui.tagebuch

import androidx.compose.animation.AnimatedVisibility
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
import com.allernutri.app.R
import com.allernutri.app.data.local.entity.TagebuchAllergenEntity
import com.allernutri.app.domain.KreuzallergenAnalyse
import com.allernutri.app.domain.KreuzallergenMatrix

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KreuzallergenScreen(
    onNavigateUp: () -> Unit,
    vm: KreuzallergenViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kreuzallergie-Analyse") },
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

            // ── Hund-Auswahl ──────────────────────────────────────────────
            if (state.hunde.size > 1) {
                item {
                    ScrollableTabRow(
                        selectedTabIndex = state.hunde
                            .indexOfFirst { it.id == state.selectedHundId }.coerceAtLeast(0),
                        edgePadding = 0.dp
                    ) {
                        state.hunde.forEach { hund ->
                            Tab(
                                selected = hund.id == state.selectedHundId,
                                onClick  = { vm.selectHund(hund.id) },
                                text     = { Text(hund.name) }
                            )
                        }
                    }
                }
            }

            // ── Leer-Zustand ──────────────────────────────────────────────
            if (state.allergene.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🔍", fontSize = 48.sp)
                            Text("Noch keine Allergene erfasst.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline)
                            Text("Erfasse zuerst bestätigte Allergene im Tagebuch.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
                return@LazyColumn
            }

            // ── Ergebnis-Header ───────────────────────────────────────────
            state.ergebnis?.let { ergebnis ->
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("⚠️", fontSize = 22.sp)
                                Column {
                                    Text("${ergebnis.risikogruppen.size} Protein-Gruppen mit Kreuzreaktions-Risiko",
                                        style = MaterialTheme.typography.titleSmall)
                                    Text("Basierend auf ${state.allergene.size} bestätigten Allergenen",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            if (ergebnis.ohneGruppe.isNotEmpty()) {
                                Text(
                                    "${ergebnis.ohneGruppe.size} Allergen(e) keiner Protein-Gruppe zugeordnet: " +
                                    ergebnis.ohneGruppe.joinToString(", ") { it.allergen },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                // ── Risikogruppen ─────────────────────────────────────────
                items(ergebnis.risikogruppen, key = { it.gruppe.name }) { rg ->
                    RisikoGruppeCard(risikogruppe = rg)
                }

                // ── Ohne Gruppe ───────────────────────────────────────────
                if (ergebnis.ohneGruppe.isNotEmpty()) {
                    item {
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Keine Protein-Gruppe bekannt",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.outline)
                                Text("Für diese Allergene ist keine Kreuzreaktions-Gruppe in der Matrix hinterlegt.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline)
                                ergebnis.ohneGruppe.forEach { allergen ->
                                    AllergenChip(allergen)
                                }
                            }
                        }
                    }
                }

                // ── Hinweis ───────────────────────────────────────────────
                item {
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment     = Alignment.Top) {
                            Icon(Icons.Default.Info, null, Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.outline)
                            Text(
                                "Kreuzreaktionen basieren auf ähnlichen Proteinstrukturen nach " +
                                "NRC 2006 und aktueller veterinärallergiologischer Literatur. " +
                                "Kandidaten sind Verdachts-Allergene — keine gesicherte Diagnose. " +
                                "Rücksprache mit dem Tierarzt empfohlen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RisikoGruppeCard(risikogruppe: KreuzallergenAnalyse.Risikogruppe) {
    var expanded by remember { mutableStateOf(true) }
    val maxStaerke = risikogruppe.bestaetigteAllergene.maxOfOrNull { it.reaktionsstaerke } ?: 0

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.elevatedCardColors(
            containerColor = when (maxStaerke) {
                5    -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                4    -> Color(0xFFFF9800).copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // ── Gruppen-Header ────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            risikogruppe.gruppe.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        RisikoLevel(maxStaerke)
                    }
                    Text(
                        risikogruppe.gruppe.protein,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null
                    )
                }
            }

            // ── Bestätigte Allergene ──────────────────────────────────────
            Text("✅ Bestätigt:", style = MaterialTheme.typography.labelMedium)
            risikogruppe.bestaetigteAllergene.forEach { allergen ->
                AllergenChip(allergen)
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    // ── Kandidaten ────────────────────────────────────────
                    if (risikogruppe.weitereKandidaten.isNotEmpty()) {
                        Text("⚠️ Kreuzreaktions-Kandidaten:",
                            style = MaterialTheme.typography.labelMedium)
                        Text(
                            "Diese Zutaten enthalten verwandte Proteine — mit Vorsicht einführen:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        com.allernutri.app.ui.common.FlowRow(
                            horizontalGap = 6.dp,
                            verticalGap   = 4.dp
                        ) {
                            risikogruppe.weitereKandidaten.take(12).forEach { kandidat ->
                                SuggestionChip(
                                    onClick = {},
                                    label   = { Text(kandidat,
                                        style = MaterialTheme.typography.labelMedium) },
                                    icon    = { Icon(Icons.Default.Warning, null,
                                        Modifier.size(14.dp),
                                        tint = Color(0xFFFF9800)) }
                                )
                            }
                        }
                    }

                    // ── Wissenschaftliche Grundlage ───────────────────────
                    Text(
                        "Protein: ${risikogruppe.gruppe.protein}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        risikogruppe.gruppe.beschreibung,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "Quelle: ${risikogruppe.gruppe.quellen}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun AllergenChip(allergen: TagebuchAllergenEntity) {
    val farbe = when (allergen.reaktionsstaerke) {
        5    -> MaterialTheme.colorScheme.errorContainer
        4    -> Color(0xFFFF9800).copy(alpha = 0.2f)
        3    -> Color(0xFFFFEB3B).copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(color = farbe, shape = MaterialTheme.shapes.small) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(allergen.allergen, style = MaterialTheme.typography.bodySmall)
            Text(
                "${allergen.reaktionsstaerke}/5",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun RisikoLevel(maxStaerke: Int) {
    val (label, farbe) = when {
        maxStaerke >= 4 -> "Hoch" to MaterialTheme.colorScheme.error
        maxStaerke >= 3 -> "Mittel" to Color(0xFFFF9800)
        else            -> "Niedrig" to Color(0xFF4CAF50)
    }
    Surface(
        color = farbe.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = farbe
        )
    }
}
