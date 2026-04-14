package com.arkcompanion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun ArkCompanionTheme(content: @Composable () -> Unit) {
    // We enforce Dark Theme only to align with One UI 8.5 dark aesthetic constraints.
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
