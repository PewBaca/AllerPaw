package com.allerpaw.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allerpaw.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeysScreen(
    onNavigateUp: () -> Unit,
    vm: ApiKeysViewModel = hiltViewModel()
) {
    val state    by vm.state.collectAsState()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API-Keys konfigurieren") },
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

            // ── Erklärung ──────────────────────────────────────────────────
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.Top) {
                        Icon(Icons.Default.Info, null, Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.outline)
                        Text(
                            "Für USDA und Edamam sind kostenlose API-Keys erforderlich. " +
                            "Open Food Facts benötigt keinen Key. " +
                            "Die Keys werden lokal gespeichert und nie übertragen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // ── Open Food Facts ────────────────────────────────────────────
            item {
                ApiKeyCard(
                    titel        = "Open Food Facts",
                    beschreibung = "Kostenlos, kein Key nötig. Barcode + Textsuche. " +
                                   "Hauptsächlich verarbeitete Lebensmittel.",
                    url          = "https://world.openfoodfacts.org",
                    statusOk     = true,
                    statusText   = "✅ Kein Key erforderlich",
                    felder       = emptyList(),
                    onSave       = {}
                )
            }

            // ── USDA FoodData Central ──────────────────────────────────────
            item {
                var keyInput by remember { mutableStateOf(state.usdaKey) }
                var sichtbar by remember { mutableStateOf(false) }

                LaunchedEffect(state.usdaKey) { keyInput = state.usdaKey }

                ApiKeyCard(
                    titel        = "USDA FoodData Central",
                    beschreibung = "Kostenloser Key, bis zu 3.500 Anfragen/Stunde. " +
                                   "Sehr detaillierte Nährwerte für Rohzutaten (Fleisch, Fisch, Gemüse).",
                    url          = "https://fdc.nal.usda.gov/api-key-signup.html",
                    statusOk     = state.usdaKey.isNotBlank(),
                    statusText   = if (state.usdaKey.isNotBlank()) "✅ Key konfiguriert" else "⚠️ Kein Key",
                    felder       = listOf(
                        ApiKeyFeld(
                            label       = "API-Key",
                            wert        = keyInput,
                            onChange    = { keyInput = it },
                            sichtbar    = sichtbar,
                            onToggle    = { sichtbar = !sichtbar },
                            placeholder = "DEMO_KEY oder dein persönlicher Key"
                        )
                    ),
                    onSave = { vm.saveUsdaKey(keyInput) }
                )
            }

            // ── Edamam ────────────────────────────────────────────────────
            item {
                var appId  by remember { mutableStateOf(state.edamamAppId) }
                var appKey by remember { mutableStateOf(state.edamamAppKey) }
                var sichtbar by remember { mutableStateOf(false) }

                LaunchedEffect(state.edamamAppId, state.edamamAppKey) {
                    appId  = state.edamamAppId
                    appKey = state.edamamAppKey
                }

                ApiKeyCard(
                    titel        = "Edamam Food Database",
                    beschreibung = "Kostenloser Tier: 1.000 Anfragen/Monat. " +
                                   "Gute Abdeckung für Lebensmittel inkl. EPA/DHA-Fettsäuren.",
                    url          = "https://developer.edamam.com/food-database-api",
                    statusOk     = state.edamamAppId.isNotBlank() && state.edamamAppKey.isNotBlank(),
                    statusText   = if (state.edamamAppId.isNotBlank() && state.edamamAppKey.isNotBlank())
                        "✅ Keys konfiguriert" else "⚠️ Keys fehlen",
                    felder       = listOf(
                        ApiKeyFeld(
                            label       = "App ID",
                            wert        = appId,
                            onChange    = { appId = it },
                            sichtbar    = sichtbar,
                            onToggle    = { sichtbar = !sichtbar },
                            placeholder = "z.B. a1b2c3d4"
                        ),
                        ApiKeyFeld(
                            label       = "App Key",
                            wert        = appKey,
                            onChange    = { appKey = it },
                            sichtbar    = sichtbar,
                            onToggle    = { sichtbar = !sichtbar },
                            placeholder = "z.B. 1234abcd…"
                        )
                    ),
                    onSave = { vm.saveEdamamKeys(appId, appKey) }
                )
            }

            // ── Erfolgs-Meldung ────────────────────────────────────────────
            state.savedMessage?.let { msg ->
                item {
                    Card(colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Row(Modifier.padding(12.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = MaterialTheme.colorScheme.primary)
                            Text(msg, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

// ── API-Key-Karte ─────────────────────────────────────────────────────────────

data class ApiKeyFeld(
    val label: String,
    val wert: String,
    val onChange: (String) -> Unit,
    val sichtbar: Boolean,
    val onToggle: () -> Unit,
    val placeholder: String = ""
)

@Composable
private fun ApiKeyCard(
    titel: String,
    beschreibung: String,
    url: String,
    statusOk: Boolean,
    statusText: String,
    felder: List<ApiKeyFeld>,
    onSave: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(titel, style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text(statusText, style = MaterialTheme.typography.labelSmall,
                        color = if (statusOk) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = { uriHandler.openUri(url) }) {
                    Icon(Icons.Default.OpenInBrowser, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Registrieren", style = MaterialTheme.typography.labelSmall)
                }
            }

            Text(beschreibung, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)

            felder.forEach { feld ->
                OutlinedTextField(
                    value         = feld.wert,
                    onValueChange = feld.onChange,
                    label         = { Text(feld.label) },
                    placeholder   = { Text(feld.placeholder,
                        style = MaterialTheme.typography.labelSmall) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    visualTransformation = if (feld.sichtbar)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon  = {
                        IconButton(onClick = feld.onToggle) {
                            Icon(
                                if (feld.sichtbar) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                if (feld.sichtbar) "Verbergen" else "Anzeigen"
                            )
                        }
                    }
                )
            }

            if (felder.isNotEmpty()) {
                Button(
                    onClick  = onSave,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_speichern))
                }
            }
        }
    }
}
