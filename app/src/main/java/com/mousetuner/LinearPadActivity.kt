package com.mousetuner

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout

class LinearPadActivity : Activity() {
    private lateinit var pad: LinearPad
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        pad = LinearPad(this)
        val fl = FrameLayout(this); fl.addView(pad)
        val bar = row()
        bar.put(btn("Bắt chuột (Raw)", true) { pad.capture() })
        bar.put(btn("Xóa") { pad.clear() })
        bar.put(btn("Tự kiểm tra") {
            AlertDialog.Builder(this).setTitle("Kiểm tra Linear Mouse").setMessage(LinearMouse.selfTest()).setPositiveButton("OK", null).show()
        })
        fl.addView(bar, FrameLayout.LayoutParams(dp(440), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END))
        setContentView(fl)
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if (pad.captured) pad.release() else super.onBackPressed() }
}

/** Vùng thử Raw: deltaX/deltaY lấy trực tiếp từ chuột (Pointer Capture), nhân sensitivity tuyến tính. */
class LinearPad(c: Context) : View(c) {
    private val lin = Cfg.lin(c)
    private val lm = LinearMouse(if (lin) Cfg.sens(c) else 1f, if (lin) Cfg.sx(c) else 1f, if (lin) Cfg.sy(c) else 1f)
    var captured = false; private set
    private var px = -1f; private var py = -1f
    private var n = 0; private var rawX = 0.0; private var rawY = 0.0; private var oX = 0L; private var oY = 0L; private var maxD = 0f
    private val trail = Path(); private var pts = 0
    private val pT = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = RED; style = Paint.Style.STROKE; strokeWidth = 4f }
    private val pD = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val pX = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = c.dp(14).toFloat() }
    init { isFocusable = true; isFocusableInTouchMode = true; setBackgroundColor(0xFF080204.toInt()) }

    fun capture() { requestFocus(); requestPointerCapture() }
    fun release() { releasePointerCapture() }
    fun clear() { n = 0; rawX = 0.0; rawY = 0.0; oX = 0; oY = 0; maxD = 0f; lm.reset(); trail.reset(); pts = 0; invalidate() }
    override fun onPointerCaptureChange(hasCapture: Boolean) { captured = hasCapture; invalidate() }
    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { px = w / 2f; py = h / 2f; trail.reset(); trail.moveTo(px, py) }

    private fun step(dx: Float, dy: Float) {
        n++; rawX += dx; rawY += dy; maxD = Math.max(maxD, Math.hypot(dx.toDouble(), dy.toDouble()).toFloat())
        lm.feed(dx, dy); oX += lm.outX; oY += lm.outY
        px = (px + lm.outX).coerceIn(0f, width.toFloat()); py = (py + lm.outY).coerceIn(0f, height.toFloat())
        trail.lineTo(px, py); if (++pts > 1500) { trail.reset(); trail.moveTo(px, py); pts = 0 }
    }
    override fun onCapturedPointerEvent(e: MotionEvent): Boolean {
        for (i in 0 until e.historySize) step(e.getHistoricalX(i), e.getHistoricalY(i))   // không bỏ sót mẫu lịch sử
        step(e.x, e.y); invalidate(); return true
    }
    override fun onGenericMotionEvent(e: MotionEvent): Boolean {
        if (!captured && e.actionMasked == MotionEvent.ACTION_HOVER_MOVE) { px = e.x; py = e.y; invalidate(); return true }
        return super.onGenericMotionEvent(e)
    }
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        c.drawPath(trail, pT); c.drawCircle(px, py, 9f, pD)
        val rx = if (rawX != 0.0) oX / rawX else 0.0; val ry = if (rawY != 0.0) oY / rawY else 0.0
        val l = listOf(
            "Linear Mouse: " + (if (lin) "ON  (sens %.2f, X×%.2f, Y×%.2f)".format(lm.sens, lm.sx, lm.sy) else "OFF (raw 1:1, chưa nhân)"),
            "Trạng thái: " + (if (captured) "ĐÃ BẮT CHUỘT (Raw) · Back để thoát" else "chưa bắt — bấm 'Bắt chuột (Raw)'"),
            "Mẫu xử lý: $n   |   Δ lớn nhất: %.1f".format(maxD),
            "Tổng gốc: (%.0f, %.0f)  →  Tổng ra: ($oX, $oY)".format(rawX, rawY),
            "Tỉ lệ ra/gốc: X=%.3f  Y=%.3f  (hằng số = tuyến tính, không đổi khi kéo nhanh/chậm)".format(rx, ry)
        )
        var y = pX.textSize * 1.6f; for (t in l) { c.drawText(t, 24f, y, pX); y += pX.textSize * 1.5f }
    }
}
