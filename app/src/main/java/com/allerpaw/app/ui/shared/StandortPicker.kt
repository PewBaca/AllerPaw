package com.allerpaw.app.ui.shared

import android.Manifest
import android.content.Context
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Wiederverwendbarer Standort-Picker.
 * Drei Modi:
 *   A) Stadtname eingeben → Geocoder → Lat/Lon
 *   B) Lat/Lon manuell eingeben
 *   C) GPS-Position des Geräts
 */
@Composable
fun StandortPicker(
    aktLat: Double,
    aktLon: Double,
    onStandortGewählt: (lat: Double, lon: Double, name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx    = LocalContext.current
    val scope  = rememberCoroutineScope()

    var modus       by remember { mutableStateOf(StandortModus.STADT) }
    var stadtInput  by remember { mutableStateOf("") }
    var latInput    by remember { mutableStateOf(aktLat.toString()) }
    var lonInput    by remember { mutableStateOf(aktLon.toString()) }
    var suche       by remember { mutableStateOf(false) }
    var fehler      by remember { mutableStateOf<String?>(null) }
    var vorschläge  by remember { mutableStateOf<List<GeoVorschlag>>(emptyList()) }

    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            scope.launch { ladeGpsPosition(ctx) { lat, lon, name ->
                latInput = lat.toString()
                lonInput = lon.toString()
                onStandortGewählt(lat, lon, name)
            } }
        } else {
            fehler = "GPS-Berechtigung verweigert"
        }
    }

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            Text("Standort", style = MaterialTheme.typography.titleSmall)

            // Modus-Auswahl
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StandortModus.entries.forEach { m ->
                    FilterChip(
                        selected = modus == m,
                        onClick  = { modus = m; fehler = null; vorschläge = emptyList() },
                        label    = { Text(m.label) }
                    )
                }
            }

            when (modus) {

                // ── Modus A: Stadtname ────────────────────────────────────
                StandortModus.STADT -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value         = stadtInput,
                            onValueChange = {
                                stadtInput = it
                                vorschläge = emptyList()
                                fehler     = null
                            },
                            label         = { Text("Stadt / Ort") },
                            placeholder   = { Text("z.B. München, DE") },
                            modifier      = Modifier.weight(1f),
                            singleLine    = true
                        )
                        IconButton(
                            onClick = {
                                scope.launch {
                                    suche = true
                                    fehler = null
                                    vorschläge = geocodeStadt(ctx, stadtInput)
                                    if (vorschläge.isEmpty()) fehler = "Ort nicht gefunden"
                                    suche = false
                                }
                            },
                            enabled = stadtInput.isNotBlank() && !suche
                        ) {
                            if (suche) CircularProgressIndicator(Modifier.size(20.dp))
                            else Icon(Icons.Default.Search, "Suchen")
                        }
                    }

                    // Vorschläge
                    vorschläge.forEach { vorschlag ->
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick  = {
                                onStandortGewählt(vorschlag.lat, vorschlag.lon, vorschlag.name)
                                latInput   = vorschlag.lat.toString()
                                lonInput   = vorschlag.lon.toString()
                                vorschläge = emptyList()
                            }
                        ) {
                            Row(
                                Modifier.padding(10.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, null,
                                    Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text(vorschlag.name,
                                        style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "%.4f, %.4f".format(vorschlag.lat, vorschlag.lon),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Modus B: Manuelle Koordinaten ─────────────────────────
                StandortModus.KOORDINATEN -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value         = latInput,
                            onValueChange = { latInput = it },
                            label         = { Text("Breitengrad") },
                            placeholder   = { Text("48.1374") },
                            modifier      = Modifier.weight(1f),
                            singleLine    = true
                        )
                        OutlinedTextField(
                            value         = lonInput,
                            onValueChange = { lonInput = it },
                            label         = { Text("Längengrad") },
                            placeholder   = { Text("11.5755") },
                            modifier      = Modifier.weight(1f),
                            singleLine    = true
                        )
                    }
                    Button(
                        onClick = {
                            val lat = latInput.toDoubleOrNull()
                            val lon = lonInput.toDoubleOrNull()
                            if (lat != null && lon != null) {
                                onStandortGewählt(lat, lon, "%.4f, %.4f".format(lat, lon))
                            } else {
                                fehler = "Ungültige Koordinaten"
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Koordinaten übernehmen")
                    }
                }

                // ── Modus C: GPS ──────────────────────────────────────────
                StandortModus.GPS -> {
                    Button(
                        onClick  = {
                            locationPermLauncher.launch(arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ))
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Aktuellen Standort verwenden")
                    }
                }
            }

            // Aktueller Standort
            Text(
                "Aktuell: %.4f, %.4f".format(aktLat, aktLon),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )

            // Fehler
            fehler?.let {
                Text(it, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ── Hilfsfunktionen ───────────────────────────────────────────────────────────

enum class StandortModus(val label: String) {
    STADT("Stadtname"),
    KOORDINATEN("Koordinaten"),
    GPS("GPS")
}

data class GeoVorschlag(val name: String, val lat: Double, val lon: Double)

private suspend fun geocodeStadt(
    ctx: Context,
    query: String
): List<GeoVorschlag> = withContext(Dispatchers.IO) {
    try {
        val geocoder = Geocoder(ctx, Locale.getDefault())
        @Suppress("DEPRECATION")
        val results  = geocoder.getFromLocationName(query, 5) ?: emptyList()
        results.mapNotNull { addr ->
            val name = buildString {
                addr.locality?.let { append(it) }
                addr.adminArea?.let { if (isNotEmpty()) append(", $it") else append(it) }
                addr.countryCode?.let { if (isNotEmpty()) append(", $it") else append(it) }
            }.ifBlank { addr.getAddressLine(0) ?: return@mapNotNull null }
            GeoVorschlag(name, addr.latitude, addr.longitude)
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private suspend fun ladeGpsPosition(
    ctx: Context,
    onResult: (Double, Double, String) -> Unit
) = withContext(Dispatchers.IO) {
    // LocationManager direkt verwenden (kein Fused Location Provider nötig)
    val lm = ctx.getSystemService(android.location.LocationManager::class.java)
    val provider = when {
        lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ->
            android.location.LocationManager.GPS_PROVIDER
        lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) ->
            android.location.LocationManager.NETWORK_PROVIDER
        else -> return@withContext
    }
    @Suppress("MissingPermission")
    val loc = lm.getLastKnownLocation(provider) ?: return@withContext
    val name = try {
        val geo = Geocoder(ctx, Locale.getDefault())
        @Suppress("DEPRECATION")
        geo.getFromLocation(loc.latitude, loc.longitude, 1)
            ?.firstOrNull()?.locality ?: "GPS-Standort"
    } catch (_: Exception) { "GPS-Standort" }
    onResult(loc.latitude, loc.longitude, name)
}
