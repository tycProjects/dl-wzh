package com.zewmod.engine.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.zewmod.engine.R
import com.zewmod.engine.ZewModApp
import com.zewmod.engine.bridge.ZewModBridge
import com.zewmod.engine.model.AppConfig
import com.zewmod.engine.ui.MainActivity
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class ZewModVpnService : VpnService() {

    companion object {
        private const val TAG = "ZewModVPN"
        const val ACTION_START = "com.zewmod.engine.VPN_START"
        const val ACTION_STOP  = "com.zewmod.engine.VPN_STOP"
        const val EXTRA_CONFIG = "vpn_config"
        const val NOTIF_ID = 1001
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRunning = false

    // JNI tunnel function
    external fun tunnelStart(fd: Int, ip: String, port: Int, key: String): Boolean
    external fun tunnelStop()
    external fun tunnelIsRunning(): Boolean

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                START_NOT_STICKY
            }
            else -> {
                val config = (applicationContext as ZewModApp).config
                startForeground(NOTIF_ID, buildNotification())
                startVpn(config)
                START_STICKY
            }
        }
    }

    private fun startVpn(config: AppConfig) {
        if (isRunning) return
        serviceScope.launch {
            try {
                vpnInterface = buildVpnInterface(config)
                val fd = vpnInterface!!.fd
                isRunning = true
                Log.i(TAG, "VPN interface created, fd=$fd")

                // Start native engine
                ZewModBridge.start(config)

                // Start native tunnel (passes fd to native lib)
                runCatching {
                    tunnelStart(fd, config.ip, config.port, config.key)
                }

                // Start packet forwarding loop
                runPacketLoop(fd, config)
            } catch (e: Exception) {
                Log.e(TAG, "VPN start error", e)
                stopVpn()
            }
        }
    }

    private fun buildVpnInterface(config: AppConfig): ParcelFileDescriptor {
        val builder = Builder()
            .setSession("ZewMod Engine")
            .addAddress(config.vpnIp, 32)
            .addDnsServer(config.vpnDns)
            .setMtu(config.vpnMtu)
            .setBlocking(true)

        // Route: intercept all traffic
        val (routeIp, routePrefix) = config.vpnRoute.split("/")
        builder.addRoute(routeIp, routePrefix.toInt())

        // Allow bypass for self-team
        builder.allowBypass()

        return builder.establish()
            ?: throw IllegalStateException("VPN establish returned null")
    }

    private suspend fun runPacketLoop(fd: Int, config: AppConfig) {
        val inputStream  = FileInputStream("/proc/self/fd/$fd")
        val outputStream = FileOutputStream("/proc/self/fd/$fd")
        val buffer = ByteBuffer.allocate(32767)

        while (isRunning && !serviceScope.coroutineContext.job.isCancelled) {
            try {
                buffer.clear()
                val len = inputStream.channel.read(buffer)
                if (len <= 0) {
                    delay(1)
                    continue
                }
                buffer.flip()

                // Pass packets to native engine for processing
                // Native engine modifies and routes packets
                // For UDP game packets — modify based on enabled features
                val processed = processPacket(buffer, len)
                if (processed != null) {
                    outputStream.channel.write(processed)
                }
            } catch (e: Exception) {
                if (isRunning) Log.e(TAG, "Packet loop error", e)
                break
            }
        }
    }

    /**
     * Process a raw IP packet.
     * Native engine handles actual modification.
     * This is the Java-side stub — real work done in JNI.
     */
    private fun processPacket(packet: ByteBuffer, len: Int): ByteBuffer? {
        // Native engine has already set up routing via tunnelStart()
        // We just forward unmodified for apps that bypass
        return packet
    }

    private fun stopVpn() {
        isRunning = false
        serviceScope.launch {
            runCatching { tunnelStop() }
            ZewModBridge.stop()
            vpnInterface?.close()
            vpnInterface = null
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.i(TAG, "VPN stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
        serviceScope.cancel()
    }

    override fun onRevoke() {
        super.onRevoke()
        stopVpn()
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, ZewModVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPi = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val openIntent = Intent(this, MainActivity::class.java)
        val openPi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, ZewModApp.CHANNEL_ID_VPN)
            .setContentTitle("ZewMod Engine")
            .setContentText("Engine đang chạy • Nhấn để mở")
            .setSmallIcon(R.drawable.ic_zewmod_notif)
            .setOngoing(true)
            .setContentIntent(openPi)
            .addAction(R.drawable.ic_stop, "Dừng", stopPi)
            .setSilent(true)
            .build()
    }
}
