package com.android.island.island

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/**
 * Live glass tuning from Settings. IslandRoot syncs it after every settings change.
 * Backed by Compose state, so composables that read a value recompose when it changes
 * (a plain field would leave skipped composables showing stale values).
 */
object GlassTuning {
    var sheenStrength: Float by mutableFloatStateOf(1f)
    var edgeHighlight: Float by mutableFloatStateOf(1f)
    var profileSheenExpanded: Float by mutableFloatStateOf(1f)
    var profileSheenBanner: Float by mutableFloatStateOf(1f)
    var lowEnd: Int by mutableIntStateOf(0)
    var glassSurfaceOpacity: Float by mutableFloatStateOf(1f)
    var stackGapDp: Float by mutableFloatStateOf(8f)
    var stackCapDp: Float by mutableFloatStateOf(300f)
    var stackInsetDp: Float by mutableFloatStateOf(8f)
}

/**
 * Artwork-local liquid lens for Android 13+.
 *
 * The normal-mode lens bends artwork toward the rim with a convex magnifier profile: the rounded
 * edge samples pixels from farther toward the centre, creating the mirrored, magnified edge band.
 * It only refracts the pixels it is given (album art / avatar), never inventing colour. Low-End
 * bypasses this GPU path entirely; normal mode retains the cached convex lens effect.
 */

/**
 * Bakes the full-surface lens for [art] into a HALF-resolution bitmap (API 33+).
 *
 * The lens is static (no time input), so its output for a given artwork, size and strength never
 * changes. Computing it once and drawing the bitmap is cheaper than running the shader every frame.
 * The shader runs in half-resolution coordinates with every pixel-based parameter halved, so the
 * geometry matches the full-resolution lens. The bitmap is scaled back up when drawn.
 */


/**
 * Liquid edge sheen on the glass, tuned to the look of iOS glass: light does not wrap the card evenly,
 * it catches on two opposite corners (top-left strong, bottom-right softer) and fades to a faint
 * line elsewhere. Drawn with plain path strokes and gradients, no shader, built once per size.
 *
 *  - Specular rim: a 1.25 dp stroke painted with a sweep gradient (bright TL / BR, dim TR / BL).
 *  - Inner rim: a second, softer stroke inset by 1.5 dp. Two edges read as glass with thickness.
 *  - Top gloss: a soft band of light from the top centre, clipped to the card.
 *  - Top hairline: a 1 px highlight just below the top edge, fading toward both corners.
 *  - Inner glow: one blurred stroke baked once (half resolution), kept faint so it never looks like a border.
 *  - Bottom caustic: a thin band of light along the bottom edge.
 */

fun Modifier.edgeSheenOverlay(strength: Float, cornerRadiusDp: Float = 24f): Modifier {
    // The Low-End renderer must not draw decorative rim strokes or allocate gradient brushes.
    if (com.android.island.core.DeviceProfile.staticGlass) return this
    val s = strength.coerceIn(0f, 1f)
    if (s <= 0.01f) return this
    // Anti-aliased rounded clip (RenderNode outline). canvas clipPath is not anti-aliased on many GPUs,
    // which is what made the glass edge look stair-stepped / boxy.
    return this.clip(RoundedCornerShape(cornerRadiusDp.dp)).drawWithCache {
        val w = size.width
        val h = size.height
        if (w <= 1f || h <= 1f) return@drawWithCache onDrawWithContent { drawContent() }
        val shortSide = min(w, h)
        val radius = (cornerRadiusDp * density).coerceIn(0f, shortSide * 0.5f)
        val bottomPx = 18f * density
        val innerPath = roundRectPath(w, h, radius, inset = 1.9f * density)
        val framePath = roundRectPath(w, h, radius, inset = 0f)
        val center = Offset(w / 2f, h / 2f)
        // Angles run clockwise from 3 o'clock: 0.125 = bottom-right, 0.625 = top-left.
        // Edge: no outline and no white line. Only soft light from the top and shade toward the
        // bottom edge, so the card reads as a raised (embossed) surface.
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
                1f to Color.White.copy(alpha = 0.34f * s)
            ),
            startY = h - bottomPx,
            endY = h
        )
        // Inner glow: ONE blurred stroke baked once into a half-resolution bitmap, so the falloff is
        // continuous (no stepped rings) and each frame draws a single image.
        val innerStroke = Stroke(width = 8f * density)
        // Uneven rim (Apple Liquid Glass): brightest upper-left, a secondary catch lower-right,
        // faint along the sides. Thin and low-alpha, so it reads as a bevel catching light,
        // not as a uniform white outline.
        val rimPath = roundRectPath(w, h, radius, inset = 0.6f * density)
        val rimStroke = Stroke(width = 1.2f * density)
        val rimSweep = Brush.sweepGradient(
            colorStops = arrayOf(
                0.000f to Color.White.copy(alpha = 0.08f * s),
                0.125f to Color.White.copy(alpha = 0.30f * s),   // bottom-right catch
                0.375f to Color.White.copy(alpha = 0.10f * s),   // bottom-left
                0.500f to Color.White.copy(alpha = 0.08f * s),   // left side
                0.625f to Color.White.copy(alpha = 0.42f * s),   // top-left: main light
                0.875f to Color.White.copy(alpha = 0.14f * s),   // top-right
                1.000f to Color.White.copy(alpha = 0.08f * s)
            ),
            center = center
        )
        onDrawWithContent {
            drawContent()
            drawRect(brush = gloss, size = Size(w, h * 0.62f))
            drawPath(rimPath, brush = rimSweep, style = rimStroke)
        }
    }
}

