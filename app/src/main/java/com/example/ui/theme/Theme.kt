package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MayaDarkColorScheme = darkColorScheme(
    primary = NeonIndigo,
    onPrimary = TextPrimary,
    primaryContainer = NeonPurple.copy(alpha = 0.25f),
    onPrimaryContainer = TextPrimary,
    secondary = NeonCyan,
    onSecondary = CosmicVoid,
    secondaryContainer = NeonCyan.copy(alpha = 0.16f),
    onSecondaryContainer = NeonCyan,
    tertiary = NeonPink,
    onTertiary = TextPrimary,
    background = CosmicVoid,
    onBackground = TextPrimary,
    surface = CosmicDeep,
    onSurface = TextPrimary,
    surfaceVariant = CosmicSurface,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHigh = CosmicSurfaceElevated,
    outline = CosmicGlassBorder,
    error = CoralError,
    onError = TextPrimary
)

val MayaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MayaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MayaDarkColorScheme,
        typography = Typography,
        shapes = MayaShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MayaTheme(content = content)
}
