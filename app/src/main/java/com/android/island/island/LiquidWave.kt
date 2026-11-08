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
@Composable
fun LiquidWaveFooter(
    playing: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val run = playing && enabled
    val amp = animateFloatAsState(
        targetValue = if (run) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
        label = "wave-amp"
    )
    val phase = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(run) {
        if (!run) return@LaunchedEffect
        var last = SystemClock.uptimeMillis()
        while (true) {
            ScreenState.awaitInteractive()
            val now = SystemClock.uptimeMillis()
            val dt = (now - last).coerceIn(0L, 100L)
            last = now
            phase.floatValue = (phase.floatValue + dt * 0.0028f) % (2f * PI.toFloat())
            // ~30 Hz on purpose: a delay() loop does not wake Choreographer on every vsync.
            delay(com.android.island.core.DeviceProfile.tickMs)
        }
    }
    val back = remember { Path() }
    val front = remember { Path() }
    Canvas(modifier) {
        val a = amp.value
        if (a < 0.01f) return@Canvas
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val ph = phase.floatValue
        val twoPi = 2f * PI.toFloat()
        val steps = 48

        fun build(path: Path, shift: Float, cycles: Float, amplitude: Float, baseline: Float) {
            path.reset()
            path.moveTo(0f, h)
            for (i in 0..steps) {
                val u = i.toFloat() / steps
                // Edges taper to calm so the wave melts into the rounded corners.
                val taper = 0.25f + 0.75f * sin(PI.toFloat() * u)
                val y = baseline + sin(shift + u * cycles * twoPi) * amplitude * taper
                path.lineTo(w * u, y)
            }
            path.lineTo(w, h)
            path.close()
        }

        build(back, -ph * 0.8f + 1.7f, 1.6f, h * 0.20f * a, h * 0.62f)
        build(front, ph, 2.1f, h * 0.15f * a, h * 0.74f)
        drawPath(
            back,
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.5f to Color.White.copy(alpha = 0.16f * a),
                1f to Color.Transparent
            )
        )
        drawPath(
            front,
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.25f to color.copy(alpha = 0.50f * a),
                0.75f to color.copy(alpha = 0.50f * a),
                1f to Color.Transparent
            )
        )
    }
}

/**
 * One-shot light bloom for the open / close morph: a soft accent glow that grows out of the
 * capsule position (top centre) as the card unfolds, peaks half-way and is gone at both ends.
 * [progress] is read while drawing only, so the morph itself never recomposes because of it.
 */
fun Modifier.openBloom(accent: Color, progress: () -> Float): Modifier = drawWithCache {
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
