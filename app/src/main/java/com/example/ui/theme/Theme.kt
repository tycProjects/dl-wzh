package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class TumbaCustomColors(
    val bgMain: Color,
    val bgSidebar: Color,
    val bgCard: Color,
    val bgElevated: Color,
    val bgHover: Color,
    val borderColor: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val danger: Color,
    val dangerBg: Color
)

val LocalTumbaColors = staticCompositionLocalOf {
    TumbaCustomColors(
        bgMain = ObsidianBg,
        bgSidebar = ObsidianSurface,
        bgCard = ObsidianCard,
        bgElevated = ObsidianElevated,
        bgHover = ObsidianHover,
        borderColor = ObsidianBorder,
        textPrimary = ObsidianTextPrimary,
        textSecondary = ObsidianTextSecondary,
        textMuted = ObsidianTextMuted,
        accent = AccentSilver,
        danger = GraphiteRed,
        dangerBg = GraphiteRedBg
    )
}

fun getAccentColor(accentName: String): Color {
    return when (accentName.lowercase()) {
        "titanium" -> AccentTitanium
        "platinum" -> AccentPlatinum
        "gold" -> AccentGold
        "ice" -> AccentIce
        else -> AccentSilver
    }
}

@Composable
fun TumbaTheme(
    themeName: String = "dark",
    accentName: String = "silver",
    language: String = "uk",
    content: @Composable () -> Unit
) {
    val accent = getAccentColor(accentName)
    val strings = getStringsForLanguage(language)

    val (colorScheme, customColors) = when (themeName.lowercase()) {
        "dim" -> {
            darkColorScheme(
                primary = accent,
                background = SlateBg,
                surface = SlateSurface,
                surfaceVariant = SlateCard,
                onPrimary = ObsidianBg,
                onBackground = SlateTextPrimary,
                onSurface = SlateTextPrimary,
                onSurfaceVariant = SlateTextSecondary,
                outline = SlateBorder
            ) to TumbaCustomColors(
                bgMain = SlateBg,
                bgSidebar = SlateSurface,
                bgCard = SlateCard,
                bgElevated = SlateElevated,
                bgHover = SlateBorder,
                borderColor = SlateBorder,
                textPrimary = SlateTextPrimary,
                textSecondary = SlateTextSecondary,
                textMuted = ObsidianTextMuted,
                accent = accent,
                danger = GraphiteRed,
                dangerBg = GraphiteRedBg
            )
        }
        "light" -> {
            lightColorScheme(
                primary = accent,
                background = PaperBg,
                surface = PaperSurface,
                surfaceVariant = PaperCard,
                onPrimary = PaperBg,
                onBackground = PaperTextPrimary,
                onSurface = PaperTextPrimary,
                onSurfaceVariant = PaperTextSecondary,
                outline = PaperBorder
            ) to TumbaCustomColors(
                bgMain = PaperBg,
                bgSidebar = PaperSurface,
                bgCard = PaperCard,
                bgElevated = PaperElevated,
                bgHover = PaperBorder,
                borderColor = PaperBorder,
                textPrimary = PaperTextPrimary,
                textSecondary = PaperTextSecondary,
                textMuted = PaperTextSecondary,
                accent = accent,
                danger = GraphiteRed,
                dangerBg = GraphiteRedBg
            )
        }
        else -> { // "dark" (Obsidian)
            darkColorScheme(
                primary = accent,
                background = ObsidianBg,
                surface = ObsidianSurface,
                surfaceVariant = ObsidianCard,
                onPrimary = ObsidianBg,
                onBackground = ObsidianTextPrimary,
                onSurface = ObsidianTextPrimary,
                onSurfaceVariant = ObsidianTextSecondary,
                outline = ObsidianBorder
            ) to TumbaCustomColors(
                bgMain = ObsidianBg,
                bgSidebar = ObsidianSurface,
                bgCard = ObsidianCard,
                bgElevated = ObsidianElevated,
                bgHover = ObsidianHover,
                borderColor = ObsidianBorder,
                textPrimary = ObsidianTextPrimary,
                textSecondary = ObsidianTextSecondary,
                textMuted = ObsidianTextMuted,
                accent = accent,
                danger = GraphiteRed,
                dangerBg = GraphiteRedBg
            )
        }
    }

    CompositionLocalProvider(
        LocalTumbaColors provides customColors,
        LocalTumbaStrings provides strings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
