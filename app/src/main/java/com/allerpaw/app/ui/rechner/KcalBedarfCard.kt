package com.allerpaw.app.ui.rechner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
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
import com.allerpaw.app.R
import com.allerpaw.app.data.local.entity.HundEntity
import com.allerpaw.app.util.FloatParser

/**
 * Energiebedarf-Karte mit:
 *   - RER / MER-Anzeige
 *   - Aktivitätsfaktor-Slider (1,0–3,0)
 *   - Manueller Kcal-Override mit Persistenz im Hund-Profil
 *
 * Wenn ein manueller Wert aktiv ist, wird MER durchgestrichen und
 * der manuelle Wert als primärer Wert angezeigt.
 */
@Composable
fun KcalBedarfCard(
    hund: HundEntity?,
    rerKcal: Double?,
    merKcal: Double?,
    effektiverKcal: Double?,
    aktivitaetsFaktor: Float,
    kcalManuellInput: String,
    kcalManuellAktiv: Boolean,
    onFaktorChange: (Float) -> Unit,
    onKcalManuellChange: (String) -> Unit,
    onAktivieren: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var manuellOffen by remember { mutableStateOf(false) }
    val manuellGueltig = FloatParser.parse(kcalManuellInput)?.let { it > 0 } == true

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🔥", fontSize = 20.sp)
                    Text(stringResource(R.string.label_energiebedarf),
                        style = MaterialTheme.typography.titleMedium)
                }
                if (kcalManuellAktiv) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            "Manuell aktiv",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            if (hund == null || rerKcal == null) {
                Text("Kein Hund ausgewählt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
                return@Column
            }

            // ── KPI-Zeile ─────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KcalKpi(
                    label   = "RER",
                    wert    = rerKcal,
                    hinweis = "Ruhebedarf",
                    farbe   = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f)
                )
                KcalKpi(
                    label        = "MER",
                    wert         = merKcal,
                    hinweis      = "Faktor ×%.1f".format(aktivitaetsFaktor),
                    farbe        = if (kcalManuellAktiv)
                        MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.primary,
                    durchgestrichen = kcalManuellAktiv,
                    modifier     = Modifier.weight(1f)
                )
                if (kcalManuellAktiv) {
                    KcalKpi(
                        label    = "Manuell",
                        wert     = effektiverKcal,
                        hinweis  = "Aktiver Wert",
                        farbe    = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Aktivitätsfaktor-Slider ───────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.label_aktivitaetsfaktor),
                        style = MaterialTheme.typography.labelMedium)
                    Text("×%.2f".format(aktivitaetsFaktor),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value         = aktivitaetsFaktor,
                    onValueChange = onFaktorChange,
                    valueRange    = 1.0f..3.0f,
                    steps         = 19,   // 0.1er-Schritte (1.0 bis 3.0 = 20 Stufen)
                    enabled       = !kcalManuellAktiv
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.label_inaktiv_faktor),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                    Text(stringResource(R.string.label_arbeitshund),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                }
                // Vorauswahl-Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        1.2f to "Inaktiv",
                        1.6f to "Normal",
                        2.0f to "Aktiv",
                        3.0f to "Arbeit"
                    ).forEach { (faktor, label) ->
                        FilterChip(
                            selected = aktivitaetsFaktor == faktor,
                            onClick  = { onFaktorChange(faktor) },
                            label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            enabled  = !kcalManuellAktiv
                        )
                    }
                }
            }

            HorizontalDivider()

            // ── Manueller Kcal-Override ───────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("Manueller Kcal-Bedarf",
                        style = MaterialTheme.typography.labelMedium)
                    Text(
                        if (kcalManuellAktiv) "Überschreibt MER-Berechnung"
                        else "Optional: z.B. vom Tierarzt empfohlen",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (kcalManuellAktiv) {
                        IconButton(onClick = onReset) {
                            Icon(Icons.Default.RestartAlt, "Zurücksetzen",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = { manuellOffen = !manuellOffen }) {
                        Icon(
                            if (manuellOffen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            if (manuellOffen) "Einklappen" else "Manuell bearbeiten"
                        )
                    }
                }
            }

            AnimatedVisibility(visible = manuellOffen) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value         = kcalManuellInput,
                        onValueChange = onKcalManuellChange,
                        label         = { Text("Kcal/Tag") },
                        placeholder   = { Text(merKcal?.let { "%.0f".format(it) } ?: "z.B. 1200") },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        isError       = kcalManuellInput.isNotBlank() && !manuellGueltig,
                        supportingText = when {
                            kcalManuellInput.isNotBlank() && !manuellGueltig ->
                                { { Text("Ungültige Zahl") } }
                            manuellGueltig -> { {
                                val kcal = FloatParser.parse(kcalManuellInput)!!
                                val diff = merKcal?.let { kcal - it }
                                Text(
                                    if (diff != null)
                                        "%+.0f kcal vs. MER (%.0f kcal)".format(diff, merKcal)
                                    else "%.0f kcal/Tag".format(kcal),
                                    color = when {
                                        diff == null         -> Color.Unspecified
                                        diff > merKcal!! * 0.2 -> MaterialTheme.colorScheme.error
                                        diff < -merKcal * 0.2  -> MaterialTheme.colorScheme.error
                                        else                    -> Color(0xFF4CAF50)
                                    }
                                )
                            } }
                            else -> null
                        },
                        trailingIcon = if (kcalManuellInput.isNotBlank()) {{
                            IconButton(onClick = { onKcalManuellChange("") }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }} else null
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick  = onReset,
                            enabled  = kcalManuellAktiv,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.RestartAlt, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_zuruecksetzen))
                        }
                        Button(
                            onClick  = {
                                onAktivieren()
                                manuellOffen = false
                            },
                            enabled  = manuellGueltig,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (kcalManuellAktiv) "Aktualisieren" else "Aktivieren")
                        }
                    }

                    OutlinedCard {
                        Row(
                            Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment     = Alignment.Top
                        ) {
                            Icon(Icons.Default.Info, null, Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.outline)
                            Text(
                                "Der manuelle Wert wird im Hund-Profil gespeichert und " +
                                "überschreibt die MER-Berechnung dauerhaft. " +
                                "Zurücksetzen reaktiviert die automatische Berechnung.",
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

@Composable
private fun KcalKpi(
    label: String,
    wert: Double?,
    hinweis: String,
    farbe: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    durchgestrichen: Boolean = false
) {
    OutlinedCard(modifier = modifier) {
        Column(
            Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline)
            Text(
                wert?.let { "%.0f".format(it) } ?: "–",
                style          = MaterialTheme.typography.titleMedium,
                color          = farbe,
                textDecoration = if (durchgestrichen)
                    androidx.compose.ui.text.style.TextDecoration.LineThrough
                else null
            )
            Text("kcal/Tag", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline)
            Text(hinweis, style = MaterialTheme.typography.labelSmall,
                color = farbe.copy(alpha = 0.7f),
                textAlign = TextAlign.Center)
        }
    }
}
