package com.android.island.island

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.android.island.core.GlassConfig

/**
 * App-drawn frosted glass (NOT Android system blur).
 *
 * - whiteOpacity = 0 → no white veil; frost is neutral/adaptive over album art
 * - frostStrength → how strong the matte "buram" layers are
 * - transparency → overall see-through amount
 */
fun Modifier.liquidGlass(
    config: GlassConfig,
    accent: Color = Color.White,
    shape: Shape = RoundedCornerShape(28.dp),
    /** Multiplies the glass fill alpha, so the panel background follows the "Opasitas latar belakang" slider. */
    surfaceOpacity: Float = 1f,
    /** Strength of the refraction rim along the edge (0 = off). Driven by the "Edge lens" slider. */
    edgeLens: Float = 0.6f
): Modifier {
    val so = surfaceOpacity.coerceIn(0f, 1f)
    val white = config.whiteOpacity.coerceIn(0f, 0.85f)
    val frost = config.frostStrength.coerceIn(0f, 1f)
    val transparency = config.transparency.coerceIn(0f, 1f)
    val refraction = config.refraction.coerceIn(0f, 1f)
    val edge = config.edgeHighlight.coerceIn(0f, 1f)
    val chromatic = config.chromaticAberration.coerceIn(0f, 1f)
    val ambient = config.ambientReflection.coerceIn(0f, 1f)
    val lens = edgeLens.coerceIn(0f, 1f)
    val baseAlpha = 0.035f + (1f - transparency) * 0.13f + frost * 0.12f

    // Everything below is built ONCE per size inside drawWithCache and is completely static:
    // body tint, top specular band, bottom inner shade, soft inner lens glow and a gradient rim.
    // No animated value is read here, so the glass costs zero per-frame work (only a handful of
    // gradient fills when the layer is re-rasterised). The "liquid" feel comes from the layered
    // light - bright top-left / bottom-right rim, dim sides - exactly like refracted real glass.
    return this
        .clip(shape)
        .drawWithCache {
            val top = Color.White.copy(alpha = baseAlpha + white * 0.16f + frost * 0.025f)
            val upperAccent = accent.copy(alpha = ambient * 0.075f + chromatic * 0.018f)
            val refracted = Color.White.copy(alpha = refraction * 0.12f + edge * 0.018f)
            val lowerTint = Color(0xFFDDE3EC).copy(alpha = baseAlpha + white * 0.17f + frost * 0.035f)
            val bottom = Color.Black.copy(alpha = frost * 0.018f)
            val glassBrush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to top,
                    0.28f to upperAccent,
                    0.52f to refracted,
                    0.78f to lowerTint,
                    1.00f to bottom
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
            val specH = size.height * 0.5f
            val specBrush = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.10f + edge * 0.12f),
                1f to Color.Transparent,
                startY = 0f,
                endY = specH
            )
            val shadeTop = size.height * 0.62f
            val shadeBrush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.07f + frost * 0.06f),
                startY = shadeTop,
                endY = size.height
            )
            val rimPath = outlineToPath(shape.createOutline(size, layoutDirection, this))
            // The stroke is centred on the edge and the outer half is clipped away, so the
            // visible width is half of the value below.
            val rimW = (1.2f + edge * 1.4f) * density
            val rimBrush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to Color.White.copy(alpha = 0.34f + edge * 0.50f),
                    0.32f to Color.White.copy(alpha = 0.07f + edge * 0.06f),
                    0.66f to Color.White.copy(alpha = 0.06f + edge * 0.05f),
                    1.00f to Color.White.copy(alpha = 0.24f + edge * 0.40f)
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
            val lensW = (4f + 8f * lens) * density
            val lensBrush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to Color.White.copy(alpha = 0.18f * lens),
                    0.50f to Color.White.copy(alpha = 0.03f * lens),
                    1.00f to Color.White.copy(alpha = 0.13f * lens)
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
            // Corner hot-spots, like real bevelled glass catching light: a sweep gradient around the
            // centre is bright toward top-left and bottom-right and dim toward the other two
            // corners. Built once; stroked on top of the linear rim for a richer edge.
            val hot = 0.20f + edge * 0.45f
            val cold = 0.04f + edge * 0.05f
            val sweepRimBrush = Brush.sweepGradient(
                colorStops = arrayOf(
                    0.000f to Color.White.copy(alpha = cold * 1.6f),
                    0.125f to Color.White.copy(alpha = hot),          // bottom-right
                    0.250f to Color.White.copy(alpha = cold),
                    0.500f to Color.White.copy(alpha = cold * 1.4f),
                    0.625f to Color.White.copy(alpha = hot * 1.15f),  // top-left
                    0.750f to Color.White.copy(alpha = cold * 2.2f),
                    1.000f to Color.White.copy(alpha = cold * 1.6f)
                ),
                center = Offset(size.width / 2f, size.height / 2f)
            )
            val sweepRimW = (1.0f + edge * 0.9f) * density
            // Thin specular glint just inside the top edge (the bevel highlight in the reference).
            val glintY = 1.6f * density
            val glintInset = (size.height * 0.30f).coerceAtMost(size.width * 0.22f)
            val glintBrush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.00f to Color.Transparent,
                    0.18f to Color.White.copy(alpha = 0.16f + edge * 0.26f),
                    0.50f to Color.White.copy(alpha = 0.30f + edge * 0.34f),
                    0.82f to Color.White.copy(alpha = 0.10f + edge * 0.18f),
                    1.00f to Color.Transparent
                ),
                startX = glintInset,
                endX = size.width - glintInset
            )
            val glintW = 1.2f * density
            onDrawWithContent {
                drawRect(glassBrush, alpha = so)
                drawRect(specBrush, size = Size(size.width, specH), alpha = so)
                drawRect(shadeBrush, topLeft = Offset(0f, shadeTop), size = Size(size.width, size.height - shadeTop), alpha = so)
                drawContent()
                if (lens > 0.01f) drawPath(rimPath, brush = lensBrush, style = Stroke(width = lensW))
                drawPath(rimPath, brush = rimBrush, style = Stroke(width = rimW))
                drawPath(rimPath, brush = sweepRimBrush, style = Stroke(width = sweepRimW))
                if (glintInset * 2f < size.width) {
                    drawLine(
                        brush = glintBrush,
                        start = Offset(glintInset, glintY),
                        end = Offset(size.width - glintInset, glintY),
                        strokeWidth = glintW,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
}

@Composable
fun LiquidGlassSurface(
    config: GlassConfig,
    accent: Color = Color.White,
    shape: Shape = RoundedCornerShape(28.dp),
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.liquidGlass(config = config, accent = accent, shape = shape)
    ) { content() }
}


/** Continuously morphs the camera-notch silhouette into the expanded rounded capsule.
 * Keeping the top-edge geometry continuous avoids the abrupt shape swap on collapse.
 */
fun morphNotchShape(progress: Float, iphone: Boolean, expandedCornerRadius: Float = 24f): Shape = MorphNotchShape(progress.coerceIn(0f, 1f), iphone, expandedCornerRadius.coerceIn(8f, 48f))

private class MorphNotchShape(private val progress: Float, private val iphone: Boolean, private val expandedCornerRadius: Float) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val w = size.width
        val h = size.height
        val compactTop = h * (if (iphone) 0.02f else 0.04f)
        val topR = compactTop + (expandedCornerRadius * density.density - compactTop) * progress
        val compactBottom = h * (if (iphone) 0.48f else 0.40f)
        val bottomR = (compactBottom + (expandedCornerRadius * density.density - compactBottom) * progress).coerceIn(0f, h / 2f)
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(topR, 0f)
            lineTo(w - topR, 0f)
            quadraticTo(w, 0f, w, topR)
            lineTo(w, h - bottomR)
            quadraticTo(w, h, w - bottomR, h)
            lineTo(bottomR, h)
            quadraticTo(0f, h, 0f, h - bottomR)
            lineTo(0f, topR)
            quadraticTo(0f, 0f, topR, 0f)
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

/** Waterdrop camera capsule: flush/square top edge and a subtly narrower, rounded lower edge. */
fun waterDropShape(topFlat: Boolean = true): Shape {
    return if (topFlat) WaterDropCapsuleShape else RoundedCornerShape(50)
}

private object WaterDropCapsuleShape : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            // Upper edge remains full-width and perfectly square against the display cutout.
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h * 0.34f)
            // The lower half tapers in slightly; its rounded base is narrower than the top.
            cubicTo(w, h * 0.76f, w * 0.91f, h, w * 0.5f, h)
            cubicTo(w * 0.09f, h, 0f, h * 0.76f, 0f, h * 0.34f)
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

/** iPhone-style display notch: flush top edge with broad, softly rounded lower corners. */
fun iPhoneNotchShape(): Shape = IPhoneNotchCapsuleShape

private object IPhoneNotchCapsuleShape : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val w = size.width
        val h = size.height
        val r = (h * 0.48f).coerceAtMost(w * 0.18f)
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h - r)
            quadraticTo(w, h, w - r, h)
            lineTo(r, h)
            quadraticTo(0f, h, 0f, h - r)
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}


/** Converts any Compose [Outline] into a [Path] so the rim can be stroked with drawPath. */
private fun outlineToPath(outline: Outline): Path = when (outline) {
    is Outline.Generic -> outline.path
    is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
    is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
}
