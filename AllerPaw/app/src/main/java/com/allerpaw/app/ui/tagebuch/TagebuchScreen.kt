package com.allerpaw.app.ui.tagebuch

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allerpaw.app.ui.tagebuch.tabs.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagebuchScreen(
    onNavigateToSettings: () -> Unit,
    vm: TagebuchViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val undoStack by vm.undoManager.stack.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(undoStack) {
        undoStack.lastOrNull()?.let { item ->
            val result = snackbarHostState.showSnackbar(
                message     = item.label,
                actionLabel = "Rückgängig",
                duration    = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                vm.undoManager.undo(item.item)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tagebuch") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Einstellungen")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Kein FAB auf Zustand-Tab (Smiley hat eigene Buttons)
            if (state.aktuellerTab != TagebuchTab.ZUSTAND) {
                FloatingActionButton(onClick = {
                    when (state.aktuellerTab) {
                        TagebuchTab.UMWELT      -> vm.newUmwelt()
                        TagebuchTab.SYMPTOM     -> vm.newSymptom()
                        TagebuchTab.FUTTER      -> vm.newFutter()
                        TagebuchTab.AUSSCHLUSS  -> vm.newAusschluss()
                        TagebuchTab.ALLERGEN    -> vm.newAllergen()
                        TagebuchTab.TIERARZT    -> vm.newTierarzt()
                        TagebuchTab.MEDIKAMENT  -> vm.newMedikament()
                        TagebuchTab.PHASEN      -> vm.newPhase()
                        else                    -> {}
                    }
                }) { Icon(Icons.Default.Add, "Neuer Eintrag") }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            // Hund-Auswahl (nur wenn mehrere Hunde)
            if (state.hunde.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = state.hunde
                        .indexOfFirst { it.id == state.selectedHundId }.coerceAtLeast(0),
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

            // Tab-Navigation (9 Tabs inkl. Zustand)
            ScrollableTabRow(
                selectedTabIndex = TagebuchTab.entries.indexOf(state.aktuellerTab),
                edgePadding      = 0.dp
            ) {
                TagebuchTab.entries.forEach { tab ->
                    Tab(
                        selected = tab == state.aktuellerTab,
                        onClick  = { vm.selectTab(tab) },
                        text     = {
                            Text(
                                tab.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }

            // Tab-Inhalt
            when (state.aktuellerTab) {
                TagebuchTab.ZUSTAND    -> ZustandTab(state, vm)
                TagebuchTab.UMWELT     -> UmweltTab(state, vm)
                TagebuchTab.SYMPTOM    -> SymptomTab(state, vm)
                TagebuchTab.FUTTER     -> FutterTab(state, vm)
                TagebuchTab.AUSSCHLUSS -> AusschlussTab(state, vm)
                TagebuchTab.ALLERGEN   -> AllergenTab(state, vm)
                TagebuchTab.TIERARZT   -> TierarztTab(state, vm)
                TagebuchTab.MEDIKAMENT -> MedikamentTab(state, vm)
                TagebuchTab.PHASEN     -> PhasenTab(state, vm)
            }
        }
    }
}
