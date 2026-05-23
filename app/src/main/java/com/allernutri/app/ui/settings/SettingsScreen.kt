package com.allernutri.app.ui.settings

import androidx.compose.foundation.clickable
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
import com.allernutri.app.R
import com.allernutri.app.util.LocaleHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigateBackup: () -> Unit,
    onNavigateSheets: () -> Unit,
    onNavigateSheetsExport: () -> Unit,
    onNavigateApiKeys: () -> Unit = {},
    vm: SettingsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val ctx   = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.einstellungen_title)) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsGruppe(titel = "Account") {
                    state.userEmail?.let { email ->
                        ListItem(
                            headlineContent   = { Text(email) },
                            supportingContent = { Text(stringResource(R.string.auth_angemeldet)) },
                            leadingContent    = { Icon(Icons.Default.Person, null) }
                        )
                        HorizontalDivider()
                    }
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.auth_abmelden)) },
                        leadingContent  = { Icon(Icons.Default.Logout, null,
                            tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable { vm.signOut() }
                    )
                }
            }

            item {
                SettingsGruppe(titel = "Sprache") {
                    LocaleHelper.SUPPORTED.forEach { (tag, label) ->
                        ListItem(
                            headlineContent = { Text(label) },
                            leadingContent  = {
                                RadioButton(
                                    selected = state.sprache == tag,
                                    onClick  = { vm.setSprache(tag, ctx) }
                                )
                            },
                            modifier = Modifier.clickable { vm.setSprache(tag, ctx) }
                        )
                    }
                }
            }

            item {
                SettingsGruppe(titel = "Vitaminanzeige") {
                    listOf(
                        "metrisch" to "Metrisch (µg / mg)",
                        "ie"       to "Internationale Einheiten (IE)"
                    ).forEach { (v, l) ->
                        ListItem(
                            headlineContent = { Text(l) },
                            leadingContent  = {
                                RadioButton(
                                    selected = state.ieAnzeige == v,
                                    onClick  = { vm.setIeAnzeige(v) }
                                )
                            },
                            modifier = Modifier.clickable { vm.setIeAnzeige(v) }
                        )
                    }
                }
            }

            item {
                SettingsGruppe(titel = "Standort (Wetter & Pollen)") {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value         = state.latInput,
                                onValueChange = vm::setLatInput,
                                label         = { Text(stringResource(R.string.label_breitengrad)) },
                                modifier      = Modifier.weight(1f),
                                singleLine    = true
                            )
                            OutlinedTextField(
                                value         = state.lonInput,
                                onValueChange = vm::setLonInput,
                                label         = { Text(stringResource(R.string.label_laengengrad)) },
                                modifier      = Modifier.weight(1f),
                                singleLine    = true
                            )
                        }
                        Button(onClick = vm::saveStandort, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.btn_speichern))
                        }
                        Text(
                            "Aktuell: ${state.standortLat}, ${state.standortLon}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            item {
                SettingsGruppe(titel = "Daten & Backup") {
                    ListItem(
                        headlineContent   = { Text(stringResource(R.string.backup_title)) },
                        supportingContent = { Text(stringResource(R.string.stammdaten_hund_erstellen)) },
                        leadingContent    = { Icon(Icons.Default.Backup, null) },
                        trailingContent   = { Icon(Icons.Default.ChevronRight, null) },
                        modifier          = Modifier.clickable { onNavigateBackup() }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent   = { Text(stringResource(R.string.sheets_title)) },
                        supportingContent = { Text(stringResource(R.string.sheets_daten_importieren)) },
                        leadingContent    = { Icon(Icons.Default.TableChart, null) },
                        trailingContent   = { Icon(Icons.Default.ChevronRight, null) },
                        modifier          = Modifier.clickable { onNavigateSheets() }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent   = { Text("Google Sheets Export") },
                        supportingContent = { Text("Tagebuch-Daten als Sheets exportieren") },
                        leadingContent    = { Icon(Icons.Default.CloudUpload, null) },
                        trailingContent   = { Icon(Icons.Default.ChevronRight, null) },
                        modifier          = Modifier.clickable { onNavigateSheetsExport() }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent   = { Text("API-Keys (USDA, Edamam)") },
                        supportingContent = { Text("Keys für Lebensmittel-Import konfigurieren") },
                        leadingContent    = { Icon(Icons.Default.Key, null) },
                        trailingContent   = { Icon(Icons.Default.ChevronRight, null) },
                        modifier          = Modifier.clickable { onNavigateApiKeys() }
                    )
                }
            }

            item {
                Text(
                    "AllerNutri v0.11.0012 · Alle Daten lokal gespeichert",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsGruppe(titel: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        titel,
        style    = MaterialTheme.typography.titleSmall,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(content = content)
    }
}
