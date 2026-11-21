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
import androidx.compose.ui.geometry.CornerRadius
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
    /** Retained for API compatibility; optical convexity is rendered by the AGSL lens and edge sheen. */
    edgeLens: Float = 0.6f,
    whiteProfile: Boolean = false
): Modifier {
    val so = surfaceOpacity.coerceIn(0f, 1f)
    val white = config.whiteOpacity.coerceIn(0f, 0.85f)
    val transparency = config.transparency.coerceIn(0f, 1f)
    val ambient = config.ambientReflection.coerceIn(0f, 1f)
    // Milky matte frost, like the reference kit: a grey substrate that reads as thick frosted glass
    // rather than clear glass. Strength comes from the settings (whiteOpacity, transparency).
    val frost = (0.10f + (1f - transparency) * 0.10f + white * 0.60f +
        if (whiteProfile) 0.02f else 0f).coerceIn(0f, 0.5f)

    return this
        .clip(shape)
        .drawWithCache {
            if (com.android.island.core.DeviceProfile.staticGlass) {
                return@drawWithCache onDrawWithContent {
                    // Low-End uses one flat fill only. No reflection, tint stack, or gradient.
                    drawRect(Color(0xFF17191D).copy(alpha = 0.48f * so))
                    drawContent()
                }
            }
            // Body: brightest at the top-left, easing darker toward the bottom-right.
            val body = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to Color.White.copy(alpha = frost * 1.15f),
                    0.50f to Color.White.copy(alpha = frost * 0.85f),
                    1.00f to Color.White.copy(alpha = frost * 0.70f)
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
            // Very faint accent tint so the glass keeps the artwork's colour.
            val tint = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to Color.Transparent,
                    0.34f to accent.copy(alpha = ambient * 0.035f),
                    1.00f to Color.Transparent
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
            val softTop = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.06f + ambient * 0.05f),
                1f to Color.Transparent,
                startY = 0f,
                endY = size.height * 0.48f
            )
            // Light pooled along the bottom edge, as in the reference: the frost catches light from below.
            val bottomGlow = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to Color.White.copy(alpha = 0.10f + white * 0.30f),
                startY = size.height * 0.62f,
                endY = size.height
            )
            onDrawWithContent {
                drawRect(body, alpha = so)
                drawRect(tint, alpha = so)
                drawRect(softTop, alpha = so)
                drawRect(bottomGlow, alpha = so)
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

/** iPhone-style display notch. */
fun iPhoneNotchShape(): Shape = IPhoneNotchCapsuleShape

/**
 * Width/height of the concave top "ear" of the classic notch, in whatever unit [w]/[h] use
 * (px for the shape, dp for layout). One formula, so content padding can never drift away from the
 * silhouette: content inside the notch must start past this ear or its edge is clipped.
 */
fun iPhoneNotchEar(w: Float, h: Float): Float = (h * 0.30f).coerceAtMost(w * 0.08f)

/**
 * STAGE 75: the classic iPhone X/XS/11 notch. It is glued to the top edge of the screen: the top
 * edge is flush and each upper corner flares OUTWARD in a small concave "ear" that melts into the
 * screen edge (the black is widest right at the top). The sides then drop perfectly straight
 * and the two bottom corners are rounded. No taper, no dome, no floating pill.
 */
private object IPhoneNotchCapsuleShape : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val w = size.width
        val h = size.height
        // Concave ear: how far the flare reaches inward from the shape's outer edge, and down.
        val e = iPhoneNotchEar(w, h)
        // Rounded bottom corners, about half the notch height like the original cutout.
        val botR = (h * 0.55f).coerceAtMost((w - 2f * e) / 2f).coerceAtLeast(0f)
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            // Right ear: hugs the top edge, then turns down into the straight right side.
            quadraticBezierTo(w - e, 0f, w - e, e)
            lineTo(w - e, h - botR)
            quadraticBezierTo(w - e, h, w - e - botR, h)
            lineTo(e + botR, h)
            quadraticBezierTo(e, h, e, h - botR)
            lineTo(e, e)
            // Left ear, mirrored.
            quadraticBezierTo(e, 0f, 0f, 0f)
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

/**
 * Shape used while the island morphs between capsule and expanded card. The top radius and the
 * bottom radius are driven separately: the capsule's top stays flat against the display edge and
 * its bottom stays blunt, then both melt into the expanded card's corners. Values are in pixels.
 */
fun morphCapsuleShape(topPx: Float, bottomPx: Float): Shape = MorphCapsuleShape(topPx, bottomPx)

/**
 * One reusable path for the morph shape. The island has a single morph surface, and everything runs
 * on the UI thread, so a single shared path is safe. It is rebuilt only when size or radii change:
 * during the morph that is every frame, but the path object itself is never reallocated, and once the
 * island settles nothing is rebuilt at all.
 */
private object MorphPathCache {
    val path = androidx.compose.ui.graphics.Path()
    var w = -1f
    var h = -1f
    var t = -1f
    var b = -1f
}

private class MorphCapsuleShape(private val topPx: Float, private val bottomPx: Float) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val w = size.width
        val h = size.height
        val half = (minOf(w, h) / 2f).coerceAtLeast(0f)
        val t = topPx.coerceIn(0f, half)
        val b = bottomPx.coerceIn(0f, half)
        val c = MorphPathCache
        if (c.w != w || c.h != h || c.t != t || c.b != b) {
            c.path.reset()
            c.path.addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = 0f, top = 0f, right = w, bottom = h,
                    topLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(t, t),
                    topRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(t, t),
                    bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(b, b),
                    bottomLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(b, b)
                )
            )
            c.w = w; c.h = h; c.t = t; c.b = b
        }
        return androidx.compose.ui.graphics.Outline.Generic(c.path)
    }
}
