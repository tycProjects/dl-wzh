package com.touchflux.app.fx

enum class EffectType(val label: String) {
    RIPPLE("Water"),
    ORB("Orbs"),
    LIGHTNING("Lightning"),
    NOVA("Nova"),
    RANDOM("Surprise me")
}

enum class Palette(val label: String, val colors: IntArray, val rainbow: Boolean = false) {
    AQUA("Aqua", intArrayOf(0xFF22D3EE.toInt(), 0xFF3B82F6.toInt(), 0xFF67E8F9.toInt())),
    NEON("Neon", intArrayOf(0xFFFF2BD6.toInt(), 0xFF7C3AED.toInt(), 0xFF22D3EE.toInt())),
    EMBER("Ember", intArrayOf(0xFFFF5A1F.toInt(), 0xFFFFB020.toInt(), 0xFFFF2D55.toInt())),
    TOXIC("Toxic", intArrayOf(0xFF84FF3A.toInt(), 0xFF10E0A0.toInt(), 0xFFE5FF3A.toInt())),
    SUNSET("Sunset", intArrayOf(0xFFFF7AB6.toInt(), 0xFFFFB86B.toInt(), 0xFFA78BFA.toInt())),
    RAINBOW(
        "Rainbow",
        intArrayOf(0xFFFF4D4D.toInt(), 0xFFFFD93D.toInt(), 0xFF3DDC97.toInt(), 0xFF4D8DFF.toInt()),
        rainbow = true
    ),
    MONO("Mono", intArrayOf(0xFFFFFFFF.toInt(), 0xFFBFC3CA.toInt(), 0xFF777D86.toInt()))
}

/** Immutable snapshot of every user-tweakable setting. */
data class FxConfig(
    val effect: EffectType = EffectType.NOVA,
    val palette: Palette = Palette.MONO,
    /** 0.6 .. 2.0 – overall size of the effect. */
    val size: Float = 1f,
    /** 0.6 .. 2.0 – how long the effect lives. */
    val duration: Float = 1f,
    /** 0.5 .. 2.0 – amount of rings / particles / bolts. */
    val intensity: Float = 1f,
    val haptics: Boolean = true,
    val trails: Boolean = true,
    val geometry: Boolean = true
) {
    companion object {
        val DEFAULT = FxConfig()
    }
}
