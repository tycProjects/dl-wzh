package com.example.screencapturenative

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingService : Service() {

    companion object {
        const val ACTION_START = "com.example.screenrecorder.START"
        const val ACTION_STOP = "com.example.screenrecorder.STOP"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_QUALITY = "extra_quality"

        private const val TAG = "RecordingService"
        private const val CHANNEL_ID = "recording_channel"
        private const val NOTIFICATION_ID = 1001

        @Volatile var isRunning = false
            private set

        /** Latest state, readable by the Flutter side. */
        @Volatile var state: Map<String, Any?> = mapOf("recording" to false)
            private set

        /** Called on the main thread whenever state changes. */
        @Volatile var listener: ((Map<String, Any?>) -> Unit)? = null
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var session: RecordingSession? = null
    private var density = 320
    private var quality: String? = "auto"
    private var baseName = ""
    private var part = 1
    private var startedAtMs = 0L
    private var stopping = false
    private var rotating = false
    private var thermalFactor = 1.0f
    private var displayListener: DisplayManager.DisplayListener? = null
    private var thermalListener: PowerManager.OnThermalStatusChangedListener? = null

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() { stopCapture() }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (isRunning || stopping) return START_NOT_STICKY
                val code = intent.getIntExtra(EXTRA_RESULT_CODE, Int.MIN_VALUE)
                val data: Intent? = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                quality = intent.getStringExtra(EXTRA_QUALITY)
                startAsForeground("Starting…")
                if (code == Activity.RESULT_OK && data != null) {
                    beginCapture(code, data)
                } else {
                    toast("Screen capture permission was not granted")
                    finishService()
                }
            }
            ACTION_STOP -> stopCapture()
        }
        return START_NOT_STICKY
    }

    // ---------- start ----------

    private fun beginCapture(code: Int, data: Intent) {
        try {
            val pm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = pm.getMediaProjection(code, data)
                ?: throw IllegalStateException("Could not create MediaProjection")
            projection!!.registerCallback(projectionCallback, mainHandler)

            density = resources.displayMetrics.densityDpi
            baseName = "Recording_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            part = 1

            val s = openSession(RecorderPlanner.candidates(this, quality), fileNameFor(1))
                ?: throw IllegalStateException("No supported video encoder configuration")
            session = s

            virtualDisplay = projection!!.createVirtualDisplay(
                "ScreenRecorder",
                s.plan.width, s.plan.height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                s.surface, null, mainHandler
            ) ?: throw IllegalStateException("Could not create virtual display")

            startedAtMs = System.currentTimeMillis()
            isRunning = true
            publish(null)
            updateNotification("Recording • ${s.plan.summary}")
            registerWatchers()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            toast("Recording failed: ${e.message ?: e.javaClass.simpleName}")
            publish("Recording failed: ${e.message ?: e.javaClass.simpleName}")
            stopCapture()
        }
    }

    private fun fileNameFor(p: Int) = if (p == 1) "$baseName.mp4" else "${baseName}_part$p.mp4"

    /** Tries each plan in order until one encoder accepts it. */
    private fun openSession(plans: List<Plan>, fileName: String): RecordingSession? {
        for (plan in plans) {
            try {
                val s = RecordingSession(this, plan, fileName)
                s.start()
                if (thermalFactor < 1f) s.setBitrate((plan.bitrate * thermalFactor).toInt())
                return s
            } catch (e: Throwable) {
                Log.w(TAG, "Plan failed (${plan.encoderName} ${plan.summary}), trying next", e)
            }
        }
        return null
    }

    // ---------- rotation + thermal ----------

    private fun registerWatchers() {
        val dm = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {}
            override fun onDisplayRemoved(displayId: Int) {}
            override fun onDisplayChanged(displayId: Int) {
                if (displayId == 0) checkOrientation()
            }
        }
        dm.registerDisplayListener(displayListener, mainHandler)

        if (Build.VERSION.SDK_INT >= 29) {
            val pwr = getSystemService(Context.POWER_SERVICE) as PowerManager
            thermalListener = PowerManager.OnThermalStatusChangedListener { status ->
                thermalFactor = when {
                    status >= PowerManager.THERMAL_STATUS_CRITICAL -> 0.4f
                    status >= PowerManager.THERMAL_STATUS_SEVERE -> 0.6f
                    status >= PowerManager.THERMAL_STATUS_MODERATE -> 0.8f
                    else -> 1.0f
                }
                session?.let { it.setBitrate((it.plan.bitrate * thermalFactor).toInt()) }
            }
            pwr.addThermalStatusListener(mainExecutor, thermalListener!!)
        }
    }

    private fun unregisterWatchers() {
        try {
            displayListener?.let {
                (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).unregisterDisplayListener(it)
            }
        } catch (_: Exception) { }
        displayListener = null
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                thermalListener?.let {
                    (getSystemService(Context.POWER_SERVICE) as PowerManager).removeThermalStatusListener(it)
                }
            } catch (_: Exception) { }
        }
        thermalListener = null
    }

    /**
     * When the phone rotates the encoder size must change, which an encoder
     * cannot do live. So we start a fresh encoder + file on the same virtual
     * display and finish the old one in the background (no dropped frames).
     */
    private fun checkOrientation() {
        val cur = session ?: return
        if (!isRunning || stopping || rotating) return
        val real = RecorderPlanner.displaySize(this)
        val nowLandscape = real.width > real.height
        val wasLandscape = cur.plan.width > cur.plan.height
        if (nowLandscape == wasLandscape) return

        rotating = true
        try {
            val next = openSession(RecorderPlanner.candidates(this, quality), fileNameFor(part + 1))
            if (next == null) { return }
            part++
            val vd = virtualDisplay
            if (vd == null) { Thread { next.abort() }.start(); return }
            vd.resize(next.plan.width, next.plan.height, density)
            vd.surface = next.surface
            session = next
            Thread({ cur.finish() }, "Rec-FinishPart").start()
            updateNotification("Recording • ${next.plan.summary}")
            publish(null)
        } catch (e: Exception) {
            Log.e(TAG, "Rotation switch failed", e)
        } finally {
            rotating = false
        }
    }

    // ---------- stop ----------

    private fun stopCapture() {
        if (stopping) return
        stopping = true
        isRunning = false
        unregisterWatchers()
        try { projection?.unregisterCallback(projectionCallback) } catch (_: Exception) { }

        // Stop producing frames first, then finish the file off the main thread.
        try { virtualDisplay?.release() } catch (_: Exception) { }
        virtualDisplay = null
        val s = session
        session = null
        val proj = projection
        projection = null
        updateNotification("Saving…")

        Thread({
            try { s?.finish() } catch (e: Exception) { Log.e(TAG, "finish failed", e) }
            mainHandler.post {
                try { proj?.stop() } catch (_: Exception) { }
                publish(null, saved = s?.displayName)
                finishService()
            }
        }, "Rec-Finalize").start()
    }

    private fun finishService() {
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE)
        else @Suppress("DEPRECATION") stopForeground(true)
        stopping = false
        stopSelf()
    }

    // ---------- state + notification ----------

    private fun publish(error: String?, saved: String? = null) {
        val p = session?.plan
        val map = HashMap<String, Any?>()
        map["recording"] = isRunning
        map["startedAt"] = startedAtMs
        map["summary"] = p?.summary
        map["part"] = part
        map["error"] = error
        map["saved"] = saved
        state = map
        listener?.let { l -> mainHandler.post { l(map) } }
    }

    private fun toast(msg: String) {
        mainHandler.post { Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show() }
    }

    private fun startAsForeground(text: String) {
        createChannel()
        val n = buildNotification(text)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, n)
        }
    }

    private fun updateNotification(text: String) {
        try {
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(text))
        } catch (_: Exception) { }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            // IMPORTANCE_MIN: no status-bar icon, no sound, no pop-up. Android still
            // requires the notification to exist while recording; it sits quietly
            // in the shade.
            val ch = NotificationChannel(CHANNEL_ID, "Screen recording", NotificationManager.IMPORTANCE_MIN).apply {
                description = "Shown while a recording is running"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(text: String): Notification {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val stop = PendingIntent.getService(
            this, 1, Intent(this, RecordingService::class.java).setAction(ACTION_STOP), flags)
        val launch = packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP) ?: Intent()
        val open = PendingIntent.getActivity(this, 2, launch, flags)
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL_ID)
        else @Suppress("DEPRECATION") Notification.Builder(this)
        b.setSmallIcon(android.R.drawable.presence_video_online)
            .setContentTitle("Screen recorder")
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
        @Suppress("DEPRECATION") b.addAction(0, "Stop", stop)
        if (Build.VERSION.SDK_INT < 26) {
            @Suppress("DEPRECATION") b.setPriority(Notification.PRIORITY_MIN)
        }
        return b.build()
    }

    override fun onDestroy() {
        if (isRunning && !stopping) stopCapture()
        super.onDestroy()
    }
}
