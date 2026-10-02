package com.example.mapphim

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.round

class KItem(val id: Int, var t: String, var x: Float, var y: Float, var s: Float,
            var k: MutableList<String>, var label: String, var sens: Int) {
    fun json(): JSONObject = JSONObject().put("id", id).put("t", t).put("x", x.toDouble()).put("y", y.toDouble())
        .put("s", s.toDouble()).put("k", JSONArray(k)).put("label", label).put("sens", sens)

    companion object {
        fun list(a: JSONArray): MutableList<KItem> = MutableList(a.length()) { i ->
            val o = a.getJSONObject(i)
            KItem(o.optInt("id", i + 1), o.getString("t"), o.getDouble("x").toFloat(), o.getDouble("y").toFloat(),
                o.getDouble("s").toFloat(), o.getJSONArray("k").let { k -> MutableList(k.length()) { k.getString(it) } },
                o.optString("label", ""), o.optInt("sens", 50))
        }
        fun array(l: List<KItem>): JSONArray = JSONArray().also { a -> l.forEach { a.put(it.json()) } }
    }
}

fun keyLabel(k: String, short: Boolean = false) = when (k) {
    "Mouse0" -> if (short) "LMB" else "Chuột trái"
    "Mouse1" -> if (short) "MMB" else "Chuột giữa"
    "Mouse2" -> if (short) "RMB" else "Chuột phải"
    else -> k
}

fun typeName(t: String) = when (t) {
    "tap" -> "Nút bấm"; "hold" -> "Nút giữ"; "toggle" -> "Bật/tắt"
    "joy" -> "Cần di chuyển"; "look" -> "Xoay camera"; else -> "Ngắm"
}

/** Sơ đồ phím 16:9: chạm để chọn, kéo để di chuyển. */
class MapCanvas(ctx: Context, attrs: AttributeSet?) : View(ctx, attrs) {
    var items: List<KItem> = emptyList()
    var selected: KItem? = null
    var bg: Bitmap? = null
    var onSelect: (KItem?) -> Unit = {}
    var onChange: () -> Unit = {}
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; color = Color.WHITE; typeface = Typeface.DEFAULT_BOLD }
    private var drag: KItem? = null

    override fun onMeasure(w: Int, h: Int) {
        val ww = MeasureSpec.getSize(w)
        setMeasuredDimension(ww, ww * 9 / 16)
    }

    private fun cx(i: KItem) = i.x / 100f * width
    private fun cy(i: KItem) = i.y / 100f * height
    private fun rad(i: KItem) = i.s / 100f * width / 2f
    private fun hit(i: KItem, px: Float, py: Float) =
        if (i.t == "look") abs(px - cx(i)) <= rad(i) && abs(py - cy(i)) <= rad(i) * 0.6f
        else hypot(px - cx(i), py - cy(i)) <= maxOf(rad(i), 28f)

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val r = RectF(0f, 0f, w, h)
        p.style = Paint.Style.FILL; p.color = Color.parseColor("#08101F"); c.drawRoundRect(r, 16f, 16f, p)
        bg?.let { c.drawBitmap(it, null, r, null) }
        p.style = Paint.Style.STROKE; p.strokeWidth = 1f
        p.color = if (bg != null) Color.argb(40, 120, 180, 255) else Color.parseColor("#14304F")
        for (g in 1..15) c.drawLine(w * g / 16f, 0f, w * g / 16f, h, p)
        for (g in 1..8) c.drawLine(0f, h * g / 9f, w, h * g / 9f, p)
        for (i in items) {
            val x = cx(i); val y = cy(i); val ra = rad(i); val on = i === selected
            val txt: String
            if (i.t == "look") {
                val rc = RectF(x - ra, y - ra * 0.6f, x + ra, y + ra * 0.6f)
                p.style = Paint.Style.FILL; p.color = Color.argb(35, 255, 181, 71); c.drawRoundRect(rc, 14f, 14f, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = if (on) Color.WHITE else Color.parseColor("#FFB547")
                p.pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f); c.drawRoundRect(rc, 14f, 14f, p); p.pathEffect = null
                txt = "Camera"
            } else {
                p.style = Paint.Style.FILL; p.color = Color.argb(70, 23, 195, 234); c.drawCircle(x, y, ra, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = if (on) Color.parseColor("#FFB547") else Color.parseColor("#17C3EA")
                c.drawCircle(x, y, ra, p)
                txt = if (i.t == "joy") i.k.joinToString("") else keyLabel(i.k.firstOrNull() ?: "?", true)
            }
            val ts = (ra * if (i.t == "joy") 0.45f else 0.6f).coerceIn(16f, 38f)
            tp.textSize = ts
            val tw = tp.measureText(txt)
            if (tw > ra * 1.8f) tp.textSize = ts * ra * 1.8f / tw
            c.drawText(txt, x, y + tp.textSize * 0.35f, tp)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                drag = items.lastOrNull { hit(it, e.x, e.y) }
                selected = drag; onSelect(drag); invalidate()
                if (drag != null) parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> drag?.let {
                it.x = round((e.x / width * 100f).coerceIn(2f, 98f) * 10f) / 10f
                it.y = round((e.y / height * 100f).coerceIn(3f, 97f) * 10f) / 10f
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { if (drag != null) onChange(); drag = null }
        }
        return true
    }
}

/** Khung có bốn góc ngoặc sáng, dùng cho popup kích hoạt. */
class CornerFrame(ctx: Context, attrs: AttributeSet?) : FrameLayout(ctx, attrs) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.parseColor("#17C3EA")
        strokeWidth = 2f * resources.displayMetrics.density; strokeCap = Paint.Cap.SQUARE
    }
    init { setWillNotDraw(false) }

    override fun dispatchDraw(c: Canvas) {
        super.dispatchDraw(c)
        val d = resources.displayMetrics.density
        val m = 12 * d; val l = 20 * d; val w = width.toFloat(); val h = height.toFloat()
        c.drawLines(floatArrayOf(
            m, m, m + l, m, m, m, m, m + l,
            w - m, m, w - m - l, m, w - m, m, w - m, m + l,
            m, h - m, m + l, h - m, m, h - m, m, h - m - l,
            w - m, h - m, w - m - l, h - m, w - m, h - m, w - m, h - m - l), p)
    }
}
