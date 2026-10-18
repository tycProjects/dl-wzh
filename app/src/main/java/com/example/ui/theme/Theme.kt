package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalZMapsAccent = staticCompositionLocalOf { ZMapsAccent.GREEN }

@Composable
fun ZMapsTheme(
    accent: ZMapsAccent = ZMapsAccent.GREEN,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = accent.color,
        onPrimary = accent.onColor,
        primaryContainer = accent.color.copy(alpha = 0.2f),
        onPrimaryContainer = accent.color,
        secondary = accent.color,
        onSecondary = accent.onColor,
        secondaryContainer = CyberSurfaceElevated,
        onSecondaryContainer = TextPrimary,
        tertiary = AccentCyan,
        onTertiary = CyberBackground,
        background = CyberBackground,
        onBackground = TextPrimary,
        surface = CyberSurface,
        onSurface = TextPrimary,
        surfaceVariant = CyberSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        outline = CyberBorder,
        outlineVariant = CyberBorder.copy(alpha = 0.5f),
        surfaceContainer = CyberSurface,
        surfaceContainerHigh = CyberSurfaceVariant,
        surfaceContainerHighest = CyberSurfaceElevated
    )

    CompositionLocalProvider(LocalZMapsAccent provides accent) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
