package com.android.island.island

import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * A pre-blurred copy of an artwork/avatar, plus whether it is dark (for adaptive text colour).
 *
 * The "blur" is a cheap progressive down-scale to ~36 px wide (a few KB). Drawn back up with
 * bilinear filtering it looks like a heavy gaussian blur, but costs no RenderEffect, no
 * offscreen render target and almost no RAM.
 *
 * [key] is a hash of the *content* of the tiny bitmap. Two different Bitmap instances of the
 * same picture produce the same key, so the UI can tell "same picture" from "new picture"
 * (the old identity-based check re-faded the backdrop whenever the player re-sent the art).
 */
@Immutable
class ArtBackdrop(val bitmap: Bitmap, val isDark: Boolean, val key: Long)

private const val BLUR_TARGET_PX = 36
private const val CACHE_SIZE = 3

/** Two tiny LRUs (a few KB total): by content key and by cheap source fingerprint.
 *  Both return the SAME ArtBackdrop instance for the same picture, so its [ArtBackdrop.key] is stable. */
private object ArtCache {
    private fun lru() = object : LinkedHashMap<Long, ArtBackdrop>(4, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, ArtBackdrop>?) = size > CACHE_SIZE
    }
    private val contentMap = lru()
    private val fpMap = lru()
    @Synchronized fun byFingerprint(fp: Long): ArtBackdrop? = fpMap[fp]
    @Synchronized fun byContent(key: Long): ArtBackdrop? = contentMap[key]
    @Synchronized fun put(v: ArtBackdrop, fp: Long?) {
        contentMap[v.key] = v
        if (fp != null) fpMap[fp] = v
    }
}

/** Cheap fingerprint of the SOURCE (size + 16 sampled pixels) so repeat calls skip all work. */
private fun sourceFingerprint(src: Bitmap): Long? {
    if (src.isRecycled) return null
    if (Build.VERSION.SDK_INT >= 26 && src.config == Bitmap.Config.HARDWARE) return null
    var h = src.width * 31L + src.height
    val w = src.width
    val hh = src.height
    for (i in 0 until 4) for (j in 0 until 4) {
        val x = ((i + 0.5f) / 4f * w).toInt().coerceIn(0, w - 1)
        val y = ((j + 0.5f) / 4f * hh).toInt().coerceIn(0, hh - 1)
        h = h * 1_000_003L + src.getPixel(x, y)
    }
    return h
}

private fun buildArtBackdrop(source: Bitmap): ArtBackdrop? {
    val fp = sourceFingerprint(source)
    if (fp != null) ArtCache.byFingerprint(fp)?.let { return it }
    return runCatching {
        var work: Bitmap = if (Build.VERSION.SDK_INT >= 26 && source.config == Bitmap.Config.HARDWARE) {
            source.copy(Bitmap.Config.ARGB_8888, false) ?: return@runCatching null
        } else source
        // Progressive halving keeps the result smooth (a single huge jump would alias).
        while (work.width > BLUR_TARGET_PX && work.height > 1) {
            val nw = max(BLUR_TARGET_PX, work.width / 2)
            val nh = max(1, (work.height.toLong() * nw / work.width).toInt())
            val next = Bitmap.createScaledBitmap(work, nw, nh, true)
            if (work !== source && work !== next) work.recycle()
            work = next
        }
        // Never hand the caller's own bitmap to the UI; keep a private tiny copy.
        if (work === source) work = source.copy(Bitmap.Config.ARGB_8888, false) ?: return@runCatching null
        val w = work.width
        val h = work.height
        val px = IntArray(w * h)
        work.getPixels(px, 0, w, 0, 0, w, h)
        var sum = 0.0
        for (c in px) {
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            sum += 0.2126 * r + 0.7152 * g + 0.0722 * b
        }
        val lum = if (px.isEmpty()) 1.0 else sum / px.size / 255.0
        // Quantise (>>3) before hashing so tiny re-encode differences give the same key.
        var key = w * 31L + h
        for (c in px) key = key * 1_000_003L + (c and 0xF8F8F8)
        val existing = ArtCache.byContent(key)
        if (existing != null) {
            ArtCache.put(existing, fp)
            existing
        } else {
            val out = ArtBackdrop(work, isDark = lum < 0.46, key = key)
            ArtCache.put(out, fp)
            out
        }
    }.getOrNull()
}

