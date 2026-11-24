package com.tyce.enhancer

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

class Palette(
    val bg: Color, val card: Color, val line: Color, val accent: Color,
    val accentDeep: Color, val muted: Color, val text: Color, val red: Color, val dark: Boolean
)

object Palettes {
    val Light = Palette(Color(0xFFF3F2EE), Color(0xFFFFFFFF), Color(0xFFE6E5E0), Color(0xFF2F5BFF),
        Color(0xFF121316), Color(0xFF6B6E76), Color(0xFF121316), Color(0xFFD93025), false)
    val Dark = Palette(Color(0xFF121316), Color(0xFF1E2024), Color(0xFF2B2D33), Color(0xFF6C8BFF),
        Color(0xFFF2F2F4), Color(0xFF9A9DA6), Color(0xFFF2F2F4), Color(0xFFFF6B5E), true)
    /** True black background (#000000); dark gray only for raised surfaces. */
    val Amoled = Palette(Color(0xFF000000), Color(0xFF111113), Color(0xFF232428), Color(0xFF6C8BFF),
        Color(0xFFF2F2F4), Color(0xFF9A9DA6), Color(0xFFF2F2F4), Color(0xFFFF6B5E), true)
}

/** Persistent theme choice. Reads/writes SharedPreferences; state is Compose-observable. */
object Appearance {
    private const val KEY = "theme_mode"
    var mode by mutableStateOf(ThemeMode.SYSTEM)
        private set
    var systemDark by mutableStateOf(false)

    fun load(context: Context) {
        val s = context.getSharedPreferences("enhancer", Context.MODE_PRIVATE).getString(KEY, null)
        mode = ThemeMode.values().firstOrNull { it.name == s } ?: ThemeMode.SYSTEM
    }

    fun set(context: Context, m: ThemeMode) {
        mode = m
        context.getSharedPreferences("enhancer", Context.MODE_PRIVATE).edit().putString(KEY, m.name).apply()
    }

    val palette: Palette
        get() = when (mode) {
            ThemeMode.LIGHT -> Palettes.Light
            ThemeMode.DARK -> Palettes.Dark
            ThemeMode.AMOLED -> Palettes.Amoled
            ThemeMode.SYSTEM -> if (systemDark) Palettes.Dark else Palettes.Light
        }
}
