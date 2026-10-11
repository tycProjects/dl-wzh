package dev.tajim.jarvis.ui.orb

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import dev.tajim.jarvis.core.PerformanceHints
import dev.tajim.jarvis.ui.theme.LocalJarvisMotion

/**
 * The ONE frame loop for everything animated on Home (Orb, connectors, pulses).
 * Consumers read [time] and [quality] only inside draw lambdas, so a tick redraws two canvases
 * and never recomposes anything.
 */
@Stable
class AnimationClock {
    /** Seconds of animation, already multiplied by the user's intensity. Stays 0 when motion is off. */
    var time by mutableFloatStateOf(0f)
        private set

    /** 0.4..1.0 particle-quality factor, lowered automatically when frames run long. */
    var quality by mutableFloatStateOf(1f)
        private set

    suspend fun run(capped: Boolean, speed: Float) {
        var last = 0L
        var acc = 0f
        var ema = 0.016f
        // 30 fps cap: accumulate until ~30 ms (threshold below 33.3 ms so 60 Hz devices land on every 2nd frame).
        val interval = if (capped) 0.030f else 0f
        if (capped) quality = minOf(quality, 0.6f)
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.1f)
                    acc += dt
                    if (!capped) {
                        // Only measure when we are not throttling ourselves, otherwise skipped frames look "fast".
                        ema = ema * 0.9f + dt * 0.1f
                        quality = when {
                            ema > 0.026f -> (quality - 0.02f).coerceAtLeast(0.4f)
                            ema < 0.019f -> (quality + 0.005f).coerceAtMost(1f)
                            else -> quality
                        }
                    }
                    if (acc >= interval) {
                        time += acc * speed
                        acc = 0f
                    }
                }
                last = now
            }
        }
    }
}

/**
 * Creates the clock and runs it only while [active] and motion is allowed.
 * No loop at all (zero per-frame work) when inactive or when reduced motion is on.
 */
@Composable
fun rememberAnimationClock(active: Boolean): AnimationClock {
    val motion = LocalJarvisMotion.current
    val context = LocalContext.current
    val clock = remember { AnimationClock() }
    LaunchedEffect(active, motion.reduced, motion.intensity) {
        if (!active || motion.reduced) return@LaunchedEffect
        val capped = PerformanceHints.shouldCapFrameRate(context) || motion.intensity <= 0.5f
        clock.run(capped, motion.intensity)
    }
    return clock
}
