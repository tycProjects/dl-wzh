package id.marvelproject.wasender

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false
        webView.settings.javaScriptCanOpenWindowsAutomatically = false
        webView.settings.setSupportMultipleWindows(false)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                return openExternalIfNeeded(request.url)
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return openExternalIfNeeded(Uri.parse(url))
            }
        }

        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun openExternalIfNeeded(uri: Uri): Boolean {
        val scheme = uri.scheme ?: return true
        val host = uri.host?.lowercase() ?: ""

        if (scheme == "https" && (
                host == "wa.me" ||
                host == "api.whatsapp.com" ||
                host == "www.whatsapp.com"
            )
        ) {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            try {
                startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, "Tidak ada aplikasi untuk membuka tautan ini.", Toast.LENGTH_LONG).show()
            }
            return true
        }

        // Tetap di dalam aplikasi untuk file lokal; blokir navigasi ke situs lain.
        return !(scheme == "file" && uri.path?.endsWith("/index.html") == true)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
