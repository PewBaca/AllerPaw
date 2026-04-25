package com.allerpaw.app.ui.zutaten

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.allerpaw.app.data.local.entity.ZutatNaehrstoffEntity
import com.allerpaw.app.domain.NaehrstoffKatalog
import com.allerpaw.app.util.FloatParser

/**
 * Dialog zum Bearbeiten aller 29 NRC-Nährstoffe einer Zutat.
 * Gruppiert nach: Makro · Fettsäure · Mineral · Vitamin
 * IE-Eingabe für Vitamin A, D, E mit Formauswahl.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NaehrstoffDialog(
    zutatId: Long,
    vitaminEForm: String,
    bestehend: List<ZutatNaehrstoffEntity>,
    onDismiss: () -> Unit,
    onSave: (List<ZutatNaehrstoffEntity>) -> Unit
) {
    // State: key → Eingabestring
    val werte = remember {
        mutableStateMapOf<String, String>().apply {
            bestehend.forEach { put(it.naehrstoffKey, FloatParser.formatOrEmpty(it.wertPer100g)) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nährstoffe (pro 100 g)") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val gruppen = NaehrstoffKatalog.alle.groupBy { it.gruppe }
                gruppen.forEach { (gruppe, naehrstoffe) ->
                    item {
                        NaehrstoffGruppeHeader(gruppe)
                    }
                    items(naehrstoffe, key = { it.key }) { n ->
                        val isIE = n.key in listOf("vitamin_a", "vitamin_d", "vitamin_e")
                        NaehrstoffRow(
                            label   = n.label,
                            einheit = n.einheit,
                            wert    = werte[n.key] ?: "",
                            isIE    = isIE,
                            vitaminEForm = if (n.key == "vitamin_e") vitaminEForm else null,
                            onWertChange = { werte[n.key] = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val result = werte.mapNotNull { (key, rawWert) ->
                    val n = NaehrstoffKatalog.byKey[key] ?: return@mapNotNull null
                    val parsed = FloatParser.parse(rawWert) ?: return@mapNotNull null
                    // IE → µg/mg Konvertierung
                    val converted = convertIE(key, parsed, vitaminEForm)
                    ZutatNaehrstoffEntity(
                        zutatId       = zutatId,
                        naehrstoffKey = key,
                        wertPer100g   = converted,
                        einheit       = n.einheit
                    )
                }
                onSave(result)
            }) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

@Composable
private fun NaehrstoffGruppeHeader(gruppe: String) {
    var expanded by remember { mutableStateOf(true) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            gruppe,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null
        )
    }
    HorizontalDivider()
}

@Composable
private fun NaehrstoffRow(
    label: String,
    einheit: String,
    wert: String,
    isIE: Boolean,
    vitaminEForm: String?,
    onWertChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            if (isIE) {
                Text("Eingabe in IE möglich", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline)
            }
        }
        OutlinedTextField(
            value         = wert,
            onValueChange = onWertChange,
            modifier      = Modifier.width(100.dp),
            singleLine    = true,
            suffix        = { Text(einheit, style = MaterialTheme.typography.labelSmall) },
            textStyle     = MaterialTheme.typography.bodySmall
        )
    }
}

// ── IE-Konvertierung ──────────────────────────────────────────────────────────

private fun convertIE(key: String, ieWert: Double, vitaminEForm: String): Double = when (key) {
    "vitamin_a" -> ieWert * 0.3          // 1 IE = 0,3 µg Retinol
    "vitamin_d" -> ieWert * 0.025        // 1 IE = 0,025 µg Cholecalciferol
    "vitamin_e" -> when (vitaminEForm) {
        "synthetisch"       -> ieWert * 0.45
        "acetat_natuerlich" -> ieWert * 0.74
        "acetat_synthetisch"-> ieWert * 0.67
        else                -> ieWert * 0.67  // natuerlich
    }
    else -> ieWert
}
