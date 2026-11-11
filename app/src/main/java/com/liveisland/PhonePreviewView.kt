package com.liveisland

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.animation.PathInterpolator
import com.google.android.material.color.MaterialColors
import com.google.android.material.R as M

/** Макет телефона: показывает, где будет стоять остров. Остров плавно «переезжает» при смене настроек. */
class PhonePreviewView(ctx: Context) : View(ctx) {
  private val d = resources.displayMetrics.density
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val rect = RectF()
  private var ax = 0.5f      // 0 — слева, 0.5 — по центру, 1 — справа
  private var ay = 8f        // отступ сверху, dp
  private var sc = 1f        // размер
  private var anim: ValueAnimator? = null

  fun setPosition(x: String, yDp: Int, scale: Float, animate: Boolean = true) {
    val tx = when (x) { "left" -> 0f; "right" -> 1f; else -> 0.5f }
    val fx = ax; val fy = ay; val fs = sc
    anim?.cancel()
    if (!animate) { ax = tx; ay = yDp.toFloat(); sc = scale; invalidate(); return }
    anim = ValueAnimator.ofFloat(0f, 1f).apply {
      duration = 420L; interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
      addUpdateListener {
        val k = it.animatedValue as Float
        ax = fx + (tx - fx) * k; ay = fy + (yDp - fy) * k; sc = fs + (scale - fs) * k
        invalidate()
      }
      start()
    }
  }

  override fun onMeasure(w: Int, h: Int) {
    setMeasuredDimension(resolveSize((150f * d).toInt(), w), resolveSize((300f * d).toInt(), h))
  }

  override fun onDraw(c: Canvas) {
    val w = width.toFloat(); val h = height.toFloat()
    val body = MaterialColors.getColor(this, M.attr.colorOutline, Color.GRAY)
    val wall = MaterialColors.getColor(this, M.attr.colorPrimaryContainer, Color.DKGRAY)
    // корпус
    paint.color = body
    rect.set(0f, 0f, w, h); c.drawRoundRect(rect, 28f * d, 28f * d, paint)
    // экран
    val m = 5f * d
    paint.color = wall
    rect.set(m, m, w - m, h - m); c.drawRoundRect(rect, 24f * d, 24f * d, paint)
    // остров: реальный экран ≈ 360 dp шириной
    val sw = w - 2 * m
    val k = sw / 360f                         // пикселей макета на 1 dp реального экрана
    val pw = 170f * sc * k
    val ph = 36f * sc * k
    val margin = 12f * k
    val left = m + margin + (sw - 2 * margin - pw) * ax
    val top = m + ay * k
    val pal = IslandStyle.palette(context, "music")
    paint.color = pal.bg
    rect.set(left, top, left + pw, top + ph); c.drawRoundRect(rect, ph / 2f, ph / 2f, paint)
    paint.color = pal.badge
    val r = ph * 0.34f
    c.drawCircle(left + ph / 2f, top + ph / 2f, r, paint)
    paint.color = pal.fg
    rect.set(left + ph + 2f * d, top + ph * 0.42f, left + pw - ph * 0.45f, top + ph * 0.58f)
    c.drawRoundRect(rect, ph * 0.08f, ph * 0.08f, paint)
  }
}
