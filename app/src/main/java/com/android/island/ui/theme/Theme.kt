package com.android.island.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Dark premium palette (original, not a raw copy of reference layout). */
object Ui {
    val Bg = Color(0xFF0E0E12)
    val Card = Color(0xFF1C1C22)
    val CardElevated = Color(0xFF25252D)
    val IconBg = Color(0xFF2E2E38)
    val Primary = Color(0xFFB8B4F8)
    val OnPrimary = Color(0xFF2A2848)
    val Text = Color(0xFFF0EDF5)
    val Sub = Color(0xFF9B98A6)
    val Divider = Color(0xFF3A3944)
    val Outline = Color(0xFF6E6B78)
}

object Tile {
    val Blue = Color(0xFF0A84FF)
    val Green = Color(0xFF34C759)
    val Orange = Color(0xFFFF9F0A)
    val Red = Color(0xFFFF453A)
    val Purple = Color(0xFFBF5AF2)
    val Pink = Color(0xFFFF2D78)
    val Teal = Color(0xFF30B0C7)
    val Indigo = Color(0xFF5E5CE6)
    val WhatsApp = Color(0xFF25D366)
    val Gray = Color(0xFF8E8E93)
}

private val DarkColors = darkColorScheme(
    primary = Ui.Primary,
    onPrimary = Ui.OnPrimary,
    secondary = Ui.Primary,
    background = Ui.Bg,
    onBackground = Ui.Text,
    surface = Ui.Card,
    onSurface = Ui.Text,
    surfaceVariant = Ui.IconBg,
    onSurfaceVariant = Ui.Sub,
    outline = Ui.Outline
)

@Composable
fun DynamicIslandTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
