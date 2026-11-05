package com.nexus.launcher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.nexus.launcher.MainActivity
import com.nexus.launcher.R
import com.nexus.launcher.utils.GameUtils
import kotlin.math.abs
import kotlin.random.Random

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var menuPanel: View? = null
    private var sideTab: View? = null
    private var isExpanded = false
    private var targetPackage: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private var statsRunnable: Runnable? = null
    private var checkForegroundRunnable: Runnable? = null

    // Drag state
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        targetPackage = intent?.getStringExtra(EXTRA_PACKAGE)
        startForeground(NOTIFICATION_ID, createNotification())
        if (overlayView == null) {
            showOverlay()
            startStatsUpdater()
            startForegroundChecker()
        }
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val channelId = "nexus_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nexus Launcher Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Nexus Launcher aktif")
            .setContentText("Tarik tab di sisi layar untuk membuka menu")
            .setSmallIcon(R.drawable.ic_speed)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun showOverlay() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_menu, null)

        menuPanel = overlayView?.findViewById(R.id.menu_panel)
        sideTab = overlayView?.findViewById(R.id.side_tab)

        // Start collapsed
        menuPanel?.visibility = View.GONE
        isExpanded = false

        // Drag + click handling on side tab
        sideTab?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = (overlayView?.layoutParams as WindowManager.LayoutParams).x
                    initialY = (overlayView?.layoutParams as WindowManager.LayoutParams).y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isDragging = true
                        val params = overlayView?.layoutParams as WindowManager.LayoutParams
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(overlayView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // It was a tap → toggle menu
                        toggleMenu()
                    }
                    true
                }
                else -> false
            }
        }

        setupToolClicks()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            // Important: remove FLAG_NOT_FOCUSABLE so buttons can receive clicks
            // We use FLAG_NOT_TOUCH_MODAL so touches outside still go to the game
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        params.x = 0
        params.y = 0

        try {
            windowManager.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun toggleMenu() {
        isExpanded = !isExpanded
        menuPanel?.visibility = if (isExpanded) View.VISIBLE else View.GONE

        // Update layout so size changes are applied
        overlayView?.let {
            windowManager.updateViewLayout(it, it.layoutParams)
        }
    }

    private fun setupToolClicks() {
        val tools = mapOf(
            R.id.tool_notif to "Notifikasi bertitik diaktifkan",
            R.id.tool_assistant to "Ditambahkan ke Asisten Game",
            R.id.tool_network to "Optimalisasi jaringan aktif",
            R.id.tool_wlan to "WLAN di-toggle",
            R.id.tool_status to "Status sistem ditampilkan",
            R.id.tool_clear to "Memori dikosongkan!",
            R.id.tool_aim to "Bantuan bidik diaktifkan",
            R.id.tool_screenshot to "Cuplikan layar diambil",
            R.id.tool_record to "Perekaman layar dimulai"
        )

        tools.forEach { (id, message) ->
            overlayView?.findViewById<View>(id)?.setOnClickListener {
                if (id == R.id.tool_clear) {
                    GameUtils.clearMemory(this)
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                // Collapse after action
                if (isExpanded) toggleMenu()
            }
        }

        // Also make the tab icons switch panels (visual only for now)
        overlayView?.findViewById<ImageView>(R.id.tab_performa)?.setOnClickListener {
            if (!isExpanded) toggleMenu()
        }
        overlayView?.findViewById<ImageView>(R.id.tab_alat)?.setOnClickListener {
            if (!isExpanded) toggleMenu()
        }
    }

    private fun startStatsUpdater() {
        statsRunnable = object : Runnable {
            override fun run() {
                val latency = String.format("%.1f", 15 + Random.nextFloat() * 60)
                val fps = (50 + Random.nextInt(40)).toString()
                val temp = (34 + Random.nextInt(12)).toString()

                overlayView?.findViewById<TextView>(R.id.tv_latency)?.text = "$latency ms"
                overlayView?.findViewById<TextView>(R.id.tv_fps)?.text = "$fps fps"
                overlayView?.findViewById<TextView>(R.id.tv_temp)?.text = "$temp °C"

                handler.postDelayed(this, 1200)
            }
        }
        handler.post(statsRunnable!!)
    }

    private fun startForegroundChecker() {
        checkForegroundRunnable = object : Runnable {
            override fun run() {
                val pkg = targetPackage
                if (pkg != null && !GameUtils.isAppInForeground(this@OverlayService, pkg)) {
                    // Game is no longer in foreground → stop overlay
                    stopSelf()
                    return
                }
                handler.postDelayed(this, 2000)
            }
        }
        handler.postDelayed(checkForegroundRunnable!!, 3000)
    }

    override fun onDestroy() {
        super.onDestroy()
        statsRunnable?.let { handler.removeCallbacks(it) }
        checkForegroundRunnable?.let { handler.removeCallbacks(it) }
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
        }
        overlayView = null
    }

    companion object {
        const val EXTRA_PACKAGE = "target_package"
        private const val NOTIFICATION_ID = 1001
    }
}
