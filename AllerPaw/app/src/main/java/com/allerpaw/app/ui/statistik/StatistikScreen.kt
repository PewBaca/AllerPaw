package com.allerpaw.app.ui.statistik

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatistikScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Statistik") }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.BarChart, null, Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(12.dp))
                Text("Symptom- & Pollenstatistik folgt in Phase 3.",
                    color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
