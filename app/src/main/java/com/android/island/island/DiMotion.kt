package com.android.island.island

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import kotlin.math.pow

/**
 * Port 1:1 dari Dynamic Island 1.7.2 (island/Common.kt: animStiffness, animDurationMs, islandSpec,
 * fadeSpec). Nilai bawaan referensi: durasi 35 %, elastisitas LOW (damping 0,8), gaya FRUIT.
 */
object DiMotion {
    const val DEFAULT_DURATION_PERCENT = 35
    /** Elasticity.LOW di referensi. */
    const val DEFAULT_DAMPING = 0.8f
    private const val VIDEO_STIFFNESS = 223f

    /** Stiffness dari persentase durasi: 0 % cepat, 35 % = 223 (bawaan), 100 % lambat. */
    fun stiffness(percent: Int = DEFAULT_DURATION_PERCENT): Float {
        val t = percent.coerceIn(0, 100) / 100f
        return if (t <= 0.35f) {
            1200f * (VIDEO_STIFFNESS / 1200f).pow(t / 0.35f)
        } else {
            VIDEO_STIFFNESS * (60f / VIDEO_STIFFNESS).pow((t - 0.35f) / 0.65f)
        }
    }

    /** Durasi tween (ms) untuk gaya SMOOTH: 160 + persen × 6. Bawaan = 370 ms. */
    fun durationMs(percent: Int = DEFAULT_DURATION_PERCENT): Int = 160 + percent.coerceIn(0, 100) * 6

    /** Pegas bentuk pulau (FRUIT/SPRING) dengan nilai referensi. */
    fun islandSpring(percent: Int = DEFAULT_DURATION_PERCENT, damping: Float = DEFAULT_DAMPING): FiniteAnimationSpec<Float> =
        spring(dampingRatio = damping.coerceAtMost(1f), stiffness = stiffness(percent))

    /** Fade konten: pegas dengan stiffness × 6 (fadeSpec referensi untuk gaya FRUIT). */
    fun fadeSpring(percent: Int = DEFAULT_DURATION_PERCENT): FiniteAnimationSpec<Float> =
        spring(stiffness = stiffness(percent) * 6f)
}
