package com.template.webapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.webkit.MimeTypeMap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * Loads the bundled site from assets/www through a fake https origin, so it behaves like a real
 * website (fetch, ES modules, localStorage, absolute paths) but never needs the network.
 * Options come from assets/config.json, written by the builder app.
 */
class MainActivity : Activity() {

    private lateinit var web: WebView
    private lateinit var pageProgress: ProgressBar
    private val host = "appassets.androidplatform.net"
    private var allowOnline = true

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cfg = try {
            JSONObject(assets.open("config.json").bufferedReader().use { it.readText() })
        } catch (e: Exception) {
            JSONObject()
        }
        when (cfg.optString("orientation", "auto")) {
            "portrait" -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "landscape" -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        @Suppress("DEPRECATION")
        if (cfg.optBoolean("fullscreen", false)) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        if (cfg.optBoolean("keepScreenOn", false)) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        allowOnline = cfg.optBoolean("allowInternet", true)
        val remoteUrl = cfg.optString("url", "")

        // Use system bars and a native loading indicator around the web content.
        // This keeps the generated app feeling like an Android app without changing the site itself.
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }
        pageProgress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progress = 0
            visibility = View.GONE
            // A slim native loading line, shown only while the page is loading.
            minimumHeight = (2 * resources.displayMetrics.density).toInt()
        }
        root.addView(pageProgress, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (2 * resources.displayMetrics.density).toInt().coerceAtLeast(2)
        ))

        web = WebView(this)
        root.addView(web, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)

        web.setBackgroundColor(Color.WHITE)
        web.overScrollMode = WebView.OVER_SCROLL_NEVER
        web.isVerticalScrollBarEnabled = false
        web.isHorizontalScrollBarEnabled = false
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mediaPlaybackRequiresUserGesture = false

            // Prefer a responsive, app-like 1:1 layout instead of shrinking
            // desktop pages to fit the screen. Keep pinch zoom available for
            // accessibility and sites that genuinely need it, but hide legacy
            // on-screen zoom buttons.
            useWideViewPort = false
            loadWithOverviewMode = false
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? =
                if (request.url.host == host) serve(request.url) else null

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url
                if (url.host == host) return false
                val scheme = url.scheme ?: return false
                if (scheme == "http" || scheme == "https") return !allowOnline
                // tel:, mailto:, intent links, etc. go to other apps
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, url))
                } catch (e: Exception) {
                    // no app to handle it
                }
                return true
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                pageProgress.progress = newProgress
                pageProgress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }
        }

        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                OnBackInvokedCallback { goBackOrFinish() }
            )
        }

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState)
        } else {
            // Website mode loads the live link; otherwise the bundled site
            web.loadUrl(if (remoteUrl.isNotEmpty()) remoteUrl else "https://$host/index.html")
        }
    }

    private fun goBackOrFinish() {
        if (web.canGoBack()) web.goBack() else finish()
    }

    @Deprecated("Used on Android 12 and below")
    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT < 33) goBackOrFinish() else super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onPause() {
        super.onPause()
        web.onPause()
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }

    // ---- serving files from assets/www ----

    private fun serve(uri: Uri): WebResourceResponse {
        var path = Uri.decode(uri.path ?: "/")
        if (path.endsWith("/")) path += "index.html"
        path = path.trimStart('/')
        if (path.isEmpty()) path = "index.html"
        if (path.split('/').any { it == ".." }) return notFound()

        val stream = try {
            assets.open("www/$path")
        } catch (e: IOException) {
            // "/docs" -> "/docs/index.html"
            try {
                assets.open("www/$path/index.html")
            } catch (e2: IOException) {
                return notFound()
            }
        }
        val mime = mimeFor(path)
        val isText = mime.startsWith("text/") || mime.contains("json") ||
            mime.contains("javascript") || mime.contains("xml")
        val response = WebResourceResponse(mime, if (isText) "UTF-8" else null, stream)
        response.responseHeaders = mapOf("Access-Control-Allow-Origin" to "*")
        return response
    }

    private fun notFound() = WebResourceResponse(
        "text/html", "UTF-8", 404, "Not Found", emptyMap(),
        ByteArrayInputStream("<h1>404 Not Found</h1>".toByteArray())
    )

    private fun mimeFor(path: String): String {
        return when (val ext = path.substringAfterLast('.', "").lowercase()) {
            "html", "htm" -> "text/html"
            "js", "mjs" -> "text/javascript"
            "css" -> "text/css"
            "json", "map" -> "application/json"
            "svg" -> "image/svg+xml"
            "wasm" -> "application/wasm"
            "webmanifest" -> "application/manifest+json"
            "txt" -> "text/plain"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            "ttf" -> "font/ttf"
            "otf" -> "font/otf"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                ?: "application/octet-stream"
        }
    }
}
