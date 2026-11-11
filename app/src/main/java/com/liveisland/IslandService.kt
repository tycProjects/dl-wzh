package com.liveisland

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.app.PendingIntent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.google.android.material.color.DynamicColors
import kotlin.math.abs
import android.media.AudioManager
import android.hardware.camera2.CameraManager
import android.nfc.NfcAdapter

/**
 * Окно-оверлей. Остров виден только когда есть что показать.
 *
 * Приоритет постоянных событий: звонок > скачивание > таймер > музыка.
 * Они хранятся отдельно, поэтому музыка не стирает таймер, а таймер не стирает музыку.
 * Сообщения и зарядка показываются на несколько секунд поверх них и возвращают прежнее состояние.
 * Когда показывать нечего — остров плавно исчезает и убирается с экрана.
 */
class IslandService : Service() {
  companion object {
    var instance: IslandService? = null
    @Volatile var pendingDemo = false
    fun start(c: Context) {
      val i = Intent(c, IslandService::class.java)
      if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i) else c.startService(i)
    }
  }

  private lateinit var wm: WindowManager
  private lateinit var ui: Context
  private lateinit var island: IslandView
  private lateinit var lp: WindowManager.LayoutParams
  private var spring: SpringAnimation? = null
  private var attached = false
  private var hiding = false
  private var expanded = false

  private val live = LinkedHashMap<String, IslandContent>()   // постоянные: call, download, timer, music
  private val order = listOf("call", "record", "download", "music", "timer", "charge")
  private var temp: IslandContent? = null                       // сообщение / зарядка

  private val h = Handler(Looper.getMainLooper())
  private val tempTimeout = Runnable { temp = null; refresh() }
  private val dp get() = resources.displayMetrics.density
  private fun px(v: Int) = (v * dp).toInt()
  private fun dur(ms: Long): Long = (ms * IslandStyle.speed(this)).toLong()

  /** Smart Priority: порядок важности. Auto Switch: при нескольких событиях главная капсула меняется по кругу. */
  private var rot = 0
  private fun top(): IslandContent? {
    val lives = order.mapNotNull { live[it] }
    if (lives.isEmpty()) return null
    if (lives.size == 1 || !IslandStyle.flag(this, "auto_switch")) return lives.first()
    return lives[rot % lives.size]
  }
  private val rotate = object : Runnable {
    override fun run() {
      rot++
      if (live.size > 1 && temp == null && ::island.isInitialized) refresh()
      h.postDelayed(this, 5000L)
    }
  }

  /** App Switch: последние приложения (нужен доступ «Статистика использования») */
  private fun recentApps(): List<Pair<String, String>> {
    if (!IslandStyle.appSwitch(this)) return emptyList()
    return try {
      val usm = getSystemService(USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
      val now = System.currentTimeMillis()
      val stats = usm.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, now - 86_400_000L, now) ?: return emptyList()
      stats.filter { it.lastTimeUsed > 0 && it.packageName != packageName && packageManager.getLaunchIntentForPackage(it.packageName) != null }
        .sortedByDescending { it.lastTimeUsed }.take(3)
        .map { it.packageName to packageManager.getApplicationLabel(packageManager.getApplicationInfo(it.packageName, 0)).toString() }
    } catch (_: Exception) { emptyList() }
  }

  /** Reply Island: окно на время ответа становится фокусируемым, чтобы принять клавиатуру */
  private var pendingReply: Pair<IslandContent, IslandAction>? = null
  private fun beginReply(msg: IslandContent, act: IslandAction) {
    pendingReply = msg to act
    lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
    try { wm.updateViewLayout(island, lp) } catch (_: Exception) {}
    island.showReply(getString(R.string.reply_hint), { text -> sendReply(text) }, { endReply() })
  }
  private fun sendReply(text: String) {
    val (msg, act) = pendingReply ?: return
    val ri = act.remote ?: return
    val intent = android.content.Intent()
    val bundle = android.os.Bundle()
    ri.forEach { bundle.putCharSequence(it.resultKey, text) }
    android.app.RemoteInput.addResultsToIntent(ri, intent, bundle)
    try { act.intent?.send(this, 0, intent) } catch (_: Exception) {}
    inbox.removeAll { it.key == msg.key }
    endReply()
  }
  private fun endReply() {
    pendingReply = null
    lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    try { wm.updateViewLayout(island, lp) } catch (_: Exception) {}
    island.hideReply()
    refresh()
  }
  private fun shown(): IslandContent? = temp ?: top() ?: timeContent()

  override fun onBind(i: Intent?) = null

  override fun onCreate() {
    super.onCreate()
    val ch = "island"
    val nm = getSystemService(NotificationManager::class.java)
    nm.createNotificationChannel(NotificationChannel(ch, "Live Island", NotificationManager.IMPORTANCE_MIN))
    val notif = Notification.Builder(this, ch).setContentTitle(getString(R.string.notif_running))
      .setSmallIcon(R.drawable.ic_stat_island).build()
    if (Build.VERSION.SDK_INT >= 34) startForeground(1, notif, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    else startForeground(1, notif)
    if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
    wm = getSystemService(WINDOW_SERVICE) as WindowManager
    buildView()
    instance = this   // считаем сервис запущенным только когда окно острова готово
    ContextCompat.registerReceiver(this, battery, IntentFilter().apply {
      addAction(Intent.ACTION_POWER_CONNECTED); addAction(Intent.ACTION_POWER_DISCONNECTED)
      addAction(Intent.ACTION_BATTERY_CHANGED) },
      ContextCompat.RECEIVER_EXPORTED)
    ContextCompat.registerReceiver(this, ringerReceiver, IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION),
      ContextCompat.RECEIVER_EXPORTED)
    try { (getSystemService(CAMERA_SERVICE) as CameraManager).registerTorchCallback(torchCallback, h) } catch (_: Exception) {}
    ContextCompat.registerReceiver(this, nfcReceiver, IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED),
      ContextCompat.RECEIVER_EXPORTED)
    h.post(clock)
    h.post(rotate)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (pendingDemo && ::island.isInitialized) { pendingDemo = false; h.postDelayed({ demo() }, 600L) }
    return START_STICKY
  }

  // ---------- положение из настроек ----------

  private fun gravityFlags(): Int = Gravity.TOP or when (IslandStyle.posX(this)) {
    "left" -> Gravity.START
    "right" -> Gravity.END
    else -> Gravity.CENTER_HORIZONTAL
  }
  private fun baseX(): Int = if (IslandStyle.posX(this) == "center") 0 else px(12)
  private fun baseY(): Int = px(IslandStyle.posY(this))

  private fun buildView() {
    ui = DynamicColors.wrapContextIfAvailable(ContextThemeWrapper(this, R.style.Theme_LiveIsland))
    island = IslandView(ui)
    island.onAction = { if (island.current?.kind != "time") dismissAll() }
    island.setOnClickListener { if (island.current != null) { expanded = !expanded; relayout(false) } }
    installSwipe()
    island.onSeek = { f -> seekMusic(f) }
    island.onMedia = { label -> mediaButton(label) }
    island.onReply = { msg, act -> beginReply(msg, act) }
    lp = WindowManager.LayoutParams(px(56), px(28),
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
      PixelFormat.TRANSLUCENT).apply {
      gravity = gravityFlags(); x = baseX(); y = baseY()
      if (Build.VERSION.SDK_INT >= 28) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
  }

  /** Положение изменили в настройках — двигаем остров на месте, без пересборки */
  fun applyPosition() {
    h.post {
      if (!::island.isInitialized) return@post
      lp.gravity = gravityFlags(); lp.x = baseX(); lp.y = baseY()
      if (attached) try { wm.updateViewLayout(island, lp) } catch (_: Exception) {}
    }
  }

  /** Размер или тема изменились — пересобираем остров */
  fun rebuild() { h.post { rebuildNow() } }

  private fun rebuildNow() {
    if (!::island.isInitialized) return
    val was = attached
    spring?.cancel(); island.animate().cancel(); island.stopLoops()
    if (was) try { wm.removeView(island) } catch (_: Exception) {}
    attached = false; hiding = false
    buildView()
    if (was) refresh()
  }

  // ---------- публичное API для NotifListener ----------

  private fun allowed(kind: String): Boolean = when (kind) {
    "notify" -> IslandStyle.flag(this, "show_msg")
    "charge" -> IslandStyle.flag(this, "show_charge")
    "timer" -> IslandStyle.flag(this, "show_timer")
    "music" -> IslandStyle.flag(this, "show_music")
    else -> true
  }

  fun submit(c: IslandContent, expand: Boolean? = null, hideAfterMs: Long? = null, force: Boolean = false) {
    h.post {
      if (!::island.isInitialized) return@post
      if (!force && !allowed(c.kind)) return@post
      when (c.kind) {
        "call", "download", "timer", "charge", "music", "record" -> {
          val old = live[c.kind]
          if (old != null && old.key == c.key && old.sig == c.sig) return@post
          live[c.kind] = c
          if (c.kind == "call") { temp = null; h.removeCallbacks(tempTimeout) }
          if (c.kind == "music") attachMusic(c.mediaToken)
        }
        else -> {
          if (live.containsKey("call")) return@post              // во время звонка не отвлекаем
          if (temp?.key == c.key && temp?.sig == c.sig) return@post
          addInbox(c)
          temp = c; h.removeCallbacks(tempTimeout)
          h.postDelayed(tempTimeout, hideAfterMs ?: (IslandStyle.hideSec(this) * 1000L))
        }
      }
      refresh(expand, true)
    }
  }

  /** Прогресс скачивания обновления: остров с волной и полоской */
  fun showDownload(pct: Int, title: String, sub: String) {
    submit(IslandContent("download", "download", title, sub, caption = "LiveIsland", progress = pct), true)
  }

  /** Скрыть прогресс скачивания */
  fun hideDownload() {
    h.post { if (live.remove("download") != null && temp == null) refresh() }
  }

  /** Уведомление исчезло (звонок завершён, плеер закрыт, таймер остановлен) */
  fun removeKey(key: String) {
    h.post {
      val gone = live.entries.filter { it.value.key == key }.map { it.key }
      if (gone.isEmpty()) return@post
      gone.forEach { live.remove(it) }
      if ("music" in gone) { musicCtl = null; lastKey = ""; lastArt = null; stopTick() }
      if (temp == null) refresh()
    }
  }

  fun demo() {
    submit(IslandContent("demo", "notify", getString(R.string.demo_title), getString(R.string.demo_sub),
      caption = getString(R.string.demo_caption),
      actions = listOf(IslandAction(getString(R.string.action_open), null))), true, 7000L, true)
  }

  private fun dismissAll() {
    h.post {
      h.removeCallbacks(tempTimeout); temp = null; live.clear(); inbox.clear()
      musicCtl = null; stopTick()
      hideIsland()
    }
  }

  // ---------- показ / скрытие ----------

  private fun refresh(expand: Boolean? = null, buzz: Boolean = false) {
    if (!::island.isInitialized) return   // окна нет (нет разрешения) — нечего обновлять
    val c = shown()
    if (c?.kind == "music") startTick() else stopTick()
    if (c == null) { hideIsland(); return }
    val wasShowing = attached && !hiding
    val prev = island.current
    expanded = when {
      expand != null -> expand
      wasShowing && prev?.key == c.key -> expanded
      else -> c.kind == "call" || c.kind == "charge" || c.kind == "timer" || c.kind == "download" || c.kind == "time" || c.kind == "record"
    }
    if (hiding) { hiding = false; island.animate().cancel() }
    if (!attached) attach()
    island.setInbox(inbox.toList()) { m -> openMessage(m) }
    island.setCircles(buildCircles())
    island.bind(withMediaActions(c), wasShowing)
    // свёрнутая загрузка: процент прямо в заголовке
    if (c.kind == "download" && c.progress in 0..99) island.setCompactTitle("${c.progress}%")
    else island.setCompactTitle(null)
    val tm = live["timer"]
    if (c.kind == "music" && tm != null) {
      val tp = IslandStyle.palette(this, "timer")
      island.setSecondary(tm.sub.ifBlank { tm.title }, tp.badge, tp.onBadge)
    } else island.setSecondary(null, 0, 0)
    relayout(!wasShowing)
    if (buzz && c.kind != "music" && (!wasShowing || prev?.key != c.key)) vibrate()
  }

  private fun attach() {
    lp.width = px(56); lp.height = px(28)
    lp.gravity = gravityFlags(); lp.x = baseX(); lp.y = baseY()
    island.alpha = 0f; island.translationY = -px(10).toFloat()
    try { wm.addView(island, lp); attached = true } catch (_: Exception) {}
  }

  private fun relayout(enter: Boolean) {
    val maxW = resources.displayMetrics.widthPixels - px(24)
    val wasOpen = island.isOpen
    val t = island.measureTarget(maxW, expanded)
    animateWindow(t[0], t[1])
    if (enter) {
      island.animate().cancel()
      island.animate().alpha(1f).translationY(0f).setStartDelay(0L).setDuration(dur(260L))
        .setInterpolator(DecelerateInterpolator()).start()
      island.play()
    } else if (expanded && !wasOpen) island.playExpand()
  }

  private fun hideIsland() {
    if (!attached || hiding) return
    hiding = true; expanded = false
    val v = island
    v.stopLoops()
    animateWindow(px(56), px(28))
    v.animate().alpha(0f).translationY(-px(10).toFloat()).setStartDelay(0L).setDuration(dur(240L))
      .setInterpolator(AccelerateInterpolator()).withEndAction {
        if (island === v) {
          spring?.cancel()
          island.reset()
          try { wm.removeView(v) } catch (_: Exception) {}
          attached = false; hiding = false
        }
      }.start()
  }

  /**
   * Размер окна меняется на пружине: движение плавное, физичное и прерываемое.
   * Раскрытие — с лёгким отскоком, сворачивание — без отскока.
   */
  private fun animateWindow(w: Int, hh: Int) {
    if (!attached) return
    spring?.cancel()
    val sp = IslandStyle.speed(this)
    if (sp <= 0f) {
      lp.width = w; lp.height = hh
      try { wm.updateViewLayout(island, lp) } catch (_: Exception) {}
      return
    }
    val sw = lp.width; val sh = lp.height
    val grow = w.toLong() * hh > sw.toLong() * sh
    island.setLayerType(View.LAYER_TYPE_HARDWARE, null)   // аппаратный слой — рисуется плавнее
    val force = SpringForce(1000f)
      .setDampingRatio(if (grow) 0.74f else 1f)
      .setStiffness(380f / (sp * sp))
    val s = SpringAnimation(FloatValueHolder(0f))
    s.setSpring(force)
    s.setMinimumVisibleChange(1f)
    s.addUpdateListener { _, value, _ ->
      val f = value / 1000f
      lp.width = (sw + (w - sw) * f).toInt().coerceAtLeast(1)
      lp.height = (sh + (hh - sh) * f).toInt().coerceAtLeast(1)
      try { wm.updateViewLayout(island, lp) } catch (_: Exception) {}
    }
    s.addEndListener { _, canceled, _, _ ->
      if (!canceled) {
        lp.width = w; lp.height = hh
        try { wm.updateViewLayout(island, lp) } catch (_: Exception) {}
        island.setLayerType(View.LAYER_TYPE_NONE, null)
      }
    }
    spring = s
    s.start()
  }

  // ---------- свайп вверх и долгое нажатие — убрать остров ----------
  private var dragStartY = 0f
  private var dragBaseY = 0
  private var dragging = false
  private val slop by lazy { ViewConfiguration.get(this).scaledTouchSlop.toFloat() }
  private val longPress = Runnable { if (!dragging) { vibrate(); dismissAll() } }

  private fun installSwipe() {
    island.setOnTouchListener { v, e ->
      when (e.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          dragStartY = e.rawY; dragBaseY = lp.y; dragging = false
          h.postDelayed(longPress, 600L); true
        }
        MotionEvent.ACTION_MOVE -> {
          val dy = e.rawY - dragStartY
          if (!dragging && abs(dy) > slop) { dragging = true; h.removeCallbacks(longPress) }
          if (dragging) {
            val up = minOf(0f, dy)
            lp.y = dragBaseY + up.toInt()
            island.alpha = (1f + up / px(120)).coerceIn(0.2f, 1f)
            try { wm.updateViewLayout(island, lp) } catch (_: Exception) {}
          }
          true
        }
        MotionEvent.ACTION_UP -> {
          h.removeCallbacks(longPress)
          if (dragging) {
            if (e.rawY - dragStartY < -px(40)) dismissAll() else springBack()
          } else v.performClick()
          true
        }
        MotionEvent.ACTION_CANCEL -> { h.removeCallbacks(longPress); if (dragging) springBack(); true }
        else -> true
      }
    }
  }

  /** Возвращает остров на место, если свайп был недостаточным */
  private fun springBack() {
    val from = lp.y; val to = baseY()
    ValueAnimator.ofFloat(0f, 1f).apply {
      duration = 320L; interpolator = OvershootInterpolator(1.1f)
      addUpdateListener {
        lp.y = (from + (to - from) * (it.animatedValue as Float)).toInt()
        try { wm.updateViewLayout(island, lp) } catch (_: Exception) {}
      }
      start()
    }
    island.animate().alpha(1f).setStartDelay(0L).setDuration(200L).start()
  }

  /** Перекрасить остров после смены темы */
  fun restyle() {
    h.post {
      val c = shown()
      if (c != null && attached) island.bind(c, false)
    }
  }

  // ---------- музыка: обложка, волна и перемотка ----------
  private var musicCtl: MediaController? = null
  private var ticking = false
  private val tick = object : Runnable {
    override fun run() {
      if (!ticking) return
      updateMusic()
      h.postDelayed(this, 300L)
    }
  }

  private fun attachMusic(token: MediaSession.Token?) {
    musicCtl = token?.let { try { MediaController(this, it) } catch (_: Exception) { null } }
    lastKey = ""
  }

  private fun startTick() { if (!ticking) { ticking = true; h.post(tick) } }
  private fun stopTick() { ticking = false; h.removeCallbacks(tick) }

  private var lastKey = ""
  private var lastArt: android.graphics.Bitmap? = null

  private fun updateMusic() {
    if (island.current?.kind != "music") return
    val c = musicCtl ?: return
    val st = c.playbackState
    val md = c.metadata
    val len = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
    val key = "${md?.getString(MediaMetadata.METADATA_KEY_TITLE)}|${md?.getString(MediaMetadata.METADATA_KEY_ARTIST)}|$len"
    if (key != lastKey) {                       // обложку читаем только при смене трека
      lastKey = key
      lastArt = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
    }
    val playing = st?.state == PlaybackState.STATE_PLAYING
    island.setMusicState(playing, key, lastArt)
    if (playing && getSharedPreferences("li", MODE_PRIVATE).getBoolean("sound_island", false)) startViz() else stopViz()
    if (st == null || len <= 0L) return
    var pos = st.position
    if (playing) {
      val dt = SystemClock.elapsedRealtime() - st.lastPositionUpdateTime
      pos += (dt * st.playbackSpeed).toLong()
    }
    island.setMusicFraction(pos.toFloat() / len)
  }

  /** Пауза/продолжить и далее — через контроллер музыки, без закрытия острова */
  private fun mediaButton(label: String) {
    val c = musicCtl ?: return
    val l = label.lowercase()
    when {
      l.contains("пауз") || l.contains("pause") -> {
        val playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
        if (playing) c.transportControls.pause() else c.transportControls.play()
      }
      l.contains("далее") || l.contains("далі") || l.contains("next") -> c.transportControls.skipToNext()
    }
  }

  /** Для музыки без системных кнопок добавляем пауза и далее */
  private fun withMediaActions(c: IslandContent): IslandContent {
    if (c.kind != "music" || c.actions.isNotEmpty() || musicCtl == null) return c
    return IslandContent(c.key, c.kind, c.title, c.sub, c.caption,
      listOf(IslandAction(getString(R.string.pv_pause), null), IslandAction(getString(R.string.pv_next), null)),
      c.progress, c.icon, c.mediaToken, c.openIntent, c.glow, c.pkg)
  }

  /** Sound Island: уровни звука из системного микса (android.media.audiofx.Visualizer) */
  private var viz: android.media.audiofx.Visualizer? = null
  private fun startViz() {
    if (viz != null) return
    try {
      val v = android.media.audiofx.Visualizer(0)
      v.captureSize = android.media.audiofx.Visualizer.getCaptureSizeRange()[1]
      v.setDataCaptureListener(object : android.media.audiofx.Visualizer.OnDataCaptureListener {
        override fun onWaveFormDataCapture(vz: android.media.audiofx.Visualizer?, wave: ByteArray?, rate: Int) {}
        override fun onFftDataCapture(vz: android.media.audiofx.Visualizer?, fft: ByteArray?, rate: Int) {
          if (fft == null) return
          val bins = minOf(fft.size / 2, 40)
          val levels = FloatArray(5) { band ->
            var sum = 0.0
            var count = 0
            for (b in (1 + band * 8) until minOf(1 + (band + 1) * 8, bins)) {
              val re = fft[2 * b].toDouble(); val im = fft[2 * b + 1].toDouble()
              sum += Math.sqrt(re * re + im * im); count++
            }
            if (count == 0) 0f else (sum / count / 96.0).toFloat().coerceIn(0f, 1f)
          }
          h.post { if (::island.isInitialized) island.setLevels(levels) }
        }
      }, android.media.audiofx.Visualizer.getMaxCaptureRate() / 2, false, true)
      v.enabled = true
      viz = v
    } catch (_: Exception) {
      viz = null
    }
  }
  private fun stopViz() {
    try { viz?.enabled = false; viz?.release() } catch (_: Exception) {}
    viz = null
    if (::island.isInitialized) island.setLevels(null)
  }

  private fun seekMusic(f: Float) {
    val c = musicCtl ?: return
    val len = c.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: return
    if (len > 0L) c.transportControls.seekTo((f * len).toLong())
  }

  // ---------- очередь сообщений, время, часы ----------
  private val inbox = mutableListOf<IslandContent>()

  private fun addInbox(c: IslandContent) {
    inbox.removeAll { it.key == c.key }
    inbox.add(c)
    while (inbox.size > 12) inbox.removeAt(0)
    // история: последние 50 событий
    history.add(0, c)
    while (history.size > 50) history.removeAt(history.size - 1)
  }

  /** История уведомлений (новые сверху) */
  private val history = mutableListOf<IslandContent>()
  fun historySnapshot(): List<IslandContent> = history.toList()

  /** Диагностика Multi Island: только реальные данные, без тестовых уведомлений */
  fun circleCount(): Int = buildCircles().size
  fun queueSize(): Int = inbox.size

  /** Multi Island: группируем сообщения по приложению; лишние сворачиваем в «+N» */
  private fun buildCircles(): List<IslandView.Circle> {
    if (!IslandStyle.flag(this, "multi")) return emptyList()
    val groups = LinkedHashMap<String, MutableList<IslandContent>>()
    inbox.forEach { groups.getOrPut(it.caption.ifBlank { it.title }) { mutableListOf() }.add(it) }
    val all = groups.entries.toList()
    val maxVisible = 3
    val visible = if (all.size > maxVisible + 1) all.take(maxVisible) else all
    val rest = if (all.size > maxVisible + 1) all.drop(maxVisible) else emptyList()
    val color = IslandStyle.palette(this, "notify").badge
    val out = visible.map { (app, msgs) ->
      IslandView.Circle(app, msgs.size, color,
        onTap = { openMessage(msgs.last()) },
        onLong = { expanded = true; relayout(false) })
    }.toMutableList()
    if (rest.isNotEmpty()) {
      val hidden = rest.sumOf { it.value.size }
      out.add(IslandView.Circle("+", hidden, color,
        onTap = { expanded = true; relayout(false) },
        onLong = { expanded = true; relayout(false) }))
    }
    recentApps().forEach { (pkg, label) ->
      val open = { packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) } }
      out.add(IslandView.Circle(label, 0, color, onTap = open, onLong = open))
    }
    return out
  }

  private fun openMessage(m: IslandContent) {
    try { m.openIntent?.send() } catch (_: Exception) {}
    inbox.removeAll { it.key == m.key }
    refresh()
  }

  private fun showTime(): Boolean =
    getSharedPreferences("li", MODE_PRIVATE).getBoolean("show_time", false)

  /** Если острову нечего показать и включено «Показывать время», остаётся часы */
  private fun timeContent(): IslandContent? {
    if (!showTime()) return null
    val now = java.util.Date()
    val hm = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(now)
    val day = java.text.SimpleDateFormat("EEEE, d MMMM", java.util.Locale.getDefault()).format(now)
    return IslandContent("time", "time", hm, day, icon = ContextCompat.getDrawable(this, R.drawable.ic_timer), actions = quickActions())
  }

  /** Быстрые действия для карточки времени: открывают системные приложения */
  private fun quickActions(): List<IslandAction> {
    fun pi(i: Intent) = PendingIntent.getActivity(this, i.action.hashCode(), i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    return listOf(
      IslandAction(getString(R.string.q_rec_btn), pi(Intent(android.provider.MediaStore.Audio.Media.RECORD_SOUND_ACTION)))
    )
  }

  /** Обновляет часы раз в полминуты, если они включены */
  private val clock = object : Runnable {
    override fun run() {
      if (!::island.isInitialized) return
      if (showTime()) refresh()
      h.postDelayed(this, 30_000L)
    }
  }

  /** Перерисовать после изменения настроек */
  fun refreshAll() { h.post { refresh() } }

  private fun vibrate() {
    if (!IslandStyle.flag(this, "vibrate")) return
    try { (getSystemService(VIBRATOR_SERVICE) as Vibrator).vibrate(VibrationEffect.createOneShot(30, 80)) } catch (_: Exception) {}
  }

  private var chargingNow = false

  /** Фонарик включили или выключили */
  private val torchCallback = object : CameraManager.TorchCallback() {
    override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
      h.post {
        if (!::island.isInitialized) return@post
        submit(IslandContent("torch", "torch", getString(if (enabled) R.string.torch_on else R.string.torch_off), ""), null, 2500L)
      }
    }
  }

  /** NFC включили или выключили (сами платежи приложение не видит) */
  private val nfcReceiver = object : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
      if (i.action != NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) return
      val on = i.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF) == NfcAdapter.STATE_ON
      submit(IslandContent("nfc", "torch", getString(if (on) R.string.nfc_on else R.string.nfc_off), ""), null, 2500L)
    }
  }

  /** Смена режима звука (звук / вибрация / без звука) — показываем на несколько секунд */
  private val ringerReceiver = object : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
      if (i.action != AudioManager.RINGER_MODE_CHANGED_ACTION) return
      val label = when (i.getIntExtra(AudioManager.EXTRA_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL)) {
        AudioManager.RINGER_MODE_SILENT -> getString(R.string.mode_silent)
        AudioManager.RINGER_MODE_VIBRATE -> getString(R.string.mode_vibrate)
        else -> getString(R.string.mode_sound)
      }
      submit(IslandContent("ringer", "mode", label, ""), null, 2500L)
    }
  }

  private val battery = object : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
      val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
      val lvl = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
      when (i.action) {
        Intent.ACTION_POWER_CONNECTED -> { chargingNow = true; showCharge(bm, lvl) }
        Intent.ACTION_POWER_DISCONNECTED -> { chargingNow = false; showUnplugged(lvl) }
        Intent.ACTION_BATTERY_CHANGED -> if (chargingNow) showCharge(bm, lvl)
      }
    }
  }

  private var chargeT0 = 0L
  private var chargeL0 = -1

  /** Зарядка: уровень, волна и сколько осталось до 100%. Если система не даёт время — считаем по скорости. */
  private fun showCharge(bm: BatteryManager, lvl: Int) {
    val now = SystemClock.elapsedRealtime()
    if (chargeL0 < 0 || lvl < chargeL0) { chargeT0 = now; chargeL0 = lvl }
    val sys = if (Build.VERSION.SDK_INT >= 28) bm.computeChargeTimeRemaining() else -1L
    val leftMs = when {
      sys > 0L -> sys
      lvl > chargeL0 -> (now - chargeT0) * (100 - lvl) / (lvl - chargeL0)
      else -> -1L
    }
    val sub = when {
      lvl >= 100 -> getString(R.string.charge_full)
      leftMs <= 0L -> getString(R.string.charge_counting)
      else -> getString(R.string.charge_left, formatMinutes(leftMs / 60000L))
    }
    submit(IslandContent("charge", "charge", "$lvl%", sub, progress = lvl))
  }

  private fun formatMinutes(min: Long): String =
    if (min < 60) getString(R.string.min_fmt, min.toInt())
    else getString(R.string.hour_fmt, (min / 60).toInt(), (min % 60).toInt())

  /** Отключили зарядку — на несколько секунд показываем сообщение и убираем */
  private fun showUnplugged(lvl: Int) {
    h.post {
      if (!::island.isInitialized) return@post
      live.remove("charge"); chargeL0 = -1
      if (allowed("charge")) {
        temp = IslandContent("charge_off", "charge", getString(R.string.charge_off), getString(R.string.charge_off_sub, lvl))
        h.removeCallbacks(tempTimeout)
        h.postDelayed(tempTimeout, 4000L)
      }
      refresh()
    }
  }

  // Смена темы системы / обоев — пересобираем остров с новыми цветами
  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    rebuildNow()
  }

  override fun onDestroy() {
    instance = null
    stopViz()
    h.removeCallbacksAndMessages(null)
    spring?.cancel()
    try { unregisterReceiver(battery) } catch (_: Exception) {}
    try { unregisterReceiver(ringerReceiver) } catch (_: Exception) {}
    try { unregisterReceiver(nfcReceiver) } catch (_: Exception) {}
    try { (getSystemService(CAMERA_SERVICE) as CameraManager).unregisterTorchCallback(torchCallback) } catch (_: Exception) {}
    try { wm.removeView(island) } catch (_: Exception) {}
    super.onDestroy()
  }
}
