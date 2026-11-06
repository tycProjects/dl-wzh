package com.supra.turbo

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

/** Chờ Free Fire mở → bật phiên chơi (dọn nền, tắt hiệu ứng, tăng nét). Thoát game → khôi phục. */
class SupraService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastPkg: String? = null
    private var lastTs = System.currentTimeMillis() - 3000
    @Volatile private var active = false

    private val poll = object : Runnable {
        override fun run() { check(); handler.postDelayed(this, 500) }
    }

    override fun onCreate() {
        super.onCreate()
        val ch = "supra_ch"
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(ch, "Supra Turbo", NotificationManager.IMPORTANCE_LOW))
        val n = NotificationCompat.Builder(this, ch)
            .setContentTitle("Supra Turbo đang chờ Free Fire")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(PendingIntent.getActivity(this, 0,
                Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            .build()
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
        handler.post(poll)
    }

    private fun foregroundPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val ev = usm.queryEvents(lastTs - 300, now)
        lastTs = now
        val e = UsageEvents.Event()
        var pkg: String? = null
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) pkg = e.packageName
        }
        return pkg
    }

    private fun ignored(pkg: String) =
        pkg.startsWith("com.android.systemui") || pkg.contains("inputmethod") ||
        pkg.contains("keyboard") || pkg == "com.android.permissioncontroller"

    private fun check() {
        val pkg = foregroundPackage() ?: return
        if (pkg == lastPkg || ignored(pkg)) return
        lastPkg = pkg
        val ctx = applicationContext
        if (!SupraEngine.hasShizuku() || !SupraEngine.sp(ctx).getBoolean("enabled", false)) return
        val isFF = pkg in SupraEngine.FREE_FIRE
        if (isFF && !active) { active = true; SupraEngine.async { SupraEngine.sessionStart(ctx) } }
        else if (!isFF && active && pkg != packageName) { active = false; SupraEngine.async { SupraEngine.sessionEnd(ctx) } }
    }

    override fun onDestroy() {
        handler.removeCallbacks(poll)
        if (active) { val c = applicationContext; SupraEngine.async { SupraEngine.sessionEnd(c) } }
        super.onDestroy()
    }

    override fun onBind(i: Intent?): IBinder? = null
}
