package com.personal.snapstreak

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.TargetApi
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

class TapService : AccessibilityService() {

    private val main = Handler(Looper.getMainLooper())
    private var running = false
    private var runId = 0
    private var loop = 1
    private var guardOn = false

    // progress of the tab check
    private var tabPolls = 0
    private var tabTaps = 0

    // floating STOP button
    private var stopView: TextView? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Status.post("Accessibility service is on. Ready.")
    }

    /** Only used as a safety net: if another app comes to the front, stop tapping. */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!running || !guardOn || !Steps.STOP_IF_LEFT_SNAPCHAT || event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == Steps.SNAPCHAT_PACKAGE || pkg == packageName || pkg in Steps.IGNORED_PACKAGES) return
        stopRun("Another app came to the front ($pkg). Stopped so it doesn't tap it.", toast = true)
    }

    override fun onInterrupt() {
        stopRun("Interrupted by Android. Stopped.")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopRun("Accessibility service turned off.")
        instance = null
        return super.onUnbind(intent)
    }

    /** Emergency stop: Volume Down while a run is active. */
    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (running && event != null &&
            event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN &&
            event.action == KeyEvent.ACTION_DOWN
        ) {
            stopRun("Stopped with Volume Down.", toast = true)
            return true
        }
        return false
    }

    fun isRunning(): Boolean = running

    /** Call right after Snapchat has been launched. */
    fun startRun(initialDelayMs: Long) {
        if (running) return
        running = true
        runId++
        loop = 1
        guardOn = false
        val id = runId
        Status.post("Waiting for Snapchat to open...")
        showStopButton()
        main.postDelayed({
            if (running && id == runId) {
                guardOn = true
                runStep(0, id)
            }
        }, initialDelayMs)
    }

    fun stopRun(message: String, toast: Boolean = false) {
        if (running) {
            running = false
            runId++
            main.removeCallbacksAndMessages(null)
        }
        guardOn = false
        hideStopButton()
        Status.post(message)
        if (toast) Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun runStep(index: Int, id: Int) {
        if (!running || id != runId) return

        val steps = Steps.all
        if (index >= steps.size) {
            if (Steps.MAX_LOOPS > 0 && loop >= Steps.MAX_LOOPS) {
                stopRun("Done. $loop loop(s) finished.", toast = true)
                return
            }
            loop++
            runStep(0, id)
            return
        }

        val progress = "Loop $loop - ${index + 1}/${steps.size}"

        when (val step = steps[index]) {
            is Step.Wait -> {
                Status.post("$progress: wait ${step.ms / 1000.0}s")
                main.postDelayed({ runStep(index + 1, id) }, step.ms)
            }
            is Step.Tap -> {
                Status.post("$progress: ${step.label}")
                tapAt(step.x, step.y, step.label, id) {
                    val next = steps.getOrNull(index + 1)
                    val pause = when {
                        next is Step.Wait -> 0L
                        step.useCameraGap -> Prefs.cameraGapMs(this)
                        else -> step.settleMs ?: Steps.SETTLE_AFTER_TAP_MS
                    }
                    if (pause > 0L) {
                        main.postDelayed({ runStep(index + 1, id) }, pause)
                    } else {
                        runStep(index + 1, id)
                    }
                }
            }
            is Step.EnsureTab -> {
                Status.post("$progress: ${step.label} tab")
                tabPolls = 0
                tabTaps = 0
                ensureTab(step, id) {
                    main.postDelayed({ runStep(index + 1, id) }, step.settleMs)
                }
            }
        }
    }

    private fun tapAt(x: Float, y: Float, label: String, id: Int, onDone: () -> Unit) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, Steps.TAP_DURATION_MS)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val callback = object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                if (running && id == runId) onDone()
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                if (running && id == runId) {
                    stopRun("Tap on '$label' was cancelled. Stopped.", toast = true)
                }
            }
        }

        if (!dispatchGesture(gesture, callback, null)) {
            stopRun("Android refused the tap on '$label'. Stopped.", toast = true)
        }
    }

    // ---- make sure the face melt tab is really selected -------------------------------------

    private fun ensureTab(step: Step.EnsureTab, id: Int, onDone: () -> Unit) {
        if (!Steps.VERIFY_TAB || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // Can't look at the screen here: tap once and carry on, like before.
            tapAt(step.x, step.y, step.label, id, onDone)
            return
        }
        checkTab(step, id, onDone)
    }

    @TargetApi(Build.VERSION_CODES.R)
    private fun checkTab(step: Step.EnsureTab, id: Int, onDone: () -> Unit) {
        if (!running || id != runId) return
        takeScreenshot(
            Display.DEFAULT_DISPLAY,
            mainExecutor,
            object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    val state = readScreen(result)
                    if (running && id == runId) handleTabState(state, step, id, onDone)
                }

                override fun onFailure(errorCode: Int) {
                    if (running && id == runId) {
                        handleTabState(ScreenCheck.State.NOT_LOADED, step, id, onDone)
                    }
                }
            }
        )
    }

    @TargetApi(Build.VERSION_CODES.R)
    private fun readScreen(result: AccessibilityService.ScreenshotResult): ScreenCheck.State {
        val buffer = result.hardwareBuffer
        try {
            val hardware = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
                ?: return ScreenCheck.State.NOT_LOADED
            val soft = hardware.copy(Bitmap.Config.ARGB_8888, false)
            hardware.recycle()
            if (soft == null) return ScreenCheck.State.NOT_LOADED
            val state = ScreenCheck.read(soft.width, soft.height) { x, y -> soft.getPixel(x, y) }
            soft.recycle()
            return state
        } finally {
            buffer.close()
        }
    }

    @TargetApi(Build.VERSION_CODES.R)
    private fun handleTabState(
        state: ScreenCheck.State,
        step: Step.EnsureTab,
        id: Int,
        onDone: () -> Unit
    ) {
        when (state) {
            ScreenCheck.State.OTHER_TAB -> onDone()   // "All" isn't selected, so the tab is already on

            ScreenCheck.State.NOT_LOADED -> {
                tabPolls++
                if (tabPolls > Steps.TAB_MAX_POLLS) {
                    stopRun("Couldn't see the Send To screen. Stopped before sending.", toast = true)
                } else {
                    main.postDelayed({ checkTab(step, id, onDone) }, Steps.TAB_POLL_MS)
                }
            }

            ScreenCheck.State.ALL_TAB -> {
                if (tabTaps >= Steps.TAB_MAX_TAPS) {
                    stopRun(
                        "Couldn't switch to the ${step.label} tab. Stopped before sending.",
                        toast = true
                    )
                } else {
                    tabTaps++
                    Status.post("${step.label} tab: tapping (try $tabTaps)")
                    tapAt(step.x, step.y, step.label, id) {
                        main.postDelayed({ checkTab(step, id, onDone) }, Steps.TAB_RECHECK_MS)
                    }
                }
            }
        }
    }

    // ---- floating STOP button ---------------------------------------------------------------

    private fun showStopButton() {
        if (!Steps.SHOW_STOP_BUTTON || stopView != null) return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.density

        val view = TextView(this).apply {
            text = "\u25A0  STOP"
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            val padH = (16 * density).toInt()
            val padV = (10 * density).toInt()
            setPadding(padH, padV, padH, padV)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#D32F2F"))
                cornerRadius = 28 * density
            }
            alpha = 0.92f
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (8 * density).toInt()
            y = Steps.STOP_BUTTON_Y_PX
        }

        // tap = stop, drag = move it out of the way
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        view.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX
                    downY = e.rawY
                    startX = params.x
                    startY = params.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (abs(dx) > 10 * density || abs(dy) > 10 * density) moved = true
                    if (moved) {
                        params.x = (startX + dx).toInt()
                        params.y = (startY + dy).toInt()
                        try {
                            wm.updateViewLayout(view, params)
                        } catch (_: Exception) {
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) stopRun("Stopped with the STOP button.", toast = true)
                    true
                }
                else -> false
            }
        }

        try {
            wm.addView(view, params)
            stopView = view
        } catch (_: Exception) {
            // No overlay available: Volume Down still stops a run.
        }
    }

    private fun hideStopButton() {
        val view = stopView ?: return
        stopView = null
        try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(view)
        } catch (_: Exception) {
        }
    }

    companion object {
        @Volatile
        var instance: TapService? = null
    }
}
