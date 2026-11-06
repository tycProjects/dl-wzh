package com.android.island.island

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Small, allocation-free motion helpers shared by the expanded island and banners.
 * Every value is read in the graphics-layer phase, so none of this recomposes.
 */

/** Springy press feedback driven by an interaction source. */
@Composable
fun Modifier.pressScaleWith(source: MutableInteractionSource, pressedScale: Float = 0.86f): Modifier {
    val pressed by source.collectIsPressedAsState()
    val s = animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.46f, stiffness = Spring.StiffnessMedium),
        label = "press-scale"
    )
    return this.graphicsLayer {
        // Jelly press: the squash is stronger vertically than horizontally (like pressing a
        // soft pad) and the looser spring lets it rebound past 1.0 before settling.
        val d = 1f - s.value
        scaleX = 1f - d * 0.80f
        scaleY = 1f - d * 1.25f
    }
}

/** IconButton with a springy press-scale. */
@Composable
fun PressIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.pressScaleWith(source),
        interactionSource = source,
        content = content
    )
}

private val StaggerOvershoot = androidx.compose.animation.core.CubicBezierEasing(0.22f, 1.35f, 0.36f, 1f)

/**
 * Staggered entrance: element [index] fades/slides in during its own slice of [enter] (0..1).
 */
fun Modifier.staggerIn(enter: Animatable<Float, AnimationVector1D>, index: Int, distanceDp: Float = 10f): Modifier =
    graphicsLayer {
        val start = index * 0.15f
        val t = ((enter.value - start) / 0.55f).coerceIn(0f, 1f)
        alpha = FastOutSlowInEasing.transform(t)
        // A little overshoot on the slide gives the entrance a soft, jelly landing.
        translationY = (1f - StaggerOvershoot.transform(t)) * distanceDp * density
    }
