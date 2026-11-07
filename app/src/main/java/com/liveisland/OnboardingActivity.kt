package com.liveisland

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.R as M

/** Обучение при первом запуске: живая демонстрация острова + выдача разрешений */
class OnboardingActivity : AppCompatActivity() {
  private class Step(val kind: String?, val open: Boolean, val ms: Long)
  // action: 0 — нет, 1 — поверх окон, 2 — доступ к уведомлениям, 3 — работа в фоне
  private class Page(val title: String, val body: String, val steps: List<Step>, val action: Int = 0)

  private val h = Handler(Looper.getMainLooper())
  private lateinit var pages: List<Page>
  private lateinit var demo: IslandView
  private lateinit var stage: FrameLayout
  private lateinit var box: View
  private var index = 0
  private var shown = false
  private var stepIdx = 0
  private var runner: Runnable? = null
  private val dots = mutableListOf<View>()

  private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

  override fun onCreate(s: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(s)
    setContentView(R.layout.activity_onboarding)

    pages = listOf(
      Page("Привет! Это Live Island",
        "Динамический остров для Android: показывает важное у верхнего края экрана и исчезает, когда делать там нечего.",
        listOf(Step("notify", false, 1500), Step("notify", true, 2800), Step(null, false, 1100))),
      Page("Всё важное — на виду",
        "Звонки, музыка, сообщения из любых приложений и подключение зарядки. У каждого события свой цвет, он подстраивается под ваши обои.",
        listOf(Step("call", true, 2300), Step("music", true, 2300), Step("charge", true, 2300), Step("notify", true, 2300), Step(null, false, 900))),
      Page("Нажми и подержи",
        "Короткое нажатие раскрывает остров, долгое — прячет. В раскрытом виде есть кнопки: ответить на звонок, открыть сообщение.",
        listOf(Step("call", false, 1600), Step("call", true, 2800), Step("call", false, 1200), Step(null, false, 1000))),
      Page("Пропадает, когда не нужен",
        "Если ничего не происходит — остров полностью исчезает. Играет музыка? Он остаётся компактным, новое сообщение показывается поверх и возвращает музыку.",
        listOf(Step("music", false, 2200), Step("notify", true, 2400), Step("music", false, 2200), Step(null, false, 1400))),
      Page("Показ поверх окон",
        "Нужно, чтобы остров рисовался над любыми приложениями и играми.",
        listOf(Step("notify", false, 2000), Step(null, false, 800)), 1),
      Page("Доступ к уведомлениям",
        "Так остров увидит звонки, музыку и сообщения из всех приложений: Telegram, WhatsApp, YouTube Music и любых других.",
        listOf(Step("notify", true, 2400), Step(null, false, 800)), 2),
      Page("Работа в фоне",
        "Чтобы система не выключала остров, разрешите работу без ограничений. На Xiaomi, Huawei, Samsung и OPPO также включите автозапуск для Live Island.",
        listOf(Step("music", false, 2000), Step(null, false, 800)), 3),
      Page("Настройте под себя",
        "Во вкладке «Вид» выберите тему, цвет острова и скорость анимаций. В «Настройках острова» задайте положение, отступ, размер и что показывать.",
        listOf(Step("music", true, 2200), Step(null, false, 800))),
      Page("Всё готово",
        "Остров запустится и покажет подсказку. Обучение всегда можно открыть заново на главном экране.",
        listOf(Step("charge", true, 2400), Step(null, false, 900)))
    )

    val content = findViewById<View>(R.id.onbContent)
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.onbRoot)) { _, insets ->
      val b = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      content.updatePadding(top = b.top + dp(8), bottom = b.bottom + dp(16))
      insets
    }

    stage = findViewById(R.id.demoStage)
    box = findViewById(R.id.pageBox)
    demo = IslandView(this).apply { alpha = 0f }
    stage.addView(demo, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
      FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL))

    buildDots()
    bindPage()
    findViewById<View>(R.id.skip).setOnClickListener { finishTour(false) }
    findViewById<View>(R.id.next).setOnClickListener {
      if (index < pages.size - 1) goTo(index + 1) else finishTour(true)
    }
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() { if (index > 0) goTo(index - 1) else finishTour(false) }
    })
    restartDemo()
  }

  override fun onResume() { super.onResume(); refreshAction() }
  override fun onDestroy() { runner?.let { h.removeCallbacks(it) }; super.onDestroy() }

  // ---------- страницы ----------

  private fun goTo(i: Int) {
    val forward = i > index
    index = i
    val shift = dp(24).toFloat()
    box.animate().cancel()
    box.animate().alpha(0f).translationX(if (forward) -shift else shift).setStartDelay(0L).setDuration(130L)
      .setInterpolator(DecelerateInterpolator()).withEndAction {
        bindPage()
        box.translationX = if (forward) shift else -shift
        box.animate().alpha(1f).translationX(0f).setStartDelay(0L).setDuration(220L)
          .setInterpolator(DecelerateInterpolator()).start()
      }.start()
    updateDots(true)
    restartDemo()
  }

  private fun bindPage() {
    val p = pages[index]
    findViewById<TextView>(R.id.pageTitle).text = p.title
    findViewById<TextView>(R.id.pageBody).text = p.body
    findViewById<MaterialButton>(R.id.next).text = if (index == pages.size - 1) "Запустить остров" else "Далее"
    refreshAction()
  }

  private fun refreshAction() {
    val p = pages[index]
    val a = findViewById<MaterialButton>(R.id.pageAction)
    if (p.action == 0) { a.visibility = View.GONE; return }
    a.visibility = View.VISIBLE
    val done = when (p.action) {
      1 -> Settings.canDrawOverlays(this)
      2 -> NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
      else -> (getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)
    }
    a.text = when (p.action) {
      1 -> if (done) "Разрешено ✓" else "Разрешить показ поверх окон"
      2 -> if (done) "Разрешено ✓" else "Открыть доступ к уведомлениям"
      else -> if (done) "Без ограничений ✓" else "Открыть настройки батареи"
    }
    a.setOnClickListener {
      when (p.action) {
        1 -> PermHelp.overlay(this)
        2 -> PermHelp.listener(this)
        else -> PermHelp.battery(this)
      }
    }
  }

  private fun finishTour(start: Boolean) {
    getSharedPreferences("li", MODE_PRIVATE).edit().putBoolean("onboarded", true).apply()
    if (start && Settings.canDrawOverlays(this)) { IslandService.pendingDemo = true; IslandService.start(this) }
    finish()
  }

  // ---------- точки-индикаторы ----------

  private fun buildDots() {
    val row = findViewById<LinearLayout>(R.id.dots)
    pages.forEach { _ ->
      val v = View(this).apply { background = GradientDrawable().apply { cornerRadius = dp(4).toFloat() } }
      row.addView(v, LinearLayout.LayoutParams(dp(8), dp(8)).apply { marginEnd = dp(6) })
      dots.add(v)
    }
    updateDots(false)
  }

  private fun updateDots(animate: Boolean) {
    if (animate) TransitionManager.beginDelayedTransition(findViewById<ViewGroup>(R.id.dots))
    val on = MaterialColors.getColor(this, M.attr.colorPrimary, Color.BLUE)
    val off = MaterialColors.getColor(this, M.attr.colorOutlineVariant, Color.GRAY)
    dots.forEachIndexed { i, v ->
      val lp = v.layoutParams; lp.width = dp(if (i == index) 24 else 8); v.layoutParams = lp
      (v.background as GradientDrawable).setColor(if (i == index) on else off)
    }
  }

  // ---------- демо-остров ----------

  private fun restartDemo() {
    runner?.let { h.removeCallbacks(it) }
    demo.animate().cancel(); demo.stopLoops(); demo.alpha = 0f; shown = false; stepIdx = 0
    runner = Runnable { runStep() }.also { h.postDelayed(it, 350L) }
  }

  private fun runStep() {
    val page = pages[index]
    val s = page.steps[stepIdx % page.steps.size]
    applyDemo(s)
    stepIdx++
    runner = Runnable { runStep() }.also { h.postDelayed(it, s.ms) }
  }

  private fun applyDemo(s: Step) {
    val kind = s.kind
    if (kind == null) {
      if (shown) {
        demo.stopLoops(); demo.animate().cancel()
        demo.animate().alpha(0f).translationY(-dp(10).toFloat()).setStartDelay(0L).setDuration(240L)
          .setInterpolator(DecelerateInterpolator()).start()
        shown = false
      }
      return
    }
    TransitionManager.beginDelayedTransition(stage, ChangeBounds().apply {
      duration = 380L; interpolator = OvershootInterpolator(0.8f)
    })
    val wasOpen = demo.isOpen
    val lp = demo.layoutParams as FrameLayout.LayoutParams
    lp.width = if (s.open) FrameLayout.LayoutParams.MATCH_PARENT else FrameLayout.LayoutParams.WRAP_CONTENT
    demo.layoutParams = lp
    demo.bind(IslandStyle.sample(this, kind), shown)
    demo.setOpen(s.open)
    if (!shown) {
      demo.animate().cancel()
      demo.alpha = 0f; demo.translationY = -dp(10).toFloat()
      demo.animate().alpha(1f).translationY(0f).setStartDelay(0L).setDuration(260L)
        .setInterpolator(DecelerateInterpolator()).start()
      demo.play(); shown = true
    } else if (s.open && !wasOpen) demo.playExpand()
  }
}
