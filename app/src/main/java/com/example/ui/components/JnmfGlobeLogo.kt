package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * JNMF Green Outline Globe Logo with integrated audio sound waves and equalizer motif.
 *
 * Design features:
 * - Earth/globe symbol inspired by 🌐
 * - Neon green outlines and thin curved grid lines (latitudes and longitudes)
 * - Lines only (no solid globe fill, no textures, no blue oceans)
 * - Clean, futuristic, minimal, and crisp at all Android display densities
 * - Optional lightweight 3D rotation and audio equalizer wave animation
 */
@Composable
fun JnmfGlobeLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    animated: Boolean = false,
    showEqualizerWaves: Boolean = true,
    showFlankingWaves: Boolean = true,
    strokeWidthDp: Dp = 1.8.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "GlobeAnimation")

    val rotationPhase by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 6000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "GlobeRotation"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    val waveBounce by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "EqWaveBounce"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0.7f) }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidthDp.toPx()
            val thinStrokePx = (strokeWidthDp * 0.75f).toPx().coerceAtLeast(1.2f)

            val w = this.size.width
            val h = this.size.height

            // Globe center and radius
            val globeRadius = if (showFlankingWaves) minOf(w, h) * 0.34f else minOf(w, h) * 0.44f
            val cx = w / 2f
            val cy = if (showEqualizerWaves) h * 0.46f else h / 2f

            val globeCirclePath = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        center = Offset(cx, cy),
                        radius = globeRadius
                    )
                )
            }

            // 1. Draw Globe within clipped circular sphere
            clipPath(globeCirclePath) {
                // Horizontal Equator
                drawLine(
                    color = tint,
                    start = Offset(cx - globeRadius, cy),
                    end = Offset(cx + globeRadius, cy),
                    strokeWidth = strokePx
                )

                // Upper Latitude (curve bending down to simulate 3D spherical perspective)
                val lat1Dist = globeRadius * 0.48f
                val yLat1 = cy - lat1Dist
                val hw1 = sqrt((globeRadius * globeRadius - lat1Dist * lat1Dist).coerceAtLeast(0f))
                val lat1Path = Path().apply {
                    moveTo(cx - hw1, yLat1)
                    quadraticBezierTo(cx, yLat1 + globeRadius * 0.14f, cx + hw1, yLat1)
                }
                drawPath(
                    path = lat1Path,
                    color = tint,
                    style = Stroke(width = thinStrokePx)
                )

                // Lower Latitude (curve bending up)
                val lat2Dist = globeRadius * 0.48f
                val yLat2 = cy + lat2Dist
                val hw2 = sqrt((globeRadius * globeRadius - lat2Dist * lat2Dist).coerceAtLeast(0f))
                val lat2Path = Path().apply {
                    moveTo(cx - hw2, yLat2)
                    quadraticBezierTo(cx, yLat2 - globeRadius * 0.14f, cx + hw2, yLat2)
                }
                drawPath(
                    path = lat2Path,
                    color = tint,
                    style = Stroke(width = thinStrokePx)
                )

                // Central Vertical Meridian
                drawLine(
                    color = tint,
                    start = Offset(cx, cy - globeRadius),
                    end = Offset(cx, cy + globeRadius),
                    strokeWidth = strokePx
                )

                // Longitude curves (ellipses)
                if (animated) {
                    // Smooth rotating wireframe effect
                    val angles = listOf(
                        (rotationPhase * 2 * PI).toFloat(),
                        ((rotationPhase + 0.25f) * 2 * PI).toFloat(),
                        ((rotationPhase + 0.50f) * 2 * PI).toFloat(),
                        ((rotationPhase + 0.75f) * 2 * PI).toFloat()
                    )

                    for (ang in angles) {
                        val cosA = cos(ang)
                        val rx = abs(cosA) * globeRadius
                        val sign = if (cosA >= 0) 1f else -1f

                        val meridianPath = Path().apply {
                            moveTo(cx, cy - globeRadius)
                            cubicTo(
                                cx + sign * rx * 1.33f, cy - globeRadius * 0.55f,
                                cx + sign * rx * 1.33f, cy + globeRadius * 0.55f,
                                cx, cy + globeRadius
                            )
                        }
                        drawPath(
                            path = meridianPath,
                            color = tint.copy(alpha = if (cosA >= 0) 0.9f else 0.45f),
                            style = Stroke(width = thinStrokePx)
                        )
                    }
                } else {
                    // Static balanced symmetric wireframe meridians
                    val rxList = listOf(globeRadius * 0.40f, globeRadius * 0.75f)
                    for (rx in rxList) {
                        // Left meridian
                        drawOval(
                            color = tint,
                            topLeft = Offset(cx - rx, cy - globeRadius),
                            size = Size(rx * 2f, globeRadius * 2f),
                            style = Stroke(width = thinStrokePx)
                        )
                    }
                }
            }

            // Outer circular boundary stroke
            drawCircle(
                color = tint,
                radius = globeRadius,
                center = Offset(cx, cy),
                style = Stroke(width = strokePx)
            )

            // 2. Flanking Audio Broadcast Sound Waves ))) 🌐 (((
            if (showFlankingWaves) {
                val waveAlpha1 = if (animated) 0.5f + 0.5f * waveBounce else 0.8f
                val waveAlpha2 = if (animated) 0.3f + 0.4f * (1f - waveBounce) else 0.5f

                // Left Inner Arc
                val arcR1 = globeRadius + 4.dp.toPx()
                drawArc(
                    color = tint.copy(alpha = waveAlpha1),
                    startAngle = 145f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(cx - arcR1, cy - arcR1),
                    size = Size(arcR1 * 2f, arcR1 * 2f),
                    style = Stroke(width = thinStrokePx, cap = StrokeCap.Round)
                )

                // Right Inner Arc
                drawArc(
                    color = tint.copy(alpha = waveAlpha1),
                    startAngle = -35f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(cx - arcR1, cy - arcR1),
                    size = Size(arcR1 * 2f, arcR1 * 2f),
                    style = Stroke(width = thinStrokePx, cap = StrokeCap.Round)
                )

                // Left Outer Arc (larger sizes only)
                if (size >= 36.dp) {
                    val arcR2 = globeRadius + 8.5.dp.toPx()
                    drawArc(
                        color = tint.copy(alpha = waveAlpha2),
                        startAngle = 152f,
                        sweepAngle = 56f,
                        useCenter = false,
                        topLeft = Offset(cx - arcR2, cy - arcR2),
                        size = Size(arcR2 * 2f, arcR2 * 2f),
                        style = Stroke(width = thinStrokePx, cap = StrokeCap.Round)
                    )

                    // Right Outer Arc
                    drawArc(
                        color = tint.copy(alpha = waveAlpha2),
                        startAngle = -28f,
                        sweepAngle = 56f,
                        useCenter = false,
                        topLeft = Offset(cx - arcR2, cy - arcR2),
                        size = Size(arcR2 * 2f, arcR2 * 2f),
                        style = Stroke(width = thinStrokePx, cap = StrokeCap.Round)
                    )
                }
            }

            // 3. Bottom Equalizer Rhythm Bars
            if (showEqualizerWaves && size >= 28.dp) {
                val barCount = 7
                val baseY = h - 2.dp.toPx()
                val totalWidth = globeRadius * 1.6f
                val barSpacing = totalWidth / (barCount - 1)
                val startX = cx - totalWidth / 2f
                val maxBarH = (h - (cy + globeRadius) - 3.dp.toPx()).coerceAtLeast(3.dp.toPx())

                val heights = listOf(0.45f, 0.75f, 0.60f, 1.0f, 0.65f, 0.80f, 0.40f)

                for (i in 0 until barCount) {
                    val bx = startX + i * barSpacing
                    val factor = heights[i]
                    val dynamicH = if (animated) {
                        maxBarH * (0.35f + 0.65f * abs(sin((waveBounce * PI + i * 0.5f).toFloat())))
                    } else {
                        maxBarH * factor
                    }

                    drawLine(
                        color = tint,
                        start = Offset(bx, baseY),
                        end = Offset(bx, baseY - dynamicH),
                        strokeWidth = (strokePx * 1.1f).coerceAtLeast(2f),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
