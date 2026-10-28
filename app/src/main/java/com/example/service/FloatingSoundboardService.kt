package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.SoundManager

class FloatingSoundboardService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var soundManager: SoundManager? = null

    companion object {
        const val CHANNEL_ID = "zonex_floating_soundboard"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "com.example.STOP_FLOATING_SERVICE"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        soundManager = SoundManager(applicationContext)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        if (Settings.canDrawOverlays(this)) {
            initOverlayView()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ZONEX Floating Soundboard",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground overlay service for gaming soundboard quick access"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ZONEX VOICE Floating Active")
            .setContentText("Tap to open soundboard controls")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun initOverlayView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        // Programmatically build overlay UI: a sleek glowing draggable pill
        val container = FrameLayout(this).apply {
            setBackgroundColor(0xCC090812.toInt())
            setPadding(16, 16, 16, 16)
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val logoIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_zonex_logo)
            val iconParams = LinearLayout.LayoutParams(60, 60).apply {
                marginEnd = 16
            }
            setLayoutParams(iconParams)
        }

        val title = TextView(this).apply {
            text = "ZONEX"
            setTextColor(0xFF06B6D4.toInt())
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // Quick sound trigger button 1
        val btnHeadshot = TextView(this).apply {
            text = "🎯 Snipe"
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF7C3AED.toInt())
            setPadding(14, 8, 14, 8)
            setOnClickListener {
                soundManager?.playSound("overlay_snipe", "synth://headshot")
            }
        }

        // Quick sound trigger button 2
        val btnAirhorn = TextView(this).apply {
            text = "📢 Horn"
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF3B82F6.toInt())
            setPadding(14, 8, 14, 8)
            setOnClickListener {
                soundManager?.playSound("overlay_horn", "synth://airhorn")
            }
        }

        // Stop button
        val btnStop = TextView(this).apply {
            text = "⏹ Stop"
            setTextColor(0xFFEF4444.toInt())
            setPadding(14, 8, 14, 8)
            setOnClickListener {
                soundManager?.stopAll()
            }
        }

        // Close overlay button
        val btnClose = TextView(this).apply {
            text = "✖"
            setTextColor(0xFF94A3B8.toInt())
            setPadding(12, 8, 12, 8)
            setOnClickListener {
                stopSelf()
            }
        }

        row.addView(logoIcon)
        row.addView(title)
        row.addView(btnHeadshot)
        row.addView(btnAirhorn)
        row.addView(btnStop)
        row.addView(btnClose)
        container.addView(row)

        floatingView = container

        // Drag listener
        container.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(floatingView, layoutParams)
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager?.addView(floatingView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            try {
                windowManager?.removeView(floatingView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        soundManager?.release()
    }
}
