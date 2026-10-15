package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = DarkPrimary,
  onPrimary = Color.White,
  primaryContainer = DarkSurfaceVariant,
  onPrimaryContainer = Color.White,
  secondary = BrandLightBlue,
  onSecondary = BrandPrimary,
  secondaryContainer = DarkSurfaceVariant,
  onSecondaryContainer = Color.White,
  background = DarkBackground,
  onBackground = DarkTextPrimary,
  surface = DarkSurface,
  onSurface = DarkTextPrimary,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkTextSecondary,
  error = BrandDanger,
  onError = Color.White,
  outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
  primary = BrandPrimary,
  onPrimary = Color.White,
  primaryContainer = BrandLightBlue,
  onPrimaryContainer = BrandPrimaryVariant,
  secondary = BrandLightBlue,
  onSecondary = BrandPrimary,
  secondaryContainer = BrandNeutralGray,
  onSecondaryContainer = BrandTextDark,
  background = BrandBackgroundLight,
  onBackground = BrandTextDark,
  surface = Color.White,
  onSurface = BrandTextDark,
  surfaceVariant = BrandNeutralGray,
  onSurfaceVariant = BrandTextSecondary,
  error = BrandDanger,
  onError = Color.White,
  outline = BrandBorder
)

@Composable
fun EduPlatformTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
