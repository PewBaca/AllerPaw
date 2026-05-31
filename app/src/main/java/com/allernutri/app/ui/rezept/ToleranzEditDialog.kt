package com.allernutri.app.ui.rezept

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.allernutri.app.R
import com.allernutri.app.data.local.entity.ToleranzEntity
import com.allernutri.app.domain.NaehrstoffKatalog

/**
 * Dialog zum Bearbeiten der individuellen Nährstoff-Toleranzen je Hund.
 *
 * Zeigt drei Schieberegler:
 *   • Min (Mangel-Grenze, Standard 80%)
 *   • Ziel/Empfehlung (Standard 100%)
 *   • Max (Überschuss-Grenze, Standard 150%)
 *
 * Validierungsregel: Min ≤ Ziel ≤ Max
 */
@Composable
fun ToleranzEditDialog(
    toleranz: ToleranzEntity,
    onDismiss: () -> Unit,
    onSave: (ToleranzEntity) -> Unit,
    onZuruecksetzen: () -> Unit
) {
    val naehrstoff = NaehrstoffKatalog.byKey[toleranz.naehrstoffKey]

    var min  by remember { mutableStateOf(toleranz.minProzent.toFloat()) }
    var ziel by remember { mutableStateOf(toleranz.empfehlungProzent.toFloat()) }
    var max  by remember { mutableStateOf(toleranz.maxProzent.toFloat()) }

    // Invariante: min ≤ ziel ≤ max
    val isValid = min <= ziel && ziel <= max

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(stringResource(R.string.toleranz_title))
                if (naehrstoff != null) {
                    Text(
                        naehrstoff.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // ── Vorschau-Balken ────────────────────────────────────────
                PreviewBalken(
                    istProzent  = ziel,   // Zeigt Ziel als Beispiel-Position
                    minProzent  = min,
                    zielProzent = ziel,
                    maxProzent  = max
                )

                // ── Min-Slider ─────────────────────────────────────────────
                ToleranzSlider(
                    label    = stringResource(R.string.toleranz_min),
                    wert     = min,
                    farbe    = Color(0xFF1565C0),
                    onChange = { neu -> min = neu.coerceAtMost(ziel) },
                    min      = 0f,
                    max      = 100f
                )

                // ── Ziel-Slider ────────────────────────────────────────────
                ToleranzSlider(
                    label    = "Ziel/Empfehlung",
                    wert     = ziel,
                    farbe    = Color(0xFF2E7D32),
                    onChange = { neu -> ziel = neu.coerceIn(min, max) },
                    min      = 0f,
                    max      = 200f
                )

                // ── Max-Slider ─────────────────────────────────────────────
                ToleranzSlider(
                    label    = "Maximum (Überschuss-Grenze)",
                    wert     = max,
                    farbe    = Color(0xFFB71C1C),
                    onChange = { neu -> max = neu.coerceAtLeast(ziel) },
                    min      = 100f,
                    max      = 300f
                )

                // ── Fehlermeldung ──────────────────────────────────────────
                if (!isValid) {
                    Text(
                        "Min ≤ Ziel ≤ Max muss gelten",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // ── NRC-Hinweis ────────────────────────────────────────────
                naehrstoff?.maxPro1000kcal?.let {
                    OutlinedCard {
                        Text(
                            "NRC Upper Limit (UL) vorhanden. Max-Slider bezieht sich auf den Prozentwert relativ zum Bedarf.",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    onSave(toleranz.copy(
                        minProzent        = min.toDouble(),
                        empfehlungProzent = ziel.toDouble(),
                        maxProzent        = max.toDouble()
                    ))
                }
            ) { Text(stringResource(R.string.btn_speichern)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onZuruecksetzen(); onDismiss() }) {
                    Icon(Icons.Default.RestartAlt, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_zuruecksetzen))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.btn_abbrechen))
                }
            }
        }
    )
}

@Composable
private fun ToleranzSlider(
    label: String,
    wert: Float,
    farbe: Color,
    onChange: (Float) -> Unit,
    min: Float,
    max: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(label,
                style = MaterialTheme.typography.labelSmall,
                color = farbe)
            Text("${wert.toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = farbe)
        }
        Slider(
            value         = wert,
            onValueChange = onChange,
            valueRange    = min..max,
            colors        = SliderDefaults.colors(
                thumbColor       = farbe,
                activeTrackColor = farbe.copy(alpha = 0.7f)
            )
        )
    }
}

@Composable
private fun PreviewBalken(
    istProzent: Float,
    minProzent: Float,
    zielProzent: Float,
    maxProzent: Float
) {
    val skalEnde = (maxProzent * 1.33f).coerceAtLeast(200f)
    val istClamped = (istProzent / skalEnde).coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.toleranz_vorschau),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline)

        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxWidth().height(14.dp)
        ) {
            val w = size.width
            val h = size.height
            val radius = androidx.compose.ui.geometry.CornerRadius(h / 2)

            // Hintergrund
            drawRoundRect(
                color        = Color.LightGray.copy(alpha = 0.3f),
                size         = androidx.compose.ui.geometry.Size(w, h),
                cornerRadius = radius
            )

            // Füllstand (Ziel als Beispiel)
            val farbe = ampelFarbeCanvas(
                istProzent.toDouble(),
                minProzent.toDouble(),
                maxProzent.toDouble()
            )
            drawRoundRect(
                color        = farbe,
                size         = androidx.compose.ui.geometry.Size(w * istClamped, h),
                cornerRadius = radius
            )

            // Min-Marker
            val xMin = (w * (minProzent / skalEnde)).coerceIn(0f, w)
            drawLine(Color(0xFF1565C0), Offset(xMin, 0f), Offset(xMin, h), 2f)

            // Ziel-Marker
            val xZiel = (w * (zielProzent / skalEnde)).coerceIn(0f, w)
            drawLine(Color(0xFF2E7D32), Offset(xZiel, 0f), Offset(xZiel, h), 2.5f)

            // Max-Marker
            val xMax = (w * (maxProzent / skalEnde)).coerceIn(0f, w)
            drawLine(Color(0xFFB71C1C), Offset(xMax, 0f), Offset(xMax, h), 2f)
        }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Min ${minProzent.toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF1565C0).copy(alpha = 0.7f))
            Text("Ziel ${zielProzent.toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF2E7D32))
            Text("Max ${maxProzent.toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB71C1C).copy(alpha = 0.7f))
        }
    }
}
