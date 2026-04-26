package com.allerpaw.app.ui.export

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allerpaw.app.util.PdfExporter
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(vm: ExportViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Export") }) }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.padding(padding).fillMaxSize(),
            contentPadding      = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Hund-Auswahl ─────────────────────────────────────────────
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Hund", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.hunde.forEach { hund ->
                                FilterChip(
                                    selected = hund.id == state.selectedHundId,
                                    onClick  = { vm.selectHund(hund.id) },
                                    label    = { Text(hund.name) }
                                )
                            }
                        }
                    }
                }
            }

            // ── Zeitraum ──────────────────────────────────────────────────
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Zeitraum", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(30 to "30 T", 90 to "90 T", 180 to "6 M", 365 to "1 J").forEach { (tage, label) ->
                                FilterChip(
                                    selected = state.bisDatum.minusDays(tage.toLong()) == state.vonDatum,
                                    onClick  = {
                                        vm.setVonDatum(LocalDate.now().minusDays(tage.toLong()))
                                        vm.setBisDatum(LocalDate.now())
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                        Text("${state.vonDatum} – ${state.bisDatum}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            // ── PDF Sektionen ─────────────────────────────────────────────
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("PDF-Sektionen", style = MaterialTheme.typography.titleSmall)
                        PdfExporter.Sektion.entries.forEach { sektion ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked   = sektion in state.sektionen,
                                    onCheckedChange = { vm.toggleSektion(sektion) }
                                )
                                Text(sektion.label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // ── Export-Buttons ────────────────────────────────────────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // PDF
                    Button(
                        onClick  = vm::exportPdf,
                        enabled  = !state.isExporting && state.selectedHundId != null,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (state.isExporting) {
                            CircularProgressIndicator(Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Default.PictureAsPdf, null, Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("PDF-Bericht erstellen & teilen")
                    }

                    // CSV
                    OutlinedButton(
                        onClick  = vm::exportCsv,
                        enabled  = !state.isExporting && state.selectedHundId != null,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.TableChart, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("CSV-Export (Symptome)")
                    }

                    // SQLite Backup
                    OutlinedButton(
                        onClick  = vm::exportBackup,
                        enabled  = !state.isExporting,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.Backup, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Datenbank-Backup (.db)")
                    }
                }
            }

            // ── Fehler ────────────────────────────────────────────────────
            state.fehler?.let { fehler ->
                item {
                    Card(colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null,
                                tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(fehler, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // ── Info ──────────────────────────────────────────────────────
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Hinweise", style = MaterialTheme.typography.titleSmall)
                        Text("• PDF wird via Share-Intent geteilt (Email, Drive, etc.)",
                            style = MaterialTheme.typography.bodySmall)
                        Text("• CSV enthält alle Symptomeinträge im gewählten Zeitraum",
                            style = MaterialTheme.typography.bodySmall)
                        Text("• Datenbank-Backup kann zur Wiederherstellung genutzt werden",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
