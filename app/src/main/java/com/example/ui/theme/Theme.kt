package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = NexoraBlue,
    onPrimary = Color.White,
    primaryContainer = NexoraBlueContainer,
    onPrimaryContainer = NexoraOnBlueContainer,
    secondary = NexoraViolet,
    onSecondary = Color.White,
    secondaryContainer = NexoraVioletContainer,
    onSecondaryContainer = NexoraOnVioletContainer,
    tertiary = NexoraSuccess,
    onTertiary = Color.White,
    tertiaryContainer = NexoraSuccessContainer,
    onTertiaryContainer = Color(0xFF14532D),
    error = NexoraDanger,
    onError = Color.White,
    errorContainer = NexoraDangerContainer,
    onErrorContainer = Color(0xFF7F1D1D),
    background = NexoraBackgroundLight,
    onBackground = NexoraTextLight,
    surface = NexoraSurfaceLight,
    onSurface = NexoraTextLight,
    surfaceVariant = NexoraSurfaceVariantLight,
    onSurfaceVariant = NexoraTextMutedLight,
    outline = NexoraOutlineLight,
    outlineVariant = Color(0xFFCBD5E1)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFFA78BFA),
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = Color(0xFF4ADE80),
    onTertiary = Color(0xFF052E16),
    tertiaryContainer = Color(0xFF14532D),
    onTertiaryContainer = Color(0xFFDCFCE7),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = NexoraBackgroundDark,
    onBackground = NexoraTextDark,
    surface = NexoraSurfaceDark,
    onSurface = NexoraTextDark,
    surfaceVariant = NexoraSurfaceVariantDark,
    onSurfaceVariant = NexoraTextMutedDark,
    outline = NexoraOutlineDark,
    outlineVariant = Color(0xFF475569)
)

val NexoraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

@Composable
fun NexoraPosTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (useDark) DarkColorScheme else LightColorScheme,
        typography = NexoraTypography,
        shapes = NexoraShapes,
        content = content
    )
}
