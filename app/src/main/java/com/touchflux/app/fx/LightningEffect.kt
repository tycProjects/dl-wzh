package com.touchflux.app.fx

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Electric discharge: forked, flickering bolts built by midpoint displacement, rendered as
 * layered additive strokes (glow → halo → hot core), a blinding impact flash, a crackling corona
 * and sparks.
 */
class LightningEffect(spec: Spec) : Effect(spec) {
    private val rng = s.rng
    private val paths = ArrayList<Path>()
    private val widths = ArrayList<Float>()
    private val corona = Path()
    private var sinceRebuild = 0f
    private var flicker = 1f

    private val glowPaint = strokePaint()
    private val haloPaint = strokePaint()
    private val corePaint = strokePaint()
    private val coronaPaint = strokePaint()
    private val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val additive = PorterDuffXfermode(PorterDuff.Mode.ADD)

    private val flash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(0xFFFFFFFF.toInt(), Colors.alpha(Colors.lighten(s.color, 0.3f), 0.65f), Colors.alpha(s.color, 0f)),
            floatArrayOf(0f, 0.30f, 1f),
            Shader.TileMode.CLAMP
        )
        xfermode = additive
    }

    private val sparkAngle: FloatArray
    private val sparkSpeed: FloatArray

    init {
        val n = if (s.small) 3 else 14
        sparkAngle = FloatArray(n) { rng.nextFloat() * (Math.PI.toFloat() * 2f) }
        sparkSpeed = FloatArray(n) { s.unit * (60f + rng.nextFloat() * 220f) }
        rebuild()
    }

    override fun update(dt: Float) {
        super.update(dt)
        flicker = 0.55f + 0.45f * rng.nextFloat()
        sinceRebuild += dt
        if (sinceRebuild >= REBUILD_EVERY && t < 0.72f) {
            sinceRebuild = 0f
            rebuild()
        }
    }

    override fun draw(c: Canvas) {
        val tt = t
        val master = Ease.clamp01((1f - tt) * 1.25f) * flicker

        // impact flash
        if (tt < 0.32f) {
            val k = 1f - tt / 0.32f
            flash.alpha = (k * k * 255f).toInt()
            val fr = s.unit * (if (s.small) 16f else 34f) * (0.7f + 0.6f * tt / 0.32f)
            c.save()
            c.translate(s.x, s.y)
            c.scale(fr, fr)
            c.drawCircle(0f, 0f, 1f, flash)
            c.restore()
        }

        // bolts: wide glow, coloured halo, white-hot core
        pass(c, glowPaint, s.unit * 15f, Colors.alpha(s.color, 0.16f * master), additive)
        pass(c, haloPaint, s.unit * 6.5f, Colors.alpha(Colors.lighten(s.color, 0.25f), 0.38f * master), additive)
        pass(c, corePaint, s.unit * 2.0f, Colors.alpha(Colors.lighten(s.color, 0.85f), 0.98f * master), null)

        // corona
        if (!s.small && tt < 0.6f) {
            val p = tt / 0.6f
            val r = s.unit * (12f + 34f * Ease.outExpo(p))
            c.save()
            c.translate(s.x, s.y)
            c.scale(r, r)
            coronaPaint.xfermode = additive
            coronaPaint.strokeWidth = s.unit * 1.5f / r
            coronaPaint.color = Colors.alpha(Colors.lighten(s.color, 0.5f), 0.7f * (1f - p) * flicker)
            c.drawPath(corona, coronaPaint)
            c.restore()
        }

        // sparks
        val decay = 1f - tt
        for (i in sparkAngle.indices) {
            val d = sparkSpeed[i] * Ease.outCubic(Ease.clamp01(tt * 1.6f)) * 0.55f
            val x = s.x + cos(sparkAngle[i]) * d
            val y = s.y + sin(sparkAngle[i]) * d
            sparkPaint.xfermode = additive
            sparkPaint.color = Colors.alpha(Colors.lighten(s.color, 0.7f), decay * decay * flicker)
            c.drawCircle(x, y, s.unit * 1.5f * decay + 0.5f, sparkPaint)
        }
    }

    private fun pass(c: Canvas, p: Paint, width: Float, color: Int, mode: PorterDuffXfermode?) {
        p.xfermode = mode
        p.color = color
        for (i in paths.indices) {
            p.strokeWidth = width * widths[i]
            c.drawPath(paths[i], p)
        }
    }

    private fun rebuild() {
        paths.clear()
        widths.clear()

        val n = if (s.small) 2 else (3 + (s.intensity * 2.2f).toInt()).coerceAtMost(8)
        val twoPi = Math.PI.toFloat() * 2f
        for (i in 0 until n) {
            val base = i * (twoPi / n) + rng.nextFloat() * 0.8f
            val len = s.unit * (if (s.small) 26f + rng.nextFloat() * 18f else 52f + rng.nextFloat() * 58f)
            val pts = jagged(s.x, s.y, s.x + cos(base) * len, s.y + sin(base) * len, 5)
            paths.add(toPath(pts))
            widths.add(1f)

            if (!s.small) {
                repeat(1 + rng.nextInt(2)) {
                    val count = pts.size / 2
                    val idx = 3 + rng.nextInt(count - 6)
                    val bx = pts[idx * 2]
                    val by = pts[idx * 2 + 1]
                    val sign = if (rng.nextBoolean()) 1f else -1f
                    val ba = base + sign * (0.4f + rng.nextFloat() * 0.6f)
                    val bl = len * (0.25f + rng.nextFloat() * 0.30f)
                    paths.add(toPath(jagged(bx, by, bx + cos(ba) * bl, by + sin(ba) * bl, 4)))
                    widths.add(0.55f)
                }
            }
        }

        // jittery unit-circle polygon for the corona
        corona.reset()
        val seg = 28
        for (i in 0 until seg) {
            val a = i * (twoPi / seg)
            val rr = 1f + (rng.nextFloat() * 2f - 1f) * 0.16f
            val px = cos(a) * rr
            val py = sin(a) * rr
            if (i == 0) corona.moveTo(px, py) else corona.lineTo(px, py)
        }
        corona.close()
    }

    /** Midpoint-displacement polyline. Returns flat [x0,y0,x1,y1,...] with 2^iters + 1 points. */
    private fun jagged(x0: Float, y0: Float, x1: Float, y1: Float, iters: Int): FloatArray {
        var pts = floatArrayOf(x0, y0, x1, y1)
        var n = 2
        var off = hypot(x1 - x0, y1 - y0) * 0.30f
        repeat(iters) {
            val next = FloatArray((n * 2 - 1) * 2)
            for (i in 0 until n - 1) {
                val ax = pts[i * 2]
                val ay = pts[i * 2 + 1]
                val bx = pts[i * 2 + 2]
                val by = pts[i * 2 + 3]
                val dx = bx - ax
                val dy = by - ay
                val l = hypot(dx, dy).coerceAtLeast(0.0001f)
                val o = (rng.nextFloat() * 2f - 1f) * off
                next[i * 4] = ax
                next[i * 4 + 1] = ay
                next[i * 4 + 2] = (ax + bx) * 0.5f - dy / l * o
                next[i * 4 + 3] = (ay + by) * 0.5f + dx / l * o
            }
            next[next.size - 2] = pts[n * 2 - 2]
            next[next.size - 1] = pts[n * 2 - 1]
            pts = next
            n = n * 2 - 1
            off *= 0.55f
        }
        return pts
    }

    private fun toPath(pts: FloatArray): Path {
        val p = Path()
        p.moveTo(pts[0], pts[1])
        var i = 2
        while (i < pts.size) {
            p.lineTo(pts[i], pts[i + 1])
            i += 2
        }
        return p
    }

    private fun strokePaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private companion object {
        const val REBUILD_EVERY = 0.055f
    }
}
