package com.lecturemanager.app.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.lecturemanager.app.ui.findActivity

/** حالة الوضع الليلي. حالة Snapshot، فأي مكوّن يقرأ الألوان أدناه يُعاد رسمه عند تغيّرها. */
object Pal { var dark by mutableStateOf(false) }

private fun pick(light: Long, dark: Long) = Color(if (Pal.dark) dark else light)

// ألوان نظام التصميم "Academic Focus" (فاتح / داكن)
val Navy: Color get() = pick(0xFF1A2542, 0xFFE2E8F0)          // لون الحبر: عناوين وحدود وأيقونات
val NavyFill: Color get() = pick(0xFF1A2542, 0xFF3B4A75)      // لون التعبئة الأساسي للأزرار والبطاقات
val Teal = Color(0xFF0D9488)
val Ochre = Color(0xFFD97706)
val Slate: Color get() = pick(0xFF64748B, 0xFF94A3B8)
val SlateLight: Color get() = pick(0xFF94A3B8, 0xFF64748B)
val Canvas: Color get() = pick(0xFFF8FAFC, 0xFF0F172A)
val CardBg: Color get() = pick(0xFFFFFFFF, 0xFF1E293B)
val Border: Color get() = pick(0xFFE2E8F0, 0xFF334155)
val InputBorder: Color get() = pick(0xFFCBD5E1, 0xFF475569)
val PdfBg: Color get() = pick(0xFFCBD5E1, 0xFF0B1220)
val TealTint: Color get() = pick(0xFFCCFBF1, 0xFF134E4A)
val TealText: Color get() = pick(0xFF0F766E, 0xFF5EEAD4)
val AmberTint: Color get() = pick(0xFFFEF3C7, 0xFF451A03)
val AmberText: Color get() = pick(0xFFB45309, 0xFFFCD34D)
val CrimsonTint: Color get() = pick(0xFFFEE2E2, 0xFF450A0A)
val CrimsonText: Color get() = pick(0xFFB91C1C, 0xFFFCA5A5)

private fun lightScheme() = lightColorScheme(
    primary = Color(0xFF1A2542), onPrimary = Color.White,
    secondary = Teal, onSecondary = Color.White,
    tertiary = Ochre, onTertiary = Color.White,
    background = Color(0xFFF8FAFC), onBackground = Color(0xFF0B1C30),
    surface = Color.White, onSurface = Color(0xFF0B1C30),
    surfaceVariant = Color(0xFFEFF4FF), onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1), outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFBA1A1A),
)

private fun darkScheme() = darkColorScheme(
    primary = Color(0xFFBBC5EB), onPrimary = Color(0xFF0F1A37),
    secondary = Color(0xFF2DD4BF), onSecondary = Color(0xFF00201D),
    tertiary = Color(0xFFFBBF24), onTertiary = Color(0xFF2F1500),
    background = Color(0xFF0F172A), onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF1E293B), onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF273449), onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569), outlineVariant = Color(0xFF334155),
    error = Color(0xFFFFB4AB),
)

// الخطوط مضمّنة في res/font: Cairo (خط متغيّر) للعناوين و Tajawal للنصوص.
@OptIn(ExperimentalTextApi::class)
private fun cairoFamily(ctx: Context): FontFamily {
    val id = ctx.resources.getIdentifier("cairo", "font", ctx.packageName)
    if (id == 0) return FontFamily.Default
    return FontFamily(
        listOf(400, 600, 700).map { w ->
            Font(id, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
        }
    )
}

private fun tajawalFamily(ctx: Context): FontFamily {
    val fonts = listOf("tajawal_regular" to FontWeight.Normal, "tajawal_medium" to FontWeight.Medium, "tajawal_bold" to FontWeight.Bold)
        .mapNotNull { (name, w) ->
            val id = ctx.resources.getIdentifier(name, "font", ctx.packageName)
            if (id != 0) Font(id, w) else null
        }
    return if (fonts.isEmpty()) FontFamily.Default else FontFamily(fonts)
}

private fun buildTypography(heading: FontFamily, body: FontFamily): Typography {
    fun t(f: FontFamily, size: Int, line: Int, w: FontWeight) =
        TextStyle(fontFamily = f, fontSize = size.sp, lineHeight = line.sp, fontWeight = w)
    return Typography(
        headlineLarge = t(heading, 24, 40, FontWeight.Bold),
        headlineMedium = t(heading, 20, 34, FontWeight.Bold),
        titleLarge = t(heading, 18, 32, FontWeight.Bold),
        titleMedium = t(heading, 16, 28, FontWeight.SemiBold),
        bodyLarge = t(body, 17, 30, FontWeight.Normal),
        bodyMedium = t(body, 15, 26, FontWeight.Normal),
        bodySmall = t(body, 13, 22, FontWeight.Normal),
        labelLarge = t(heading, 14, 24, FontWeight.SemiBold),
        labelMedium = t(heading, 12, 20, FontWeight.SemiBold),
        labelSmall = t(heading, 11, 18, FontWeight.Medium),
    )
}

private val shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp),
)

/** themeMode: 0 حسب النظام، 1 فاتح، 2 داكن. */
@Composable
fun AppTheme(themeMode: Int = 0, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val dark = when (themeMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    remember(dark) { Pal.dark = dark; dark }
    val typography = remember { buildTypography(cairoFamily(ctx), tajawalFamily(ctx)) }

    val view = LocalView.current
    SideEffect {
        val window = (view.context.findActivity() as? Activity)?.window ?: return@SideEffect
        window.statusBarColor = Canvas.toArgb()
        window.navigationBarColor = CardBg.toArgb()
        val c = WindowCompat.getInsetsController(window, view)
        c.isAppearanceLightStatusBars = !dark
        c.isAppearanceLightNavigationBars = !dark
    }
    MaterialTheme(
        colorScheme = if (dark) darkScheme() else lightScheme(),
        typography = typography, shapes = shapes, content = content,
    )
}
