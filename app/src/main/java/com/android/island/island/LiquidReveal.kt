package com.android.island.island

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/**
 * Liquid content switch, modelled on the behaviour of the liquid expansion in Akira's Dynamic Island
 * (MIT-style reference only; this is our own implementation, no code copied).
 *
 * - Both the outgoing and the incoming content live during the change (AnimatedContent).
 * - The incoming content grows out of the outgoing content's width and fades in; the outgoing one
 *   shrinks toward the new width and fades out. Both use the island's own spring for position.
 * - Fading uses a stiffer, bounce-free spring so new content is visible early in the move.
 * - With [blurDp] > 0 on Android 12+, the content emerges from a blur that clears before the end.
 *
 * All per-frame values are read in the layer phase, so switching content does not recompose the island.
 */
@Composable
fun <T> LiquidReveal(
    state: T,
    contentKey: (T) -> Any,
    enabled: Boolean,
    blurDp: Float,
    shapeSpec: FiniteAnimationSpec<Float>,
    fadeSpec: FiniteAnimationSpec<Float>,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    val widths = remember { HashMap<Any, Int>() }
    val transition = updateTransition(state, label = "liquidReveal")
    val sizeSpec = remember { spring<IntSize>(dampingRatio = 0.85f, stiffness = 380f) }

    transition.AnimatedContent(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
        contentKey = contentKey,
        transitionSpec = {
            (EnterTransition.None togetherWith ExitTransition.None).using(
                SizeTransform(clip = false) { _, _ -> sizeSpec }
            )
        }
    ) { s ->
        Box(
            liquidFlow(contentKey(s), transition, contentKey, widths, enabled, blurDp, shapeSpec, fadeSpec)
        ) {
            content(s)
        }
    }
}

@Composable
private fun <T> AnimatedVisibilityScope.liquidFlow(
    key: Any,
    states: Transition<T>,
    contentKey: (T) -> Any,
    widths: HashMap<Any, Int>,
    enabled: Boolean,
    blurDp: Float,
    shapeSpec: FiniteAnimationSpec<Float>,
    fadeSpec: FiniteAnimationSpec<Float>
): Modifier {
    // The other content in this change: for the incoming one it is the outgoing one, and vice versa.
    val target = contentKey(states.targetState)
    val other = if (key == target) contentKey(states.currentState) else target
    val from = remember { arrayOfNulls<Any>(1) }
    if (other != key) from[0] = other

    // 0 = not present, 1 = fully present. Read only while drawing.
    val presence = transition.animateFloat({ shapeSpec }, label = "presence") {
        if (it == EnterExitState.Visible) 1f else 0f
    }
    val fade = transition.animateFloat({ fadeSpec }, label = "fade") {
        if (it == EnterExitState.Visible) 1f else 0f
    }
    DisposableEffect(key) { onDispose { widths.remove(key) } }

    val blurRadius = if (enabled) blurDp else 0f
    return Modifier.layout { measurable, constraints ->
        val content = measurable.measure(constraints)
        widths[key] = content.width
        layout(content.width, content.height) {
            content.placeWithLayer(0, 0) {
                val p = presence.value
                alpha = fade.value.coerceIn(0f, 1f)
                val otherWidth = from[0]?.let { widths[it] }
                if (enabled && otherWidth != null && content.width > 0) {
                    val k = (otherWidth.toFloat() / content.width).coerceIn(0.25f, 3f)
                    scaleX = k + (1f - k) * p
                    scaleY = scaleX
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
                // The blur is strongest at the start and clears before the end of the move.
                val rest = (1f - p).coerceAtLeast(0f)
                blurLayer(blurRadius * rest * sqrt(rest))
            }
        }
    }
}

/** Blur of the layer by [dp] on Android 12+, and only when it is visible. */
private fun GraphicsLayerScope.blurLayer(dp: Float) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dp > 0.3f) {
        val px = dp.dp.toPx()
        renderEffect = BlurEffect(px, px, TileMode.Decal)
    }
}
