package com.android.island.island

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.android.island.core.GlassConfig

/**
 * One material profile per kind of glass surface. All profiles render through the same
 * [UnifiedGlassEngine] pipeline (body, lens, edge sheen); a profile only scales how strong
 * each layer is for that surface.
 *
 * Stage 1: every scale is 1f, so visuals are unchanged. Later stages tune the values per surface.
 */
enum class GlassProfile(val sheenScale: Float, val surfaceScale: Float) {
    CAPSULE(sheenScale = 1f, surfaceScale = 1f),
    EXPANDED(sheenScale = 1f, surfaceScale = 1f),
    BANNER(sheenScale = 1f, surfaceScale = 1f),
    STACK(sheenScale = 1f, surfaceScale = 1f),
    CONTROLS(sheenScale = 1f, surfaceScale = 1f)
}

/** Single entry point for all glass surfaces. Components call these instead of the low-level modifiers. */
/** Glass body: transparency, tint, refraction and lens, rendered by [liquidGlass]. */
fun Modifier.glassBody(
        profile: GlassProfile,
        config: GlassConfig,
        accent: Color,
        shape: Shape,
        edgeLens: Float,
        whiteProfile: Boolean
    ): Modifier = liquidGlass(
        config = config,
        accent = accent,
        shape = shape,
        surfaceOpacity = GlassTuning.glassSurfaceOpacity * profile.surfaceScale,
        edgeLens = edgeLens,
        whiteProfile = whiteProfile
    )

/** Edge sheen: bevel and rim lighting, rendered by [edgeSheenOverlay]. */
fun Modifier.glassEdge(profile: GlassProfile, cornerRadiusDp: Float): Modifier =
        edgeSheenOverlay(GlassTuning.sheenStrength * GlassTuning.edgeHighlight * profile.sheenScale * profileSheenFor(profile), cornerRadiusDp)

private fun profileSheenFor(p: GlassProfile): Float = when (p) {
    GlassProfile.EXPANDED -> GlassTuning.profileSheenExpanded
    GlassProfile.BANNER -> GlassTuning.profileSheenBanner
    else -> 1f
}

/**
 * Small glass control (play, prev, next): a translucent tinted body with a soft bevel,
 * no outline. Apply after a clip to the control's shape. [ink] is the panel's ink colour.
 */
fun Modifier.glassControl(ink: Color, strength: Float = 1f): Modifier = this.drawBehind {
    val s = strength.coerceIn(0f, 1f)
    if (com.android.island.core.DeviceProfile.staticGlass) {
        drawRect(ink.copy(alpha = 0.12f * s))
        return@drawBehind
    }
    val body = Brush.linearGradient(
        colorStops = arrayOf(0f to ink.copy(alpha = 0.20f * s), 1f to ink.copy(alpha = 0.05f * s)),
        start = Offset.Zero,
        end = Offset(size.width, size.height)
    )
    drawRect(brush = body)
    val topLight = Brush.verticalGradient(
        colorStops = arrayOf(0f to ink.copy(alpha = 0.22f * s), 0.45f to Color.Transparent),
        startY = 0f,
        endY = size.height
    )
    drawRect(brush = topLight)
    val bottomShade = Brush.verticalGradient(
        colorStops = arrayOf(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.22f * s)),
        startY = 0f,
        endY = size.height
    )
    drawRect(brush = bottomShade)
}
