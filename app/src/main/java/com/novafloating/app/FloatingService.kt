package com.novafloating.app

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private lateinit var params: WindowManager.LayoutParams

    private lateinit var tvDragHandle: TextView
    private lateinit var btnScanAndRead: Button
    private lateinit var btnCloseFloating: TextView

    companion object {
        const val CHANNEL_ID = "NovaFloatingChannel"
        const val NOTIFICATION_ID = 5001
        const val ACTION_START = "ACTION_START_NOVA"
        const val ACTION_STOP = "ACTION_STOP_NOVA"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundNotification()

        if (floatingView == null) {
            initFloatingView()
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NovaFloating Reader Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menjaga bilah melayang NovaFloating tetap aktif"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundNotification() {
        val openApp = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openApp,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FloatingService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⚡ NovaFloating Assistive Reader")
            .setContentText("Widget pembaca layar melayang aktif")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Tutup", pendingStop)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("InflateParams")
    private fun initFloatingView() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_bar, null)
        val view = floatingView ?: return

        tvDragHandle = view.findViewById(R.id.tv_drag_handle)
        btnScanAndRead = view.findViewById(R.id.btn_scan_and_solve)
        btnCloseFloating = view.findViewById(R.id.btn_close_floating)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 220
        }

        setupDraggable()
        setupActions()

        windowManager.addView(view, params)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDraggable() {
        tvDragHandle.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        try {
                            floatingView?.let { windowManager.updateViewLayout(it, params) }
                        } catch (e: Exception) {}
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun setupActions() {
        btnScanAndRead.setOnClickListener {
            executeScanAndSearch()
        }

        btnCloseFloating.setOnClickListener {
            Toast.makeText(this, "NovaFloating ditutup", Toast.LENGTH_SHORT).show()
            stopSelf()
        }
    }

    private fun executeScanAndSearch() {
        val extractedText = ScreenReaderAccessibilityService.extractScreenText()?.trim() ?: ""

        val finalQuery = if (extractedText.isNotEmpty()) {
            "Informasi dan pembahasan materi: $extractedText"
        } else {
            "Informasi dan pembahasan materi: artikel edukasi dan referensi pembelajaran"
        }

        Toast.makeText(this, "⚡ Membaca layar & mencari informasi...", Toast.LENGTH_SHORT).show()
        openGoogleSearch(finalQuery)
    }

    private fun openGoogleSearch(query: String) {
        try {
            val encodedQuery = Uri.encode(query)
            val searchUrl = "https://www.google.com/search?q=$encodedQuery"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka browser: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let {
            if (it.isAttachedToWindow) {
                windowManager.removeView(it)
            }
            floatingView = null
        }
    }
}
