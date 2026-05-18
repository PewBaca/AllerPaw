package com.allerpaw.app.ui.rechner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Energiebedarf-Tab im Rechner.
 * Zeigt KcalBedarfCard mit RER/MER/Manuell-Override.
 */
@Composable
fun Energiebedarf(state: RechnerUiState, vm: RechnerViewModel) {
    val hund = state.hunde.find { it.id == state.selectedHundId }

    LazyColumn(
        contentPadding      = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier            = Modifier.fillMaxSize()
    ) {
        item {
            KcalBedarfCard(
                hund               = hund,
                rerKcal            = state.rerKcal,
                merKcal            = state.merKcal,
                effektiverKcal     = state.effektiverKcal,
                aktivitaetsFaktor  = state.aktivitaetsFaktor,
                kcalManuellInput   = state.kcalManuellInput,
                kcalManuellAktiv   = state.kcalManuellAktiv,
                onFaktorChange     = vm::setAktivitaetsFaktor,
                onKcalManuellChange = vm::setKcalManuellInput,
                onAktivieren       = vm::aktiviereKcalManuell,
                onReset            = vm::resetKcalManuell
            )
        }

        // Erklärungskarte
        item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Wie wird der Energiebedarf berechnet?",
                        style = MaterialTheme.typography.titleSmall)
                    Text(
                        "RER (Ruhenergiebedarf) = 70 × Gewicht^0,75 kcal/Tag\n" +
                        "MER (Erhaltungsbedarf) = RER × Aktivitätsfaktor\n\n" +
                        "Typische Faktoren:\n" +
                        "  1,0 – Inaktiv / krank\n" +
                        "  1,2 – Kastriert, wenig Bewegung\n" +
                        "  1,6 – Normal aktiver Hund (Standard)\n" +
                        "  2,0 – Aktiver Hund (viel Sport)\n" +
                        "  3,0 – Arbeitshund / intensive Nutzung\n\n" +
                        "Quelle: NRC 2006 (National Research Council)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
