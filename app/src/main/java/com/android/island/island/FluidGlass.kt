package com.android.island.island
import kotlin.math.roundToInt
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize

import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin

/**
 * A bounded cached artwork/avatar source plus whether it is dark (for adaptive text colour).
 * Gaussian frost is applied by the GPU RenderEffect on the displayed artwork layer, not by
 * repeatedly reading or filtering pixels on the CPU.
 *
 * [key] is a hash of the *content* of the tiny bitmap. Two different Bitmap instances of the
 * same picture produce the same key, so the UI can tell "same picture" from "new picture"
 * (the old identity-based check re-faded the backdrop whenever the player re-sent the art).
 */
@Immutable
class ArtBackdrop(val bitmap: Bitmap, val isDark: Boolean, val key: Long)

private val ART_MAX_PX: Int get() = com.android.island.core.DeviceProfile.artMaxPx
private val CACHE_SIZE: Int get() = com.android.island.core.DeviceProfile.artCacheSize

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
    /** Drops everything except the most recently used picture (the one on screen or about to be). */
    @Synchronized fun trimToNewest() {
        for (m in listOf(contentMap, fpMap)) {
            val drop = m.size - 1
            if (drop <= 0) continue
            val it = m.entries.iterator()
            var n = 0
            while (it.hasNext() && n < drop) { it.next(); it.remove(); n++ }
        }
    }
}

/** Called when the system asks the app to give memory back. Older artwork copies are rebuilt on demand. */
fun trimArtCaches() = ArtCache.trimToNewest()

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
        while (work.width > ART_MAX_PX && work.height > 1) {
            val nw = max(ART_MAX_PX, work.width / 2)
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
 * Builds (off the main thread) a bounded artwork backdrop for [source]. GPU effects are applied at draw time. Null while loading/disabled.
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
            delay(com.android.island.core.DeviceProfile.tickMs)
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
            delay(com.android.island.core.DeviceProfile.tickMs)
        }
    }
    return phase
}

/**
 * Gentle, smooth frost. [amount] 0..1 maps to a Gaussian sigma of 0..12 artwork pixels, so a small
 * slider value (e.g. 4 %) is a barely-there softening instead of a heavy smear.
 *
 * Quality: the old version always shrank the picture to 1/8 and then stretched it back, which left
 * visible blocks. Now the picture is only shrunk as far as the radius allows (1x, 2x or 4x), blurred
 * with three box passes that approximate a true Gaussian, and scaled back with bilinear filtering.
 * Computed once per artwork and slider step off the main thread; no GPU blur pass per frame.
 */
fun cheapBlurBitmap(src: Bitmap, amount: Float): Bitmap {
    val a = amount.coerceIn(0f, 1f)
    val sigma = a * 12f
    if (sigma < 0.30f) return src
    val ds = when {
        sigma < 2.5f -> 1
        sigma < 6f -> 2
        else -> 4
    }
    val sw = (src.width / ds).coerceAtLeast(8)
    val sh = (src.height / ds).coerceAtLeast(8)
    val small = if (ds == 1) src.copy(Bitmap.Config.ARGB_8888, true)
        else Bitmap.createScaledBitmap(src, sw, sh, true)
    val w = small.width
    val h = small.height
    val px = IntArray(w * h)
    small.getPixels(px, 0, w, 0, 0, w, h)
    var buf = px
    var tmp = IntArray(px.size)   // second buffer: passes ping-pong between the two instead of allocating six arrays
    for (r in gaussianBoxRadii(sigma / ds)) {
        if (r <= 0) continue
        blurAxis(buf, tmp, w, h, r, horizontal = true)
        var t = buf; buf = tmp; tmp = t
        blurAxis(buf, tmp, w, h, r, horizontal = false)
        t = buf; buf = tmp; tmp = t
    }
    small.setPixels(buf, 0, w, 0, 0, w, h)
    if (ds == 1) return small
    val out = Bitmap.createScaledBitmap(small, src.width, src.height, true)
    if (out !== small) small.recycle()
    return out
}

/** Three box radii whose combined response approximates a Gaussian of the given sigma. */
private fun gaussianBoxRadii(sigma: Float): IntArray {
    val n = 3
    val wIdeal = kotlin.math.sqrt(12f * sigma * sigma / n + 1f)
    var wl = kotlin.math.floor(wIdeal).toInt()
    if (wl % 2 == 0) wl--
    wl = wl.coerceAtLeast(1)
    val wu = wl + 2
    val mIdeal = (12f * sigma * sigma - n * wl * wl - 4f * n * wl - 3f * n) / (-4f * wl - 4f)
    val m = mIdeal.roundToInt().coerceIn(0, n)
    return IntArray(n) { i -> (((if (i < m) wl else wu) - 1) / 2) }
}

