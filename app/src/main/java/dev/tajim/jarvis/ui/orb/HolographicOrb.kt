package dev.tajim.jarvis.ui.orb

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.tajim.jarvis.R
import dev.tajim.jarvis.ui.theme.JarvisColors
import dev.tajim.jarvis.ui.theme.LocalJarvisColors
import dev.tajim.jarvis.ui.theme.LocalJarvisMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val BASE_POINTS = 560
private const val BUCKETS = 4
private val TWO_PI = (2.0 * PI).toFloat()

/** Evenly spread points on a unit sphere, shuffled so any prefix is still evenly spread (adaptive quality drops a suffix). */
private fun buildSpherePoints(count: Int): FloatArray {
    val golden = (PI * (3.0 - sqrt(5.0))).toFloat()
    val order = (0 until count).shuffled(Random(7))
    val out = FloatArray(count * 3)
    for ((slot, i) in order.withIndex()) {
        val y = 1f - (i / (count - 1f)) * 2f
        val radius = sqrt(1f - y * y)
        val theta = golden * i
        out[slot * 3] = cos(theta) * radius
        out[slot * 3 + 1] = y
        out[slot * 3 + 2] = sin(theta) * radius
    }
    return out
}

private data class OrbPalette(val front: Color, val back: Color, val ring: Color, val glow: Color)

private fun paletteFor(state: OrbState, c: JarvisColors) = when (state) {
    OrbState.ERROR -> OrbPalette(c.error, c.magenta, c.error, c.error)
    OrbState.OFFLINE -> OrbPalette(c.textSecondary, c.textSecondary.copy(alpha = 0.5f), c.textSecondary, c.textSecondary)
    OrbState.LISTENING -> OrbPalette(c.cyan, c.blue, c.cyan, c.cyan)
    OrbState.PROCESSING -> OrbPalette(c.violet, c.cyan, c.violet, c.violet)
    OrbState.SPEAKING -> OrbPalette(c.magenta, c.violet, c.magenta, c.magenta)
    OrbState.IDLE -> OrbPalette(c.cyan, c.violet, c.blue, c.blue)
}

/** Lets the user's drag rotation survive navigating away and back (Home -> workspace -> Home). */
private val FloatStateSaver = Saver<MutableFloatState, Float>(
    save = { it.floatValue },
    restore = { mutableFloatStateOf(it) },
)

/**
 * 3D-projected particle sphere with three orbital rings and moving connection nodes.
 *
 * Optimized for weak GPUs/CPUs:
 * - no frame loop of its own: it reads [clock], which is the single loop for the whole screen;
 * - particles are quantized into 4 depth buckets and drawn with 4 native drawPoints calls instead of hundreds of drawCircle calls;
 * - scratch arrays and the ring Path are reused, so a frame allocates almost nothing;
 * - particle count follows [AnimationClock.quality].
 *
 * Horizontal drag rotates the sphere; vertical drags are left to the parent so the page can still scroll.
 * A tap calls [onTap] (Home uses it to start or stop listening) and is exposed to accessibility as a click.
 */
