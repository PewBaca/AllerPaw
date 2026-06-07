package com.allernutri.app.ui.tagebuch.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.allernutri.app.R
import com.allernutri.app.data.local.entity.TagebuchUmweltEntity
import com.allernutri.app.ui.tagebuch.TagebuchUiState
import com.allernutri.app.ui.tagebuch.TagebuchViewModel

@Composable
fun UmweltTab(state: TagebuchUiState, vm: TagebuchViewModel) {
    // Automatisch Wetter laden wenn Tab gewechselt wird oder Hund gewechselt
    LaunchedEffect(state.selectedHundId) { vm.ladeWetter() }

    TabListe(
        eintraege   = state.umweltEintraege,
        leerText    = "Noch kein Umwelt-Eintrag",
        headerContent = {
            // Lade-Indikator
            if (state.wetterLaedt) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp))
            }

            state.wetterFehler?.let { fehler ->
                Card(
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(fehler, Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            state.wetterBanner?.let { banner ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.WbSunny, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(banner.stadtName, style = MaterialTheme.typography.titleSmall)
                        }
                        Text(
                            "🌡 ${banner.tempMin}–${banner.tempMax}°C  " +
                            "💧 ${banner.feuchte}%  " +
                            "🌧 ${banner.regenMm} mm",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (banner.pollenMap.isNotEmpty()) {
                            Text(
                                banner.pollenMap.entries.joinToString("  ") { (art, wert) -> "🌿 $art: $wert/5" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        if (!banner.autoBefuellt) {
                            OutlinedButton(
                                onClick  = vm::befuelleAktuelleUmweltAusWetter,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Download, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_wetter_laden))
                            }
                        } else {
                            Text(
                                "✅ " + stringResource(R.string.wetter_geladen, banner.stadtName),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            if (state.wetterBanner == null && state.wetterFehler == null) {
                TextButton(
                    onClick  = vm::ladeWetter,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Cloud, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.wetter_klicke_laden))
                }
            }
        },
        itemContent = { e ->
            UmweltCard(e, onEdit = { vm.editUmwelt(e) }, onDelete = { vm.deleteUmwelt(e.id) })
        }
    )
    state.editUmwelt?.let { e ->
        UmweltEditDialog(eintrag = e, onDismiss = vm::dismissUmwelt, onSave = vm::saveUmwelt)
    }
}

@Composable
private fun UmweltCard(e: TagebuchUmweltEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.datum.toString(), style = MaterialTheme.typography.titleSmall)
                Text(
                    "🌡 ${e.tempMinC}–${e.tempMaxC}°C  💧 ${e.luftfeuchte}%  🌧 ${e.niederschlagMm} mm",
                    style = MaterialTheme.typography.bodySmall
                )
                if (false) { // pollenMap entfernt
                    Text(
                        // e.pollenMap removed //("  ") { (art, wert) -> "🌿 $art: $wert/5" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            IconButton(onClick = onEdit)   { Icon(Icons.Default.Edit, stringResource(R.string.cd_bearbeiten)) }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, stringResource(R.string.cd_loeschen),
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun UmweltEditDialog(
    eintrag: TagebuchUmweltEntity,
    onDismiss: () -> Unit,
    onSave: (TagebuchUmweltEntity) -> Unit
) {
    var tempMin    by remember { mutableStateOf(eintrag.tempMinC?.toString() ?: "") }
    var tempMax    by remember { mutableStateOf(eintrag.tempMaxC?.toString() ?: "") }
    var feuchte    by remember { mutableStateOf(eintrag.luftfeuchte?.toString() ?: "") }
    var regenMm    by remember { mutableStateOf(eintrag.niederschlagMm?.toString() ?: "") }
    var raumTemp   by remember { mutableStateOf(eintrag.raumtempC?.toString() ?: "") }
    var raumFeuchte by remember { mutableStateOf(eintrag.raumfeuchte?.toString() ?: "") }
    var pollenMap  by remember { mutableStateOf(mutableMapOf<String, Int>()) }
    var neueArt    by remember { mutableStateOf("") }
    var neueStaerke by remember { mutableStateOf(3f) }
    var standortOffen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Umwelt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(tempMin, { tempMin = it },
                        label = { Text(stringResource(R.string.label_temp_min)) },
                        modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(tempMax, { tempMax = it },
                        label = { Text(stringResource(R.string.label_temp_max)) },
                        modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(feuchte, { feuchte = it },
                        label = { Text(stringResource(R.string.label_feuchte_pct)) },
                        modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(regenMm, { regenMm = it },
                        label = { Text(stringResource(R.string.label_regen_mm)) },
                        modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(raumTemp, { raumTemp = it },
                        label = { Text(stringResource(R.string.label_raumtemp)) },
                        modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(raumFeuchte, { raumFeuchte = it },
                        label = { Text(stringResource(R.string.label_raumfeuchte)) },
                        modifier = Modifier.weight(1f), singleLine = true)
                }

                HorizontalDivider()
                Text(stringResource(R.string.label_pollen_staerke),
                    style = MaterialTheme.typography.labelMedium)
                pollenMap.entries.toList().forEach { (art, wert) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(art, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text("$wert/5", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                        IconButton(onClick = { pollenMap = pollenMap.toMutableMap().also { it.remove(art) } },
                            modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Remove, stringResource(R.string.cd_entfernen),
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically) {
                    OutlinedTextField(neueArt, { neueArt = it },
                        label       = { Text(stringResource(R.string.label_neue_pollenart)) },
                        modifier    = Modifier.weight(1f),
                        singleLine  = true,
                        placeholder = { Text(stringResource(R.string.label_stark)) })
                    Text("${neueStaerke.toInt()}/5", style = MaterialTheme.typography.labelSmall)
                    IconButton(
                        onClick  = {
                            if (neueArt.isNotBlank()) {
                                pollenMap = pollenMap.toMutableMap().also { it[neueArt.trim()] = neueStaerke.toInt() }
                                neueArt = ""
                            }
                        },
                        enabled  = neueArt.isNotBlank()
                    ) { Icon(Icons.Default.Add, stringResource(R.string.cd_hinzufuegen)) }
                }
                Slider(neueStaerke, { neueStaerke = it }, valueRange = 0f..5f, steps = 4)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(eintrag.copy(
                    tempMinC = tempMin.toDoubleOrNull(),
                    tempMaxC = tempMax.toDoubleOrNull(),
                    luftfeuchte = feuchte.toIntOrNull(),
                    niederschlagMm = regenMm.toDoubleOrNull(),
                    raumtempC = raumTemp.toDoubleOrNull(),
                    raumfeuchte    = raumFeuchte.toIntOrNull(),
                                ))
            }) { Text(stringResource(R.string.btn_speichern)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_abbrechen)) }
        }
    )
}
