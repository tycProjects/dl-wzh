package com.touchflux.app.fx

import kotlin.math.pow

/** Small collection of easing helpers. All inputs/outputs are normalised to 0..1. */
object Ease {
    fun clamp01(v: Float): Float = if (v < 0f) 0f else if (v > 1f) 1f else v

    fun outQuad(t: Float): Float = 1f - (1f - t) * (1f - t)

    fun outCubic(t: Float): Float {
        val k = 1f - t
        return 1f - k * k * k
    }

    fun outExpo(t: Float): Float = if (t >= 1f) 1f else 1f - 2f.pow(-10f * t)

    fun smooth(t: Float): Float {
        val k = clamp01(t)
        return k * k * (3f - 2f * k)
    }

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}
