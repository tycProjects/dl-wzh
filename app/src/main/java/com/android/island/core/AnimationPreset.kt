package com.android.island.core

import com.android.island.data.AnimationOption
import com.android.island.data.IslandSettings
import kotlin.math.abs

/**
 * Paket animasi siap pakai. Satu pilihan mengatur gaya, elastisitas, kecepatan, dan sapuan kaca
 * sekaligus. Semua memakai pegas (tanpa blur), jadi tetap ringan.
 */
enum class AnimationPreset(
    val label: String,
    val option: AnimationOption,
    val style: ExpandStyle,
    val elasticity: Elasticity,
    val speed: Float,
    val glassSweep: Boolean
) {
    /** Rekomendasi: pegas kritis seperti iOS. Membuka dan menutup tanpa pantulan, tanpa blur. */
    IOS("iOS Ringan", AnimationOption.OPTION_2, ExpandStyle.SPRING, Elasticity.LOW, 0.9f, false),
    SOFT("Lembut", AnimationOption.OPTION_2, ExpandStyle.SPRING, Elasticity.LOW, 0.7f, true),
    /** Rekomendasi: jelly dengan pantulan penuh, tetapi kecepatan sedikit lebih tenang. */
    JELLY("Jelly Halus", AnimationOption.OPTION_3, ExpandStyle.SPRING, Elasticity.HIGH, 0.8f, true),
    STABLE("Stabil", AnimationOption.OPTION_1, ExpandStyle.SPRING, Elasticity.NONE, 1f, false);

    fun applyTo(s: IslandSettings): IslandSettings = s.copy(
        animationOption = option,
        expandStyle = style,
        elasticity = elasticity,
        animationSpeed = speed,
        glassSweepEnabled = glassSweep
    )

    fun matches(s: IslandSettings): Boolean =
        s.animationOption == option && s.expandStyle == style &&
            s.elasticity == elasticity && abs(s.animationSpeed - speed) < 0.01f

    companion object {
        val recommended: AnimationPreset = IOS
    }
}
