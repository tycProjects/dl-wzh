package com.lenqin.lenai

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager.LayoutParams
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class OverlayService : Service() {
    companion object {
        const val ACTION_START_PROJECTION = "com.lenqin.lenai.START_PROJECTION"
        const val EXTRA_RESULT_CODE = "projection_result_code"
        const val EXTRA_RESULT_DATA = "projection_result_data"
        private const val CHANNEL_ID = "lenai_assistant"
        private const val NOTIFICATION_ID = 2
    }

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var panelOpen = false
    private var projection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: android.hardware.display.VirtualDisplay? = null
    private var captureRequested = false
    private var captureInProgress = false
    private var projectionReady = false
    private var resultText = "LEN AI siap. Buka aplikasi materi, lalu tekan BACA LAYAR."
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        ensureNotificationChannel()
        startAssistantForeground(false)
        showBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START_PROJECTION) {
            val code = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            val data = getProjectionIntent(intent)
            if (code == Activity.RESULT_OK && data != null) startProjection(code, data)
        }
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun getProjectionIntent(intent: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        else intent.getParcelableExtra(EXTRA_RESULT_DATA)

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "LEN AI Assistant", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("LEN AI aktif")
                .setContentText(text)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("LEN AI aktif")
                .setContentText(text)
                .setOngoing(true)
                .build()
        }
    }

    private fun startAssistantForeground(withProjection: Boolean) {
        val notification = buildNotification(
            if (withProjection) "Izin baca layar aktif" else "Floating assistant berjalan"
        )
        if (Build.VERSION.SDK_INT >= 34) {
            val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
                (if (withProjection) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else 0)
            startForeground(NOTIFICATION_ID, notification, type)
        } else if (Build.VERSION.SDK_INT >= 29 && withProjection) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startProjection(resultCode: Int, data: Intent) {
        if (projection != null) {
            showMessage("Sesi baca layar sudah aktif.")
            return
        }
        try {
            startAssistantForeground(true)
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, data)
            projection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    releaseProjectionResources()
                    projectionReady = false
                    captureRequested = false
                    startAssistantForeground(false)
                    showMessage("Sesi baca layar dihentikan Android. Aktifkan lagi dari aplikasi LEN AI.")
                }
            }, mainHandler)

            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)

            val reader = ImageReader.newInstance(
                metrics.widthPixels,
                metrics.heightPixels,
                PixelFormat.RGBA_8888,
                3
            )
            imageReader = reader
            reader.setOnImageAvailableListener({ source ->
                val frame = source.acquireLatestImage() ?: return@setOnImageAvailableListener
                if (captureRequested && !captureInProgress) {
                    captureRequested = false
                    captureInProgress = true
                    processFrame(frame)
                } else {
                    frame.close()
                }
            }, mainHandler)

            virtualDisplay = projection?.createVirtualDisplay(
                "LEN_AI_Persistent_Screen",
                metrics.widthPixels,
                metrics.heightPixels,
                metrics.densityDpi,
                android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                mainHandler
            )
            projectionReady = virtualDisplay != null
            showMessage(if (projectionReady) "Baca layar aktif. Kamu bisa pindah ke Chrome." else "Gagal memulai screen capture.")
            Toast.makeText(this, "LEN AI siap membaca layar aplikasi lain.", Toast.LENGTH_LONG).show()
        } catch (error: Exception) {
            projectionReady = false
            showMessage("Tidak dapat mengaktifkan baca layar: ${error.localizedMessage}")
        }
    }

    private fun windowParams(widthDp: Int, heightDp: Int, focusable: Boolean = false): WindowManager.LayoutParams {
        val width = dp(widthDp)
        val height = if (heightDp < 0) heightDp else dp(heightDp)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            if (focusable) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        return WindowManager.LayoutParams(width, height, type, flags, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(12)
            y = dp(150)
        }
    }

    private fun showBubble() {
        removeFloatingView()
        panelOpen = false
        val bubble = TextView(this).apply {
            text = "✦"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF147DD1.toInt())
            elevation = dp(8).toFloat()
            contentDescription = "Buka LEN AI"
        }
        floatingView = bubble
        try {
            windowManager.addView(bubble, windowParams(48, 48))
        } catch (_: Exception) {
            stopSelf()
            return
        }

        bubble.setOnTouchListener(object : View.OnTouchListener {
            private var downX = 0f
            private var downY = 0f
            private var startX = 0
            private var startY = 0
            private var moved = false
            override fun onTouch(view: View, event: MotionEvent): Boolean {
                val params = view.layoutParams as WindowManager.LayoutParams
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX; downY = event.rawY
                        startX = params.x; startY = params.y; moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - downX).toInt()
                        val dy = (event.rawY - downY).toInt()
                        if (abs(dx) + abs(dy) > dp(6)) moved = true
                        params.x = max(0, startX - dx)
                        params.y = max(0, startY + dy)
                        windowManager.updateViewLayout(view, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) showPanel()
                        return true
                    }
                }
                return true
            }
        })
    }

    private fun showPanel() {
        if (panelOpen) return
        panelOpen = true
        removeFloatingView()

        val screenHeightDp = (resources.displayMetrics.heightPixels / resources.displayMetrics.density).toInt()
        val panelHeight = min(620, max(420, screenHeightDp - 120))

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(14))
            setBackgroundColor(0xF20B1426.toInt())
            elevation = dp(18).toFloat()
        }

        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val brand = TextView(this).apply {
            text = "✦  LEN AI"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
        }
        val minimize = TextView(this).apply {
            text = "—"
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(dp(14), dp(4), dp(8), dp(4))
            contentDescription = "Minimalkan LEN AI"
            setOnClickListener { showBubble() }
        }
        header.addView(brand, LinearLayout.LayoutParams(0, dp(46), 1f))
        header.addView(minimize)

        val status = TextView(this).apply {
            text = if (projectionReady) "● SCREEN READING READY" else "● FLOATING ACTIVE  •  BACA LAYAR BELUM AKTIF"
            textSize = 10f
            setTextColor(0xFF49D6B5.toInt())
            setPadding(0, 0, 0, dp(10))
        }

        val output = TextView(this).apply {
            text = resultText
            textSize = 14f
            setTextColor(0xFFEAF2FF.toInt())
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(0xFF17243A.toInt())
            setTextIsSelectable(true)
        }
        val outputScroll = ScrollView(this).apply {
            isFillViewport = true
            // AUTO-REPAIR: disabled -- unresolved reference 'LayoutParams' (was: addView(output, ScrollView.LayoutParams(-1, -2)))
        }

        val readButton = actionButton("◎   BACA LAYAR SEKARANG") {
            if (!projectionReady) {
                showMessage("Aktifkan IZIN BACA LAYAR dari aplikasi LEN AI terlebih dahulu, lalu kembali ke Chrome.")
            } else if (captureInProgress || captureRequested) {
                showMessage("Sedang membaca layar. Tunggu sebentar...")
            } else {
                resultText = "Panel disembunyikan sebentar untuk mengambil layar aplikasi di belakangnya..."
                output.text = resultText
                captureRequested = true
                // Hide the entire overlay so the screenshot contains the target app, not this panel.
                removeFloatingView()
                panelOpen = false
                mainHandler.postDelayed({
                    if (captureRequested) {
                        showBubble()
                        showPanel()
                        showMessage("Belum menerima frame layar. Coba tekan BACA LAYAR lagi.")
                        captureRequested = false
                    }
                }, 5000)
            }
        }

        val question = EditText(this).apply {
            hint = "Tanyakan sesuatu tentang materi..."
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF8193AF.toInt())
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(0xFF1B2A42.toInt())
        }
        val askButton = actionButton("KIRIM KE GEMINI") {
            val value = question.text.toString().trim()
            if (value.isNotEmpty()) {
                output.text = "Gemini sedang menganalisis..."
                scope.launch {
                    val answer = Gemini(this@OverlayService).ask(value)
                    resultText = answer
                    output.text = answer
                }
            }
        }

        panel.addView(header)
        panel.addView(status)
        panel.addView(outputScroll, LinearLayout.LayoutParams(-1, 0, 1f).apply {
            bottomMargin = dp(10)
        })
        panel.addView(readButton)
        panel.addView(question, LinearLayout.LayoutParams(-1, dp(54)).apply {
            topMargin = dp(9)
        })
        panel.addView(askButton, LinearLayout.LayoutParams(-1, dp(48)).apply {
            topMargin = dp(8)
        })

        floatingView = panel
        try {
            windowManager.addView(panel, windowParams( min(380, (resources.displayMetrics.widthPixels / resources.displayMetrics.density).toInt() - 24), panelHeight, true))
        } catch (_: Exception) {
            panelOpen = false
            showBubble()
        }
    }

    private fun actionButton(label: String, action: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 14f
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF147DD1.toInt())
            setPadding(dp(10), dp(12), dp(10), dp(12))
            setOnClickListener { action() }
        }
    }

    private fun showMessage(message: String) {
        resultText = message
        val panel = floatingView as? LinearLayout ?: return
        val scroll = panel.getChildAt(2) as? ScrollView ?: return
        val output = scroll.getChildAt(0) as? TextView ?: return
        output.text = message
    }

    private fun processFrame(image: Image) {
        try {
            val plane = image.planes[0]
            val paddedWidth = image.width + (plane.rowStride - plane.pixelStride * image.width) / plane.pixelStride
            val bitmap = android.graphics.Bitmap.createBitmap(
                paddedWidth, image.height, android.graphics.Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(plane.buffer)
            val cropped = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            image.close()
            bitmap.recycle()

            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(InputImage.fromBitmap(cropped, 0))
                .addOnSuccessListener { visionText ->
                    val text = visionText.text.trim()
                    if (text.isBlank()) {
                        resultText = "Tidak ada teks yang terbaca. Pastikan soal terlihat jelas dan bukan dari aplikasi yang memblokir tangkapan layar."
                        finishCaptureAndShowPanel()
                    } else {
                        resultText = "Teks berhasil dibaca. Gemini sedang menyusun penjelasan..."
                        showBubble()
                        showPanel()
                        showMessage(resultText)
                        scope.launch {
                            val answer = Gemini(this@OverlayService).ask(
                                """
Bacalah teks hasil OCR berikut. Identifikasi pertanyaan yang ada, lalu jelaskan jawaban dan alasan secara runtut dan mudah dipahami. Jika ada bagian yang tidak terbaca, beri tahu pengguna.

$text
"""
                            )
                            resultText = """
HASIL BACA LAYAR

$text

PEMBAHASAN GEMINI

$answer
""".trimIndent()
                            showMessage(resultText)
                            captureInProgress = false
                        }
                    }
                }
                .addOnFailureListener {
                    resultText = "OCR gagal membaca teks. Coba lagi dengan teks yang lebih jelas."
                    finishCaptureAndShowPanel()
                }
                .addOnCompleteListener {
                    cropped.recycle()
                }
        } catch (error: Exception) {
            try { image.close() } catch (_: Exception) {}
            resultText = "Gagal memproses gambar: ${error.localizedMessage}"
            finishCaptureAndShowPanel()
        }
    }

    private fun finishCaptureAndShowPanel() {
        captureInProgress = false
        showBubble()
        showPanel()
        showMessage(resultText)
    }

    private fun releaseProjectionResources() {
        try { virtualDisplay?.release() } catch (_: Exception) {}
        virtualDisplay = null
        try { imageReader?.setOnImageAvailableListener(null, null); imageReader?.close() } catch (_: Exception) {}
        imageReader = null
    }

    private fun removeFloatingView() {
        floatingView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        floatingView = null
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        scope.cancel()
        try { projection?.stop() } catch (_: Exception) {}
        releaseProjectionResources()
        removeFloatingView()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
