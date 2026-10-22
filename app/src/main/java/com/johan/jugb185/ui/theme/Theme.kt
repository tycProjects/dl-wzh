package com.johan.jugb185.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JugbCarbon = Color(0xFF0A0E14)
private val JugbSurface = Color(0xFF131A26)
private val JugbCyan = Color(0xFF00E5FF)
private val JugbOrange = Color(0xFFFF6D00)
private val JugbSuccess = Color(0xFF00E676)

private val DarkScheme = darkColorScheme(
    primary = JugbCyan,
    secondary = JugbOrange,
    tertiary = JugbSuccess,
    background = JugbCarbon,
    surface = JugbSurface,
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun JugbTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
