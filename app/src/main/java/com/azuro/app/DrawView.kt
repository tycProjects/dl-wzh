package com.azuro.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** Full-screen, touch-through overlay. Shapes are anchored to element boxes (normalized 0..1 of the real screen). */
class DrawView(c: Context) : View(c) {
    class Sh(val t: String, val box: RectF, val label: String, val n: Int)

    private val shapes = ArrayList<Sh>()
    private var sw = 1; private var sh = 1
    private var phase = 0f
    private val dirty = Rect()
    private fun d(v: Float) = context.dpf(v)

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = d(3f); color = T.mark; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val halo = Paint(stroke).apply { strokeWidth = d(6.5f); color = Color.argb(220, 255, 255, 255) }
    private val edge = Paint(stroke).apply { strokeWidth = d(2f); color = Color.WHITE }
    private val ring = Paint(stroke)
    private val soft = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = T.mark; alpha = 34 }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = T.mark }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = d(13f); typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) }
    private val anim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1500; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
        addUpdateListener { phase = it.animatedValue as Float; invalidate(dirty) }
    }

    fun setScreen(w: Int, h: Int) { sw = w; sh = h }
    fun clear() { shapes.clear(); anim.cancel(); invalidate() }

    fun set(list: List<Sh>) {
        shapes.clear(); shapes.addAll(list)
        var u: RectF? = null
        for (s in shapes) { val r = rect(s); if (u == null) u = RectF(r) else u.union(r) }
        if (u != null) { val m = d(170f); dirty.set((u.left - m).toInt(), (u.top - m).toInt(), (u.right + m).toInt(), (u.bottom + m).toInt()) }
        if (shapes.isEmpty()) anim.cancel() else if (!anim.isStarted) anim.start()
        invalidate()
    }

    override fun onDetachedFromWindow() { anim.cancel(); super.onDetachedFromWindow() }

    private fun rect(s: Sh): RectF {
        val r = RectF(s.box.left * sw, s.box.top * sh, s.box.right * sw, s.box.bottom * sh)
        val min = d(44f)
        if (r.width() < min) r.inset(-(min - r.width()) / 2, 0f)
        if (r.height() < min) r.inset(0f, -(min - r.height()) / 2)
        r.inset(-d(5f), -d(5f)); return r
    }

    override fun onDraw(cv: Canvas) {
        val loc = IntArray(2); getLocationOnScreen(loc); cv.translate(-loc[0].toFloat(), -loc[1].toFloat())
        for (s in shapes) {
            val r = rect(s); val rad = d(14f)
            when (s.t) {
                "circle" -> { cv.drawOval(r, soft); cv.drawOval(r, halo); cv.drawOval(r, stroke); pulse(cv, r, true) }
                "arrow" -> arrow(cv, r, s.label)
                else -> {
                    cv.drawRoundRect(r, rad, rad, soft); cv.drawRoundRect(r, rad, rad, halo); cv.drawRoundRect(r, rad, rad, stroke); pulse(cv, r, false)
                    if (s.t == "step") badge(cv, r.left + d(2f), r.top + d(2f), s.n)
                    if (s.label.isNotEmpty()) { val up = r.top > d(54f); pill(cv, s.label, r.centerX(), if (up) r.top - d(8f) else r.bottom + d(8f), up) }
                }
            }
        }
    }

    private fun pulse(cv: Canvas, r: RectF, oval: Boolean) {
        val g = phase * d(12f); val rr = RectF(r).apply { inset(-g, -g) }
        ring.alpha = ((1 - phase) * 150).toInt()
        if (oval) cv.drawOval(rr, ring) else cv.drawRoundRect(rr, d(14f) + g, d(14f) + g, ring)
    }

    private fun arrow(cv: Canvas, r: RectF, label: String) {
        val gap = d(6f); val len = d(62f) + sin(phase * 2 * PI).toFloat() * d(5f)
        val sp = floatArrayOf(r.top, sh - r.bottom, r.left, sw - r.right)
        var side = if (sp[0] >= sp[1]) 0 else 1
        if (max(sp[2], sp[3]) > max(sp[0], sp[1]) * 1.6f) side = if (sp[2] >= sp[3]) 2 else 3
        val tx: Float; val ty: Float; val ax: Float; val ay: Float
        when (side) {
            0 -> { tx = r.centerX(); ty = r.top - gap; ax = tx; ay = ty - len }
            1 -> { tx = r.centerX(); ty = r.bottom + gap; ax = tx; ay = ty + len }
            2 -> { tx = r.left - gap; ty = r.centerY(); ax = tx - len; ay = ty }
            else -> { tx = r.right + gap; ty = r.centerY(); ax = tx + len; ay = ty }
        }
        val dx = tx - ax; val dy = ty - ay; val l = sqrt(dx * dx + dy * dy); val ux = dx / l; val uy = dy / l
        val hs = d(15f); val px = -uy; val py = ux
        val head = Path().apply {
            moveTo(tx, ty); lineTo(tx - ux * hs + px * hs * 0.62f, ty - uy * hs + py * hs * 0.62f)
            lineTo(tx - ux * hs - px * hs * 0.62f, ty - uy * hs - py * hs * 0.62f); close()
        }
        val ex = tx - ux * hs * 0.6f; val ey = ty - uy * hs * 0.6f
        cv.drawLine(ax, ay, ex, ey, halo); cv.drawPath(head, halo); cv.drawLine(ax, ay, ex, ey, stroke)
        cv.drawPath(head, fill); cv.drawCircle(ax, ay, d(4.5f), fill); cv.drawCircle(ax, ay, d(4.5f), edge)
        if (label.isNotEmpty()) when (side) {
            0 -> pill(cv, label, ax, ay - d(6f), true)
            1 -> pill(cv, label, ax, ay + d(6f), false)
            else -> pill(cv, label, ax, ay - d(10f), true)
        }
    }

    private fun badge(cv: Canvas, x: Float, y: Float, n: Int) {
        cv.drawCircle(x, y, d(13f), fill); cv.drawCircle(x, y, d(13f), edge)
        val s = n.toString(); cv.drawText(s, x - txt.measureText(s) / 2, y - (txt.ascent() + txt.descent()) / 2, txt)
    }

    private fun pill(cv: Canvas, text: String, x: Float, y: Float, above: Boolean) {
        val t = if (text.length > 28) text.take(27) + "…" else text
        val w = txt.measureText(t) + d(22f); val h = d(28f)
        val l = (x - w / 2).coerceIn(d(8f), max(d(8f), sw - w - d(8f)))
        val tp = (if (above) y - h else y).coerceIn(d(8f), max(d(8f), sh - h - d(8f)))
        val rr = RectF(l, tp, l + w, tp + h)
        cv.drawRoundRect(rr, h / 2, h / 2, fill); cv.drawRoundRect(rr, h / 2, h / 2, edge)
        cv.drawText(t, l + d(11f), tp + h / 2 - (txt.ascent() + txt.descent()) / 2, txt)
    }
}
