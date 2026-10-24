package com.devrafa.gamebooster

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.view.View

class CursorOverlayView(
    context: Context,
    private val style: Int,
    private val sizeDp: Float,
    private val opacity: Float,
    private val thicknessDp: Float
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = (opacity.coerceIn(0.1f, 1f) * 255).toInt()
        strokeWidth = thicknessDp.coerceIn(1f, 8f) * resources.displayMetrics.density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = (opacity.coerceIn(0.1f, 1f) * 255).toInt()
        style = Paint.Style.FILL
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = (sizeDp * resources.displayMetrics.density / 2f).coerceAtMost(width / 2f - 2f)
        val gap = r * .28f
        when (style) {
            0 -> {
                canvas.drawCircle(cx, cy, r * .45f, paint)
                canvas.drawCircle(cx, cy, paint.strokeWidth * .7f, fill)
            }
            1 -> {
                canvas.drawLine(cx-r, cy, cx-gap, cy, paint)
                canvas.drawLine(cx+gap, cy, cx+r, cy, paint)
                canvas.drawLine(cx, cy-r, cx, cy-gap, paint)
                canvas.drawLine(cx, cy+gap, cx, cy+r, paint)
                canvas.drawCircle(cx, cy, paint.strokeWidth * .65f, fill)
            }
            2 -> {
                canvas.drawLine(cx-r, cy, cx-gap, cy, paint)
                canvas.drawLine(cx+gap, cy, cx+r, cy, paint)
                canvas.drawLine(cx, cy-r, cx, cy-gap, paint)
                canvas.drawLine(cx, cy+gap, cx, cy+r, paint)
                canvas.drawCircle(cx, cy, r*.62f, paint)
            }
            3 -> {
                paint.color = Color.LTGRAY
                canvas.drawCircle(cx, cy, r*.7f, paint)
                paint.color = Color.WHITE
                canvas.drawLine(cx-r, cy, cx-r*.65f, cy, paint)
                canvas.drawLine(cx+r*.65f, cy, cx+r, cy, paint)
                canvas.drawLine(cx, cy-r, cx, cy-r*.65f, paint)
                canvas.drawLine(cx, cy+r*.65f, cx, cy+r, paint)
            }
            else -> {
                val h = r*.65f
                val l = r*.28f
                canvas.drawLine(cx-h, cy-h, cx-h+l, cy-h, paint)
                canvas.drawLine(cx-h, cy-h, cx-h, cy-h+l, paint)
                canvas.drawLine(cx+h, cy-h, cx+h-l, cy-h, paint)
                canvas.drawLine(cx+h, cy-h, cx+h, cy-h+l, paint)
                canvas.drawLine(cx-h, cy+h, cx-h+l, cy+h, paint)
                canvas.drawLine(cx-h, cy+h, cx-h, cy+h-l, paint)
                canvas.drawLine(cx+h, cy+h, cx+h-l, cy+h, paint)
                canvas.drawLine(cx+h, cy+h, cx+h, cy+h-l, paint)
            }
        }
    }
}
