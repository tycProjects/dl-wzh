package com.ahnaf.ff

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class OverlayService : Service() {

    companion object {
        const val EXTRA_CROSSHAIR = "crosshair"
        const val EXTRA_STATS = "stats"
        private const val CHANNEL = "ahnaf_ff"
    }

    private lateinit var wm: WindowManager
    private var crosshair: View? = null
    private var statsView: TextView? = null
    private val handler = Handler(Looper.getMainLooper())

    private val tick = object : Runnable {
        override fun run() {
            statsView?.text = StatsReader.read(this@OverlayService)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        setCrosshair(intent?.getBooleanExtra(EXTRA_CROSSHAIR, false) == true)
        setStats(intent?.getBooleanExtra(EXTRA_STATS, false) == true)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        setCrosshair(false)
        setStats(false)
        super.onDestroy()
    }

    private fun params(gravity: Int, w: Int, h: Int) = WindowManager.LayoutParams(
        w, h,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply { this.gravity = gravity }

    private fun setCrosshair(show: Boolean) {
        if (show && crosshair == null) {
            val size = (70 * resources.displayMetrics.density).toInt()
            val v = CrosshairView(this)
            wm.addView(v, params(Gravity.CENTER, size, size))
            crosshair = v
        } else if (!show && crosshair != null) {
            wm.removeView(crosshair)
            crosshair = null
        }
    }

    private fun setStats(show: Boolean) {
        if (show && statsView == null) {
            val d = resources.displayMetrics.density
            val tv = TextView(this).apply {
                setTextColor(Color.GREEN)
                setBackgroundColor(Color.argb(140, 0, 0, 0))
                textSize = 11f
                setPadding((6 * d).toInt(), (4 * d).toInt(), (6 * d).toInt(), (4 * d).toInt())
            }
            wm.addView(tv, params(Gravity.TOP or Gravity.START, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT))
            statsView = tv
            handler.post(tick)
        } else if (!show && statsView != null) {
            handler.removeCallbacks(tick)
            wm.removeView(statsView)
            statsView = null
        }
    }

    private fun startAsForeground() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "AHNAF FF", NotificationManager.IMPORTANCE_LOW))
        val n: Notification = Notification.Builder(this, CHANNEL)
            .setContentTitle("AHNAF FF")
            .setContentText("Overlay running")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }
}
