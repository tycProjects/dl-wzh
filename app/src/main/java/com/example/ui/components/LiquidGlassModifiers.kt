package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidGlassBorder
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LiquidGlassSurfaceDense
import com.example.ui.theme.LiquidMagenta
import com.example.ui.theme.LiquidViolet

/**
 * Creates a rich "Liquid Glass" frosted surface with iridescent chromatic specular borders,
 * subtle background diffusion gradient, and optional refractive highlights.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = LiquidGlassSurface,
    borderColors: List<Color> = listOf(
        Color.White.copy(alpha = 0.55f),
        LiquidCyan.copy(alpha = 0.40f),
        Color.White.copy(alpha = 0.15f),
        LiquidMagenta.copy(alpha = 0.35f),
        Color.White.copy(alpha = 0.45f)
    ),
    borderWidth: Dp = 1.2.dp,
    hasSpecularGlare: Boolean = true
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                backgroundColor,
                backgroundColor.copy(alpha = (backgroundColor.alpha * 0.65f).coerceIn(0f, 1f))
            )
        ),
        shape = shape
    )
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = borderColors,
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        ),
        shape = shape
    )
    .then(
        if (hasSpecularGlare) {
            Modifier.drawBehind {
                // Top-left specular glare reflection mimicking curved liquid glass
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.25f, size.height * 0.15f),
                        radius = size.width * 0.6f
                    ),
                    center = Offset(size.width * 0.25f, size.height * 0.15f),
                    radius = size.width * 0.6f
                )
            }
        } else Modifier
    )

/**
 * Animated liquid sheen traversing diagonally across the component
 */
@Composable
fun Modifier.liquidShimmer(
    durationMillis: Int = 3000,
    shimmerColor: Color = Color.White.copy(alpha = 0.18f)
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "LiquidShimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -1.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LiquidShimmerOffset"
    )

    this.drawBehind {
        val width = size.width
        val height = size.height
        val startX = translateAnim * width
        val startY = translateAnim * height

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    shimmerColor.copy(alpha = 0.02f),
                    shimmerColor,
                    shimmerColor.copy(alpha = 0.02f),
                    Color.Transparent
                ),
                start = Offset(startX - width * 0.5f, startY - height * 0.5f),
                end = Offset(startX + width * 0.5f, startY + height * 0.5f)
            )
        )
    }
}
