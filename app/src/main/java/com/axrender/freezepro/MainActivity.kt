package com.axrender.freezepro

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.JavascriptInterface
import android.os.Build

class MainActivity : Activity() {
    private lateinit var web: WebView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = false
        web.webViewClient = WebViewClient()
        web.webChromeClient = WebChromeClient()
        web.addJavascriptInterface(DeviceBridge(), "AndroidDevice")
        web.setBackgroundColor(android.graphics.Color.rgb(8, 10, 18))
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }
    inner class DeviceBridge {
        @JavascriptInterface fun deviceInfo(): String = "Android ${Build.VERSION.RELEASE} • ${Build.MANUFACTURER} ${Build.MODEL} • API ${Build.VERSION.SDK_INT}"
        @JavascriptInterface fun launchPackage(pkg: String): Boolean { return try { val intent = packageManager.getLaunchIntentForPackage(pkg) ?: return false; startActivity(intent); true } catch (e: Exception) { false } }
    }
    override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }
}
