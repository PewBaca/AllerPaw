package com.allerpaw.app.ui.tagebuch.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun <T : Any> TabListe(
    eintraege: List<T>,
    leerText: String,
    headerContent: (@Composable () -> Unit)? = null,
    itemContent: @Composable (T) -> Unit
) {
    if (eintraege.isEmpty() && headerContent == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📭", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text(leerText, color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    } else {
        LazyColumn(
            contentPadding      = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier            = Modifier.fillMaxSize()
        ) {
            headerContent?.let { header ->
                item { header() }
            }
            if (eintraege.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📭", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(leerText, color = MaterialTheme.colorScheme.outline,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                items(eintraege) { item ->
                    itemContent(item)
                }
            }
        }
    }
}
