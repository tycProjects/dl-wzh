package com.android.island.island

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.max
import kotlin.math.min

/**
 * Live glass tuning from Settings. IslandRoot syncs it after every settings change.
 * Backed by Compose state, so composables that read a value recompose when it changes
 * (a plain field would leave skipped composables showing stale values).
 */
object GlassTuning {
    var domeStrength: Float by mutableFloatStateOf(1f)
    var domeRelief: Float by mutableFloatStateOf(0.5f)
    var sheenStrength: Float by mutableFloatStateOf(1f)
    var glassSurfaceOpacity: Float by mutableFloatStateOf(1f)
    var stackGapDp: Float by mutableFloatStateOf(8f)
    var stackCapDp: Float by mutableFloatStateOf(300f)
    var stackInsetDp: Float by mutableFloatStateOf(8f)
}

/**
 * Artwork-local liquid lens for Android 13+.
 *
 * The lens only REFRACTS the pixels it is given (album art / avatar). It never invents colour:
 * output alpha follows the source alpha, so an artwork opacity of 0% produces a fully transparent
 * layer (no black fill). The edge sheen is drawn by [edgeSheenOverlay] on
 * the glass itself, so the domes are visible on every card, with or without artwork.
 */

/**
 * Bakes the full-surface lens for [art] into a HALF-resolution bitmap (API 33+).
 *
 * The lens is static (no time input), so its output for a given artwork, size and strength never
 * changes. Computing it once and drawing the bitmap is cheaper than running the shader every frame.
 * The shader runs in half-resolution coordinates with every pixel-based parameter halved, so the
 * geometry matches the full-resolution lens. The bitmap is scaled back up when drawn.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun bakeLensBitmap(
    art: Bitmap,
    widthPx: Int,
    heightPx: Int,
    cornerDp: Float,
    density: Float,
    strength: Float
): Bitmap? {
    if (strength <= 0.01f || widthPx < 2 || heightPx < 2 || art.width < 1 || art.height < 1) return null
    // Any shader / driver / memory failure (some Mali, PowerVR and old Adreno drivers) falls back to the plain artwork.
    return runCatching { bakeLensBitmapUnsafe(art, widthPx, heightPx, cornerDp, density, strength) }.getOrNull()
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun bakeLensBitmapUnsafe(
    art: Bitmap,
    widthPx: Int,
    heightPx: Int,
    cornerDp: Float,
    density: Float,
    strength: Float
): Bitmap? {
    val div = com.android.island.core.DeviceProfile.lensDivisor
    val inv = 1f / div
    val hw = (widthPx / div).coerceAtLeast(1)
    val hh = (heightPx / div).coerceAtLeast(1)
    // Cover-crop the artwork into half-resolution space, like ContentScale.Crop.
    val cropped = Bitmap.createBitmap(hw, hh, Bitmap.Config.ARGB_8888)
    val k = max(hw / art.width.toFloat(), hh / art.height.toFloat())
    val m = Matrix().apply {
        setScale(k, k)
        postTranslate((hw - art.width * k) / 2f, (hh - art.height * k) / 2f)
    }
    val soft = if (art.config == Bitmap.Config.HARDWARE) art.copy(Bitmap.Config.ARGB_8888, false) else art
    if (soft == null) { cropped.recycle(); return null }
    Canvas(cropped).drawBitmap(soft, m, Paint(Paint.FILTER_BITMAP_FLAG))
    if (soft !== art) soft.recycle()
    val shortSide = min(widthPx, heightPx).toFloat()
    val band = (shortSide * 0.30f).coerceIn(10f * density, 26f * density)
    val radius = (cornerDp * density).coerceIn(0f, shortSide * 0.5f)
    val shader = RuntimeShader(LENS_AGSL)
    shader.setInputShader("contents", BitmapShader(cropped, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
    shader.setFloatUniform("uSize", hw.toFloat(), hh.toFloat())
    shader.setFloatUniform("uRadius", radius * inv)
    shader.setFloatUniform("uBand", band * inv)
    shader.setFloatUniform("uShift", band * (0.040f + 0.20f * strength) * inv)
    shader.setFloatUniform("uDispersion", 0.0015f + 0.006f * strength)
    shader.setFloatUniform("uStrength", strength)
    val out = Bitmap.createBitmap(hw, hh, Bitmap.Config.ARGB_8888)
    Canvas(out).drawRect(0f, 0f, hw.toFloat(), hh.toFloat(), Paint().apply { this.shader = shader })
    cropped.recycle()
    return out
}

/**
 * Liquid edge sheen on the glass, tuned to the look of iOS glass: light does not wrap the card evenly,
 * it catches on two opposite corners (top-left strong, bottom-right softer) and fades to a faint
 * line elsewhere. Drawn with plain path strokes and gradients, no shader, built once per size.
 *
 *  - Specular rim: a 1.25 dp stroke painted with a sweep gradient (bright TL / BR, dim TR / BL).
 *  - Inner rim: a second, softer stroke inset by 1.5 dp. Two edges read as glass with thickness.
 *  - Top gloss: a soft dome of light from the top centre, clipped to the card.
 *  - Top hairline: a 1 px highlight just below the top edge, fading toward both corners.
 *  - Inner glow: one blurred stroke baked once (half resolution), kept faint so it never looks like a border.
 *  - Bottom caustic: a thin band of light along the bottom edge.
 */
