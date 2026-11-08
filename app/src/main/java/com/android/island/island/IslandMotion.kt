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
    val speed = settings.animationSpeed.coerceIn(0.5f, 2f)
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
    val stiffness = (BASE_STIFFNESS * speed).coerceIn(180f, 780f)
    // Higher damping reduces bounce/stutter while still feeling responsive.
    val damping = when (settings.elasticity) {
        Elasticity.NONE -> 1f
        Elasticity.LOW -> 0.96f
        Elasticity.MEDIUM -> 0.90f
        Elasticity.HIGH -> 0.82f
    }
    if (settings.animationOption == AnimationOption.OPTION_3) {
        // Jelly profile: low-damping spring. The overshoot is rendered as a squash-and-stretch
        // on the panel (width opens first, height follows) and capped in IslandRoot.
        return when (settings.expandStyle) {
            ExpandStyle.INSTANT -> snap()
            ExpandStyle.SMOOTH -> tween(
                durationMillis = (440 / speed).toInt().coerceIn(260, 560),
                easing = CubicBezierEasing(0.34f, 1.45f, 0.64f, 1f)
            )
            ExpandStyle.SPRING -> spring(
                // Lower damping than before (0.56 -> 0.50 at HIGH) and the overshoot is no longer
                // flattened in IslandRoot, so the jelly wobble is actually visible now.
                dampingRatio = when (settings.elasticity) {
                    Elasticity.NONE -> 0.86f
                    Elasticity.LOW -> 0.74f
                    Elasticity.MEDIUM -> 0.66f
                    Elasticity.HIGH -> 0.58f
                },
                stiffness = (340f * speed).coerceIn(200f, 600f)
            )
        }
    }
    if (settings.animationOption == AnimationOption.OPTION_2) {
        // Flow profile: a softer, longer spring gives the shape a continuous liquid settle.
        // It still animates the same scalar so Option 1 remains fully unchanged.
        return when (settings.expandStyle) {
            ExpandStyle.INSTANT -> snap()
            ExpandStyle.SMOOTH -> tween(
                durationMillis = (520 / speed).toInt().coerceIn(380, 680),
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

/** Short spring for swipe-dismiss settle / snap-back. */
fun dismissSpring(): FiniteAnimationSpec<Float> = spring(
    dampingRatio = 0.92f,
    stiffness = 250f
)


/**
 * Closing curve (expanded -> capsule).
 *
 * Option 1 keeps its crisp tween. Flow / Jelly use a (nearly) critically damped spring instead:
 * it is velocity-continuous when the user taps again mid-close, decelerates for longer (no more
 * "snaps shut" feeling) and its undershoot is < 0.1 %, so the panel is never torn down early.
 */
fun islandCloseSpring(settings: IslandSettings): FiniteAnimationSpec<Float> {
    val speed = settings.animationSpeed.coerceIn(0.5f, 2f)
    if (settings.expandStyle == ExpandStyle.INSTANT) return snap()
    // Spring on Flow/Jelly in every performance mode (Low Power and Ultra are the defaults).
    // A spring keeps its velocity if the user re-opens mid-close; a tween restarts from rest and stutters.
    if (settings.expandStyle == ExpandStyle.SPRING) {
        return spring(
            dampingRatio = 1f,
            stiffness = (340f * speed).coerceIn(200f, 600f)
        )
    }
    return tween(
        durationMillis = (if (settings.animationOption == AnimationOption.OPTION_1) 430 else 500).let { (it / speed).toInt() }.coerceIn(300, 660),
        easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    )
}

// ---------------------------------------------------------------------------------------------
// Banner "tuck into capsule" (swipe-up dismiss AND timeout share exactly the same motion so the
// two paths never look different). e = 0 -> card in place, e = 1 -> card absorbed by the capsule.
// ---------------------------------------------------------------------------------------------
const val BANNER_TUCK_MS = 420
/** Soft start, long glide at the end: reads as being drawn into the notch instead of cut off. */
val BannerTuckEasing = CubicBezierEasing(0.30f, 0f, 0.15f, 1f)

fun tuckScaleX(e: Float): Float = 1f - 0.54f * e
fun tuckScaleY(e: Float): Float = 1f - 0.70f * e
/** Fades only in the later part so the shrinking card is still readable while it travels. */
fun tuckAlpha(e: Float): Float {
    val t = ((e - 0.28f) / 0.72f).coerceIn(0f, 1f)
    return 1f - t * t * (3f - 2f * t)
}

/** Banner entrance: a little springy, it drops out of the capsule and settles without a hard stop. */
fun bannerEnterSpring(): FiniteAnimationSpec<Float> = spring(dampingRatio = 0.78f, stiffness = 320f)

/** Banner entrance progress (0 = still inside the capsule, 1 = in place) from the legacy -26 dp offset channel. */
fun bannerEnterT(offsetDp: Float): Float = (1f - offsetDp / -26f).coerceIn(0f, 1.04f)
