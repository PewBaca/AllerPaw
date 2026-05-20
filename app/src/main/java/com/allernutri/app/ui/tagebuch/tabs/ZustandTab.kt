package com.allernutri.app.ui.tagebuch.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allernutri.app.R
import com.allernutri.app.ui.tagebuch.TagebuchUiState
import com.allernutri.app.ui.tagebuch.TagebuchViewModel

@Composable
fun ZustandTab(state: TagebuchUiState, vm: TagebuchViewModel) {
    val emojis = listOf("😊" to 5, "🙂" to 4, "😐" to 3, "😟" to 2, "😰" to 1)
    var gewaehlt by remember { mutableStateOf<Int?>(state.heutigenZustand?.zustand) }
    var notiz    by remember { mutableStateOf(state.heutigenZustand?.notiz ?: "") }

    Column(
        modifier            = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.tagebuch_wie_geht_es),
            style = MaterialTheme.typography.titleMedium)

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            emojis.forEach { (emoji, wert) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.padding(4.dp)
                ) {
                    ElevatedCard(
                        onClick = { gewaehlt = wert },
                        colors  = if (gewaehlt == wert)
                            CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer)
                        else CardDefaults.elevatedCardColors()
                    ) {
                        Text(emoji, fontSize = 36.sp, modifier = Modifier.padding(10.dp))
                    }
                    if (gewaehlt == wert) {
                        Icon(Icons.Default.Check, null,
                            modifier = Modifier.size(16.dp),
                            tint     = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        OutlinedTextField(
            value         = notiz,
            onValueChange = { notiz = it },
            label         = { Text(stringResource(R.string.label_notiz)) },
            modifier      = Modifier.fillMaxWidth(),
            minLines      = 3
        )

        Button(
            onClick  = { gewaehlt?.let { vm.saveZustand(it, notiz) } },
            enabled  = gewaehlt != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Save, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.tagebuch_zustand_speichern))
        }

        state.heutigenZustand?.let { zustand ->
            HorizontalDivider()
            OutlinedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(emojis.firstOrNull { it.second == zustand.zustand }?.first ?: "😐",
                        fontSize = 28.sp)
                    Column {
                        Text("Heute gespeichert", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                        if (zustand.notiz.isNotBlank())
                            Text(zustand.notiz, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                }
            }
        }
    }
}
