package com.android.island.island

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import com.android.island.core.ExpandStyle
import com.android.island.core.Elasticity
import com.android.island.data.IslandSettings
import com.android.island.data.AnimationOption

/**
 * Single consistent motion system for the entire island.
 * Tuned for natural iOS-like settle: medium stiffness, damping that avoids
 * visible stutter on both high and low refresh-rate panels.
 */
val ExpoOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/** iOS "emphasized" curve: quick departure, long soft landing. Used for every non-spring fade/slide. */
val IosEase = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/** Smoothstep on [edge0, edge1]. Shared by the morph choreography so every fade has the same S-curve. */
fun smooth(x: Float, edge0: Float, edge1: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** Base stiffness tuned for a soft, physical settle (iOS-like). */
private const val BASE_STIFFNESS = 370f

fun islandSpring(settings: IslandSettings, reducedMotion: Boolean = false): FiniteAnimationSpec<Float> {
    // Performance profiles may reduce decorative effects, but must not shorten
    // the core capsule transition to a visibly abrupt 145ms snap. Keep a smooth,
    // deterministic motion curve in Low Power / Ultra as well.
    // Floor 0.05x: very slow but still a finite animation (0 or negative would freeze or reverse it).
    val speed = settings.animationSpeed.coerceIn(0.05f, 2f)
    if (reducedMotion) {
        if (settings.animationOption == AnimationOption.OPTION_3) {
            // Jelly without overshoot: same silhouette, no spring energy.
            return tween(durationMillis = (300 / speed).toInt().coerceIn(200, 380), easing = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f))
        }
        return if (settings.animationOption == AnimationOption.OPTION_2) {
            tween(durationMillis = (285 / speed).toInt().coerceIn(190, 360), easing = CubicBezierEasing(0.18f, 0.78f, 0.22f, 1.0f))
        } else {
            tween(durationMillis = 235, easing = FastOutSlowInEasing)
        }
    }
    val stiffness = (BASE_STIFFNESS * speed).coerceIn(1f, 900f)
    // Higher damping reduces bounce/stutter while still feeling responsive.
    val damping = when (settings.elasticity) {
        Elasticity.NONE -> 1f
        Elasticity.LOW -> 0.90f
        Elasticity.MEDIUM -> 0.84f
        Elasticity.HIGH -> 0.78f
    }
    if (settings.animationOption == AnimationOption.OPTION_3) {
        // Jelly profile: low-damping spring. The overshoot is rendered as a squash-and-stretch
        // on the panel (width opens first, height follows) and capped in IslandRoot.
        return when (settings.expandStyle) {
            ExpandStyle.INSTANT -> snap()
            ExpandStyle.SMOOTH -> tween(
                // Durasi dari referensi: 160 + persen × 6 ms (370 ms pada 35 %).
                durationMillis = DiMotion.durationMs(durationPercentFromSpeed(speed)),
                easing = CubicBezierEasing(0.34f, 1.45f, 0.64f, 1f)
            )
            ExpandStyle.SPRING -> spring(
                // Lower damping = bigger, livelier overshoot. Shared table, see jellyDamping().
                // Bounce tetap dari jellyDamping; yang diambil dari referensi Dynamic Island adalah
                // kecepatannya (stiffness dari DiMotion, bawaan 223 pada 35 %).
                dampingRatio = jellyDamping(settings.elasticity),
                stiffness = DiMotion.stiffness(durationPercentFromSpeed(speed))
            )
        }
    }
    if (settings.animationOption == AnimationOption.OPTION_2) {
        // Flow profile: a softer, longer spring gives the shape a continuous liquid settle.
        // It still animates the same scalar so Option 1 remains fully unchanged.
        return when (settings.expandStyle) {
            ExpandStyle.INSTANT -> snap()
            ExpandStyle.SMOOTH -> tween(
                durationMillis = (560 / speed).toInt().coerceIn(320, 720),
                easing = CubicBezierEasing(0.20f, 0.82f, 0.20f, 1.0f)
            )
            ExpandStyle.SPRING -> spring(
                dampingRatio = when (settings.elasticity) {
                    Elasticity.NONE -> 0.94f
                    Elasticity.LOW -> 0.88f
                    Elasticity.MEDIUM -> 0.80f
                    Elasticity.HIGH -> 0.72f
                },
                stiffness = (340f * speed).coerceIn(180f, 620f)
            )
        }
    }
    return when (settings.expandStyle) {
        ExpandStyle.INSTANT -> snap()
        ExpandStyle.SMOOTH -> tween(
            durationMillis = (460 / speed).toInt().coerceIn(320, 620),
            easing = FastOutSlowInEasing
        )
        ExpandStyle.SPRING -> spring(
            dampingRatio = damping,
            stiffness = stiffness
        )
    }
}

