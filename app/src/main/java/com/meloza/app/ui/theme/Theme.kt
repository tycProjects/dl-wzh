package com.meloza.app.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.LaunchedEffect

/** Brand seed colors from the Meloza logo: violet, lilac, blue. */
private val Violet = Color(0xFF7C4DFF)
private val Lilac = Color(0xFFC58AF9)
private val Blue = Color(0xFF448AFF)

/** Current accent taken from the playing album art (null = brand colors). */
class ArtworkAccent {
    var seed by mutableStateOf<Color?>(null)
}

/**
 * Extracts a vibrant swatch from album artwork. Runs off the main thread.
 * Returns null when no artwork is available, so the brand palette is used.
 */
suspend fun extractAccent(bitmap: Bitmap?): Color? = withContext(Dispatchers.Default) {
    if (bitmap == null) return@withContext null
    val palette = Palette.from(bitmap).maximumColorCount(16).generate()
    val swatch = palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch
    swatch?.rgb?.let { Color(it) }
}

private fun schemeFrom(seed: Color?, dark: Boolean): ColorScheme {
    val primary = seed ?: Violet
    return if (dark) {
        darkColorScheme(
            primary = lighten(primary, 0.35f),
            onPrimary = Color(0xFF1A1030),
            primaryContainer = darken(primary, 0.55f),
            onPrimaryContainer = lighten(primary, 0.8f),
            secondary = Lilac,
            onSecondary = Color(0xFF2A1140),
            secondaryContainer = Color(0xFF3A2650),
            onSecondaryContainer = Color(0xFFEDDCFF),
            tertiary = Blue,
            background = Color(0xFF0E0B16),
            surface = Color(0xFF0E0B16),
            surfaceContainer = Color(0xFF191426),
            surfaceContainerHigh = Color(0xFF211B31),
            surfaceContainerHighest = Color(0xFF2B2440),
            onSurface = Color(0xFFEDE7F6),
            onBackground = Color(0xFFEDE7F6),
        )
    } else {
        lightColorScheme(
            primary = darken(primary, 0.1f),
            onPrimary = Color.White,
            primaryContainer = lighten(primary, 0.75f),
            onPrimaryContainer = darken(primary, 0.6f),
            secondary = Lilac,
            tertiary = Blue,
            surface = Color(0xFFFBF8FF),
            surfaceContainer = Color(0xFFF2EDF9),
            surfaceContainerHigh = Color(0xFFECE6F5),
            surfaceContainerHighest = Color(0xFFE6E0EF),
        )
    }
}

private fun lighten(c: Color, f: Float) = Color(
    red = c.red + (1f - c.red) * f, green = c.green + (1f - c.green) * f,
    blue = c.blue + (1f - c.blue) * f, alpha = 1f,
)

private fun darken(c: Color, f: Float) = Color(
    red = c.red * (1f - f), green = c.green * (1f - f), blue = c.blue * (1f - f), alpha = 1f,
)

private val MelozaTypography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 40.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
)

@Composable
fun MelozaTheme(accent: Color?, darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = when {
        // Material You: wallpaper-based colors on Android 12+ when no album accent is playing.
        accent == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> schemeFrom(accent, darkTheme)
    }
    MaterialTheme(colorScheme = scheme, typography = MelozaTypography, content = content)
}
