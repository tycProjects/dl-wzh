package com.touchflux.app.fx

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Energy nova / 4-point star: iridescent shock, crisp star core, side sparks that
 * fly outward creating a long luminous path when dragged.
 */
class NovaEffect(spec: Spec) : Effect(spec) {
    private val accent = Colors.hueShift(s.color, 42f)
    private val additive = PorterDuffXfermode(PorterDuff.Mode.ADD)

    private val core = glowPaint(0xFFFFFFFF.toInt(), s.color)
    private val flare = glowPaint(0xFFFFFFFF.toInt(), accent)

    private val ringShader = SweepGradient(
        0f, 0f,
        intArrayOf(s.color, accent, 0xFFFFFFFF.toInt(), accent, s.color),
        floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
    )
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        shader = ringShader
        xfermode = additive
    }
    private val underside = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val streak = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        xfermode = additive
    }
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        xfermode = additive
    }
    private val starPath = Path()

    private val angle: FloatArray
    private val speed: FloatArray
    private val warm: BooleanArray

    init {
        val n = if (s.small) 6 else (14 + 10 * s.intensity).toInt()
        angle = FloatArray(n) { s.rng.nextFloat() * (Math.PI.toFloat() * 2f) }
        speed = FloatArray(n) { s.unit * (90f + s.rng.nextFloat() * 320f) * (if (s.small) 0.65f else 1f) }
        warm = BooleanArray(n) { s.rng.nextBoolean() }
    }

    override fun draw(c: Canvas) {
        val tt = t
        val fade = 1f - tt

        // 4-point star core (especially visible on trails)
        run {
            val p = Ease.outCubic(Ease.clamp01(tt * 2.8f))
            val r = s.unit * (if (s.small) 14f else 26f) * (0.55f + p * 0.7f)
            starPaint.color = Colors.alpha(0xFFFFFFFF.toInt(), fade * fade * 0.95f)
            c.save()
            c.translate(s.x, s.y)
            c.rotate(tt * 90f)
            drawFourPointStar(c, r)
            c.restore()

            // soft glow under star
            core.alpha = (fade * fade * 200f).toInt()
            c.save()
            c.translate(s.x, s.y)
            c.scale(r * 1.35f, r * 1.35f)
            c.drawCircle(0f, 0f, 1f, core)
            c.restore()
        }

        // anamorphic flare streaks (only full size)
        if (!s.small) {
            val grow = Ease.outExpo(Ease.clamp01(tt * 2.2f))
            val a = (fade * fade.coerceAtLeast(0f) * 220f).toInt()
            flare.alpha = a
            c.save()
            c.translate(s.x, s.y)
            c.save()
            c.scale(s.unit * 110f * grow, s.unit * 4.2f * fade + 0.5f)
            c.drawCircle(0f, 0f, 1f, flare)
            c.restore()
            c.save()
            c.scale(s.unit * 3.0f * fade + 0.5f, s.unit * 42f * grow)
            c.drawCircle(0f, 0f, 1f, flare)
            c.restore()
            c.restore()
        }

        // shock ring
        val p = Ease.outExpo(tt)
        val r = s.unit * (10f + (if (s.small) 36f else 92f) * p)
        val w = s.unit * ((if (s.small) 3.5f else 9f) * fade + 0.7f)
        underside.strokeWidth = w
        underside.color = Colors.alpha(0xFF000010.toInt(), 0.3f * fade)
        c.drawCircle(s.x, s.y + s.unit * 1.8f, r, underside)

        c.save()
        c.translate(s.x, s.y)
        c.rotate(tt * 380f)
        ring.strokeWidth = w
        ring.alpha = (fade * 240f).toInt()
        c.drawCircle(0f, 0f, r, ring)
        c.restore()

        // sparks / side particles that fly and create the long path feel
        val k = 3.0f
        val ageSafe = age
        val e = exp(-k * ageSafe)
        for (i in angle.indices) {
            val dist = speed[i] * (1f - e) / k
            val v = speed[i] * e
            val dx = cos(angle[i])
            val dy = sin(angle[i])
            val hx = s.x + dx * dist
            val hy = s.y + dy * dist
            val len = v * 0.06f + s.unit * (if (s.small) 1.8f else 1.4f)
            val col = if (warm[i]) Colors.lighten(accent, 0.5f) else Colors.lighten(s.color, 0.5f)
            streak.strokeWidth = s.unit * (1.0f + 1.4f * fade)
            streak.color = Colors.alpha(col, fade * 0.95f)
            c.drawLine(hx - dx * len, hy - dy * len, hx, hy, streak)
        }
    }

    private fun drawFourPointStar(c: Canvas, r: Float) {
        starPath.reset()
        val outer = r
        val inner = r * 0.28f
        for (i in 0 until 8) {
            val a = (i * Math.PI / 4.0).toFloat() - (Math.PI / 2).toFloat()
            val rad = if (i % 2 == 0) outer else inner
            val px = cos(a) * rad
            val py = sin(a) * rad
            if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
        }
        starPath.close()
        c.drawPath(starPath, starPaint)
    }

    private fun glowPaint(center: Int, edge: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(center, Colors.alpha(edge, 0.55f), Colors.alpha(edge, 0f)),
            floatArrayOf(0f, 0.32f, 1f),
            Shader.TileMode.CLAMP
        )
        xfermode = additive
    }
}
