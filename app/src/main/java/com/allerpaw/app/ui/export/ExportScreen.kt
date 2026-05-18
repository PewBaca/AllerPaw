package com.allerpaw.app.ui.export

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allerpaw.app.R
import com.allerpaw.app.util.PdfExporter
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(vm: ExportViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.export_title)) }) }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.padding(padding).fillMaxSize(),
            contentPadding      = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.label_hund), style = MaterialTheme.typography.titleSmall)
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

            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.label_zeitraum), style = MaterialTheme.typography.titleSmall)
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
                        Text(stringResource(R.string.zeitraum_von_bis, state.vonDatum, state.bisDatum),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.export_pdf_sektionen), style = MaterialTheme.typography.titleSmall)
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

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        Text(stringResource(R.string.export_pdf_bericht))
                    }

                    OutlinedButton(
                        onClick  = vm::exportCsv,
                        enabled  = !state.isExporting && state.selectedHundId != null,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.TableChart, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.export_csv_symptome))
                    }

                    OutlinedButton(
                        onClick  = vm::exportBackup,
                        enabled  = !state.isExporting,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.Backup, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.backup_format))
                    }
                }
            }

            state.fehler?.let { fehler ->
                item {
                    Card(colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(fehler, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.label_hinweise), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.export_tipp_pdf), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.export_tipp_csv), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.export_tipp_backup), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
