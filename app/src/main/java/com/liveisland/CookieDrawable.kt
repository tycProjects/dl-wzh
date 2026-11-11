package com.liveisland

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Фигурка «печенье» из Material 3 Expressive: круг с мягкими волнистыми краями. */
class CookieDrawable(initial: Int, private val lobes: Int = 9, private val amp: Float = 0.07f) : Drawable() {
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = initial }
  private val path = Path()

  var fill: Int
    get() = paint.color
    set(v) { paint.color = v; invalidateSelf() }

  override fun onBoundsChange(b: Rect) {
    path.reset()
    val cx = b.exactCenterX(); val cy = b.exactCenterY()
    val r = min(b.width(), b.height()) / 2f / (1f + amp)
    val steps = 180
    for (i in 0..steps) {
      val t = (2.0 * PI * i / steps).toFloat()
      val rr = r * (1f + amp * cos(lobes * t))
      val x = cx + rr * cos(t); val y = cy + rr * sin(t)
      if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
  }

  /** Угол поворота в градусах — «печенье» крутится, пока играет музыка */
  var rotation: Float = 0f
    set(v) { field = v; invalidateSelf() }

  override fun draw(c: Canvas) {
    val b = bounds
    c.save(); c.rotate(rotation, b.exactCenterX(), b.exactCenterY())
    c.drawPath(path, paint)
    c.restore()
  }
  override fun setAlpha(a: Int) { paint.alpha = a; invalidateSelf() }
  override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf; invalidateSelf() }
  @Suppress("OVERRIDE_DEPRECATION")
  override fun getOpacity() = PixelFormat.TRANSLUCENT
}
