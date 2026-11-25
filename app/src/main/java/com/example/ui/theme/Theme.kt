package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MayaDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ObsidianDeep,
    primaryContainer = Color(0xFF0E3A5C),
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = MayaRose,
    onSecondary = ObsidianDeep,
    secondaryContainer = Color(0xFF4C1D42),
    onSecondaryContainer = Color(0xFFFBCFE8),
    tertiary = NeuralViolet,
    onTertiary = ObsidianDeep,
    tertiaryContainer = Color(0xFF2E1E59),
    onTertiaryContainer = Color(0xFFDDD6FE),
    background = ObsidianDeep,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = GlassBase,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianDeep,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = GlassBase,
    surfaceContainerHigh = GlassElevated,
    outline = GlassBorder,
    outlineVariant = Color(0xFF1E293B),
    error = StatusCrimson,
    onError = Color.White,
    errorContainer = Color(0xFF4C0D1D),
    onErrorContainer = Color(0xFFFECDD3)
)

val MayaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MayaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MayaDarkColorScheme,
        typography = MayaTypography,
        shapes = MayaShapes,
        content = content
    )
}
