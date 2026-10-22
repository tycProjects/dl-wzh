package com.johan.jugb185.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.johan.jugb185.R
import com.johan.jugb185.core.ArmedStore
import com.johan.jugb185.core.VendorStub
import kotlinx.coroutines.*

class WatchdogService : Service() {
    companion object {
        const val TAG = "JUGB-Watchdog"
        const val NOTIF_ID = 18500
        const val CH_ID = "jugb_turbo"

        // JUGB Turbo: 2.5s vs Armer 5s — 2x faster hold
        const val INTERVAL_MS = 2500L

        fun start(ctx: Context) {
            val i = Intent(ctx, WatchdogService::class.java)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        }
        fun stop(ctx: Context) = ctx.stopService(Intent(ctx, WatchdogService::class.java))
    }

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var vendor: VendorStub
    private lateinit var store: ArmedStore

    override fun onCreate() {
        super.onCreate()
        createNotifChannel()
        vendor = VendorStub()
        store = ArmedStore(this)
        val res = vendor.init()
        Log.i(TAG, "Watchdog init: $res | ${vendor.getInfo()}")

        // Foreground last fix — only vote foreground + others
        job = scope.launch {
            store.armedFlow.collect { armed ->
                while (isActive) {
                    if (armed.isNotEmpty()) {
                        // JUGB improvement: foreground-last
                        val fg = getForegroundPackage()
                        val ordered = armed.toList().sortedBy { it.first.lowercase() }.toMutableList()
                        // Move fg to last so its vote wins
                        fg?.let { pkg ->
                            val idx = ordered.indexOfFirst { it.first == pkg }
                            if (idx >= 0) {
                                val item = ordered.removeAt(idx)
                                ordered.add(item)
                            }
                        }
                        ordered.forEach { (pkg, vid) ->
                            val ok = vendor.request(pkg, vid)
                            Log.d(TAG, "vote $pkg -> $vid ok=$ok fg=$fg")
                        }
                    }
                    delay(INTERVAL_MS)
                    // Thermal guard: if hot, log but don't spam
                    if (isThermalThrottled()) {
                        Log.w(TAG, "Thermal guard: device hot, votes still hold")
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notif = NotificationCompat.Builder(this, CH_ID)
            .setContentTitle("JUGB 185 • TURBO ACTIVE ⚡")
            .setContentText("185Hz locked • 2.5s watchdog • OnePlus 16 God Mode")
            .setSmallIcon(android.R.drawable.btn_star_big_on)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        job?.cancel()
        scope.cancel()
        super.onDestroy()
        // Auto-restart — locked-in
        if (Build.VERSION.SDK_INT >= 26) {
            try { start(this) } catch (_: Exception) {}
        }
    }

    private fun createNotifChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CH_ID, "JUGB Turbo", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Keeps 185Hz locked"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun getForegroundPackage(): String? {
        return try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            val now = System.currentTimeMillis()
            val stats = usm.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, now - 10000, now)
            stats?.maxByOrNull { it.lastTimeUsed }?.packageName
        } catch (_: Exception) { null }
    }

    private fun isThermalThrottled(): Boolean {
        return try {
            val temp = (getSystemService(Context.POWER_SERVICE) as PowerManager).currentThermalStatus
            temp >= PowerManager.THERMAL_STATUS_SEVERE
        } catch (_: Exception) { false }
    }
}
