package com.novafloating.app

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import java.net.URLEncoder

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager

    // 1. Floating Bubble Views & Params
    private var bubbleView: View? = null
    private lateinit var bubbleParams: WindowManager.LayoutParams

    // 2. Selection Crop Box Views & Params
    private var cropBoxView: View? = null
    private lateinit var cropBoxParams: WindowManager.LayoutParams
    private var isCropBoxVisible = false

    // 3. Floating AI Viewer Views & Params
    private var aiViewerWindow: View? = null
    private lateinit var aiViewerParams: WindowManager.LayoutParams
    private var isAiViewerVisible = false

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

        if (bubbleView == null) {
            initBubbleView()
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NovaFloating Area Reader Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menjaga layanan NovaFloating Area Reader tetap aktif"
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
            .setContentTitle("⚡ NovaFloating Area Reader")
            .setContentText("Sistem Crop Selection Box & Floating AI Viewer aktif")
            .setSmallIcon(android.R.drawable.ic_menu_crop)
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

    private fun getOverlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    // 1. FLOATING TRIGGER BUBBLE
    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun initBubbleView() {
        bubbleView = LayoutInflater.from(this).inflate(R.layout.layout_floating_bubble, null)
        val view = bubbleView ?: return

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 300
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleParams.x
                    initialY = bubbleParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isClick = false
                    }
                    bubbleParams.x = initialX + dx
                    bubbleParams.y = initialY + dy
                    updateViewLayoutSafe(view, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        showSelectionCropBox()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(view, bubbleParams)
    }

    private fun hideBubbleView() {
        bubbleView?.visibility = View.GONE
    }

    private fun showBubbleView() {
        bubbleView?.visibility = View.VISIBLE
    }

    // 2. FLOATING SELECTION CROP BOX
    @SuppressLint("InflateParams", "ClickableViewAccessibility", "SetTextI18n")
    private fun showSelectionCropBox() {
        hideBubbleView()

        if (cropBoxView == null) {
            cropBoxView = LayoutInflater.from(this).inflate(R.layout.layout_selection_box, null)
            val view = cropBoxView ?: return

            val displayMetrics = resources.displayMetrics
            val defaultWidth = (displayMetrics.widthPixels * 0.75).toInt()
            val defaultHeight = (displayMetrics.heightPixels * 0.28).toInt()

            cropBoxParams = WindowManager.LayoutParams(
                defaultWidth,
                defaultHeight,
                getOverlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (displayMetrics.widthPixels - defaultWidth) / 2
                y = (displayMetrics.heightPixels - defaultHeight) / 3
            }

            val headerDrag = view.findViewById<View>(R.id.header_crop_drag)
            val ivResizeHandle = view.findViewById<ImageView>(R.id.iv_resize_handle)
            val btnConfirm = view.findViewById<Button>(R.id.btn_confirm_crop)
            val btnCancel = view.findViewById<Button>(R.id.btn_cancel_crop)
            val tvBoxDimensions = view.findViewById<TextView>(R.id.tv_box_dimensions)
            val btnShrink = view.findViewById<TextView>(R.id.btn_shrink_box)
            val btnExpand = view.findViewById<TextView>(R.id.btn_expand_box)
            val btnPreset = view.findViewById<TextView>(R.id.btn_preset_box)

            tvBoxDimensions.text = "${defaultWidth}x${defaultHeight}"

            // 1. Tombol Perkecil [-]: Mengurangi lebar & tinggi kotak seleksi secara bertahap
            btnShrink.setOnClickListener {
                cropBoxParams.width = Math.max(160, cropBoxParams.width - 50)
                cropBoxParams.height = Math.max(90, cropBoxParams.height - 35)
                tvBoxDimensions.text = "${cropBoxParams.width}x${cropBoxParams.height}"
                updateViewLayoutSafe(view, cropBoxParams)
            }

            // 2. Tombol Perbesar [+]: Menambah lebar & tinggi kotak seleksi secara bertahap
            btnExpand.setOnClickListener {
                val maxWidth = displayMetrics.widthPixels - cropBoxParams.x - 10
                val maxHeight = displayMetrics.heightPixels - cropBoxParams.y - 20
                cropBoxParams.width = Math.min(maxWidth, cropBoxParams.width + 50)
                cropBoxParams.height = Math.min(maxHeight, cropBoxParams.height + 35)
                tvBoxDimensions.text = "${cropBoxParams.width}x${cropBoxParams.height}"
                updateViewLayoutSafe(view, cropBoxParams)
            }

            // 3. Tombol Siklus Preset Ukuran [⤢]: Mengganti ukuran preset (Kecil -> Sedang -> Besar -> Maksimal)
            var currentPresetIdx = 1
            btnPreset.setOnClickListener {
                currentPresetIdx = (currentPresetIdx + 1) % 4
                when (currentPresetIdx) {
                    0 -> { // Ukuran Kecil
                        cropBoxParams.width = (displayMetrics.widthPixels * 0.55).toInt()
                        cropBoxParams.height = (displayMetrics.heightPixels * 0.18).toInt()
                    }
                    1 -> { // Ukuran Sedang (Default)
                        cropBoxParams.width = (displayMetrics.widthPixels * 0.75).toInt()
                        cropBoxParams.height = (displayMetrics.heightPixels * 0.28).toInt()
                    }
                    2 -> { // Ukuran Besar
                        cropBoxParams.width = (displayMetrics.widthPixels * 0.88).toInt()
                        cropBoxParams.height = (displayMetrics.heightPixels * 0.40).toInt()
                    }
                    3 -> { // Ukuran Maksimal / Layar Penuh
                        cropBoxParams.width = (displayMetrics.widthPixels * 0.94).toInt()
                        cropBoxParams.height = (displayMetrics.heightPixels * 0.55).toInt()
                    }
                }
                tvBoxDimensions.text = "${cropBoxParams.width}x${cropBoxParams.height}"
                updateViewLayoutSafe(view, cropBoxParams)
            }

            // 4. Drag Handler: Memindahkan kotak seleksi ke posisi mana pun di layar
            headerDrag.setOnTouchListener(object : View.OnTouchListener {
                private var initX = 0
                private var initY = 0
                private var touchX = 0f
                private var touchY = 0f

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initX = cropBoxParams.x
                            initY = cropBoxParams.y
                            touchX = event.rawX
                            touchY = event.rawY
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            cropBoxParams.x = initX + (event.rawX - touchX).toInt()
                            cropBoxParams.y = initY + (event.rawY - touchY).toInt()
                            updateViewLayoutSafe(view, cropBoxParams)
                            return true
                        }
                    }
                    return false
                }
            })

            // 5. Freeform Corner Resize Handler: Tarik pojok kanan bawah bebas sesuka hati
            ivResizeHandle.setOnTouchListener(object : View.OnTouchListener {
                private var initW = 0
                private var initH = 0
                private var touchX = 0f
                private var touchY = 0f

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initW = cropBoxParams.width
                            initH = cropBoxParams.height
                            touchX = event.rawX
                            touchY = event.rawY
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val maxWidth = displayMetrics.widthPixels - cropBoxParams.x - 10
                            val maxHeight = displayMetrics.heightPixels - cropBoxParams.y - 20
                            val newW = Math.max(160, Math.min(maxWidth, initW + (event.rawX - touchX).toInt()))
                            val newH = Math.max(90, Math.min(maxHeight, initH + (event.rawY - touchY).toInt()))
                            cropBoxParams.width = newW
                            cropBoxParams.height = newH
                            tvBoxDimensions.text = "${newW}x${newH}"
                            updateViewLayoutSafe(view, cropBoxParams)
                            return true
                        }
                    }
                    return false
                }
            })

            btnConfirm.setOnClickListener {
                executeCropAreaExtraction()
            }

            btnCancel.setOnClickListener {
                dismissSelectionCropBox()
                showBubbleView()
            }

            windowManager.addView(view, cropBoxParams)
            isCropBoxVisible = true
        } else {
            cropBoxView?.visibility = View.VISIBLE
            isCropBoxVisible = true
        }
    }

    private fun dismissSelectionCropBox() {
        cropBoxView?.visibility = View.GONE
        isCropBoxVisible = false
    }

    private fun executeCropAreaExtraction() {
        val view = cropBoxView ?: return

        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val selectionRect = Rect(
            location[0],
            location[1],
            location[0] + cropBoxParams.width,
            location[1] + cropBoxParams.height
        )

        val extractedText = AreaReaderAccessibilityService.extractTextInBounds(selectionRect)?.trim() ?: ""
        val finalQuery = if (extractedText.isNotEmpty()) {
            extractedText
        } else {
            "Konsep dan pembahasan materi"
        }

        Toast.makeText(this, "⚡ Area terseleksi: ${finalQuery.take(45)}...", Toast.LENGTH_SHORT).show()
        dismissSelectionCropBox()
        showInAppFloatingAiViewer(finalQuery)
    }

    // 3. IN-APP FLOATING AI VIEWER
    @SuppressLint("InflateParams", "SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun showInAppFloatingAiViewer(queryText: String) {
        val displayMetrics = resources.displayMetrics
        val windowWidth = (displayMetrics.widthPixels * 0.88).toInt()
        val windowHeight = (displayMetrics.heightPixels * 0.68).toInt()

        if (aiViewerWindow == null) {
            aiViewerWindow = LayoutInflater.from(this).inflate(R.layout.layout_floating_ai_view, null)
            val window = aiViewerWindow ?: return

            aiViewerParams = WindowManager.LayoutParams(
                windowWidth,
                windowHeight,
                getOverlayType(),
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
                x = 0
                y = 0
            }

            val btnClose = window.findViewById<TextView>(R.id.btn_close_ai_view)
            val tvQueryPreview = window.findViewById<TextView>(R.id.tv_active_query_preview)
            val progressBar = window.findViewById<ProgressBar>(R.id.progress_ai_loading)
            val webView = window.findViewById<WebView>(R.id.webview_ai_viewer)

            tvQueryPreview.text = queryText

            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                cacheMode = WebSettings.LOAD_DEFAULT
                userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
            }

            webView.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress < 100) {
                        progressBar.visibility = View.VISIBLE
                        progressBar.progress = newProgress
                    } else {
                        progressBar.visibility = View.GONE
                    }
                }
            }

            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    request?.url?.let { view?.loadUrl(it.toString()) }
                    return true
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    progressBar.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    progressBar.visibility = View.GONE
                }
            }

            btnClose.setOnClickListener {
                dismissInAppFloatingAiViewer()
                showBubbleView()
            }

            val headerAi = window.findViewById<View>(R.id.header_ai_window)
            headerAi.setOnTouchListener(object : View.OnTouchListener {
                private var initX = 0
                private var initY = 0
                private var touchX = 0f
                private var touchY = 0f

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initX = aiViewerParams.x
                            initY = aiViewerParams.y
                            touchX = event.rawX
                            touchY = event.rawY
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            aiViewerParams.x = initX + (event.rawX - touchX).toInt()
                            aiViewerParams.y = initY + (event.rawY - touchY).toInt()
                            updateViewLayoutSafe(window, aiViewerParams)
                            return true
                        }
                    }
                    return false
                }
            })

            val searchUrl = "https://www.google.com/search?q=" + URLEncoder.encode(queryText, "UTF-8")
            webView.loadUrl(searchUrl)

            windowManager.addView(window, aiViewerParams)
            isAiViewerVisible = true
        } else {
            val window = aiViewerWindow ?: return
            val tvQueryPreview = window.findViewById<TextView>(R.id.tv_active_query_preview)
            val webView = window.findViewById<WebView>(R.id.webview_ai_viewer)

            tvQueryPreview.text = queryText
            val searchUrl = "https://www.google.com/search?q=" + URLEncoder.encode(queryText, "UTF-8")
            webView.loadUrl(searchUrl)

            window.visibility = View.VISIBLE
            isAiViewerVisible = true
        }
    }

    private fun dismissInAppFloatingAiViewer() {
        aiViewerWindow?.visibility = View.GONE
        isAiViewerVisible = false
    }

    private fun updateViewLayoutSafe(view: View, params: WindowManager.LayoutParams) {
        try {
            if (view.isAttachedToWindow) {
                windowManager.updateViewLayout(view, params)
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        bubbleView?.let {
            if (it.isAttachedToWindow) windowManager.removeView(it)
            bubbleView = null
        }
        cropBoxView?.let {
            if (it.isAttachedToWindow) windowManager.removeView(it)
            cropBoxView = null
        }
        aiViewerWindow?.let {
            if (it.isAttachedToWindow) {
                val webView = it.findViewById<WebView>(R.id.webview_ai_viewer)
                webView?.destroy()
                windowManager.removeView(it)
            }
            aiViewerWindow = null
        }
    }
}
