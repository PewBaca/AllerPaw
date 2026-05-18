package com.allerpaw.app.ui.sheets

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allerpaw.app.R
import com.allerpaw.app.data.repository.ExportTab
import com.allerpaw.app.data.repository.SheetsExportResult
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetsExportScreen(
    onNavigateUp: () -> Unit,
    vm: SheetsViewModel = hiltViewModel()
) {
    val state   = by vm.state.collectAsState()
    val ctx     = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Google Sheets Export") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.cd_zurueck))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.padding(padding).fillMaxSize(),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Hund-Auswahl ──────────────────────────────────────────────
            if (state.hunde.size > 1) {
                item {
                    Text("1. Hund wählen",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            state.hunde.forEach { hund ->
                                FilterChip(
                                    selected = hund.id == state.selectedHundId,
                                    onClick  = { vm.selectHund(hund.id) },
                                    label    = { Text(hund.name) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // ── Zeitraum ──────────────────────────────────────────────────
            item {
                Text("2. Zeitraum",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(30 to "30 T", 90 to "90 T", 180 to "6 M", 365 to "1 J").forEach { (tage, label) ->
                                FilterChip(
                                    selected = state.exportVon == LocalDate.now().minusDays(tage.toLong()),
                                    onClick  = {
                                        vm.setExportVon(LocalDate.now().minusDays(tage.toLong()))
                                        vm.setExportBis(LocalDate.now())
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                        Text(
                            stringResource(R.string.zeitraum_von_bis, state.exportVon, state.exportBis),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // ── Tab-Auswahl ───────────────────────────────────────────────
            item {
                Text("3. Daten-Tabs auswählen",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                "${state.exportTabs.size} von ${ExportTab.entries.size} ausgewählt",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Row {
                                TextButton(onClick = vm::selectAllTabs) { Text("Alle") }
                                TextButton(onClick = vm::clearAllTabs)  { Text("Keine") }
                            }
                        }
                        HorizontalDivider()
                        ExportTab.entries.forEach { tab ->
                            val istGewaehlt = tab in state.exportTabs
                            val tabInfo = tabBeschreibung(tab)
                            Row(
                                modifier          = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked       = istGewaehlt,
                                    onCheckedChange = { vm.toggleExportTab(tab) }
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(tab.label, style = MaterialTheme.typography.bodyMedium)
                                    Text(tabInfo, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline)
                                }
                                Text(tabIcon(tab), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }

            // ── Ziel-Sheet (optional) ─────────────────────────────────────
            item {
                Text("4. Ziel-Spreadsheet (optional)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value         = state.exportSpreadsheetId,
                            onValueChange = vm::setExportSpreadsheetId,
                            label         = { Text(stringResource(R.string.label_spreadsheet_id) + " (leer = neu erstellen)") },
                            placeholder   = { Text("Leer lassen für neues Spreadsheet") },
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true,
                            trailingIcon  = if (state.exportSpreadsheetId.isNotBlank()) {{
                                IconButton(onClick = { vm.setExportSpreadsheetId("") }) {
                                    Icon(Icons.Default.Clear, null)
                                }
                            }} else null
                        )
                        Text(
                            stringResource(R.string.sheets_id_hinweis),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // ── Export-Button ─────────────────────────────────────────────
            item {
                Button(
                    onClick  = vm::starteExport,
                    enabled  = !state.isLoading
                              && state.selectedHundId != null
                              && state.exportTabs.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.CloudUpload, null, Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (state.isLoading) "Exportiere…"
                        else "Export starten (${state.exportTabs.size} Tabs)"
                    )
                }
            }

            // ── Ergebnis: Erfolg ──────────────────────────────────────────
            state.exportedSheetUrl?.let { url ->
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors   = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, null,
                                    tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    state.successMessage ?: "Export erfolgreich!",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            OutlinedButton(
                                onClick  = {
                                    ctx.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.OpenInBrowser, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("In Google Sheets öffnen")
                            }
                            Text(
                                url,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // ── Ergebnis: Fehler ──────────────────────────────────────────
            state.fehler?.let { fehler ->
                item {
                    Card(
                        colors   = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            Text(
                                fehler,
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = vm::clearFehler, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    }
                }
            }

            // ── Hinweise ──────────────────────────────────────────────────
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.label_hinweise),
                            style = MaterialTheme.typography.titleSmall)
                        listOf(
                            "• Google-Anmeldung in den Einstellungen erforderlich.",
                            "• Leeres Spreadsheet-Feld → automatisch neues Sheet erstellen.",
                            "• Jeder Tab wird als eigenes Arbeitsblatt exportiert.",
                            "• Zeitraum gilt für Symptome, Umwelt und Futter.",
                            "  Allergene, Phasen und Medikamente werden vollständig exportiert.",
                            "• Bestehende Blätter im Ziel-Sheet werden überschrieben."
                        ).forEach { hint ->
                            Text(hint, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}

private fun tabBeschreibung(tab: ExportTab): String = when (tab) {
    ExportTab.SYMPTOME    -> "Beobachtete Symptome im gewählten Zeitraum"
    ExportTab.UMWELT      -> "Wetter, Pollen, Raumklima im gewählten Zeitraum"
    ExportTab.ALLERGENE   -> "Alle bestätigten Allergene (vollständig)"
    ExportTab.PHASEN      -> "Ausschluss- und Provokationsphasen (vollständig)"
    ExportTab.FUTTER      -> "Futter-Einträge mit Erstgabe/Provokation im Zeitraum"
    ExportTab.MEDIKAMENTE -> "Alle Medikamente (vollständig)"
}

private fun tabIcon(tab: ExportTab): String = when (tab) {
    ExportTab.SYMPTOME    -> "🤒"
    ExportTab.UMWELT      -> "🌤"
    ExportTab.ALLERGENE   -> "⚠️"
    ExportTab.PHASEN      -> "📋"
    ExportTab.FUTTER      -> "🍖"
    ExportTab.MEDIKAMENTE -> "💊"
}