fun Modifier.edgeSheenOverlay(strength: Float, cornerRadiusDp: Float = 24f): Modifier {
    val s = strength.coerceIn(0f, 1f)
    if (s <= 0.01f) return this
    return this.drawWithCache {
        val w = size.width
        val h = size.height
        if (w <= 1f || h <= 1f) return@drawWithCache onDrawWithContent { drawContent() }
        val shortSide = min(w, h)
        val radius = (cornerRadiusDp * density).coerceIn(0f, shortSide * 0.5f)
        val bottomPx = 18f * density
        val rimPath = roundRectPath(w, h, radius, inset = 0.6f * density)
        val innerPath = roundRectPath(w, h, radius, inset = 1.9f * density)
        val framePath = roundRectPath(w, h, radius, inset = 0f)
        val center = Offset(w / 2f, h / 2f)
        // Angles run clockwise from 3 o'clock: 0.125 = bottom-right, 0.625 = top-left.
        val rim = Brush.sweepGradient(
            colorStops = arrayOf(
                0.000f to Color.White.copy(alpha = 0.30f * s),
                0.125f to Color.White.copy(alpha = 0.85f * s),
                0.250f to Color.White.copy(alpha = 0.38f * s),
                0.375f to Color.White.copy(alpha = 0.14f * s),
                0.500f to Color.White.copy(alpha = 0.30f * s),
                0.625f to Color.White.copy(alpha = 1.00f * s),
                0.750f to Color.White.copy(alpha = 0.55f * s),
                0.875f to Color.White.copy(alpha = 0.16f * s),
                1.000f to Color.White.copy(alpha = 0.30f * s)
            ),
            center = center
        )
        val inner = Brush.sweepGradient(
            colorStops = arrayOf(
                0.000f to Color.White.copy(alpha = 0.05f * s),
                0.125f to Color.White.copy(alpha = 0.22f * s),
                0.375f to Color.White.copy(alpha = 0.03f * s),
                0.625f to Color.White.copy(alpha = 0.26f * s),
                0.875f to Color.White.copy(alpha = 0.03f * s),
                1.000f to Color.White.copy(alpha = 0.05f * s)
            ),
            center = center
        )
        val gloss = Brush.radialGradient(
            colorStops = easedStops(Color.White, 0.17f * s),
            center = Offset(w * 0.5f, -h * 0.12f),
            radius = max(w * 0.62f, h * 0.9f)
        )
        val topLine = Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                0.30f to Color.White.copy(alpha = 0.42f * s),
                0.70f to Color.White.copy(alpha = 0.42f * s),
                1f to Color.Transparent
            )
        )
        val caustic = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                1f to Color.White.copy(alpha = 0.20f * s)
            ),
            startY = h - bottomPx,
            endY = h
        )
        // Inner glow: ONE blurred stroke baked once into a half-resolution bitmap, so the falloff is
        // continuous (no stepped rings) and each frame draws a single image.
        val glow = bakeRimGlow(w, h, radius, density, s * 0.55f)
        val glowSize = androidx.compose.ui.unit.IntSize(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1))
        val rimStroke = Stroke(width = 1.25f * density)
        val innerStroke = Stroke(width = 3f * density)
        onDrawWithContent {
            drawContent()
            clipPath(framePath) {
                drawRect(brush = gloss, size = Size(w, h * 0.62f))
                drawRect(brush = caustic, topLeft = Offset(0f, h - bottomPx), size = Size(w, bottomPx))
            }
            if (glow != null) {
                drawImage(
                    image = glow,
                    dstSize = glowSize,
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium
                )
            }
            drawPath(innerPath, brush = inner, style = innerStroke)
            drawPath(rimPath, brush = rim, style = rimStroke)
            drawRect(
                brush = topLine,
                topLeft = Offset(radius * 0.5f, 1.6f * density),
                size = Size((w - radius).coerceAtLeast(1f), density)
            )
        }
    }
}

