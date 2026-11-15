package com.zewmod.engine.service

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.core.app.NotificationCompat
import com.zewmod.engine.R
import com.zewmod.engine.ZewModApp
import com.zewmod.engine.bridge.ZewModBridge
import com.zewmod.engine.model.Feature
import com.zewmod.engine.model.FeatureCategory
import com.zewmod.engine.model.ZewModFeatures
import com.zewmod.engine.ui.MainActivity
import kotlinx.coroutines.*

class FloatingService : Service() {

    companion object {
        const val NOTIF_ID = 1002
        const val ACTION_STOP_FLOAT = "com.zewmod.engine.STOP_FLOAT"
        const val ACTION_SHOW = "com.zewmod.engine.SHOW_FLOAT"
        const val ACTION_HIDE = "com.zewmod.engine.HIDE_FLOAT"
    }

    private lateinit var windowManager: WindowManager
    private var floatRoot: View? = null
    private var dragHandle: View? = null
    private var menuPanel: View? = null
    private var isMenuVisible = true
    private var initialX = 0; private var initialY = 0
    private var initialTouchX = 0f; private var initialTouchY = 0f

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // LayoutParams for floating window
    private val params by lazy {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 200
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_FLOAT -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_HIDE -> { hideMenu(); return START_STICKY }
            ACTION_SHOW -> { showMenu(); return START_STICKY }
        }
        startForeground(NOTIF_ID, buildNotification())
        setupFloatingWindow()
        return START_STICKY
    }

    private fun setupFloatingWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        floatRoot = LayoutInflater.from(this)
            .inflate(R.layout.layout_floating_menu, null)

        setupDragHandle()
        setupMenuContent()

        windowManager.addView(floatRoot, params)

        // Entrance animation
        floatRoot?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.float_enter))
    }

    private fun setupDragHandle() {
        dragHandle = floatRoot?.findViewById(R.id.float_drag_handle)
        menuPanel  = floatRoot?.findViewById(R.id.float_menu_panel)

        dragHandle?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatRoot, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // If barely moved: toggle menu
                    val dx = Math.abs(event.rawX - initialTouchX)
                    val dy = Math.abs(event.rawY - initialTouchY)
                    if (dx < 10 && dy < 10) toggleMenu()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupMenuContent() {
        val container = floatRoot?.findViewById<LinearLayout>(R.id.float_feature_container)
            ?: return

        // Add feature rows
        val combatFeatures = ZewModFeatures.getByCategory(FeatureCategory.COMBAT)
        val visionFeatures = ZewModFeatures.getByCategory(FeatureCategory.VISION)

        (combatFeatures + visionFeatures).forEach { feature ->
            val row = createFeatureRow(feature)
            container.addView(row)
        }

        // Close button
        floatRoot?.findViewById<View>(R.id.float_btn_close)?.setOnClickListener {
            stopSelf()
        }

        // Open full UI button
        floatRoot?.findViewById<View>(R.id.float_btn_open_ui)?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        }
    }

    private fun createFeatureRow(feature: Feature): View {
        val row = LayoutInflater.from(this)
            .inflate(R.layout.item_float_feature, null)

        row.findViewById<TextView>(R.id.float_feat_name).text = feature.nameVi

        val toggle = row.findViewById<Switch>(R.id.float_feat_toggle)
        toggle.isChecked = feature.enabled
        toggle.setOnCheckedChangeListener { _, checked ->
            feature.enabled = checked
            serviceScope.launch {
                ZewModBridge.applyAllFeatures()
            }
        }

        return row
    }

    private fun toggleMenu() {
        if (isMenuVisible) hideMenu() else showMenu()
    }

    private fun hideMenu() {
        menuPanel?.visibility = View.GONE
        isMenuVisible = false
    }

    private fun showMenu() {
        menuPanel?.visibility = View.VISIBLE
        isMenuVisible = true
    }

    override fun onDestroy() {
        super.onDestroy()
        floatRoot?.let {
            floatRoot?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.float_exit))
            windowManager.removeView(it)
        }
        floatRoot = null
        serviceScope.cancel()
    }

    private fun buildNotification(): Notification {
        val stopPi = PendingIntent.getService(
            this, 0,
            Intent(this, FloatingService::class.java).apply { action = ACTION_STOP_FLOAT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, ZewModApp.CHANNEL_ID_FLOAT)
            .setContentTitle("ZewMod Menu")
            .setContentText("Menu nổi đang hiển thị")
            .setSmallIcon(R.drawable.ic_zewmod_notif)
            .setOngoing(true)
            .addAction(R.drawable.ic_stop, "Đóng", stopPi)
            .setSilent(true)
            .build()
    }
}
