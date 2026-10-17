package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LiquidGlassColorScheme = darkColorScheme(
    primary = LiquidCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF083344),
    onPrimaryContainer = LiquidCyanLight,
    secondary = LiquidViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E1065),
    onSecondaryContainer = LiquidVioletLight,
    tertiary = LiquidMagenta,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF500724),
    onTertiaryContainer = LiquidMagentaLight,
    background = LiquidDarkBackground,
    onBackground = TextPrimary,
    surface = LiquidDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = LiquidDarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = LiquidGlassBorder,
    outlineVariant = Color(0x2038BDF8)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Liquid Glass aesthetic relies on custom tuned iridescent dark styling
    MaterialTheme(
        colorScheme = LiquidGlassColorScheme,
        typography = Typography,
        content = content
    )
}
