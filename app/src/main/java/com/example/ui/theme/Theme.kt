package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FuturisticDarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = CyberDarkBg,
    primaryContainer = CyanDarkSubtle,
    onPrimaryContainer = CyanNeonGlow,
    secondary = CyanNeon,
    onSecondary = CyberDarkBg,
    secondaryContainer = BlueDeep,
    onSecondaryContainer = CyanNeonGlow,
    tertiary = BlueBorder,
    onTertiary = TextWhite,
    background = CyberDarkBg,
    onBackground = TextWhite,
    surface = CyberDarkElevated,
    onSurface = TextWhite,
    surfaceVariant = CyberDarkSurface,
    onSurfaceVariant = TextLightGray,
    outline = CyberDarkBorder,
    outlineVariant = BlueDeep
)

private val CrispLightColorScheme = lightColorScheme(
    primary = LightCyanPrimary,
    onPrimary = TextWhite,
    primaryContainer = Color(0xFFE0F7FA),
    onPrimaryContainer = Color(0xFF006064),
    secondary = LightCyanPrimary,
    onSecondary = TextWhite,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = LightTextDark,
    background = LightBg,
    onBackground = LightTextDark,
    surface = LightSurface,
    onSurface = LightTextDark,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSubtle,
    outline = LightBorder,
    outlineVariant = Color(0xFFE2E8F0)
)

@Composable
fun BisayaSubanenTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) FuturisticDarkColorScheme else CrispLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Keep alias for compatibility with test templates
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    BisayaSubanenTheme(darkTheme = darkTheme, content = content)
}

