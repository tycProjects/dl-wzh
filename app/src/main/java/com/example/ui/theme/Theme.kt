package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val TouchlineDarkColorScheme = darkColorScheme(
    primary = TurfEmerald,
    onPrimary = PitchBlack,
    primaryContainer = TurfEmeraldMuted,
    onPrimaryContainer = TurfEmeraldBright,
    secondary = ElectricCyan,
    onSecondary = PitchBlack,
    secondaryContainer = CyanMuted,
    onSecondaryContainer = ElectricCyan,
    tertiary = BroadcastAmber,
    onTertiary = PitchBlack,
    tertiaryContainer = AmberMuted,
    onTertiaryContainer = BroadcastAmber,
    error = CrimsonRed,
    onError = TextPrimary,
    errorContainer = CrimsonMuted,
    onErrorContainer = CrimsonRed,
    background = PitchBlack,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = SlateCard,
    surfaceContainerHigh = SlateElevated,
    outline = HairlineBorder,
    outlineVariant = HairlineBorder,
)

val TouchlineShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun TouchlineTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = TouchlineDarkColorScheme,
        typography = TouchlineTypography,
        shapes = TouchlineShapes,
        content = content,
    )
}
