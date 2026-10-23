package com.zikotv.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZikoColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF0A0A0A),
    onSurface = Color.White,
    outline = Color.White.copy(alpha = 0.22f)
)

@Composable
fun ZikoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZikoColors,
        content = content
    )
}
