package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = StegoPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = StegoTealCircleBg,
    onPrimaryContainer = StegoTealText,
    secondary = StegoTealActive,
    onSecondary = Color.White,
    background = StegoBgLight,
    onBackground = StegoTextTitle,
    surface = StegoCardLight,
    onSurface = StegoTextTitle,
    surfaceVariant = StegoInputBg,
    onSurfaceVariant = StegoTextSubtitle,
    outline = StegoBorderLight
)

private val DarkColorScheme = darkColorScheme(
    primary = StegoPrimaryBlue,
    onPrimary = Color.White,
    secondary = StegoTealActive,
    onSecondary = Color.White,
    background = StegoBgDark,
    onBackground = Color(0xFFF8FAFC),
    surface = StegoCardDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = StegoBorderDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Clean crisp light design matching screenshot by default
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
