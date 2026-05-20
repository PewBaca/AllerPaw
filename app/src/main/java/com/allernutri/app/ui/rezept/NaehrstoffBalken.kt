package com.allernutri.app.ui.rezept

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.allernutri.app.R
import com.allernutri.app.data.local.entity.ToleranzEntity
import com.allernutri.app.domain.NaehrstoffErgebnis
import kotlin.math.min

/**
 * Horizontaler Nährstoff-Balken mit individuellen Toleranzmarkern.
 *
 * Skalierung: 0–200% des Bedarfswerts (statt hardcoded 0–150%).
 * Marker: ┆ Min (standard 80%) · ┆ Empfehlung (100%) · ┆ Max (150% oder UL-basiert)
 * Ampelfarbe: basiert auf Toleranz-Grenzen statt hardcoded.
 * Long-Press: öffnet Tooltip mit Ist/Bedarf/Max.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NaehrstoffBalken(
    ergebnis: NaehrstoffErgebnis,
    toleranz: ToleranzEntity?,
    vergleich: NaehrstoffErgebnis? = null,
    onEditToleranz: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val min      = toleranz?.minProzent         ?: 80.0
    val empf     = toleranz?.empfehlungProzent  ?: 100.0
    val max      = toleranz?.maxProzent         ?: 150.0
    val skalEnde = (max * 1.33).coerceAtLeast(200.0)   // Balken geht bis 133% des Max-Werts

    val hauptFarbe = ampelFarbe(ergebnis.prozent, min, max)
    val prozentClamped = (ergebnis.prozent / skalEnde).coerceIn(0.0, 1.0).toFloat()

    var zeigeTooltip by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Name — Long-Press für Tooltip
            Text(
                ergebnis.naehrstoff.label,
                style    = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick      = {},
                        onLongClick  = { zeigeTooltip = true }
                    )
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Prozentwert farbig
                Text(
                    "${ergebnis.prozent.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = hauptFarbe
                )
                // Delta zum Vergleichsrezept
                if (vergleich != null) {
                    val delta    = ergebnis.prozent - vergleich.prozent
                    val deltaStr = if (delta >= 0) "+${delta.toInt()}%" else "${delta.toInt()}%"
                    Text(
                        deltaStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (delta >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                }
                // Ist-Wert + Einheit
                Text(
                    "${String.format("%.2f", ergebnis.istWert)} ${ergebnis.naehrstoff.einheit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                // Bearbeiten-Button für Toleranz
                if (onEditToleranz != null) {
                    IconButton(
                        onClick  = onEditToleranz,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            stringResource(R.string.cd_bearbeiten),
                            modifier = Modifier.size(12.dp),
                            tint     = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(2.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(10.dp)) {
            val w = size.width
            val h = size.height
            val radius = CornerRadius(h / 2)

            // ── Hintergrund ────────────────────────────────────────────────
            drawRoundRect(
                color        = Color.LightGray.copy(alpha = 0.3f),
                size         = Size(w, h),
                cornerRadius = radius
            )

            // ── Vergleichsbalken (halbtransparent, hinter Hauptbalken) ────
            if (vergleich != null) {
                val vProzent = (vergleich.prozent / skalEnde).coerceIn(0.0, 1.0).toFloat()
                drawRoundRect(
                    color        = ampelFarbe(vergleich.prozent, min, max).copy(alpha = 0.3f),
                    size         = Size(w * vProzent, h),
                    cornerRadius = radius
                )
            }

            // ── Füllstand (Hauptbalken) ────────────────────────────────────
            drawRoundRect(
                color        = hauptFarbe,
                size         = Size(w * prozentClamped, h),
                cornerRadius = radius
            )

            // ── Min-Markierung ─────────────────────────────────────────────
            val markMin = (w * (min / skalEnde)).coerceIn(0f, w).toFloat()
            drawLine(
                color       = Color(0xFF1565C0).copy(alpha = 0.7f),
                start       = Offset(markMin, 0f),
                end         = Offset(markMin, h),
                strokeWidth = 1.5f
            )

            // ── Empfehlungs-Markierung ─────────────────────────────────────
            val markEmpf = (w * (empf / skalEnde)).coerceIn(0f, w).toFloat()
            drawLine(
                color       = Color(0xFF2E7D32).copy(alpha = 0.9f),
                start       = Offset(markEmpf, 0f),
                end         = Offset(markEmpf, h),
                strokeWidth = 2f
            )

            // ── Max-Markierung ─────────────────────────────────────────────
            val markMax = (w * (max / skalEnde)).coerceIn(0f, w).toFloat()
            drawLine(
                color       = Color(0xFFB71C1C).copy(alpha = 0.7f),
                start       = Offset(markMax, 0f),
                end         = Offset(markMax, h),
                strokeWidth = 1.5f
            )
        }

        // ── Marker-Legende (nur eingeblendet wenn Wert nah an Grenze) ─────
        val zeigeLegende = ergebnis.prozent < min * 1.1 ||
                           ergebnis.prozent > max * 0.9 ||
                           (ergebnis.prozent in (empf * 0.95)..(empf * 1.05))
        if (zeigeLegende) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(top = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Min ${min.toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f),
                    color = Color(0xFF1565C0).copy(alpha = 0.7f)
                )
                Text(
                    "Ziel ${empf.toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f),
                    color = Color(0xFF2E7D32).copy(alpha = 0.9f)
                )
                Text(
                    "Max ${max.toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f),
                    color = Color(0xFFB71C1C).copy(alpha = 0.7f)
                )
            }
        }

        Spacer(Modifier.height(4.dp))
    }

    // ── Tooltip bei Long-Press ─────────────────────────────────────────────
    if (zeigeTooltip) {
        AlertDialog(
            onDismissRequest = { zeigeTooltip = false },
            title = { Text(ergebnis.naehrstoff.label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TooltipZeile("Ist-Wert",    "${String.format("%.3f", ergebnis.istWert)} ${ergebnis.naehrstoff.einheit}")
                    TooltipZeile("Bedarf",      "${String.format("%.3f", ergebnis.bedarfswert)} ${ergebnis.naehrstoff.einheit}")
                    ergebnis.maxWert?.let {
                        TooltipZeile("UL (NRC)",    "${String.format("%.3f", it)} ${ergebnis.naehrstoff.einheit}")
                    }
                    HorizontalDivider()
                    TooltipZeile("Ist / Bedarf", "${ergebnis.prozent.toInt()}%", hauptFarbe)
                    TooltipZeile("Toleranz Min", "${min.toInt()}%",  Color(0xFF1565C0))
                    TooltipZeile("Toleranz Ziel","${empf.toInt()}%", Color(0xFF2E7D32))
                    TooltipZeile("Toleranz Max", "${max.toInt()}%",  Color(0xFFB71C1C))
                }
            },
            confirmButton = {
                TextButton(onClick = { zeigeTooltip = false }) {
                    Text(stringResource(R.string.btn_ok))
                }
            }
        )
    }
}

@Composable
private fun TooltipZeile(label: String, wert: String, farbe: Color = Color.Unspecified) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline)
        Text(wert, style = MaterialTheme.typography.bodySmall,
            color = if (farbe == Color.Unspecified) MaterialTheme.colorScheme.onSurface else farbe)
    }
}

/** Ampelfarbe basierend auf individuellen Toleranzgrenzen */
@Composable
fun ampelFarbe(prozent: Double, min: Double, max: Double): Color = when {
    prozent > max * 1.2  -> Color(0xFFD32F2F)               // Weit über Max → Rot dunkel
    prozent < min         -> MaterialTheme.colorScheme.error  // Unter Min → Rot
    prozent > max         -> Color(0xFFFF9800)                // Über Max → Orange
    else                  -> Color(0xFF4CAF50)                // Im Zielbereich → Grün
}

// Overload für Canvas (kein @Composable context)
fun ampelFarbeCanvas(prozent: Double, min: Double, max: Double): Color = when {
    prozent < min   -> Color(0xFFF44336)
    prozent > max   -> Color(0xFFFF9800)
    else            -> Color(0xFF4CAF50)
}