/** Main-thread safe: returns an already-built backdrop (16 pixel reads) or null. */
private fun peekArtBackdrop(source: Bitmap): ArtBackdrop? =
    sourceFingerprint(source)?.let { ArtCache.byFingerprint(it) }

/** Call from a background thread as soon as new artwork arrives so expanding is instant. */
fun prewarmArtBackdrop(source: Bitmap) { buildArtBackdrop(source) }

/**
 * Builds (off the main thread) the blurred backdrop for [source]. Null while loading/disabled.
 * When the new result is the same picture as the one already shown, the SAME instance is kept,
 * so nothing downstream (cross-fade, text colour) re-triggers.
 */
@Composable
fun rememberArtBackdrop(source: Bitmap?, enabled: Boolean = true): State<ArtBackdrop?> =
    produceState<ArtBackdrop?>(
        initialValue = if (enabled && source != null) runCatching { peekArtBackdrop(source) }.getOrNull() else null,
        source, enabled
    ) {
        if (!enabled || source == null) { value = null; return@produceState }
        val built = withContext(Dispatchers.Default) { buildArtBackdrop(source) }
        val prev = value
        // Keep the previous picture on screen if the rebuild failed; never flash to empty.
        value = if (built == null) prev else if (prev != null && prev.key == built.key) prev else built
    }

/** Lyric-shine sweep stepped at ~30 Hz. rememberInfiniteTransition re-ran composition on every
 *  vsync (60/120 Hz); a text sweep is visually identical at 30 Hz at a fraction of the CPU/GPU work. */
private const val SHINE_FRAME_MS = 33L

@Composable
fun rememberShinePhase(
    active: Boolean,
    durationMs: Int,
    from: Float = -280f,
    to: Float = 900f
): State<Float> {
    val phase = remember { mutableFloatStateOf(from) }
    LaunchedEffect(active, durationMs) {
        if (!active || durationMs <= 0) return@LaunchedEffect
        while (true) {
            com.android.island.core.ScreenState.awaitInteractive()
            val t = (SystemClock.uptimeMillis() % durationMs).toFloat() / durationMs
            phase.floatValue = from + (to - from) * t
            delay(SHINE_FRAME_MS)
        }
    }
    return phase
}

/** Slow 0..1 clock at ~30 Hz. Slow drift gains nothing from 60/120 Hz, and a delay() loop
 *  (instead of withFrameNanos) does not wake Choreographer on every vsync. */
@Composable
fun rememberDriftPhase(animate: Boolean, periodMs: Long = 24_000L): State<Float> {
    val phase = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        val start = SystemClock.uptimeMillis() - (phase.floatValue * periodMs).toLong()
        while (true) {
            com.android.island.core.ScreenState.awaitInteractive()
            phase.floatValue = ((SystemClock.uptimeMillis() - start) % periodMs).toFloat() / periodMs
            delay(34L)
        }
    }
    return phase
}

/**
 * Blurred artwork + lightweight fluid-glass motion.
 *
 * Cost model:
 *  - blurred bitmap is tiny and drawn as a normal image (GPU texture sample);
 *  - drift is a GPU layer transform at ~30 Hz (no re-rasterising, no recomposition);
 *  - three radial light blobs: brushes built once per size, only translated per tick;
 *  - when [animate] is false (low-power / ultra) nothing runs and the layer is static.
 *
 * Flicker fixes: a solid black base sits under the photo (no more see-through to whatever
 * app is behind the island); photos are drawn opaque and darkened by a single scrim instead of
 * using alpha, and the cross-fade layers the NEW picture over the OLD one, so there is never a
 * mid-fade dip in brightness.
 */
