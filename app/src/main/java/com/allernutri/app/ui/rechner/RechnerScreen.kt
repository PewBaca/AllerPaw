package com.allernutri.app.ui.rechner

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allernutri.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RechnerScreen(vm: RechnerViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.label_futterrechner)) },
                actions = {
                    if (state.selectedHundId != null) {
                        IconButton(onClick = vm::newRezept) {
                            Icon(Icons.Default.Add, stringResource(R.string.cd_neues_rezept))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            if (state.hunde.isEmpty()) {
                EmptyHundeHint()
                return@Scaffold
            }

            if (state.hunde.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = state.hunde.indexOfFirst { it.id == state.selectedHundId }
                        .coerceAtLeast(0),
                    edgePadding = 12.dp
                ) {
                    state.hunde.forEach { hund ->
                        Tab(
                            selected = hund.id == state.selectedHundId,
                            onClick  = { vm.selectHund(hund.id) },
                            text     = { Text(hund.name) }
                        )
                    }
                }
            }

            ScrollableTabRow(
                selectedTabIndex = RechnerTab.entries.indexOf(state.aktuellerTab),
                edgePadding      = 0.dp
            ) {
                RechnerTab.entries.forEach { tab ->
                    Tab(
                        selected = tab == state.aktuellerTab,
                        onClick  = { vm.selectTab(tab) },
                        text     = { Text(tab.label, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            when (state.aktuellerTab) {
                RechnerTab.REZEPTE       -> RezeptListeTab(state, vm)
                RechnerTab.NRC           -> NrcAnalyseTab(state, vm)
                RechnerTab.UMSTELLUNG    -> FutterUmstellungsRechner(state, vm)
                RechnerTab.ENERGIEBEDARF -> Energiebedarf(state, vm)
            }
        }
    }

    state.editRezept?.let { rezept ->
        RezeptEditDialog(
            rezept    = rezept,
            hundeList = state.hunde,
            onDismiss = vm::dismissEdit,
            onSave    = { r -> vm.saveRezept(r, emptyList()) }
        )
    }
}

@Composable
private fun EmptyHundeHint() {
    Box(
        modifier          = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment  = androidx.compose.ui.Alignment.Center
    ) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text("🐾", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.empty_hunde_detail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline)
        }
    }
}
