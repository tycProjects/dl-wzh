package com.zychocuwek.musify.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Palet "Ruang dengar" dari skin.css v5.1 */
object C {
    val Bg = Color(0xFF1A1223)
    val Surface = Color(0xFF251A32)
    val Raised = Color(0xFF32244A)
    val Bone = Color(0xFFF4EFE6)
    val Muted = Color(0x9EF4EFE6)       // 62%
    val Muted2 = Color(0x6BF4EFE6)      // 42%
    val Accent = Color(0xFFF5B83D)      // saffron
    val AccentInk = Color(0xFF20150A)
    val Line = Color(0x17F4EFE6)
}

private val scheme = darkColorScheme(
    primary = C.Accent, onPrimary = C.AccentInk,
    background = C.Bg, onBackground = C.Bone,
    surface = C.Surface, onSurface = C.Bone,
    surfaceVariant = C.Raised, onSurfaceVariant = C.Muted,
    secondary = C.Accent, onSecondary = C.AccentInk,
    outline = C.Line
)

@Composable
fun MusifyTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = scheme, content = content)
