package com.aetherisland.design.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AetherColors = darkColorScheme(
    primary = Color(0xFFB9C7FF),
    secondary = Color(0xFFB8D9D0),
    background = Color(0xFF090A0D),
    surface = Color(0xFF171A21)
)

@Composable
fun AetherTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AetherColors, content = content)
}
