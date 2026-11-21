package com.android.island.island

import android.os.SystemClock
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import com.android.island.core.ScreenState
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/**
 * Two soft layered waves along the bottom edge of the expanded card (the "wave" of the dark
 * reference widget). It is drawn only while music plays: the amplitude eases in when playback
 * starts and settles flat when it pauses, so a pause is a gentle calm instead of a hard stop.
 *
 * Cost: one tiny state tick at ~30 Hz (paused when the screen is off or playback is paused) and
 * two paths of 48 segments per tick. The tick is read in the draw phase, so nothing recomposes.
 */

/**
 * One-shot light bloom for the open / close morph: a soft accent glow that grows out of the
 * capsule position (top centre) as the card unfolds, peaks half-way and is gone at both ends.
 * [progress] is read while drawing only, so the morph itself never recomposes because of it.
 */
fun Modifier.openBloom(accent: Color, progress: () -> Float): Modifier {
    // Low-End must not construct even a static radial-gradient shader for the transition.
    if (com.android.island.core.DeviceProfile.staticGlass) return this
    return this.drawWithCache {
    val r = size.width * 0.85f
    val center = Offset(size.width / 2f, 0f)
    val brush = Brush.radialGradient(
        colorStops = bloomStops(accent),
        center = center,
        radius = r
    )
    onDrawWithContent {
        drawContent()
        val p = progress().coerceIn(0f, 1f)
        val e = sin(PI.toFloat() * p)
        if (e > 0.02f) {
            scale(scale = 0.35f + 0.65f * p, pivot = center) {
                drawCircle(brush = brush, radius = r, center = center, alpha = e * 0.85f)
            }
        }
    }
    }
}


/** Eased white-to-accent-to-clear falloff for [openBloom]; many small steps, no banding rings. */
private fun bloomStops(accent: Color): Array<Pair<Float, Color>> {
    val n = 10
    return Array(n + 1) { i ->
        val t = i / n.toFloat()
        val k = 1f - t
        val fade = k * k * (3f - 2f * k)
        val mix = (t / 0.45f).coerceIn(0f, 1f)
        val base = Color(
            red = 1f + (accent.red - 1f) * mix,
            green = 1f + (accent.green - 1f) * mix,
            blue = 1f + (accent.blue - 1f) * mix
        )
        t to base.copy(alpha = 0.36f * fade)
    }
}
