package com.allernutri.app.ui.sheets

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
import com.allernutri.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetsImportScreen(
    onNavigateUp: () -> Unit,
    vm: SheetsImportViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sheets_title)) },
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
            item {
                Text("Schritt 1: Spreadsheet-ID",
                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value         = state.spreadsheetId,
                            onValueChange = vm::setSpreadsheetId,
                            label         = { Text(stringResource(R.string.label_spreadsheet_id)) },
                            placeholder   = { Text("1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgVE2upms") },
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true
                        )
                        Text(stringResource(R.string.sheets_id_hinweis),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                        Button(
                            onClick  = vm::ladeVorschau,
                            enabled  = state.spreadsheetId.isNotBlank() && !state.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.isLoading) CircularProgressIndicator(
                                Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                            else Icon(Icons.Default.CloudDownload, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (state.isLoading) stringResource(R.string.sheets_lade_vorschau)
                                 else stringResource(R.string.btn_vorschau_laden))
                        }
                    }
                }
            }

            state.vorschauZeilen.takeIf { it.isNotEmpty() }?.let { zeilen ->
                item {
                    Text("Schritt 2: Spalten-Mapping",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.sheets_vorschau_laden),
                                style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            zeilen.take(3).forEachIndexed { i, zeile ->
                                Text("Z${i + 1}: ${zeile.values.take(4).joinToString("  |  ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }

                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.mappings.forEach { (feld, spalte) ->
                                val autoErkannt = state.autoMappings[feld] != null
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(feld, style = MaterialTheme.typography.bodySmall)
                                        if (autoErkannt) {
                                            Text(stringResource(R.string.sheets_auto_erkannt),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    val spaltenOptionen = listOf(stringResource(R.string.sheets_nicht_zuordnen)) +
                                        (zeilen.firstOrNull()?.keys?.toList() ?: emptyList())
                                    var expanded by remember { mutableStateOf(false) }

                                    ExposedDropdownMenuBox(
                                        expanded         = expanded,
                                        onExpandedChange = { expanded = it },
                                        modifier         = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value           = spalte ?: stringResource(R.string.sheets_nicht_zuordnen),
                                            onValueChange   = {},
                                            readOnly        = true,
                                            trailingIcon    = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                            singleLine      = true,
                                            textStyle       = MaterialTheme.typography.bodySmall,
                                            modifier        = Modifier.menuAnchor().fillMaxWidth(),
                                            label           = if (autoErkannt) {{
                                                Text(stringResource(R.string.sheets_manuell_zuordnen),
                                                    style = MaterialTheme.typography.labelSmall)
                                            }} else null
                                        )
                                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                            val nichtZuordnenLabel = stringResource(R.string.sheets_nicht_zuordnen)
                                            spaltenOptionen.forEach { opt ->
                                                DropdownMenuItem(
                                                    text    = { Text(opt, style = MaterialTheme.typography.bodySmall) },
                                                    onClick = {
                                                        vm.setMapping(feld, if (opt == stringResource(R.string.sheets_nicht_zuordnen)) null else opt)
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Schritt 3: Import starten",
                        style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
                item {
                    Button(
                        onClick  = vm::starteImport,
                        enabled  = !state.isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (state.isLoading) CircularProgressIndicator(
                            Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                        else Icon(Icons.Default.CloudUpload, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.isLoading) stringResource(R.string.sheets_importiere)
                             else stringResource(R.string.btn_import_starten))
                    }
                }
            }

            state.fehler?.let { fehler ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            Text(fehler, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            state.successMessage?.let { msg ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            Text(msg, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
