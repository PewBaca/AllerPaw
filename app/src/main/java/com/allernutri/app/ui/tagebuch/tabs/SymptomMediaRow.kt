package com.allernutri.app.ui.tagebuch.tabs

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.allernutri.app.R

@Composable
fun SymptomMediaRow(mediaUris: List<String>) {
    if (mediaUris.isEmpty()) return
    val fotos  = mediaUris.filter { !it.endsWith(".mp4") && !it.endsWith(".3gp") }
    val videos = mediaUris.filter { it.endsWith(".mp4") || it.endsWith(".3gp") }
    var zoom by remember { mutableStateOf<String?>(null) }

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        fotos.take(3).forEach { uri ->
            AsyncImage(
                model             = Uri.parse(uri),
                contentDescription = stringResource(R.string.cd_kamera),
                modifier          = Modifier.size(48.dp)
                    .clickable { zoom = uri }
            )
        }
        if (videos.isNotEmpty()) {
            Surface(
                color    = MaterialTheme.colorScheme.secondaryContainer,
                shape    = MaterialTheme.shapes.small,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.tagebuch_video_label),
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    zoom?.let { uri ->
        AlertDialog(
            onDismissRequest = { zoom = null },
            text = {
                AsyncImage(
                    model             = Uri.parse(uri),
                    contentDescription = null,
                    modifier          = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { zoom = null }) { Text(stringResource(R.string.cd_schliessen)) }
            }
        )
    }
}
