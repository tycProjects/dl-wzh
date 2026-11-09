package com.mousetuner

import android.content.Context
import android.graphics.*
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** Ảnh minh họa đồ họa vẽ bằng code (không dùng ảnh có bản quyền): một căn phòng kiểu game bắn súng. */
object SceneArt {
    private var base: Bitmap? = null
    private val cache = HashMap<Int, Bitmap>()
    private fun Paint.c(v: Long): Paint { shader = null; color = v.toInt(); return this }

    fun base(): Bitmap = base ?: Bitmap.createBitmap(320, 200, Bitmap.Config.ARGB_8888).also { draw(Canvas(it)); base = it }

    /** 0 = Gốc (nét) · 1 = Low Texture (bề mặt mềm) · 2 = Low Pixel (vỡ hạt). */
    fun variant(mode: Int): Bitmap = cache.getOrPut(mode) {
        val b = base()
        when (mode) {
            1 -> Bitmap.createScaledBitmap(Bitmap.createScaledBitmap(b, 90, 56, true), 320, 200, true)
            2 -> Bitmap.createScaledBitmap(Bitmap.createScaledBitmap(b, 34, 21, false), 320, 200, false)
            else -> b
        }
    }

    private fun draw(c: Canvas) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG); val w = 320f; val h = 200f
        p.shader = LinearGradient(0f, 0f, 0f, h * 0.62f, 0xFFDDD6C4.toInt(), 0xFF9C9A92.toInt(), Shader.TileMode.CLAMP); c.drawRect(0f, 0f, w, h * 0.62f, p)
        c.drawRect(0f, 0f, w, h * 0.07f, p.c(0xFFF2EEE2)); c.drawRect(w * 0.3f, h * 0.07f, w * 0.7f, h * 0.085f, p.c(0xFFFFFFFF))
        c.drawRect(0f, h * 0.40f, w, h * 0.62f, p.c(0xFFB8793B))
        for (i in 0..19) c.drawRect(i * 16f, h * 0.40f, i * 16f + 8f, h * 0.62f, p.c(if (i % 2 == 0) 0xFFA66A30 else 0xFFC4864A))
        c.drawRect(0f, h * 0.40f, w, h * 0.415f, p.c(0xFF6B4120))
        for (x in floatArrayOf(96f, 232f)) { c.drawRect(x, h * 0.07f, x + 26f, h * 0.64f, p.c(0xFF4C4E54)); c.drawRect(x + 3f, h * 0.07f, x + 8f, h * 0.64f, p.c(0xFF7E8189)); c.drawRect(x, h * 0.50f, x + 26f, h * 0.53f, p.c(0xFFE0B030)) }
        p.shader = LinearGradient(0f, h * 0.62f, 0f, h, 0xFFCF8644.toInt(), 0xFF8A4B20.toInt(), Shader.TileMode.CLAMP); c.drawRect(0f, h * 0.62f, w, h, p)
        p.shader = null; p.style = Paint.Style.STROKE; p.strokeWidth = 1.4f; p.color = 0x33000000
        for (i in -8..8) c.drawLine(w / 2f, h * 0.62f, w / 2f + i * 64f, h, p)
        var y = h * 0.66f; var s = 3f; while (y < h) { c.drawLine(0f, y, w, y, p); y += s; s *= 1.28f }
        p.style = Paint.Style.FILL
        c.drawRect(14f, h * 0.45f, 64f, h * 0.66f, p.c(0xFF2F4E8A)); c.drawRect(14f, h * 0.45f, 64f, h * 0.49f, p.c(0xFF4A6FB5)); c.drawRect(38f, h * 0.45f, 42f, h * 0.66f, p.c(0xFF1F3560))
        c.drawRect(252f, h * 0.60f, 306f, h * 0.67f, p.c(0xFF2FD0E0)); c.drawRect(252f, h * 0.60f, 306f, h * 0.615f, p.c(0xFFA8F4FA))
        // nhân vật
        c.drawRect(136f, h * 0.60f, 150f, h * 0.90f, p.c(0xFF2B2B30)); c.drawRect(152f, h * 0.60f, 166f, h * 0.90f, p.c(0xFF1F1F24))
        c.drawRoundRect(RectF(130f, h * 0.36f, 172f, h * 0.64f), 8f, 8f, p.c(0xFF3A3028))
        c.drawRoundRect(RectF(106f, h * 0.42f, 134f, h * 0.52f), 6f, 6f, p.c(0xFFE8631A)); c.drawRoundRect(RectF(92f, h * 0.43f, 112f, h * 0.49f), 4f, 4f, p.c(0xFF9BE03A))
        c.drawCircle(151f, h * 0.30f, 14f, p.c(0xFFD9A77A)); c.drawRect(136f, h * 0.30f, 166f, h * 0.345f, p.c(0xFF55392A))
        c.drawRoundRect(RectF(132f, h * 0.18f, 170f, h * 0.27f), 8f, 8f, p.c(0xFFE8DCC0)); c.drawRect(126f, h * 0.255f, 176f, h * 0.27f, p.c(0xFFC8B890))
        for (x in floatArrayOf(60f, 262f)) { c.drawCircle(x, h * 0.74f, 5f, p.c(0xFF1A1A1E)); c.drawRect(x - 4f, h * 0.62f, x + 4f, h * 0.74f, p.c(0xFF222226)) }
    }
}

/** Khung ảnh minh họa; mode 0/1/2 = chất lượng, sat = độ rực màu. */
class SceneView(c: Context, private val mode: Int, private val sat: Float = 1f) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = mode != 2; if (sat != 1f) colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(sat) }) }
    private val clip = Path()
    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { clip.reset(); clip.addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), context.dp(12).toFloat(), context.dp(12).toFloat(), Path.Direction.CW) }
    override fun onDraw(c: Canvas) { c.save(); c.clipPath(clip); c.drawBitmap(SceneArt.variant(mode), null, Rect(0, 0, width, height), p); c.restore() }
}

/** Hàng 3 ảnh minh họa chọn được (giống phần Rendering Graphics). */
fun Context.thumbRow(labels: List<String>, modes: List<Int>, sats: List<Float>, sel: Int, onPick: (Int) -> Unit): LinearLayout {
    val ctx = this; val r = row(); val cells = ArrayList<LinearLayout>()
    fun paint(s: Int) = cells.forEachIndexed { i, v ->
        v.background = android.graphics.drawable.GradientDrawable().apply { cornerRadius = dp(16).toFloat()
            if (i == s) { setColor(Color.argb(40, Color.red(Theme.acc), Color.green(Theme.acc), Color.blue(Theme.acc))); setStroke(dp(2), Theme.acc) } else { setColor(0x14FFFFFF); setStroke(dp(1), 0x22FFFFFF) } }
        (v.getChildAt(1) as TextView).setTextColor(if (i == s) Theme.acc else Color.WHITE)
    }
    labels.indices.forEach { i ->
        val cell = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(dp(6), dp(6), dp(6), dp(8)); setOnClickListener { paint(i); onPick(i) } }
        cell.addView(SceneView(ctx, modes[i], sats[i]), lpw(-1, dp(84)))
        cell.addView(TextView(ctx).apply { text = labels[i]; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setPadding(0, dp(6), 0, 0) })
        cells.add(cell); r.addView(cell, lpw(0, -2, 1f).apply { if (i > 0) leftMargin = dp(8) })
    }
    paint(sel); return r
}
