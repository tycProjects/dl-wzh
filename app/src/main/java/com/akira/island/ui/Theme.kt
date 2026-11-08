package com.akira.island.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Тёмная тема с лавандовым акцентом, как на скриншотах. */
object Ui {
    val Bg = Color(0xFF080A14)
    val Card = Color(0xB51B2034)
    val IconBg = Color(0xFF252B43)
    val Primary = Color(0xFF9CB8FF)
    val OnPrimary = Color(0xFF2F2D4C)
    val Text = Color(0xFFE6E1E9)
    val Sub = Color(0xFFA9A6B1)
    val Divider = Color(0xFF47464F)
    val Nav = Color(0xC9141A2B)
    val Pink = Color(0xFFFBDDF4)
    val Outline = Color(0x6689B8FF)
}

/** Цвета значков у пунктов настроек — как в «Настройках» iPhone: у каждой категории свой. */
object Tile {
    val Blue = Color(0xFF0A84FF)
    val Green = Color(0xFF34C759)
    val Orange = Color(0xFFFF9F0A)
    val Red = Color(0xFFFF453A)
    val Purple = Color(0xFFBF5AF2)
    val Pink = Color(0xFFFF2D78)
    val Teal = Color(0xFF30B0C7)
    val Indigo = Color(0xFF5E5CE6)
    val Yellow = Color(0xFFFFB800)
    val Gray = Color(0xFF8E8E93)
}

@Composable
fun IslandTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Ui.Primary,
            onPrimary = Ui.OnPrimary,
            secondary = Ui.Primary,
            background = Ui.Bg,
            onBackground = Ui.Text,
            surface = Ui.Card,
            onSurface = Ui.Text,
            surfaceVariant = Ui.IconBg,
            onSurfaceVariant = Ui.Sub,
            surfaceContainerHigh = Ui.Card,
            outline = Ui.Outline,
        ),
        typography = InterTypography,
        content = content,
    )
}
