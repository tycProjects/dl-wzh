package com.example.ui.keyboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import com.example.model.KeyBorderStyle
import com.example.model.KeyboardFontFamily
import com.example.model.KeyboardThemePreset

data class KeyboardPalette(
    val backgroundBrush: Brush,
    val surfaceColor: Color,
    val keyColor: Color,
    val specialKeyColor: Color,
    val accentColor: Color,
    val textColor: Color,
    val specialTextColor: Color,
    val borderColor: Color,
    val glassGlossBrush: Brush,
    val suggestionBg: Color,
    val suggestionText: Color
)

object ThemePaletteResolver {

    fun resolvePalette(preset: KeyboardThemePreset, borderStyle: KeyBorderStyle, glassOpacity: Float): KeyboardPalette {
        return when (preset) {
            KeyboardThemePreset.LIQUID_GLASS -> {
                val glassAlpha = glassOpacity.coerceIn(0.15f, 0.70f)
                KeyboardPalette(
                    backgroundBrush = Brush.verticalGradient(
                        listOf(Color(0xFF0F172A), Color(0xFF020617))
                    ),
                    surfaceColor = Color(0x301E293B),
                    keyColor = Color.White.copy(alpha = glassAlpha),
                    specialKeyColor = Color(0xFF00E5FF).copy(alpha = glassAlpha + 0.10f),
                    accentColor = Color(0xFF00E5FF),
                    textColor = Color.White,
                    specialTextColor = Color(0xFFE0F7FA),
                    borderColor = when (borderStyle) {
                        KeyBorderStyle.BORDERLESS -> Color.Transparent
                        KeyBorderStyle.BORDERED -> Color(0x5500E5FF)
                        KeyBorderStyle.LIQUID_GLASS -> Color(0x80FFFFFF)
                    },
                    glassGlossBrush = Brush.verticalGradient(
                        listOf(Color(0x60FFFFFF), Color(0x15FFFFFF), Color.Transparent)
                    ),
                    suggestionBg = Color(0x400F172A),
                    suggestionText = Color(0xFF00E5FF)
                )
            }

            KeyboardThemePreset.DARK_SLATE -> {
                KeyboardPalette(
                    backgroundBrush = Brush.verticalGradient(
                        listOf(Color(0xFF1E2026), Color(0xFF141519))
                    ),
                    surfaceColor = Color(0xFF1E2026),
                    keyColor = if (borderStyle == KeyBorderStyle.LIQUID_GLASS) Color(0x403A3F4D) else Color(0xFF2C303B),
                    specialKeyColor = Color(0xFF383E4C),
                    accentColor = Color(0xFF6366F1),
                    textColor = Color(0xFFF1F5F9),
                    specialTextColor = Color(0xFFCBD5E1),
                    borderColor = when (borderStyle) {
                        KeyBorderStyle.BORDERLESS -> Color.Transparent
                        KeyBorderStyle.BORDERED -> Color(0xFF474E5D)
                        KeyBorderStyle.LIQUID_GLASS -> Color(0x70FFFFFF)
                    },
                    glassGlossBrush = Brush.verticalGradient(
                        listOf(Color(0x35FFFFFF), Color.Transparent)
                    ),
                    suggestionBg = Color(0xFF242731),
                    suggestionText = Color(0xFF818CF8)
                )
            }

            KeyboardThemePreset.AMOLED_BLACK -> {
                KeyboardPalette(
                    backgroundBrush = Brush.verticalGradient(
                        listOf(Color(0xFF000000), Color(0xFF000000))
                    ),
                    surfaceColor = Color(0xFF0A0A0A),
                    keyColor = if (borderStyle == KeyBorderStyle.LIQUID_GLASS) Color(0x30333333) else Color(0xFF181818),
                    specialKeyColor = Color(0xFF242424),
                    accentColor = Color(0xFFFFFFFF),
                    textColor = Color(0xFFEEEEEE),
                    specialTextColor = Color(0xFFAAAAAA),
                    borderColor = when (borderStyle) {
                        KeyBorderStyle.BORDERLESS -> Color.Transparent
                        KeyBorderStyle.BORDERED -> Color(0xFF333333)
                        KeyBorderStyle.LIQUID_GLASS -> Color(0x55FFFFFF)
                    },
                    glassGlossBrush = Brush.verticalGradient(
                        listOf(Color(0x30FFFFFF), Color.Transparent)
                    ),
                    suggestionBg = Color(0xFF0E0E0E),
                    suggestionText = Color(0xFFFFFFFF)
                )
            }

            KeyboardThemePreset.CYBER_NEON -> {
                KeyboardPalette(
                    backgroundBrush = Brush.verticalGradient(
                        listOf(Color(0xFF08071A), Color(0xFF0F0B26))
                    ),
                    surfaceColor = Color(0xFF130E30),
                    keyColor = if (borderStyle == KeyBorderStyle.LIQUID_GLASS) Color(0x40FF007F) else Color(0xFF1F1646),
                    specialKeyColor = Color(0xFF2B1C63),
                    accentColor = Color(0xFF00F0FF),
                    textColor = Color(0xFFFDFDFD),
                    specialTextColor = Color(0xFF00F0FF),
                    borderColor = when (borderStyle) {
                        KeyBorderStyle.BORDERLESS -> Color.Transparent
                        KeyBorderStyle.BORDERED -> Color(0xFF00F0FF)
                        KeyBorderStyle.LIQUID_GLASS -> Color(0x80FF007F)
                    },
                    glassGlossBrush = Brush.verticalGradient(
                        listOf(Color(0x6000F0FF), Color.Transparent)
                    ),
                    suggestionBg = Color(0xFF1A1340),
                    suggestionText = Color(0xFFFF007F)
                )
            }

            KeyboardThemePreset.SUNSET_GLOW -> {
                KeyboardPalette(
                    backgroundBrush = Brush.verticalGradient(
                        listOf(Color(0xFF2D122D), Color(0xFF180A20))
                    ),
                    surfaceColor = Color(0xFF2A1535),
                    keyColor = if (borderStyle == KeyBorderStyle.LIQUID_GLASS) Color(0x40FFA726) else Color(0xFF3E1E4C),
                    specialKeyColor = Color(0xFF532468),
                    accentColor = Color(0xFFFF9E80),
                    textColor = Color(0xFFFFF3E0),
                    specialTextColor = Color(0xFFFFAB91),
                    borderColor = when (borderStyle) {
                        KeyBorderStyle.BORDERLESS -> Color.Transparent
                        KeyBorderStyle.BORDERED -> Color(0x80FF9E80)
                        KeyBorderStyle.LIQUID_GLASS -> Color(0x90FFD54F)
                    },
                    glassGlossBrush = Brush.verticalGradient(
                        listOf(Color(0x50FFE082), Color.Transparent)
                    ),
                    suggestionBg = Color(0xFF381B45),
                    suggestionText = Color(0xFFFFB74D)
                )
            }

            KeyboardThemePreset.SAKURA_PASTEL -> {
                KeyboardPalette(
                    backgroundBrush = Brush.verticalGradient(
                        listOf(Color(0xFF2B1B27), Color(0xFF1C121B))
                    ),
                    surfaceColor = Color(0xFF352030),
                    keyColor = if (borderStyle == KeyBorderStyle.LIQUID_GLASS) Color(0x40F48FB1) else Color(0xFF45273F),
                    specialKeyColor = Color(0xFF5A3052),
                    accentColor = Color(0xFFF48FB1),
                    textColor = Color(0xFFFCE4EC),
                    specialTextColor = Color(0xFFF8BBD0),
                    borderColor = when (borderStyle) {
                        KeyBorderStyle.BORDERLESS -> Color.Transparent
                        KeyBorderStyle.BORDERED -> Color(0x80F48FB1)
                        KeyBorderStyle.LIQUID_GLASS -> Color(0x80FFFFFF)
                    },
                    glassGlossBrush = Brush.verticalGradient(
                        listOf(Color(0x45FFFFFF), Color.Transparent)
                    ),
                    suggestionBg = Color(0xFF3A1F34),
                    suggestionText = Color(0xFFF48FB1)
                )
            }

            KeyboardThemePreset.MINT_EMERALD -> {
                KeyboardPalette(
                    backgroundBrush = Brush.verticalGradient(
                        listOf(Color(0xFF0C241F), Color(0xFF061411))
                    ),
                    surfaceColor = Color(0xFF11322C),
                    keyColor = if (borderStyle == KeyBorderStyle.LIQUID_GLASS) Color(0x4026A69A) else Color(0xFF18423A),
                    specialKeyColor = Color(0xFF20554B),
                    accentColor = Color(0xFF80CBC4),
                    textColor = Color(0xFFE0F2F1),
                    specialTextColor = Color(0xFFA7FFEB),
                    borderColor = when (borderStyle) {
                        KeyBorderStyle.BORDERLESS -> Color.Transparent
                        KeyBorderStyle.BORDERED -> Color(0x8080CBC4)
                        KeyBorderStyle.LIQUID_GLASS -> Color(0x80A7FFEB)
                    },
                    glassGlossBrush = Brush.verticalGradient(
                        listOf(Color(0x40A7FFEB), Color.Transparent)
                    ),
                    suggestionBg = Color(0xFF153B34),
                    suggestionText = Color(0xFF64FFDA)
                )
            }

            KeyboardThemePreset.NORDIC_FROST -> {
                KeyboardPalette(
                    backgroundBrush = Brush.verticalGradient(
                        listOf(Color(0xFF112233), Color(0xFF0A141F))
                    ),
                    surfaceColor = Color(0xFF162D42),
                    keyColor = if (borderStyle == KeyBorderStyle.LIQUID_GLASS) Color(0x4090CAF9) else Color(0xFF1D3954),
                    specialKeyColor = Color(0xFF26496B),
                    accentColor = Color(0xFF90CAF9),
                    textColor = Color(0xFFF0F8FF),
                    specialTextColor = Color(0xFFBBDEFB),
                    borderColor = when (borderStyle) {
                        KeyBorderStyle.BORDERLESS -> Color.Transparent
                        KeyBorderStyle.BORDERED -> Color(0x8090CAF9)
                        KeyBorderStyle.LIQUID_GLASS -> Color(0x90E3F2FD)
                    },
                    glassGlossBrush = Brush.verticalGradient(
                        listOf(Color(0x50E3F2FD), Color.Transparent)
                    ),
                    suggestionBg = Color(0xFF173048),
                    suggestionText = Color(0xFF90CAF9)
                )
            }
        }
    }

    @Composable
    fun resolveFontFamily(fontFamily: KeyboardFontFamily): FontFamily {
        return when (fontFamily) {
            KeyboardFontFamily.MODERN_SANS -> FontFamily.SansSerif
            KeyboardFontFamily.POPPINS -> FontFamily(Font(R.font.poppins, FontWeight.Normal))
            KeyboardFontFamily.PLAYFAIR -> FontFamily(Font(R.font.playfair_display, FontWeight.Normal))
            KeyboardFontFamily.FIRA_CODE -> FontFamily(Font(R.font.fira_code, FontWeight.Normal))
            KeyboardFontFamily.CAVEAT -> FontFamily(Font(R.font.caveat, FontWeight.Normal))
            KeyboardFontFamily.MONOSPACE -> FontFamily.Monospace
        }
    }
}
