package com.logalyze.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF050B16)
val NavySurface = Color(0xFF0A1426)
val NavyCard = Color(0xFF0D1B33)
val NavyLine = Color(0xFF17305A)
val TextMuted = Color(0xFF7F9BB8)
val Success = Color(0xFF1FBF8A)
val ErrorRed = Color(0xFFEF4444)
val Warning = Color(0xFFF59E0B)

enum class ThemeMode { System, Dark, Light }

enum class Accent(val dark: Color, val darkAlt: Color, val light: Color, val lightAlt: Color) {
    Cyan(Color(0xFF19E3FF), Color(0xFF2D7BFF), Color(0xFF0891B2), Color(0xFF2563EB)),
    Blue(Color(0xFF5B9BFF), Color(0xFF3A6BFF), Color(0xFF2563EB), Color(0xFF1D4ED8)),
    Green(Color(0xFF2BE3A0), Color(0xFF1FB57A), Color(0xFF059669), Color(0xFF047857)),
    Purple(Color(0xFFB28DFF), Color(0xFF7C4DFF), Color(0xFF7C3AED), Color(0xFF6D28D9)),
    Orange(Color(0xFFFFA14D), Color(0xFFFF7A1A), Color(0xFFEA580C), Color(0xFFC2410C))
}

private fun schemeFor(dark: Boolean, accent: Accent): ColorScheme =
    if (dark) {
        darkColorScheme(
            primary = accent.dark,
            onPrimary = Navy,
            secondary = accent.darkAlt,
            background = Navy,
            onBackground = Color(0xFFE6F4FF),
            surface = NavySurface,
            onSurface = Color(0xFFE6F4FF),
            surfaceVariant = NavyCard,
            onSurfaceVariant = TextMuted,
            outline = NavyLine,
            error = ErrorRed
        )
    } else {
        lightColorScheme(
            primary = accent.light,
            onPrimary = Color.White,
            secondary = accent.lightAlt,
            background = Color(0xFFF3F7FC),
            onBackground = Color(0xFF0A1426),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0A1426),
            surfaceVariant = Color(0xFFE6EEF8),
            onSurfaceVariant = Color(0xFF4A6280),
            outline = Color(0xFFC5D3E6),
            error = ErrorRed
        )
    }

@Composable
fun resolveDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Dark -> true
    ThemeMode.Light -> false
}

@Composable
fun LogalyzeTheme(
    mode: ThemeMode = ThemeMode.Dark,
    accent: Accent = Accent.Cyan,
    content: @Composable () -> Unit
) {
    MaterialTheme(colorScheme = schemeFor(resolveDark(mode), accent), content = content)
}