/** Soft white inner glow along the rounded edge, rendered at half size with one blur pass. */
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

private const val LENS_AGSL = """
uniform shader contents;
uniform float2 uSize;
uniform float uRadius;
uniform float uBand;
uniform float uShift;
uniform float uStrength;
uniform float uRimSoft;

float sdRoundBox(float2 p, float2 halfSize, float radius) {
    float2 q = abs(p) - halfSize + radius;
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
}

float2 mirrorUV(float2 c, float2 mx) {
                return mx - abs(mod(c, 2.0 * mx) - mx);
            }

            half4 main(float2 coord) {
    float2 halfSize = uSize * 0.5;
    float2 p = coord - halfSize;
    float sd = sdRoundBox(p, halfSize, uRadius);
    float inside = -sd;
    half4 original = contents.eval(coord);

    float coverage = smoothstep(-2.25, 2.25, inside);
    if (inside < -2.0 || original.a <= 0.001) return original * half(coverage);

    // Outward surface normal of the rounded rectangle.
    const float eps = 1.0;
    float2 grad = float2(
        sdRoundBox(p + float2(eps, 0.0), halfSize, uRadius) - sdRoundBox(p - float2(eps, 0.0), halfSize, uRadius),
        sdRoundBox(p + float2(0.0, eps), halfSize, uRadius) - sdRoundBox(p - float2(0.0, eps), halfSize, uRadius)
    );
    float2 normal = grad * inversesqrt(max(dot(grad, grad), 0.0001));

    // Thick convex magnifying glass: t = 0 at the rim, 1 in the flat centre.
    float t = clamp(inside / max(uBand, 1.0), 0.0, 1.0);
    float u = 1.0 - t;
    // Circular bevel profile: height rises steeply at the rim and flattens toward the centre.
    float h = sqrt(max(1.0 - u * u, 0.0));
    float slope = 1.0 - h;

    // Convex mirror/magnifier: the rounded edge pulls image content from deeper inside,
    // so the rim visibly reflects the centre instead of merely drawing a bright outline.
    // Magnifier profile: the rim samples content from deeper inside, so the edge shows the
    // centre folded back onto itself. (1-t)^2 is 1 at the rim and falls smoothly to 0 at the
    // bevel boundary, so there is no hard step and no box-shaped band.
    float fold = 1.0 - t;
    float2 bend = -normal * (uShift * fold * fold);
    float2 maxCoord = uSize - float2(1.0);
    float2 greenCoord = mirrorUV(coord + bend, maxCoord);
    // Rim softening (reference lens technique): average three taps along the normal, so the bent
    // band is a gradient instead of a hard step. This is what removes the boxy look at the edge.
    float2 smR = normal * (uRimSoft * slope);
    half4 gs = (contents.eval(mirrorUV(greenCoord - smR, maxCoord)) + contents.eval(greenCoord) + contents.eval(mirrorUV(greenCoord + smR, maxCoord))) / 3.0;
    float a = gs.a;
    float3 refracted = min(float3(gs.rgb), float3(a));

    // 3D bevel lighting: the surface normal tilts with the slope, light comes from the top-left.
    float3 n3 = normalize(float3(normal * slope, max(h, 0.05)));
    float3 l3 = normalize(float3(-0.4, -0.56, 0.72));
    float lit = dot(n3, l3);
    float light = lit - l3.z;                     // > 0 faces the light, < 0 falls in shade
    float spec = pow(max(dot(n3, normalize(l3 + float3(0.0, 0.0, 1.0))), 0.0), 18.0);
    float highlight = (max(light, 0.0) * 1.1 + spec * 0.25) * uStrength;
    float shadow = max(-light, 0.0) * 0.28 * uStrength;

    float3 col = refracted * (1.0 - shadow) + float3(highlight * a);
    col = min(col, float3(a));
    return half4(half3(col * coverage), half(a * coverage));
}
"""


