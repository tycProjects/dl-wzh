package com.touchflux.app.service

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.touchflux.app.data.Prefs
import com.touchflux.app.fx.EffectView

/**
 * "Everywhere mode": draws effects above every app.
 *
 * Two non-blocking accessibility overlay windows are added:
 *  - a full-screen, fully pass-through layer where the effects are painted;
 *  - a 1x1 px "listener" window that Android notifies (ACTION_OUTSIDE) whenever the screen is
 *    touched anywhere else, which gives us the tap position without intercepting anything.
 *
 * Fallback: on devices that blank the coordinates of outside-touches, the centre of the clicked
 * view (from the accessibility click event) is used instead.
 *
 * Nothing is recorded, stored or sent anywhere – the app does not even hold the INTERNET permission.
 */
class TouchFluxAccessibilityService : AccessibilityService() {
    private lateinit var wm: WindowManager
    private lateinit var prefs: Prefs
    private var fxView: EffectView? = null
    private var listener: View? = null
    private var lastOutsideMs = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = Prefs(this)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        addWindows()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_VIEW_CLICKED && type != AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) return
        if (FxState.appInForeground) return
        // A precise outside-touch just handled this tap.
        if (SystemClock.uptimeMillis() - lastOutsideMs < 450L) return

        val node = event.source ?: return
        val r = Rect()
        node.getBoundsInScreen(r)
        if (r.isEmpty) return
        spawnAtScreen(r.exactCenterX(), r.exactCenterY())
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        removeWindows()
        super.onDestroy()
    }

    private fun addWindows() {
        removeWindows()

        val fx = EffectView(this).apply {
            interactive = false
            showBackdrop = false
        }
        val fxParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        val listenerView = object : View(this) {
            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) onOutsideTouch(event)
                return false
            }
        }
        val listenerParams = WindowManager.LayoutParams(
            1, 1,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        try {
            wm.addView(fx, fxParams)
            fxView = fx
            wm.addView(listenerView, listenerParams)
            listener = listenerView
        } catch (e: Exception) {
            removeWindows()
        }
    }

    private fun removeWindows() {
        val wmLocal = if (::wm.isInitialized) wm else return
        fxView?.let { runCatching { wmLocal.removeViewImmediate(it) } }
        listener?.let { runCatching { wmLocal.removeViewImmediate(it) } }
        fxView = null
        listener = null
    }

    private fun onOutsideTouch(event: MotionEvent) {
        if (FxState.appInForeground) return
        val rx = event.rawX
        val ry = event.rawY
        // Android 12+ may report (0,0) for outside touches from other apps; ignore those.
        if (rx <= 0.5f && ry <= 0.5f) return
        lastOutsideMs = SystemClock.uptimeMillis()
        spawnAtScreen(rx, ry)
    }

    private fun spawnAtScreen(screenX: Float, screenY: Float) {
        val view = fxView ?: return
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        view.engine.config = prefs.config()
        view.tap(screenX - loc[0], screenY - loc[1])
    }
}
