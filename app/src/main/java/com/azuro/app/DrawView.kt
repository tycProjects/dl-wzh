package com.azuro.app

import android.content.Context
import android.graphics.*
import android.view.View
import org.json.JSONArray
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Lớp phủ toàn màn hình, không bắt chạm. Toạ độ chuẩn hoá 0..1. */
class DrawView(c: Context) : View(c) {
    private class S(val t: String, val a: Float, val b: Float, val c: Float, val d: Float, val s: String)
    private val shapes = mutableListOf<S>()
    private val red = Color.parseColor("#FF3B5C")
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 8f; color = red; strokeCap = Paint.Cap.ROUND }
    private val halo = Paint(line).apply { strokeWidth = 14f; color = Color.WHITE }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = red }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 44f; isFakeBoldText = true }

    fun clear() { shapes.clear(); invalidate() }

    fun set(arr: JSONArray) {
        shapes.clear()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            when (o.optString("t")) {
                "arrow" -> shapes += S("arrow", o.getDouble("x1").toFloat(), o.getDouble("y1").toFloat(), o.getDouble("x2").toFloat(), o.getDouble("y2").toFloat(), "")
                "circle" -> shapes += S("circle", o.getDouble("x").toFloat(), o.getDouble("y").toFloat(), o.optDouble("r", 0.06).toFloat(), 0f, "")
                "rect" -> shapes += S("rect", o.getDouble("x").toFloat(), o.getDouble("y").toFloat(), o.getDouble("w").toFloat(), o.getDouble("h").toFloat(), "")
                "text" -> shapes += S("text", o.getDouble("x").toFloat(), o.getDouble("y").toFloat(), 0f, 0f, o.optString("s"))
            }
        }
        invalidate()
    }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        for (s in shapes) when (s.t) {
            "arrow" -> {
                val x1 = s.a * w; val y1 = s.b * h; val x2 = s.c * w; val y2 = s.d * h
                val ang = atan2(y2 - y1, x2 - x1); val L = 46f
                val hx1 = x2 - L * cos(ang - 0.45f); val hy1 = y2 - L * sin(ang - 0.45f)
                val hx2 = x2 - L * cos(ang + 0.45f); val hy2 = y2 - L * sin(ang + 0.45f)
                for (p in listOf(halo, line)) { cv.drawLine(x1, y1, x2, y2, p); cv.drawLine(x2, y2, hx1, hy1, p); cv.drawLine(x2, y2, hx2, hy2, p) }
            }
            "circle" -> { cv.drawCircle(s.a * w, s.b * h, s.c * w, halo); cv.drawCircle(s.a * w, s.b * h, s.c * w, line) }
            "rect" -> { val r = RectF(s.a * w, s.b * h, (s.a + s.c) * w, (s.b + s.d) * h); cv.drawRoundRect(r, 16f, 16f, halo); cv.drawRoundRect(r, 16f, 16f, line) }
            "text" -> {
                val x = s.a * w; val y = s.b * h; val tw = txt.measureText(s.s)
                cv.drawRoundRect(RectF(x - 16, y - 52, x + tw + 16, y + 16), 20f, 20f, fill); cv.drawText(s.s, x, y, txt)
            }
        }
    }
}
