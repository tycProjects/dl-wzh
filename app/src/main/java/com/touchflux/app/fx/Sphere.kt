package com.touchflux.app.fx

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader

/**
 * A fake-3D glossy ball: ambient shadow, soft glow, diffuse body lit from the upper-left,
 * Fresnel rim light and two specular highlights. All gradients are built once at unit radius
 * and scaled by the canvas, so drawing allocates nothing per frame.
 */
class Sphere(base: Int) {
    private val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            -0.30f, -0.36f, 1.45f,
            intArrayOf(
                Colors.lighten(base, 0.92f),
                Colors.lighten(base, 0.40f),
                base,
                Colors.darken(base, 0.70f)
            ),
            floatArrayOf(0f, 0.20f, 0.60f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    private val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            0.08f, 0.12f, 1.05f,
            intArrayOf(0x00FFFFFF, 0x00FFFFFF, Colors.alpha(Colors.lighten(base, 0.55f), 0.60f)),
            floatArrayOf(0f, 0.74f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            0f, 0f, 2.4f,
            intArrayOf(Colors.alpha(base, 0.55f), Colors.alpha(base, 0.18f), Colors.alpha(base, 0f)),
            floatArrayOf(0.35f, 0.62f, 1f),
            Shader.TileMode.CLAMP
        )
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }
    private val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            0f, 0f, 1.25f,
            intArrayOf(0x66000000, 0x33000000, 0x00000000),
            floatArrayOf(0.55f, 0.82f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    private val spec = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(0xF2FFFFFF.toInt(), 0x66FFFFFF, 0x00FFFFFF),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    /** @param glowOn adds an additive halo; @param alpha 0..1 master opacity. */
    fun draw(c: Canvas, cx: Float, cy: Float, r: Float, alpha: Float, glowOn: Boolean = true) {
        if (r <= 0.5f || alpha <= 0.01f) return
        val a = (Ease.clamp01(alpha) * 255f).toInt()
        c.save()
        c.translate(cx, cy)
        c.scale(r, r)

        if (glowOn) {
            glow.alpha = a
            c.drawCircle(0f, 0f, 2.4f, glow)
        }
        shadow.alpha = (a * 0.8f).toInt()
        c.drawCircle(0.16f, 0.30f, 1.2f, shadow)

        body.alpha = a
        c.drawCircle(0f, 0f, 1f, body)
        rim.alpha = a
        c.drawCircle(0f, 0f, 1f, rim)

        // broad soft specular
        spec.alpha = a
        c.save()
        c.translate(-0.36f, -0.44f)
        c.rotate(-32f)
        c.scale(0.40f, 0.24f)
        c.drawCircle(0f, 0f, 1f, spec)
        c.restore()
        // tiny hard glint
        dot.alpha = a
        c.drawCircle(-0.47f, -0.55f, 0.075f, dot)

        c.restore()
    }
}
