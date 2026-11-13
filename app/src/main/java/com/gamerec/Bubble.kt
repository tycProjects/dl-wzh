package com.gamerec

import android.content.Context
import android.graphics.*
import android.view.View

/** الدائرة العائمة. خفيفة: بدون أنيميشن مستمر، والظل المتدرج يتخزن مرة وحدة. */
class Bubble(c: Context) : View(c) {
    var accent = 0xFF7C5CFF.toInt()
        set(v) { field = v; sh = null }
    var accent2 = 0xFFFF4DA6.toInt()
        set(v) { field = v; sh = null }
    var img: Bitmap? = null; var label: String? = null; var core = 100f
    private var rec = false
    private var sh: Shader? = null
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val logo = c.getDrawable(R.drawable.ic_logo)!!

    fun setRec(on: Boolean) { rec = on; invalidate() }
    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { sh = null }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f; val cy = height / 2f; val r = core / 2f; val rw = core * .075f
        p.shader = null; p.style = Paint.Style.FILL
        p.color = if (rec) Ui.RED else accent; p.alpha = if (rec) 80 else 50
        c.drawCircle(cx, cy, r + core * .14f, p)
        p.color = 0xFF0E1120.toInt(); c.drawCircle(cx, cy, r, p)
        val inner = r - rw * 1.6f
        val im = img
        if (im != null) {
            c.save(); val path = Path(); path.addCircle(cx, cy, inner, Path.Direction.CW); c.clipPath(path)
            c.drawBitmap(im, null, RectF(cx - inner, cy - inner, cx + inner, cy + inner), p); c.restore()
        } else {
            val s = inner * 1.05f
            logo.setBounds((cx - s / 2).toInt(), (cy - s / 2).toInt(), (cx + s / 2).toInt(), (cy + s / 2).toInt()); logo.draw(c)
        }
        p.style = Paint.Style.STROKE; p.strokeWidth = rw
        if (rec) p.color = Ui.RED else p.shader = sh ?: SweepGradient(cx, cy, intArrayOf(accent, accent2, accent), null).also { sh = it }
        c.drawCircle(cx, cy, r - rw / 2f, p); p.shader = null
        label?.let {
            p.style = Paint.Style.FILL; p.color = 0xCC0E1120.toInt(); c.drawCircle(cx, cy, inner, p)
            p.color = Color.WHITE; p.textSize = core * .5f; p.textAlign = Paint.Align.CENTER; p.isFakeBoldText = true
            c.drawText(it, cx, cy + p.textSize * .35f, p); p.isFakeBoldText = false
        }
    }
}
