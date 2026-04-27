package com.allerpaw.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allerpaw.app.util.LocaleHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val ctx   = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Account ─────────────────────────────────────────────────────
            item {
                Text("Account", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                OutlinedCard(Modifier.fillMaxWidth()) {
                    state.userEmail?.let { email ->
                        ListItem(
                            headlineContent   = { Text(email) },
                            supportingContent = { Text("Angemeldet") },
                            leadingContent    = { Icon(Icons.Default.Person, null) }
                        )
                        HorizontalDivider()
                    }
                    ListItem(
                        headlineContent = { Text("Abmelden") },
                        leadingContent  = { Icon(Icons.Default.Logout, null,
                            tint = MaterialTheme.colorScheme.error) },
                        modifier        = Modifier.clickable { vm.signOut() }
                    )
                }
            }

            // ── Sprache ──────────────────────────────────────────────────────
            item {
                Text("Sprache", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                OutlinedCard(Modifier.fillMaxWidth()) {
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

            // ── Standort ─────────────────────────────────────────────────────
            item {
                Text("Standort (Wetter & Pollen)", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value         = state.latInput,
                                onValueChange = vm::setLatInput,
                                label         = { Text("Breitengrad") },
                                modifier      = Modifier.weight(1f),
                                singleLine    = true
                            )
                            OutlinedTextField(
                                value         = state.lonInput,
                                onValueChange = vm::setLonInput,
                                label         = { Text("Längengrad") },
                                modifier      = Modifier.weight(1f),
                                singleLine    = true
                            )
                        }
                        Button(onClick = vm::saveStandort,
                            modifier = Modifier.fillMaxWidth()) { Text("Speichern") }
                        Text("Aktuell: ${state.standortLat}, ${state.standortLon}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            // ── Vitaminanzeige ────────────────────────────────────────────────
            item {
                Text("Vitaminanzeige", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                OutlinedCard(Modifier.fillMaxWidth()) {
                    listOf("metrisch" to "Metrisch (µg / mg)",
                           "ie"       to "Internationale Einheiten (IE)").forEach { (v, l) ->
                        ListItem(
                            headlineContent = { Text(l) },
                            leadingContent  = {
                                RadioButton(selected = state.ieAnzeige == v,
                                    onClick = { vm.setIeAnzeige(v) })
                            },
                            modifier = Modifier.clickable { vm.setIeAnzeige(v) }
                        )
                    }
                }
            }

            // ── Daten ─────────────────────────────────────────────────────────
            item {
                Text("Daten & Backup", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                OutlinedCard(Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("Datenbank-Backup erstellen") },
                        leadingContent  = { Icon(Icons.Default.Backup, null) },
                        modifier        = Modifier.clickable { vm.onBackupClick() }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Backup wiederherstellen") },
                        leadingContent  = { Icon(Icons.Default.Restore, null) },
                        modifier        = Modifier.clickable { vm.onRestoreClick() }
                    )
                }
            }

            // ── Version ───────────────────────────────────────────────────────
            item {
                Text("AllerPaw v0.6.0 · Daten lokal gespeichert",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
