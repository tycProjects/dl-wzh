package com.tyce.enhancer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** false = skip intro, wow */
const val SPLASH_ENABLED = true

private const val TOTAL = 3.0f
private val Mint = Color(0xFF2F5BFF)
private val MintLight = Color(0xFF7C9BFF)
private val White = Color(0xFF121316)

private val Out = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)
private val InOut = CubicBezierEasing(0.65f, 0.05f, 0.36f, 1f)
private val Sweep = CubicBezierEasing(0.45f, 0.05f, 0.25f, 1f)

private fun seg(t: Float, s: Float, e: Float, c: Easing = Out): Float =
    c.transform(((t - s) / (e - s)).coerceIn(0f, 1f))

/** intro, 3 segundo, gwapa kaayo */
@Composable
fun IntroSplash(onDone: () -> Unit) {
    val clock = remember { Animatable(0f) }
    val done by rememberUpdatedState(onDone)
    LaunchedEffect(Unit) {
        clock.animateTo(TOTAL, tween((TOTAL * 1000).toInt(), easing = LinearEasing))
        done()
    }
    val density = LocalDensity.current.density

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            // ayaw hikapa, naay splash pa
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        val t = clock.value
        val u = min(maxWidth.value / 300f, 1.4f)
        val uPx = u * density
        val wy = maxHeight / 2 - (18f * u).dp
        val fade = 1f - seg(t, 2.4f, 3.0f, InOut)
        val nameIn = seg(t, 0.1f, 0.7f)
        val tagIn = seg(t, 1.55f, 2.1f)
        val mk = seg(t, 1.7f, 2.2f)

        Box(Modifier.fillMaxSize().graphicsLayer { alpha = fade.coerceIn(0f, 1f) }) {
            Canvas(Modifier.fillMaxSize()) {
                drawSplash(t, uPx, size.height / 2f - 18f * uPx)
            }

            // ang ngalan sa app
            Box(
                Modifier
                    .offset(y = wy - (30f * u).dp)
                    .fillMaxWidth()
                    .height((60f * u).dp)
                    .graphicsLayer {
                        alpha = nameIn
                        translationY = 8f * uPx * (1f - nameIn)
                    },
                contentAlignment = Alignment.Center
            ) {
                val base = TextStyle(
                    fontFamily = InterFamily,
                    fontSize = (30f * u).sp,
                    letterSpacing = (1.0f * u).sp
                )
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = White, fontWeight = FontWeight.ExtraBold)) { append("Sen ") }
                        withStyle(SpanStyle(color = Mint, fontWeight = FontWeight.ExtraBold)) { append("Enchancer") }
                    },
                    style = base,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }

            // ang tagline, siyempre
            Box(
                Modifier
                    .offset(y = wy + (40f * u).dp)
                    .fillMaxWidth()
                    .graphicsLayer { alpha = tagIn },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Sharpen photos on your phone",
                    style = TextStyle(
                        fontFamily = InterFamily,
                        fontSize = (13f * u).sp,
                        color = C.Muted,
                        letterSpacing = 0.4.sp
                    )
                )
            }

            // ang maker, ako ni
            Box(Modifier.fillMaxSize().safeDrawingPadding().padding(bottom = 30.dp), contentAlignment = Alignment.BottomCenter) {
                Text(
                    "Tycept",
                    modifier = Modifier.graphicsLayer { alpha = (mk * 0.3f).coerceIn(0f, 1f) },
                    style = TextStyle(
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = White,
                        letterSpacing = (12f * 0.18f).sp
                    )
                )
            }
        }
    }
}

/** kometa ug sparkle, ngitngit background */
private fun DrawScope.drawSplash(t: Float, u: Float, wy: Float) {
    val w = size.width
    val cx = w / 2f
    drawRect(C.Bg)

    val span = min(w * 0.74f, 300f * u)
    val xl = cx - span / 2f - 4f * u
    val xr = cx + span / 2f + 6f * u
    val yl = wy - 14f * u
    val yr = wy - 36f * u
    val peak = wy - 76f * u
    val path = Path().apply {
        moveTo(xl, yl)
        cubicTo(xl + span * 0.22f, peak, xr - span * 0.22f, peak - 4f * u, xr, yr)
    }
    val pm = PathMeasure()
    pm.setPath(path, false)
    val len = pm.length

    val p = seg(t, 0.45f, 1.55f, Sweep)
    if (p <= 0f) return
    val head = len * p
    val settle = seg(t, 1.45f, 2.0f, InOut)
    val trail = len * 0.5f + len * 0.5f * settle
    val tail = max(0f, head - trail)

    val n = 56
    for (i in 0 until n) {
        val f0 = i / n.toFloat()
        val f1 = (i + 1) / n.toFloat()
        val a = tail + (head - tail) * f0
        val b = tail + (head - tail) * f1
        val p0 = pm.getPosition(a)
        val p1 = pm.getPosition(b)
        if (!p0.isSpecified || !p1.isSpecified) continue
        val k = f1.pow(1.7f)
        val sw = (0.8f + 2.8f * f1) * u
        drawLine(Mint.copy(alpha = (k * 0.16f).coerceIn(0f, 1f)), p0, p1, sw * 4.5f, StrokeCap.Round)
        drawLine(MintLight.copy(alpha = k.coerceIn(0f, 1f)), p0, p1, sw, StrokeCap.Round)
    }

    // ulo sa kometa, hayag
    val hp = pm.getPosition(head)
    val moving = 1f - seg(t, 1.4f, 1.7f)
    if (hp.isSpecified && moving > 0f) {
        drawCircle(Mint.copy(alpha = (0.28f * moving).coerceIn(0f, 1f)), 5f * u, hp)
        drawCircle(White.copy(alpha = moving.coerceIn(0f, 1f)), 2f * u, hp)
    }

    // sparkle sa katapusan, wow
    val s = seg(t, 1.4f, 1.75f)
    if (s > 0f) {
        val e = Offset(xr, yr)
        drawCircle(Mint.copy(alpha = (0.25f * s).coerceIn(0f, 1f)), 14f * u, e)
        val hl = 26f * u * s
        val vl = 11f * u * s
        val lineColor = White.copy(alpha = (0.9f * s).coerceIn(0f, 1f))
        drawLine(lineColor, Offset(e.x - hl, e.y), Offset(e.x + hl, e.y), 1.4f * u, StrokeCap.Round)
        drawLine(lineColor, Offset(e.x, e.y - vl), Offset(e.x, e.y + vl), 1.4f * u, StrokeCap.Round)
        drawCircle(White.copy(alpha = s.coerceIn(0f, 1f)), 2f * u, e)
    }
}
