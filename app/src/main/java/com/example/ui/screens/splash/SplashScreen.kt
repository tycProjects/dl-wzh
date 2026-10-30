package com.example.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentLilac
import com.example.ui.theme.DarkBg
import com.example.ui.theme.HindSiliguriFontFamily
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SecondaryViolet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val logoScale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val creditAlpha = remember { Animatable(0f) }
    val particlePhase = remember { Animatable(0f) }

    // Particle positions
    val particles = remember {
        List(25) {
            Triple(
                Random.nextFloat(),
                Random.nextFloat(),
                Random.nextFloat() * 4f + 2f
            )
        }
    }

    LaunchedEffect(Unit) {
        // Particle animation loop
        launch {
            particlePhase.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }

        // Sequential splash animation
        // 1. Logo fades in and scales smoothly
        logoAlpha.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
        logoScale.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))

        // 2. App name text appears
        textAlpha.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))

        // 3. Made by Mahin Parvez credit appears
        creditAlpha.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))

        delay(900)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBg,
                        PrimaryPurple.copy(alpha = 0.9f),
                        DarkBg
                    )
                )
            )
            .clickable { onSplashFinished() }, // Tap to skip immediately
        contentAlignment = Alignment.Center
    ) {
        // Twinkling particles background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val phase = particlePhase.value

            particles.forEachIndexed { i, p ->
                val x = (p.first * canvasW + (phase * 30f * (i % 3 - 1))) % canvasW
                val y = (p.second * canvasH + (phase * 50f)) % canvasH
                val radius = p.third
                val alpha = (0.3f + 0.5f * kotlin.math.sin(phase * 2 * Math.PI.toFloat() + i)).coerceIn(0.1f, 0.9f)

                drawCircle(
                    color = AccentLilac.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_fg_img),
                    contentDescription = "গল্পকথা লোগো",
                    modifier = Modifier.size(105.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Name in Bengali
            Text(
                text = "গল্পকথা",
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 38.sp,
                color = Color.White,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "বাংলা সাহিত্যের ডিজিটাল ভুবন",
                fontFamily = HindSiliguriFontFamily,
                fontSize = 15.sp,
                color = AccentLilac,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Creator Credit Badge
            Box(
                modifier = Modifier
                    .alpha(creditAlpha.value)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Made by Mahin Parvez",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.95f),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
