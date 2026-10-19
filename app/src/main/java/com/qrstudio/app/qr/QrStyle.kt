package com.qrstudio.app.qr

data class QrStyleConfig(
    val dotShape: DotShape = DotShape.ROUNDED,
    val cornerFrame: CornerFrame = CornerFrame.ROUND,
    val cornerDot: CornerDot = CornerDot.DOT,
    val primaryColor: Long = 0xFF000000,
    val secondaryColor: Long = 0xFF000000,
    val gradient: Boolean = false,
    val backgroundColor: Long = 0xFFFFFFFF,
    val transparentBg: Boolean = false,
    val errorCorrection: String = "M",
    val exportSize: Int = 600,
    val quietZone: Int = 2,
    val logoSizePercent: Int = 25,
    val logoMargin: Int = 6,
    val caption: String = "",
    val captionColor: Long = 0xFF000000,
    val captionSize: Int = 18,
)

enum class DotShape(val label: String) {
    SQUARE("مربّع"),
    ROUNDED("مقوّس"),
    DOTS("نقاط"),
    SMOOTH("دائري ناعم"),
    CLASSY("أنيق"),
    CLASSY_ROUND("أنيق مقوّس"),
}

enum class CornerFrame(val label: String) {
    SQUARE("مربع"),
    ROUND("مقوّس"),
}

enum class CornerDot(val label: String) {
    SQUARE("مربع"),
    DOT("دائري"),
}

data class QrThemePreset(
    val label: String,
    val primary: Long,
    val secondary: Long,
    val gradient: Boolean,
    val background: Long,
    val transparent: Boolean = false,
)

object QrThemes {
    val all: List<QrThemePreset> = listOf(
        QrThemePreset("كلاسيكي", 0xFF000000, 0xFF000000, gradient = false, background = 0xFFFFFFFF),
        QrThemePreset("نيون", 0xFF2dd4bf, 0xFF10b981, gradient = true, background = 0xFF0b0f14),
        QrThemePreset("بنفسجي", 0xFF8b5cf6, 0xFFd946ef, gradient = true, background = 0xFFFFFFFF),
        QrThemePreset("ذهبي", 0xFFf59e0b, 0xFFfbbf24, gradient = true, background = 0xFF1a1408),
        QrThemePreset("محيط", 0xFF0ea5e9, 0xFF2dd4bf, gradient = true, background = 0xFFFFFFFF),
        QrThemePreset("داكن", 0xFFe5e7eb, 0xFFe5e7eb, gradient = false, background = 0xFF111827),
    )
}

fun QrStyleConfig.fromPreset(preset: QrThemePreset): QrStyleConfig = copy(
    primaryColor = preset.primary,
    secondaryColor = preset.secondary,
    gradient = preset.gradient,
    backgroundColor = preset.background,
    transparentBg = preset.transparent,
    captionColor = if (preset.background == 0xFFFFFFFFL) 0xFF000000 else 0xFFFFFFFF,
)

object QrPalettes {
    val accents: List<Long> = listOf(
        0xFF000000, 0xFF10b981, 0xFF2dd4bf, 0xFF0ea5e9,
        0xFF8b5cf6, 0xFFec4899, 0xFFef4444, 0xFFf59e0b, 0xFFFFFFFF,
    )
    val backgrounds: List<Long> = listOf(
        0xFFFFFFFF, 0xFFf8fafc, 0xFF0b0f14, 0xFF111827, 0xFF1a1408, 0xFFecfeff,
    )
    val captionColors: List<Long> = listOf(
        0xFF000000, 0xFF475569, 0xFF10b981, 0xFF2563eb, 0xFFdc2626, 0xFF9333ea,
    )
}
