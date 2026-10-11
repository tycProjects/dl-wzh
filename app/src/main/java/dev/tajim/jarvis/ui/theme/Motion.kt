package dev.tajim.jarvis.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/** Effective motion policy: user setting OR system "animations off". */
@Immutable
data class JarvisMotion(val reduced: Boolean, val intensity: Float)

val LocalJarvisMotion = staticCompositionLocalOf { JarvisMotion(reduced = false, intensity = 1f) }
