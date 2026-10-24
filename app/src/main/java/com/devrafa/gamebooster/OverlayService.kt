package com.devrafa.gamebooster

import android.app.Service
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

class OverlayService : Service() {
    companion object {
        const val ACTION_CURSOR = "com.devrafa.gamebooster.CURSOR"
        const val ACTION_PANEL = "com.devrafa.gamebooster.PANEL"
        const val ACTION_STOP = "com.devrafa.gamebooster.STOP"
    }

    private lateinit var wm: WindowManager
    private var cursorView: View? = null
    private var panelView: View? = null
    private var cursorParams: WindowManager.LayoutParams? = null
    private var panelParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CURSOR -> {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Izin overlay belum diberikan", Toast.LENGTH_SHORT).show()
                    stopSelf()
                    return START_NOT_STICKY
                }
                removeCursor()
                val style = intent.getIntExtra("cursor", 0)
                val size = intent.getFloatExtra("size", 64f)
                val alpha = intent.getFloatExtra("alpha", 90f)
                val thickness = intent.getFloatExtra("thickness", 3f)
                val x = intent.getFloatExtra("x", 0f)
                val y = intent.getFloatExtra("y", 0f)
                val v = CursorOverlayView(this, style, size, alpha / 100f, thickness)
                val p = WindowManager.LayoutParams(
                    dp(size.roundToInt().coerceIn(20, 150)),
                    dp(size.roundToInt().coerceIn(20, 150)),
                    overlayType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                )
                p.gravity = Gravity.CENTER
                p.x = dp(x.roundToInt())
                p.y = dp(y.roundToInt())
                try {
                    wm.addView(v, p)
                    cursorView = v
                    cursorParams = p
                } catch (_: Exception) {
                    Toast.makeText(this, "Overlay gagal ditampilkan. Periksa izin Android.", Toast.LENGTH_LONG).show()
                }
            }
            ACTION_PANEL -> showPanel()
            ACTION_STOP -> {
                removeCursor()
                removePanel()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun showPanel() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Izin overlay belum diberikan", Toast.LENGTH_SHORT).show()
            return
        }
        removePanel()
        val root = FrameLayout(this)
        val tv = TextView(this).apply {
            text = "DRX"
            textSize = 13f
            setTextColor(AndroidColor.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(AndroidColor.WHITE)
                setStroke(dp(2), AndroidColor.LTGRAY)
            }
        }
        root.addView(tv, FrameLayout.LayoutParams(dp(58), dp(58)))
        root.setOnClickListener {
            Toast.makeText(this, "DEV RAFA • gunakan aplikasi untuk mengatur overlay", Toast.LENGTH_SHORT).show()
        }
        val p = WindowManager.LayoutParams(
            dp(58), dp(58), overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        p.gravity = Gravity.TOP or Gravity.START
        p.x = dp(12); p.y = dp(220)
        root.setOnTouchListener(object : View.OnTouchListener {
            var startX = 0
            var startY = 0
            var downX = 0f
            var downY = 0f
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = p.x; startY = p.y
                        downX = event.rawX; downY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        p.x = startX + (event.rawX - downX).roundToInt()
                        p.y = startY + (event.rawY - downY).roundToInt()
                        try { wm.updateViewLayout(root, p) } catch (_: Exception) {}
                        return true
                    }
                    MotionEvent.ACTION_UP -> return true
                }
                return false
            }
        })
        try {
            wm.addView(root, p)
            panelView = root
            panelParams = p
        } catch (_: Exception) {
            Toast.makeText(this, "Floating panel gagal ditampilkan", Toast.LENGTH_LONG).show()
        }
    }

    private fun overlayType() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).roundToInt()

    private fun removeCursor() {
        cursorView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        cursorView = null; cursorParams = null
    }
    private fun removePanel() {
        panelView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        panelView = null; panelParams = null
    }

    override fun onDestroy() {
        removeCursor()
        removePanel()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
