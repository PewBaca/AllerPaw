package com.allerpaw.app.ui.rechner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.allerpaw.app.R

@Composable
fun FutterUmstellungsRechner(state: RechnerUiState, vm: RechnerViewModel) {
    var rezeptBId  by remember { mutableStateOf<Long?>(null) }
    var tage       by remember { mutableStateOf(7) }
    var eigenTage  by remember { mutableStateOf("") }
    var schnell    by remember { mutableStateOf(false) }

    val eigenTageZahl = eigenTage.toIntOrNull()?.coerceIn(5, 14)

    LazyColumn(
        contentPadding      = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.rezept_aktuelles),
                        style = MaterialTheme.typography.titleSmall)
                    Text(
                        state.rezepte.firstOrNull { it.id == state.selectedRezeptId }?.name
                            ?: stringResource(R.string.rezept_waehlen),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.rezept_neues),
                        style = MaterialTheme.typography.titleSmall)
                    if (state.rezepte.size <= 1) {
                        Text(stringResource(R.string.empty_rezept_hund),
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodySmall)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            state.rezepte.filter { it.id != state.selectedRezeptId }.forEach { r ->
                                FilterChip(
                                    selected = r.id == rezeptBId,
                                    onClick  = { rezeptBId = r.id },
                                    label    = { Text(r.name) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.umstellung_geschwindigkeit),
                        style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5 to "5 T", 7 to "7 T", 10 to "10 T", 14 to "14 T").forEach { (t, l) ->
                            FilterChip(selected = tage == t && eigenTage.isBlank(),
                                onClick = { tage = t; eigenTage = "" },
                                label = { Text(l) })
                        }
                        FilterChip(selected = eigenTage.isNotBlank(),
                            onClick = { eigenTage = "7" },
                            label = { Text(stringResource(R.string.label_eigene_tageanzahl).take(7)) })
                    }
                    if (eigenTage.isNotBlank()) {
                        OutlinedTextField(
                            value         = eigenTage,
                            onValueChange = { eigenTage = it },
                            label         = { Text(stringResource(R.string.label_tage)) },
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth()
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = schnell, onCheckedChange = { schnell = it })
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.umstellung_sanft),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            val effektiveTage = eigenTageZahl ?: tage
            Button(
                onClick = { vm.berechneUmstellungsplan(rezeptBId, effektiveTage, schnell) },
                enabled  = rezeptBId != null,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(stringResource(R.string.umstellung_plan_erstellen, effektiveTage))
            }
        }

        state.umstellungsplan?.let { plan ->
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.umstellung_plan),
                            style = MaterialTheme.typography.titleSmall)
                        plan.forEach { tag ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Text("Tag ${tag.tagNr}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.width(50.dp))
                                LinearProgressIndicator(
                                    progress = { tag.anteilB },
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                )
                                Text("${(tag.anteilB * 100).toInt()}% B",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.umstellung_tipps),
                            style = MaterialTheme.typography.titleSmall)
                        listOf(
                            "Beobachte Stuhlkonsistenz und Allgemeinbefinden täglich.",
                            "Bei Durchfall oder Erbrechen: Schritt wiederholen.",
                            "Notiere Auffälligkeiten im Tagebuch.",
                            "Wasser immer frisch und ausreichend anbieten."
                        ).forEach { tipp ->
                            Text("• $tipp", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        if (rezeptBId == null) {
            item {
                Text(
                    stringResource(R.string.rezept_neues) + " " + stringResource(R.string.label_kein),
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
