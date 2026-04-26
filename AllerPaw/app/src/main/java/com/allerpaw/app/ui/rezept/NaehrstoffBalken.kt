package com.allerpaw.app.ui.rezept

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.allerpaw.app.domain.NaehrstoffErgebnis
import kotlin.math.min

/**
 * Horizontaler Nährstoff-Balken mit Ampelfarbe.
 * Zeigt Ist-Wert / Bedarfswert als Prozent, optional mit Vergleichswert (Delta).
 */
@Composable
fun NaehrstoffBalken(
    ergebnis: NaehrstoffErgebnis,
    vergleich: NaehrstoffErgebnis? = null,
    modifier: Modifier = Modifier
) {
    val hauptFarbe = ampelFarbe(ergebnis.status)
    val prozentClamped = min(ergebnis.prozent / 150.0, 1.0).toFloat()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                ergebnis.naehrstoff.label,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${ergebnis.prozent.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = hauptFarbe
                )
                // Delta zum Vergleichsrezept
                if (vergleich != null) {
                    val delta = ergebnis.prozent - vergleich.prozent
                    val deltaStr = if (delta >= 0) "+${delta.toInt()}%" else "${delta.toInt()}%"
                    Text(
                        deltaStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (delta >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    "${String.format("%.2f", ergebnis.istWert)} ${ergebnis.naehrstoff.einheit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            val w = size.width
            val h = size.height
            val radius = CornerRadius(h / 2)

            // Hintergrund
            drawRoundRect(
                color        = Color.LightGray.copy(alpha = 0.3f),
                size         = Size(w, h),
                cornerRadius = radius
            )

            // Füllstand
            drawRoundRect(
                color        = hauptFarbe,
                size         = Size(w * prozentClamped, h),
                cornerRadius = radius
            )

            // 80%-Markierung (Mindestbedarf)
            val mark80 = w * (80f / 150f)
            drawLine(
                color       = Color.DarkGray.copy(alpha = 0.5f),
                start       = Offset(mark80, 0f),
                end         = Offset(mark80, h),
                strokeWidth = 1.5f
            )

            // 100%-Markierung (Empfehlung)
            val mark100 = w * (100f / 150f)
            drawLine(
                color       = Color.DarkGray.copy(alpha = 0.8f),
                start       = Offset(mark100, 0f),
                end         = Offset(mark100, h),
                strokeWidth = 2f
            )

            // Vergleichsbalken (halbtransparent)
            if (vergleich != null) {
                val vProzent = min(vergleich.prozent / 150.0, 1.0).toFloat()
                drawRoundRect(
                    color        = ampelFarbe(vergleich.status).copy(alpha = 0.35f),
                    size         = Size(w * vProzent, h),
                    cornerRadius = radius
                )
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
fun ampelFarbe(status: NaehrstoffErgebnis.Status): Color = when (status) {
    NaehrstoffErgebnis.Status.OK             -> Color(0xFF4CAF50)
    NaehrstoffErgebnis.Status.MANGEL         -> MaterialTheme.colorScheme.error
    NaehrstoffErgebnis.Status.UEBERSCHUSS    -> Color(0xFFFF9800)
    NaehrstoffErgebnis.Status.UEBERSCHRITTEN -> Color(0xFFD32F2F)
}
