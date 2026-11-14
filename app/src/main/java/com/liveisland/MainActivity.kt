package com.liveisland

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.R as M

class MainActivity : AppCompatActivity() {
  private val kinds = listOf("call", "music", "charge", "notify")
  private var idx = 0
  private val h = Handler(Looper.getMainLooper())
  private var askedNotif = false
  private lateinit var preview: IslandView
  private lateinit var pages: List<View>
  private var currentPage = 0
  private var currentTab = 0
  private val easeOut = PathInterpolator(0.2f, 0f, 0f, 1f)

  // Вкладки нижней панели: 0 Главная, 1 Вид, 2 Остров (открывает настройки), 3 Обновления
  private val tabIds = listOf(R.id.tabHome, R.id.tabLook, R.id.tabIsland, R.id.tabUpdate)
  private val tabIcons = listOf(R.id.icHome, R.id.icLook, R.id.icIsland, R.id.icUpdate)
  private val tabLabels = listOf(R.id.lblHome, R.id.lblLook, R.id.lblIsland, R.id.lblUpdate)
  private val tabTitles = listOf(R.string.tab_home, R.string.tab_look, R.string.tab_island, R.string.tab_update)
  private val pageForTab = mapOf(0 to 0, 1 to 1, 3 to 2)

  private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

  override fun onCreate(s: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(s)
    setContentView(R.layout.activity_main)
    showStartLoader()

    val root = findViewById<View>(R.id.root)
    ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
      val b = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      root.setPadding(0, b.top, 0, b.bottom)
      insets
    }

    // Заголовок: значок в мягкой «печенье»-подложке и версия
    findViewById<TextView>(R.id.heroVer).text = getString(R.string.hero_ver, currentName())
    findViewById<TextView>(R.id.updVer).text = getString(R.string.upd_version, currentName(), currentCode())

    pages = listOf(findViewById(R.id.pageHome), findViewById(R.id.pageLook), findViewById(R.id.pageUpdate))
    findViewById<View>(R.id.hero).visibility = if (currentPage == 2) View.VISIBLE else View.GONE
    setupNav()

