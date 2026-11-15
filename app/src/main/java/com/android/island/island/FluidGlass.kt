package com.android.island.island

import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.IntOffset
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
    /** Last time each picture was used (content key -> ms). Drives idle expiry. */
    private val lastUse = HashMap<Long, Long>()
    @Synchronized fun byFingerprint(fp: Long): ArtBackdrop? = fpMap[fp]?.also { lastUse[it.key] = System.currentTimeMillis() }
    @Synchronized fun byContent(key: Long): ArtBackdrop? = contentMap[key]?.also { lastUse[key] = System.currentTimeMillis() }
    @Synchronized fun put(v: ArtBackdrop, fp: Long?) {
        contentMap[v.key] = v
        if (fp != null) fpMap[fp] = v
        lastUse[v.key] = System.currentTimeMillis()
    }
    /** Drops pictures unused for longer than [maxAgeMs]. The picture on screen (most recently used) is kept. */
    @Synchronized fun expireIdle(maxAgeMs: Long, now: Long) {
        val newest = contentMap.keys.lastOrNull()
        for (m in listOf(contentMap, fpMap)) {
            m.values.removeAll { v -> v.key != newest && now - (lastUse[v.key] ?: 0L) > maxAgeMs }
        }
        val alive = contentMap.values.map { it.key }.toSet()
        lastUse.keys.retainAll(alive)
    }
    /** Drops every cached image reference. Active UI owners keep their own reference until recomposition/disposal. */
    @Synchronized fun clearAll() {
        contentMap.clear()
        fpMap.clear()
        lastUse.clear()
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
fun trimArtCaches() {
    // Low-End does not use an artwork LRU at all: the currently visible activity owns its
    // already-bounded source bitmap. Retaining a second cached copy defeats the minimal profile.
    if (com.android.island.core.DeviceProfile.staticGlass) ArtCache.clearAll()
    else ArtCache.trimToNewest()
    clearCachedLensEffect()
    clearCachedLensBitmap()
}

/** Removes artwork copies that have not been used for [maxAgeMs]. Called after the screen has been off for a while. */
fun trimIdleArt(maxAgeMs: Long) {
    if (com.android.island.core.DeviceProfile.staticGlass) ArtCache.clearAll()
    else ArtCache.expireIdle(maxAgeMs, System.currentTimeMillis())
}

/** Long screen-off: release the baked lens bitmap and lens effect (rebuilt on the next expand). */
fun trimDeepIdleCaches() {
    clearCachedLensBitmap()
    clearCachedLensEffect()
}

/** Cheap source fingerprint: sampled content for software bitmaps, stable instance identity for hardware bitmaps. */
private fun sourceFingerprint(src: Bitmap): Long? {
    if (src.isRecycled) return null
    if (Build.VERSION.SDK_INT >= 26 && src.config == Bitmap.Config.HARDWARE) {
        // Hardware bitmaps cannot be sampled with getPixel(). Use a stable per-instance
        // fingerprint so recompositions of the same player artwork skip the CPU copy,
        // pixel scan and temporary bitmap allocation. A newly delivered Bitmap instance
        // still falls through the content-key path in buildArtBackdrop().
        return (System.identityHashCode(src).toLong() shl 32) xor
            (src.width.toLong() shl 16) xor src.height.toLong()
    }
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
            // Draw the hardware bitmap straight into a small software bitmap. The previous full-size copy
            // allocated the whole cover (about 8 MB for a 1400 px image) on every track change.
            val k = minOf(1f, ART_MAX_PX.toFloat() / max(source.width, source.height).toFloat())
            val nw = (source.width * k).toInt().coerceAtLeast(1)
            val nh = (source.height * k).toInt().coerceAtLeast(1)
            val small = Bitmap.createBitmap(nw, nh, Bitmap.Config.ARGB_8888)
            android.graphics.Canvas(small).drawBitmap(
                source,
                android.graphics.Rect(0, 0, source.width, source.height),
                android.graphics.Rect(0, 0, nw, nh),
                android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
            )
            small
        } else source
        // Scale by the longest edge (not width alone): portrait notification artwork must be bounded too.
        // Progressive halving keeps the result smooth while limiting peak temporary allocations.
        while (max(work.width, work.height) > ART_MAX_PX) {
            val scale = ART_MAX_PX.toFloat() / max(work.width, work.height).toFloat()
            val nw = (work.width * scale).toInt().coerceAtLeast(1)
            val nh = (work.height * scale).toInt().coerceAtLeast(1)
            val next = Bitmap.createScaledBitmap(work, nw, nh, true)
            if (work !== source && work !== next && !work.isRecycled) work.recycle()
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
            // The cache already owns an equivalent compact bitmap; do not leave this temporary copy alive.
            if (work !== source && !work.isRecycled) work.recycle()
            ArtCache.put(existing, fp)
            existing
        } else {
            // Cover art is opaque: RGB_565 keeps the same look at half the memory of ARGB_8888.
            val compact = work.copy(Bitmap.Config.RGB_565, false)
            val retained = compact ?: work
            if (compact != null && work !== source && !work.isRecycled) work.recycle()
            val out = ArtBackdrop(retained, isDark = lum < 0.46, key = key)
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
fun rememberArtBackdrop(source: Bitmap?, enabled: Boolean = true): State<ArtBackdrop?> {
    val lowEnd = com.android.island.core.DeviceProfile.staticGlass
    return produceState<ArtBackdrop?>(
        initialValue = if (!enabled || source == null) null else if (lowEnd) {
            // Low-End owns no backdrop copy and no LRU entry. The already-bounded activity bitmap
            // is drawn directly; this avoids a second native bitmap for every avatar/cover.
            ArtBackdrop(source, isDark = false, key = (System.identityHashCode(source).toLong() shl 32) xor source.width.toLong() xor source.height.toLong())
        } else runCatching { peekArtBackdrop(source) }.getOrNull(),
        source, enabled, lowEnd
    ) {
        if (!enabled || source == null) { value = null; return@produceState }
        if (lowEnd) {
            value = ArtBackdrop(source, isDark = false, key = (System.identityHashCode(source).toLong() shl 32) xor source.width.toLong() xor source.height.toLong())
            return@produceState
        }
        val built = withContext(Dispatchers.Default) { buildArtBackdrop(source) }
        val prev = value
        // Keep the previous picture on screen if the rebuild failed; never flash to empty.
        value = if (built == null) prev else if (prev != null && prev.key == built.key) prev else built
    }
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

/**
 * Lens layer with a one-shot cache. While the surface is morphing, the lens shader runs every frame.
 * Once the size has been stable for a moment, the lensed artwork is recorded once into a bitmap, and that
 * bitmap is drawn until the artwork, the size or a lens setting changes (the key changes). Same pixels,
 * without running the shader each frame.
 */
/** One retained half-resolution shader result for the current artwork/settings key. */
private object LensBitmapCache {
    private var cachedKey: String? = null
    private var cachedImage: ImageBitmap? = null

    @Synchronized fun get(key: String): ImageBitmap? = if (cachedKey == key) cachedImage else null
    @Synchronized fun put(key: String, image: ImageBitmap) {
        cachedKey = key
        cachedImage = image
    }
    @Synchronized fun clear() {
        cachedKey = null
        cachedImage = null
    }
}

private fun clearCachedLensBitmap() = LensBitmapCache.clear()
private fun hasCachedLensBitmap(key: String): Boolean = LensBitmapCache.get(key) != null

@Composable
private fun LensCachedArt(bitmap: Bitmap, effect: androidx.compose.ui.graphics.RenderEffect?, key: String, sizeKey: Any, alpha: () -> Float) {
    // Seed from the process-wide one-entry cache so closing/reopening the panel does not
    // re-run the AGSL shader for unchanged artwork. A changed artwork/settings key replaces it.
    var cached by remember(key) { mutableStateOf(LensBitmapCache.get(key)?.let { key to it }) }
    val hit = cached?.takeIf { it.first == key }?.second
    if (hit != null) {
        // One recorded result per artwork: drawn scaled to the current size, so the shader does not run again.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.alpha = alpha().coerceIn(0f, 1f)
                    compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
                }
                .drawWithContent {
                    drawImage(
                        image = hit,
                        dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)),
                        filterQuality = FilterQuality.Medium
                    )
                }
        )
    } else if (effect != null) {
        LiveLensArt(bitmap, effect, key, sizeKey, alpha) {
            LensBitmapCache.put(it.first, it.second)
            cached = it
        }
    } else {
        // Defensive fallback if memory pressure cleared the cache between the lookup and draw.
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha().coerceIn(0f, 1f) },
            contentScale = ContentScale.Crop,
            colorFilter = GlassVibrance
        )
    }
}

