package com.allerpaw.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateUp: () -> Unit) {
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
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Account", style = MaterialTheme.typography.titleSmall)
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Abmelden") },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Daten", style = MaterialTheme.typography.titleSmall)
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                ListItem(headlineContent = { Text("Backup erstellen") })
                HorizontalDivider()
                ListItem(headlineContent = { Text("Backup wiederherstellen") })
            }
        }
    }
}
