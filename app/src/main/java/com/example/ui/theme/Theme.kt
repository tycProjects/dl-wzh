package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZonexDarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = Color.White,
    primaryContainer = NeonPurpleGlow,
    onPrimaryContainer = Color.White,
    secondary = ElectricCyan,
    onSecondary = CyberBlack,
    secondaryContainer = CyberSurfaceVariant,
    onSecondaryContainer = ElectricCyan,
    tertiary = ElectricBlue,
    onTertiary = Color.White,
    background = CyberBlack,
    onBackground = TextPrimary,
    surface = CyberDark,
    onSurface = TextPrimary,
    surfaceVariant = CyberCard,
    onSurfaceVariant = TextSecondary,
    outline = CyberCardBorder,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun ZonexVoiceTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ZonexDarkColorScheme,
        typography = Typography,
        content = content
    )
}