/**
 * Live branch: the shader runs here once per artwork. It records at HALF resolution and is scaled back up
 * when drawn, which cuts the layer and bitmap memory to a quarter. Capture waits until the size has settled.
 */
@Composable
private fun LiveLensArt(
    bitmap: Bitmap,
    effect: androidx.compose.ui.graphics.RenderEffect,
    key: String,
    sizeKey: Any,
    alpha: () -> Float,
    onCaptured: (Pair<String, ImageBitmap>) -> Unit
) {
    val layer = rememberGraphicsLayer()
    val src = remember(bitmap) { bitmap.asImageBitmap() }
    LaunchedEffect(key, sizeKey) {
        delay(160L)
        val sz = layer.size
        if (sz.width > 0 && sz.height > 0) {
            // GraphicsLayer.toImageBitmap() returns a Config.HARDWARE bitmap: its pixels live in a
            // GPU-resident GraphicsBuffer, not the normal heap. Caching that directly means the
            // PREVIOUS cached entry's hardware buffer is only released whenever its finalizer
            // eventually runs, which lags behind quick track changes and reads as a GL/EGL mtrack
            // climb. Copying once to plain ARGB_8888 (still half-resolution, per the bake above)
            // moves the cached entry onto the ordinary heap; the hardware copy is recycled right
            // after, which is safe because it has had no other owner since the line above.
            val hwBitmap = layer.toImageBitmap().asAndroidBitmap()
            val swBitmap = hwBitmap.copy(Bitmap.Config.ARGB_8888, false)
            hwBitmap.recycle()
            onCaptured(key to swBitmap.asImageBitmap())
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = alpha().coerceIn(0f, 1f)
                compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
            }
            .drawWithContent {
                layer.renderEffect = effect
                val w = (size.width / 2f).roundToInt().coerceAtLeast(1)
                val h = (size.height / 2f).roundToInt().coerceAtLeast(1)
                layer.record(IntSize(w, h)) { drawCroppedArt(src, Size(w.toFloat(), h.toFloat()), GlassVibrance) }
                scale(2f, 2f, Offset.Zero) { drawLayer(layer) }
            }
    )
}

