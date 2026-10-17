package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidDarkBackground
import com.example.ui.theme.LiquidMagenta
import com.example.ui.theme.LiquidTeal
import com.example.ui.theme.LiquidViolet
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated liquid glass background with multi-harmonic sinusoidal wave deformation,
 * organic fluid mesh, and floating refractive iridescent orbs.
 */
@Composable
fun LiquidBackgroundCanvas(
    modifier: Modifier = Modifier,
    waveIntensity: Float = 1.0f,
    primaryColor: Color = LiquidCyan,
    secondaryColor: Color = LiquidViolet,
    tertiaryColor: Color = LiquidMagenta
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LiquidWaves")

    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase2"
    )

    val orbFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbFloat"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Base deep midnight background
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF0F172A),
                    LiquidDarkBackground,
                    Color(0xFF020617)
                ),
                center = Offset(width * 0.5f, height * 0.4f),
                radius = width * 1.2f
            )
        )

        // 2. Floating Liquid Glass Iridescent Orbs (Blobs)
        val orb1X = width * (0.25f + 0.15f * sin(phase1.toDouble()).toFloat())
        val orb1Y = height * (0.30f + 0.12f * cos(phase1.toDouble() * 0.7).toFloat())
        val orb1Radius = width * 0.45f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.22f),
                    primaryColor.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(orb1X, orb1Y),
                radius = orb1Radius
            ),
            center = Offset(orb1X, orb1Y),
            radius = orb1Radius
        )

        val orb2X = width * (0.75f - 0.18f * cos(phase2.toDouble() * 0.8).toFloat())
        val orb2Y = height * (0.65f - 0.14f * sin(phase2.toDouble()).toFloat())
        val orb2Radius = width * 0.50f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = 0.20f),
                    secondaryColor.copy(alpha = 0.06f),
                    Color.Transparent
                ),
                center = Offset(orb2X, orb2Y),
                radius = orb2Radius
            ),
            center = Offset(orb2X, orb2Y),
            radius = orb2Radius
        )

        val orb3X = width * (0.5f + 0.25f * sin(phase2.toDouble() * 0.5).toFloat())
        val orb3Y = height * (0.15f + 0.10f * cos(phase1.toDouble() * 0.9).toFloat())
        val orb3Radius = width * 0.38f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    tertiaryColor.copy(alpha = 0.16f),
                    tertiaryColor.copy(alpha = 0.04f),
                    Color.Transparent
                ),
                center = Offset(orb3X, orb3Y),
                radius = orb3Radius
            ),
            center = Offset(orb3X, orb3Y),
            radius = orb3Radius
        )

        // 3. Ambient Fluid Bottom Waves
        val waveBaseY = height * 0.78f
        val waveAmplitude = 45f * waveIntensity

        val wavePath1 = Path().apply {
            moveTo(0f, height)
            lineTo(0f, waveBaseY)
            var x = 0f
            while (x <= width) {
                val y = waveBaseY +
                        waveAmplitude * sin((x / width * 3f * Math.PI + phase1).toDouble()).toFloat() +
                        (waveAmplitude * 0.4f) * cos((x / width * 2f * Math.PI + phase2).toDouble()).toFloat()
                lineTo(x, y)
                x += 16f
            }
            lineTo(width, height)
            close()
        }

        drawPath(
            path = wavePath1,
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.12f),
                    secondaryColor.copy(alpha = 0.04f),
                    Color.Transparent
                ),
                startY = waveBaseY - 60f,
                endY = height
            )
        )

        val wavePath2 = Path().apply {
            moveTo(0f, height)
            lineTo(0f, waveBaseY + 25f)
            var x = 0f
            while (x <= width) {
                val y = waveBaseY + 25f +
                        (waveAmplitude * 0.8f) * sin((x / width * 2.5f * Math.PI + phase2 + 1.5f).toDouble()).toFloat()
                lineTo(x, y)
                x += 16f
            }
            lineTo(width, height)
            close()
        }

        drawPath(
            path = wavePath2,
            brush = Brush.verticalGradient(
                colors = listOf(
                    tertiaryColor.copy(alpha = 0.10f),
                    primaryColor.copy(alpha = 0.02f),
                    Color.Transparent
                ),
                startY = waveBaseY,
                endY = height
            )
        )
    }
}
