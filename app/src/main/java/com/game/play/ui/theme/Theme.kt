package com.game.play.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue = Color(0xFF2F6BFF)

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    secondary = Color(0xFFFFB020),
    secondaryContainer = Color(0xFFDCE8FF),
    onSecondaryContainer = Color(0xFF17324D),
    background = Color(0xFFF4F9FF),
    onBackground = Color(0xFF17324D),
    surface = Color.White,
    onSurface = Color(0xFF17324D),
    surfaceVariant = Color(0xFFE6EFFC),
    onSurfaceVariant = Color(0xFF5B6B82),
    outline = Color(0xFFD5E3F5)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FA2FF),
    onPrimary = Color(0xFF10284A),
    secondary = Color(0xFFFFC45C),
    secondaryContainer = Color(0xFF30486A),
    onSecondaryContainer = Color(0xFFE8F1FF),
    background = Color(0xFF172536),
    onBackground = Color(0xFFE8F1FF),
    surface = Color(0xFF22354B),
    onSurface = Color(0xFFE8F1FF),
    surfaceVariant = Color(0xFF2C415A),
    onSurfaceVariant = Color(0xFFB3C3D9),
    outline = Color(0xFF405873)
)

@Composable
fun PorGameTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
