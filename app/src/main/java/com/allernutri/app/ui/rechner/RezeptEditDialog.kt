package com.allernutri.app.ui.rechner

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.allernutri.app.R
import com.allernutri.app.data.local.entity.HundEntity
import com.allernutri.app.data.local.entity.RezeptEntity

/**
 * Dialog zum Erstellen und Bearbeiten eines Rezepts.
 */
@Composable
fun RezeptEditDialog(
    rezept:    RezeptEntity,
    hundeList: List<HundEntity>,
    onDismiss: () -> Unit,
    onSave:    (RezeptEntity) -> Unit
) {
    var name      by remember { mutableStateOf(rezept.name) }
    var kategorie by remember { mutableStateOf(rezept.kategorie) }
    var notizen   by remember { mutableStateOf(rezept.notizen) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = {
            Text(if (rezept.id == 0L) stringResource(R.string.rezept_neu)
                 else stringResource(R.string.rezept_bearbeiten))
        },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text(stringResource(R.string.label_name)) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true
                )
                OutlinedTextField(
                    value         = kategorie,
                    onValueChange = { kategorie = it },
                    label         = { Text(stringResource(R.string.label_kategorie)) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true
                )
                OutlinedTextField(
                    value         = notizen,
                    onValueChange = { notizen = it },
                    label         = { Text(stringResource(R.string.label_notizen)) },
                    modifier      = Modifier.fillMaxWidth(),
                    minLines      = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(rezept.copy(
                        name      = name.trim(),
                        kategorie = kategorie.trim(),
                        notizen   = notizen.trim()
                    ))
                }
            ) { Text(stringResource(R.string.btn_speichern)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_abbrechen))
            }
        }
    )
}
