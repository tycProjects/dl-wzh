package com.logalyze.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import com.logalyze.app.R
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val word = "Logalyze"
    val letters = remember { word.map { Animatable(0f) } }
    val lines = remember { List(2) { Animatable(0f) } }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // الحروف واحد واحد
        letters.forEach { a ->
            launch { a.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
            delay(110)
        }
        delay(300)
        // سطور الـ tagline واحد واحد
        lines.forEach { a ->
            launch { a.animateTo(1f, tween(500)) }
            delay(380)
        }
        // شريط التحميل
        progress.animateTo(1f, tween(900, easing = LinearEasing))
        delay(250)
        onDone()
    }

    val white = MaterialTheme.colorScheme.onBackground
    val cyan = MaterialTheme.colorScheme.primary
    val glow = TextStyle(shadow = Shadow(color = cyan.copy(alpha = 0.55f), blurRadius = 28f))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(horizontalArrangement = Arrangement.Center) {
                word.forEachIndexed { i, ch ->
                    val a = letters[i]
                    Text(
                        text = ch.toString(),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (i < 3) white else cyan,
                        style = if (i < 3) TextStyle.Default else glow,
                        modifier = Modifier.graphicsLayer {
                            alpha = a.value
                            translationY = (1f - a.value) * 50f
                            val s = 0.6f + 0.4f * a.value
                            scaleX = s
                            scaleY = s
                        }
                    )
                }
            }
            }
            Spacer(Modifier.height(14.dp))
            listOf(stringResource(R.string.tagline_1), stringResource(R.string.tagline_2)).forEachIndexed { i, line ->
                val a = lines[i]
                Text(
                    text = line,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer {
                        alpha = a.value
                        translationY = (1f - a.value) * 16f
                    }
                )
            }
        }

        LinearProgressIndicator(
            progress = { progress.value },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
                .fillMaxWidth(0.6f)
                .height(4.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = cyan,
            trackColor = MaterialTheme.colorScheme.outline
        )
    }
}
