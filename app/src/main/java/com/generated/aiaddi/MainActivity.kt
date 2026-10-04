package com.generated.aiaddi

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import org.json.JSONObject

class MainActivity : Activity() {

    private lateinit var web: WebView
    private var pageLoaded = false
    private var pendingSettings = false
    private var step = 0 // 0 diam, 1 menunggu izin overlay, 2 menunggu aksesibilitas
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        web = WebView(this)
        web.setBackgroundColor(0xFF071313.toInt())
        setContentView(web)

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
        web.settings.allowContentAccess = true
        web.addJavascriptInterface(Bridge(), "AndroidBridge")

        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                pageLoaded = true
                if (pendingSettings) { pendingSettings = false; openSettingsDialog() }
            }
        }
        // Supaya alert()/prompt() di HTML muncul dan upload gambar bisa membuka galeri.
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?, callback: ValueCallback<Array<Uri>>?, params: FileChooserParams?
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                val i = Intent(Intent.ACTION_GET_CONTENT)
                i.addCategory(Intent.CATEGORY_OPENABLE)
                i.type = "image/*"
                startActivityForResult(Intent.createChooser(i, "Pilih gambar"), 1)
                return true
            }
        }
        web.loadUrl("file:///android_asset/index.html")

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2)
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        if (i?.getBooleanExtra("open_settings", false) == true) {
            i.removeExtra("open_settings")
            if (pageLoaded) openSettingsDialog() else pendingSettings = true
        }
    }

    private fun openSettingsDialog() {
        web.evaluateJavascript("settings();", null)
    }

    override fun onResume() {
        super.onResume()
        when (step) {
            1 -> { step = 0; if (Settings.canDrawOverlays(this)) startOverlay() }
            2 -> {
                step = 0
                if (AddiAccessibilityService.instance != null) startFloating()
                else Toast.makeText(this, "Aksesibilitas Addi AI belum aktif.", Toast.LENGTH_LONG).show()
            }
        }
        // tampilkan hasil AI terakhir di halaman kalau app dibuka lagi
        val r = FloatingService.lastResult
        if (r != null && pageLoaded) {
            FloatingService.lastResult = null
            web.evaluateJavascript("window.showAIResult('Hasil Addi AI'," + JSONObject.quote(r) + ");", null)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1) {
            fileCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
            fileCallback = null
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else moveTaskToBack(true)
    }

    private fun startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            step = 1
            Toast.makeText(this, "Aktifkan \"Tampilkan di atas aplikasi lain\" untuk Addi AI", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } else if (AddiAccessibilityService.instance == null) {
            step = 2
            Toast.makeText(this, "Nyalakan \"Addi AI\" di layar Aksesibilitas", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } else {
            startFloating()
        }
    }

    private fun startFloating() {
        startForegroundService(Intent(this, FloatingService::class.java))
        Toast.makeText(this, "Aktif. Tekan Home, lalu ketuk tombol robot.", Toast.LENGTH_LONG).show()
    }

    private fun showResult(title: String, detail: String) {
        web.evaluateJavascript(
            "window.showAIResult(" + JSONObject.quote(title) + "," + JSONObject.quote(detail) + ");", null
        )
    }

    private fun readFromOtherApp(action: String) {
        if (!FloatingService.running) {
            showResult("Belum aktif", "Tekan tombol Aktifkan AI dulu.")
            return
        }
        startService(Intent(this, FloatingService::class.java).setAction(action))
        moveTaskToBack(true) // supaya yang dibaca adalah layar app lain, bukan app ini
    }

    // Jembatan HTML -> Android (dipanggil lewat AndroidBridge di index.html)
    inner class Bridge {
        @JavascriptInterface fun activate() { runOnUiThread { startOverlay() } }
        @JavascriptInterface fun captureScreen() { runOnUiThread { readFromOtherApp(FloatingService.ACTION_READ) } }
        @JavascriptInterface fun captureScreenEssay() { runOnUiThread { readFromOtherApp(FloatingService.ACTION_ESSAY) } }
        @JavascriptInterface fun ocrImage() {
            runOnUiThread { showResult("Belum tersedia", "Upload screenshot belum didukung. Ketuk tombol robot di app lain.") }
        }
        @JavascriptInterface fun saveSettings(provider: String, key: String, model: String) {
            getSharedPreferences("ai", MODE_PRIVATE).edit()
                .putString("provider", provider.trim()).putString("key", key.trim())
                .putString("model", model.trim()).apply()
        }
    }
}
