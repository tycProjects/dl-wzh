package com.example.gditroll

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.DisplayMetrics
import android.widget.Toast
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager

/**
 * Mostra efeitos visuais por cima da tela. Pode capturar a tela (com sua permissão) só para
 * distorcê-la em tempo real; não salva nada, não usa internet e não toca em arquivos. A sobreposição nunca recebe toques.
 * Sem limite de tempo por padrão: fica ativo até o botão PARAR, a ação PARAR da
 * notificação, a tela apagar, ou o aparelho reiniciar.
 */
class GdiOverlayService : Service() {
    companion object {
        const val ACTION_START = "gdi.START"
        const val ACTION_STOP = "gdi.STOP"
        const val EXTRA_EFFECT = "effect"
        const val EXTRA_INTENSITY = "intensity"
        const val EXTRA_SOUND = "soundEnabled"
        const val EXTRA_DURATION_SEC = "durationSec" // 0 = até parar manualmente
        const val EXTRA_RESULT_CODE = "captureResultCode"
        const val EXTRA_RESULT_DATA = "captureResultData"
        private const val CHANNEL = "gdi_overlay"
        private const val NOTIFICATION_ID = 71

        @Volatile var isRunning = false
            private set
    }

    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { stopEverything() }
    private var soundPlayer: MediaPlayer? = null
    private var overlay: EffectView? = null
    private var windowManager: WindowManager? = null
    private var receiverRegistered = false
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { stopEverything() }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopEverything()
            }
            ACTION_START -> {
                if (!enterForeground() || !Settings.canDrawOverlays(this)) {
                    stopEverything()
                    return START_NOT_STICKY
                }
                // Reinício limpo caso já esteja ativo.
                removeOverlay()
                handler.removeCallbacks(autoStop)

                val effect = Effect.from(intent.getIntExtra(EXTRA_EFFECT, 0))
                val intensity = Intensity.from(intent.getIntExtra(EXTRA_INTENSITY, Intensity.NORMAL.ordinal))
                val sound = intent.getBooleanExtra(EXTRA_SOUND, true)
                val duration = intent.getIntExtra(EXTRA_DURATION_SEC, 30)

                if (!showOverlay(effect, intensity)) {
                    stopEverything()
                    return START_NOT_STICKY
                }
                if (effect.needsCapture()) {
                    val code = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                    val data = captureData(intent)
                    if (data == null || !startCapture(code, data)) {
                        Toast.makeText(this, "Não foi possível iniciar a gravação de tela.", Toast.LENGTH_LONG).show()
                        stopEverything()
                        return START_NOT_STICKY
                    }
                }
                if (sound) startSound(effect)
                registerScreenOff()
                if (duration > 0) handler.postDelayed(autoStop, duration * 1000L)
                isRunning = true
            }
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun enterForeground(): Boolean {
        val n = buildNotification()
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            } else {
                startForeground(NOTIFICATION_ID, n)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun captureData(intent: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        else intent.getParcelableExtra(EXTRA_RESULT_DATA)

    /**
     * Captura a tela só para redesenhá-la distorcida, em tempo real. Nada é gravado em
     * arquivo, enviado pela rede ou guardado: cada quadro é copiado para um bitmap e
     * substituído pelo próximo. A captura usa metade da resolução para ser fluida.
     */
    private fun startCapture(resultCode: Int, data: Intent): Boolean {
        return try {
            val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val mp = mpm.getMediaProjection(resultCode, data) ?: return false
            mp.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    if (projection != null) handler.post { stopEverything() }
                }
            }, handler)
            projection = mp

            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            val sw: Int
            val sh: Int
            if (Build.VERSION.SDK_INT >= 30) {
                val b = wm.currentWindowMetrics.bounds
                sw = b.width(); sh = b.height()
            } else {
                val m = DisplayMetrics()
                @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(m)
                sw = m.widthPixels; sh = m.heightPixels
            }
            val cw = ((sw * 0.5f).toInt() and 1.inv()).coerceAtLeast(2)
            val ch = ((sh * 0.5f).toInt() and 1.inv()).coerceAtLeast(2)

            val reader = ImageReader.newInstance(cw, ch, PixelFormat.RGBA_8888, 2)
            reader.setOnImageAvailableListener({ r ->
                val img = try { r.acquireLatestImage() } catch (_: Exception) { null }
                if (img != null) {
                    try { overlay?.updateFrame(img) } finally { img.close() }
                }
            }, handler)
            imageReader = reader
            virtualDisplay = mp.createVirtualDisplay(
                "gdi_capture", cw, ch, resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader.surface, null, handler
            )
            virtualDisplay != null
        } catch (_: Exception) {
            false
        }
    }

    private fun stopCapture() {
        try { virtualDisplay?.release() } catch (_: Exception) {}
        virtualDisplay = null
        try {
            imageReader?.setOnImageAvailableListener(null, null)
            imageReader?.close()
        } catch (_: Exception) {}
        imageReader = null
        val p = projection
        projection = null
        try { p?.stop() } catch (_: Exception) {}
    }

    private fun showOverlay(effect: Effect, intensity: Intensity): Boolean {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm
        val view = EffectView(this, effect, intensity)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        return try {
            wm.addView(view, params)
            overlay = view
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun removeOverlay() {
        try { overlay?.let { windowManager?.removeView(it) } } catch (_: Exception) {}
        overlay = null
    }

    private fun startSound(effect: Effect) {
        try {
            soundPlayer?.release()
            soundPlayer = MediaPlayer.create(this, effect.soundRes)?.apply {
                isLooping = true
                setVolume(0.35f, 0.35f)
                start()
            }
        } catch (_: Exception) {
            soundPlayer?.release()
            soundPlayer = null
        }
    }

    private fun stopSound() {
        try { soundPlayer?.stop() } catch (_: Exception) {}
        soundPlayer?.release()
        soundPlayer = null
    }

    private fun registerScreenOff() {
        if (receiverRegistered) return
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenOff, filter, Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(screenOff, filter)
        receiverRegistered = true
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Box", NotificationManager.IMPORTANCE_LOW)
        )
        val immutable = PendingIntent.FLAG_IMMUTABLE
        val stop = PendingIntent.getService(
            this, 1, Intent(this, GdiOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or immutable
        )
        val open = PendingIntent.getActivity(
            this, 2, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or immutable
        )
        return Notification.Builder(this, CHANNEL)
            .setContentTitle("Box está ativo")
            .setContentText("Efeitos visuais ligados. Toque em PARAR para encerrar.")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_media_pause, "PARAR", stop)
            .setOngoing(true)
            .build()
    }

    private fun stopEverything() {
        isRunning = false
        handler.removeCallbacks(autoStop)
        stopCapture()
        removeOverlay()
        stopSound()
        if (receiverRegistered) {
            try { unregisterReceiver(screenOff) } catch (_: Exception) {}
            receiverRegistered = false
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(autoStop)
        stopCapture()
        removeOverlay()
        stopSound()
        if (receiverRegistered) {
            try { unregisterReceiver(screenOff) } catch (_: Exception) {}
            receiverRegistered = false
        }
        super.onDestroy()
    }
}
