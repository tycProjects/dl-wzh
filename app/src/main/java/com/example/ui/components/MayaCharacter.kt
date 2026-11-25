package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CoralError
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.viewmodel.AssistantMode
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MayaCharacter(
    mode: AssistantMode,
    speechAmplitude: Float,
    listeningRms: Float,
    size: Dp = 230.dp,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "maya_idle")

    val breathingScale by infinite.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    val headTilt by infinite.animateFloat(
        initialValue = -1.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "headTilt"
    )

    val haloRotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (mode == AssistantMode.THINKING) 2800 else 12000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloRotation"
    )

    val pulseProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    // Natural periodic blink animation
    var blinkProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3600L)
            blinkProgress = 1f
            delay(130L)
            blinkProgress = 0f
        }
    }
    val smoothBlink by animateFloatAsState(
        targetValue = blinkProgress,
        animationSpec = tween(100),
        label = "blink"
    )

    val smoothAmp by animateFloatAsState(
        targetValue = if (mode == AssistantMode.SPEAKING) speechAmplitude.coerceIn(0.15f, 1f) else 0f,
        animationSpec = tween(80),
        label = "amp"
    )

    val smoothRms by animateFloatAsState(
        targetValue = if (mode == AssistantMode.LISTENING) listeningRms.coerceIn(0.1f, 1f) else 0f,
        animationSpec = tween(70),
        label = "rms"
    )

    val primaryGlow = when (mode) {
        AssistantMode.LISTENING -> NeonPink
        AssistantMode.THINKING -> NeonCyan
        AssistantMode.SPEAKING -> NeonPurple
        AssistantMode.ERROR -> CoralError
        AssistantMode.IDLE -> NeonIndigo
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size + 52.dp)
    ) {
        // Outer Cosmic Neon Halo, Sonar Ripples, Thinking Orbiters & Lip-Sync Visualizer
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (size.toPx() / 2f)

            // Ambient radial backdrop glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = 0.34f + smoothAmp * 0.22f + smoothRms * 0.2f),
                        NeonPurple.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.48f
                ),
                center = center,
                radius = baseRadius * 1.48f
            )

            // Listening / Speaking expanding energy rings
            if (mode == AssistantMode.LISTENING || mode == AssistantMode.SPEAKING) {
                val ringRadius = baseRadius * (1.03f + pulseProgress * (0.25f + smoothRms * 0.2f + smoothAmp * 0.2f))
                drawCircle(
                    color = primaryGlow.copy(alpha = (1f - pulseProgress) * 0.55f),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Rotating futuristic arc ring
            rotate(degrees = haloRotation, pivot = center) {
                val arcRadius = baseRadius * 1.07f
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            NeonCyan.copy(alpha = 0.85f),
                            NeonIndigo.copy(alpha = 0.2f),
                            NeonPink.copy(alpha = 0.85f),
                            NeonPurple.copy(alpha = 0.2f),
                            NeonCyan.copy(alpha = 0.85f)
                        )
                    ),
                    startAngle = 0f,
                    sweepAngle = 290f,
                    useCenter = false,
                    topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Thinking orbital nodes
                if (mode == AssistantMode.THINKING) {
                    for (i in 0 until 3) {
                        val angleRad = Math.toRadians((i * 120).toDouble())
                        val ox = center.x + (arcRadius * 1.04f) * cos(angleRad).toFloat()
                        val oy = center.y + (arcRadius * 1.04f) * sin(angleRad).toFloat()
                        drawCircle(
                            color = if (i % 2 == 0) NeonCyan else NeonPink,
                            radius = 5.dp.toPx(),
                            center = Offset(ox, oy)
                        )
                    }
                }
            }
        }

        // Animated Maya Female Portrait
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    val dynamicScale = breathingScale + (smoothAmp * 0.03f) + (smoothRms * 0.025f)
                    scaleX = dynamicScale
                    scaleY = dynamicScale
                    rotationZ = if (mode == AssistantMode.SPEAKING) headTilt * 1.4f else headTilt
                }
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(NeonCyan.copy(alpha = 0.7f), NeonPurple.copy(alpha = 0.8f), NeonPink.copy(alpha = 0.6f))
                    ),
                    shape = CircleShape
                )
                .background(Color(0xFF070913))
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_maya_avatar),
                contentDescription = "মায়া AI অ্যাসিস্ট্যান্ট",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Blinking eyelid shimmer & Lip-Sync lower aura overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height

                // Subtle blink shadow across eye region
                if (smoothBlink > 0.05f) {
                    val blinkHeight = h * 0.055f * smoothBlink
                    drawRoundRect(
                        color = Color(0xFF0B0F20).copy(alpha = 0.75f * smoothBlink),
                        topLeft = Offset(w * 0.30f, h * 0.37f),
                        size = Size(w * 0.40f, blinkHeight),
                        cornerRadius = CornerRadius(16f, 16f)
                    )
                }

                // Speaking lip-sync holographic energy wave near voice/mouth region
                if (mode == AssistantMode.SPEAKING && smoothAmp > 0.05f) {
                    val waveCenterY = h * 0.79f
                    val barCount = 7
                    val barSpacing = w * 0.045f
                    val startX = (w - (barCount - 1) * barSpacing) / 2f
                    for (i in 0 until barCount) {
                        val factor = 1f - kotlin.math.abs(i - 3) * 0.2f
                        val barH = (h * 0.025f) + (h * 0.08f * smoothAmp * factor)
                        val x = startX + i * barSpacing
                        drawLine(
                            color = if (i % 2 == 0) NeonCyan.copy(alpha = 0.9f) else NeonPink.copy(alpha = 0.9f),
                            start = Offset(x, waveCenterY - barH / 2f),
                            end = Offset(x, waveCenterY + barH / 2f),
                            strokeWidth = 3.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}