/**
 * A single-entry cache for the expensive AGSL RuntimeShader + RenderEffect pair.
 * The effect is static for a given artwork and render geometry, so it is built once
 * and reused through every expand/collapse/recomposition while that artwork remains
 * current. Keeping only one entry bounds native/GPU resource retention.
 */
private data class LensEffectKey(
    val artKey: Long,
    val width: Int,
    val height: Int,
    val cornerBits: Int,
    val densityBits: Int,
    val strengthBits: Int,
    val blurBits: Int
)

private object LensEffectCache {
    private var cachedKey: LensEffectKey? = null
    private var cachedEffect: androidx.compose.ui.graphics.RenderEffect? = null

    @Synchronized
    fun obtain(key: LensEffectKey, create: () -> androidx.compose.ui.graphics.RenderEffect?): androidx.compose.ui.graphics.RenderEffect? {
        if (cachedKey == key) return cachedEffect
        val next = create()
        cachedKey = if (next != null) key else null
        cachedEffect = next
        return next
    }

    @Synchronized
    fun clear() {
        cachedKey = null
        cachedEffect = null
    }
}

fun clearCachedLensEffect() = LensEffectCache.clear()

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun cachedLensEffect(
    artKey: Long,
    widthPx: Int,
    heightPx: Int,
    cornerDp: Float,
    density: Float,
    strength: Float,
    blur: Float = 0f
): androidx.compose.ui.graphics.RenderEffect? {
    if (com.android.island.core.DeviceProfile.staticGlass) return null
    val key = LensEffectKey(
        artKey, widthPx, heightPx,
        cornerDp.toBits(), density.toBits(), strength.toBits(), blur.toBits()
    )
    return LensEffectCache.obtain(key) {
        buildLensEffect(widthPx, heightPx, cornerDp, density, strength, blur)
    }
}

/**
 * Lens as a GPU render effect on the artwork layer. A RuntimeShader cannot be drawn into a software
 * bitmap canvas ("Software rendering doesn't support RuntimeShader"), so the bake path never worked.
 * Applying the shader as a RenderEffect runs it on the hardware renderer instead.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun buildLensEffect(
    widthPx: Int,
    heightPx: Int,
    cornerDp: Float,
    density: Float,
    strength: Float,
    blur: Float = 0f
): androidx.compose.ui.graphics.RenderEffect? {
    if (com.android.island.core.DeviceProfile.staticGlass) return null
    if (strength <= 0.01f || widthPx < 2 || heightPx < 2) return null
    return runCatching {
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()
        val shortSide = min(w, h)
        val band = (shortSide * 0.42f).coerceIn(16f * density, 44f * density)
        val radius = (cornerDp * density).coerceIn(0f, shortSide * 0.5f)
        val shader = RuntimeShader(LENS_AGSL)
        shader.setFloatUniform("uSize", w, h)
        shader.setFloatUniform("uRadius", radius)
        shader.setFloatUniform("uBand", band)
        // Mirror reach: the rim pulls content from across the whole bevel, so the edge reflects the centre.
    shader.setFloatUniform("uShift", band * (0.9f + 0.6f * strength))
        shader.setFloatUniform("uStrength", strength)
        shader.setFloatUniform("uRimSoft", band * 0.14f)
        val lensFx = android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "contents")
        // Blur runs on the GPU as the inner step of the chain (blur first, then the lens), so no blurred copy is made on the CPU.
        val blurR = blur.coerceIn(0f, 1f) * 20f * density
        val fx = if (blurR > 0.01f) {
            val blurFx = android.graphics.RenderEffect.createBlurEffect(blurR, blurR, android.graphics.Shader.TileMode.CLAMP)
            android.graphics.RenderEffect.createChainEffect(lensFx, blurFx)
        } else lensFx
        fx.asComposeRenderEffect()
    }.onFailure {
        com.android.island.util.IslandDiagnostics.record("LENS_FAILED", "render effect: ${it.javaClass.simpleName}: ${it.message}")
    }.getOrNull()
}
