package com.allerpaw.app.ui.auth

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val ctx = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        // Logo / Icon
        Icon(
            imageVector       = Icons.Default.Pets,
            contentDescription = null,
            modifier          = Modifier.size(80.dp),
            tint              = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text("AllerPaw",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary)
        Text("Allergiemanagement für deinen Hund",
            style     = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.outline,
            modifier  = Modifier.padding(bottom = 40.dp, top = 8.dp))

        // Fehler-Banner
        uiState.fehler?.let { fehler ->
            Card(
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Text(fehler, Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }

        // Google Sign-In
        Button(
            onClick  = {
                viewModel.signInWithGoogle(ctx as Activity, onLoginSuccess)
            },
            enabled  = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Mit Google anmelden")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Demo-Login (für Entwicklung / Tests)
        OutlinedButton(
            onClick  = { viewModel.signInDemo(onLoginSuccess) },
            enabled  = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Demo-Modus (ohne Konto)")
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Deine Daten werden lokal gespeichert.\nGoogle-Login nur für optionalen Backup.",
            style     = MaterialTheme.typography.labelSmall,
            color     = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}
