package com.alaoke.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AlaokeDarkColorScheme = darkColorScheme(
    primary = AlaokeNeonBlue,
    onPrimary = Color.White,
    secondary = AlaokeNeonPink,
    onSecondary = Color.White,
    tertiary = AlaokeGold,
    background = AlaokeBackground,
    onBackground = AlaokeOnBackground,
    surface = AlaokeSurface,
    onSurface = AlaokeOnBackground,
    surfaceVariant = AlaokeSurfaceElevated,
    onSurfaceVariant = AlaokeOnSurfaceMuted,
    error = Color(0xFFFF5C5C)
)

@Composable
fun AlaokeTheme(
    // The app is dark-only by design (Platinum-videoke style), but this flag is kept
    // so a future light mode could be wired in without changing call sites.
    useSystemDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AlaokeDarkColorScheme,
        typography = AlaokeTypography,
        content = content
    )
}

/** Event types selectable in Smart Party Mode, each with its own accent + emoji. */
enum class PartyEvent(val displayName: String, val emoji: String, val accent: Color, val tag: String) {
    BIRTHDAY("Birthday", "\uD83C\uDF82", EventBirthday, "birthday"),
    WEDDING("Wedding", "\uD83D\uDC8D", EventWedding, "wedding"),
    PARTY("Party", "\uD83E\uDE85", EventParty, "party"),
    CHRISTMAS("Christmas", "\uD83C\uDF84", EventChristmas, "christmas"),
    NEW_YEAR("New Year", "\uD83C\uDF86", EventNewYear, "newyear")
}
