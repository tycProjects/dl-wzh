package com.example.musicplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.webkit.WebViewAssetLoader
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Выбранные пользователем файлы: id -> Uri (страница читает их по адресу /pick/<id>) */
object Picked {
    val map = ConcurrentHashMap<String, Uri>()
    val counter = AtomicInteger()
}

class MainActivity : ComponentActivity() {
    private lateinit var web: WebView
    private var fileCb: ValueCallback<Array<Uri>>? = null

    private val picker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        fileCb?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(r.resultCode, r.data)); fileCb = null
    }
    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // Системный выбор НЕСКОЛЬКИХ аудиофайлов
    private val pickFiles = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> deliver(uris) }
    // Выбор целой папки: добавляются все треки внутри
    private val pickTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> if (uri != null) scanTree(uri) }

    fun openFiles() { pickFiles.launch(arrayOf("audio/*", "application/ogg", "application/octet-stream")) }
    fun openFolder() { pickTree.launch(null) }

    private fun nameOf(u: Uri): String =
        contentResolver.query(u, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null } ?: "track"

    private fun deliver(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val arr = JSONArray()
        for (u in uris) {
            val id = Picked.counter.incrementAndGet().toString()
            Picked.map[id] = u
            arr.put(JSONObject().put("n", nameOf(u)).put("u", "/pick/" + id).put("t", contentResolver.getType(u) ?: ""))
        }
        web.post { web.evaluateJavascript("window.__picked&&window.__picked(" + arr.toString() + ")", null) }
    }

    private fun scanTree(tree: Uri) {
        Thread {
            val out = mutableListOf<Uri>()
            val re = Regex(".*\\.(mp3|m4a|aac|wav|ogg|opus|flac)\$", RegexOption.IGNORE_CASE)
            fun walk(d: DocumentFile) {
                for (f in d.listFiles()) {
                    if (f.isDirectory) walk(f)
                    else if ((f.type ?: "").startsWith("audio/") || re.matches(f.name ?: "")) out.add(f.uri)
                }
            }
            DocumentFile.fromTreeUri(this, tree)?.let { walk(it) }
            runOnUiThread { deliver(out) }
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/pick/", WebViewAssetLoader.PathHandler { path ->
                val uri = Picked.map[path]
                if (uri == null) null else try {
                    WebResourceResponse(contentResolver.getType(uri) ?: "audio/mpeg", null, contentResolver.openInputStream(uri))
                } catch (e: Exception) { null }
            }).build()
        web = WebView(this)
        setContentView(web)
        Bridge.web = web
        web.settings.apply {
            javaScriptEnabled = true; domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false; allowFileAccess = false
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(v: WebView, r: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(r.url)
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(w: WebView, cb: ValueCallback<Array<Uri>>, p: FileChooserParams): Boolean {
                fileCb?.onReceiveValue(null); fileCb = cb
                return try { picker.launch(p.createIntent().putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)); true } catch (e: Exception) { fileCb = null; false }
            }
        }
        web.addJavascriptInterface(AndroidBridge(this), "AndroidMedia")
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
        // «Назад» сворачивает приложение, музыка продолжает играть
        onBackPressedDispatcher.addCallback(this) { moveTaskToBack(true) }
    }

    /** В горизонтальном положении прячем строку состояния и навигацию (свайп от края их покажет) */
    private fun applyBars() {
        val c = WindowCompat.getInsetsController(window, window.decorView)
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.hide(WindowInsetsCompat.Type.systemBars())
        } else c.show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); applyBars() }

    override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus); if (hasFocus) applyBars() }

    // Важно: НЕ вызываем web.onPause(), иначе звук остановится при блокировке экрана

    override fun onDestroy() {
        Bridge.web = null
        PlayerState.playing = false
        PlayerWidget.refresh(this)
        stopService(Intent(this, MediaPlaybackService::class.java))
        web.destroy()
        super.onDestroy()
    }
}
