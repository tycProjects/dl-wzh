package com.liveisland

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.app.PendingIntent
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator

/**
 * Сам остров. Используется в оверлее, в превью, в настройках и в обучении — везде одинаково.
 * Скорость анимаций и размер берутся из настроек.
 */
class IslandView(ctx: Context) : LinearLayout(ctx) {
  private val d = resources.displayMetrics.density
  private val sc = IslandStyle.scale(ctx)
  private fun px(v: Int) = (v * d * sc).toInt()
  private fun ts(v: Float) = v * sc
  private fun dur(ms: Long): Long = (ms * IslandStyle.speed(context)).toLong()
  private val animOn get() = IslandStyle.speed(context) > 0f
  private val emphasized = PathInterpolator(0.2f, 0f, 0f, 1f)
  private val gentle = PathInterpolator(0.4f, 0f, 0.6f, 1f)

  private val bg = GradientDrawable().apply { cornerRadius = 28 * d * sc }
  private val cookie = CookieDrawable(0)
  private val badge = ImageView(ctx)
  private val caption = TextView(ctx)
  private val title = TextView(ctx)
  private val sub = TextView(ctx)
  private val progress = LinearProgressIndicator(ctx)
  private val seek = WavySeekView(ctx)
  private val wave = WaveView(ctx)
  private var artKey = ""
  private val actionsRow = LinearLayout(ctx)
  private val inboxScroll = android.widget.ScrollView(ctx)
  private val inboxBox = LinearLayout(ctx)
  private var inboxCount = 0

  var current: IslandContent? = null; private set
  var isOpen = false; private set
  var onAction: (() -> Unit)? = null
  /** Пользователь перемотал музыку: значение от 0 до 1 */
  var onSeek: ((Float) -> Unit)? = null

  private var bgAnim: ValueAnimator? = null
  private var loop: ValueAnimator? = null
  private var seekAnim: ValueAnimator? = null
  private var curBg = 0
  private var seeking = false

  init {
    orientation = VERTICAL
    background = bg
    badge.background = cookie
    badge.scaleType = ImageView.ScaleType.FIT_CENTER
    caption.apply { textSize = ts(12f); maxLines = 1; ellipsize = TextUtils.TruncateAt.END; visibility = View.GONE }
    title.apply {
      maxLines = 1; ellipsize = TextUtils.TruncateAt.END; textSize = ts(14f)
      typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    sub.apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END; textSize = ts(14f); visibility = View.GONE }
    val col = LinearLayout(ctx).apply {
      orientation = VERTICAL
      addView(caption); addView(title); addView(sub)
    }
    val header = LinearLayout(ctx).apply {
      orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
      addView(badge, LayoutParams(px(28), px(28)))
      addView(col, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = px(10) })
      addView(wave, LayoutParams(px(22), px(16)).apply { marginStart = px(8) })
    }
    progress.apply { visibility = View.GONE; max = 100; trackThickness = px(8); trackCornerRadius = px(4) }
    seek.apply {
      visibility = View.GONE
      onSeek = { f -> this@IslandView.onSeek?.invoke(f) }
      onSeeking = { s -> seeking = s }
    }
    actionsRow.apply { orientation = HORIZONTAL; visibility = View.GONE }

