package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ArafatDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF001F25),
    primaryContainer = Color(0xFF004F59),
    onPrimaryContainer = NeonCyanBright,
    secondary = ElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF002F6C),
    onSecondaryContainer = ElectricBlueGlow,
    tertiary = NeonPurple,
    onTertiary = Color.White,
    background = CyberNavyDark,
    onBackground = TextPrimaryDark,
    surface = CyberNavySurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CyberNavySurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = CyberBorder,
    outlineVariant = Color(0xFF16253D),
    error = NeonRed,
    onError = Color.White
)

val ArafatLightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001B3F),
    secondary = Color(0xFF00838F),
    onSecondary = Color.White,
    background = Color(0xFFF2F6FC),
    onBackground = Color(0xFF0E1A29),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E1A29),
    surfaceVariant = Color(0xFFDEE5F2),
    onSurfaceVariant = Color(0xFF404D60),
    outline = Color(0xFFB0BDD0)
)

@Composable
fun ArafatAiTheme(
    darkTheme: Boolean = true, // Default to futuristic dark navy cyber theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ArafatDarkColorScheme else ArafatLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward-compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    ArafatAiTheme(darkTheme = darkTheme, content = content)
}
