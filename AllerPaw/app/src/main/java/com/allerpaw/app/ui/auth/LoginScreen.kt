package com.allerpaw.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Pets,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text  = "AllerPaw",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text      = "Allergiemanagement für deinen Hund",
            style     = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(top = 8.dp, bottom = 48.dp)
        )
        Button(
            onClick  = {
                // TODO: launch Credential Manager → onLoginSuccess()
                viewModel.signInWithGoogle("demo-token")
                onLoginSuccess()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Mit Google anmelden")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick  = {
                // TODO: e-mail / password flow
                viewModel.signInWithGoogle("demo-token")
                onLoginSuccess()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Mit E-Mail anmelden")
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text  = "Deine Daten werden Ende-zu-Ende verschlüsselt gespeichert.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}