@Composable
fun HolographicOrb(
    state: OrbState,
    clock: AnimationClock,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
) {
    val currentTap by rememberUpdatedState(onTap)
    val colors = LocalJarvisColors.current
    val motion = LocalJarvisMotion.current
    val points = remember { buildSpherePoints(BASE_POINTS) }
    val dragYaw = rememberSaveable(saver = FloatStateSaver) { mutableFloatStateOf(0f) }
    val scratch = remember { FloatArray(3) }
    val ringPath = remember { Path() }
    val buckets = remember { Array(BUCKETS) { FloatArray(BASE_POINTS * 2) } }
    val counts = remember { IntArray(BUCKETS) }
    val paint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
        }
    }

    val description = stringResource(
        when (state) {
            OrbState.IDLE -> R.string.orb_desc_idle
            OrbState.LISTENING -> R.string.orb_desc_listening
            OrbState.PROCESSING -> R.string.orb_desc_processing
            OrbState.SPEAKING -> R.string.orb_desc_speaking
            OrbState.ERROR -> R.string.orb_desc_error
            OrbState.OFFLINE -> R.string.orb_desc_offline
        },
    )

    Canvas(
        modifier
            .aspectRatio(1f)
            .semantics {
                contentDescription = description
                if (onTap != null) onClick { currentTap?.invoke(); true }
            }
            .pointerInput(Unit) { detectTapGestures { currentTap?.invoke() } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    dragYaw.floatValue += dragAmount * 0.008f
                }
            },
    ) {
        val t = clock.time
        val quality = clock.quality
        val center = Offset(size.width / 2f, size.height / 2f)
        val base = size.minDimension / 2f * 0.62f
        val pal = paletteFor(state, colors)
        val pulse = if (motion.reduced) 1f else when (state) {
            OrbState.LISTENING -> 1f + 0.05f * sin(t * 7f)
            OrbState.SPEAKING -> 1f + 0.03f * sin(t * 9f) + 0.02f * sin(t * 13.3f)
            OrbState.PROCESSING -> 1f + 0.015f * sin(t * 4f)
            else -> 1f + 0.01f * sin(t * 1.2f)
        }
        val r = base * pulse

        // Soft core glow
        drawCircle(
            Brush.radialGradient(listOf(pal.glow.copy(alpha = if (colors.isDark) 0.40f else 0.28f), Color.Transparent), center, r * 1.7f),
            radius = r * 1.7f, center = center,
        )

        // Particle sphere
        val yaw = 0.6f + t * 0.25f + dragYaw.floatValue
        val pitch = 0.3f
        val cy = cos(yaw); val sy = sin(yaw); val cp = cos(pitch); val sp = sin(pitch)
        counts.fill(0)
        val n = (BASE_POINTS * quality).toInt()
        for (i in 0 until n) {
            val x = points[i * 3]; val y = points[i * 3 + 1]; val z = points[i * 3 + 2]
            val x1 = x * cy + z * sy
            val z1 = -x * sy + z * cy
            val y2 = y * cp - z1 * sp
            val z2 = y * sp + z1 * cp
            val depth = (z2 + 1f) * 0.5f
            val b = (depth * BUCKETS).toInt().coerceIn(0, BUCKETS - 1)
            val arr = buckets[b]
            val k = counts[b]
            arr[k * 2] = center.x + x1 * r
            arr[k * 2 + 1] = center.y + y2 * r
            counts[b] = k + 1
        }
        val dotRadius = 1.2.dp.toPx()
        val nativeCanvas = drawContext.canvas.nativeCanvas
        for (b in 0 until BUCKETS) {
            val count = counts[b]
            if (count == 0) continue
            val d = (b + 0.5f) / BUCKETS
            paint.color = lerp(pal.back, pal.front, d).copy(alpha = 0.16f + 0.84f * d).toArgb()
            paint.strokeWidth = 2f * dotRadius * (0.7f + 1.3f * d)
            nativeCanvas.drawPoints(buckets[b], 0, count * 2, paint)
        }

        // Orbital rings with moving connection nodes
        val speed = if (state == OrbState.PROCESSING) 2.2f else 0.5f
        val ringStroke = 1.4.dp.toPx()
        val segments = if (quality < 0.7f) 32 else 48
        drawOrbitRing(center, r * 1.22f, 1.25f, 0.2f + t * 0.10f, yaw, pitch, pal.ring.copy(alpha = 0.55f), ringStroke, t * speed, colors.isDark, ringPath, scratch, segments)
        drawOrbitRing(center, r * 1.36f, 0.55f, 1.3f - t * 0.08f, yaw, pitch, colors.violet.copy(alpha = 0.45f), ringStroke, -t * speed * 0.8f, colors.isDark, ringPath, scratch, segments)
        drawOrbitRing(center, r * 1.50f, 1.45f, 2.4f + t * 0.06f, yaw, pitch, colors.magenta.copy(alpha = 0.35f), ringStroke, t * speed * 0.6f, colors.isDark, ringPath, scratch, segments)

        // State overlays: only what the state actually means
        val stroke = 2.dp.toPx()
        when (state) {
            OrbState.LISTENING -> for (k in 0..1) {
                val phase = (t * 0.9f + k * 0.5f) % 1f
                drawCircle(pal.glow.copy(alpha = 0.5f * (1f - phase)), radius = r * (1.1f + 0.7f * phase), center = center, style = Stroke(stroke))
            }
            OrbState.PROCESSING -> {
                val rad = r * 1.62f
                drawArc(
                    pal.front, startAngle = t * 240f, sweepAngle = 100f, useCenter = false,
                    topLeft = Offset(center.x - rad, center.y - rad), size = Size(rad * 2, rad * 2),
                    style = Stroke(stroke * 1.5f, cap = StrokeCap.Round),
                )
            }
            OrbState.ERROR -> drawCircle(pal.front.copy(alpha = 0.8f), radius = r * 1.62f, center = center, style = Stroke(stroke))
            OrbState.OFFLINE -> drawCircle(
                pal.front.copy(alpha = 0.7f), radius = r * 1.62f, center = center,
                style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 14f))),
            )
            else -> Unit
        }
    }
}

private fun DrawScope.drawOrbitRing(
    center: Offset, radius: Float, tilt: Float, roll: Float,
    yaw: Float, pitch: Float, color: Color, strokeWidth: Float, nodeAngle: Float, isDark: Boolean,
    path: Path, out: FloatArray, segments: Int,
) {
    val ct = cos(tilt); val st = sin(tilt); val cr = cos(roll); val sr = sin(roll)
    val cy = cos(yaw * 0.35f); val sy = sin(yaw * 0.35f); val cp = cos(pitch); val sp = sin(pitch)
    // Writes screen x, screen y, depth z into [out] (no per-call allocation).
    fun project(a: Float) {
        val x0 = cos(a); val y0 = sin(a)
        val y1 = y0 * ct; val z1 = y0 * st
        val x2 = x0 * cr - y1 * sr; val y2 = x0 * sr + y1 * cr
        val x3 = x2 * cy + z1 * sy; val z3 = -x2 * sy + z1 * cy
        out[0] = center.x + x3 * radius
        out[1] = center.y + (y2 * cp - z3 * sp) * radius
        out[2] = y2 * sp + z3 * cp
    }
    path.rewind()
    for (i in 0..segments) {
        project(TWO_PI * i / segments)
        if (i == 0) path.moveTo(out[0], out[1]) else path.lineTo(out[0], out[1])
    }
    drawPath(path, color, style = Stroke(strokeWidth))
    for (k in 0..1) {
        project(nodeAngle + k * PI.toFloat())
        val nx = out[0]; val ny = out[1]
        val depth = (out[2] + 1f) * 0.5f
        val a = 0.5f + 0.5f * depth
        drawCircle(color.copy(alpha = 0.25f), radius = 7.dp.toPx(), center = Offset(nx, ny))
        drawCircle(
            if (isDark) Color.White.copy(alpha = a) else color.copy(alpha = a),
            radius = 2.5.dp.toPx() * (0.7f + 0.6f * depth), center = Offset(nx, ny),
        )
    }
}
