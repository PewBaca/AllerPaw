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
import com.allerpaw.app.domain.ReaktionsScoreAnalyse
import java.time.format.DateTimeFormatter

/**
 * Reaktionsscore-Karte für die Statistik.
 *
 * Zeigt für jede Futter-Erstgabe den berechneten 48h-Reaktionsscore:
 *   - Farbiger Score-Balken (0–5)
 *   - Durchschnittsschweregrad
 *   - Häufigkeit (Anteil der Erstgaben mit Symptom-Reaktion)
 *   - Beispiel-Daten der Einführung
 *   - Signifikanz-Badge
 */
@Composable
fun ReaktionsScoreCard(
    scores: List<ReaktionsScore>,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

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
                    Text("⏱", fontSize = 20.sp)
                    Column {
                        Text("Reaktionsscore (48h-Fenster)",
                            style = MaterialTheme.typography.titleMedium)
                        Text("Symptome nach Erstgabe / Provokation",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text("${scores.size}",
                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            HorizontalDivider()

            // ── Score-Einträge ────────────────────────────────────────────
            scores.forEach { score ->
                ScoreZeile(score)
            }

            // ── Methodik-Hinweis ──────────────────────────────────────────
            OutlinedCard {
                Row(
                    Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.Top
                ) {
                    Icon(Icons.Default.Info, null,
                        Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline)
                    Text(
                        "Score = Schweregrad × Häufigkeit. " +
                        "Gewertet werden Symptome in 48h nach Erstgabe/Provokation. " +
                        "Mind. 2 Einträge für valides Signal.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreZeile(score: ReaktionsScore) {
    var expanded by remember { mutableStateOf(false) }
    val datumFmt = DateTimeFormatter.ofPattern("dd.MM")

    val scoreFarbe = when {
        score.score >= 3.5 -> MaterialTheme.colorScheme.error
        score.score >= 2.5 -> Color(0xFFFF9800)
        score.score >= 1.5 -> Color(0xFFFFEB3B)
        else               -> Color(0xFF4CAF50)
    }
    val risikoLabel = ReaktionsScoreAnalyse.risikoLabel(score.score)

    ElevatedCard(
        onClick   = { expanded = !expanded },
        modifier  = Modifier.fillMaxWidth(),
        colors    = if (score.istSignifikant)
            CardDefaults.elevatedCardColors(containerColor = scoreFarbe.copy(alpha = 0.08f))
        else CardDefaults.elevatedCardColors()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {

            // ── Kopfzeile ─────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier              = Modifier.weight(1f)
                ) {
                    Text("🍖", style = MaterialTheme.typography.bodyMedium)
                    Text(score.zutatName,
                        style    = MaterialTheme.typography.bodyMedium,
                        maxLines = 1)
                    if (score.istSignifikant) {
                        Icon(Icons.Default.Warning, null,
                            Modifier.size(14.dp), tint = scoreFarbe)
                    }
                }

                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = scoreFarbe.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(risikoLabel,
                            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = scoreFarbe)
                    }
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null, Modifier.size(18.dp))
                }
            }

            // ── Score-Balken ──────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress  = { (score.score / 5.0).toFloat().coerceIn(0f, 1f) },
                    modifier  = Modifier.weight(1f),
                    color     = scoreFarbe,
                    trackColor = scoreFarbe.copy(alpha = 0.2f)
                )
                Text(
                    "%.1f/5".format(score.score),
                    style = MaterialTheme.typography.labelMedium,
                    color = scoreFarbe
                )
            }

            // ── Meta-Zeile ────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetaChip("${score.anzahlBeobachtungen}× Erstgabe")
                MetaChip("Ø ${String.format("%.1f", score.durchschnittSchweregrad)}/5 Schweregrad")
                MetaChip("${(score.haeufigkeit * 100).roundToInt()}% Häufigkeit")
            }

            // ── Ausgeklappt: Details ──────────────────────────────────────
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HorizontalDivider()

                    Text("Einführungsdaten:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        score.beispielDaten.forEach { datum ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    datum.format(datumFmt),
                                    Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    Text(
                        "Symptome im 48h-Fenster: ${score.anzahlBeobachtungen} Ereignisse " +
                        "→ Score ${String.format("%.2f", score.score)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    if (score.istSignifikant) {
                        Text(
                            "⚠️ Signifikantes Signal — Rücksprache mit Tierarzt empfohlen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scoreFarbe
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(label,
            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun Int.roundToInt(): Int = this
