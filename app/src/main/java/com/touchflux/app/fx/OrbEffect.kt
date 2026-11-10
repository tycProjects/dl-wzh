package com.touchflux.app.fx

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin

/**
 * A burst of glossy 3D beads flung outward from the touch point, with a glassy bubble "pop"
 * and a shock ring at the centre.
 */
class OrbEffect(spec: Spec) : Effect(spec) {
    private class Orb(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        val r: Float, val sphere: Sphere, val delay: Float
    )

    private val orbs = ArrayList<Orb>()
    private val hero = Sphere(Colors.lighten(s.color, 0.15f))
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val drag = 2.6f
    private val g = s.unit * 70f

    init {
        val n = if (s.small) 2 else (6 + 6 * s.intensity).toInt()
        val sizeK = if (s.small) 0.7f else 1f
        for (i in 0 until n) {
            val a = s.rng.nextFloat() * (Math.PI.toFloat() * 2f)
            val speed = s.unit * (70f + s.rng.nextFloat() * 210f) * (if (s.small) 0.5f else 1f)
            val col = when (i % 3) {
                0 -> Colors.hueShift(s.color, 38f)
                1 -> s.color
                else -> Colors.lighten(s.color, 0.22f)
            }
            orbs.add(
                Orb(
                    s.x, s.y, cos(a) * speed, sin(a) * speed,
                    s.unit * (2.6f + s.rng.nextFloat() * 5.8f) * sizeK,
                    Sphere(col),
                    s.rng.nextFloat() * 0.05f
                )
            )
        }
    }

    override fun update(dt: Float) {
        super.update(dt)
        val k = (1f - drag * dt).coerceAtLeast(0f)
        for (o in orbs) {
            o.x += o.vx * dt
            o.y += o.vy * dt
            o.vx *= k
            o.vy = o.vy * k + g * dt
        }
    }

    override fun draw(c: Canvas) {
        val tt = t

        // shock ring
        if (tt < 0.5f) {
            val p = tt / 0.5f
            val r = s.unit * (10f + (if (s.small) 28f else 64f) * Ease.outExpo(p))
            ring.strokeWidth = s.unit * (3.2f * (1f - p) + 0.6f)
            ring.color = Colors.alpha(Colors.lighten(s.color, 0.6f), 0.75f * (1f - p))
            c.drawCircle(s.x, s.y, r, ring)
        }

        // glassy bubble pop in the centre
        if (!s.small && tt < 0.36f) {
            val p = tt / 0.36f
            val r = s.unit * (9f + 24f * Ease.outCubic(p))
            hero.draw(c, s.x, s.y, r, 0.55f * (1f - p * p), glowOn = true)
        }

        val fadeOut = 1f - Ease.smooth((tt - 0.55f) / 0.45f)
        val shrink = 1f - 0.55f * Ease.smooth((tt - 0.45f) / 0.55f)
        for (o in orbs) {
            val pop = Ease.outCubic(Ease.clamp01((age - o.delay) / 0.14f))
            o.sphere.draw(c, o.x, o.y, o.r * pop * shrink, fadeOut)
        }
    }
}
