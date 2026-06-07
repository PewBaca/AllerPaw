package com.allernutri.app.ui.rechner

import com.allernutri.app.domain.NaehrstoffErgebnis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.allernutri.app.R

/**
 * Tab: NRC-Nährstoffanalyse für das ausgewählte Rezept.
 */
@Composable
fun NrcAnalyseTab(
    state: RechnerUiState,
    vm: RechnerViewModel
) {
    if (state.selectedHundId == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.kein_hund_ausgewaehlt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        return
    }

    if (state.ergebnisse.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.nrc_keine_analyse),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.nrc_hinweis_rezept_waehlen),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.nrc_analyse_titel),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(state.ergebnisse) { ergebnis ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text  = ergebnis.naehrstoff.key,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text  = "${ergebnis.istWert} / ${ergebnis.naehrstoff.bedarfPro1000kcal}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    val ratio = if (ergebnis.naehrstoff.bedarfPro1000kcal > 0)
                        (ergebnis.istWert / ergebnis.naehrstoff.bedarfPro1000kcal).coerceIn(0.0, 2.0)
                    else 0.0
                    val color = when {
                        ratio < 0.8  -> MaterialTheme.colorScheme.error
                        ratio > 1.5  -> MaterialTheme.colorScheme.tertiary
                        else         -> MaterialTheme.colorScheme.primary
                    }
                    LinearProgressIndicator(
                        progress            = { ratio.toFloat().coerceIn(0f, 1f) },
                        modifier            = Modifier.width(80.dp),
                        color               = color,
                        trackColor          = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}
