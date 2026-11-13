package com.meloza.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * "Liquid glass" surface: translucent tint, a light-catching rim that is brighter
 * at the top, and a soft inner sheen. Android has no native liquid-glass API, so this
 * is an approximation; it does not refract the content behind it.
 */
fun Modifier.glass(shape: Shape, tint: Color = Color(0xFF2A2140)): Modifier {
    val rim = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.22f)),
    )
    val sheen = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
    )
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.82f), tint.copy(alpha = 0.66f))))
        .background(sheen)
        .border(1.dp, rim, shape)
}

/**
 * Springy press feedback. Pair with a clickable that uses the same [source].
 */
@Composable
fun Modifier.pressScale(source: MutableInteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "press",
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * Background that matches the current cover: the artwork is blurred, darkened, and
 * cross-fades when the track changes.
 */
@Composable
fun LiquidBackdrop(artwork: Any?, modifier: Modifier = Modifier) {
    Crossfade(targetState = artwork, animationSpec = tween(durationMillis = 900), label = "backdrop") { model ->
        Box(modifier.fillMaxSize()) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = 1.25f; scaleY = 1.25f }
                    .blur(90.dp),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0x66000000), Color(0xE6000000))),
                ),
            )
        }
    }
}
