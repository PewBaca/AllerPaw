package com.allerpaw.app.ui.statistik

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allerpaw.app.data.local.entity.HundEntity

/**
 * Picker für den Vergleichshund + KPI-Delta-Darstellung.
 *
 * Eingeklappt: zeigt "kein Vergleich" oder den gewählten Vergleichshund.
 * Ausgeklappt: RadioButton-Liste aller anderen Hunde + "Kein Vergleich".
 *
 * KPI-Vergleich: Haupthund vs. Vergleichshund nebeneinander mit Δ-Badge.
 */
@Composable
fun HundVergleichCard(
    hunde: List<HundEntity>,
    selectedHundId: Long?,
    vergleichsHundId: Long?,
    hauptKpi: KpiState,
    vergleichsKpi: KpiState?,
    onVergleichsHundSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val hauptHund     = hunde.find { it.id == selectedHundId }
    val vergleichsHund = hunde.find { it.id == vergleichsHundId }
    var expanded by remember { mutableStateOf(false) }
    val verfuegbareHunde = hunde.filter { it.id != selectedHundId }

    if (verfuegbareHunde.isEmpty()) return

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // ── Header ────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("⚖️", fontSize = 18.sp)
                    Column {
                        Text("Hund-Vergleich",
                            style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (vergleichsHund != null) "vs. ${vergleichsHund.name}"
                            else "Kein Vergleich aktiv",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (vergleichsHund != null)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (vergleichsHundId != null) {
                        IconButton(
                            onClick  = { onVergleichsHundSelected(null) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, "Vergleich beenden",
                                modifier = Modifier.size(18.dp),
                                tint     = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(
                        onClick  = { expanded = !expanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null
                        )
                    }
                }
            }

            // ── Hund-Auswahl ──────────────────────────────────────────────
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HorizontalDivider()
                    Text("Vergleichshund wählen:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = vergleichsHundId == null,
                            onClick  = { onVergleichsHundSelected(null); expanded = false }
                        )
                        Text("Kein Vergleich",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline)
                    }

                    verfuegbareHunde.forEach { hund ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = hund.id == vergleichsHundId,
                                onClick  = { onVergleichsHundSelected(hund.id); expanded = false }
                            )
                            Column {
                                Text(hund.name, style = MaterialTheme.typography.bodyMedium)
                                if (hund.rasse.isNotBlank()) {
                                    Text(hund.rasse,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }

            // ── KPI-Vergleichs-Tabelle ────────────────────────────────────
            AnimatedVisibility(visible = vergleichsHund != null && vergleichsKpi != null) {
                if (vergleichsKpi == null) return@AnimatedVisibility
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider()

                    // Spalten-Header
                    Row(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.weight(1f))
                        Text(hauptHund?.name ?: "–",
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1.2f))
                        Text(vergleichsHund?.name ?: "–",
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1.2f))
                        Text("Δ",
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(0.6f))
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // KPI-Zeilen
                    KpiVergleichsZeile(
                        label       = "Symptomtage",
                        hauptWert   = hauptKpi.symptomTage.toDouble(),
                        verglWert   = vergleichsKpi.symptomTage.toDouble(),
                        einheit     = "T",
                        lowerIsBetter = true
                    )
                    KpiVergleichsZeile(
                        label       = "Ø Schweregrad",
                        hauptWert   = hauptKpi.durchschnittSchweregrad,
                        verglWert   = vergleichsKpi.durchschnittSchweregrad,
                        einheit     = "/5",
                        nachkomma   = 1,
                        lowerIsBetter = true
                    )
                    KpiVergleichsZeile(
                        label       = "Pollentage",
                        hauptWert   = hauptKpi.pollenTage.toDouble(),
                        verglWert   = vergleichsKpi.pollenTage.toDouble(),
                        einheit     = "T",
                        lowerIsBetter = false
                    )
                    KpiVergleichsZeile(
                        label       = "Allergene",
                        hauptWert   = hauptKpi.anzahlAllergene.toDouble(),
                        verglWert   = vergleichsKpi.anzahlAllergene.toDouble(),
                        einheit     = "",
                        lowerIsBetter = true
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiVergleichsZeile(
    label: String,
    hauptWert: Double,
    verglWert: Double,
    einheit: String,
    nachkomma: Int = 0,
    lowerIsBetter: Boolean
) {
    val fmt = if (nachkomma == 0) "%.0f" else "%.${nachkomma}f"
    val delta     = hauptWert - verglWert
    val hauptBesser = if (lowerIsBetter) delta < -0.05 else delta > 0.05
    val verglBesser = if (lowerIsBetter) delta > 0.05  else delta < -0.05
    val deltaStr  = if (delta >= 0) "+${fmt.format(delta)}" else fmt.format(delta)

    val deltaFarbe = when {
        lowerIsBetter && delta < -0.05 -> Color(0xFF4CAF50)   // Haupt besser
        lowerIsBetter && delta > 0.05  -> MaterialTheme.colorScheme.error
        !lowerIsBetter && delta > 0.05 -> Color(0xFF4CAF50)
        !lowerIsBetter && delta < -0.05 -> MaterialTheme.colorScheme.error
        else                            -> MaterialTheme.colorScheme.outline
    }

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label,
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.outline,
            modifier = Modifier.weight(1f))

        // Haupthund
        Row(Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${fmt.format(hauptWert)}$einheit",
                style = MaterialTheme.typography.bodyMedium,
                color = if (hauptBesser) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface)
            if (hauptBesser) Icon(Icons.Default.CheckCircle, null,
                Modifier.size(12.dp), tint = Color(0xFF4CAF50))
        }

        // Vergleichshund
        Row(Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${fmt.format(verglWert)}$einheit",
                style = MaterialTheme.typography.bodyMedium,
                color = if (verglBesser) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface)
            if (verglBesser) Icon(Icons.Default.CheckCircle, null,
                Modifier.size(12.dp), tint = Color(0xFF4CAF50))
        }

        // Delta
        Text(deltaStr,
            style    = MaterialTheme.typography.labelSmall,
            color    = deltaFarbe,
            modifier = Modifier.weight(0.6f))
    }
}
