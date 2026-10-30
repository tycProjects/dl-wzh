package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.crypto.CryptoEngine
import com.example.localization.LanguageManager
import java.nio.charset.StandardCharsets

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var clipboardManager: ClipboardManager

    private var bubbleView: View? = null
    private var windowView: View? = null

    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var windowParams: WindowManager.LayoutParams

    private var currentTabIsEncode = true
    private var selectedCarrierEmoji = "🤔"

    companion object {
        const val ACTION_START = "ACTION_START_FLOATING_SERVICE"
        const val ACTION_STOP = "ACTION_STOP_FLOATING_SERVICE"
        const val ACTION_EXPAND = "ACTION_EXPAND_WINDOW"
        const val ACTION_MINIMIZE = "ACTION_MINIMIZE_WINDOW"

        const val CHANNEL_ID = "zonex_emoji_overlay_channel"
        const val NOTIFICATION_ID = 1001

        @Volatile
        var isServiceRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        sharedPrefs = getSharedPreferences("zonex_emoji_prefs", Context.MODE_PRIVATE)

        createNotificationChannel()
        startAsForeground()

        initBubbleView()
        initWindowView()

        showWindow()
        isServiceRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_EXPAND -> showWindow()
            ACTION_MINIMIZE -> showBubble()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Zonex Emoji Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Zonex Emoji single emoji floating overlay"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startAsForeground() {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FloatingWindowService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Zonex Emoji Floating Window")
            .setContentText("Single emoji secret overlay active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingOpenApp)
            .addAction(0, "Close", pendingStop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // ==========================================
    // FLOATING BUBBLE
    // ==========================================

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun initBubbleView() {
        bubbleView = LayoutInflater.from(this).inflate(R.layout.layout_floating_bubble, null)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = sharedPrefs.getInt("bubble_x", 40)
            y = sharedPrefs.getInt("bubble_y", 250)
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var touchStartTime = 0L

        bubbleView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleParams.x
                    initialY = bubbleParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    touchStartTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    bubbleParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    bubbleParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        windowManager.updateViewLayout(bubbleView, bubbleParams)
                    } catch (_: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaX = Math.abs(event.rawX - initialTouchX)
                    val deltaY = Math.abs(event.rawY - initialTouchY)
                    val duration = System.currentTimeMillis() - touchStartTime
                    if (deltaX < 15 && deltaY < 15 && duration < 300) {
                        showWindow()
                    } else {
                        sharedPrefs.edit()
                            .putInt("bubble_x", bubbleParams.x)
                            .putInt("bubble_y", bubbleParams.y)
                            .apply()
                    }
                    true
                }
                else -> false
            }
        }
    }

    // ==========================================
    // EXPANDED FLOATING WINDOW
    // ==========================================

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun initWindowView() {
        windowView = LayoutInflater.from(this).inflate(R.layout.layout_floating_window, null)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = sharedPrefs.getInt("window_x", 30)
            y = sharedPrefs.getInt("window_y", 150)
        }

        setupWindowControls()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupWindowControls() {
        val root = windowView ?: return

        val headerBar = root.findViewById<LinearLayout>(R.id.header_bar)
        val tvTitle = root.findViewById<TextView>(R.id.tv_window_title)
        val btnMinimize = root.findViewById<TextView>(R.id.btn_minimize)
        val btnClose = root.findViewById<TextView>(R.id.btn_close)
        val btnLangToggle = root.findViewById<TextView>(R.id.btn_lang_toggle)

        val tabEncode = root.findViewById<TextView>(R.id.tab_encode)
        val tabDecode = root.findViewById<TextView>(R.id.tab_decode)

        val layoutEncode = root.findViewById<LinearLayout>(R.id.layout_encode_container)
        val layoutDecode = root.findViewById<LinearLayout>(R.id.layout_decode_container)

        // Encode Views
        val etInputEncode = root.findViewById<EditText>(R.id.et_input_encode)
        val tvByteCounter = root.findViewById<TextView>(R.id.tv_byte_counter)
        val btnPasteEncode = root.findViewById<Button>(R.id.btn_paste_encode)
        val btnClearEncode = root.findViewById<Button>(R.id.btn_clear_encode)

        val btnQuickEmoji1 = root.findViewById<TextView>(R.id.btn_quick_emoji_1)
        val btnQuickEmoji2 = root.findViewById<TextView>(R.id.btn_quick_emoji_2)
        val btnQuickEmoji3 = root.findViewById<TextView>(R.id.btn_quick_emoji_3)
        val btnQuickEmoji4 = root.findViewById<TextView>(R.id.btn_quick_emoji_4)
        val etCarrierEmoji = root.findViewById<EditText>(R.id.et_carrier_emoji)

        val btnExecuteEncode = root.findViewById<Button>(R.id.btn_execute_encode)
        val tvResultEncode = root.findViewById<TextView>(R.id.tv_result_encode)
        val btnCopyEncode = root.findViewById<Button>(R.id.btn_copy_encode)
        val btnShareEncode = root.findViewById<Button>(R.id.btn_share_encode)

        // Decode Views
        val etInputDecode = root.findViewById<EditText>(R.id.et_input_decode)
        val tvDecodeStatus = root.findViewById<TextView>(R.id.tv_decode_status)
        val btnPasteDecode = root.findViewById<Button>(R.id.btn_paste_decode)
        val btnClearDecode = root.findViewById<Button>(R.id.btn_clear_decode)
        val btnExecuteDecode = root.findViewById<Button>(R.id.btn_execute_decode)
        val tvResultDecode = root.findViewById<TextView>(R.id.tv_result_decode)
        val btnCopyDecode = root.findViewById<Button>(R.id.btn_copy_decode)

        // Dragging window via header bar
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        headerBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = windowParams.x
                    initialY = windowParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    windowParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    windowParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        windowManager.updateViewLayout(windowView, windowParams)
                    } catch (_: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    sharedPrefs.edit()
                        .putInt("window_x", windowParams.x)
                        .putInt("window_y", windowParams.y)
                        .apply()
                    true
                }
                else -> false
            }
        }

        btnMinimize.setOnClickListener { showBubble() }
        btnClose.setOnClickListener { stopSelf() }

        // Keyboard Focus Handler
        val focusListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                windowParams.flags = windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            } else {
                windowParams.flags = windowParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
            try {
                windowManager.updateViewLayout(windowView, windowParams)
            } catch (_: Exception) {}
        }
        etInputEncode.onFocusChangeListener = focusListener
        etCarrierEmoji.onFocusChangeListener = focusListener
        etInputDecode.onFocusChangeListener = focusListener

        // Byte Counter Watcher
        etInputEncode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val bytes = s?.toString()?.toByteArray(StandardCharsets.UTF_8)?.size ?: 0
                tvByteCounter.text = "$bytes B"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // ==========================================
        // SMOOTH TAB SWITCHING WITH CROSSFADE ANIMATION
        // ==========================================
        fun switchTabAnimated(toEncode: Boolean) {
            if (currentTabIsEncode == toEncode) return
            currentTabIsEncode = toEncode

            val fadeOut = AlphaAnimation(1f, 0f).apply { duration = 160 }
            val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 220 }

            if (toEncode) {
                tabEncode.setBackgroundResource(R.drawable.bg_tab_active)
                tabEncode.setTextColor(0xFF0F172A.toInt())
                tabDecode.setBackgroundResource(R.drawable.bg_tab_inactive)
                tabDecode.setTextColor(0xFF94A3B8.toInt())

                layoutDecode.startAnimation(fadeOut)
                layoutDecode.visibility = View.GONE

                layoutEncode.visibility = View.VISIBLE
                layoutEncode.startAnimation(fadeIn)
            } else {
                tabDecode.setBackgroundResource(R.drawable.bg_tab_active)
                tabDecode.setTextColor(0xFF0F172A.toInt())
                tabEncode.setBackgroundResource(R.drawable.bg_tab_inactive)
                tabEncode.setTextColor(0xFF94A3B8.toInt())

                layoutEncode.startAnimation(fadeOut)
                layoutEncode.visibility = View.GONE

                layoutDecode.visibility = View.VISIBLE
                layoutDecode.startAnimation(fadeIn)
            }
        }

        tabEncode.setOnClickListener { switchTabAnimated(true) }
        tabDecode.setOnClickListener { switchTabAnimated(false) }

        // ==========================================
        // CARRIER SELECTION (Single emoji & Custom)
        // ==========================================
        fun updateCarrierPills() {
            btnQuickEmoji1.setBackgroundResource(if (selectedCarrierEmoji == "🤔") R.drawable.bg_pill_btn else R.drawable.bg_tab_inactive)
            btnQuickEmoji2.setBackgroundResource(if (selectedCarrierEmoji == "😎") R.drawable.bg_pill_btn else R.drawable.bg_tab_inactive)
            btnQuickEmoji3.setBackgroundResource(if (selectedCarrierEmoji == "❤️") R.drawable.bg_pill_btn else R.drawable.bg_tab_inactive)
            btnQuickEmoji4.setBackgroundResource(if (selectedCarrierEmoji == "🔥") R.drawable.bg_pill_btn else R.drawable.bg_tab_inactive)
        }

        btnQuickEmoji1.setOnClickListener {
            selectedCarrierEmoji = "🤔"
            etCarrierEmoji.setText("")
            updateCarrierPills()
        }
        btnQuickEmoji2.setOnClickListener {
            selectedCarrierEmoji = "😎"
            etCarrierEmoji.setText("")
            updateCarrierPills()
        }
        btnQuickEmoji3.setOnClickListener {
            selectedCarrierEmoji = "❤️"
            etCarrierEmoji.setText("")
            updateCarrierPills()
        }
        btnQuickEmoji4.setOnClickListener {
            selectedCarrierEmoji = "🔥"
            etCarrierEmoji.setText("")
            updateCarrierPills()
        }

        etCarrierEmoji.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val custom = s?.toString()?.trim() ?: ""
                if (custom.isNotEmpty()) {
                    selectedCarrierEmoji = custom
                    btnQuickEmoji1.setBackgroundResource(R.drawable.bg_tab_inactive)
                    btnQuickEmoji2.setBackgroundResource(R.drawable.bg_tab_inactive)
                    btnQuickEmoji3.setBackgroundResource(R.drawable.bg_tab_inactive)
                    btnQuickEmoji4.setBackgroundResource(R.drawable.bg_tab_inactive)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // ==========================================
        // ENCODE ACTIONS
        // ==========================================
        var lastEncodedPayload = ""

        btnPasteEncode.setOnClickListener {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                etInputEncode.setText(clip.getItemAt(0).text?.toString() ?: "")
                Toast.makeText(this, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        btnClearEncode.setOnClickListener {
            etInputEncode.setText("")
            tvResultEncode.text = selectedCarrierEmoji
            lastEncodedPayload = ""
        }

        btnExecuteEncode.setOnClickListener {
            val msg = etInputEncode.text.toString().trim()
            if (msg.isEmpty()) {
                Toast.makeText(this, "বার্তা লিখুন", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val encoded = CryptoEngine.encodeSteganography(selectedCarrierEmoji, msg)
            lastEncodedPayload = encoded
            // Display only that single emoji
            tvResultEncode.text = encoded
            Toast.makeText(this, "একক ইমোজিতে তৈরি সম্পন্ন!", Toast.LENGTH_SHORT).show()
        }

        btnCopyEncode.setOnClickListener {
            if (lastEncodedPayload.isNotEmpty()) {
                val clip = ClipData.newPlainText("Zonex Emoji Secret", lastEncodedPayload)
                clipboardManager.setPrimaryClip(clip)
                Toast.makeText(this, "ইমোজি কপি হয়েছে!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "আগে বার্তা এনকোড করুন", Toast.LENGTH_SHORT).show()
            }
        }

        btnShareEncode.setOnClickListener {
            if (lastEncodedPayload.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, lastEncodedPayload)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val chooser = Intent.createChooser(intent, "Send via...").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(chooser)
            } else {
                Toast.makeText(this, "আগে বার্তা এনকোড করুন", Toast.LENGTH_SHORT).show()
            }
        }

        // ==========================================
        // DECODE ACTIONS
        // ==========================================
        fun performDecode(text: String) {
            etInputDecode.setText(text)
            if (text.isEmpty()) {
                tvResultDecode.text = ""
                tvDecodeStatus.text = ""
                return
            }

            val res = CryptoEngine.decodeSteganography(text)
            if (res.isSuccess) {
                val decoded = res.getOrNull() ?: ""
                tvResultDecode.text = decoded
                tvDecodeStatus.text = "● পাওয়া গেছে • ${decoded.length} অক্ষর"
                tvDecodeStatus.setTextColor(0xFF10B981.toInt())
            } else {
                // Try other fallback formats
                val detected = CryptoEngine.detectFormat(text)
                if (detected != null) {
                    if (detected == com.example.crypto.SecretFormat.EMOJI) {
                        val emo = CryptoEngine.decodeEmoji(text)
                        if (emo.isSuccess) {
                            tvResultDecode.text = emo.getOrNull()
                            tvDecodeStatus.text = "● উদ্ধারকৃত"
                            tvDecodeStatus.setTextColor(0xFF10B981.toInt())
                            return
                        }
                    }
                }
                tvResultDecode.text = "কোনো গোপন বার্তা পাওয়া যায়নি"
                tvDecodeStatus.text = "● নেই"
                tvDecodeStatus.setTextColor(0xFFEF4444.toInt())
            }
        }

        btnPasteDecode.setOnClickListener {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString() ?: ""
                performDecode(text)
                Toast.makeText(this, "ক্লিপবোর্ড থেকে পেস্ট হয়েছে", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "ক্লিপবোর্ড খালি", Toast.LENGTH_SHORT).show()
            }
        }

        btnClearDecode.setOnClickListener {
            etInputDecode.setText("")
            tvResultDecode.text = "ডিকোড করা বার্তা এখানে দৃশ্যমান হবে..."
            tvDecodeStatus.text = ""
        }

        btnExecuteDecode.setOnClickListener {
            val text = etInputDecode.text.toString()
            performDecode(text)
        }

        btnCopyDecode.setOnClickListener {
            val text = tvResultDecode.text.toString()
            if (text.isNotEmpty() && !text.startsWith("কোনো") && !text.startsWith("ডিকোড করা বার্তা")) {
                val clip = ClipData.newPlainText("Decoded Message", text)
                clipboardManager.setPrimaryClip(clip)
                Toast.makeText(this, "আসল বার্তা কপি হয়েছে!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "কপি করার মতো বার্তা নেই", Toast.LENGTH_SHORT).show()
            }
        }

        // ==========================================
        // LANGUAGE TOGGLE
        // ==========================================
        btnLangToggle.setOnClickListener {
            LanguageManager.toggleLanguage()
            val isBn = LanguageManager.isBengali()
            btnLangToggle.text = if (isBn) "EN" else "বাং"

            tabEncode.text = if (isBn) "🔒 এনকোড" else "🔒 Encode"
            tabDecode.text = if (isBn) "🔓 ডিকোড" else "🔓 Decode"
            btnExecuteEncode.text = if (isBn) "🔒 একক ইমোজিতে এনকোড করুন" else "🔒 Encode into Single Emoji"
            btnExecuteDecode.text = if (isBn) "🔓 গোপন বার্তা ডিকোড করুন" else "🔓 Decode Secret Message"
            btnCopyEncode.text = if (isBn) "📋 কপি" else "📋 Copy"
            btnCopyDecode.text = if (isBn) "📋 কপি বার্তা" else "📋 Copy Message"
        }

        updateCarrierPills()
    }

    private fun showBubble() {
        try {
            if (windowView?.isAttachedToWindow == true) {
                windowManager.removeView(windowView)
            }
            if (bubbleView?.isAttachedToWindow != true) {
                windowManager.addView(bubbleView, bubbleParams)
            }
        } catch (_: Exception) {}
    }

    private fun showWindow() {
        try {
            if (bubbleView?.isAttachedToWindow == true) {
                windowManager.removeView(bubbleView)
            }
            if (windowView?.isAttachedToWindow != true) {
                windowManager.addView(windowView, windowParams)
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        try {
            if (bubbleView?.isAttachedToWindow == true) {
                windowManager.removeView(bubbleView)
            }
            if (windowView?.isAttachedToWindow == true) {
                windowManager.removeView(windowView)
            }
        } catch (_: Exception) {}
    }
}
