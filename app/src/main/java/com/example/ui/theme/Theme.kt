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

private val DarkColorScheme = darkColorScheme(
    primary = FlameOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4E1D0C),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = FlameAmber,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF452B06),
    onSecondaryContainer = Color(0xFFFFDEAC),
    tertiary = FlameGold,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = Color(0xFFECEBEE),
    surface = DarkSurface,
    onSurface = Color(0xFFECEBEE),
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = Color(0xFFC7C5CF),
    outline = DarkSurfaceBorder
)

private val LightColorScheme = lightColorScheme(
    primary = FlameOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFECE4),
    onPrimaryContainer = Color(0xFF4E1D0C),
    secondary = FlameAmber,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF3DE),
    onSecondaryContainer = Color(0xFF452B06),
    tertiary = FlameCrimson,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF1B1A1E),
    surface = LightSurface,
    onSurface = Color(0xFF1B1A1E),
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = Color(0xFF5A5862),
    outline = LightSurfaceBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to deep energetic dark theme for the fire glow experience
    dynamicColor: Boolean = false, // Keep signature fiery branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
