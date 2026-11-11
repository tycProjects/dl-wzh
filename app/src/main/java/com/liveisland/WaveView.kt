package com.liveisland

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.sin

/** Волна эквалайзера, как в «Сейчас играет» на Pixel. Двигается, пока играет музыка. */
class WaveView(ctx: Context) : View(ctx) {
  private val bars = 5
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
  private var phase = 0f
  private var anim: ValueAnimator? = null

  var color: Int
    get() = paint.color
    set(v) { paint.color = v; invalidate() }

  /** Реальные уровни звука (0..1) по полосам; null — обычная анимация */
  var levels: FloatArray? = null
    set(v) { field = v; invalidate() }
  var playing: Boolean = false
    set(v) {
      if (field == v) return
      field = v
      if (v) startAnim() else stopAnim()
      invalidate()
    }

  private fun startAnim() {
    if (anim != null) return
    anim = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
      duration = 1100L
      repeatCount = ValueAnimator.INFINITE
      interpolator = LinearInterpolator()
      addUpdateListener { phase = it.animatedValue as Float; invalidate() }
      start()
    }
  }

  private fun stopAnim() {
    anim?.cancel(); anim = null
    phase = 0f
  }

  override fun onDraw(c: Canvas) {
    val w = width.toFloat(); val h = height.toFloat()
    if (w <= 0f || h <= 0f) return
    val gap = w * 0.14f
    val bw = (w - gap * (bars - 1)) / bars
    for (i in 0 until bars) {
      val lv = levels
      val k = if (lv != null && playing && lv.isNotEmpty()) 0.2f + 0.8f * lv[i % lv.size].coerceIn(0f, 1f)
              else if (playing) 0.35f + 0.65f * ((sin(phase + i * 1.3f) + 1f) / 2f) else 0.3f
      val bh = h * k
      val x = i * (bw + gap)
      val top = (h - bh) / 2f
      c.drawRoundRect(x, top, x + bw, top + bh, bw / 2f, bw / 2f, paint)
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    stopAnim()
  }
}
