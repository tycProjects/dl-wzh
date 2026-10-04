package com.countrydetector

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.*
import android.widget.Toast
import android.provider.Settings.SettingNotFoundException
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var projectionManager: MediaProjectionManager

    // Request codes
    private val RC_OVERLAY   = 1001
    private val RC_CAPTURE   = 1002

    // ─────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make status bar transparent so our HTML topbar looks native
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        webView = WebView(this)
        setContentView(webView)

        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        setupWebView()
    }

    // ─────────────────────────────────────────────────────────────
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled          = true
            domStorageEnabled          = true
            allowFileAccess            = true
            allowContentAccess         = true
            setSupportZoom(false)
            displayZoomControls        = false
            builtInZoomControls        = false
            useWideViewPort            = true
            loadWithOverviewMode       = true
        }

        // Inject the Android bridge so HTML can call Android.*
        webView.addJavascriptInterface(AndroidBridge(), "Android")

        webView.webChromeClient = object : WebChromeClient() {}
        webView.webViewClient   = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                // Sync permission state back to HTML on load
                syncPermissionState()
            }
        }

        // Load our premium HTML from assets
        webView.loadUrl("file:///android_asset/index.html")
    }

    // ─────────────────────────────────────────────────────────────
    // Sync granted state to JS after activity resume
    private fun syncPermissionState() {
        val overlayGranted = Settings.canDrawOverlays(this)
        if (overlayGranted) {
            webView.evaluateJavascript("setOverlayGranted()", null)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ── BRIDGE ──────────────────────────────────────────────────
    inner class AndroidBridge {

        // Called from HTML: Grant button for overlay
        @JavascriptInterface
        fun requestOverlayPermission() {
            if (Settings.canDrawOverlays(this@MainActivity)) {
                // Already granted — just notify JS
                runOnUiThread {
                    webView.evaluateJavascript("setOverlayGranted()", null)
                }
                return
            }
            // Open system Settings > Display over other apps
            try {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivityForResult(intent, RC_OVERLAY)
            } catch (_: Exception) {
                try {
                    startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION), RC_OVERLAY)
                } catch (_: Exception) {
                    startActivityForResult(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")), RC_OVERLAY)
                }
            }
        }

        // Called from HTML: Grant button for screen capture
        @JavascriptInterface
        fun requestScreenCapture() {
            if (!Settings.canDrawOverlays(this@MainActivity)) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity,
                        "Please grant overlay permission first", Toast.LENGTH_SHORT).show()
                }
                return
            }
            // This triggers the real Android screen-capture consent dialog
            val captureIntent = projectionManager.createScreenCaptureIntent()
            startActivityForResult(captureIntent, RC_CAPTURE)
        }

        @JavascriptInterface
        fun openAppSettings() {
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }

        // Called from HTML: Launch the floating bubble
        @JavascriptInterface
        fun launchFloatingBubble() {
            if (!Settings.canDrawOverlays(this@MainActivity)) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity,
                        "Overlay permission required", Toast.LENGTH_SHORT).show()
                }
                return
            }
            runOnUiThread {
                // Start the foreground service that owns the bubble
                val intent = Intent(this@MainActivity, FloatingService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                webView.evaluateJavascript("setBubbleActive()", null)
                // Move app to background so the overlay is visible
                moveTaskToBack(true)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) {
            webView.postDelayed({ syncPermissionState() }, 150)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RC_OVERLAY -> {
                if (Settings.canDrawOverlays(this)) {
                    webView.evaluateJavascript("setOverlayGranted()", null)
                    Toast.makeText(this, "✅ Overlay permission granted", Toast.LENGTH_SHORT).show()
                }
            }
            RC_CAPTURE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // Pass the MediaProjection token to the floating service
                    val intent = Intent(this, FloatingService::class.java).apply {
                        action = FloatingService.ACTION_SET_PROJECTION
                        putExtra(FloatingService.EXTRA_RESULT_CODE, resultCode)
                        putExtra(FloatingService.EXTRA_RESULT_DATA, data)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                    webView.evaluateJavascript("setScreenGranted()", null)
                    Toast.makeText(this, "✅ Screen capture granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
