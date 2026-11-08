package com.akira.island.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.prismalGlassEffects
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import com.styropyr0.prismal.shapes.PrismalRoundedRectangle

/** Backdrop sampled by every Dyland glass surface. */
val LocalDylandGlassBackdrop = staticCompositionLocalOf<PrismalBackdrop?> { null }

@Composable
fun rememberDylandGlassBackdrop(): PrismalBackdrop = rememberPrismalGlassLayer()

/** Capture only the decorative app background; foreground UI stays outside this layer. */
fun Modifier.dylandGlassBackdrop(backdrop: PrismalBackdrop): Modifier = prismalGlassLayer(backdrop)

/** Real backdrop glass on API 33+, RenderEffect blur on API 31–32, frosted fallback below. */
@Composable
fun Modifier.dylandLiquidGlass(
    radius: Dp = 24.dp,
    shape: Shape = RoundedCornerShape(radius),
    fallback: Color = Ui.Card,
): Modifier {
    val backdrop = LocalDylandGlassBackdrop.current
    val density = LocalDensity.current
    return if (backdrop != null) {
        this.drawPrismalGlass(
            backdrop = backdrop,
            shape = { PrismalRoundedRectangle(radius) },
            effects = prismalGlassEffects(density),
        ).clip(shape)
    } else {
        this.background(fallback, shape)
    }
}

/** Animated blue-violet light field sampled by all glass panels. */
@Composable
fun DylandBackdropLayer(backdrop: PrismalBackdrop) {
    val motion = rememberInfiniteTransition(label = "dyland-ambient-light")
    val phase by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ambient-phase",
    )

    Canvas(Modifier.fillMaxSize().dylandGlassBackdrop(backdrop)) {
        drawRect(
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF080A14),
                    Color(0xFF111A35),
                    Color(0xFF17132D),
                    Color(0xFF080A14),
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            )
        )

        val blueCenter = Offset(size.width * (0.16f + phase * 0.24f), size.height * 0.18f)
        val blueRadius = size.minDimension * 0.76f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x805C8BFF), Color(0x335C8BFF), Color.Transparent),
                center = blueCenter,
                radius = blueRadius,
            ),
            radius = blueRadius,
            center = blueCenter,
        )

        val violetCenter = Offset(size.width * (0.83f - phase * 0.20f), size.height * 0.76f)
        val violetRadius = size.minDimension * 0.82f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x805D35D6), Color(0x335D35D6), Color.Transparent),
                center = violetCenter,
                radius = violetRadius,
            ),
            radius = violetRadius,
            center = violetCenter,
        )
    }
}
