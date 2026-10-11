package dev.tajim.jarvis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import dev.tajim.jarvis.data.ThemeMode

@Composable
fun JarvisTheme(
    themeMode: ThemeMode,
    reducedMotion: Boolean,
    animationIntensity: Float,
    content: @Composable () -> Unit,
) {
    val colors = if (themeMode == ThemeMode.DARK) DarkJarvisColors else LightJarvisColors
    val scheme = if (colors.isDark) {
        darkColorScheme(
            primary = colors.cyan, onPrimary = Color(0xFF00131A),
            secondary = colors.violet, tertiary = colors.magenta,
            background = colors.backgroundBottom, onBackground = colors.textPrimary,
            surface = colors.backgroundTop, onSurface = colors.textPrimary,
            onSurfaceVariant = colors.textSecondary, outline = colors.borderStart,
            error = colors.error,
        )
    } else {
        lightColorScheme(
            primary = colors.cyan, onPrimary = Color.White,
            secondary = colors.violet, tertiary = colors.magenta,
            background = colors.backgroundBottom, onBackground = colors.textPrimary,
            surface = colors.backgroundTop, onSurface = colors.textPrimary,
            onSurfaceVariant = colors.textSecondary, outline = colors.borderStart,
            error = colors.error,
        )
    }
    CompositionLocalProvider(
        LocalJarvisColors provides colors,
        LocalJarvisMotion provides JarvisMotion(reducedMotion, animationIntensity),
    ) {
        MaterialTheme(colorScheme = scheme, typography = JarvisTypography, content = content)
    }
}