/** Soft white inner glow along the rounded edge, rendered at half size with one blur pass. */
private fun bakeRimGlow(w: Float, h: Float, radius: Float, density: Float, strength: Float): androidx.compose.ui.graphics.ImageBitmap? {
    val k = 0.5f
    val bw = (w * k).toInt().coerceAtLeast(2)
    val bh = (h * k).toInt().coerceAtLeast(2)
    return runCatching {
        val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val band = 5f * density * k
        val rr = android.graphics.RectF(0f, 0f, bw.toFloat(), bh.toFloat())
        val rad = radius * k
        // Only the inside of the card may glow.
        val clip = android.graphics.Path().apply { addRoundRect(rr, rad, rad, android.graphics.Path.Direction.CW) }
        canvas.clipPath(clip)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = band * 1.1f
            color = android.graphics.Color.argb((255 * 0.70f * strength).toInt().coerceIn(0, 255), 255, 255, 255)
            maskFilter = android.graphics.BlurMaskFilter(band * 1.2f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(rr, rad, rad, paint)
        bmp.asImageBitmap()
    }.getOrNull()
}

private fun roundRectPath(w: Float, h: Float, radius: Float, inset: Float): Path =
    Path().apply {
        addRoundRect(
            RoundRect(
                left = inset,
                top = inset,
                right = w - inset,
                bottom = h - inset,
                cornerRadius = CornerRadius(max(0f, radius - inset))
            )
        )
    }

private const val DOME_FIELD_AGSL = """
// Three unequal, overlapping hills that merge into one liquid body. Each hill has its own
// height, width and vertical offset, so they read as one surface with three swells, not three
// circles in a row. Units are the card's short side, so every card shows the same shape.
// Returns (height, dH/dx, dH/dy) with the gradient in px units.
float3 domeField(float2 p, float2 halfSize) {
    float hs = min(halfSize.x, halfSize.y);
    float2 v0 = (p - float2(-0.50 * halfSize.x, -0.16 * halfSize.y)) / hs;
    float2 v1 = (p - float2( 0.08 * halfSize.x,  0.12 * halfSize.y)) / hs;
    float2 v2 = (p - float2( 0.58 * halfSize.x, -0.30 * halfSize.y)) / hs;
    float k0 = 1.10; float k1 = 1.45; float k2 = 1.25;
    float a0 = 1.00; float a1 = 0.72; float a2 = 0.90;
    float d0 = exp(-k0 * dot(v0, v0));
    float d1 = exp(-k1 * dot(v1, v1));
    float d2 = exp(-k2 * dot(v2, v2));
    float H = a0 * d0 + a1 * d1 + a2 * d2;
    float2 g = (-2.0 / hs) * (a0 * k0 * d0 * v0 + a1 * k1 * d1 * v1 + a2 * k2 * d2 * v2);
    return float3(H, g.x, g.y);
}
"""

private const val LENS_AGSL = DOME_FIELD_AGSL + """
uniform shader contents;
uniform float2 uSize;
uniform float uRadius;
uniform float uBand;
uniform float uShift;
uniform float uDispersion;
uniform float uStrength;

float sdRoundBox(float2 p, float2 halfSize, float radius) {
    float2 q = abs(p) - halfSize + radius;
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
}

half4 main(float2 coord) {
    float2 halfSize = uSize * 0.5;
    float hs = min(halfSize.x, halfSize.y);
    float2 p = coord - halfSize;
    float sd = sdRoundBox(p, halfSize, uRadius);
    float inside = -sd;
    half4 original = contents.eval(coord);

    float coverage = smoothstep(-2.25, 2.25, inside);
    if (inside < -2.0 || original.a <= 0.001) return original * half(coverage);

    const float eps = 1.0;
    float2 grad = float2(
        sdRoundBox(p + float2(eps, 0.0), halfSize, uRadius) - sdRoundBox(p - float2(eps, 0.0), halfSize, uRadius),
        sdRoundBox(p + float2(0.0, eps), halfSize, uRadius) - sdRoundBox(p - float2(0.0, eps), halfSize, uRadius)
    );
    float2 normal = grad * inversesqrt(max(dot(grad, grad), 0.0001));

    float3 f = domeField(p, halfSize);
    float dome = clamp(f.x, 0.0, 1.0);
    float2 domeBend = -normalize(f.yz + float2(0.0001)) * (uShift * 1.5 * dome);

    float edgeT = clamp(1.0 - inside / max(uBand * 5.0, 1.0), 0.0, 1.0);
    float bevel = edgeT * edgeT * (3.0 - 2.0 * edgeT);
    float2 edgeBend = -normal * (uShift * 0.9 * bevel);

    float2 c = p / hs;
    float waveA = sin(c.y * 4.4 + c.x * 2.0);
    float waveB = sin(c.x * 3.1 - c.y * 2.6 + 0.8);
    float2 flow = float2(waveA, waveB) * (uShift * 0.006 * uStrength) * (0.25 + 0.75 * dome);
    float2 bend = domeBend + edgeBend + flow;

    float dispersion = uDispersion * (0.22 + 0.78 * max(dome, bevel));
    float2 maxCoord = uSize - float2(1.0);
    float2 redCoord = clamp(coord + bend * (1.0 - dispersion), float2(0.0), maxCoord);
    float2 greenCoord = clamp(coord + bend, float2(0.0), maxCoord);
    float2 blueCoord = clamp(coord + bend * (1.0 + dispersion), float2(0.0), maxCoord);
    half4 rs = contents.eval(redCoord);
    half4 gs = contents.eval(greenCoord);
    half4 bs = contents.eval(blueCoord);

    float a = gs.a;
    float3 refracted = min(float3(rs.r, gs.g, bs.b), float3(a));
    return half4(half3(refracted * coverage), half(a * coverage));
}
"""