    // Живое превью острова
    val stage = findViewById<FrameLayout>(R.id.stage)
    preview = IslandView(this)
    stage.addView(preview, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP))
    preview.bind(IslandStyle.sample(this, kinds[0]), false)
    preview.setOpen(true)
    preview.post { preview.play() }
    preview.setOnClickListener {
      TransitionManager.beginDelayedTransition(stage, ChangeBounds().apply {
        duration = 380L; interpolator = OvershootInterpolator(0.8f)
      })
      idx = (idx + 1) % kinds.size
      preview.bind(IslandStyle.sample(this, kinds[idx]), true)
    }

    findViewById<View>(R.id.checkUpdate).setOnClickListener { checkUpdates() }
    findViewById<View>(R.id.openDownload).setOnClickListener { startActivity(Intent(this, DownloadActivity::class.java)) }
    findViewById<View>(R.id.tgChannel).setOnClickListener { openUrl("https://t.me/X_material") }
    findViewById<View>(R.id.tgSupport).setOnClickListener { openUrl("https://t.me/X_material?direct") }
    findViewById<View>(R.id.replay).setOnClickListener { startActivity(Intent(this, OnboardingActivity::class.java)) }

    // Тёмный интерфейс приложения
    findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.swAppDark).apply {
      isChecked = getSharedPreferences("li", MODE_PRIVATE).getBoolean("app_dark", true)
      setOnCheckedChangeListener { _, on ->
        getSharedPreferences("li", MODE_PRIVATE).edit().putBoolean("app_dark", on).apply()
        AppCompatDelegate.setDefaultNightMode(if (on) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
      }
    }

    // Язык приложения: English / Русский / Українська
    val langGroup = findViewById<MaterialButtonToggleGroup>(R.id.langGroup)
    val langIds = linkedMapOf(R.id.langEn to "en", R.id.langRu to "ru", R.id.langUk to "uk")
    val curLocale = AppCompatDelegate.getApplicationLocales()
    val curTag = if (curLocale.isEmpty()) Locale.getDefault().language else curLocale.toLanguageTags()
    langGroup.check((langIds.entries.firstOrNull { curTag.startsWith(it.value) } ?: langIds.entries.first()).key)
    langGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
      if (isChecked) {
        val tag = langIds.getValue(checkedId)
        if (!curTag.startsWith(tag)) AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
      }
    }

    // Тема острова
    val themeGroup = findViewById<MaterialButtonToggleGroup>(R.id.themeGroup)
    val themeIds = linkedMapOf(R.id.themeMaterial to "material", R.id.themeBlack to "black", R.id.themeWhite to "white")
    themeGroup.check((themeIds.entries.firstOrNull { it.value == IslandStyle.theme(this) } ?: themeIds.entries.first()).key)
    themeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
      if (isChecked) { IslandStyle.setTheme(this, themeIds.getValue(checkedId)); applyTheme() }
    }

    // Цвет острова
    val accentGroup = findViewById<MaterialButtonToggleGroup>(R.id.accentGroup)
    val accentIds = linkedMapOf(R.id.accAuto to "auto", R.id.accBlue to "blue", R.id.accPink to "pink", R.id.accGreen to "green")
    accentGroup.check((accentIds.entries.firstOrNull { it.value == IslandStyle.accent(this) } ?: accentIds.entries.first()).key)
    accentGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
      if (isChecked) { IslandStyle.setAccent(this, accentIds.getValue(checkedId)); applyTheme() }
    }

    // Скорость анимаций
    val animGroup = findViewById<MaterialButtonToggleGroup>(R.id.animGroup)
    val animIds = linkedMapOf(R.id.animOff to "off", R.id.animFast to "fast", R.id.animNormal to "normal", R.id.animSlow to "slow")
    animGroup.check((animIds.entries.firstOrNull { it.value == IslandStyle.speedName(this) } ?: animIds.entries.first()).key)
    animGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
      if (isChecked) {
        IslandStyle.setSpeed(this, animIds.getValue(checkedId))
        preview.bind(IslandStyle.sample(this, kinds[idx]), false)
        preview.setOpen(true); preview.play()
      }
    }

    // Первый запуск — обучение
    if (!getSharedPreferences("li", MODE_PRIVATE).getBoolean("onboarded", false))
      startActivity(Intent(this, OnboardingActivity::class.java))
  }

  // ---------- нижняя панель ----------

  private fun setupNav() {
    tabLabels.forEachIndexed { i, id -> findViewById<TextView>(id).setText(tabTitles[i]) }
    tabIds.forEachIndexed { i, id -> findViewById<View>(id).setOnClickListener { onTab(i) } }
    updateTabColors(currentTab)
    findViewById<View>(R.id.tabRow).post { placeIndicator(currentTab, false) }
  }

  private fun onTab(i: Int) {
    tabIds.getOrNull(i)?.let { id ->
      findViewById<View>(id).animate().cancel()
      findViewById<View>(id).animate().scaleX(0.9f).scaleY(0.9f).setDuration(90L).withEndAction {
        findViewById<View>(id).animate().scaleX(1f).scaleY(1f).setDuration(220L).setInterpolator(OvershootInterpolator(1.4f)).start()
      }.start()
    }
    if (i == 2) { startActivity(Intent(this, SettingsActivity::class.java)); return }
    val page = pageForTab[i] ?: return
    if (i == currentTab) return
    currentTab = i
    placeIndicator(i, true)
    updateTabColors(i)
    showPage(page)
  }

  /** Индикатор плавно «переезжает» к выбранной вкладке, как в Telegram */
  private fun placeIndicator(i: Int, animate: Boolean) {
    val row = findViewById<LinearLayout>(R.id.tabRow)
    val ind = findViewById<View>(R.id.navIndicator)
    val w = row.width / tabIds.size
    if (w <= 0) return
    if (ind.layoutParams.width != w) ind.layoutParams = FrameLayout.LayoutParams(w, dp(58))
    val target = (i * w).toFloat()
    if (!animate) { ind.translationX = target; return }
    SpringAnimation(ind, DynamicAnimation.TRANSLATION_X, target).apply {
      spring = SpringForce(target).setDampingRatio(0.8f).setStiffness(SpringForce.STIFFNESS_MEDIUM)
      start()
    }
  }

  private fun updateTabColors(sel: Int) {
    val active = ContextCompat.getColor(this, R.color.nav_active)
    val idle = ContextCompat.getColor(this, R.color.nav_idle)
    tabIds.indices.forEach { i ->
      val to = if (i == sel) active else idle
      val icon = findViewById<ImageView>(tabIcons[i])
      val label = findViewById<TextView>(tabLabels[i])
      val from = label.currentTextColor
      ValueAnimator.ofObject(ArgbEvaluator(), from, to).apply {
        duration = 260L
        addUpdateListener { val c = it.animatedValue as Int; icon.setColorFilter(c); label.setTextColor(c) }
        start()
      }
    }
  }

  /** Плавное переключение страниц: уходящая уплывает и гаснет, новая проявляется */
  private fun showPage(i: Int) {
    if (i == currentPage) return
    val dir = if (i > currentPage) 1 else -1
    val shift = dp(32).toFloat() * dir
    val out = pages[currentPage]; val inn = pages[i]
    out.animate().cancel(); inn.animate().cancel()
    out.animate().alpha(0f).translationX(-shift * 0.6f).setDuration(180L).setInterpolator(DecelerateInterpolator())
      .withEndAction { out.visibility = View.GONE; out.translationX = 0f }.start()
    inn.translationX = shift; inn.alpha = 0f; inn.visibility = View.VISIBLE
    inn.animate().alpha(1f).translationX(0f).setStartDelay(90L).setDuration(380L).setInterpolator(easeOut).start()
    currentPage = i
    findViewById<View>(R.id.hero).visibility = if (i == 2) View.VISIBLE else View.GONE
  }

  override fun onResume() {
    super.onResume()
    refresh()
    val onboarded = getSharedPreferences("li", MODE_PRIVATE).getBoolean("onboarded", false)
    if (onboarded && !askedNotif && Build.VERSION.SDK_INT >= 33 &&
      checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
      askedNotif = true
      requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
    }
  }

  /** Загрузочный экран при запуске: фигура-загрузчик и название, затем плавно уходит */
  private fun showStartLoader() {
    val bg = MaterialColors.getColor(findViewById(R.id.root), M.attr.colorSurface, 0)
    val box = FrameLayout(this).apply { setBackgroundColor(bg); isClickable = true }
    box.addView(MorphLoaderView(this), FrameLayout.LayoutParams(dp(120), dp(120), Gravity.CENTER))
    box.addView(TextView(this).apply {
      text = getString(R.string.app_name)
      textSize = 18f
      setTypeface(null, android.graphics.Typeface.BOLD)
      setTextColor(MaterialColors.getColor(this, M.attr.colorOnSurface, 0))
    }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
      .apply { topMargin = dp(110) })
    (window.decorView as android.view.ViewGroup).addView(box, FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
    h.postDelayed({
      box.animate().alpha(0f).setDuration(350L).withEndAction {
        (box.parent as? android.view.ViewGroup)?.removeView(box)
      }.start()
    }, 3000L)
  }

  private fun openUrl(url: String) {
    try { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) } catch (_: Exception) {}
  }

  private fun applyTheme() {
    preview.bind(IslandStyle.sample(this, kinds[idx]), false)
    IslandService.instance?.restyle()
  }

  private fun currentName(): String = try { packageManager.getPackageInfo(packageName, 0).versionName ?: "" } catch (_: Exception) { "" }
  @Suppress("DEPRECATION")
  private fun currentCode(): Int = try { packageManager.getPackageInfo(packageName, 0).versionCode } catch (_: Exception) { 0 }

  // ---------- обновления ----------

  private fun checkUpdates() {
    val btn = findViewById<MaterialButton>(R.id.checkUpdate)
    btn.isEnabled = false
    btn.text = getString(R.string.checking)
    val code = currentCode()
    val name = currentName()
    Thread {
      val result = runCatching { UpdateChecker.fetch(code) }
      h.post {
        btn.isEnabled = true
        btn.text = getString(R.string.check_updates)
        result.fold(
          onSuccess = { info -> showUpdate(info, name) },
          onFailure = { e -> showError(e) }
        )
      }
    }.start()
  }

  private fun showUpdate(info: UpdateChecker.Info, currentName: String) {
    if (!info.newer) {
      MaterialAlertDialogBuilder(this)
        .setMessage(getString(R.string.update_latest, currentName))
        .setPositiveButton(R.string.ok, null)
        .show()
      return
    }
    val notes = info.changes.joinToString("\n") { "• $it" }
    MaterialAlertDialogBuilder(this)
      .setTitle(getString(R.string.update_title, info.versionName))
      .setMessage(notes)
      .setPositiveButton(R.string.update_download) { _, _ -> ensureInstallAllowed(info) }
      .setNegativeButton(R.string.update_later, null)
      .show()
  }

  /** Показываем причину ошибки, чтобы было понятно, что именно не так с сервером */
  private fun showError(e: Throwable) {
    val reason = e.message ?: e.javaClass.simpleName
    MaterialAlertDialogBuilder(this)
      .setMessage(getString(R.string.update_error_detail, reason))
      .setPositiveButton(R.string.ok, null)
      .show()
  }

  /** Показывает ход загрузки на вкладке «Обновления» */
  private fun setDl(pct: Int, text: String) {
    val bar = findViewById<LinearProgressIndicator>(R.id.dlBar)
    bar.setProgressCompat(pct, true)
    findViewById<TextView>(R.id.dlStatus).text = if (pct in 1..99) "$text · $pct%" else text
  }

  /** Перед скачиванием проверяем разрешение на установку. Без него установщик не откроется. */
  private fun ensureInstallAllowed(info: UpdateChecker.Info) {
    if (Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
      PermHelp.install(this)
      return
    }
    startDownload(info)
  }

  /** Скачивание APK прямо в приложении; прогресс показывается в острове */
  private fun startDownload(info: UpdateChecker.Info) {
    val t = getString(R.string.dl_title)
    val sub = getString(R.string.dl_sub, info.versionName)
    IslandService.start(this)
    startActivity(Intent(this, DownloadActivity::class.java))
    Thread {
      try {
        h.post { setDl(0, t); IslandService.instance?.showDownload(0, t, sub) }
        val file = UpdateInstaller.download(this, info.apkUrl) { pct ->
          h.post { setDl(pct, t); IslandService.instance?.showDownload(pct, t, sub) }
        }
        h.post {
          setDl(100, getString(R.string.dl_done))
          IslandService.instance?.showDownload(100, getString(R.string.dl_done), sub)
          UpdateInstaller.install(this, file)
          h.postDelayed({ IslandService.instance?.hideDownload() }, 2500L)
        }
      } catch (e: Exception) {
        h.post {
          IslandService.instance?.hideDownload()
          setDl(0, getString(R.string.dl_fail))
          MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.dl_fail) + "\n" + (e.message ?: ""))
            .setPositiveButton(R.string.ok, null)
            .show()
        }
      }
    }.start()
  }

  // ---------- разрешения и шаги ----------

  private fun refresh() {
    val overlay = Settings.canDrawOverlays(this)
    val listener = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
    val running = IslandService.instance != null

    bindStep(R.id.step1, "1", overlay, R.string.step1_title, getString(R.string.step1_desc),
      if (overlay) R.string.action_change else R.string.action_allow, true) {
      PermHelp.overlay(this)
    }
    bindStep(R.id.step2, "2", listener, R.string.step2_title, getString(R.string.step2_desc),
      if (listener) R.string.action_change else R.string.action_allow, true) {
      PermHelp.listener(this)
    }
    bindStep(R.id.step3, "3", running, R.string.step3_title,
      getString(if (running) R.string.step3_on else R.string.step3_off),
      if (running) R.string.action_stop else R.string.action_start, running || (overlay && listener)) {
      if (running) stopService(Intent(this, IslandService::class.java)) else IslandService.start(this)
      h.postDelayed({ refresh() }, 400)
    }
  }

  private fun bindStep(id: Int, number: String, done: Boolean, titleRes: Int, desc: String,
                       actionRes: Int, enabled: Boolean, onClick: () -> Unit) {
    val v = findViewById<View>(id)
    fun col(attr: Int) = MaterialColors.getColor(v, attr)
    v.findViewById<TextView>(R.id.badge).apply {
      text = if (done) "✓" else number
      background = CookieDrawable(if (done) col(M.attr.colorPrimary) else col(M.attr.colorSecondaryContainer))
      setTextColor(if (done) col(M.attr.colorOnPrimary) else col(M.attr.colorOnSecondaryContainer))
    }
    v.findViewById<TextView>(R.id.title).setText(titleRes)
    v.findViewById<TextView>(R.id.desc).text = desc
    v.findViewById<MaterialButton>(R.id.action).apply {
      setText(actionRes); isEnabled = enabled; setOnClickListener { onClick() }
    }
  }
}
