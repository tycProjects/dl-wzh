package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.VoiceState
import com.example.ui.theme.CyberNavyDark
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HudVisualizer(
    modifier: Modifier = Modifier,
    voiceState: VoiceState = VoiceState.IDLE,
    size: Dp = 220.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_animation")

    // Rotation angles
    val rotateClockwise by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate_clockwise"
    )

    val rotateCounterClockwise by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate_counter_clockwise"
    )

    // Pulse animation based on state
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = when (voiceState) {
            VoiceState.LISTENING -> 1.14f
            VoiceState.THINKING -> 1.08f
            VoiceState.SPEAKING -> 1.10f
            VoiceState.IDLE -> 1.02f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (voiceState) {
                    VoiceState.LISTENING -> 600
                    VoiceState.THINKING -> 800
                    VoiceState.SPEAKING -> 700
                    VoiceState.IDLE -> 1800
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = when (voiceState) {
            VoiceState.LISTENING -> 0.95f
            VoiceState.THINKING -> 0.85f
            VoiceState.SPEAKING -> 0.90f
            VoiceState.IDLE -> 0.45f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val ringColor = when (voiceState) {
        VoiceState.LISTENING -> NeonRed
        VoiceState.THINKING -> NeonPurple
        VoiceState.SPEAKING -> NeonCyan
        VoiceState.IDLE -> ElectricBlue
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Outer Canvas for rotating HUD arcs and tick marks
        Canvas(
            modifier = Modifier
                .size(size)
                .scale(pulseScale)
        ) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f - 10.dp.toPx()

            // Outer subtle boundary ring
            drawCircle(
                color = ringColor.copy(alpha = glowAlpha * 0.25f),
                radius = radius,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Segmented HUD outer ring
            val sweepSegments = 6
            val segmentAngle = 360f / sweepSegments
            for (i in 0 until sweepSegments) {
                val start = rotateClockwise + i * segmentAngle
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            ringColor.copy(alpha = glowAlpha),
                            NeonCyan.copy(alpha = glowAlpha * 0.3f)
                        )
                    ),
                    startAngle = start,
                    sweepAngle = segmentAngle * 0.6f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Inner counter-rotating ring
            val innerRadius = radius - 18.dp.toPx()
            val innerSegments = 4
            val innerSegmentAngle = 360f / innerSegments
            for (i in 0 until innerSegments) {
                val start = rotateCounterClockwise + i * innerSegmentAngle
                drawArc(
                    color = NeonCyan.copy(alpha = glowAlpha * 0.85f),
                    startAngle = start,
                    sweepAngle = innerSegmentAngle * 0.45f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Radar dots
            for (deg in listOf(0, 90, 180, 270)) {
                val rad = Math.toRadians((rotateClockwise + deg).toDouble())
                val dotX = center.x + (innerRadius - 8.dp.toPx()) * cos(rad).toFloat()
                val dotY = center.y + (innerRadius - 8.dp.toPx()) * sin(rad).toFloat()
                drawCircle(
                    color = NeonCyan.copy(alpha = glowAlpha),
                    radius = 3.5.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }
        }

        // Center ARAFAT AI Core Logo / Avatar
        Box(
            modifier = Modifier
                .size(size * 0.62f)
                .shadow(elevation = 24.dp, shape = CircleShape, spotColor = ringColor)
                .clip(CircleShape)
                .background(CyberNavyDark)
                .border(2.5.dp, Brush.linearGradient(listOf(NeonCyan, ringColor)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_arafat_logo),
                contentDescription = "ARAFAT AI Core",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}
