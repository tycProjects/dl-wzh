package com.carter.linkbypasspro

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.EditorInfo
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.Intent
import org.json.JSONArray
import java.net.URL
import java.net.HttpURLConnection
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var address: EditText
    private lateinit var browser: WebView
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var historyText: TextView
    private val history = mutableListOf<String>()
    private val resolverExecutor = Executors.newSingleThreadExecutor()
    private val red = Color.rgb(255, 48, 67)
    private val bg = Color.rgb(8, 8, 10)
    private val panel = Color.rgb(23, 17, 19)

    // Provider registry is for recognition/routing diagnostics, not for defeating
    // site access controls. Unknown hosts use the generic HTTP/DOM resolver.
    private val providerRegistry = mapOf(
        "sfl.gl" to "SFL.GL",
        "bicolink.net" to "BICOLINK",
        "khaddavi.net" to "KHADDAVI",
        "linkvertise.com" to "LINKVERTISE",
        "ouo.io" to "OUO",
        "ouo.press" to "OUO",
        "shrinkme.io" to "SHRINKME",
        "shorturl.at" to "SHORTURL",
        "bit.ly" to "BITLY",
        "tinyurl.com" to "TINYURL"
    )

    private val bridge = object {
        @JavascriptInterface
        fun onLinksFound(json: String) {
            runOnUiThread { showCandidates(json) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildInterface()
        configureBrowser()
        status.text = "READY • Masukkan link untuk memulai"
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 10)
            setBackgroundColor(bg)
        }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isSmoothScrollingEnabled = true
            clipToPadding = false
        }
        scroll.addView(content)

        content.addView(TextView(this).apply {
            text = "LINKBYPASS PRO"
            textSize = 25f
            setTextColor(red)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        }, matchWrap())
        content.addView(TextView(this).apply {
            text = "SMART LINK SCANNER  •  DOWNLOAD MANAGER"
            textSize = 10f
            setTextColor(Color.LTGRAY)
            setPadding(0, 3, 0, 12)
        })

        address = EditText(this).apply {
            hint = "Tempel URL https://..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textSize = 14f
            singleLine = true
            setPadding(12, 10, 12, 10)
            background = rounded(panel, 12, Color.rgb(85, 35, 43))
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard()
                    openAddress()
                    true
                } else false
            }
        }
        content.addView(address, matchWrap())

        val firstRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        firstRow.addView(actionButton("BUKA LINK") { openAddress() }, weighted())
        firstRow.addView(actionButton("PASTE") { pasteClipboard() }, weighted(8))
        content.addView(firstRow)

        val secondRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        secondRow.addView(actionButton("SCAN LINK") { scanPage() }, weighted())
        secondRow.addView(actionButton("RESOLVE LINK") { resolveAddress() }, weighted(8))
        val downloadRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        downloadRow.addView(actionButton("DOWNLOADS") { openDownloads() }, weighted())
        downloadRow.addView(actionButton("BUKA TUJUAN") { openAddress() }, weighted(8))
        content.addView(secondRow)
        content.addView(downloadRow)

        val thirdRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        thirdRow.addView(actionButton("KEMBALI") { if (browser.canGoBack()) browser.goBack() }, weighted())
        thirdRow.addView(actionButton("REFRESH") { browser.reload() }, weighted(8))
        content.addView(thirdRow)
        content.addView(actionButton("UJI LINK SFL.GL") {
            address.setText("https://sfl.gl/woE9vko")
            openAddress()
        }, matchWrap())

        status = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.rgb(255, 170, 178))
            setPadding(2, 10, 2, 8)
        }
        content.addView(status)

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressTintList = android.content.res.ColorStateList.valueOf(red)
            visibility = View.GONE
        }
        content.addView(progress, matchWrap())

        content.addView(TextView(this).apply {
            text = "BROWSER / LINK SCANNER"
            textSize = 11f
            setTextColor(red)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 9, 0, 6)
        })

        browser = WebView(this).apply {
            setBackgroundColor(Color.rgb(245, 245, 245))
            isFocusable = true
            isFocusableInTouchMode = true
        }
        content.addView(browser, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(440)
        ))

        content.addView(TextView(this).apply {
            text = "RIWAYAT LINK"
            textSize = 11f
            setTextColor(red)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 10, 0, 5)
        })
        historyText = TextView(this).apply {
            text = "Belum ada riwayat."
            textSize = 10f
            setTextColor(Color.LTGRAY)
            setPadding(10, 8, 10, 8)
            background = rounded(panel, 10, Color.DKGRAY)
        }
        content.addView(historyText)

        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
    }

    private fun configureBrowser() {
        browser.settings.javaScriptEnabled = true
        browser.settings.domStorageEnabled = true
        browser.settings.loadsImagesAutomatically = true
        browser.settings.javaScriptCanOpenWindowsAutomatically = false
        browser.settings.allowFileAccess = false
        browser.settings.allowContentAccess = false
        browser.settings.setSupportMultipleWindows(false)
        browser.addJavascriptInterface(bridge, "LinkScanner")
        CookieManager.getInstance().setAcceptCookie(true)

        browser.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
                progress.progress = newProgress
            }
        }

        browser.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme?.lowercase()
                if (scheme == "http" || scheme == "https") {
                    addHistory(uri.toString())
                    return false
                }
                if (scheme == "intent") {
                    try {
                        startActivity(Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME))
                    } catch (_: Exception) { updateStatus("Link aplikasi eksternal tidak dapat dibuka.") }
                    return true
                }
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                updateStatus("Membuka halaman…")
                if (url != null) {
                    address.setText(url)
                    addHistory(url)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                updateStatus("Halaman siap. Tekan SCAN LINK untuk mencari tautan unduh yang terlihat.")
                if (url != null) address.setText(url)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?,
                                         error: android.webkit.WebResourceError?) {
                if (request?.isForMainFrame == true) updateStatus("Gagal memuat halaman. Periksa URL/koneksi.")
            }
        }

        browser.setDownloadListener(DownloadListener { url, userAgent, disposition, mime, _ ->
            enqueueDownload(url, userAgent, disposition, mime)
        })
    }

    /**
     * Resolves ordinary HTTP redirect chains only. It does not defeat CAPTCHA,
     * authentication, payment gates, anti-bot challenges, or site-specific tokens.
     */
    private fun identifyProvider(rawUrl: String): String {
        return try {
            val host = Uri.parse(rawUrl).host?.lowercase()?.removePrefix("www.") ?: return "UNKNOWN / GENERIC"
            providerRegistry.entries.firstOrNull { (domain, _) ->
                host == domain || host.endsWith(".$domain")
            }?.value ?: "UNKNOWN / GENERIC"
        } catch (_: Exception) {
            "UNKNOWN / GENERIC"
        }
    }

    private fun resolveAddress() {
        var raw = address.text.toString().trim()
        if (raw.isBlank()) raw = browser.url.orEmpty()
        if (raw.isBlank()) {
            updateStatus("Tempel link terlebih dahulu.")
            return
        }
        if (!raw.contains("://")) raw = "https://$raw"
        val parsed = Uri.parse(raw)
        if ((parsed.scheme != "http" && parsed.scheme != "https") || parsed.host.isNullOrBlank()) {
            updateStatus("URL tidak valid.")
            return
        }
        val provider = identifyProvider(raw)
        updateStatus("Provider: $provider • Memeriksa redirect HTTP standar…")
        resolverExecutor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(raw).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    requestMethod = "HEAD"
                    connectTimeout = 12000
                    readTimeout = 12000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
                    setRequestProperty("Accept", "*/*")
                }
                var code = connection.responseCode
                var finalUrl = connection.url.toString()
                var contentType = connection.contentType.orEmpty()
                connection.disconnect()
                if (code == 405 || code == 403 || code == 501 || contentType.isBlank()) {
                    connection = (URL(raw).openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = true
                        requestMethod = "GET"
                        connectTimeout = 12000
                        readTimeout = 12000
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
                        setRequestProperty("Range", "bytes=0-0")
                    }
                    code = connection.responseCode
                    finalUrl = connection.url.toString()
                    contentType = connection.contentType.orEmpty()
                    try { connection.inputStream?.close() } catch (_: Exception) {}
                    connection.disconnect()
                }
                runOnUiThread {
                    address.setText(finalUrl)
                    addHistory(finalUrl)
                    val lower = finalUrl.lowercase()
                    val likelyFile = Regex("""\.(apk|zip|rar|7z|pdf|mp4|mkv|mp3|jpg|jpeg|png|exe|msi|docx?)(\?|#|$)""").containsMatchIn(lower) ||
                        (contentType.isNotBlank() && !contentType.contains("text/html", true) && !contentType.contains("application/json", true))
                    if (likelyFile) {
                        AlertDialog.Builder(this)
                            .setTitle("Tujuan file ditemukan")
                            .setMessage("HTTP $code\n$contentType\n\n$finalUrl\n\nMulai download?")
                            .setPositiveButton("DOWNLOAD") { _, _ -> enqueueDownload(finalUrl, null, null, contentType) }
                            .setNegativeButton("BUKA", null)
                            .show()
                        updateStatus("Tujuan redirect ditemukan. Periksa URL sebelum download.")
                    } else {
                        AlertDialog.Builder(this)
                            .setTitle("Tujuan redirect ditemukan")
                            .setMessage("HTTP $code\n$contentType\n\n$finalUrl\n\nIni masih terlihat seperti halaman web, bukan file langsung.")
                            .setPositiveButton("BUKA TUJUAN") { _, _ -> browser.loadUrl(finalUrl) }
                            .setNegativeButton("TUTUP", null)
                            .show()
                        updateStatus("Redirect HTTP selesai. Halaman mungkin masih memerlukan proses situs.")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    updateStatus("Resolver HTTP tidak berhasil: ${e.message ?: "koneksi ditolak"}. Coba buka halaman lalu SCAN LINK.")
                }
            } finally {
                try { connection?.disconnect() } catch (_: Exception) {}
            }
        }
    }

    private fun scanPage() {
        if (browser.url.isNullOrBlank()) {
            updateStatus("Buka halaman terlebih dahulu.")
            return
        }
        hideKeyboard()
        updateStatus("Memindai tautan pada halaman…")
        val js = """
            (function() {
              const bad = /(doubleclick|googlesyndication|googleadservices|adservice|popads|popcash|adsterra|exoclick|taboola|outbrain|tracking|analytics)/i;
              const fileExt = /\.(apk|zip|rar|7z|pdf|mp4|mkv|mp3|jpg|png|exe|msi|docx?)(\?|#|$)/i;
              const words = /(download|unduh|mirror|direct|file|save|get file)/i;
              const seen = new Set();
              const out = [];
              const add = (raw, label) => {
                try {
                  const u = new URL(raw, location.href);
                  if (!/^https?:$/.test(u.protocol) || bad.test(u.hostname) || seen.has(u.href)) return;
                  const isFile = fileExt.test(u.href);
                  const isDownload = words.test(label || '') || /[?&](download|attachment)=?(1|true)?/i.test(u.search);
                  if (isFile || isDownload) {
                    seen.add(u.href);
                    out.push({url:u.href, label:(label || (isFile ? 'File link' : 'Download link')).slice(0,100), direct:isFile});
                  }
                } catch(e) {}
              };
              document.querySelectorAll('a[href]').forEach(a => {
                const text = (a.innerText || a.textContent || a.getAttribute('aria-label') || '').trim().replace(/\s+/g,' ').slice(0,100);
                add(a.href, text);
              });
              document.querySelectorAll('[data-href],[data-url],button[onclick],form[action],meta[http-equiv="refresh"]').forEach(el => {
                add(el.getAttribute('data-href') || el.getAttribute('data-url') || el.getAttribute('action') || '',
                    el.innerText || el.textContent || el.getAttribute('aria-label') || 'Download action');
                const oc = el.getAttribute('onclick') || '';
                const matches = oc.match(/https?:\/\/[^\s'"`<>]+/g) || [];
                matches.forEach(u => add(u, el.innerText || 'Download action'));
              });
              out.sort((a,b) => Number(b.direct)-Number(a.direct));
              LinkScanner.onLinksFound(JSON.stringify(out.slice(0,40)));
            })();
        """.trimIndent()
        browser.evaluateJavascript(js, null)
    }

    private fun showCandidates(json: String) {
        try {
            val arr = JSONArray(json)
            if (arr.length() == 0) {
                updateStatus("Tidak ditemukan link file yang terlihat. Provider: ${identifyProvider(browser.url.orEmpty())}.")
                AlertDialog.Builder(this)
                    .setTitle("Belum ada link unduh ditemukan")
                    .setMessage("Scanner belum menemukan URL unduhan yang terlihat pada halaman ini. Ini bukan bukti bahwa CAPTCHA muncul. Coba tunggu halaman selesai dimuat, tekan tombol unduh di situs, lalu scan lagi. Link yang dibuat secara dinamis atau berada di dalam proteksi situs mungkin tidak dapat dibaca scanner umum.")
                    .setPositiveButton("OK", null).show()
                return
            }
            val labels = Array(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                val label = o.optString("label", "Download")
                val host = Uri.parse(o.getString("url")).host ?: "link"
                val direct = if (o.optBoolean("direct")) " • FILE" else " • LINK"
                "$label\n$host$direct"
            }
            AlertDialog.Builder(this)
                .setTitle("${arr.length()} kandidat link ditemukan")
                .setItems(labels) { _, which ->
                    val item = arr.getJSONObject(which)
                    val url = item.getString("url")
                    if (item.optBoolean("direct")) {
                        enqueueDownload(url, browser.settings.userAgentString, null, null)
                    } else {
                        address.setText(url)
                        browser.loadUrl(url)
                        updateStatus("Membuka kandidat link. Scan lagi jika halaman berikutnya memuat link file.")
                    }
                }
                .setNegativeButton("Tutup", null)
                .show()
            updateStatus("${arr.length()} kandidat ditemukan. Pilih link yang sesuai.")
        } catch (_: Exception) {
            updateStatus("Gagal membaca hasil scan.")
        }
    }

    private fun openAddress() {
        var raw = address.text.toString().trim()
        if (raw.isBlank()) { updateStatus("Masukkan link terlebih dahulu."); return }
        if (!raw.contains("://")) raw = "https://$raw"
        val uri = try { Uri.parse(raw) } catch (_: Exception) { null }
        if (uri == null || (uri.scheme != "https" && uri.scheme != "http") || uri.host.isNullOrBlank()) {
            updateStatus("URL tidak valid. Gunakan alamat http atau https."); return
        }
        hideKeyboard()
        address.clearFocus()
        addHistory(raw)
        browser.loadUrl(raw)
        updateStatus("Provider: ${identifyProvider(raw)} • Membuka link…")
    }

    private fun pasteClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            address.setText(clip.getItemAt(0).coerceToText(this).toString())
            updateStatus("Link ditempel dari clipboard.")
        } else updateStatus("Clipboard kosong.")
    }

    private fun enqueueDownload(url: String, userAgent: String?, disposition: String?, mime: String?) {
        try {
            val uri = Uri.parse(url)
            if (uri.scheme != "https" && uri.scheme != "http") {
                updateStatus("Skema download tidak didukung."); return
            }
            val filename = android.webkit.URLUtil.guessFileName(url, disposition, mime)
            val request = DownloadManager.Request(uri)
                .setTitle(filename)
                .setDescription("LINKBYPASS PRO • Download")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
            if (!userAgent.isNullOrBlank()) request.addRequestHeader("User-Agent", userAgent)
            val cookie = CookieManager.getInstance().getCookie(url)
            if (!cookie.isNullOrBlank()) request.addRequestHeader("Cookie", cookie)
            (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            updateStatus("Download dimulai: $filename")
            Toast.makeText(this, "Download masuk ke notifikasi", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            updateStatus("Download gagal: ${e.message ?: "kesalahan"}")
        }
    }

    private fun openDownloads() {
        try {
            startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
        } catch (_: Exception) {
            Toast.makeText(this, "Buka folder Download perangkat untuk melihat file.", Toast.LENGTH_LONG).show()
        }
    }

    private fun addHistory(url: String) {
        if (history.firstOrNull() != url) history.add(0, url)
        while (history.size > 8) history.removeAt(history.lastIndex)
        if (::historyText.isInitialized) historyText.text = history.joinToString("\n") { "• $it" }
    }

    private fun updateStatus(message: String) {
        if (::status.isInitialized) status.text = "● $message"
    }

    private fun actionButton(label: String, action: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            textSize = 12f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            includeFontPadding = false
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
            minHeight = dp(50)
            setPadding(dp(5), dp(8), dp(5), dp(8))
            background = rounded(Color.rgb(52, 14, 20), 11, red)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                hideKeyboard()
                action()
            }
        }

    private fun rounded(color: Int, radius: Int, stroke: Int) =
        GradientDrawable().apply { setColor(color); cornerRadius = radius.toFloat(); setStroke(1, stroke) }

    private fun weighted(left: Int = 0) = LinearLayout.LayoutParams(0, dp(54), 1f).apply {
        if (left > 0) leftMargin = dp(left)
        topMargin = dp(5)
        bottomMargin = dp(2)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun hideKeyboard() {
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(address.windowToken, 0)
            address.clearFocus()
        } catch (_: Exception) { }
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    override fun onBackPressed() {
        if (::browser.isInitialized && browser.canGoBack()) browser.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::browser.isInitialized) { browser.stopLoading(); browser.destroy() }
        super.onDestroy()
    }
}