/** Center-crop, same as ContentScale.Crop: fill the destination without distortion. */
private fun DrawScope.drawCroppedArt(src: ImageBitmap, dst: Size, colorFilter: ColorFilter?) {
    val sw = src.width.toFloat()
    val sh = src.height.toFloat()
    val scale = maxOf(dst.width / sw, dst.height / sh)
    val cw = dst.width / scale
    val ch = dst.height / scale
    drawImage(
        image = src,
        srcOffset = IntOffset(((sw - cw) / 2f).roundToInt(), ((sh - ch) / 2f).roundToInt()),
        srcSize = IntSize(cw.roundToInt().coerceAtLeast(1), ch.roundToInt().coerceAtLeast(1)),
        dstSize = IntSize(dst.width.roundToInt().coerceAtLeast(1), dst.height.roundToInt().coerceAtLeast(1)),
        colorFilter = colorFilter,
        filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium
    )
}

/** Artwork layer. [fitBounds] is used for a baked lens bitmap that is already cropped to the surface. */
@Composable
private fun ArtLayer(bitmap: Bitmap, fitBounds: Boolean, renderEffect: androidx.compose.ui.graphics.RenderEffect? = null, cacheKey: String = "", sizeKey: Any = Unit, alpha: () -> Float) {
    if (cacheKey.isNotEmpty()) {
        LensCachedArt(bitmap, renderEffect, cacheKey, sizeKey, alpha)
        return
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val a = alpha().coerceIn(0f, 1f)
                this.alpha = a
                // Skip the lens layer entirely while the artwork is invisible; the glass shape itself is unchanged.
                this.renderEffect = if (a > 0.001f) renderEffect else null
                // One draw call: apply alpha directly, no full-surface offscreen texture while cross-fading.
                compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
            },
        contentScale = if (fitBounds) ContentScale.FillBounds else ContentScale.Crop,
        colorFilter = GlassVibrance,
        filterQuality = if (fitBounds) androidx.compose.ui.graphics.FilterQuality.High else androidx.compose.ui.graphics.FilterQuality.Medium
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
    // Legacy flat-glass fallback; disabled by DeviceProfile to preserve the selected visual style.
    if (com.android.island.core.DeviceProfile.flatGlass) {
        FlatGlassBackdrop(art, accent, photoAlpha, artworkAlpha, modifier)
        return
    }
    if (com.android.island.core.DeviceProfile.staticGlass) {
        StaticGlassBackdrop(art, photoAlpha, artworkAlpha, modifier)
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
    // Blur on the GPU (RenderEffect). Used only when the lens is off; with the lens, blur is part of its chain.
    val blurFx = remember(frostStrength, density) { buildBlurEffect(frostStrength, density) }

    // Lens: baked once per source and surface size (Android 13+). Static, so it is never recomputed per frame.
    // Refraction is baked from the CRISP artwork. Blur must never soften the image that gets bent.
    val source: Bitmap? = top?.bitmap
    var boxPx by remember { mutableStateOf(IntSize.Zero) }
    // The lens strength comes from the edge-lens setting only. 0 = no lens.
    val lensAmount = lensStrength
    val lensKey = "${top?.key}|$lensAmount|$lensCornerDp|$frostStrength|${GlassTuning.lowEnd}"
    // If this artwork/settings pair already has a baked half-resolution result, do not
    // even instantiate a RuntimeShader on reopen. The cached image is the render source.
    val bakedLensReady = remember(lensKey) { hasCachedLensBitmap(lensKey) }
    // GPU lens: create the effect only for a cache miss. It is retained globally for this
    // artwork + geometry and the rasterized result is retained separately across panel removal.
    val lensEffect: androidx.compose.ui.graphics.RenderEffect? = remember(top?.key, boxPx, lensAmount, lensCornerDp, density, frostStrength, GlassTuning.lowEnd, bakedLensReady) {
        if (bakedLensReady || top == null || boxPx.width < 2 || boxPx.height < 2 || lensAmount <= 0.01f) null
        else if (!com.android.island.core.DeviceProfile.lensEnabled) {
            com.android.island.util.IslandDiagnostics.record("LENS_SKIPPED", "reason=tier_disabled tier=${com.android.island.core.DeviceProfile.tier}")
            null
        } else if (android.os.Build.VERSION.SDK_INT >= 33) {
            val divisor = com.android.island.core.DeviceProfile.lensDivisor
            cachedLensEffect(
                artKey = top?.key ?: 0L,
                widthPx = (boxPx.width / divisor).coerceAtLeast(2),
                heightPx = (boxPx.height / divisor).coerceAtLeast(2),
                cornerDp = lensCornerDp,
                density = density / divisor,
                strength = lensAmount,
                blur = frostStrength
            )
        }
        else null
    }
    Box(modifier.onSizeChanged { boxPx = it }) {
        below?.let { ArtLayer(bitmap = it.bitmap, fitBounds = false) { artworkAlpha } }
        top?.let { t ->
            // Same as the reference: the lens bends the cached, pre-blurred artwork (smooth CPU blur).
            // Without frost the source is the crisp artwork.
            if (lensEffect != null || bakedLensReady) {
                ArtLayer(bitmap = t.bitmap, fitBounds = false, renderEffect = lensEffect, cacheKey = lensKey, sizeKey = boxPx) { fade.value * artworkAlpha }
            } else {
                ArtLayer(bitmap = t.bitmap, fitBounds = false, renderEffect = blurFx) { fade.value * artworkAlpha }
            }
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


/**
 * Low-End backdrop: draw the activity's already-bounded source bitmap directly under one flat dark scrim.
 * No second bitmap, LRU entry, reflection strokes, gradients, blur, shader, or ambient animation.
 */
@Composable
private fun StaticGlassBackdrop(
    art: ArtBackdrop?,
    photoAlpha: Float,
    artworkAlpha: Float,
    modifier: Modifier
) {
    val artImage = remember(art?.bitmap) { art?.bitmap?.asImageBitmap() }
    val scrimAlpha = ((1f - photoAlpha).coerceIn(0f, 1f) * 0.42f).coerceIn(0f, 0.55f)
    Box(modifier.background(Color(0xFF17191D))) {
        artImage?.let { image ->
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = artworkAlpha.coerceIn(0f, 1f),
                filterQuality = FilterQuality.Low
            )
        }
        if (scrimAlpha > 0.001f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha)))
        }
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

/** GPU blur for the artwork layer. Returns null when there is no blur. */
fun buildBlurEffect(frost: Float, density: Float): androidx.compose.ui.graphics.RenderEffect? {
    // Hard guard: Low-End must never instantiate a RenderEffect, even if a caller passes frost > 0.
    if (com.android.island.core.DeviceProfile.staticGlass) return null
    val r = frost.coerceIn(0f, 1f) * 20f * density
    if (r <= 0.01f) return null
    return android.graphics.RenderEffect.createBlurEffect(r, r, android.graphics.Shader.TileMode.CLAMP).asComposeRenderEffect()
}
