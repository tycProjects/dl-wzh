package com.touchflux.app.fx

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin

/**
 * Geometric shapes: outlined squares and triangles with subtle 3D edge lighting that
 * tumble and fall outward. Clean, minimal, not over-AI.
 */
class GeoEffect(spec: Spec) : Effect(spec) {
    private data class Shape(
        val kind: Int, // 0 square, 1 triangle
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var rot: Float,
        var vrot: Float,
        val size: Float,
        val delay: Float
    )

    private val shapes: Array<Shape>
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private val rect = RectF()

    init {
        val n = if (s.small) 3 else (6 + (8 * s.intensity).toInt()).coerceAtMost(18)
        shapes = Array(n) { i ->
            val a = s.rng.nextFloat() * (Math.PI * 2).toFloat()
            val speed = s.unit * (40f + s.rng.nextFloat() * 140f) * (if (s.small) 0.6f else 1f)
            Shape(
                kind = if (s.rng.nextBoolean()) 0 else 1,
                x = s.x,
                y = s.y,
                vx = cos(a) * speed,
                vy = sin(a) * speed - s.unit * 20f, // slight upward bias then fall
                rot = s.rng.nextFloat() * 360f,
                vrot = (s.rng.nextFloat() - 0.5f) * 280f,
                size = s.unit * (8f + s.rng.nextFloat() * 18f) * (if (s.small) 0.7f else 1f),
                delay = s.rng.nextFloat() * 0.12f
            )
        }
    }

    override fun update(dt: Float) {
        super.update(dt)
        val g = s.unit * 220f // gravity
        for (sh in shapes) {
            if (age < sh.delay) continue
            sh.vy += g * dt
            sh.x += sh.vx * dt
            sh.y += sh.vy * dt
            sh.rot += sh.vrot * dt
            // slight air drag
            sh.vx *= 0.995f
            sh.vy *= 0.995f
        }
    }

    override fun draw(c: Canvas) {
        val fade = (1f - t).coerceIn(0f, 1f)
        val alpha = (fade * fade * 255f).toInt()
        if (alpha < 4) return

        for (sh in shapes) {
            if (age < sh.delay) continue
            val localT = ((age - sh.delay) / (s.duration - sh.delay)).coerceIn(0f, 1f)
            val a = (alpha * (1f - localT * 0.3f)).toInt().coerceIn(0, 255)

            c.save()
            c.translate(sh.x, sh.y)
            c.rotate(sh.rot)

            // soft outer glow (3D edge feel)
            glow.color = Colors.alpha(s.color, a / 255f * 0.35f)
            glow.strokeWidth = sh.size * 0.18f
            drawShape(c, sh, glow)

            // main crisp stroke
            stroke.color = Colors.alpha(0xFFFFFFFF.toInt(), a / 255f * 0.92f)
            stroke.strokeWidth = sh.size * 0.07f
            drawShape(c, sh, stroke)

            // inner accent line for depth
            stroke.color = Colors.alpha(s.color, a / 255f * 0.55f)
            stroke.strokeWidth = sh.size * 0.035f
            c.scale(0.82f, 0.82f)
            drawShape(c, sh, stroke)

            c.restore()
        }
    }

    private fun drawShape(c: Canvas, sh: Shape, p: Paint) {
        path.reset()
        val half = sh.size * 0.5f
        if (sh.kind == 0) {
            // square
            rect.set(-half, -half, half, half)
            c.drawRect(rect, p)
        } else {
            // equilateral triangle pointing up
            path.moveTo(0f, -half * 1.15f)
            path.lineTo(half * 1.05f, half * 0.7f)
            path.lineTo(-half * 1.05f, half * 0.7f)
            path.close()
            c.drawPath(path, p)
        }
    }
}
