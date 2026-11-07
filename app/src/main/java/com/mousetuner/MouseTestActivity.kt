package com.mousetuner

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout

class MouseTestActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val v = TestView(this)
        val fl = FrameLayout(this); fl.addView(v)
        val bar = row()
        bar.put(btn("Xóa") { v.clear() })
        bar.put(btn("Raw input") { v.toggleRaw() })
        fl.addView(bar, FrameLayout.LayoutParams(dp(260), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END))
        setContentView(fl)
    }
}

/** Đo Hz chuột thật từ các mẫu sự kiện (kể cả mẫu lịch sử), độ giật, và độ lệch khi kéo thẳng. */
class TestView(c: Context) : View(c) {
    private val times = ArrayList<Long>()
    private var hz = 0.0; private var maxHz = 0.0
    private var lastDispatch = 0L; private var gapSum = 0L; private var gapN = 0; private var maxGap = 0L
    private val trail = Path()
    private val dx = ArrayList<Float>(); private val dy = ArrayList<Float>()
    private var straight = "Giữ chuột trái và kéo 1 đường thẳng dài để đo độ lệch"
    private var raw = false
    private var t0 = 0L; private var t1 = 0L
    private val runs = ArrayList<Pair<Float, Long>>()   // (quãng px, thời gian ms) của các lần kéo
    private var cx = -1f; private var cy = -1f
    private val pTrail = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF2A3D.toInt(); style = Paint.Style.STROKE; strokeWidth = 4f }
    private val pText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 15f * c.resources.displayMetrics.density }
    private val pDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }

    init { isFocusable = true; isFocusableInTouchMode = true; setBackgroundColor(0xFF080204.toInt()) }

    fun clear() {
        times.clear(); hz = 0.0; maxHz = 0.0; lastDispatch = 0; gapSum = 0; gapN = 0; maxGap = 0
        trail.reset(); dx.clear(); dy.clear(); straight = "Đã xóa. Kéo 1 đường thẳng để đo lại"; invalidate()
    }
    fun toggleRaw() {
        raw = !raw; clear()
        if (raw) { requestFocus(); requestPointerCapture(); straight = "RAW: chuột bị bắt hoàn toàn, nhấn Esc/Back để thoát. Chỉ đo Hz." }
        else releasePointerCapture()
    }

    private fun record(e: MotionEvent) {
        val now = e.eventTime
        if (lastDispatch != 0L && now - lastDispatch > 150) times.clear()   // nghỉ -> đo lại đoạn mới
        if (lastDispatch != 0L) { val g = now - lastDispatch; if (g < 500) { gapSum += g; gapN++; if (g > maxGap) maxGap = g } }
        lastDispatch = now
        for (i in 0 until e.historySize) times.add(e.getHistoricalEventTime(i))
        times.add(now)
        while (times.size > 1 && times[0] < now - 1000) times.removeAt(0)
        if (times.size >= 2) {
            val span = times.last() - times[0]
            if (span > 0) { hz = (times.size - 1) * 1000.0 / span; if (times.size > 20 && hz > maxHz) maxHz = hz }
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                requestUnbufferedDispatch(e); lastDispatch = 0
                trail.reset(); trail.moveTo(e.x, e.y); dx.clear(); dy.clear(); dx.add(e.x); dy.add(e.y); t0 = e.eventTime
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.historySize) { val x = e.getHistoricalX(i); val y = e.getHistoricalY(i); trail.lineTo(x, y); dx.add(x); dy.add(y) }
                trail.lineTo(e.x, e.y); dx.add(e.x); dy.add(e.y); record(e)
            }
            MotionEvent.ACTION_UP -> { trail.lineTo(e.x, e.y); dx.add(e.x); dy.add(e.y); t1 = e.eventTime; evaluate() }
        }
        cx = e.x; cy = e.y; invalidate(); return true
    }
    override fun onGenericMotionEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_HOVER_MOVE) { requestUnbufferedDispatch(e); record(e); cx = e.x; cy = e.y; invalidate(); return true }
        return super.onGenericMotionEvent(e)
    }
    override fun onCapturedPointerEvent(e: MotionEvent): Boolean { record(e); invalidate(); return true }

    private fun evaluate() {
        val n = dx.size; if (n < 3) return
        val x0 = dx[0]; val y0 = dy[0]; val x1 = dx[n - 1]; val y1 = dy[n - 1]
        val ex = x1 - x0; val ey = y1 - y0; val len = Math.hypot(ex.toDouble(), ey.toDouble()).toFloat()
        if (len < 100) { straight = "Đường kéo quá ngắn, kéo dài hơn"; return }
        var md = 0f
        for (i in 0 until n) { val d = Math.abs(ey * dx[i] - ex * dy[i] + x1 * y0 - y1 * x0) / len; if (d > md) md = d }
        straight = "Độ lệch khỏi đường thẳng: %.1f px (%.2f%% chiều dài)".format(md, md / len * 100)
        runs.add(len to (t1 - t0)); if (runs.size > 6) runs.removeAt(0)
    }

    /** Kiểm tra gia tốc thật: kéo CÙNG một quãng vật lý (vd 10cm trên thảm) lúc chậm, lúc nhanh → quãng px phải bằng nhau. */
    private fun rulerLines(): List<String> {
        val l = ArrayList<String>()
        l.add("Thước: kéo cùng 1 quãng thật (vd 10cm) lúc chậm / nhanh, so số px")
        runs.takeLast(4).forEachIndexed { i, (len, ms) -> l.add("  Lần ${runs.size - minOf(4, runs.size) + i + 1}: %.0f px trong %d ms (%.0f px/s)".format(len, ms, if (ms > 0) len * 1000f / ms else 0f)) }
        if (runs.size >= 2) {
            val mx = runs.maxOf { it.first }; val mn = runs.minOf { it.first }; val spread = (mx - mn) / mx * 100
            l.add("  Chênh lệch: %.1f%% → %s".format(spread, if (spread < 8) "TUYẾN TÍNH (không gia tốc)" else "CÓ GIA TỐC hoặc kéo không đều"))
        }
        return l
    }

    private fun rating() = when {
        hz <= 0 -> "—"
        hz >= 800 -> "Rất tốt (~1000Hz)"
        hz >= 400 -> "Tốt (~500Hz)"
        hz >= 200 -> "Khá (~250Hz)"
        hz >= 100 -> "Chuẩn (~125Hz)"
        else -> "Thấp / chuột hoặc cổng OTG đang bị giới hạn"
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        if (!raw) c.drawPath(trail, pTrail)
        if (cx >= 0 && !raw) c.drawCircle(cx, cy, 8f, pDot)
        val avg = if (gapN > 0) gapSum.toFloat() / gapN else 0f
        val lines = listOf(
            "Hz chuột (đo thật): ${hz.toInt()}   |   cao nhất: ${maxHz.toInt()}",
            "Đánh giá: ${rating()}",
            "Nhịp nhận sự kiện: %.1f ms (lâu nhất %d ms%s)".format(avg, maxGap, if (maxGap > 40) " → có giật" else ""),
            straight,
            *rulerLines().toTypedArray(),
            "Mẹo: nếu Hz thấp, thử cáp OTG tốt hơn / cắm trực tiếp / tắt chế độ tiết kiệm pin."
        )
        var y = pText.textSize * 1.6f
        for (l in lines) { c.drawText(l, 24f, y, pText); y += pText.textSize * 1.5f }
    }
}
