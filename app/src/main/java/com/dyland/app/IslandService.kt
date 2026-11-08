package com.dyland.app
import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.*
import android.view.*

class IslandService : Service() {
    companion object { var running = false }
    private lateinit var wm: WindowManager
    private var view: IslandView? = null
    private lateinit var lp: WindowManager.LayoutParams
    override fun onBind(i: Intent?): IBinder? = null
    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY
    private val rx = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            val on = i.action == Intent.ACTION_POWER_CONNECTED
            Bus.post(Ev(if (on) "Đang sạc" else "Đã rút sạc", "", c.getDrawable(android.R.drawable.ic_lock_idle_charging), tint = true))
        }
    }
    override fun onCreate() {
        super.onCreate(); running = true
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("d", "Dyland", NotificationManager.IMPORTANCE_MIN))
        val n = Notification.Builder(this, "d").setSmallIcon(R.drawable.ic_stat).setContentTitle("Dyland").setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(1, n)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val d = resources.displayMetrics.density
        lp = WindowManager.LayoutParams((150 * d).toInt(), (64 * d).toInt(), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = (Prefs.top(this@IslandService) * d).toInt() }
        view = IslandView(this) { w, h -> lp.width = (w * d).toInt(); lp.height = (h * d).toInt(); view?.let { wm.updateViewLayout(it, lp) } }
        wm.addView(view, lp)
        Bus.sink = { view?.show(it) }
        registerReceiver(rx, IntentFilter().apply { addAction(Intent.ACTION_POWER_CONNECTED); addAction(Intent.ACTION_POWER_DISCONNECTED) })
    }
    override fun onDestroy() {
        running = false; Bus.sink = null
        try { unregisterReceiver(rx) } catch (e: Exception) {}
        view?.let { wm.removeView(it) }; view = null
        super.onDestroy()
    }
}
