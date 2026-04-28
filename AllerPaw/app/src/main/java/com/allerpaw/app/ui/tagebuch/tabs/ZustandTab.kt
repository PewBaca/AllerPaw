package com.allerpaw.app.ui.tagebuch.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allerpaw.app.data.local.entity.TagebuchHundZustandEntity
import com.allerpaw.app.ui.tagebuch.TagebuchUiState
import com.allerpaw.app.ui.tagebuch.TagebuchViewModel

private val SMILEYS = listOf(
    1 to ("😄" to "Sehr gut"),
    2 to ("🙂" to "Gut"),
    3 to ("😐" to "Neutral"),
    4 to ("😟" to "Schlecht"),
    5 to ("😢" to "Sehr schlecht")
)

private fun zustandFarbe(zustand: Int): Color = when (zustand) {
    1    -> Color(0xFF4CAF50)
    2    -> Color(0xFF8BC34A)
    3    -> Color(0xFFFF9800)
    4    -> Color(0xFFFF5722)
    5    -> Color(0xFFF44336)
    else -> Color.Gray
}

@Composable
fun ZustandTab(state: TagebuchUiState, vm: TagebuchViewModel) {
    Column(Modifier.fillMaxSize()) {

        // ── Heutiger Zustand eingeben ─────────────────────────────────────
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Wie geht es heute?",
                    style = MaterialTheme.typography.titleMedium)

                // Smiley-Auswahl
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SMILEYS.forEach { (wert, pair) ->
                        val (emoji, label) = pair
                        val istGewählt = state.heutigerZustand == wert
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { vm.setZustand(wert) }
                                .padding(4.dp)
                        ) {
                            Text(
                                text     = emoji,
                                fontSize = if (istGewählt) 44.sp else 34.sp,
                                modifier = Modifier.padding(4.dp)
                            )
                            if (istGewählt) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = zustandFarbe(wert),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Notiz-Feld
                OutlinedTextField(
                    value         = state.zustandNotiz,
                    onValueChange = vm::setZustandNotiz,
                    label         = { Text("Notiz (optional)") },
                    modifier      = Modifier.fillMaxWidth(),
                    minLines      = 2
                )

                Button(
                    onClick  = vm::saveZustand,
                    enabled  = state.heutigerZustand > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Zustand speichern")
                }
            }
        }

        // ── Verlauf ───────────────────────────────────────────────────────
        if (state.zustandVerlauf.isNotEmpty()) {
            Text(
                "Verlauf",
                style    = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            LazyColumn(
                contentPadding      = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.zustandVerlauf, key = { it.id }) { eintrag ->
                    ZustandVerlaufCard(eintrag)
                }
            }
        }
    }
}

@Composable
private fun ZustandVerlaufCard(e: TagebuchHundZustandEntity) {
    val (emoji, label) = SMILEYS.find { it.first == e.zustand }?.second ?: ("❓" to "Unbekannt")
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(emoji, fontSize = 28.sp)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(e.datum.toString(),
                        style = MaterialTheme.typography.bodyMedium)
                    Text(label,
                        style = MaterialTheme.typography.bodySmall,
                        color = zustandFarbe(e.zustand))
                }
                if (e.notizen.isNotBlank()) {
                    Text(e.notizen,
                        style   = MaterialTheme.typography.bodySmall,
                        color   = MaterialTheme.colorScheme.outline,
                        maxLines = 2)
                }
            }
        }
    }
}