/** Box blur along one axis of a packed ARGB buffer, with edge clamping. */
private fun blurAxis(src: IntArray, out: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
    val len = if (horizontal) w else h
    val lines = if (horizontal) h else w
    val div = 2 * r + 1
    for (line in 0 until lines) {
        fun at(i: Int): Int = if (horizontal) line * w + i else i * w + line
        var sa = 0
        var sr = 0
        var sg = 0
        var sb = 0
        for (k in -r..r) {
            val c = src[at(k.coerceIn(0, len - 1))]
            sa += c ushr 24
            sr += (c shr 16) and 0xFF
            sg += (c shr 8) and 0xFF
            sb += c and 0xFF
        }
        for (i in 0 until len) {
            out[at(i)] = ((sa / div) shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
            val rem = src[at((i - r).coerceIn(0, len - 1))]
            val add = src[at((i + r + 1).coerceIn(0, len - 1))]
            sa += (add ushr 24) - (rem ushr 24)
            sr += ((add shr 16) and 0xFF) - ((rem shr 16) and 0xFF)
            sg += ((add shr 8) and 0xFF) - ((rem shr 8) and 0xFF)
            sb += (add and 0xFF) - (rem and 0xFF)
        }
    }
}

/** iOS glass lifts the colour of what sits behind it a little. One cheap colour matrix on the artwork layer, no extra pass. */
private val GlassVibrance = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.22f) })

/** Artwork layer. [fitBounds] is used for a baked lens bitmap that is already cropped to the surface. */
@Composable
private fun ArtLayer(bitmap: Bitmap, fitBounds: Boolean, alpha: () -> Float) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = alpha().coerceIn(0f, 1f)
                // One draw call: apply alpha directly, no full-surface offscreen texture while cross-fading.
                compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
            },
        contentScale = if (fitBounds) ContentScale.FillBounds else ContentScale.Crop,
        colorFilter = GlassVibrance,
        filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium
    )
}

/**
 * Liquid-glass artwork backdrop.
 *
 * Cost model (what changed from the previous version):
 *  - frost: a cheap down/up-scaled blur, cached per artwork and slider step (no RenderEffect per frame);
 *  - lens (Android 13+): baked once per artwork and surface size at half resolution, then drawn as an image;
 *  - edge sheen: path strokes on each surface (see edgeSheenOverlay), no shader;
 *  - artwork cache stays bounded (see ArtCache).
 *
 * Layer separation is unchanged: this composable draws only the glass. Text and controls are siblings
 * placed above it. [animate] and [externalPhase] are kept so existing call sites compile.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun FluidGlassBackdrop(
    art: ArtBackdrop?,
    accent: Color,
    photoAlpha: Float,
    animate: Boolean,
    modifier: Modifier = Modifier,
    /** Artwork bitmap opacity (0..1), independent from glass fill and refraction. */
    opacity: Float = 1f,
    externalPhase: State<Float>? = null,
    /** Full-surface lens strength (0..1). Baked once per artwork and size (Android 13+). */
    lensStrength: Float = 0f,
    lensCornerDp: Float = 24f,
    waveStrength: Float = 0f,
    /** 0 = crisp artwork; 1 = maximum frost (cheap blur, cached). */
    frostStrength: Float = 0f
) {
    // This control is artwork opacity only; it must never tint or frost the glass material.
    val artworkAlpha = opacity.coerceIn(0f, 1f)
    // Exynos mid-range / weakest tier: flat two-tone glass, no frost, lens or artwork bitmap layers.
    if (com.android.island.core.DeviceProfile.flatGlass) {
        FlatGlassBackdrop(art, accent, photoAlpha, artworkAlpha, modifier)
        return
    }
    val density = LocalDensity.current.density

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
            below = null
            // Late-arriving artwork fades in instead of popping (prewarmed art is already there on first composition).
            fade.snapTo(0f)
            fade.animateTo(1f, tween(if (animate) 420 else 260, easing = IosEase))
        } else {
            // New picture is layered over the old one: no brightness dip in the middle of the fade.
            below = old
            fade.snapTo(0f)
            fade.animateTo(1f, tween(if (animate) 520 else 300, easing = IosEase))
            below = null
        }
    }

    // Frost: a pre-blurred copy of the artwork, computed once per artwork and slider step.
    val frostStep = (frostStrength.coerceIn(0f, 1f) * 50f).roundToInt()
    val frosted: Bitmap? by produceState<Bitmap?>(initialValue = null, top?.key, frostStep) {
        value = null
        val t = top ?: return@produceState
        if (frostStep <= 0) return@produceState
        value = withContext(Dispatchers.Default) { cheapBlurBitmap(t.bitmap, frostStep / 50f) }
    }

    // Lens: baked once per source and surface size (Android 13+). Static, so it is never recomputed per frame.
    val source: Bitmap? = frosted ?: top?.bitmap
    var boxPx by remember { mutableStateOf(IntSize.Zero) }
    val lensed: Bitmap? by produceState<Bitmap?>(initialValue = null, source, boxPx, lensStrength, lensCornerDp, density) {
        value = null
        val s = source ?: return@produceState
        if (lensStrength <= 0.01f || !com.android.island.core.DeviceProfile.lensEnabled) return@produceState
        if (boxPx.width < 2 || boxPx.height < 2) return@produceState
        value = withContext(Dispatchers.Default) {
            bakeLensBitmap(s, boxPx.width, boxPx.height, lensCornerDp, density, lensStrength)
        }
    }

    Box(modifier.onSizeChanged { boxPx = it }) {
        below?.let { ArtLayer(bitmap = it.bitmap, fitBounds = false) { artworkAlpha } }
        top?.let { t ->
            ArtLayer(
                bitmap = lensed ?: frosted ?: t.bitmap,
                fitBounds = lensed != null
            ) { fade.value * artworkAlpha }
        }

        // Optional photographic dimming remains independent from artwork opacity.
        val scrim = (1f - photoAlpha).coerceIn(0f, 1f)
        // Scrim is scaled by artwork opacity: at 0% nothing is darkened.
        val scrimAlpha = scrim * artworkAlpha
        if (scrimAlpha > 0.001f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha)))

        // Static light on the glass: soft highlight top-left, accent glow bottom-right and a faint
        // diagonal sheen. Frost is independently adjustable; zero leaves the artwork crisp.
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val big = max(size.width, size.height)
                    val hi = Brush.radialGradient(
                        colorStops = easedStops(Color.White, 0.10f),
                        center = Offset(size.width * 0.18f, size.height * 0.12f),
                        radius = big * 0.55f
                    )
                    val glow = Brush.radialGradient(
                        colorStops = easedStops(accent, 0.11f),
                        center = Offset(size.width * 0.86f, size.height * 0.92f),
                        radius = big * 0.50f
                    )
                    val sheen = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.42f to Color.White.copy(alpha = 0.035f),
                            0.58f to Color.White.copy(alpha = 0.008f),
                            1.00f to Color.Transparent
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                    onDrawBehind {
                        // Frost veil follows its own slider and disappears completely at zero.
                        drawRect(hi)
                        drawRect(glow)
                        drawRect(sheen)
                    }
                }
        )
    }
}


