package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.model.AppLanguage
import com.example.model.AppUiColor
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.getAppStrings

data class JnmfAccentColors(
    val primary: Color,
    val bright: Color,
    val glow: Color,
    val borderGlow: Color,
    val background: Color = JnmfDarkBg,
    val surface: Color = JnmfSurface,
    val surfaceVariant: Color = JnmfSurfaceVariant,
    val surfaceElevated: Color = JnmfSurfaceElevated,
    val textPrimary: Color = JnmfTextPrimary,
    val textSecondary: Color = JnmfTextSecondary,
    val textMuted: Color = JnmfTextMuted,
    val border: Color = JnmfBorder,
    val alertRed: Color = JnmfAlertRed,
    val warningYellow: Color = JnmfWarningYellow
)

val LocalAccentColors = staticCompositionLocalOf {
    JnmfAccentColors(
        primary = JnmfNeonGreen,
        bright = JnmfNeonBright,
        glow = JnmfNeonGlow,
        borderGlow = JnmfBorderGlow
    )
}

object JnmfTheme {
    val colors: JnmfAccentColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAccentColors.current
}

@Composable
fun JnmfTheme(
    uiColor: AppUiColor = AppUiColor.GREEN,
    language: AppLanguage = AppLanguage.ENGLISH,
    content: @Composable () -> Unit
) {
    val accentColors = JnmfAccentColors(
        primary = uiColor.primary,
        bright = uiColor.secondary,
        glow = uiColor.glow,
        borderGlow = uiColor.borderGlow
    )

    val colorScheme = darkColorScheme(
        primary = uiColor.primary,
        onPrimary = JnmfDarkBg,
        primaryContainer = JnmfSurfaceVariant,
        onPrimaryContainer = uiColor.secondary,
        secondary = uiColor.secondary,
        onSecondary = JnmfDarkBg,
        secondaryContainer = JnmfSurfaceElevated,
        onSecondaryContainer = uiColor.secondary,
        tertiary = uiColor.secondary,
        onTertiary = JnmfDarkBg,
        background = JnmfDarkBg,
        onBackground = JnmfTextPrimary,
        surface = JnmfSurface,
        onSurface = JnmfTextPrimary,
        surfaceVariant = JnmfSurfaceVariant,
        onSurfaceVariant = JnmfTextSecondary,
        outline = JnmfBorder,
        outlineVariant = uiColor.borderGlow,
        error = JnmfAlertRed,
        onError = JnmfTextPrimary
    )

    val strings = getAppStrings(language)

    CompositionLocalProvider(
        LocalAccentColors provides accentColors,
        LocalAppStrings provides strings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MatrixMusicPlayerTheme(
    content: @Composable () -> Unit
) = JnmfTheme(content = content)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) = JnmfTheme(content = content)
