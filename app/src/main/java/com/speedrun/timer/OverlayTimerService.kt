package com.speedrun.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class OverlayTimerService : Service() {
    companion object {
        const val ACTION_START = "com.speedrun.timer.START"
        const val ACTION_STOP = "com.speedrun.timer.STOP"
        const val ACTION_RESET = "com.speedrun.timer.RESET"
        const val ACTION_REFRESH_KEYS = "com.speedrun.timer.REFRESH_KEYS"
        const val ACTION_REFRESH_STYLE = "com.speedrun.timer.REFRESH_STYLE"
    }

    private lateinit var windowManager: WindowManager
    private var overlay: View? = null
    private lateinit var timerText: TextView
    private lateinit var controls: LinearLayout
    private lateinit var container: LinearLayout
    private lateinit var params: WindowManager.LayoutParams
    private val controlButtons = mutableListOf<TextView>()

    private val handler = Handler(Looper.getMainLooper())
    private var running = true
    private var accumulated = 0L
    private var startedAt = System.currentTimeMillis()
    private var controlsVisible = false

    // Android 12+ Material You system palette; falls back to a purple accent on older Android.
    private val activeTimerColor: Int
        get() {
            if (getSharedPreferences("appearance", MODE_PRIVATE).getString("timer_style", "Padrão") == "Liquid Glass") {
                return Color.rgb(90, 170, 255)
            }
            if (Build.VERSION.SDK_INT >= 31) {
                val colorId = resources.getIdentifier("system_accent1_500", "color", "android")
                if (colorId != 0) return getColor(colorId)
            }
            return Color.rgb(103, 80, 164)
        }

    private val pausedGrayColor = Color.rgb(158, 158, 158)
    private val pausedWhiteColor = Color.WHITE

    private fun materialColor(name: String, fallback: Int): Int {
        if (Build.VERSION.SDK_INT >= 31) {
            val id = resources.getIdentifier(name, "color", "android")
            if (id != 0) return getColor(id)
        }
        return fallback
    }

    private fun dp(value: Float): Int =
        (value * resources.displayMetrics.density).toInt()

    // Only the timer text changes while paused; the overlay surface and outline stay unchanged.
    private fun updateTimerAppearance() {
        if (::timerText.isInitialized) {
            val pausedColor = if ((System.currentTimeMillis() / 1000L) % 2L == 0L)
                pausedGrayColor else pausedWhiteColor
            timerText.setTextColor(if (running) activeTimerColor else pausedColor)
        }
    }

    private val hideControls = Runnable { setControlsVisible(false) }

    private val ticker = object : Runnable {
        override fun run() {
            if (running && ::timerText.isInitialized) {
                val elapsed = accumulated + (System.currentTimeMillis() - startedAt)
                timerText.text = formatTime(elapsed)
            }
            updateTimerAppearance()
            handler.postDelayed(this, 25)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotification()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        showOverlay()
        handler.post(ticker)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!running) {
                    startedAt = System.currentTimeMillis()
                    running = true
                    updateTimerAppearance()
                }
            }
            ACTION_STOP -> stopTimer()
            ACTION_RESET -> resetTimer()
            ACTION_REFRESH_KEYS -> updateKeyboardFocus()
            ACTION_REFRESH_STYLE -> applyOverlayStyle()
        }
        return START_STICKY
    }

    private fun stopTimer() {
        if (running) {
            accumulated += System.currentTimeMillis() - startedAt
            running = false
            timerText.text = formatTime(accumulated)
            updateTimerAppearance()
        }
    }

    private fun resetTimer() {
        running = false
        accumulated = 0L
        startedAt = 0L
        timerText.text = "00:00.00"
        updateTimerAppearance()
    }

    private fun handleShortcut(keyCode: Int) {
        val prefs = getSharedPreferences("keyboard_shortcuts", MODE_PRIVATE)
        fun matches(action: String) = prefs.getInt("key_$action", KeyEvent.KEYCODE_UNKNOWN) == keyCode
        when {
            matches("pause") -> {
                if (running) stopTimer() else { startedAt = System.currentTimeMillis(); running = true }
                updateTimerAppearance()
                revealControls()
            }
            matches("reset") -> { resetTimer(); revealControls() }
            matches("home") -> {
                val appIntent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(appIntent)
            }
            matches("close") -> stopSelf()
        }
    }

    private fun createNotification() {
        val channelId = "speedrun_overlay"
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Timer flutuante", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }.setContentTitle("Speedrun Timer ativo")
            .setContentText("A sobreposição do cronômetro está em execução.")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(7, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(7, notification)
        }
    }

    private fun showOverlay() {
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(14f), dp(8f), dp(14f), dp(8f))
            elevation = dp(8f).toFloat()
            val surface = materialColor("system_neutral1_900", Color.rgb(28, 27, 31))
            background = GradientDrawable().apply {
                setColor(surface)
                cornerRadius = dp(26f).toFloat()
                setStroke(dp(1f), (activeTimerColor and 0x00FFFFFF) or 0x55000000)
            }
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        fun icon(label: String, action: () -> Unit): TextView {
            val tonalButtonColor = materialColor(
                "system_neutral2_800", Color.rgb(55, 53, 60)
            )
            return TextView(this).apply {
                controlButtons.add(this)
                text = label
                textSize = 23f
                setTextColor(Color.WHITE)
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                gravity = Gravity.CENTER
                includeFontPadding = false
                background = GradientDrawable().apply {
                    setColor(tonalButtonColor)
                    cornerRadius = dp(18f).toFloat()
                }
                setPadding(dp(10f), 0, dp(10f), 0)
                setOnClickListener { action() }
            }
        }

        val home = icon("⌂") {
            val appIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(appIntent)
        }
        val reset = icon("↶") {
            running = false
            accumulated = 0L
            startedAt = 0L
            timerText.text = "00:00.00"
            updateTimerAppearance()
            revealControls()
        }
        val pause = icon("Ⅱ") {
            if (running) stopTimer()
            else {
                startedAt = System.currentTimeMillis()
                running = true
            }
            updateTimerAppearance()
            revealControls()
        }
        val close = icon("×") { stopSelf() }

        val buttonSize = dp(42f)
        val buttonGap = dp(6f)
        fun addControl(view: View) {
            val lp = LinearLayout.LayoutParams(buttonSize, buttonSize)
            lp.marginStart = buttonGap / 2
            lp.marginEnd = buttonGap / 2
            topRow.addView(view, lp)
        }
        addControl(home)
        addControl(reset)
        addControl(pause)
        addControl(close)

        timerText = TextView(this).apply {
            text = "00:00.00"
            textSize = 34f
            setTextColor(activeTimerColor)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            includeFontPadding = false
            letterSpacing = 0.01f
            setPadding(dp(2f), dp(2f), dp(2f), dp(2f))
        }

        controls = topRow
        container.addView(controls, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(6f)
        })
        container.addView(timerText, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        setControlsVisible(false)

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(24f)
            y = dp(180f)
        }

        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        val dragTouchListener = View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (kotlin.math.abs(dx) > dp(4f) || kotlin.math.abs(dy) > dp(4f)) moved = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(container, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    revealControls()
                    true
                }
                else -> false
            }
        }

        container.setOnTouchListener(dragTouchListener)
        timerText.setOnTouchListener(dragTouchListener)
        applyOverlayStyle()
        updateTimerAppearance()

        overlay = container
        windowManager.addView(container, params)
        updateKeyboardFocus()
    }

    private fun applyOverlayStyle() {
        if (!::container.isInitialized || !::timerText.isInitialized) return
        val style = getSharedPreferences("appearance", MODE_PRIVATE).getString("timer_style", "Padrão") ?: "Padrão"
        when (style) {
            "Texto" -> {
                container.setPadding(0, 0, 0, 0)
                container.background = null
                container.elevation = 0f
                timerText.setPadding(0, 0, 0, 0)
                controlButtons.forEach { it.background = null; it.setPadding(dp(7f), 0, dp(7f), 0) }
            }
            "Liquid Glass" -> {
                container.setPadding(dp(14f), dp(8f), dp(14f), dp(8f))
                container.elevation = dp(10f).toFloat()
                container.background = GradientDrawable().apply {
                    setColor(Color.argb(145, 225, 238, 255))
                    cornerRadius = dp(26f).toFloat()
                    setStroke(dp(1.5f), Color.argb(190, 255, 255, 255))
                }
                timerText.setPadding(dp(2f), dp(2f), dp(2f), dp(2f))
                controlButtons.forEach { button ->
                    button.background = GradientDrawable().apply {
                        setColor(Color.argb(105, 255, 255, 255))
                        cornerRadius = dp(18f).toFloat()
                        setStroke(dp(1f), Color.argb(130, 255, 255, 255))
                    }
                    button.setPadding(dp(10f), 0, dp(10f), 0)
                }
            }
            else -> {
                container.setPadding(dp(14f), dp(8f), dp(14f), dp(8f))
                container.elevation = dp(8f).toFloat()
                container.background = GradientDrawable().apply {
                    setColor(materialColor("system_neutral1_900", Color.rgb(28, 27, 31)))
                    cornerRadius = dp(26f).toFloat()
                    setStroke(dp(1f), (activeTimerColor and 0x00FFFFFF) or 0x55000000)
                }
                timerText.setPadding(dp(2f), dp(2f), dp(2f), dp(2f))
                controlButtons.forEach { button ->
                    button.background = GradientDrawable().apply {
                        setColor(materialColor("system_neutral2_800", Color.rgb(55, 53, 60)))
                        cornerRadius = dp(18f).toFloat()
                    }
                    button.setPadding(dp(10f), 0, dp(10f), 0)
                }
            }
        }
        updateTimerAppearance()
        if (::windowManager.isInitialized && container.isAttachedToWindow) windowManager.updateViewLayout(container, params)
    }

    private fun updateKeyboardFocus() {
        if (!::container.isInitialized || !::params.isInitialized) return
        val prefs = getSharedPreferences("keyboard_shortcuts", MODE_PRIVATE)
        val hasShortcuts = listOf("pause", "reset", "home", "close").any {
            prefs.getInt("key_$it", KeyEvent.KEYCODE_UNKNOWN) != KeyEvent.KEYCODE_UNKNOWN
        }
        container.isFocusableInTouchMode = hasShortcuts
        container.isFocusable = hasShortcuts
        container.setOnKeyListener { _, keyCode, event ->
            if (hasShortcuts && event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                val configured = listOf("pause", "reset", "home", "close").any {
                    prefs.getInt("key_$it", KeyEvent.KEYCODE_UNKNOWN) == keyCode
                }
                if (configured) { handleShortcut(keyCode); true } else false
            } else false
        }
        val focusFlag = if (hasShortcuts) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        params.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or focusFlag
        windowManager.updateViewLayout(container, params)
        if (hasShortcuts) container.requestFocus()
    }

    private fun setControlsVisible(visible: Boolean) {
        controlsVisible = visible
        if (::controls.isInitialized) controls.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun revealControls() {
        setControlsVisible(true)
        handler.removeCallbacks(hideControls)
        handler.postDelayed(hideControls, 3000)
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        handler.removeCallbacks(hideControls)
        overlay?.let {
            if (it.isAttachedToWindow) windowManager.removeView(it)
        }
        overlay = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