/** Average colour of the top and bottom half of the artwork (4x4 sample): the flat-glass tint. */
private fun artTwoTone(src: Bitmap): Pair<Color, Color> {
    val soft = if (src.config == Bitmap.Config.HARDWARE) src.copy(Bitmap.Config.ARGB_8888, false) else src
    val base = soft ?: return Color(0xFF1C1C1E) to Color(0xFF111113)
    val small = Bitmap.createScaledBitmap(base, 4, 4, true)
    val px = IntArray(16)
    small.getPixels(px, 0, 4, 0, 0, 4, 4)
    if (small !== base) small.recycle()
    if (soft != null && soft !== src) soft.recycle()
    fun avg(from: Int, to: Int): Color {
        var r = 0; var g = 0; var b = 0
        for (i in from until to) {
            r += (px[i] shr 16) and 0xFF; g += (px[i] shr 8) and 0xFF; b += px[i] and 0xFF
        }
        val n = (to - from).coerceAtLeast(1)
        return Color(r / n, g / n, b / n)
    }
    return avg(0, 8) to avg(8, 16)
}

/**
 * Flat glass for weak GPUs: a vertical two-tone gradient from the artwork's own colours, the same scrim and the
 * same soft highlight/glow as the full version. Looks like the artwork's mood without any bitmap drawing.
 */
@Composable
private fun FlatGlassBackdrop(
    art: ArtBackdrop?,
    accent: Color,
    photoAlpha: Float,
    artworkAlpha: Float,
    modifier: Modifier
) {
    val tone: Pair<Color, Color>? by produceState<Pair<Color, Color>?>(initialValue = null, art?.key) {
        val a = art
        value = if (a == null) null else withContext(Dispatchers.Default) { runCatching { artTwoTone(a.bitmap) }.getOrNull() }
    }
    Box(modifier) {
        tone?.let { (topColor, bottomColor) ->
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = artworkAlpha
                        compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
                    }
                    .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
            )
        }
        val scrimAlpha = (1f - photoAlpha).coerceIn(0f, 1f) * artworkAlpha
        if (scrimAlpha > 0.001f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha)))
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val big = max(size.width, size.height)
                    val hi = Brush.radialGradient(
                        colorStops = easedStops(Color.White, 0.10f),
                        center = Offset(size.width * 0.18f, size.height * 0.12f),
                        radius = big * 0.55f
                    )
                    val glow = Brush.radialGradient(
                        colorStops = easedStops(accent, 0.11f),
                        center = Offset(size.width * 0.86f, size.height * 0.92f),
                        radius = big * 0.50f
                    )
                    onDrawBehind {
                        drawRect(hi)
                        drawRect(glow)
                    }
                }
        )
    }
}

/**
 * Colour stops that fade [color] from [peak] alpha to nothing along a smoothstep curve.
 * A plain two-stop gradient has a visible corner where it starts and ends; eight eased stops
 * make the falloff read as one soft glow with no rings or edges.
 */
internal fun easedStops(color: Color, peak: Float, steps: Int = 8): Array<Pair<Float, Color>> =
    Array(steps + 1) { i ->
        val t = i / steps.toFloat()
        val k = 1f - t
        val eased = k * k * (3f - 2f * k)
        t to color.copy(alpha = peak * eased)
    }
