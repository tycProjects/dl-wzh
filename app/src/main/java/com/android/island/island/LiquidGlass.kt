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
import kotlin.math.hypot

/**
 * App-drawn frosted glass (NOT Android system blur).
 *
 * - whiteOpacity controls only a very restrained optical reflection tint
 * - frostStrength controls optional Gaussian blur on the artwork layer
 * - artwork opacity is independent and is applied by FluidGlassBackdrop
 */
fun Modifier.liquidGlass(
    config: GlassConfig,
    accent: Color = Color.White,
    shape: Shape = RoundedCornerShape(28.dp),
    /** Independent glass substrate strength; never coupled to artwork opacity. */
    surfaceOpacity: Float = 1f,
    /** Retained for API compatibility; optical convexity is rendered by the AGSL lens, not a stroke. */
    edgeLens: Float = 0.6f,
    whiteProfile: Boolean = false
): Modifier {
    val so = surfaceOpacity.coerceIn(0f, 1f)
    val white = config.whiteOpacity.coerceIn(0f, 0.85f)
    val transparency = config.transparency.coerceIn(0f, 1f)
    val ambient = config.ambientReflection.coerceIn(0f, 1f)
    // This is only the independent substrate strength. Artwork alpha is applied to the
    // artwork bitmap layer in FluidGlassBackdrop, never to this glass material.
    val material = so
    // Keep the substrate optically clear. Artwork opacity is controlled by the artwork
    // layer, never by adding a white/milky fill on top of it.
    val baseAlpha = 0.018f + (1f - transparency) * 0.045f + material * 0.12f
    val whiteBoost = if (whiteProfile) 0.008f else 0f

    // The old linear/sweep strokes are intentionally removed. They produced a permanent
    // rectangular-looking white outline. The raised-liquid appearance now comes from
    // the SDF/normal field in LiquidLens (directional specular response, not a border).
    return this
        .clip(shape)
        .drawWithCache {
            val body = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to Color.White.copy(alpha = baseAlpha + white * (0.018f + material * 0.018f) + whiteBoost),
                    0.34f to accent.copy(alpha = ambient * (0.035f + material * 0.045f)),
                    0.68f to Color.White.copy(alpha = baseAlpha + white * (0.012f + material * 0.018f)),
                    1.00f to Color.Black.copy(alpha = 0.018f + material * 0.035f)
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
            val softTop = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.012f + ambient * 0.018f + material * 0.035f),
                1f to Color.Transparent,
                startY = 0f,
                endY = size.height * 0.48f
            )
            val softBottom = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.025f + material * 0.045f),
                startY = size.height * 0.58f,
                endY = size.height
            )
            // Soft inner shadow toward the ends of the card: makes the glass read as a thick pillow, not a flat sheet.
            val vignette = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    0.70f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.10f + material * 0.10f)
                ),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = hypot(size.width, size.height) / 2f
            )
            onDrawWithContent {
                drawRect(body, alpha = so)
                drawRect(vignette, alpha = so)
                drawRect(softTop, alpha = so)
                drawRect(softBottom, alpha = so)
                drawContent()
            }
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
