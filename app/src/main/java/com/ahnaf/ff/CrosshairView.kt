package com.ahnaf.ff

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class CrosshairView(ctx: Context) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        strokeWidth = 3f * resources.displayMetrics.density
        style = Paint.Style.FILL
    }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val gap = width * 0.10f
        val len = width * 0.40f
        c.drawLine(cx - len, cy, cx - gap, cy, paint)
        c.drawLine(cx + gap, cy, cx + len, cy, paint)
        c.drawLine(cx, cy - len, cx, cy - gap, paint)
        c.drawLine(cx, cy + gap, cx, cy + len, paint)
        c.drawCircle(cx, cy, paint.strokeWidth, paint)
    }
}
