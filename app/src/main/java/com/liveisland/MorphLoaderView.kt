package com.liveisland

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/** Загрузчик: синяя фигура плавно меняет форму, пока идёт загрузка. */
class MorphLoaderView(ctx: Context) : View(ctx) {
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0B57D0.toInt() }
  private val path = Path()
  private val points = 120
  private var phase = 0f
  private var anim: ValueAnimator? = null

  /** Радиус фигуры в точке угла theta для формы номер s (0..3) */
  private fun radius(s: Int, theta: Double): Double = when (s) {
    0 -> 1.0
    1 -> 1.0 + 0.12 * cos(6 * theta)
    2 -> 1.0 + 0.10 * cos(9 * theta)
    else -> 1.05 / (abs(cos(theta)).pow(4.0) + abs(sin(theta)).pow(4.0)).pow(0.25)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    anim = ValueAnimator.ofFloat(0f, 4f).apply {
      duration = 2400L
      repeatCount = ValueAnimator.INFINITE
      interpolator = LinearInterpolator()
      addUpdateListener { phase = it.animatedValue as Float; invalidate() }
      start()
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    anim?.cancel(); anim = null
  }

  override fun onDraw(c: Canvas) {
    val w = width.toFloat(); val h = height.toFloat()
    if (w <= 0f || h <= 0f) return
    val base = minOf(w, h) * 0.36f
    val cx = w / 2f; val cy = h / 2f
    val s = phase.toInt() % 4
    val next = (s + 1) % 4
    val k = phase - phase.toInt()
    val t = if (k < 0.5f) 2 * k * k else 1 - (-2 * k + 2) * (-2 * k + 2) / 2   // плавный переход
    path.reset()
    for (i in 0..points) {
      val theta = 2 * Math.PI * i / points
      val r = radius(s, theta) * (1 - t) + radius(next, theta) * t
      val x = cx + (base * r * cos(theta)).toFloat()
      val y = cy + (base * r * sin(theta)).toFloat()
      if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    c.drawPath(path, paint)
  }
}
