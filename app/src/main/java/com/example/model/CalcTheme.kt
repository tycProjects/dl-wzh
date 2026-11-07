package com.example.model

import androidx.compose.ui.graphics.Color

enum class ThemePalette(
    val title: String,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val darkBackground: Color,
    val darkSurface: Color,
    val darkSurfaceVariant: Color,
    val darkNumberKey: Color,
    val darkOperatorKey: Color,
    val lightBackground: Color,
    val lightSurface: Color,
    val lightSurfaceVariant: Color,
    val lightNumberKey: Color,
    val lightOperatorKey: Color
) {
    CYAN_OLED(
        title = "Cyber Cyan (OLED)",
        primary = Color(0xFF00E5FF),
        secondary = Color(0xFF00B0FF),
        accent = Color(0xFFFF9100),
        darkBackground = Color(0xFF07090C),
        darkSurface = Color(0xFF10141A),
        darkSurfaceVariant = Color(0xFF181F27),
        darkNumberKey = Color(0xFF141A22),
        darkOperatorKey = Color(0xFF1C2734),
        lightBackground = Color(0xFFF0F4F8),
        lightSurface = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFE2E8F0),
        lightNumberKey = Color(0xFFF8FAFC),
        lightOperatorKey = Color(0xFFE0F2FE)
    ),
    AMBER_SUNSET(
        title = "Solar Amber",
        primary = Color(0xFFFFB300),
        secondary = Color(0xFFFF8F00),
        accent = Color(0xFFFF3D00),
        darkBackground = Color(0xFF0C0A07),
        darkSurface = Color(0xFF17130F),
        darkSurfaceVariant = Color(0xFF231D16),
        darkNumberKey = Color(0xFF1C1712),
        darkOperatorKey = Color(0xFF2E2217),
        lightBackground = Color(0xFFFFFBF0),
        lightSurface = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFFEEBC8),
        lightNumberKey = Color(0xFFFFFDF8),
        lightOperatorKey = Color(0xFFFEF3C7)
    ),
    EMERALD_MATRIX(
        title = "Sharp Emerald",
        primary = Color(0xFF00E676),
        secondary = Color(0xFF00C853),
        accent = Color(0xFF76FF03),
        darkBackground = Color(0xFF050E09),
        darkSurface = Color(0xFF0D1C13),
        darkSurfaceVariant = Color(0xFF14291D),
        darkNumberKey = Color(0xFF0F2218),
        darkOperatorKey = Color(0xFF163524),
        lightBackground = Color(0xFFF0FDF4),
        lightSurface = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFDCFCE7),
        lightNumberKey = Color(0xFFF7FEE7),
        lightOperatorKey = Color(0xFFD1FAE5)
    ),
    NEON_VIOLET(
        title = "Neon Violet",
        primary = Color(0xFFB388FF),
        secondary = Color(0xFF7C4DFF),
        accent = Color(0xFFFF4081),
        darkBackground = Color(0xFF090712),
        darkSurface = Color(0xFF141124),
        darkSurfaceVariant = Color(0xFF1F1A36),
        darkNumberKey = Color(0xFF18142C),
        darkOperatorKey = Color(0xFF261F44),
        lightBackground = Color(0xFFF8F5FF),
        lightSurface = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFEDE9FE),
        lightNumberKey = Color(0xFFFAF5FF),
        lightOperatorKey = Color(0xFFE9D5FF)
    ),
    CRIMSON_SHARP(
        title = "Crimson Sharp",
        primary = Color(0xFFFF5252),
        secondary = Color(0xFFFF1744),
        accent = Color(0xFFFFD740),
        darkBackground = Color(0xFF0E0707),
        darkSurface = Color(0xFF1A0E0E),
        darkSurfaceVariant = Color(0xFF261414),
        darkNumberKey = Color(0xFF201111),
        darkOperatorKey = Color(0xFF331919),
        lightBackground = Color(0xFFFFF1F2),
        lightSurface = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFFFE4E6),
        lightNumberKey = Color(0xFFFFF5F5),
        lightOperatorKey = Color(0xFFFEE2E2)
    )
}

enum class DarkModePreference(val label: String) {
    SYSTEM("Auto (System)"),
    DARK("Always Dark"),
    LIGHT("Always Light")
}
