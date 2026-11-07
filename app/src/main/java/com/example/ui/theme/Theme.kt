package com.example.ui.theme

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.data.repository.ThemeColor

fun getJnmfColorScheme(themeColor: ThemeColor): ColorScheme {
    return when (themeColor) {
        ThemeColor.NEON_GREEN -> darkColorScheme(
            primary = NeonGreenPrimary,
            onPrimary = Color.Black,
            primaryContainer = NeonGreenSecondary,
            onPrimaryContainer = Color.Black,
            secondary = NeonGreenSecondary,
            onSecondary = Color.Black,
            background = JnmfBackgroundDark,
            onBackground = JnmfTextPrimary,
            surface = JnmfSurfaceDark,
            onSurface = JnmfTextPrimary,
            surfaceVariant = NeonGreenSurface,
            onSurfaceVariant = JnmfTextSecondary,
            outline = NeonGreenPrimary.copy(alpha = 0.5f),
            outlineVariant = JnmfDivider
        )
        ThemeColor.ELECTRIC_BLUE -> darkColorScheme(
            primary = ElectricBluePrimary,
            onPrimary = Color.Black,
            primaryContainer = ElectricBlueSecondary,
            onPrimaryContainer = Color.White,
            secondary = ElectricBlueSecondary,
            onSecondary = Color.White,
            background = JnmfBackgroundDark,
            onBackground = JnmfTextPrimary,
            surface = JnmfSurfaceDark,
            onSurface = JnmfTextPrimary,
            surfaceVariant = ElectricBlueSurface,
            onSurfaceVariant = JnmfTextSecondary,
            outline = ElectricBluePrimary.copy(alpha = 0.5f),
            outlineVariant = JnmfDivider
        )
        ThemeColor.CRIMSON_RED -> darkColorScheme(
            primary = CrimsonRedPrimary,
            onPrimary = Color.White,
            primaryContainer = CrimsonRedSecondary,
            onPrimaryContainer = Color.White,
            secondary = CrimsonRedSecondary,
            onSecondary = Color.White,
            background = JnmfBackgroundDark,
            onBackground = JnmfTextPrimary,
            surface = JnmfSurfaceDark,
            onSurface = JnmfTextPrimary,
            surfaceVariant = CrimsonRedSurface,
            onSurfaceVariant = JnmfTextSecondary,
            outline = CrimsonRedPrimary.copy(alpha = 0.5f),
            outlineVariant = JnmfDivider
        )
        ThemeColor.MODERN_WHITE -> darkColorScheme(
            primary = ModernWhitePrimary,
            onPrimary = Color.Black,
            primaryContainer = ModernWhiteSecondary,
            onPrimaryContainer = Color.Black,
            secondary = ModernWhiteSecondary,
            onSecondary = Color.Black,
            background = JnmfBackgroundDark,
            onBackground = JnmfTextPrimary,
            surface = JnmfSurfaceDark,
            onSurface = JnmfTextPrimary,
            surfaceVariant = ModernWhiteSurface,
            onSurfaceVariant = JnmfTextSecondary,
            outline = Color.White.copy(alpha = 0.45f),
            outlineVariant = JnmfDivider
        )
        ThemeColor.VIBRANT_CYAN -> darkColorScheme(
            primary = VibrantCyanPrimary,
            onPrimary = Color.Black,
            primaryContainer = VibrantCyanSecondary,
            onPrimaryContainer = Color.Black,
            secondary = VibrantCyanSecondary,
            onSecondary = Color.Black,
            background = JnmfBackgroundDark,
            onBackground = JnmfTextPrimary,
            surface = JnmfSurfaceDark,
            onSurface = JnmfTextPrimary,
            surfaceVariant = VibrantCyanSurface,
            onSurfaceVariant = JnmfTextSecondary,
            outline = VibrantCyanPrimary.copy(alpha = 0.5f),
            outlineVariant = JnmfDivider
        )
        ThemeColor.DEEP_VIOLET -> darkColorScheme(
            primary = DeepVioletPrimary,
            onPrimary = Color.White,
            primaryContainer = DeepVioletSecondary,
            onPrimaryContainer = Color.White,
            secondary = DeepVioletSecondary,
            onSecondary = Color.White,
            background = JnmfBackgroundDark,
            onBackground = JnmfTextPrimary,
            surface = JnmfSurfaceDark,
            onSurface = JnmfTextPrimary,
            surfaceVariant = DeepVioletSurface,
            onSurfaceVariant = JnmfTextSecondary,
            outline = DeepVioletPrimary.copy(alpha = 0.55f),
            outlineVariant = JnmfDivider
        )
    }
}

@Composable
fun rememberReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }
}

@Composable
fun JnmfTheme(
    themeColor: ThemeColor = ThemeColor.NEON_GREEN,
    content: @Composable () -> Unit
) {
    val targetScheme = getJnmfColorScheme(themeColor)
    val reducedMotion = rememberReducedMotionEnabled()
    val colorSpec: AnimationSpec<Color> = if (reducedMotion) {
        snap()
    } else {
        tween(durationMillis = 280, easing = FastOutSlowInEasing)
    }

    val animatedPrimary = animateColorAsState(targetScheme.primary, colorSpec, label = "jnmfPrimary").value
    val animatedOnPrimary = animateColorAsState(targetScheme.onPrimary, colorSpec, label = "jnmfOnPrimary").value
    val animatedPrimaryContainer = animateColorAsState(targetScheme.primaryContainer, colorSpec, label = "jnmfPrimaryContainer").value
    val animatedOnPrimaryContainer = animateColorAsState(targetScheme.onPrimaryContainer, colorSpec, label = "jnmfOnPrimaryContainer").value
    val animatedSecondary = animateColorAsState(targetScheme.secondary, colorSpec, label = "jnmfSecondary").value
    val animatedOnSecondary = animateColorAsState(targetScheme.onSecondary, colorSpec, label = "jnmfOnSecondary").value
    val animatedSurfaceVariant = animateColorAsState(targetScheme.surfaceVariant, colorSpec, label = "jnmfSurfaceVariant").value
    val animatedOutline = animateColorAsState(targetScheme.outline, colorSpec, label = "jnmfOutline").value

    val animatedScheme = targetScheme.copy(
        primary = animatedPrimary,
        onPrimary = animatedOnPrimary,
        primaryContainer = animatedPrimaryContainer,
        onPrimaryContainer = animatedOnPrimaryContainer,
        secondary = animatedSecondary,
        onSecondary = animatedOnSecondary,
        surfaceVariant = animatedSurfaceVariant,
        outline = animatedOutline
    )

    MaterialTheme(
        colorScheme = animatedScheme,
        typography = Typography,
        content = content
    )
}
