package com.schedule.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import com.schedule.app.R

/** الوضع الفاتح/الداكن: الألوان دوال قراءة (getters) فتتحدّث الواجهة تلقائيا عند التبديل. */
object ThemeState { var light by mutableStateOf(false) }

private fun pick(dark: Long, light: Long) = Color(if (ThemeState.light) light else dark)

// تصميم داكن فاخر: أسود عميق + قرمزي
val Bg: Color get() = pick(0xFF0A0A0B, 0xFFF6F6F7)
val Surface1: Color get() = pick(0xFF141416, 0xFFFFFFFF)
val Surface2: Color get() = pick(0xFF1C1C1F, 0xFFEDEDF0)
val Border: Color get() = pick(0xFF2A2A2E, 0xFFD9D9DE)
val Crimson = Color(0xFFB91C3C)
val CrimsonSoft: Color get() = pick(0xFF2E1219, 0xFFFBE4EA)
val TextMain: Color get() = pick(0xFFEDEDED, 0xFF16161A)
val Muted: Color get() = pick(0xFF8E8E96, 0xFF6B6B73)

/** ألوان المواد: خافتة تناسب الخلفية الداكنة. */
val SLOT_COLORS = listOf(0xFFB91C3C, 0xFF2563EB, 0xFF0D9488, 0xFFD97706, 0xFF7C3AED, 0xFF16A34A, 0xFFDB2777, 0xFF64748B).map { Color(it) }

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun cairo(w: Int) = Font(R.font.cairo, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
private val Cairo = FontFamily(cairo(400), cairo(600), cairo(700))

private fun scheme() = (if (ThemeState.light) lightColorScheme() else darkColorScheme()).copy(
    primary = Crimson, onPrimary = Color.White, secondary = Muted, background = Bg, onBackground = TextMain,
    surface = Surface1, onSurface = TextMain, surfaceVariant = Surface2, onSurfaceVariant = Muted,
    outline = Border, error = Color(0xFFEF4444), surfaceContainerHigh = Surface2, surfaceContainer = Surface1,
    surfaceContainerHighest = Surface2, surfaceContainerLow = Surface1, surfaceContainerLowest = Bg,
)

private val base = Typography()
private fun TextStyle.c(w: FontWeight = FontWeight.Normal) = copy(fontFamily = Cairo, fontWeight = w)
private val typo = Typography(
    headlineSmall = base.headlineSmall.c(FontWeight.Bold), titleLarge = base.titleLarge.c(FontWeight.Bold),
    titleMedium = base.titleMedium.c(FontWeight.SemiBold), titleSmall = base.titleSmall.c(FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.c(), bodyMedium = base.bodyMedium.c(), bodySmall = base.bodySmall.c(),
    labelLarge = base.labelLarge.c(FontWeight.SemiBold), labelMedium = base.labelMedium.c(), labelSmall = base.labelSmall.c(),
)

@Composable
fun AppTheme(light: Boolean, content: @Composable () -> Unit) {
    ThemeState.light = light
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme(), typography = typo, content = content)
    }
}
