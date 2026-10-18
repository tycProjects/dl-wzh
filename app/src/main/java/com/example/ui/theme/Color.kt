package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val CyberBackground = Color(0xFF050705)
val CyberSurface = Color(0xFF101510)
val CyberSurfaceVariant = Color(0xFF182018)
val CyberSurfaceElevated = Color(0xFF202C20)
val CyberBorder = Color(0xFF283A28)

val TextPrimary = Color(0xFFEAF5EA)
val TextSecondary = Color(0xFF8B9E8B)
val TextMuted = Color(0xFF5A6E5A)

// Accent Colors requested: Green, Blue, Red, White, Cyan, Violet
val AccentNeonGreen = Color(0xFF39FF14)
val AccentElectricBlue = Color(0xFF00D4FF)
val AccentVividRed = Color(0xFFFF3344)
val AccentCrispWhite = Color(0xFFF5F5F5)
val AccentCyan = Color(0xFF00FFE0)
val AccentViolet = Color(0xFFC74BFF)

enum class ZMapsAccent(
    val id: String,
    val displayName: String,
    val color: Color,
    val onColor: Color = Color(0xFF050705)
) {
    GREEN("green", "Neon Green", AccentNeonGreen),
    BLUE("blue", "Electric Blue", AccentElectricBlue),
    RED("red", "Vivid Red", AccentVividRed),
    WHITE("white", "Crisp White", AccentCrispWhite),
    CYAN("cyan", "Cyber Cyan", AccentCyan),
    VIOLET("violet", "Hyper Violet", AccentViolet);

    companion object {
        fun fromId(id: String?): ZMapsAccent {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: GREEN
        }
    }
}
