package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val NexoraDarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = SoftWhite,
    primaryContainer = NexoraClay,
    onPrimaryContainer = ElectricViolet,
    secondary = MetallicGold,
    onSecondary = NexoraBlack,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = BrightGold,
    tertiary = ElectricViolet,
    onTertiary = SoftWhite,
    background = NexoraBlack,
    onBackground = SoftWhite,
    surface = NexoraCharcoal,
    onSurface = SoftWhite,
    surfaceVariant = NexoraSurface,
    onSurfaceVariant = CoolGrey,
    outline = VioletBorder,
    outlineVariant = DividerDark,
    error = ErrorRed,
    onError = SoftWhite,
    errorContainer = ErrorContainer,
    onErrorContainer = ErrorRed
)

val NexoraShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NexoraHubTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NexoraDarkColorScheme,
        typography = NexoraTypography,
        shapes = NexoraShapes,
        content = content
    )
}
