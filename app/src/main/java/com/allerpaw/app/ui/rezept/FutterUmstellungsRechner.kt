package com.allerpaw.app.ui.rezept

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allerpaw.app.data.local.entity.RezeptEntity
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// Datenmodell
// ─────────────────────────────────────────────────────────────────────────────

data class UmstellungsTag(
    val tag: Int,              // 1-basiert
    val rezept1AnteilProzent: Int,
    val rezept2AnteilProzent: Int,
    val rezept1Gramm: Double,
    val rezept2Gramm: Double,
    val gesamtGramm: Double
)

enum class UmstellungsGeschwindigkeit(
    val label: String,
    val beschreibung: String,
    val tage: Int,
    val emoji: String
) {
    SCHNELL(
        "Schnell",
        "Gesunder Magen, problemlose Umstellung",
        5,
        "🐕"
    ),
    NORMAL(
        "Normal",
        "Standard-Empfehlung für die meisten Hunde",
        7,
        "🐶"
    ),
    SANFT(
        "Sanft",
        "Empfindlicher Magen oder erste BARF-Umstellung",
        10,
        "🐩"
    ),
    SEHR_SANFT(
        "Sehr sanft",
        "Allergiker, Magenprobleme oder Welpen",
        14,
        "🐾"
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Berechnungslogik
// ─────────────────────────────────────────────────────────────────────────────

fun berechneUmstellungsplan(
    tagesrationGramm: Double,
    gesamtTage: Int
): List<UmstellungsTag> {
    return (1..gesamtTage).map { tag ->
        // Linearer Übergang: Tag 1 = 100% alt / 0% neu, letzter Tag = 0% alt / 100% neu
        val fortschritt = (tag - 1).toDouble() / (gesamtTage - 1).toDouble()
        val neu   = (fortschritt * 100).roundToInt().coerceIn(0, 100)
        val alt   = 100 - neu
        val grammNeu = tagesrationGramm * neu / 100.0
        val grammAlt = tagesrationGramm * alt / 100.0

        UmstellungsTag(
            tag                      = tag,
            rezept1AnteilProzent     = alt,
            rezept2AnteilProzent     = neu,
            rezept1Gramm             = grammAlt,
            rezept2Gramm             = grammNeu,
            gesamtGramm              = tagesrationGramm
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UI
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FutterUmstellungsCard(
    alleRezepte: List<RezeptEntity>,
    tagesrationGramm: Double
) {
    var expanded          by remember { mutableStateOf(false) }
    var rezept1Id         by remember { mutableStateOf<Long?>(null) }
    var rezept2Id         by remember { mutableStateOf<Long?>(null) }
    var geschwindigkeit   by remember { mutableStateOf(UmstellungsGeschwindigkeit.NORMAL) }
    var customTage        by remember { mutableStateOf("") }
    var showPlan          by remember { mutableStateOf(false) }

    val rezept1 = alleRezepte.find { it.id == rezept1Id }
    val rezept2 = alleRezepte.find { it.id == rezept2Id }

    val gesamtTage = customTage.toIntOrNull()
        ?.coerceIn(5, 14) ?: geschwindigkeit.tage

    val plan = if (showPlan && rezept1 != null && rezept2 != null && tagesrationGramm > 0) {
        berechneUmstellungsplan(tagesrationGramm, gesamtTage)
    } else emptyList()

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔄", fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Futterumstellungsrechner",
                        style = MaterialTheme.typography.titleSmall)
                    Text("Sanfter Wechsel von Rezept A → B",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Ausklappen"
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // ── Rezept A ─────────────────────────────────────────
                    Text("Aktuelles Futter (Rezept A)",
                        style = MaterialTheme.typography.labelMedium)
                    RezeptDropdown(
                        label       = "Rezept A (von)",
                        alleRezepte = alleRezepte,
                        selected    = rezept1Id,
                        exclude     = rezept2Id,
                        onSelect    = { rezept1Id = it; showPlan = false }
                    )

                    // ── Rezept B ─────────────────────────────────────────
                    Text("Neues Futter (Rezept B)",
                        style = MaterialTheme.typography.labelMedium)
                    RezeptDropdown(
                        label       = "Rezept B (zu)",
                        alleRezepte = alleRezepte,
                        selected    = rezept2Id,
                        exclude     = rezept1Id,
                        onSelect    = { rezept2Id = it; showPlan = false }
                    )

                    // ── Geschwindigkeit ───────────────────────────────────
                    Text("Umstellungsgeschwindigkeit",
                        style = MaterialTheme.typography.labelMedium)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        UmstellungsGeschwindigkeit.entries.forEach { g ->
                            GeschwindigkeitsChip(
                                g         = g,
                                selected  = geschwindigkeit == g && customTage.isBlank(),
                                onClick   = {
                                    geschwindigkeit = g
                                    customTage      = ""
                                    showPlan        = false
                                }
                            )
                        }
                    }

                    // ── Eigene Taganzahl ──────────────────────────────────
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value         = customTage,
                            onValueChange = {
                                customTage = it.filter { c -> c.isDigit() }
                                showPlan   = false
                            },
                            label         = { Text("Eigene Tageanzahl (5–14)") },
                            modifier      = Modifier.weight(1f),
                            singleLine    = true,
                            suffix        = { Text("Tage") }
                        )
                        if (customTage.isNotBlank()) {
                            val tage = customTage.toIntOrNull() ?: 0
                            val farbe = when {
                                tage < 5  -> MaterialTheme.colorScheme.error
                                tage > 14 -> MaterialTheme.colorScheme.error
                                else      -> Color(0xFF4CAF50)
                            }
                            Text(
                                when {
                                    tage < 5  -> "Min. 5"
                                    tage > 14 -> "Max. 14"
                                    else      -> "✓"
                                },
                                color = farbe,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // ── Tagesration Info ──────────────────────────────────
                    if (tagesrationGramm > 0) {
                        Text(
                            "Tagesration: ${tagesrationGramm.roundToInt()} g " +
                            "(aus aktivem Rezept)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        OutlinedCard {
                            Row(
                                Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Wähle zuerst einen Hund im Rechner-Tab " +
                                    "um die Tagesration zu berechnen.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    // ── Plan generieren ───────────────────────────────────
                    Button(
                        onClick  = { showPlan = true },
                        enabled  = rezept1 != null && rezept2 != null && tagesrationGramm > 0,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("$gesamtTage-Tage-Plan erstellen")
                    }

                    // ── Plan Anzeige ──────────────────────────────────────
                    if (plan.isNotEmpty() && rezept1 != null && rezept2 != null) {
                        UmstellungsPlanTabelle(
                            plan    = plan,
                            name1   = rezept1.name.ifBlank { "Rezept A" },
                            name2   = rezept2.name.ifBlank { "Rezept B" }
                        )
                    }
                }
            }
        }
    }
}

// ── Sub-Composables ───────────────────────────────────────────────────────────

@Composable
private fun GeschwindigkeitsChip(
    g: UmstellungsGeschwindigkeit,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth(),
        colors   = if (selected)
            CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer)
        else CardDefaults.outlinedCardColors(),
        onClick  = onClick
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(g.emoji, fontSize = 20.sp)
            Column(Modifier.weight(1f)) {
                Text(
                    "${g.label} · ${g.tage} Tage",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    g.beschreibung,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            if (selected) {
                Icon(Icons.Default.CheckCircle, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RezeptDropdown(
    label: String,
    alleRezepte: List<RezeptEntity>,
    selected: Long?,
    exclude: Long?,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val verfuegbar = alleRezepte.filter { it.id != exclude }
    val selectedName = alleRezepte.find { it.id == selected }?.name?.ifBlank { "Rezept #$selected" }

    ExposedDropdownMenuBox(
        expanded         = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value         = selectedName ?: "Rezept wählen…",
            onValueChange = {},
            readOnly      = true,
            label         = { Text(label) },
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier      = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (verfuegbar.isEmpty()) {
                DropdownMenuItem(
                    text    = { Text("Keine Rezepte vorhanden") },
                    onClick = {},
                    enabled = false
                )
            } else {
                verfuegbar.forEach { rezept ->
                    DropdownMenuItem(
                        text    = {
                            Text(rezept.name.ifBlank { "Rezept #${rezept.id}" })
                        },
                        onClick = {
                            onSelect(rezept.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UmstellungsPlanTabelle(
    plan: List<UmstellungsTag>,
    name1: String,
    name2: String
) {
    val n1 = if (name1.length > 12) name1.take(10) + "…" else name1
    val n2 = if (name2.length > 12) name2.take(10) + "…" else name2

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

        Text("Umstellungsplan",
            style = MaterialTheme.typography.titleSmall)

        // Legende
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(12.dp).background(
                    Color(0xFF1565C0), MaterialTheme.shapes.extraSmall))
                Text(n1, style = MaterialTheme.typography.labelSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(12.dp).background(
                    Color(0xFF2E7D32), MaterialTheme.shapes.extraSmall))
                Text(n2, style = MaterialTheme.typography.labelSmall)
            }
        }

        // Tages-Karten
        plan.forEach { tag ->
            UmstellungsTagRow(
                tag   = tag,
                name1 = n1,
                name2 = n2,
                istErster = tag.tag == 1,
                istLetzter = tag.tag == plan.size
            )
        }

        // Zusammenfassung
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp)) {
                Text("💡 Tipps zur Umstellung",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                listOf(
                    "Beobachte Kot-Konsistenz und Verträglichkeit täglich",
                    "Bei Durchfall: Tempo reduzieren oder 2 Tage Pause einlegen",
                    "Frisches Wasser immer griffbereit",
                    "Symptome im Tagebuch dokumentieren"
                ).forEach { tipp ->
                    Text("• $tipp",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun UmstellungsTagRow(
    tag: UmstellungsTag,
    name1: String,
    name2: String,
    istErster: Boolean,
    istLetzter: Boolean
) {
    val tagLabel = when {
        istErster  -> "Tag ${tag.tag} · Start"
        istLetzter -> "Tag ${tag.tag} · Fertig! 🎉"
        else       -> "Tag ${tag.tag}"
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors   = when {
            istLetzter -> CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer)
            istErster  -> CardDefaults.elevatedCardColors()
            else       -> CardDefaults.elevatedCardColors()
        }
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {

            Text(tagLabel, style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold)

            // Fortschrittsbalken
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(Color(0xFF1565C0), MaterialTheme.shapes.small)
            ) {
                if (tag.rezept2AnteilProzent > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(tag.rezept2AnteilProzent / 100f)
                            .background(Color(0xFF2E7D32), MaterialTheme.shapes.small)
                    )
                }
            }

            // Gramm-Angaben
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Rezept A
                Column(horizontalAlignment = Alignment.Start) {
                    Text(name1,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF1565C0))
                    Text(
                        "${tag.rezept1AnteilProzent}% · " +
                        "${tag.rezept1Gramm.roundToInt()} g",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                // Rezept B
                Column(horizontalAlignment = Alignment.End) {
                    Text(name2,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2E7D32))
                    Text(
                        "${tag.rezept2AnteilProzent}% · " +
                        "${tag.rezept2Gramm.roundToInt()} g",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
