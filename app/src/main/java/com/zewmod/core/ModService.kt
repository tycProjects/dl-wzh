package com.zewmod.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.zewmod.hack.HackState
import com.zewmod.hack.ProxyRelay
import com.zewmod.net.ConfigManager
import com.zewmod.ui.OverlayRenderer
import kotlinx.coroutines.*

/**
 * ZewMod — ModService v2
 * Shizuku → WirelessAdbBridge hoàn toàn
 */
class ModService : Service() {

    companion object {
        private const val TAG = "ZewMod.Service"
        private const val NOTIF_CHANNEL_ID = "zewmod_ch"
        private const val NOTIF_ID = 1
        const val ACTION_START = "com.zewmod.START"
        const val ACTION_STOP  = "com.zewmod.STOP"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var proxyRelay: ProxyRelay? = null
    private var overlay: OverlayRenderer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("ZewMod active"))

        // Kết nối ADB qua WiFi — không cần Shizuku
        scope.launch {
            val ok = WirelessAdbBridge.autoConnect()
            Log.i(TAG, "Wireless ADB: ${if (ok) "connected" else "not available"}")
        }

        scope.launch { ConfigManager(this@ModService).fetchAndApply() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMod()
            ACTION_STOP  -> { stopMod(); stopSelf() }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopMod()
        scope.cancel()
        WirelessAdbBridge.disconnect()
    }

    private fun startMod() {
        // Inject patch qua Wireless ADB
        scope.launch {
            val patcher = ILFixPatch(this@ModService)
            val patch   = patcher.loadBundledPatch()
            val pkg     = targetGamePackage
            if (pkg != null && WirelessAdbBridge.isConnected) {
                WirelessAdbBridge.injectPatchToGame(patch, pkg)
            }
        }

        // ESP overlay
        overlay = OverlayRenderer(this)
        overlay?.show()

        // Proxy relay
        if (HackState.proxy.enabled) {
            proxyRelay = ProxyRelay(HackState.proxy).also { it.start() }
        }

        Log.i(TAG, "ZewMod started")
    }

    private fun stopMod() {
        overlay?.hide(); overlay = null
        proxyRelay?.stop(); proxyRelay = null
        HackState.reset()
        Log.i(TAG, "ZewMod stopped")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                NOTIF_CHANNEL_ID, "ZewMod",
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    private fun buildNotification(text: String): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NOTIF_CHANNEL_ID)
                .setContentTitle("ZewMod").setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_manage).setOngoing(true).build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("ZewMod").setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_manage).setOngoing(true).build()
        }
    }
}

var targetGamePackage: String? = null
