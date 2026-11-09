package com.example.model

enum class KeyBorderStyle(val label: String, val description: String) {
    BORDERLESS("Borderless", "Ultra-clean flush keys with floating minimalist tap targets"),
    BORDERED("Bordered", "Tactile high-contrast bordered keys with defined edges"),
    LIQUID_GLASS("Liquid Glass", "Translucent frosted glass with glossy specular highlights and neon rim")
}

enum class KeyboardThemePreset(val label: String, val description: String) {
    LIQUID_GLASS("Liquid Glass", "Signature frosted glass with cyan/magenta neon glow"),
    DARK_SLATE("Dark Slate", "Accessible deep slate & graphite with crisp white legends"),
    AMOLED_BLACK("AMOLED Black", "Pure pitch black #000000 for maximum OLED battery savings"),
    CYBER_NEON("Cyber Neon", "Electric cyan and neon magenta matrix vibe"),
    SUNSET_GLOW("Sunset Glow", "Warm golden amber, coral, and violet twilight"),
    SAKURA_PASTEL("Sakura Pastel", "Soft Japanese cherry blossom pink & lavender"),
    MINT_EMERALD("Mint Emerald", "Crisp oceanic mint teal & forest slate"),
    NORDIC_FROST("Nordic Frost", "Icy arctic blue & silver glacier sheen")
}

enum class KeyboardFontFamily(val label: String, val sample: String) {
    MODERN_SANS("Modern Sans", "Aa Bb Cc"),
    POPPINS("Poppins Rounded", "Friendly Aa"),
    PLAYFAIR("Playfair Serif", "Luxury Serif"),
    FIRA_CODE("Fira Code", "Code { 123 }"),
    CAVEAT("Caveat Script", "Handwritten"),
    MONOSPACE("System Mono", "[ 0101 ]")
}

enum class KeyboardIconStyle(val label: String) {
    MODERN_FILLED("Modern Filled"),
    OUTLINED("Minimal Outline"),
    NEON_GLOW("Neon Glow"),
    RETRO_FLAT("Retro Flat")
}

enum class KeyboardWallpaper(val label: String, val isDrawable: Boolean) {
    NONE("No Wallpaper (Pure Theme)", false),
    AURORA_GLASS("Aurora Liquid Glass", true),
    CYBER_GRID("Cyber Grid Matrix", true),
    GRADIENT_SUNSET("Sunset Horizon Gradient", false),
    GRADIENT_COSMIC("Cosmic Nebula Gradient", false),
    GRADIENT_EMERALD("Emerald Mystic Gradient", false)
}

enum class KeyHeight(val label: String, val heightDp: Int) {
    COMPACT("Compact", 44),
    NORMAL("Normal", 50),
    COMFORTABLE("Comfortable", 56),
    TALL("Tall", 62)
}

data class KeyboardSettings(
    val keyBorderStyle: KeyBorderStyle = KeyBorderStyle.LIQUID_GLASS,
    val themePreset: KeyboardThemePreset = KeyboardThemePreset.LIQUID_GLASS,
    val fontFamily: KeyboardFontFamily = KeyboardFontFamily.POPPINS,
    val iconStyle: KeyboardIconStyle = KeyboardIconStyle.MODERN_FILLED,
    val wallpaper: KeyboardWallpaper = KeyboardWallpaper.AURORA_GLASS,
    val hapticEnabled: Boolean = true,
    val hapticStrength: Int = 30, // milliseconds (5..75)
    val soundEnabled: Boolean = false,
    val soundVolume: Float = 0.5f,
    val autoCorrectEnabled: Boolean = true,
    val suggestionsEnabled: Boolean = true,
    val autoCapitalization: Boolean = true,
    val doubleSpacePeriod: Boolean = true,
    val numberRowEnabled: Boolean = false,
    val keyHeight: KeyHeight = KeyHeight.NORMAL,
    val glassOpacity: Float = 0.35f
)
