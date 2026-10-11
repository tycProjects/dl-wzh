package dev.tajim.jarvis.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Semantic tokens shared by both holographic themes. Screens read these, never raw hex values. */
@Immutable
data class JarvisColors(
    val isDark: Boolean,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val glassFill: Color,
    val borderStart: Color,
    val borderEnd: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val cyan: Color,
    val blue: Color,
    val violet: Color,
    val magenta: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val gridLine: Color,
)

val DarkJarvisColors = JarvisColors(
    isDark = true,
    backgroundTop = Color(0xFF0A1030),
    backgroundBottom = Color(0xFF03050F),
    glassFill = Color(0x1AFFFFFF),
    borderStart = Color(0xAA22D3EE),
    borderEnd = Color(0xAA8B5CF6),
    textPrimary = Color(0xFFEAF4FF),
    textSecondary = Color(0xFF9FB4D6),
    cyan = Color(0xFF22D3EE),
    blue = Color(0xFF3B82F6),
    violet = Color(0xFF8B5CF6),
    magenta = Color(0xFFEC4899),
    success = Color(0xFF34D399),
    warning = Color(0xFFFBBF24),
    error = Color(0xFFFB7185),
    gridLine = Color(0x1222D3EE),
)

val LightJarvisColors = JarvisColors(
    isDark = false,
    backgroundTop = Color(0xFFF8FBFF),
    backgroundBottom = Color(0xFFDCEBFF),
    glassFill = Color(0xB3FFFFFF),
    borderStart = Color(0xAA0891B2),
    borderEnd = Color(0xAA7C3AED),
    textPrimary = Color(0xFF0B1B3F),
    textSecondary = Color(0xFF42557D),
    cyan = Color(0xFF0891B2),
    blue = Color(0xFF2563EB),
    violet = Color(0xFF7C3AED),
    magenta = Color(0xFFDB2777),
    success = Color(0xFF059669),
    warning = Color(0xFFB45309),
    error = Color(0xFFDC2626),
    gridLine = Color(0x1A0891B2),
)

val LocalJarvisColors = staticCompositionLocalOf { DarkJarvisColors }
