package com.qrstudio.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import android.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Emerald,
    onPrimary = Color(0xFF06281f),
    primaryContainer = EmeraldDark,
    onPrimaryContainer = Color(0xFFd1fae5),
    secondary = TealAccent,
    onSecondary = Color(0xFF042f2a),
    background = Bg,
    onBackground = TextMain,
    surface = CardBg,
    onSurface = TextMain,
    surfaceVariant = CardBgAlt,
    onSurfaceVariant = TextMuted,
    outline = BorderColor,
)

@Composable
fun QRStudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        content = content,
    )
}
