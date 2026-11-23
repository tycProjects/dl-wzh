package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Core Tactical Dark Broadcast Palette
val PitchBlack = Color(0xFF090D14)
val ObsidianSurface = Color(0xFF101622)
val SlateCard = Color(0xFF161F2E)
val SlateElevated = Color(0xFF1E293B)
val HairlineBorder = Color(0xFF283548)

val TurfEmerald = Color(0xFF10B981)
val TurfEmeraldBright = Color(0xFF34D399)
val TurfEmeraldMuted = Color(0xFF064E3B)

val ElectricCyan = Color(0xFF38BDF8)
val CyanMuted = Color(0xFF0C4A6E)

val BroadcastAmber = Color(0xFFF59E0B)
val AmberMuted = Color(0xFF78350F)

val CrimsonRed = Color(0xFFEF4444)
val CrimsonMuted = Color(0xFF7F1D1D)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextTertiary = Color(0xFF64748B)

// Curated Club Customization Swatches
val ClubColorPalette: List<Pair<String, String>> = listOf(
    "#10B981" to "Emerald",
    "#EF4444" to "Crimson",
    "#3B82F6" to "Royal Blue",
    "#F59E0B" to "Gold",
    "#8B5CF6" to "Violet",
    "#06B6D4" to "Cyan",
    "#EC4899" to "Magenta",
    "#F97316" to "Tangerine",
    "#E2E8F0" to "Silver",
    "#1E293B" to "Obsidian",
)

fun parseHexColor(hex: String, fallback: Color = TurfEmerald): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        val colorInt = when (cleaned.length) {
            6 -> ("FF$cleaned").toLong(16)
            8 -> cleaned.toLong(16)
            else -> return fallback
        }
        Color(colorInt)
    } catch (_: Exception) {
        fallback
    }
}