/**
 * Jelly spring damping per elasticity: the single source for both the spring and the morph path, so
 * they cannot drift apart. HIGH gives about 20 % overshoot (zeta 0.46), which stays inside the 25 %
 * the window slack and the IslandRoot scale clamp reserve for it.
 */
/** Kecepatan slider (0,5 = bawaan) dipetakan ke persen durasi referensi: 0,5 -> 35 %, makin cepat makin kecil. */
internal fun durationPercentFromSpeed(speed: Float): Int =
    (35f + (0.5f - speed) * 46.7f).toInt().coerceIn(0, 100)

fun jellyDamping(elasticity: Elasticity): Float = when (elasticity) {
    Elasticity.NONE -> 0.86f
    Elasticity.LOW -> 0.70f
    Elasticity.MEDIUM -> 0.60f
    Elasticity.HIGH -> 0.46f
}

/**
 * Dedicated capsule morph motion. Opening gets a slightly softer acceleration;
 * closing is a little more decisive so the capsule settles cleanly without a
 * visible one-second after-flicker. The normal iOS/Flow presets are critically
 * damped (no progress overshoot); Jelly keeps only a restrained, intentional
 * overshoot for its squash-and-stretch treatment.
 */
fun islandMorphSpring(settings: IslandSettings, opening: Boolean): FiniteAnimationSpec<Float> {
    val speed = settings.animationSpeed.coerceIn(0.05f, 2f)
    return when (settings.expandStyle) {
        ExpandStyle.INSTANT -> snap()
        ExpandStyle.SMOOTH -> tween(
            // STAGE 72: the morph runs slightly longer (560/520 base) and lands softer, per the
            // user's request for a smoother, a touch slower capsule↔panel transition.
            durationMillis = ((if (opening) 560f else 520f) / speed).toInt().coerceIn(280, 680),
            easing = if (settings.animationOption == AnimationOption.OPTION_3) {
                CubicBezierEasing(0.30f, 1.12f, 0.34f, 1f)
            } else {
                CubicBezierEasing(0.22f, 0.78f, 0.20f, 1f)
            }
        )
        ExpandStyle.SPRING -> spring(
            // STAGE 72: softer stiffness (300/320) = a longer, more liquid settle. Jelly keeps
            // its restrained overshoot; other presets stay critically damped.
            dampingRatio = if (settings.animationOption == AnimationOption.OPTION_3) {
                jellyDamping(settings.elasticity)
            } else Spring.DampingRatioNoBouncy,
            stiffness = ((if (settings.animationOption == AnimationOption.OPTION_3) 300f else 320f) * speed)
                .coerceIn(160f, 640f),
            visibilityThreshold = 0.0005f
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Banner "tuck into capsule" (swipe-up dismiss AND timeout share exactly the same motion so the
// two paths never look different). e = 0 -> card in place, e = 1 -> card absorbed by the capsule.
// ---------------------------------------------------------------------------------------------
const val BANNER_TUCK_MS = 460
/** Soft start, long glide at the end: reads as being drawn into the notch instead of cut off. */
val BannerTuckEasing = CubicBezierEasing(0.30f, 0f, 0.15f, 1f)

fun tuckScaleX(e: Float): Float = (1f - 0.54f * e).coerceIn(0.46f, 1f)
fun tuckScaleY(e: Float): Float = (1f - 0.70f * e).coerceIn(0.30f, 1f)
/** Fades only in the later part so the shrinking card is still readable while it travels. */
fun tuckAlpha(e: Float): Float {
    val t = ((e - 0.28f) / 0.72f).coerceIn(0f, 1f)
    return 1f - t * t * (3f - 2f * t)
}

/**
 * Banner entrance: a springy drop out of the capsule that settles with one soft rebound.
 * Damping 0.72 keeps the overshoot inside the ~4 % the bannerEnterT/scale mapping clamps
 * (1.04 / scale 1.008), so the bounce is visible as jelly but can never push the card into
 * the capsule above or past its own resting geometry.
 */
/** Masuk banner: pegas referensi Dynamic Island (damping 0,8, stiffness 223). */
fun bannerEnterSpring(): FiniteAnimationSpec<Float> = DiMotion.islandSpring()

/** Banner entrance progress (0 = still inside the capsule, 1 = in place) from the legacy -26 dp offset channel. */
fun bannerEnterT(offsetDp: Float): Float = (1f - offsetDp / -26f).coerceIn(0f, 1.04f)
