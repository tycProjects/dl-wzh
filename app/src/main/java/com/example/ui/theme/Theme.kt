package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.DarkModePreference
import com.example.model.ThemePalette

@Composable
fun SharpCalculatorTheme(
    palette: ThemePalette = ThemePalette.CYAN_OLED,
    darkModePref: DarkModePreference = DarkModePreference.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (darkModePref) {
        DarkModePreference.SYSTEM -> isSystemInDarkTheme()
        DarkModePreference.DARK -> true
        DarkModePreference.LIGHT -> false
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = Color.Black,
            primaryContainer = palette.darkSurfaceVariant,
            onPrimaryContainer = palette.primary,
            secondary = palette.secondary,
            onSecondary = Color.Black,
            secondaryContainer = palette.darkOperatorKey,
            onSecondaryContainer = Color.White,
            tertiary = palette.accent,
            onTertiary = Color.Black,
            background = palette.darkBackground,
            onBackground = Color(0xFFF0F6FC),
            surface = palette.darkSurface,
            onSurface = Color(0xFFE6EDF3),
            surfaceVariant = palette.darkSurfaceVariant,
            onSurfaceVariant = Color(0xFF8B949E),
            outline = Color(0xFF30363D)
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = Color.White,
            primaryContainer = palette.lightOperatorKey,
            onPrimaryContainer = Color(0xFF0F172A),
            secondary = palette.secondary,
            onSecondary = Color.White,
            secondaryContainer = palette.lightSurfaceVariant,
            onSecondaryContainer = Color(0xFF1E293B),
            tertiary = palette.accent,
            onTertiary = Color.White,
            background = palette.lightBackground,
            onBackground = Color(0xFF0F172A),
            surface = palette.lightSurface,
            onSurface = Color(0xFF1E293B),
            surfaceVariant = palette.lightSurfaceVariant,
            onSurfaceVariant = Color(0xFF64748B),
            outline = Color(0xFFCBD5E1)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
