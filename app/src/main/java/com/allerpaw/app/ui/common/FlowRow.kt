package com.allerpaw.app.ui.common

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Thin wrapper um Compose FlowRow aus androidx.compose.foundation.layout.
 * Fällt auf manuelle Implementation zurück wenn nicht verfügbar.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalGap: Dp = 8.dp,
    verticalGap: Dp = 4.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier             = modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(horizontalGap),
        verticalArrangement  = androidx.compose.foundation.layout.Arrangement.spacedBy(verticalGap),
        content              = content
    )
}
