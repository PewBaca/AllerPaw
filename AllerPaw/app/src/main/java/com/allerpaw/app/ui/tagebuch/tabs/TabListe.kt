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

@Composable
fun <T> TabListe(
    eintraege: List<T>,
    leerText: String,
    itemContent: @Composable (T) -> Unit
) {
    if (eintraege.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Inbox, null, Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(8.dp))
                Text(leerText, color = MaterialTheme.colorScheme.outline)
            }
        }
    } else {
        LazyColumn(
            contentPadding      = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier            = Modifier.fillMaxSize()
        ) {
            items(eintraege as List<Any?>, key = { it.hashCode() }) { item ->
                @Suppress("UNCHECKED_CAST")
                itemContent(item as T)
            }
        }
    }
}
