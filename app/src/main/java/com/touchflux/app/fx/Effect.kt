package com.touchflux.app.fx

import android.graphics.Canvas
import kotlin.random.Random

/** Everything an effect needs to know about the touch that spawned it. */
class Spec(
    val x: Float,
    val y: Float,
    /** Pixels per "design dp", already multiplied by the user's size setting. */
    val unit: Float,
    /** Lifetime in seconds. */
    val duration: Float,
    val color: Int,
    val intensity: Float,
    /** True for drag-trail puffs: smaller, shorter, cheaper. */
    val small: Boolean,
    val rng: Random
)

abstract class Effect(spec: Spec) {
    protected val s: Spec = spec

    var age = 0f
        private set

    val dead: Boolean get() = age >= s.duration

    /** Normalised progress 0..1. */
    val t: Float get() = Ease.clamp01(age / s.duration)

    open fun update(dt: Float) {
        age += dt
    }

    abstract fun draw(c: Canvas)
}