    val mp = LayoutParams.MATCH_PARENT; val wc = LayoutParams.WRAP_CONTENT
    addView(header, LayoutParams(mp, wc))
    addView(progress, LayoutParams(mp, wc).apply { topMargin = px(14) })
    addView(seek, LayoutParams(mp, px(22)).apply { topMargin = px(8) })
    addView(actionsRow, LayoutParams(mp, wc).apply { topMargin = px(14) })
    inboxScroll.apply { visibility = View.GONE; isVerticalScrollBarEnabled = false; addView(inboxBox) }
    addView(inboxScroll, LayoutParams(mp, px(190)).apply { topMargin = px(10) })
  }

  private fun ViewPropertyAnimator.cfg(ms: Long, delay: Long, ip: TimeInterpolator) =
    setDuration(dur(ms)).setStartDelay(dur(delay)).setInterpolator(ip)

  /** Подставляет содержимое. morph=true — плавная смена, когда остров уже на экране. */
  fun bind(c: IslandContent, morph: Boolean) {
    val prev = current; current = c
    val p = IslandStyle.palette(context, c.kind)
    val kindChanged = prev == null || prev.kind != c.kind
    if (morph && kindChanged && animOn) animateBg(p.bg) else { bgAnim?.cancel(); curBg = p.bg; bg.setColor(p.bg) }
    bg.setStroke(px(1), p.outline)
    caption.setTextColor(p.fgVar); title.setTextColor(p.fg); sub.setTextColor(p.fgVar)
    swapText(caption, c.caption, morph); swapText(title, c.title, morph); swapText(sub, c.sub, morph)
    cookie.fill = p.badge
    wave.color = p.badge
    seek.activeColor = p.badge
    seek.trackColor = (p.fg and 0x00FFFFFF) or 0x33000000
    if (c.kind == "download" || c.kind == "charge") wave.playing = true
    if (c.kind == "music") {
      // Нет медиасессии — не знаем, играет ли, поэтому показываем движение волны
      if (c.mediaToken == null) { wave.playing = true; seek.playing = true }
      if (artKey.isEmpty()) {
        badge.setImageDrawable(ContextCompat.getDrawable(context, IslandStyle.icon("music")))
        badge.setColorFilter(p.onBadge)
      }
    } else {
      artKey = ""
      badge.setImageDrawable(c.icon ?: ContextCompat.getDrawable(context, IslandStyle.icon(c.kind)))
      badge.setColorFilter(p.onBadge)
    }

    progress.setIndicatorColor(p.badge)
    progress.trackColor = (p.onBadge and 0x00FFFFFF) or 0x33000000
    val tint = ColorStateList.valueOf(p.badge)

    if (c.progress >= 0 && (c.kind == "charge" || c.kind == "download")) progress.setProgressCompat(c.progress, animOn && morph)
    if (c.kind == "music" && c.progress >= 0) setMusicFraction(c.progress / 100f, morph)

    actionsRow.removeAllViews()
    c.actions.forEachIndexed { i, a ->
      val icRes = iconFor(a.label)
      val isAnswer = icRes == R.drawable.ic_call
      val isDecline = icRes == R.drawable.ic_decline
      val bgColor = when {
        isAnswer -> 0xFF7FD9A6.toInt()
        isDecline -> 0xFFFF8A8A.toInt()
        else -> p.badge
      }
      val fg = if (isAnswer || isDecline) 0xFF0B0B0F.toInt() else p.onBadge
      val b = MaterialButton(context).apply {
        contentDescription = a.label
        if (icRes != null) {
          text = ""
          icon = ContextCompat.getDrawable(context, icRes)
          iconTint = ColorStateList.valueOf(fg)
          iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
          iconPadding = 0
          iconSize = px(22)
        } else {
          text = a.label; textSize = ts(14f); setTextColor(fg)
        }
        maxLines = 1; insetTop = 0; insetBottom = 0; cornerRadius = px(20)
        backgroundTintList = ColorStateList.valueOf(bgColor)
        setOnClickListener {
          val pi = a.intent ?: c.openIntent
          try { pi?.send() } catch (_: PendingIntent.CanceledException) {
            try { c.openIntent?.send() } catch (_: PendingIntent.CanceledException) {}
          }
          onAction?.invoke()
        }
      }
      actionsRow.addView(b, LayoutParams(0, px(44), 1f).apply { if (i > 0) marginStart = px(8) })
    }
    if (morph && kindChanged) startLoop()
    applyState()
  }

  /**
   * Состояние музыки: играет ли (волна) и обложка трека.
   * key — название+исполнитель+длительность; обложку переставляем только при смене трека.
   */
  fun setMusicState(playing: Boolean, key: String, art: Bitmap?) {
    wave.playing = playing
    seek.playing = playing
    if (key == artKey) return
    artKey = key
    if (art == null) {
      badge.setImageDrawable(ContextCompat.getDrawable(context, IslandStyle.icon("music")))
      badge.setColorFilter(IslandStyle.palette(context, "music").onBadge)
      if (animOn) startLoop()
    } else {
      loop?.cancel(); loop = null
      badge.setImageDrawable(RoundedBitmapDrawableFactory.create(resources, art).apply { isCircular = true })
      badge.clearColorFilter()
    }
  }

  /** Положение полоски музыки (0..1). Пока палец на полоске — не трогаем. */
  fun setMusicFraction(f: Float, animate: Boolean = true) {
    if (seeking) return
    val target = f.coerceIn(0f, 1f)
    seekAnim?.cancel()
    if (!animate || !animOn) { seek.fraction = target; return }
    seekAnim = ValueAnimator.ofFloat(seek.fraction, target).apply {
      duration = dur(300L); interpolator = LinearInterpolator()
      addUpdateListener { seek.fraction = it.animatedValue as Float }
      start()
    }
  }

  /** Новый текст выезжает снизу, старый уходит вверх */
  private fun swapText(tv: TextView, t: String, morph: Boolean) {
    if (!morph || !animOn || tv.text.toString() == t || tv.visibility != View.VISIBLE) { tv.text = t; return }
    tv.animate().cancel()
    tv.animate().alpha(0f).translationY(-px(6).toFloat()).cfg(120L, 0L, LinearInterpolator()).withEndAction {
      tv.text = t; tv.translationY = px(6).toFloat()
      tv.animate().alpha(1f).translationY(0f).cfg(240L, 0L, DecelerateInterpolator()).start()
    }.start()
  }

  private fun animateBg(to: Int) {
    bgAnim?.cancel()
    bgAnim = ValueAnimator.ofArgb(curBg, to).apply {
      duration = dur(340L); interpolator = emphasized
      addUpdateListener { curBg = it.animatedValue as Int; bg.setColor(curBg) }
      start()
    }
  }

  fun setOpen(o: Boolean) { isOpen = o; applyState() }

  private fun applyState() {
    val c = current ?: return
    val bs = px(if (isOpen) 44 else 28)
    val blp = badge.layoutParams as LayoutParams
    if (blp.width != bs) { blp.width = bs; blp.height = bs; badge.layoutParams = blp }
    val ip = px(if (isOpen) 10 else 6); badge.setPadding(ip, ip, ip, ip)
    title.textSize = ts(if (isOpen) 16f else 14f)
    fun vis(v: View, show: Boolean) {
      val t = if (show) View.VISIBLE else View.GONE
      if (v.visibility != t) {
        v.visibility = t
        if (!show) { v.animate().cancel(); v.alpha = 1f; v.translationY = 0f }
      }
    }
    vis(caption, isOpen && c.caption.isNotBlank())
    vis(sub, isOpen && c.sub.isNotBlank())
    vis(progress, isOpen && (c.kind == "charge" || c.kind == "download") && c.progress >= 0)
    vis(seek, c.kind == "music" && (c.mediaToken != null || c.progress >= 0))
    vis(wave, c.kind == "music" || c.kind == "download" || c.kind == "charge")
    vis(actionsRow, isOpen && c.actions.isNotEmpty())
    inboxScroll.visibility = if (isOpen && inboxCount > 1) View.VISIBLE else View.GONE
    val hp = px(if (isOpen) 16 else 8); val vp = px(if (isOpen) 16 else 6)
    setPadding(hp, vp, if (isOpen) hp else px(16), vp)
  }

  /** Список сообщений: раскрываем остров и листаем вниз. Нажатие на строку открывает сообщение. */
  fun setInbox(items: List<IslandContent>, onOpen: (IslandContent) -> Unit) {
    inboxCount = items.size
    inboxBox.removeAllViews()
    val p = IslandStyle.palette(context, "notify")
    items.asReversed().forEach { msg ->
      val row = LinearLayout(context).apply {
        orientation = VERTICAL
        setPadding(px(12), px(8), px(12), px(8))
        background = GradientDrawable().apply { cornerRadius = px(14).toFloat(); setColor((p.fg and 0x00FFFFFF) or 0x22000000) }
        setOnClickListener { onOpen(msg) }
      }
      row.addView(TextView(context).apply {
        text = msg.title; setTextColor(p.fg); textSize = ts(14f); maxLines = 1
        ellipsize = TextUtils.TruncateAt.END; typeface = Typeface.DEFAULT_BOLD
      })
      row.addView(TextView(context).apply {
        text = msg.sub; setTextColor(p.fgVar); textSize = ts(13f); maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
      })
      inboxBox.addView(row, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { bottomMargin = px(6) })
    }
    applyState()
  }

  /** Сбрасываем всё, когда остров убирают: иначе полоска и волна «остаются» */
  fun reset() {
    wave.playing = false; seek.playing = false
    seek.visibility = View.GONE
    artKey = ""
    loop?.cancel(); loop = null
    current = null
    inboxCount = 0
  }

  /** Текст кнопки → значок. Если значка нет, кнопка остаётся с текстом. */
  private fun iconFor(label: String): Int? {
    val l = label.lowercase()
    fun has(vararg k: String) = k.any { l.contains(it) }
    return when {
      has("ответ", "answer", "accept", "трубк") -> R.drawable.ic_call
      has("отклон", "сброс", "decline", "reject", "заверш", "hang") -> R.drawable.ic_decline
      has("пауз", "pause") -> R.drawable.ic_pause
      has("играть", "play", "продолж", "resume") -> R.drawable.ic_play
      has("далее", "next", "след", "skip") -> R.drawable.ic_next
      has("таймер", "timer") -> R.drawable.ic_timer
      has("будильник", "alarm") -> R.drawable.ic_alarm
      has("диктофон", "запись", "record", "mic") -> R.drawable.ic_mic
      else -> null
    }
  }

  /** Целевой размер окна: [ширина, высота] */
  fun measureTarget(maxW: Int, open: Boolean): IntArray {
    setOpen(open)
    val unspec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
    return if (open) {
      val w = minOf(px(360), maxW)
      measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), unspec)
      intArrayOf(w, measuredHeight)
    } else {
      measure(MeasureSpec.makeMeasureSpec(minOf(px(280), maxW), MeasureSpec.AT_MOST), unspec)
      intArrayOf(maxOf(measuredWidth, px(120)), measuredHeight)
    }
  }

  // ---------- анимации ----------

  /** Появление: фигурка выпрыгивает с поворотом, строки выезжают по очереди */
  fun play() {
    if (current == null) return
    stopLoops()
    if (!animOn) { badge.alpha = 1f; return }
    badge.animate().cancel()
    badge.scaleX = 0.2f; badge.scaleY = 0.2f; badge.alpha = 0f
    spinCookie(-140f, 0f, dur(620L))
    badge.animate().scaleX(1f).scaleY(1f).alpha(1f)
      .setStartDelay(dur(60L)).setDuration(dur(520L)).setInterpolator(OvershootInterpolator(1.4f))
      .withEndAction { startLoop() }.start()
    stagger(listOf(caption, title, sub, seek, progress, actionsRow), 120L)
    fillProgress(dur(380L))
  }

  /** Раскрытие: дополнительные части появляются каскадом */
  fun playExpand() {
    if (!animOn) return
    stagger(listOf(caption, sub, seek, progress, actionsRow), 80L)
    fillProgress(dur(260L))
  }

  private fun stagger(views: List<View>, firstMs: Long) {
    views.filter { it.visibility == View.VISIBLE }.forEachIndexed { i, v ->
      v.animate().cancel(); v.alpha = 0f; v.translationY = px(8).toFloat()
      v.animate().alpha(1f).translationY(0f)
        .setDuration(dur(280L)).setStartDelay(dur(firstMs + 50L * i)).setInterpolator(emphasized).start()
    }
  }

  private fun fillProgress(delay: Long) {
    val c = current ?: return
    if (c.kind != "charge" || c.progress < 0) return
    progress.progress = 0
    postDelayed({ progress.setProgressCompat(c.progress, animOn) }, delay)
  }

  private fun spinCookie(from: Float, to: Float, ms: Long) {
    ValueAnimator.ofFloat(from, to).apply {
      duration = ms; interpolator = emphasized
      addUpdateListener { cookie.rotation = it.animatedValue as Float }
      start()
    }
  }

  /** Постоянное движение: музыка — вращение, звонок — пульс, зарядка — мягкое мерцание */
  private fun startLoop() {
    loop?.cancel(); loop = null
    badge.scaleX = 1f; badge.scaleY = 1f; badge.alpha = 1f; cookie.rotation = 0f
    if (!animOn) return
    when (current?.kind) {
      "music" -> loop = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = dur(9000L); repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
        addUpdateListener { cookie.rotation = it.animatedValue as Float }
        start()
      }
      "call" -> loop = ValueAnimator.ofFloat(1f, 1.12f).apply {
        duration = dur(700L); repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE
        interpolator = gentle
        addUpdateListener { val v = it.animatedValue as Float; badge.scaleX = v; badge.scaleY = v }
        start()
      }
      "charge" -> loop = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = dur(1400L); repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE
        interpolator = gentle
        addUpdateListener {
          val v = it.animatedValue as Float
          badge.alpha = 0.55f + 0.45f * v
          badge.scaleX = 1f + 0.06f * v; badge.scaleY = badge.scaleX
        }
        start()
      }
    }
  }

  fun stopLoops() {
    loop?.cancel(); loop = null
    badge.animate().cancel()
    badge.scaleX = 1f; badge.scaleY = 1f; badge.alpha = 1f; cookie.rotation = 0f
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    stopLoops(); bgAnim?.cancel(); seekAnim?.cancel()
  }
}
