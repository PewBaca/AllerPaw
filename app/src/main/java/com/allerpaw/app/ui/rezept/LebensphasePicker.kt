package com.allerpaw.app.ui.rezept

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.allerpaw.app.R
import com.allerpaw.app.domain.NrcLebensphasen

/**
 * Kompakter Picker für die NRC-Lebensphase.
 *
 * Eingeklappt: zeigt nur die gewählte Phase als Chip + Expand-Icon.
 * Ausgeklappt: zeigt alle Phasen als FilterChips + Info-Text zu Bedarfsänderungen.
 *
 * Wichtige Bedarfsänderungen je Phase sind inline dokumentiert,
 * damit der Nutzer die Auswirkung versteht.
 */
@Composable
fun LebensphasePicker(
    selected: NrcLebensphasen.Lebensphase,
    onSelect: (NrcLebensphasen.Lebensphase) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Lebensphase (NRC-Bedarf)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "Skaliert den Nährstoffbedarf nach NRC 2006",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Aktuelle Phase als Badge
                    Surface(
                        color  = MaterialTheme.colorScheme.primaryContainer,
                        shape  = MaterialTheme.shapes.small
                    ) {
                        Text(
                            lebensphasenEmoji(selected) + " " + selected.label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(
                        onClick  = { expanded = !expanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Einklappen" else "Lebensphase wählen"
                        )
                    }
                }
            }

            // ── Ausgeklappte Auswahl ───────────────────────────────────────
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                    HorizontalDivider()

                    // Chips für alle Phasen
                    NrcLebensphasen.Lebensphase.entries.forEach { phase ->
                        FilterChip(
                            selected    = phase == selected,
                            onClick     = {
                                onSelect(phase)
                                expanded = false
                            },
                            label = {
                                Column(Modifier.padding(vertical = 4.dp)) {
                                    Text(
                                        lebensphasenEmoji(phase) + "  " + phase.label,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        lebensphasenHinweis(phase),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (phase == selected)
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.outline
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Info-Box mit NRC-Quelle
                    OutlinedCard {
                        Row(
                            Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment     = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info, null,
                                modifier = Modifier.size(16.dp),
                                tint     = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                "Bedarfswerte nach NRC 2006 (National Research Council). " +
                                "Faktoren gelten für Haupt-Nährstoffe. " +
                                "Toleranzgrenzen bleiben unverändert.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun lebensphasenEmoji(phase: NrcLebensphasen.Lebensphase): String = when (phase) {
    NrcLebensphasen.Lebensphase.ADULT      -> "🐕"
    NrcLebensphasen.Lebensphase.WELPE      -> "🐶"
    NrcLebensphasen.Lebensphase.SENIOR     -> "🦮"
    NrcLebensphasen.Lebensphase.TRÄCHTIG   -> "🤰"
    NrcLebensphasen.Lebensphase.LAKTIEREND -> "🍼"
}

private fun lebensphasenHinweis(phase: NrcLebensphasen.Lebensphase): String = when (phase) {
    NrcLebensphasen.Lebensphase.ADULT ->
        "Standard-Bedarf (Faktor 1,0)"
    NrcLebensphasen.Lebensphase.WELPE ->
        "↑ Protein +56%, Calcium +200%, Phosphor +150%, Vitamin D +47%"
    NrcLebensphasen.Lebensphase.SENIOR ->
        "↑ Protein +11%, Vitamin E +33%  ↓ Phosphor −33%, Natrium −25%"
    NrcLebensphasen.Lebensphase.TRÄCHTIG ->
        "↑ Protein +44%, Calcium +160%, Phosphor +110%, Eisen +127%"
    NrcLebensphasen.Lebensphase.LAKTIEREND ->
        "↑ Protein +100%, Calcium +380%, Phosphor +290%, Eisen +167%"
}
