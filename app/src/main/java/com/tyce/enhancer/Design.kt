package com.tyce.enhancer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** ngitngit nga UI*/

object C {
    val Bg = Color(0xFFF3F2EE)
    val Card = Color(0xFFFFFFFF)
    val Panel = Color(0xFFFFFFFF)
    val Line = Color(0xFFE6E5E0)
    val Accent = Color(0xFF2F5BFF)
    val AccentAlt = Color(0xFF2F5BFF)
    val Cyan = Color(0xFF2F5BFF)
    val AccentDeep = Color(0xFF121316)
    val Muted = Color(0xFF6B6E76)
    val Red = Color(0xFFD93025)
    val Text = Color(0xFF121316)
    val Brand: Brush = SolidColor(Color(0xFF2F5BFF))
}

// ------- letra-letra ra ni

val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold)
)

private val trackSizes = floatArrayOf(10f, 11f, 12f, 13f, 14f, 15f, 16f, 17f, 18f, 20f, 22f, 24f, 28f, 34f)
private val trackVals = floatArrayOf(.12f, .06f, 0f, -.08f, -.15f, -.24f, -.32f, -.41f, -.43f, -.45f, -.26f, -.2f, .36f, .38f)

/** gi-copy ni sa apple, ssshh */
private fun sfTracking(size: Float): Float {
    if (size <= trackSizes.first()) return trackVals.first()
    if (size >= trackSizes.last()) return trackVals.last()
    for (i in 1 until trackSizes.size) {
        if (size <= trackSizes[i]) {
            val a = trackSizes[i - 1]
            val b = trackSizes[i]
            return trackVals[i - 1] + (trackVals[i] - trackVals[i - 1]) * ((size - a) / (b - a))
        }
    }
    return 0f
}

fun inter(
    size: Float,
    w: FontWeight = FontWeight.Normal,
    c: Color = C.Text,
    h: Float = 1.28f
): TextStyle {
    val fs = (size * 0.92f * 2f).roundToInt() / 2f
    return TextStyle(
        fontFamily = InterFamily,
        fontSize = fs.sp,
        fontWeight = w,
        color = c,
        lineHeight = (fs * h).sp,
        letterSpacing = sfTracking(fs).sp
    )
}

fun heading(size: Float, w: FontWeight = FontWeight.Bold, c: Color = C.Text): TextStyle =
    inter(size, w, c, 1.15f)

// ------- boing boing ayaw pangutana

/** kurba sa iOS */
val IosEase = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

val PopExitEase = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)

/** spring pa-iOS */
fun iosSpring(response: Float, ratio: Float) = spring<Float>(
    dampingRatio = ratio,
    stiffness = ((2.0 * Math.PI / response) * (2.0 * Math.PI / response)).toFloat()
)

/** isa ra setting, para tanan */
object Motion {
    const val Fast = 120     // pag-hawa, bye bye
    const val Base = 220     // pag-ilis screen, dali ra
    const val Tint = 250     // kolor ug progress, ambot ana
    const val Slow = 320     // ang slider, kusog mag-dagan
    const val Sheet = 340    // sheet misaka, pataas dong
    const val SheetExit = 240 // sheet manaog, paubos dong
    const val ExitScale = 0.92f
    const val PopExitScale = 0.2f
    const val PopExit = 110
    const val MenuExit = 200      // menu mulubog kung isira
    const val MenuFade = 170      // menu mawala, parang ex
    const val MenuExitScale = 0.5f
    /** pop spring, tanan gamit ani */
    val PopSpring = iosSpring(0.36f, 0.80f)
}

private val PressSpec = iosSpring(0.18f, 1.0f)
private val ReleaseSpec = iosSpring(0.30f, 0.85f)

/** pislit, nilukso, balik, sakto */
fun Modifier.pressable(
    scale: Float = 0.97f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val s = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val latest by rememberUpdatedState(onClick)
    val release = remember { arrayOfNulls<Job>(1) }
    val stamp = remember { LongArray(1) }
    this
        .graphicsLayer {
            val v = s.value
            scaleX = v
            scaleY = v
            val span = 1f - scale
            val dip = if (span <= 0f) 0f else ((1f - v) / span).coerceIn(0f, 1f)
            alpha = 1f - 0.12f * dip
        }
        .pointerInput(enabled, scale) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    release[0]?.cancel()
                    stamp[0] = System.currentTimeMillis()
                    scope.launch { s.animateTo(scale, PressSpec) }
                    tryAwaitRelease()
                    release[0] = scope.launch {
                        // ayaw hilabti, bahala si batman
                        delay(maxOf(0L, 70L - (System.currentTimeMillis() - stamp[0])))
                        s.animateTo(1f, ReleaseSpec)
                    }
                },
                onTap = { latest() }
            )
        }
}
