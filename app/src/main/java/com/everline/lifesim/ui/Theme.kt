package com.everline.lifesim.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Midnight = Color(0xFF09172C)
val DeepBlue = Color(0xFF102745)
val Slate = Color(0xFF203757)
val Gold = Color(0xFFE0BF79)
val Cream = Color(0xFFF8F1E5)
val Mist = Color(0xFFC8D4E3)
val Success = Color(0xFF89C6AD)

private val colors = darkColorScheme(
    primary = Gold, onPrimary = Midnight,
    secondary = Mist, onSecondary = Midnight,
    background = Midnight, onBackground = Cream,
    surface = DeepBlue, onSurface = Cream,
    surfaceVariant = Slate, onSurfaceVariant = Mist,
    outline = Color(0xFF57708D), error = Color(0xFFFF9D9D)
)

@Composable fun EverlineTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}
