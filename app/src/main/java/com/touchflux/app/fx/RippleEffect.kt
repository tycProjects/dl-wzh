package com.touchflux.app.fx

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import kotlin.math.cos
import kotlin.math.sin

/**
 * Photoreal-ish water ripple.
 *
 * The rings live on a tilted water plane (vertical squash = perspective). Each ring is built from
 * a refractive tinted lens, a soft body, a dark trough on the far side and a bright crest lit from
 * the upper-left, plus a few glossy droplets thrown up by the impact.
 */
class RippleEffect(spec: Spec) : Effect(spec) {
    private val tint = Colors.lighten(s.color, 0.28f)
    private val ringCount = if (s.small) 1 else (1.5f + s.intensity * 1.5f).toInt().coerceIn(2, 4)
    private val maxR = s.unit * (if (s.small) 36f else 76f)
    private val stagger = 0.13f

    private val crest = peak(0xFFFFFFFF.toInt())
    private val trough = peak(0xFF02060F.toInt())

    private val lens = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(Colors.alpha(tint, 0f), Colors.alpha(tint, 0.02f), Colors.alpha(tint, 0.38f), Colors.alpha(tint, 0f)),
            floatArrayOf(0f, 0.55f, 0.93f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    private val flash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(0xFFFFFFFF.toInt(), Colors.alpha(tint, 0.55f), Colors.alpha(tint, 0f)),
            floatArrayOf(0f, 0.35f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    private val body = stroke()
    private val crestPaint = stroke().apply { shader = crest }
    private val troughPaint = stroke().apply { shader = trough }
    private val caustic = stroke()

    private class Drop(var x: Float, var y: Float, var vx: Float, var vy: Float, val r: Float)

    private val drops = ArrayList<Drop>()
    private val dropSphere = Sphere(tint)
    private val gravity = s.unit * 560f

    init {
        val n = if (s.small) 0 else (5 + 7 * s.intensity).toInt()
        repeat(n) {
            val a = (-90f + (s.rng.nextFloat() * 2f - 1f) * 58f) * (Math.PI.toFloat() / 180f)
            val speed = s.unit * (120f + s.rng.nextFloat() * 170f)
            drops.add(
                Drop(
                    s.x, s.y,
                    cos(a) * speed, sin(a) * speed,
                    s.unit * (1.6f + s.rng.nextFloat() * 2.8f)
                )
            )
        }
    }

    override fun update(dt: Float) {
        super.update(dt)
        for (d in drops) {
            d.x += d.vx * dt
            d.y += d.vy * dt
            d.vy += gravity * dt
        }
    }

    override fun draw(c: Canvas) {
        val tt = t

        // ---- water plane (tilted) ----
        c.save()
        c.translate(s.x, s.y)
        c.scale(1f, TILT)

        if (tt < 0.30f) {
            val k = 1f - tt / 0.30f
            flash.alpha = (k * k * 255f).toInt()
            val fr = s.unit * (14f + 30f * tt / 0.30f)
            c.save()
            c.scale(fr, fr)
            c.drawCircle(0f, 0f, 1f, flash)
            c.restore()
        }

        val life = 1f - (ringCount - 1) * stagger
        for (i in 0 until ringCount) {
            val p = (tt - i * stagger) / life
            if (p <= 0f || p >= 1f) continue

            val fade = 1f - p
            val amp = (fade * fade * 0.55f + fade * 0.45f) * (1f - i * 0.14f)
            val r = maxR * (0.10f + 0.90f * Ease.outCubic(p)) * (1f - i * 0.07f)
            val w = s.unit * (3.2f + 8.5f * fade)

            // refractive lens
            c.save()
            c.scale(r, r)
            lens.alpha = (amp * 235f).toInt()
            c.drawCircle(0f, 0f, 1f, lens)
            c.restore()

            // soft tinted body
            body.strokeWidth = w * 1.6f
            body.color = Colors.alpha(tint, 0.30f * amp)
            c.drawCircle(0f, 0f, r, body)

            // dark trough on the lee side (lower right)
            c.save()
            c.rotate(-135f)
            troughPaint.strokeWidth = w * 0.85f
            troughPaint.alpha = (amp * 255f * 0.50f).toInt()
            c.drawCircle(0f, 0f, r + w * 0.55f, troughPaint)
            c.restore()

            // bright crest facing the light (upper left)
            c.save()
            c.rotate(45f)
            crestPaint.strokeWidth = w * 0.52f
            crestPaint.alpha = (amp * 255f).toInt()
            c.drawCircle(0f, 0f, r - w * 0.28f, crestPaint)
            c.restore()

            // thin inner caustic
            caustic.strokeWidth = s.unit * 1.1f
            caustic.color = Colors.alpha(Colors.lighten(tint, 0.5f), 0.28f * amp)
            c.drawCircle(0f, 0f, r * 0.80f, caustic)
        }
        c.restore()

        // ---- droplets (screen space) ----
        val da = 1f - Ease.smooth((tt - 0.15f) / 0.60f)
        if (da > 0.01f) {
            for (d in drops) {
                if (d.vy > 0f && d.y > s.y + s.unit * 2f) continue // landed
                dropSphere.draw(c, d.x, d.y, d.r, da, glowOn = false)
            }
        }
    }

    private fun stroke() = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    /** Symmetric sweep that peaks at 180° – rotate the canvas to aim the highlight. */
    private fun peak(c: Int) = SweepGradient(
        0f, 0f,
        intArrayOf(Colors.alpha(c, 0f), Colors.alpha(c, 0f), c, Colors.alpha(c, 0f), Colors.alpha(c, 0f)),
        floatArrayOf(0f, 0.27f, 0.5f, 0.73f, 1f)
    )

    private companion object {
        /** Perspective squash of the water plane. */
        const val TILT = 0.64f
    }
}
