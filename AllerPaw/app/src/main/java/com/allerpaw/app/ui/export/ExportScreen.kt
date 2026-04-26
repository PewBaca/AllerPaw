package com.allerpaw.app.ui.export

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Export") }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PictureAsPdf, null, Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(12.dp))
                Text("PDF- & CSV-Export folgt in Phase 4.",
                    color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
