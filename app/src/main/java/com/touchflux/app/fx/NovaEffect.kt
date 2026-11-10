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

/** Monochrome Nova: a four-point optical star wrapped by two flowing orbital ribbons. */
class NovaEffect(spec: Spec) : Effect(spec) {
    private val accent = Colors.hueShift(s.color, 18f)
    private val additive = PorterDuffXfermode(PorterDuff.Mode.ADD)

    private val core = glowPaint(0xFFFFFFFF.toInt(), s.color)
    private val flare = glowPaint(0xFFFFFFFF.toInt(), accent)
    private val ringShader = SweepGradient(
        0f, 0f,
        intArrayOf(0x00FFFFFF, 0xCCFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xCCFFFFFF.toInt(), 0x00FFFFFF),
        floatArrayOf(0f, .20f, .48f, .76f, 1f)
    )
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        shader = ringShader
        strokeCap = Paint.Cap.ROUND
        xfermode = additive
    }
    private val underside = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val ribbonGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        xfermode = additive
    }
    private val ribbonCore = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val starGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        color = 0x99FFFFFF.toInt()
    }
    private val starCore = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        color = 0xFFFFFFFF.toInt()
    }
    private val geometryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val geometryEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val starPath = Path()
    private val orbitA = Path()
    private val orbitB = Path()
    private val squarePath = Path().apply {
        moveTo(-1f, -1f); lineTo(1f, -1f); lineTo(1f, 1f); lineTo(-1f, 1f); close()
    }
    private val trianglePath = Path().apply {
        moveTo(0f, -1.15f); lineTo(1.05f, .9f); lineTo(-1.05f, .9f); close()
    }

    private val angle: FloatArray
    private val speed: FloatArray
    private val warm: BooleanArray
    private val shapeAngle: FloatArray
    private val shapeSide: FloatArray
    private val shapeSpin: FloatArray
    private val streak = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; xfermode = additive
    }

    init {
        val n = if (s.small) 3 else (14 + 8 * s.intensity).toInt()
        angle = FloatArray(n) { s.rng.nextFloat() * (Math.PI.toFloat() * 2f) }
        speed = FloatArray(n) { s.unit * (75f + s.rng.nextFloat() * 250f) * (if (s.small) .45f else 1f) }
        warm = BooleanArray(n) { s.rng.nextBoolean() }
        val count = if (s.small) 2 else (5 + 3 * s.intensity).toInt()
        shapeAngle = FloatArray(count) { s.rng.nextFloat() * (Math.PI.toFloat() * 2f) }
        shapeSide = FloatArray(count) { 16f + s.rng.nextFloat() * 62f }
        shapeSpin = FloatArray(count) { (s.rng.nextFloat() - .5f) * 240f }
    }

    override fun draw(c: Canvas) {
        val progress = t
        val fade = 1f - Ease.outCubic(progress)
        val pulse = .94f + .06f * cos(progress * Math.PI * 5.0).toFloat()
        val size = s.unit * (if (s.small) 8.5f else 20f) * pulse

        // Soft optical bloom, kept separate from the crisp white centre.
        core.alpha = (fade * 190f).toInt()
        c.save(); c.translate(s.x, s.y); c.scale(size * 1.45f, size * 1.45f)
        c.drawCircle(0f, 0f, 1f, core); c.restore()

        if (!s.small) {
            val flareAlpha = (fade * fade * 185f).toInt()
            flare.alpha = flareAlpha
            c.save(); c.translate(s.x, s.y)
            c.save(); c.scale(s.unit * 76f, s.unit * 2.4f); c.drawCircle(0f, 0f, 1f, flare); c.restore()
            c.save(); c.scale(s.unit * 2.4f, s.unit * 48f); c.drawCircle(0f, 0f, 1f, flare); c.restore()
            c.restore()
        }

        // Expanding, thinning shock ring.
        val radius = s.unit * (if (s.small) 8f else 14f + 84f * Ease.outExpo(progress))
        val width = s.unit * (if (s.small) 1.2f else 5.5f * fade + .35f)
        underside.strokeWidth = width * 1.25f
        underside.color = Colors.alpha(0xFFBFC5CE.toInt(), .18f * fade)
        c.drawCircle(s.x, s.y + s.unit, radius, underside)
        ring.strokeWidth = width
        ring.alpha = (fade * 220f).toInt()
        c.save(); c.translate(s.x, s.y); c.rotate(-progress * 125f)
        c.drawCircle(0f, 0f, radius, ring); c.restore()

        // Four-point star, constructed in local coordinates and reused each frame.
        val arm = size * 1.8f
        val neck = size * .19f
        starPath.reset()
        starPath.moveTo(0f, -arm)
        starPath.cubicTo(neck, -neck, neck, -neck, arm, 0f)
        starPath.cubicTo(neck, neck, neck, neck, 0f, arm)
        starPath.cubicTo(-neck, neck, -neck, neck, -arm, 0f)
        starPath.cubicTo(-neck, -neck, -neck, -neck, 0f, -arm)
        starPath.close()
        starGlow.strokeWidth = s.unit * (if (s.small) 3.8f else 7f)
        starGlow.alpha = (fade * 95f).toInt()
        starCore.strokeWidth = s.unit * (if (s.small) .9f else 1.55f)
        starCore.alpha = (fade * 255f).toInt()
        c.save(); c.translate(s.x, s.y)
        c.drawPath(starPath, starGlow); c.drawPath(starPath, starCore)
        c.restore()

        // Two asymmetric Bezier ribbons orbit the star. Their ends taper visually by alpha/width.
        val orbitScale = if (s.small) .72f else 1f
        val orbitW = size * 2.65f * orbitScale
        val orbitH = size * .92f * orbitScale
        orbitA.reset()
        orbitA.moveTo(-orbitW, orbitH * .08f)
        orbitA.cubicTo(-orbitW * .62f, -orbitH * 1.35f, orbitW * .78f, -orbitH * 1.55f, orbitW, -orbitH * .28f)
        orbitA.cubicTo(orbitW * 1.12f, orbitH * .48f, orbitW * .16f, orbitH * .92f, 0f, orbitH * .18f)
        orbitB.reset()
        orbitB.moveTo(orbitW, -orbitH * .05f)
        orbitB.cubicTo(orbitW * .48f, orbitH * 1.32f, -orbitW * .82f, orbitH * 1.38f, -orbitW, orbitH * .30f)
        orbitB.cubicTo(-orbitW * 1.12f, -orbitH * .42f, -orbitW * .18f, -orbitH * .90f, 0f, -orbitH * .16f)
        val rotation = -22f + progress * 38f
        ribbonGlow.strokeWidth = s.unit * (if (s.small) 2.6f else 5.4f) * fade
        ribbonGlow.color = Colors.alpha(0xFFFFFFFF.toInt(), .22f * fade)
        ribbonCore.strokeWidth = s.unit * (if (s.small) .75f else 1.15f) * fade
        ribbonCore.color = Colors.alpha(0xFFF7F8FA.toInt(), .88f * fade)
        c.save(); c.translate(s.x, s.y); c.rotate(rotation)
        c.drawPath(orbitA, ribbonGlow); c.drawPath(orbitB, ribbonGlow)
        c.drawPath(orbitA, ribbonCore); c.drawPath(orbitB, ribbonCore)
        c.restore()

        if (s.geometry) drawGeometry(c, fade, progress)

        val decay = exp(-3.0f * age)
        for (i in angle.indices) {
            val dx = cos(angle[i]); val dy = sin(angle[i])
            val distance = speed[i] * (1f - decay) / 3f
            val velocity = speed[i] * decay
            val x = s.x + dx * distance
            val y = s.y + dy * distance
            val length = velocity * .045f + s.unit
            val color = if (warm[i]) Colors.lighten(accent, .48f) else 0xFFFFFFFF.toInt()
            streak.strokeWidth = s.unit * (1f + fade)
            streak.color = Colors.alpha(color, fade)
            c.drawLine(x - dx * length, y - dy * length, x, y, streak)
        }
    }

    private fun drawGeometry(c: Canvas, fade: Float, progress: Float) {
        for (i in shapeAngle.indices) {
            val travel = s.unit * shapeSide[i] * (.22f + progress * 1.22f)
            val x = s.x + cos(shapeAngle[i]) * travel
            val y = s.y + sin(shapeAngle[i]) * travel + s.unit * 30f * progress * progress
            val size = s.unit * (if (s.small) 2.8f else 5f) * (1f - progress * .2f)
            val alpha = (fade * 205f).toInt().coerceIn(0, 255)
            geometryPaint.strokeWidth = maxOf(.8f, s.unit * .78f)
            geometryPaint.color = android.graphics.Color.argb(alpha, 245, 247, 250)
            geometryEdge.strokeWidth = maxOf(.7f, s.unit * .65f)
            geometryEdge.color = android.graphics.Color.argb((alpha * .38f).toInt(), 125, 133, 145)
            c.save(); c.translate(x, y); c.rotate(shapeSpin[i] * progress); c.scale(size, size)
            val path = if (i % 2 == 0) squarePath else trianglePath
            c.save(); c.translate(.22f, .22f); c.drawPath(path, geometryEdge); c.restore()
            c.drawPath(path, geometryPaint)
            c.restore()
        }
    }

    private fun glowPaint(center: Int, edge: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(0f, 0f, 1f,
            intArrayOf(center, Colors.alpha(edge, .5f), Colors.alpha(edge, 0f)),
            floatArrayOf(0f, .32f, 1f), Shader.TileMode.CLAMP)
        xfermode = additive
    }
}
