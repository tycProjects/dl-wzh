package com.liveisland

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Волнистая полоска перемотки, как в плеере на Pixel.
 * Пройденная часть — бегущая волна, пока музыка играет; на паузе волна плавно распрямляется.
 * Перемотка: потяните ползунок, трек перематывается в момент отпускания.
 */
class WavySeekView(ctx: Context) : View(ctx) {
  private val d = resources.displayMetrics.density
  private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 4f * d
  }
  private val thumb = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
  private val path = Path()
  private var phase = 0f
  private var amp = 0f
  private var phaseAnim: ValueAnimator? = null
  private var ampAnim: ValueAnimator? = null
  private var dragging = false

  /** Положение 0..1 */
  var fraction = 0f
    set(v) { field = v.coerceIn(0f, 1f); invalidate() }
  var activeColor = Color.WHITE
    set(v) { field = v; invalidate() }
  var trackColor = 0x33FFFFFF
    set(v) { field = v; invalidate() }
  /** Играет ли музыка: волна идёт или стоит ровно */
  var playing = false
    set(v) {
      if (field == v) return
      field = v
      animateAmp(if (v) 1f else 0f)
    }
  /** false — полоска только показывает прогресс (зарядка, скачивание), касания не принимает */
  var interactive = true
  /** Вызывается в момент отпускания ползунка */
  var onSeek: ((Float) -> Unit)? = null
  /** true — палец на ползунке, false — отпустили */
  var onSeeking: ((Boolean) -> Unit)? = null

  private fun animateAmp(to: Float) {
    ampAnim?.cancel()
    ampAnim = ValueAnimator.ofFloat(amp, to).apply {
      duration = 350L
      addUpdateListener { amp = it.animatedValue as Float; invalidate() }
      addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(a: Animator) { if (!playing) stopPhase() }
      })
      start()
    }
    if (to > 0f) startPhase()
  }

  private fun startPhase() {
    if (phaseAnim != null) return
    phaseAnim = ValueAnimator.ofFloat(0f, (2.0 * PI).toFloat()).apply {
      duration = 1600L
      repeatCount = ValueAnimator.INFINITE
      interpolator = LinearInterpolator()
      addUpdateListener { phase = it.animatedValue as Float; invalidate() }
      start()
    }
  }

  private fun stopPhase() { phaseAnim?.cancel(); phaseAnim = null }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (playing) startPhase()
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    stopPhase(); ampAnim?.cancel()
  }

  override fun onMeasure(w: Int, h: Int) {
    setMeasuredDimension(resolveSize((120f * d).toInt(), w), resolveSize((22f * d).toInt(), h))
  }

  override fun onDraw(c: Canvas) {
    val l = 6f * d
    val r = width - 6f * d
    val cy = height / 2f
    if (r <= l) return
    val px = l + (r - l) * fraction
    val gap = 7f * d

    // оставшаяся часть — ровная линия
    if (px + gap < r) {
      line.color = trackColor
      c.drawLine(px + gap, cy, r, cy, line)
    }

    // пройденная часть — волна, затухающая у ползунка
    val end = px - gap
    line.color = activeColor
    if (end > l) {
      path.reset()
      val a = amp * 3.2f * d
      val wl = 26f * d
      val step = 2f * d
      var x = l
      path.moveTo(l, cy)
      while (x < end) {
        x = min(x + step, end)
        val fadeOut = min(1f, (end - x) / (14f * d))
        val fadeIn = min(1f, (x - l) / (10f * d))
        val y = cy + a * fadeOut * fadeIn * sin(2f * PI.toFloat() * (x - l) / wl - phase)
        path.lineTo(x, y)
      }
      c.drawPath(path, line)
    }

    // ползунок
    thumb.color = activeColor
    val tw = if (dragging) 6f * d else 4f * d
    c.drawRoundRect(px - tw / 2f, cy - 9f * d, px + tw / 2f, cy + 9f * d, tw / 2f, tw / 2f, thumb)
  }

  override fun onTouchEvent(e: MotionEvent): Boolean {
    if (!interactive) return false
    val l = 6f * d
    val r = width - 6f * d
    fun f(x: Float) = ((x - l) / (r - l)).coerceIn(0f, 1f)
    when (e.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        parent?.requestDisallowInterceptTouchEvent(true)
        dragging = true; onSeeking?.invoke(true)
        fraction = f(e.x)
        return true
      }
      MotionEvent.ACTION_MOVE -> {
        if (dragging) fraction = f(e.x)
        return true
      }
      MotionEvent.ACTION_UP -> {
        if (dragging) {
          dragging = false
          fraction = f(e.x)
          onSeek?.invoke(fraction)
          onSeeking?.invoke(false)
          invalidate()
        }
        return true
      }
      MotionEvent.ACTION_CANCEL -> {
        dragging = false; onSeeking?.invoke(false); invalidate()
        return true
      }
    }
    return super.onTouchEvent(e)
  }
}
