package com.android.island.island

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Small, allocation-free motion helpers shared by the expanded island and banners.
 * Every value is read in the graphics-layer / draw phase, so none of this recomposes.
 */

/** Springy press feedback driven by an interaction source. */
@Composable
fun Modifier.pressScaleWith(source: MutableInteractionSource, pressedScale: Float = 0.88f): Modifier {
    val pressed by source.collectIsPressedAsState()
    val s = animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        // Slightly firmer than before (0.46 -> 0.58): still rebounds past 1.0 but settles in ~1 bounce.
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 520f),
        label = "press-scale"
    )
    return this.graphicsLayer {
        // Jelly press: the squash is stronger vertically than horizontally (like pressing a
        // soft pad) and the looser spring lets it rebound past 1.0 before settling.
        val d = 1f - s.value
        scaleX = 1f - d * 0.90f
        scaleY = 1f - d * 1.05f
    }
}

/**
 * Soft round halo that blooms behind a control while it is held and fades out slowly after the
 * finger lifts. Drawn only (no recomposition); gives every button a small "light" reaction.
 */
@Composable
fun Modifier.pressHalo(source: MutableInteractionSource, color: Color): Modifier {
    val pressed by source.collectIsPressedAsState()
    val p = animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = if (pressed) {
            spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)
        } else {
            tween<Float>(durationMillis = 320, easing = FastOutSlowInEasing)
        },
        label = "press-halo"
    )
    return this.drawBehind {
        val v = p.value
        if (v > 0.01f) {
            drawCircle(
                color = color.copy(alpha = color.alpha * v),
                radius = size.minDimension / 2f * (0.72f + 0.28f * v)
            )
        }
    }
}

/** IconButton with a springy press-scale and an optional press halo. */
@Composable
fun PressIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    haloColor: Color? = null,
    content: @Composable () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val base = if (haloColor != null) modifier.pressHalo(source, haloColor) else modifier
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = base.pressScaleWith(source),
        interactionSource = source,
        content = content
    )
}

private val StaggerOvershoot = CubicBezierEasing(0.22f, 1.18f, 0.36f, 1f)

/**
 * Staggered entrance: element [index] fades, lifts and scales in during its own slice of [enter]
 * (0..1). With the defaults up to 8 elements (index 0..7) finish exactly when [enter] reaches 1.
 * A little overshoot on the lift gives the entrance a soft, jelly landing.
 */
fun Modifier.staggerIn(
    enter: Animatable<Float, AnimationVector1D>,
    index: Int,
    distanceDp: Float = 10f,
    step: Float = 0.07f,
    span: Float = 0.46f,
    fromScale: Float = 0.92f
): Modifier =
    graphicsLayer {
        val start = index * step
        val t = ((enter.value - start) / span).coerceIn(0f, 1f)
        alpha = FastOutSlowInEasing.transform(t)
        val k = StaggerOvershoot.transform(t)
        translationY = (1f - k) * distanceDp * density
        val s = fromScale + (1f - fromScale) * k
        scaleX = s
        scaleY = s
    }

// ---------------------------------------------------------------------------------------------
// Stage 68 "alive" details: a one-shot entrance pop and a cheap breathing aura. Both run in the
// graphics/draw phase only, have no loops of their own (the caller passes a shared clock) and
// are skipped entirely on the static-glass / reduced-motion paths, so RAM, battery and the
// frame budget never notice them.
// ---------------------------------------------------------------------------------------------

/**
 * One-shot entrance for content that appears without a parent cascade (chat bubbles, hints):
 * fades, lifts and un-squashes into place with a single soft spring. Runs once per [key];
 * no infinite animation, nothing to clean up.
 */
@Composable
fun Modifier.enterOnce(
    key: Any? = null,
    enabled: Boolean = true,
    distanceDp: Float = 8f,
    fromScale: Float = 0.94f
): Modifier {
    val anim = remember(key) { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(key, enabled) {
        if (enabled && anim.value < 1f) {
            anim.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 340f))
        }
    }
    return graphicsLayer {
        val k = anim.value.coerceIn(0f, 1f)
        alpha = FastOutSlowInEasing.transform(k)
        translationY = (1f - k) * distanceDp * density
        val s = fromScale + (1f - fromScale) * k
        scaleX = s
        scaleY = s
    }
}

/**
 * STAGE 72: constant soft aura (glow biasa) around a card — three drawn outline layers at a
 * fixed near-peak brightness. The breathing pulse is retired per the user's request; this also
 * removes the banner drift clocks, so a resting banner costs zero wakeups. Drawn behind the
 * content (the outside part is visible, the inside part hides under the glass), so nothing ever
 * overlaps layout or leaves the caller in charge of clipping.
 */
fun Modifier.staticAura(
    color: Color,
    maxAlpha: Float = 0.34f,
    spreadDp: Float = 5f,
    cornerRadiusPx: Float = 0f
): Modifier = drawBehind {
    val b = 0.92f
    val w = size.width
    val h = size.height
    val spreadPx = spreadDp * density
    var layer = 1
    while (layer <= 3) {
        val t = layer / 3f
        val spread = spreadPx * t
        val alpha = maxAlpha * (1f - t) * b
        if (alpha > 0.01f) {
            val corner = if (cornerRadiusPx > 0f) CornerRadius(cornerRadiusPx + spread) else CornerRadius((h + 2f * spread) / 2f)
            drawRoundRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(-spread, -spread),
                size = Size(w + 2f * spread, h + 2f * spread),
                cornerRadius = corner,
                style = Stroke(width = 2.2f)
            )
        }
        layer++
    }
}
