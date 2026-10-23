package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.repository.ThemeColor

fun getJnmfColorScheme(themeColor: ThemeColor): ColorScheme {
    return when (themeColor) {
        ThemeColor.NEON_GREEN -> darkColorScheme(
            primary = NeonGreenPrimary,
            onPrimary = Color.Black,
            primaryContainer = NeonGreenSecondary,
            onPrimaryContainer = Color.Black,
            secondary = NeonGreenSecondary,
            onSecondary = Color.Black,
            background = JnmfBackgroundDark,
            onBackground = JnmfTextPrimary,
            surface = JnmfSurfaceDark,
            onSurface = JnmfTextPrimary,
            surfaceVariant = NeonGreenSurface,
            onSurfaceVariant = JnmfTextSecondary,
            outline = NeonGreenPrimary.copy(alpha = 0.5f)
        )
        ThemeColor.ELECTRIC_BLUE -> darkColorScheme(
            primary = ElectricBluePrimary,
            onPrimary = Color.Black,
            primaryContainer = ElectricBlueSecondary,
            onPrimaryContainer = Color.White,
            secondary = ElectricBlueSecondary,
            onSecondary = Color.White,
            background = JnmfBackgroundDark,
            onBackground = JnmfTextPrimary,
            surface = JnmfSurfaceDark,
            onSurface = JnmfTextPrimary,
            surfaceVariant = ElectricBlueSurface,
            onSurfaceVariant = JnmfTextSecondary,
            outline = ElectricBluePrimary.copy(alpha = 0.5f)
        )
        ThemeColor.CRIMSON_RED -> darkColorScheme(
            primary = CrimsonRedPrimary,
            onPrimary = Color.White,
            primaryContainer = CrimsonRedSecondary,
            onPrimaryContainer = Color.White,
            secondary = CrimsonRedSecondary,
            onSecondary = Color.White,
            background = JnmfBackgroundDark,
            onBackground = JnmfTextPrimary,
            surface = JnmfSurfaceDark,
            onSurface = JnmfTextPrimary,
            surfaceVariant = CrimsonRedSurface,
            onSurfaceVariant = JnmfTextSecondary,
            outline = CrimsonRedPrimary.copy(alpha = 0.5f)
        )
        ThemeColor.MODERN_WHITE -> darkColorScheme(
            primary = ModernWhitePrimary,
            onPrimary = Color.Black,
            primaryContainer = ModernWhiteSecondary,
            onPrimaryContainer = Color.Black,
            secondary = ModernWhiteSecondary,
            onSecondary = Color.Black,
            background = JnmfBackgroundDark,
            onBackground = JnmfTextPrimary,
            surface = JnmfSurfaceDark,
            onSurface = JnmfTextPrimary,
            surfaceVariant = ModernWhiteSurface,
            onSurfaceVariant = JnmfTextSecondary,
            outline = Color.White.copy(alpha = 0.4f)
        )
        ThemeColor.VIBRANT_CYAN -> darkColorScheme(
            primary = VibrantCyanPrimary,
            onPrimary = Color.Black,
            primaryContainer = VibrantCyanSecondary,
            onPrimaryContainer = Color.Black,
            secondary = VibrantCyanSecondary,
            onSecondary = Color.Black,
            background = JnmfBackgroundDark,
            onBackground = JnmfTextPrimary,
            surface = JnmfSurfaceDark,
            onSurface = JnmfTextPrimary,
            surfaceVariant = VibrantCyanSurface,
            onSurfaceVariant = JnmfTextSecondary,
            outline = VibrantCyanPrimary.copy(alpha = 0.5f)
        )
        ThemeColor.DEEP_VIOLET -> darkColorScheme(
            primary = DeepVioletPrimary,
            onPrimary = Color.White,
            primaryContainer = DeepVioletSecondary,
            onPrimaryContainer = Color.White,
            secondary = DeepVioletSecondary,
            onSecondary = Color.White,
            background = JnmfBackgroundDark,
            onBackground = JnmfTextPrimary,
            surface = JnmfSurfaceDark,
            onSurface = JnmfTextPrimary,
            surfaceVariant = DeepVioletSurface,
            onSurfaceVariant = JnmfTextSecondary,
            outline = DeepVioletPrimary.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun JnmfTheme(
    themeColor: ThemeColor = ThemeColor.NEON_GREEN,
    content: @Composable () -> Unit
) {
    val colorScheme = getJnmfColorScheme(themeColor)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
