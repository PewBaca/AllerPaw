package com.allerpaw.app.ui.rechner

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allerpaw.app.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StandortPicker(
    onStandortGewaehlt: (lat: Double, lon: Double, stadtName: String) -> Unit,
    modifier: Modifier = Modifier,
    vm: StandortPickerViewModel = hiltViewModel()
) {
    val state             = by vm.state.collectAsState()
    var stadtInput        by remember { mutableStateOf("") }
    var latInput          by remember { mutableStateOf("") }
    var lonInput          by remember { mutableStateOf("") }
    var modus             by remember { mutableStateOf(StandortModus.STADT) }
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                StandortModus.STADT       to stringResource(R.string.label_stadt_ort),
                StandortModus.KOORDINATEN to "GPS-Koordinaten",
                StandortModus.AKTUELL     to "Aktuell"
            ).forEach { (m, l) ->
                FilterChip(selected = modus == m, onClick = { modus = m }, label = { Text(l) })
            }
        }

        when (modus) {
            StandortModus.STADT -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value         = stadtInput,
                        onValueChange = { stadtInput = it },
                        label         = { Text(stringResource(R.string.label_stadt_ort)) },
                        placeholder   = { Text(stringResource(R.string.placeholder_stadt)) },
                        singleLine    = true,
                        modifier      = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick  = { vm.geocodeStadt(stadtInput, onStandortGewaehlt) },
                        enabled  = stadtInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.Search, stringResource(R.string.cd_suchen))
                    }
                }
                state.fehler?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            StandortModus.KOORDINATEN -> {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value         = latInput,
                        onValueChange = { latInput = it },
                        label         = { Text(stringResource(R.string.label_breitengrad)) },
                        singleLine    = true,
                        modifier      = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value         = lonInput,
                        onValueChange = { lonInput = it },
                        label         = { Text(stringResource(R.string.label_laengengrad)) },
                        singleLine    = true,
                        modifier      = Modifier.weight(1f)
                    )
                }
                Button(
                    onClick  = {
                        val lat = latInput.toDoubleOrNull()
                        val lon = lonInput.toDoubleOrNull()
                        if (lat != null && lon != null) {
                            onStandortGewaehlt(lat, lon, "$lat, $lon")
                        }
                    },
                    enabled  = latInput.toDoubleOrNull() != null && lonInput.toDoubleOrNull() != null,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.btn_koordinaten_uebernehmen)) }
            }

            StandortModus.AKTUELL -> {
                if (locationPermission.status.isGranted) {
                    LaunchedEffect(Unit) { vm.holeAktuellenStandort(onStandortGewaehlt) }
                    if (state.isLoading) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(Modifier.size(20.dp))
                            Text("Standort wird ermittelt…",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    Button(
                        onClick  = { locationPermission.launchPermissionRequest() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LocationOn, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_standort_aktuell))
                    }
                }
            }
        }

        AnimatedVisibility(visible = state.isLoading && modus == StandortModus.STADT) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

enum class StandortModus { STADT, KOORDINATEN, AKTUELL }