@Composable
fun FluidGlassBackdrop(
    art: ArtBackdrop?,
    accent: Color,
    photoAlpha: Float,
    animate: Boolean,
    modifier: Modifier = Modifier,
    /** Whole-backdrop opacity (0..1). Below 1 the island lets the app behind show through. */
    opacity: Float = 1f,
    /** Shared drift clock (so the edge lens and the photo move in sync). Null = own clock. */
    externalPhase: State<Float>? = null
) {
    val ownPhase = rememberDriftPhase(animate && externalPhase == null)
    val phase = externalPhase ?: ownPhase
    val op = opacity.coerceIn(0f, 1f)

    var top by remember { mutableStateOf(art) }
    var below by remember { mutableStateOf<ArtBackdrop?>(null) }
    val fade = remember { Animatable(1f) }
    LaunchedEffect(art) {
        if (art === top) return@LaunchedEffect
        val old = top
        top = art
        if (art == null) {
            below = null
            fade.snapTo(1f)
        } else if (old == null) {
            // First photo (async build finished after the panel opened): fade it in instead of popping.
            below = null
            if (animate) {
                fade.snapTo(0f)
                fade.animateTo(1f, tween(320))
            } else fade.snapTo(1f)
        } else {
            below = old
            fade.snapTo(0f)
            fade.animateTo(1f, tween(if (animate) 380 else 160))
            below = null
        }
    }

    Box(modifier.background(Color.Black.copy(alpha = op))) {
        below?.let { DriftImage(it, phase) { op } }
        top?.let { DriftImage(it, phase) { fade.value * op } }
        // Single darkening scrim replaces per-image alpha.
        val scrim = (1f - photoAlpha).coerceIn(0f, 1f)
        if (scrim > 0.001f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrim)))

        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val r1 = max(size.width, size.height) * 0.60f
                    val r2 = max(size.width, size.height) * 0.52f
                    val r3 = max(size.width, size.height) * 0.38f
                    val b1 = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(r1, r1), radius = r1
                    )
                    val b2 = Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.26f), Color.Transparent),
                        center = Offset(r2, r2), radius = r2
                    )
                    val b3 = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.13f), Color.Transparent),
                        center = Offset(r3, r3), radius = r3
                    )
                    onDrawBehind {
                        val t = phase.value * 2f * PI.toFloat()
                        val w = size.width
                        val h = size.height
                        translate(
                            left = w * (0.25f + 0.18f * sin(t)) - r1,
                            top = h * (0.30f + 0.28f * cos(t)) - r1
                        ) { drawCircle(b1, radius = r1, center = Offset(r1, r1)) }
                        translate(
                            left = w * (0.78f + 0.16f * cos(t * 2f)) - r2,
                            top = h * (0.72f + 0.26f * sin(t * 2f)) - r2
                        ) { drawCircle(b2, radius = r2, center = Offset(r2, r2)) }
                        translate(
                            left = w * (0.55f + 0.22f * sin(t + 2.1f)) - r3,
                            top = h * (0.40f + 0.30f * cos(t + 2.1f)) - r3
                        ) { drawCircle(b3, radius = r3, center = Offset(r3, r3)) }
                    }
                }
        )
    }
}

@Composable
private fun DriftImage(art: ArtBackdrop, phase: State<Float>, alphaProvider: () -> Float) {
    // asImageBitmap wraps without copying; remember keeps one wrapper per backdrop.
    val image = remember(art) { art.bitmap.asImageBitmap() }
    // Saturation lift (1.55x, up from 1.3x for a livelier, more glassy colour) makes the blurred photo read as rich, vibrant liquid glass (like a
    // refracted album cover). A colour matrix is applied by the GPU in the same draw call.
    val vibrance = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.55f) }) }
    Image(
        bitmap = image,
        contentDescription = null,
        colorFilter = vibrance,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val t = phase.value * 2f * PI.toFloat()
                scaleX = 1.22f
                scaleY = 1.22f
                translationX = sin(t) * size.width * 0.035f
                translationY = cos(t) * size.height * 0.05f
                alpha = alphaProvider()
            }
    )
}
