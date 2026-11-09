package com.nomango.browser

import android.Manifest
import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.res.ColorStateList
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.view.ViewConfiguration
import android.view.HapticFeedbackConstants
import android.util.LruCache
import android.graphics.drawable.BitmapDrawable
import android.graphics.Matrix
import android.graphics.Canvas
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.SystemClock
import android.os.Looper
import android.os.Message
import android.os.Parcel
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Base64
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.CheckBox
import android.widget.ListView
import android.widget.BaseAdapter
import android.widget.AbsListView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

private class Pal(
    val bar: Int, val pill: Int, val text: Int, val sub: Int,
    val accent: Int, val bg: Int, val card: Int, val line: Int, val dark: Boolean
)

private class Eng(val web: String, val img: String, val safe: String)

/** One download request: where from, how to ask the server, and what it is. [kind] is image, video, audio or empty. */
private class DlReq(
    val url: String, val ua: String, val cd: String, val mime: String, val ref: String, val kind: String
)

/** An output file that is being written (MediaStore on Android 10+, the Downloads folder before). */
private class SaveTarget(val out: OutputStream, val uri: Uri?, val file: File?, val name: String, val mime: String)

/** One tab as it was written to disk. [state] is the WebView back/forward history. */
private class SavedTab(
    val url: String, val title: String, val desktop: Boolean, val home: Boolean,
    val parent: Int, val state: ByteArray?, val id: String, val pinned: Boolean
)

/** A blob: download that is being streamed from the page, chunk by chunk. */
private class BlobJob(val kind: String, val mime: String, val source: String) {
    var target: SaveTarget? = null
    var written: Long = 0L
}

/**
 * NOMAN GO browser shell.
 *
 *  - native top toolbar: Back, Forward, Refresh, URL/Search bar, three-dot menu
 *  - one WebView per tab (normal or incognito-style), desktop/mobile mode per tab
 *  - the home page, History, Bookmarks, Downloads and Settings screens are the bundled
 *    assets/index.html, talking to this class through the "NomanNative" JavaScript bridge
 */
class MainActivity : Activity() {

    companion object {
        private const val HOME = "file:///android_asset/index.html"
        private const val ASSET = "file:///android_asset/"
        private const val REQ_FILE = 11
        private const val REQ_STORAGE = 12
        private const val REQ_VOICE = 13
        private const val TR_CAP = 150000
        private val STAR: Int = 0xFFF5A524.toInt()
        private val ACC_LIGHT = arrayOf("#E5192B", "#2B6BFF", "#10B981", "#F97316", "#6366F1")
        private val ACC_DARK = arrayOf("#FF2D3F", "#4D86FF", "#34D399", "#FB923C", "#818CF8")
        private val SCOPES = arrayOf("Images", "YouTube", "Wikipedia", "GitHub", "Maps", "Shopping")
        private val URL_RE = Regex(
            "^(https?://)?(([\\w-]+\\.)+[a-z]{2,}|localhost|\\d{1,3}(\\.\\d{1,3}){3})(:\\d+)?(/\\S*)?\$",
            RegexOption.IGNORE_CASE
        )
        /** remembers the file name of <a download> links, so a blob: download keeps its real name */
        private const val HOOK_JS =
            "(function(){if(window.__ngP)return;window.__ngP=1;window.__ngDl={};" +
                "function note(a){try{if(a&&a.hasAttribute&&a.hasAttribute('download')&&a.href){" +
                "window.__ngDl[a.href]=a.getAttribute('download')||''}}catch(e){}}" +
                "try{var oc=HTMLAnchorElement.prototype.click;" +
                "HTMLAnchorElement.prototype.click=function(){note(this);return oc.apply(this,arguments)}}catch(e){}" +
                "document.addEventListener('click',function(e){var a=e.target&&e.target.closest?" +
                "e.target.closest('a[download]'):null;note(a)},true);})();"

        /** reads a blob: URL inside the page and streams it to NomanBlob in 256 KB pieces */
        private const val BLOB_JS =
            "(function(u,t){var B=window.NomanBlob;if(!B)return;" +
                "function fail(m){try{B.fail(t,String(m||'error'))}catch(e){}}" +
                "try{var nm='';try{nm=(window.__ngDl&&window.__ngDl[u])||''}catch(e){}" +
                "fetch(u).then(function(r){return r.blob()}).then(function(b){" +
                "B.begin(t,b.type||'',nm,String(b.size));var CH=262144,pos=0;" +
                "function next(){if(pos>=b.size){B.end(t);return}" +
                "var fr=new FileReader();" +
                "fr.onload=function(){var s=String(fr.result);var i=s.indexOf(',');" +
                "B.chunk(t,s.substring(i+1));pos+=CH;setTimeout(next,0)};" +
                "fr.onerror=function(){fail('read')};" +
                "fr.readAsDataURL(b.slice(pos,pos+CH))}" +
                "next()}).catch(function(e){fail(e)})}catch(e){fail(e)}})(__U__,__T__);"

        /** finds an image, video or audio under a long press, even behind transparent overlays */
        private const val PROBE_JS =
            "(function(x,y){try{var els=document.elementsFromPoint?document.elementsFromPoint(x,y):" +
                "[document.elementFromPoint(x,y)];var link='',res=null;" +
                "for(var i=0;i<els.length;i++){var e=els[i];if(!e)continue;" +
                "var tn=(e.tagName||'').toLowerCase();" +
                "if(!link){var a=e.closest?e.closest('a[href]'):null;if(a)link=a.href}" +
                "if(!res){if(tn==='img'){var u=e.currentSrc||e.src;if(u)res={k:'image',u:u}}" +
                "else if(tn==='video'||tn==='audio'){var s=e.currentSrc||e.src;" +
                "if(!s){var so=e.querySelector('source[src]');if(so)s=so.src}" +
                "if(s)res={k:tn,u:s}}}}" +
                "if(res){res.l=link;return JSON.stringify(res)}return ''}catch(err){return ''}})(__X__,__Y__)"

        private const val DESKTOP_JS =
            "(function(){var m=document.querySelector('meta[name=viewport]');" +
                "if(!m){m=document.createElement('meta');m.name='viewport';" +
                "(document.head||document.documentElement).appendChild(m);}" +
                "m.setAttribute('content','width=980');})();"
    }

    private val ENGINES = mapOf(
        "google" to Eng("https://www.google.com/search?q=", "https://www.google.com/search?tbm=isch&q=", "&safe=active"),
        "bing" to Eng("https://www.bing.com/search?q=", "https://www.bing.com/images/search?q=", "&adlt=strict"),
        "ddg" to Eng("https://duckduckgo.com/?q=", "https://duckduckgo.com/?iax=images&ia=images&q=", "&kp=1"),
        "yahoo" to Eng("https://search.yahoo.com/search?p=", "https://images.search.yahoo.com/search/images?p=", "&vm=r"),
        "brave" to Eng("https://search.brave.com/search?q=", "https://search.brave.com/images?q=", "&safesearch=strict")
    )

    private inner class Tab(val incognito: Boolean) {
        private var w: WebView? = null

        /** created on first use, so a restored background tab costs nothing until it is opened */
        val web: WebView
            get() {
                var x = w
                if (x == null) {
                    x = WebView(this@MainActivity)
                    w = x
                    attachWeb(this)
                }
                return x
            }

        fun peek(): WebView? = w

        /** restored from the last session and not loaded yet */
        var dormant: Boolean = false
        var pendingState: ByteArray? = null

        /** content type of the page itself, so a bare image, video or audio page can offer Download */
        var docMime: String = ""

        /** pinned tabs stay at the top of the list and are not closed by "Close all" unless you tick the box */
        var pinned: Boolean = false

        /** the site icon (null until the page reports one) */
        var favicon: Bitmap? = null
        var iconTried: Boolean = false

        /** takes a fresh preview picture a moment after a page has loaded */
        val thumbRun: Runnable = Runnable { captureThumb(this@Tab) }

        /** stable name of this tab (its preview picture is stored under it) */
        var id: String = UUID.randomUUID().toString().substring(0, 12)

        /** translation of the page: the language it was translated to ("" when not translated) */
        var trTarget: String = ""
        var trGen: Int = 0
        var trBusy: Boolean = false
        var trChars: Int = 0
        var trDone: Int = 0
        var title: String = ""
        var url: String = ""
        var desktop: Boolean = false
        var trusted: Boolean = false
        /** when a load really ends, or has got stuck, is decided here */
        val guard = LoadGuard()
        var loading: Boolean
            get() = guard.loading
            set(v) {
                guard.loading = v
            }
        val watch: Runnable = Runnable { loadWatchTick(this@Tab) }
        var progress: Int = 0
        var parent: Tab? = null
        var errorPending: Boolean = false
        var errorShown: Boolean = false
        var manualHost: String? = null
    }

    // ---------- views ----------
    private lateinit var main: LinearLayout
    private lateinit var toolbar: LinearLayout
    private lateinit var btnHome: ImageButton
    private lateinit var btnMic: ImageButton
    private lateinit var urlPill: LinearLayout
    private lateinit var urlIcon: ImageView
    private lateinit var urlInput: EditText
    private lateinit var btnClear: ImageButton
    private lateinit var btnMenu: ImageButton
    private lateinit var btnTabs: FrameLayout
    private lateinit var tabCount: TextView
    private lateinit var progress: LoadBar
    private lateinit var content: FrameLayout
    private lateinit var suggestPanel: ScrollView
    private lateinit var suggestList: LinearLayout
    private lateinit var tabsOverlay: FrameLayout
    private lateinit var tabsTitle: TextView
    private lateinit var btnNewTab: TextView
    private lateinit var btnNewInc: ImageButton
    private lateinit var btnTabsMenu: ImageButton
    private lateinit var btnTabsDone: ImageButton
    private lateinit var tabsBody: FrameLayout
    private lateinit var tabsEmpty: TextView
    private lateinit var tabSearch: EditText
    private lateinit var tabSearchClear: ImageButton
    private lateinit var tabsSearchPill: LinearLayout
    private lateinit var tabSearchIcon: ImageView
    private lateinit var btnCloseAll: ImageButton
    private lateinit var tabsListView: ListView
    private lateinit var tabAdapter: TabAdapter
    private val shown = ArrayList<Tab>()
    private var tabQuery = ""
    private var shineAnim: ValueAnimator? = null
    private var shineView: View? = null
    private val iconCache = object : LruCache<String, Bitmap>(2 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private lateinit var toastView: TextView

    // ---------- state ----------
    private lateinit var store: Store
    private var st: JSONObject = JSONObject()
    private val tabs = ArrayList<Tab>()
    private var current: Tab? = null
    private val ui = Handler(Looper.getMainLooper())
    private lateinit var uaMobile: String
    private lateinit var uaDesktop: String
    private val pillBg = GradientDrawable()
    private val tabBg = GradientDrawable()
    private var tabsOpen = false
    private var lastCount = 0
    private var lastRate = 0f
    private var lastPalKey = ""
    private var spinAnim: ObjectAnimator? = null
    private val menuAnims = ArrayList<Animator>()
    private var barColor = 0
    private var iconRes = 0
    private var settingText = false
    private var lastLoading = false
    private var animRows = true
    private var rowIdx = 0
    private var remoteHolder: LinearLayout? = null
    private var sugToken = 0
    private var sugRunnable: Runnable? = null
    private var menuPopup: PopupWindow? = null
    private var fileCb: ValueCallback<Array<Uri>>? = null
    private var customView: View? = null
    private var customCb: WebChromeClient.CustomViewCallback? = null
    private var pendingReq: DlReq? = null
    private var pendingTab: Tab? = null
    private var sessionReady = false
    private val io = Executors.newSingleThreadExecutor()
    private val saveSessionRun = Runnable { saveSession(false) }
    private val blobJobs = ConcurrentHashMap<String, BlobJob>()
    private val blobBridge = BlobBridge()
    private var dlReceiver: BroadcastReceiver? = null
    private var touchX = 0f
    private var touchY = 0f
    private val thumbPool = Executors.newSingleThreadExecutor()
    private val trPool = Executors.newFixedThreadPool(2)
    private val trJs: String by lazy { assetText("translate.js") }
    private val pullJs: String by lazy { assetText("pull.js") }
    private val thumbCache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private lateinit var ptrView: PullIndicator
    private val pullBridge = PullBridge()
    private var ptrArmed = false
    private var ptrPulling = false
    private var ptrPast = false
    private var ptrRefreshing = false
    private var ptrDist = 0f
    private var ptrStartX = 0f
    private var ptrStartY = 0f
    private var touchSlopPx = 0
    @Volatile
    private var ptrOk = false
    private val ptrStopRun = Runnable { ptrStop() }
    private var keepTabsOpen = false

    private val toastHide = Runnable {
        toastView.animate().alpha(0f).translationY(dpf(12f)).setDuration(d(200)).start()
    }

    // =====================================================================
    // lifecycle
    // =====================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        store = Store(this)
        st = store.settings()
        migrateStartup()

        main = findViewById(R.id.main)
        toolbar = findViewById(R.id.toolbar)
        btnHome = findViewById(R.id.btnHome)
        btnMic = findViewById(R.id.btnMic)
        urlPill = findViewById(R.id.urlPill)
        urlIcon = findViewById(R.id.urlIcon)
        urlInput = findViewById(R.id.urlInput)
        btnClear = findViewById(R.id.btnClear)
        btnMenu = findViewById(R.id.btnMenu)
        btnTabs = findViewById(R.id.btnTabs)
        tabCount = findViewById(R.id.tabCount)
        progress = findViewById(R.id.progress)
        content = findViewById(R.id.content)
        suggestPanel = findViewById(R.id.suggestPanel)
        suggestList = findViewById(R.id.suggestList)
        tabsOverlay = findViewById(R.id.tabsOverlay)
        tabsTitle = findViewById(R.id.tabsTitle)
        btnNewTab = findViewById(R.id.btnNewTab)
        btnNewInc = findViewById(R.id.btnNewInc)
        btnTabsMenu = findViewById(R.id.btnTabsMenu)
        btnTabsDone = findViewById(R.id.btnTabsDone)
        tabsBody = findViewById(R.id.tabsBody)
        tabsEmpty = findViewById(R.id.tabsEmpty)
        tabSearch = findViewById(R.id.tabSearch)
        tabSearchClear = findViewById(R.id.tabSearchClear)
        tabsSearchPill = findViewById(R.id.tabsSearchPill)
        tabSearchIcon = findViewById(R.id.tabSearchIcon)
        btnCloseAll = findViewById(R.id.btnCloseAll)
        toastView = findViewById(R.id.toast)

        val def = WebSettings.getDefaultUserAgent(this)
        uaMobile = def.replace("; wv", "")
        val ver = Regex("Chrome/([\\d.]+)").find(def)?.groupValues?.get(1) ?: "120.0.0.0"
        uaDesktop = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$ver Safari/537.36"

        pillBg.cornerRadius = dpf(20f)
        tabBg.cornerRadius = dpf(8f)
        CookieManager.getInstance().setAcceptCookie(true)
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        setupToolbar()
        setupTabsOverlay()
        setupPull()
        applyTheme(false)
        applySettings()
        startSession(intent)
        registerDownloadReceiver()
        applyBrandFont()
        playToolbarIntro()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val u = intent?.data?.toString()
        if (u != null) openOrSwitch(u)
    }

    private fun urlKey(u: String): String = u.trim().substringBefore('#').trimEnd('/').lowercase()

    /** The tab that is already showing [u], so opening the same link twice does not pile up identical tabs. */
    private fun findTabByUrl(u: String): Tab? {
        val k = urlKey(u)
        if (k.isEmpty()) return null
        for (t in tabs) {
            if (!t.incognito && !t.trusted && urlKey(t.url) == k) return t
        }
        return null
    }

    private fun openOrSwitch(u: String) {
        val ex = findTabByUrl(u)
        if (ex != null) switchTo(ex, true) else newTab(u)
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
        stopIdle()
        current?.web?.onPause()
    }

    override fun onStop() {
        super.onStop()
        val cur = current
        if (cur != null) captureThumb(cur)
        saveSession(true)
    }

    override fun onResume() {
        super.onResume()
        current?.web?.onResume()
        startIdle()
    }

    override fun onDestroy() {
        ui.removeCallbacks(saveSessionRun)
        try {
            val r = dlReceiver
            if (r != null) unregisterReceiver(r)
        } catch (e: Exception) {
        }
        dlReceiver = null
        shineAnim?.cancel()
        trPool.shutdownNow()
        thumbPool.shutdown()
        for (t in tabs) {
            val w = t.peek()
            if (w != null) {
                (w.parent as? ViewGroup)?.removeView(w)
                w.destroy()
            }
        }
        tabs.clear()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyTheme(true)
    }

    private fun startSession(i: Intent?) {
        val data = i?.data?.toString()
        if (stB("clrx", false)) store.clearHistory()
        val mode = stS("start", "last")
        val restored = restoreSession()
        if (restored) {
            val cur = current
            if (data != null) {
                openOrSwitch(data)
            } else if (mode == "page" && stS("startUrl", "").isNotBlank()) {
                newTab(resolve(stS("startUrl", "")))
            } else if (mode == "ntp" && !(cur != null && cur.trusted && cur.url == HOME)) {
                newTab(null)
            }
            if (stB("incs", false) && data == null) newTab(null, true)
        } else {
            val url: String? = when {
                data != null -> data
                mode == "last" -> store.get(Store.K_LAST)
                mode == "page" -> stS("startUrl", "").let { if (it.isBlank()) null else resolve(it) }
                else -> null
            }
            newTab(url, stB("incs", false) && data == null)
        }
        sessionReady = true
        cleanThumbs()
        scheduleSessionSave()
    }

    /** One time: "New tab page" used to be the default and it threw the open tabs away. "Where I left off" is the new default. */
    private fun migrateStartup() {
        if (st.optInt("mig3", 0) >= 1) return
        try {
            if (st.optString("start", "ntp") == "ntp") st.put("start", "last")
            st.put("mig3", 1)
            store.set(Store.K_ST, st.toString())
        } catch (e: Exception) {
        }
    }

    // =====================================================================
    // session: every tab, its back/forward history and the active page survive leaving or killing the app
    // =====================================================================
    private fun sessionDir(): File {
        val d = File(filesDir, "session")
        if (!d.exists()) d.mkdirs()
        return d
    }

    private fun scheduleSessionSave() {
        if (!sessionReady) return
        ui.removeCallbacks(saveSessionRun)
        ui.postDelayed(saveSessionRun, 1200L)
    }

    private fun bundleToBytes(b: Bundle): ByteArray? {
        val p = Parcel.obtain()
        return try {
            p.writeBundle(b)
            p.marshall()
        } catch (e: Exception) {
            null
        } finally {
            p.recycle()
        }
    }

    private fun bytesToBundle(bytes: ByteArray): Bundle? {
        val p = Parcel.obtain()
        return try {
            p.unmarshall(bytes, 0, bytes.size)
            p.setDataPosition(0)
            p.readBundle(MainActivity::class.java.classLoader)
        } catch (e: Exception) {
            null
        } finally {
            p.recycle()
        }
    }

    /** Incognito tabs are never written to disk. [sync] is used when the app goes to the background. */
    private fun saveSession(sync: Boolean) {
        if (!sessionReady) return
        ui.removeCallbacks(saveSessionRun)
        val keep = ArrayList<Tab>()
        for (t in tabs) if (!t.incognito) keep.add(t)
        val arr = JSONArray()
        val blobs = ArrayList<ByteArray?>()
        var active = 0
        for ((i, t) in keep.withIndex()) {
            if (t === current) active = i
            val home = t.trusted || t.url.isEmpty()
            val o = JSONObject()
            o.put("url", if (home) HOME else t.url)
            o.put("title", t.title)
            o.put("desktop", t.desktop)
            o.put("home", home)
            o.put("id", t.id)
            o.put("pinned", t.pinned)
            val par = t.parent
            o.put("parent", if (par != null) keep.indexOf(par) else -1)
            arr.put(o)
            var bytes: ByteArray? = null
            if (!home) {
                val w = t.peek()
                if (w != null && !t.dormant) {
                    val b = Bundle()
                    val list = w.saveState(b)
                    if (list != null && list.size > 0) bytes = bundleToBytes(b)
                } else {
                    bytes = t.pendingState
                }
            }
            blobs.add(bytes)
        }
        val json = JSONObject().put("v", 1).put("active", active).put("tabs", arr).toString()
        val job = Runnable { writeSession(json, blobs) }
        if (sync) {
            try {
                io.submit(job).get(2500L, TimeUnit.MILLISECONDS)
            } catch (e: Exception) {
            }
        } else {
            io.execute(job)
        }
    }

    private fun writeSession(json: String, blobs: List<ByteArray?>) {
        try {
            val dir = sessionDir()
            for ((i, b) in blobs.withIndex()) {
                val f = File(dir, "tab_$i.bin")
                if (b != null) {
                    val tmp = File(dir, "tab_$i.tmp")
                    FileOutputStream(tmp).use { it.write(b) }
                    if (!tmp.renameTo(f)) {
                        f.delete()
                        tmp.renameTo(f)
                    }
                } else if (f.exists()) {
                    f.delete()
                }
            }
            val files = dir.listFiles()
            if (files != null) {
                for (f in files) {
                    val n = f.name
                    if (n.startsWith("tab_") && n.endsWith(".bin")) {
                        val idx = n.substring(4, n.length - 4).toIntOrNull()
                        if (idx == null || idx >= blobs.size) f.delete()
                    }
                }
            }
            val tmp = File(dir, "session.tmp")
            FileOutputStream(tmp).use { it.write(json.toByteArray(Charsets.UTF_8)) }
            val f = File(dir, "session.json")
            if (!tmp.renameTo(f)) {
                f.delete()
                tmp.renameTo(f)
            }
        } catch (e: Exception) {
        }
    }

    private fun readSession(): Pair<List<SavedTab>, Int>? {
        try {
            val dir = sessionDir()
            val f = File(dir, "session.json")
            if (!f.exists()) return null
            val root = JSONObject(f.readText(Charsets.UTF_8))
            val arr = root.optJSONArray("tabs") ?: return null
            val out = ArrayList<SavedTab>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: return null
                val url = o.optString("url", "")
                val home = o.optBoolean("home", false) || url.isEmpty() || url.startsWith(ASSET)
                val bf = File(dir, "tab_$i.bin")
                var state: ByteArray? = null
                if (!home && bf.exists() && bf.length() > 0L && bf.length() < 16L * 1024L * 1024L) state = bf.readBytes()
                out.add(
                    SavedTab(
                        if (home) HOME else url, o.optString("title", ""), o.optBoolean("desktop", false),
                        home, o.optInt("parent", -1), state,
                        o.optString("id", UUID.randomUUID().toString().substring(0, 12)),
                        o.optBoolean("pinned", false)
                    )
                )
            }
            return Pair(out, root.optInt("active", 0))
        } catch (e: Exception) {
            return null
        }
    }

    /** Rebuilds all tabs from the last run. Only the active one is loaded now, the rest wake up when opened. */
    private fun restoreSession(): Boolean {
        val r = readSession() ?: return false
        val list = r.first
        if (list.isEmpty()) return false
        val made = ArrayList<Tab>()
        for (sv in list) {
            val t = Tab(false)
            t.id = sv.id
            t.pinned = sv.pinned
            t.url = sv.url
            t.title = sv.title
            t.desktop = sv.desktop
            t.trusted = sv.home
            t.dormant = true
            t.pendingState = sv.state
            tabs.add(t)
            made.add(t)
        }
        for ((i, sv) in list.withIndex()) {
            if (sv.parent >= 0 && sv.parent < made.size && sv.parent != i) made[i].parent = made[sv.parent]
        }
        updateTabCount()
        switchTo(made[r.second.coerceIn(0, made.size - 1)], false)
        return true
    }

    /** Loads a restored tab the first time it is shown: WebView history first, plain URL if that fails. */
    private fun ensureLoaded(t: Tab) {
        if (!t.dormant) return
        t.dormant = false
        val bytes = t.pendingState
        t.pendingState = null
        if (t.trusted || t.url.isEmpty()) {
            t.trusted = true
            loadWithMode(t, HOME)
            return
        }
        var ok = false
        if (bytes != null) {
            val b = bytesToBundle(bytes)
            if (b != null) {
                try {
                    ok = t.web.restoreState(b) != null
                } catch (e: Exception) {
                    ok = false
                }
            }
        }
        if (!ok) loadWithMode(t, t.url)
    }

    // =====================================================================
    // helpers
    // =====================================================================
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
    private fun dpf(v: Float): Float = v * resources.displayMetrics.density
    private fun c(hex: String): Int = Color.parseColor(hex)
    private fun stS(k: String, d: String): String = st.optString(k, d)
    private fun stB(k: String, d: Boolean): Boolean = st.optBoolean(k, d)
    private fun stI(k: String, d: Int): Int = st.optInt(k, d)

    /** animation duration; 0 when the user chose reduced animations / battery saver */
    private fun d(ms: Long): Long = if (stS("anim", "full") == "reduced" || stB("eco", false)) 0L else ms

    private fun zoomPct(): Int = ((stS("zoom", "1").toFloatOrNull() ?: 1f) * 100f).toInt().coerceIn(50, 200)

    private fun rounded(fill: Int, radiusDp: Float, strokeColor: Int = 0, strokeDp: Int = 0): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(fill)
        g.cornerRadius = dpf(radiusDp)
        if (strokeDp > 0) g.setStroke(dp(strokeDp), strokeColor)
        return g
    }

    private fun circle(fill: Int): GradientDrawable {
        val g = GradientDrawable()
        g.shape = GradientDrawable.OVAL
        g.setColor(fill)
        return g
    }

    private fun ripple(v: View) {
        val tv = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true)
        v.setBackgroundResource(tv.resourceId)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(urlInput.windowToken, 0)
    }

    private fun dropFocus() {
        urlInput.clearFocus()
        main.requestFocus()
        hideKeyboard()
    }

    private fun esc(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun displayUrl(url: String): String {
        val q = searchQueryOf(url)
        if (q != null) return q
        val u = Uri.parse(url)
        return u.host?.removePrefix("www.") ?: url
    }

    /** The searched text when [url] is a results page of a known search engine, else null. */
    private fun searchQueryOf(url: String): String? {
        val u = Uri.parse(url)
        val h = (u.host ?: return null).removePrefix("www.")
        val path = u.path ?: ""
        val isSearch = (h.startsWith("google.") && path == "/search") ||
            (h.endsWith("bing.com") && path.startsWith("/search")) ||
            h == "duckduckgo.com" || h == "search.brave.com" || h == "search.yahoo.com"
        if (!isSearch) return null
        val q = u.getQueryParameter("q") ?: u.getQueryParameter("p")
        return if (q.isNullOrBlank()) null else q
    }

    private fun tabColor(s: String): Int {
        val cols = arrayOf("#4F8BFF", "#FF3B4E", "#16A56A", "#8B5CF6", "#F5A524", "#E84FA0", "#0FB5A5")
        var h = 0
        for (ch in s) h = (h * 31 + ch.code) and 0x7fffffff
        return c(cols[h % cols.size])
    }

    private fun showToast(msg: String) {
        toastView.text = msg
        ui.removeCallbacks(toastHide)
        toastView.animate().cancel()
        toastView.translationY = dpf(14f)
        toastView.animate().alpha(1f).translationY(0f).setDuration(d(220))
            .setInterpolator(OvershootInterpolator(1.6f)).start()
        ui.postDelayed(toastHide, 1900L)
    }

    // =====================================================================
    // settings + theme
    // =====================================================================
    private fun applySettings() {
        st = store.settings()
        val z = zoomPct()
        for (t in tabs) t.peek()?.settings?.textZoom = if (t.trusted) 100 else z
        for (t in tabs) {
            if (!t.trusted && t !== current) {
                val want = modeFor(t, hostOf(t.url))
                if (want != t.desktop) {
                    t.desktop = want
                    if (t.peek() != null) applyMode(t)
                }
            }
        }
        if (stB("wake", false)) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        applyMic()
        applyRefreshRate()
        applyTheme(true)
    }

    private fun pal(): Pal {
        val inc = current?.incognito == true
        val th = stS("theme", "system")
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val dark = inc || th == "dark" || (th == "system" && night)
        val a = stI("acc", 0).coerceIn(0, 4)
        return when {
            inc -> Pal(c("#150F2B"), c("#241B45"), c("#F1ECFF"), c("#A69BD0"), c("#9A73FF"), c("#0B0816"), c("#1B1336"), c("#2E2352"), true)
            dark -> Pal(c("#101015"), c("#1D1D26"), c("#F5F5FA"), c("#9A9AAE"), c(ACC_DARK[a]), c("#050507"), c("#17171F"), c("#26262F"), true)
            else -> Pal(c("#FFFFFF"), c("#EEF0F6"), c("#0D0D14"), c("#6F6F82"), c(ACC_LIGHT[a]), c("#F6F6F9"), c("#FFFFFF"), c("#E3E5EE"), false)
        }
    }

    private fun setBars(color: Int) {
        toolbar.setBackgroundColor(color)
        window.statusBarColor = color
        window.navigationBarColor = color
    }

    @Suppress("DEPRECATION")
    private fun setLightBars(light: Boolean) {
        var f = window.decorView.systemUiVisibility
        f = if (light) f or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else f and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        if (Build.VERSION.SDK_INT >= 26) {
            f = if (light) f or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            else f and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        }
        window.decorView.systemUiVisibility = f
    }

    private fun applyTheme(animate: Boolean) {
        val p = pal()
        val key = "" + p.bar + "|" + p.pill + "|" + p.text + "|" + p.accent + "|" + p.bg
        if (key == lastPalKey) return
        lastPalKey = key
        val from = if (barColor == 0) p.bar else barColor
        barColor = p.bar
        window.statusBarColor = p.bar
        window.navigationBarColor = p.bar
        if (animate && from != p.bar && d(1L) > 0L) {
            val va = ValueAnimator.ofArgb(from, p.bar)
            va.duration = d(260)
            va.addUpdateListener { toolbar.setBackgroundColor(it.animatedValue as Int) }
            va.start()
        } else {
            toolbar.setBackgroundColor(p.bar)
        }
        setLightBars(!p.dark)
        window.setBackgroundDrawable(ColorDrawable(p.bg))
        btnHome.setColorFilter(c("#4F8BFF"), PorterDuff.Mode.SRC_IN)
        btnMic.setColorFilter(c("#8B5CF6"), PorterDuff.Mode.SRC_IN)
        btnMenu.setColorFilter(c("#FF7A2F"), PorterDuff.Mode.SRC_IN)
        btnClear.setColorFilter(p.sub, PorterDuff.Mode.SRC_IN)
        pillBg.setColor(if (p.dark) p.bg else p.pill)
        urlPill.background = pillBg
        urlInput.setTextColor(p.text)
        urlInput.setHintTextColor(p.sub)
        progress.setColors(loadColors())
        suggestPanel.setBackgroundColor(p.bar)
        tabsOverlay.setBackgroundColor(p.bg)
        tabsTitle.setTextColor(p.text)
        val newBg = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(p.accent, mix(Color.WHITE, p.accent, 0.28f)))
        newBg.cornerRadius = dpf(25f)
        btnNewTab.background = newBg
        btnNewTab.setTextColor(Color.WHITE)
        btnNewInc.setColorFilter(c("#8B5CF6"), PorterDuff.Mode.SRC_IN)
        btnTabsMenu.setColorFilter(c("#FF7A2F"), PorterDuff.Mode.SRC_IN)
        btnTabsDone.setColorFilter(c("#16A56A"), PorterDuff.Mode.SRC_IN)
        ptrView.setColors(p.card, p.accent)
        tabsSearchPill.background = rounded(p.card, 22f, p.line, 1)
        tabSearch.setTextColor(p.text)
        tabSearch.setHintTextColor(p.sub)
        tabSearchIcon.setColorFilter(p.sub, PorterDuff.Mode.SRC_IN)
        tabSearchClear.setColorFilter(p.sub, PorterDuff.Mode.SRC_IN)
        btnCloseAll.background = rounded(p.card, 22f, p.line, 1)
        btnCloseAll.setColorFilter(c("#FF3B4E"), PorterDuff.Mode.SRC_IN)
        tabsEmpty.setTextColor(p.sub)
        toastView.background = rounded(if (p.dark) c("#F5F5FA") else c("#0D0D14"), 22f)
        toastView.setTextColor(if (p.dark) c("#0D0D14") else c("#F5F5FA"))
        for (t in tabs) t.peek()?.setBackgroundColor(p.bg)
        refreshTabsButton()
        refreshUrlStroke()
        updateUrlUi()
    }

    private fun refreshUrlStroke() {
        val pp = pal()
        if (urlInput.hasFocus() || lastLoading) pillBg.setStroke(dp(2), pp.accent) else pillBg.setStroke(dp(1), pp.line)
    }

    // =====================================================================
    // toolbar
    // =====================================================================
    @SuppressLint("ClickableViewAccessibility")
    private fun press(v: View) {
        v.setOnTouchListener { view, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN ->
                    view.animate().scaleX(0.82f).scaleY(0.82f).setDuration(d(90)).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    view.animate().scaleX(1f).scaleY(1f).setDuration(d(240))
                        .setInterpolator(OvershootInterpolator(2.5f)).start()
            }
            false
        }
    }

    private fun setupToolbar() {
        for (v in arrayOf<View>(btnHome, btnMic, btnTabs, btnMenu, btnClear)) press(v)

        btnHome.setOnClickListener { goHome() }
        btnMic.setOnClickListener { startVoice() }
        btnMenu.setOnClickListener { showMenu() }
        btnTabs.setOnClickListener { toggleTabs() }
        btnClear.setOnClickListener {
            urlInput.setText("")
            urlInput.requestFocus()
        }

        urlInput.setOnFocusChangeListener { _, focus -> onUrlFocus(focus) }
        urlInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!settingText) onUrlText(s?.toString() ?: "")
            }
        })
        urlInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                submitUrl()
                true
            } else {
                false
            }
        }
    }

    private fun setTextSilently(s: String) {
        settingText = true
        urlInput.setText(s)
        settingText = false
        btnClear.visibility = if (urlInput.hasFocus() && s.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateUrlUi() {
        val t = current ?: return
        val p = pal()
        if (!urlInput.hasFocus()) {
            setTextSilently(if (t.trusted || t.url.isEmpty()) "" else displayUrl(t.url))
        }
        if (lastLoading && !t.trusted) return
        val res: Int
        val tint: Int
        when {
            t.trusted || t.url.isEmpty() -> { res = R.drawable.ic_search; tint = p.sub }
            searchQueryOf(t.url) != null -> { res = R.drawable.ic_search; tint = p.accent }
            t.url.startsWith("https://") -> { res = R.drawable.ic_lock; tint = c("#16A56A") }
            else -> { res = R.drawable.ic_info; tint = c("#F59E0B") }
        }
        if (iconRes != res) {
            iconRes = res
            urlIcon.setImageResource(res)
            urlIcon.scaleX = 0.5f
            urlIcon.scaleY = 0.5f
            urlIcon.animate().scaleX(1f).scaleY(1f).setDuration(d(240))
                .setInterpolator(OvershootInterpolator(2f)).start()
        }
        urlIcon.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
    }

    private fun onUrlFocus(focus: Boolean) {
        refreshUrlStroke()
        urlPill.animate().scaleY(if (focus) 1.06f else 1f).setDuration(d(160)).start()
        if (focus) {
            val t = current
            setTextSilently(if (t == null || t.trusted) "" else t.url)
            urlInput.post { urlInput.selectAll() }
            animRows = true
            showSuggestPanel()
            buildSuggestions(urlInput.text.toString())
            animRows = false
            fetchRemote(urlInput.text.toString())
        } else {
            hideSuggestPanel()
            btnClear.visibility = View.GONE
            updateUrlUi()
        }
    }

    private fun onUrlText(q: String) {
        btnClear.visibility = if (urlInput.hasFocus() && q.isNotEmpty()) View.VISIBLE else View.GONE
        if (!urlInput.hasFocus()) return
        animRows = false
        buildSuggestions(q)
        fetchRemote(q)
    }

    private fun submitUrl() {
        val txt = urlInput.text.toString().trim()
        if (txt.isEmpty()) return
        dropFocus()
        navigateCurrent(txt)
    }

    private fun setLoading(b: Boolean) {
        if (b == lastLoading) return
        lastLoading = b
        spinAnim?.cancel()
        spinAnim = null
        urlIcon.rotation = 0f
        if (b) {
            iconRes = R.drawable.ic_refresh
            urlIcon.setImageResource(R.drawable.ic_refresh)
            urlIcon.setColorFilter(pal().accent, PorterDuff.Mode.SRC_IN)
            if (d(1L) > 0L) {
                val a = ObjectAnimator.ofFloat(urlIcon, View.ROTATION, 0f, 360f)
                a.duration = 900L
                a.interpolator = LinearInterpolator()
                a.repeatCount = ValueAnimator.INFINITE
                a.start()
                spinAnim = a
            }
        } else {
            iconRes = 0
        }
        refreshUrlStroke()
        if (!b) updateUrlUi()
    }

    // =====================================================================
    // loading state: a page is "loading" from its start until it has finished, failed, been stopped, been
    // taken over by a download or an app, or been given up on. Every one of those ends in finishLoad().
    // =====================================================================
    private fun beginLoad(t: Tab) {
        val fresh = !t.loading
        t.guard.begin(SystemClock.uptimeMillis())
        t.progress = 5
        ui.removeCallbacks(t.watch)
        ui.postDelayed(t.watch, 5000L)
        if (t === current) {
            if (fresh && !t.trusted) progress.reset()
            setLoading(!t.trusted)
            if (!t.trusted) setProgress(5)
        }
    }

    /** Safe to call any number of times: stops the spinner and the pull indicator, and completes the bar once. */
    private fun finishLoad(t: Tab) {
        val was = t.loading
        t.guard.end()
        t.progress = 100
        ui.removeCallbacks(t.watch)
        if (t === current) {
            setLoading(false)
            if (was && !t.trusted) updateProgress(100)
            if (ptrRefreshing) ptrStop()
        }
        // a moment after the page is done, take a fresh preview picture of it
        ui.removeCallbacks(t.thumbRun)
        ui.postDelayed(t.thumbRun, 1100L)
    }

    /** Runs every 5 s while a tab is loading: nothing moved for 30 s means it is stuck, 90 s in total means enough. */
    private fun loadWatchTick(t: Tab) {
        if (!t.loading || !tabs.contains(t)) return
        when (t.guard.verdict(SystemClock.uptimeMillis())) {
            1 -> {
                try {
                    t.peek()?.stopLoading()
                } catch (e: Exception) {
                }
                finishLoad(t)
                if (t === current) showToast("This page is taking too long to load")
            }
            2 -> finishLoad(t)
            else -> ui.postDelayed(t.watch, 5000L)
        }
    }

    private fun updateProgress(p: Int) {
        progress.update(p, d(1L) > 0L)
        if (p >= 100 && ptrRefreshing) ptrStop()
    }

    /** Loading bar colours: blue, cyan, white, purple (the last repeats the first so it loops without a seam). */
    private fun loadColors(): IntArray =
        intArrayOf(c("#2F80FF"), c("#22D3EE"), c("#FFFFFF"), c("#8B5CF6"), c("#2F80FF"))

    /** Back/Forward/Refresh now live in the three-dot menu (Chrome style) and read their state when it opens. */
    private fun updateNav() {}

    // =====================================================================
    // URL resolving + search
    // =====================================================================
    private fun looksLikeUrl(s: String): Boolean =
        !s.contains(' ') && (URL_RE.matches(s) || s.startsWith("about:"))

    private fun resolve(input: String): String {
        val s = input.trim()
        if (s.startsWith("http://") || s.startsWith("https://") || s.startsWith(ASSET)) return s
        if (looksLikeUrl(s)) {
            val local = s.startsWith("localhost") || Regex("^\\d{1,3}(\\.\\d{1,3}){3}.*").matches(s)
            return if (local) "http://$s" else "https://$s"
        }
        return searchUrl(s, false)
    }

    private fun engineName(): String {
        val k = stS("eng", "google")
        return when (k) {
            "custom" -> "custom engine"
            "ddg" -> "DuckDuckGo"
            else -> k.replaceFirstChar { it.uppercase() }
        }
    }

    private fun searchUrl(q: String, img: Boolean): String {
        val enc = URLEncoder.encode(q, "UTF-8")
        val key = stS("eng", "google")
        if (key == "custom" && !img) {
            val cu = stS("cust", "")
            if (cu.contains("%s")) return cu.replace("%s", enc)
        }
        val e = ENGINES[key] ?: ENGINES["google"]!!
        var u = (if (img) e.img else e.web) + enc
        if (key == "google" || !ENGINES.containsKey(key)) {
            if (stB("safe", false)) u += "&safe=active"
            val hl = stS("hl", "")
            if (hl.isNotEmpty()) u += "&hl=$hl"
        } else if (stB("safe", false)) {
            u += e.safe
        }
        return u
    }

    private fun scopeUrl(i: Int, q: String): String {
        val enc = URLEncoder.encode(q, "UTF-8")
        return when (i) {
            0 -> searchUrl(q, true)
            1 -> "https://www.youtube.com/results?search_query=$enc"
            2 -> "https://en.wikipedia.org/w/index.php?search=$enc"
            3 -> "https://github.com/search?q=$enc"
            4 -> "https://www.google.com/maps/search/$enc"
            else -> "https://www.amazon.com/s?k=$enc"
        }
    }

    private fun loadCurrent(url: String) {
        dropFocus()
        current?.let { loadWithMode(it, url) }
    }

    private fun navigateCurrent(input: String) {
        val s = input.trim()
        if (s.isEmpty()) return
        if (!looksLikeUrl(s) && !s.startsWith("http")) store.addRecent(s)
        loadCurrent(resolve(s))
    }

    private fun openInternal(page: String) {
        if (tabsOpen) hideTabs()
        loadCurrent("$HOME#$page")
    }

    // =====================================================================
    // suggestions panel
    // =====================================================================
    private fun showSuggestPanel() {
        suggestPanel.animate().cancel()
        if (suggestPanel.visibility != View.VISIBLE) {
            suggestPanel.scrollTo(0, 0)
            suggestPanel.alpha = 0f
            suggestPanel.translationY = -dpf(12f)
            suggestPanel.visibility = View.VISIBLE
        }
        suggestPanel.animate().alpha(1f).translationY(0f).setDuration(d(200))
            .setInterpolator(DecelerateInterpolator()).start()
    }

    private fun hideSuggestPanel() {
        suggestPanel.animate().cancel()
        if (suggestPanel.visibility == View.VISIBLE) {
            suggestPanel.animate().alpha(0f).translationY(-dpf(12f)).setDuration(d(150))
                .withEndAction { suggestPanel.visibility = View.GONE }.start()
        }
    }

    private fun animateIn(v: View, i: Int) {
        if (!animRows) return
        v.alpha = 0f
        v.translationY = dpf(10f)
        v.animate().alpha(1f).translationY(0f).setStartDelay(d(minOf(i, 8) * 24L)).setDuration(d(210))
            .setInterpolator(DecelerateInterpolator()).start()
    }

    private fun addRow(v: View) {
        suggestList.addView(v)
        animateIn(v, rowIdx)
        rowIdx++
    }

    private fun suggestRow(icon: Int, tint: Int, title: String, sub: String?, fill: String,
                           onRemove: ((View) -> Unit)? = null, onClick: () -> Unit): View {
        val p = pal()
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(16), 0, dp(4), 0)
        row.minimumHeight = dp(52)
        ripple(row)
        val ic = ImageView(this)
        ic.setImageResource(icon)
        ic.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
        row.addView(ic, LinearLayout.LayoutParams(dp(20), dp(20)))
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(14), dp(8), dp(8), dp(8))
        val tv = TextView(this)
        tv.text = title
        tv.setTextColor(p.text)
        tv.textSize = 15f
        tv.maxLines = 1
        tv.ellipsize = TextUtils.TruncateAt.END
        col.addView(tv)
        if (sub != null) {
            val s = TextView(this)
            s.text = sub
            s.setTextColor(p.sub)
            s.textSize = 12f
            s.maxLines = 1
            s.ellipsize = TextUtils.TruncateAt.END
            col.addView(s)
        }
        row.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (onRemove != null) {
            val xb = ImageButton(this)
            xb.setImageResource(R.drawable.ic_close)
            xb.setColorFilter(p.sub, PorterDuff.Mode.SRC_IN)
            xb.setBackgroundResource(0)
            xb.setPadding(dp(13), dp(13), dp(13), dp(13))
            xb.scaleType = ImageView.ScaleType.FIT_CENTER
            xb.contentDescription = "Remove from recent searches"
            xb.setOnClickListener { onRemove(row) }
            press(xb)
            row.addView(xb, LinearLayout.LayoutParams(dp(44), dp(44)))
        }
        val fb = ImageButton(this)
        fb.setImageResource(R.drawable.ic_fill)
        fb.setColorFilter(p.sub, PorterDuff.Mode.SRC_IN)
        fb.setBackgroundResource(0)
        fb.setPadding(dp(11), dp(11), dp(11), dp(11))
        fb.scaleType = ImageView.ScaleType.FIT_CENTER
        fb.contentDescription = "Use this suggestion"
        fb.setOnClickListener {
            setTextSilently(fill)
            urlInput.setSelection(urlInput.text.length)
            onUrlText(fill)
        }
        press(fb)
        row.addView(fb, LinearLayout.LayoutParams(dp(44), dp(44)))
        row.setOnClickListener { onClick() }
        return row
    }

    private fun recentRow(q: String): View {
        val row = suggestRow(R.drawable.ic_history, pal().sub, q, null, q, { v -> removeRecentRow(v, q) }) { navigateCurrent(q) }
        row.tag = "recent:" + q
        return row
    }

    /** Deletes one recent search for good, slides the row away and pulls the next older search up. */
    private fun removeRecentRow(v: View, q: String) {
        store.removeRecent(q)
        v.isEnabled = false
        val done = Runnable {
            suggestList.removeView(v)
            if (urlInput.text.toString().trim().isEmpty() && stB("recent", true)) {
                val have = HashSet<String>()
                var last = -1
                for (i in 0 until suggestList.childCount) {
                    val tg = suggestList.getChildAt(i).tag
                    if (tg is String && tg.startsWith("recent:")) {
                        have.add(tg.substring(7))
                        last = i
                    }
                }
                var next: String? = null
                for (r in store.recents()) {
                    if (!have.contains(r)) {
                        next = r
                        break
                    }
                }
                if (next != null && have.size < 6) {
                    val nv = recentRow(next)
                    suggestList.addView(nv, if (last >= 0) last + 1 else suggestList.childCount)
                    animateIn(nv, 0)
                }
            }
            if (suggestList.childCount == 0) {
                val hint = TextView(this)
                hint.text = "Type a search or web address"
                hint.setTextColor(pal().sub)
                hint.textSize = 14f
                hint.setPadding(dp(20), dp(20), dp(20), dp(20))
                suggestList.addView(hint)
            }
        }
        if (d(1L) == 0L) {
            done.run()
            return
        }
        v.animate().alpha(0f).translationX(dpf(48f)).setDuration(d(180)).withEndAction(done).start()
    }

    private fun chipsRow(q: String, p: Pal): View {
        val hs = HorizontalScrollView(this)
        hs.isHorizontalScrollBarEnabled = false
        val ll = LinearLayout(this)
        ll.orientation = LinearLayout.HORIZONTAL
        ll.gravity = Gravity.CENTER_VERTICAL
        ll.setPadding(dp(14), dp(10), dp(8), dp(14))
        val label = TextView(this)
        label.text = "Search in"
        label.textSize = 12f
        label.setTextColor(p.sub)
        label.setPadding(0, 0, dp(10), 0)
        ll.addView(label)
        for (i in SCOPES.indices) {
            val chip = TextView(this)
            chip.text = SCOPES[i]
            chip.textSize = 13f
            chip.setTextColor(p.text)
            chip.gravity = Gravity.CENTER
            chip.setPadding(dp(14), dp(8), dp(14), dp(8))
            chip.background = rounded(p.pill, 18f)
            chip.setOnClickListener {
                store.addRecent(q)
                loadCurrent(scopeUrl(i, q))
            }
            press(chip)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.rightMargin = dp(8)
            ll.addView(chip, lp)
        }
        hs.addView(ll)
        return hs
    }

    private fun buildSuggestions(q: String) {
        val p = pal()
        suggestList.removeAllViews()
        remoteHolder = null
        rowIdx = 0
        val text = q.trim()
        if (text.isEmpty()) {
            if (stB("recent", true)) {
                for (r in store.recents().take(6)) {
                    addRow(recentRow(r))
                }
            }
            if (suggestList.childCount == 0) {
                val hint = TextView(this)
                hint.text = "Type a search or web address"
                hint.setTextColor(p.sub)
                hint.textSize = 14f
                hint.setPadding(dp(20), dp(20), dp(20), dp(20))
                addRow(hint)
            }
            return
        }
        val isUrl = looksLikeUrl(text)
        addRow(
            suggestRow(
                if (isUrl) R.drawable.ic_forward else R.drawable.ic_search, p.accent, text,
                if (isUrl) "Open web address" else "Search with " + engineName(), text
            ) { navigateCurrent(text) }
        )
        if (!isUrl && stB("recent", true)) {
            var n = 0
            val low0 = text.lowercase()
            for (r in store.recents()) {
                if (n >= 3) break
                if (r.equals(text, ignoreCase = true)) continue
                if (r.lowercase().contains(low0)) {
                    addRow(recentRow(r))
                    n++
                }
            }
        }
        val cands = ArrayList<Triple<String, String, Boolean>>()
        val b = store.bookmarks()
        for (i in 0 until b.length()) {
            b.optJSONObject(i)?.let { cands.add(Triple(it.optString("t"), it.optString("u"), true)) }
        }
        val h = store.history()
        for (i in 0 until h.length()) {
            h.optJSONObject(i)?.let { cands.add(Triple(it.optString("q"), it.optString("u"), false)) }
        }
        val low = text.lowercase()
        val shown = HashSet<String>()
        for (cnd in cands) {
            if (shown.size >= 4) break
            if (cnd.second.isEmpty() || shown.contains(cnd.second)) continue
            if (cnd.first.lowercase().contains(low) || cnd.second.lowercase().contains(low)) {
                shown.add(cnd.second)
                val url = cnd.second
                addRow(
                    suggestRow(
                        if (cnd.third) R.drawable.ic_star else R.drawable.ic_history,
                        if (cnd.third) STAR else p.sub,
                        cnd.first.ifBlank { url }, displayUrl(url), url
                    ) { loadCurrent(url) }
                )
            }
        }
        val holder = LinearLayout(this)
        holder.orientation = LinearLayout.VERTICAL
        suggestList.addView(holder)
        remoteHolder = holder
        if (!isUrl) addRow(chipsRow(text, p))
    }

    private fun fillRemote(list: List<String>) {
        val h = remoteHolder ?: return
        val p = pal()
        val first = h.childCount == 0
        val typed = urlInput.text.toString().trim()
        h.removeAllViews()
        var i = 0
        for (s in list) {
            if (s.equals(typed, ignoreCase = true)) continue
            val v = suggestRow(R.drawable.ic_search, p.sub, s, null, s) { navigateCurrent(s) }
            h.addView(v)
            if (first) {
                v.alpha = 0f
                v.translationY = dpf(10f)
                v.animate().alpha(1f).translationY(0f).setStartDelay(d(i * 24L)).setDuration(d(210))
                    .setInterpolator(DecelerateInterpolator()).start()
            }
            i++
        }
    }

    private fun fetchRemote(q: String) {
        sugRunnable?.let { ui.removeCallbacks(it) }
        val text = q.trim()
        if (!stB("sugg", true) || text.length < 2) return
        val token = ++sugToken
        val r = Runnable {
            thread {
                val res: List<String> = try {
                    googleSuggest(text)
                } catch (e: Exception) {
                    emptyList()
                }
                runOnUiThread {
                    if (token == sugToken && urlInput.hasFocus() && res.isNotEmpty()) fillRemote(res)
                }
            }
        }
        sugRunnable = r
        ui.postDelayed(r, 220L)
    }

    private fun googleSuggest(q: String): List<String> {
        val hl = stS("hl", "")
        val url = "https://suggestqueries.google.com/complete/search?client=firefox&q=" +
            URLEncoder.encode(q, "UTF-8") + (if (hl.isNotEmpty()) "&hl=$hl" else "")
        val con = URL(url).openConnection() as HttpURLConnection
        try {
            con.connectTimeout = 3000
            con.readTimeout = 3000
            con.setRequestProperty("User-Agent", uaMobile)
            val txt = con.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(txt).getJSONArray(1)
            val out = ArrayList<String>()
            for (i in 0 until arr.length()) {
                if (out.size >= 6) break
                out.add(arr.getString(i))
            }
            return out
        } finally {
            con.disconnect()
        }
    }

    // =====================================================================
    // tabs
    // =====================================================================
    @Suppress("DEPRECATION")
    @SuppressLint("SetJavaScriptEnabled")
    private fun configure(t: Tab) {
        val w = t.web
        val s = w.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.allowFileAccess = false
        s.allowContentAccess = false
        s.mediaPlaybackRequiresUserGesture = true
        s.setSupportMultipleWindows(true)
        s.javaScriptCanOpenWindowsAutomatically = true
        s.setSupportZoom(true)
        s.builtInZoomControls = true
        s.displayZoomControls = false
        s.useWideViewPort = true
        s.loadWithOverviewMode = true
        s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        s.textZoom = if (t.trusted) 100 else zoomPct()
        if (t.incognito) {
            s.cacheMode = WebSettings.LOAD_NO_CACHE
            s.saveFormData = false
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(w, !t.incognito)
        w.setBackgroundColor(pal().bg)
        w.addJavascriptInterface(Bridge(t), "NomanNative")
        w.webViewClient = Client(t)
        w.webChromeClient = Chrome(t)
        w.addJavascriptInterface(blobBridge, "NomanBlob")
        w.setDownloadListener { url, ua, cd, mime, _ ->
            // the navigation turned into a download, so the page will never report "finished"
            if (t.loading) finishLoad(t)
            startDownload(t, url, ua, cd, mime)
        }
        w.addJavascriptInterface(pullBridge, "NomanPull")
        w.setOnTouchListener { v, ev ->
            if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
                touchX = ev.x
                touchY = ev.y
            }
            ptrTouch(t, v, ev)
        }
        w.setOnLongClickListener { onWebLongPress(t) }
        applyMode(t)
    }

    private fun applyMode(t: Tab) {
        val s = t.web.settings
        s.userAgentString = if (t.desktop) uaDesktop else uaMobile
        s.useWideViewPort = true
        s.loadWithOverviewMode = true
    }

    private fun attachWeb(t: Tab) {
        configure(t)
        val w = t.web
        w.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        w.visibility = View.GONE
        content.addView(w, 0)
    }

    private fun newTab(url: String?, incognito: Boolean = false, select: Boolean = true,
                       parent: Tab? = null, load: Boolean = true): Tab {
        val t = Tab(incognito)
        t.parent = parent
        tabs.add(t)
        updateTabCount()
        if (load) {
            t.trusted = url == null || url.startsWith(ASSET)
            loadWithMode(t, url ?: HOME)
        }
        if (select) switchTo(t, true)
        scheduleSessionSave()
        return t
    }

    private fun switchTo(t: Tab, animate: Boolean) {
        val old = current
        ptrCancel()
        if (old != null && old !== t && tabs.contains(old)) {
            if (!tabsOpen) captureThumb(old)
            old.web.onPause()
            old.web.visibility = View.GONE
        }
        current = t
        dropFocus()
        ensureLoaded(t)
        if (tabsOpen && !keepTabsOpen) hideTabs()
        t.web.visibility = View.VISIBLE
        t.web.onResume()
        t.web.alpha = 1f
        val wantSpin = t.loading && !t.trusted
        lastLoading = !wantSpin
        setLoading(wantSpin)
        progress.reset()
        if (t.loading && !t.trusted) setProgress(t.progress)
        applyTheme(true)
        updateUrlUi()
        updateNav()
        if (t.trusted) t.web.evaluateJavascript("window.nomanRefresh&&nomanRefresh()", null)
        scheduleSessionSave()
    }

    private fun closeTab(t: Tab, next: Tab? = null) {
        val idx = tabs.indexOf(t)
        if (idx < 0) return
        val wasCurrent = t === current
        tabs.removeAt(idx)
        forgetThumb(t)
        ui.removeCallbacks(t.watch)
        updateTabCount()
        val w = t.peek()
        if (w != null) {
            w.stopLoading()
            if (t.incognito) {
                w.clearHistory()
                w.clearCache(true)
                w.clearFormData()
            }
            (w.parent as? ViewGroup)?.removeView(w)
            w.destroy()
        }
        if (tabs.isEmpty()) {
            current = null
            newTab(null)
            return
        }
        if (wasCurrent) switchTo(next ?: tabs[minOf(idx, tabs.size - 1)], true)
        scheduleSessionSave()
    }

    private fun navBack(t: Tab): Boolean {
        if (t.web.canGoBack()) {
            t.web.goBack()
            return true
        }
        val par = t.parent
        if (par != null && tabs.contains(par)) {
            closeTab(t, par)
            return true
        }
        return false
    }

    private fun setupTabsOverlay() {
        press(btnNewTab)
        press(btnNewInc)
        press(btnTabsMenu)
        press(btnTabsDone)
        press(btnCloseAll)
        btnNewTab.setOnClickListener { hideTabs(); newTab(null) }
        btnNewInc.setOnClickListener { hideTabs(); newTab(null, true) }
        btnTabsMenu.setOnClickListener { showMenu() }
        btnTabsDone.setOnClickListener { hideTabs() }
        btnCloseAll.setOnClickListener { closeAllDialog() }
        tabSearchClear.setOnClickListener { tabSearch.setText("") }
        tabSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                tabQuery = s?.toString() ?: ""
                tabSearchClear.visibility = if (tabQuery.isEmpty()) View.GONE else View.VISIBLE
                applyTabFilter(false)
            }
        })
        tabSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else {
                false
            }
        }
        tabsListView = ListView(this)
        tabsListView.divider = null
        tabsListView.dividerHeight = 0
        tabsListView.selector = ColorDrawable(Color.TRANSPARENT)
        tabsListView.cacheColorHint = Color.TRANSPARENT
        tabsListView.overScrollMode = View.OVER_SCROLL_NEVER
        tabsListView.isVerticalScrollBarEnabled = false
        tabsListView.clipToPadding = false
        tabsListView.setPadding(0, 0, 0, dp(8))
        tabAdapter = TabAdapter()
        tabsListView.adapter = tabAdapter
        tabsBody.addView(tabsListView, 0, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun updateTabsTitle() {
        val n = tabs.size
        tabsTitle.text = if (n == 1) "1 tab" else "$n tabs"
    }

    private fun toggleTabs() {
        if (tabsOpen) hideTabs() else showTabs()
    }

    private fun refreshTabsButton() {
        val p = pal()
        val violet = c("#8B5CF6")
        tabBg.setColor(if (tabsOpen) violet else Color.TRANSPARENT)
        tabBg.setStroke(dp(2), violet)
        tabCount.background = tabBg
        tabCount.setTextColor(if (tabsOpen) Color.WHITE else violet)
    }

    private fun updateTabCount() {
        val n = tabs.size
        tabCount.text = if (n > 99) ":D" else n.toString()
        if (n != lastCount) {
            lastCount = n
            tabCount.animate().cancel()
            tabCount.scaleX = 0.6f
            tabCount.scaleY = 0.6f
            tabCount.animate().scaleX(1f).scaleY(1f).setDuration(d(260))
                .setInterpolator(OvershootInterpolator(2.4f)).start()
        }
    }

    // =====================================================================
    // tab previews: a real screenshot of each tab, like Chrome
    // =====================================================================
    private fun thumbFile(id: String): File {
        val d = File(cacheDir, "thumbs")
        if (!d.exists()) d.mkdirs()
        return File(d, "$id.jpg")
    }

    /** Nothing was drawn when every sampled pixel is still fully transparent. */
    private fun thumbBlank(b: Bitmap): Boolean {
        val xs = intArrayOf(b.width / 4, b.width / 2, b.width * 3 / 4)
        val ys = intArrayOf(b.height / 5, b.height / 2, b.height * 4 / 5)
        for (x in xs) {
            for (y in ys) {
                if (Color.alpha(b.getPixel(x, y)) != 0) return false
            }
        }
        return true
    }

    /** Draws the page of [t] (it has to be the tab on screen) into a small bitmap and remembers it. */
    private fun captureThumb(t: Tab) {
        val w = t.peek() ?: return
        if (t !== current || w.visibility != View.VISIBLE || w.width < 80 || w.height < 80) return
        try {
            val tw = 320
            val sc = tw.toFloat() / w.width
            val th = Math.min((w.height * sc).toInt(), (tw * 1.6f).toInt())
            val bmp = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.scale(sc, sc)
            w.draw(c)
            if (thumbBlank(bmp)) return
            thumbCache.put(t.id, bmp)
            if (!t.incognito) {
                thumbPool.execute {
                    try {
                        FileOutputStream(thumbFile(t.id)).use { bmp.compress(Bitmap.CompressFormat.JPEG, 78, it) }
                    } catch (e: Exception) {
                    }
                }
            }
        } catch (e: Throwable) {
        }
    }

    private fun iconFile(id: String): File {
        val d = File(cacheDir, "icons")
        if (!d.exists()) d.mkdirs()
        return File(d, "$id.png")
    }

    private fun dropIcon(t: Tab) {
        t.favicon = null
        t.iconTried = false
        iconCache.remove(t.id)
        val name = t.id
        thumbPool.execute {
            try {
                iconFile(name).delete()
            } catch (e: Exception) {
            }
        }
    }

    private fun forgetThumb(t: Tab) {
        thumbCache.remove(t.id)
        iconCache.remove(t.id)
        ui.removeCallbacks(t.thumbRun)
        val name = t.id
        thumbPool.execute {
            try {
                iconFile(name).delete()
                thumbFile(name).delete()
            } catch (e: Exception) {
            }
        }
    }

    /** Pictures of tabs that no longer exist are deleted. */
    private fun cleanThumbs() {
        val keep = HashSet<String>()
        for (t in tabs) keep.add(t.id)
        thumbPool.execute {
            try {
                val files = File(cacheDir, "thumbs").listFiles()
                if (files != null) {
                    for (f in files) {
                        if (!keep.contains(f.name.removeSuffix(".jpg"))) f.delete()
                    }
                }
                val icons = File(cacheDir, "icons").listFiles()
                if (icons != null) {
                    for (f in icons) {
                        if (!keep.contains(f.name.removeSuffix(".png"))) f.delete()
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    /** Shows the top of the page, as wide as the preview area (the way Chrome crops it). */
    private fun fitTop(iv: ImageView, b: Bitmap) {
        val vw = iv.width.toFloat()
        val vh = iv.height.toFloat()
        if (vw <= 0f || vh <= 0f || b.width <= 0 || b.height <= 0) return
        val s = Math.max(vw / b.width, vh / b.height)
        val m = Matrix()
        m.setScale(s, s)
        m.postTranslate((vw - b.width * s) / 2f, 0f)
        iv.imageMatrix = m
    }

    private fun showThumb(iv: ImageView, bars: View, b: Bitmap) {
        iv.setImageBitmap(b)
        fitTop(iv, b)
        bars.visibility = View.GONE
        if (iv.visibility != View.VISIBLE) {
            iv.visibility = View.VISIBLE
            if (d(1L) > 0L) {
                iv.alpha = 0f
                iv.animate().alpha(1f).setDuration(d(160)).start()
            }
        }
    }

    private fun loadThumb(tab: Tab, iv: ImageView, bars: View) {
        val cached = thumbCache.get(tab.id)
        if (cached != null) {
            showThumb(iv, bars, cached)
            return
        }
        if (tab.incognito) return
        val name = tab.id
        thumbPool.execute {
            val f = thumbFile(name)
            if (f.exists()) {
                val b = try {
                    BitmapFactory.decodeFile(f.absolutePath)
                } catch (e: Throwable) {
                    null
                }
                if (b != null) {
                    runOnUiThread {
                        thumbCache.put(name, b)
                        if (iv.isAttachedToWindow) showThumb(iv, bars, b)
                    }
                }
            }
        }
    }

    // =====================================================================
    // translate the page
    // =====================================================================
    private fun assetText(name: String): String = try {
        assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
    } catch (e: Exception) {
        ""
    }

    private fun trConfig(): TrConfig =
        TrConfig(stS("trp", "ms"), store.secretGet("tr.key") ?: "", stS("trr", ""), stS("tre", ""))

    private fun langRow(l: Lang, p: Pal, onClick: () -> Unit): View {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.setPadding(dp(14), 0, dp(14), 0)
        ripple(r)
        val a = TextView(this)
        a.text = l.native
        a.textSize = 15f
        a.typeface = Typeface.DEFAULT_BOLD
        a.setTextColor(p.text)
        r.addView(a, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val b = TextView(this)
        b.text = l.english
        b.textSize = 12f
        b.setTextColor(p.sub)
        r.addView(b)
        r.setOnClickListener { onClick() }
        return r
    }

    private fun showTranslateSheet(t: Tab) {
        val p = pal()
        val cfg = trConfig()
        val dlg = Dialog(this)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.background = rounded(p.card, 22f, p.line, 1)
        box.setPadding(dp(6), dp(14), dp(6), dp(8))

        val title = TextView(this)
        title.text = "Translate this page"
        title.textSize = 17f
        title.typeface = Typeface.DEFAULT_BOLD
        title.setTextColor(p.text)
        title.setPadding(dp(14), 0, dp(14), 0)
        box.addView(title)
        val sub = TextView(this)
        sub.textSize = 12f
        sub.setTextColor(p.sub)
        sub.setPadding(dp(14), dp(2), dp(14), dp(8))
        sub.text = if (cfg.ready()) "Using " + cfg.providerName() + ". Checking the page language..." else "One step is missing"
        box.addView(sub)

        if (!cfg.ready()) {
            val note = TextView(this)
            note.text = "Translation needs a key from Microsoft or Google (both give a free amount every month) or a LibreTranslate server. Add it once in Settings > Translate."
            note.textSize = 14f
            note.setTextColor(p.text)
            note.setPadding(dp(14), dp(4), dp(14), dp(14))
            box.addView(note)
            val go = TextView(this)
            go.text = "Set up translation"
            go.gravity = Gravity.CENTER
            go.textSize = 15f
            go.typeface = Typeface.DEFAULT_BOLD
            go.setTextColor(Color.WHITE)
            go.background = rounded(p.accent, 25f)
            go.setOnClickListener {
                dlg.dismiss()
                openInternal("translate")
            }
            val glp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50))
            glp.setMargins(dp(14), 0, dp(14), dp(6))
            box.addView(go, glp)
        } else {
            val sv = ScrollView(this)
            val list = LinearLayout(this)
            list.orientation = LinearLayout.VERTICAL
            val def = stS("trl", "bn")
            val order = ArrayList<Lang>()
            for (l in Langs.all) if (l.code == def) order.add(l)
            for (l in Langs.all) if (l.code != def) order.add(l)
            for (l in order) {
                list.addView(
                    langRow(l, p) {
                        dlg.dismiss()
                        startTranslate(t, l.code)
                    },
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52))
                )
            }
            sv.addView(list)
            val maxH = Math.min(dp(340), (resources.displayMetrics.heightPixels * 0.5f).toInt())
            box.addView(sv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxH))
        }

        val wrap = FrameLayout(this)
        wrap.setPadding(dp(10), 0, dp(10), dp(10))
        wrap.addView(box, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        dlg.setContentView(wrap)
        val win = dlg.window
        if (win != null) {
            win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            win.setGravity(Gravity.BOTTOM)
            win.setDimAmount(0.5f)
        }
        dlg.setCanceledOnTouchOutside(true)
        dlg.show()
        box.translationY = dpf(40f)
        box.alpha = 0f
        box.animate().translationY(0f).alpha(1f).setDuration(d(220)).setInterpolator(DecelerateInterpolator()).start()

        // which language is the page in?
        t.web.evaluateJavascript(trJs, null)
        t.web.evaluateJavascript("__ngTr.info()") { raw ->
            var lang = ""
            var sample = ""
            try {
                val o = JSONObject(unquoteJs(raw))
                lang = o.optString("lang")
                sample = o.optString("sample")
            } catch (e: Exception) {
            }
            val prefix = if (cfg.ready()) "Using " + cfg.providerName() + ". " else ""
            if (sample.length >= 40 && cfg.ready()) {
                trPool.execute {
                    val det = Translator(cfg).detect(sample)
                    val shownLang = if (det.isNotEmpty()) det else lang
                    runOnUiThread {
                        sub.text = prefix + (if (shownLang.isNotEmpty()) "Page language: " + Langs.name(shownLang) else "Page language: unknown")
                    }
                }
            } else if (lang.isNotEmpty()) {
                sub.text = prefix + "Page language: " + Langs.name(lang)
            } else if (cfg.ready()) {
                sub.text = prefix + "Page language: unknown"
            }
        }
    }

    private fun startTranslate(t: Tab, target: String) {
        val cfg = trConfig()
        if (!cfg.ready()) {
            showToast("Add a translation key in Settings > Translate")
            openInternal("translate")
            return
        }
        t.trGen++
        val gen = t.trGen
        t.trTarget = target
        t.trBusy = true
        t.trChars = 0
        t.trDone = 0
        progress.update(8, d(1L) > 0L)
        t.web.evaluateJavascript(trJs, null)
        t.web.evaluateJavascript("__ngTr.info()") { raw ->
            if (t.trGen != gen) return@evaluateJavascript
            var lang = ""
            var sample = ""
            try {
                val o = JSONObject(unquoteJs(raw))
                lang = o.optString("lang")
                sample = o.optString("sample")
            } catch (e: Exception) {
            }
            trPool.execute {
                val det = if (sample.length >= 40) Translator(cfg).detect(sample) else lang
                runOnUiThread {
                    if (t.trGen != gen) return@runOnUiThread
                    if (Langs.same(det, target)) {
                        t.trTarget = ""
                        t.trBusy = false
                        progress.reset()
                        showToast("This page is already in " + Langs.name(target))
                    } else {
                        trNext(t, gen, cfg, true)
                    }
                }
            }
        }
    }

    /** Takes the next group of texts from the page, translates it and puts it back. Repeats until nothing is left. */
    private fun trNext(t: Tab, gen: Int, cfg: TrConfig, first: Boolean) {
        if (t.trGen != gen || !tabs.contains(t)) return
        t.trBusy = true
        t.web.evaluateJavascript("__ngTr.take(80,4200)") { raw ->
            if (t.trGen != gen) return@evaluateJavascript
            var items: JSONArray? = null
            var all = 0
            var done = 0
            try {
                val o = JSONObject(unquoteJs(raw))
                items = o.getJSONArray("items")
                all = o.optInt("all")
                done = o.optInt("done")
            } catch (e: Exception) {
            }
            val list = items
            if (list == null || list.length() == 0) {
                trFinish(t, gen, first)
                return@evaluateJavascript
            }
            val texts = ArrayList<String>()
            val ids = ArrayList<JSONArray>()
            var chars = 0
            for (i in 0 until list.length()) {
                val g = list.getJSONObject(i)
                val s = g.getString("t")
                texts.add(s)
                ids.add(g.getJSONArray("ids"))
                chars += s.length
            }
            if (t.trChars + chars > TR_CAP) {
                t.web.evaluateJavascript("__ngTr.requeue()", null)
                trFinish(t, gen, first)
                showToast("This page is very long. The first part is translated.")
                return@evaluateJavascript
            }
            t.trChars += chars
            progress.update(8 + (85 * done / Math.max(all, 1)), d(1L) > 0L)
            val target = t.trTarget
            trPool.execute {
                val res = Translator(cfg).translate(texts, target)
                runOnUiThread {
                    if (t.trGen != gen) return@runOnUiThread
                    if (!res.ok) {
                        trFail(t, res.error)
                        return@runOnUiThread
                    }
                    val out = JSONArray()
                    for (i in texts.indices) out.put(JSONObject().put("ids", ids[i]).put("t", res.texts[i]))
                    val call = "__ngTr.put(" + JSONObject.quote(out.toString()) + "," + JSONObject.quote(target) + ")"
                    t.web.evaluateJavascript(call) { _ ->
                        if (t.trGen == gen) {
                            t.trDone += texts.size
                            trNext(t, gen, cfg, first)
                        }
                    }
                }
            }
        }
    }

    private fun trFinish(t: Tab, gen: Int, first: Boolean) {
        if (t.trGen != gen) return
        t.trBusy = false
        progress.update(100, d(1L) > 0L)
        if (first && t.trDone == 0) {
            t.trTarget = ""
            t.web.evaluateJavascript("window.__ngTr&&__ngTr.restore()", null)
            showToast("There is no text to translate on this page")
            return
        }
        if (first) showToast("Translated to " + Langs.name(t.trTarget))
        ui.postDelayed({ trPollTick(t, gen) }, 2000L)
    }

    private fun trFail(t: Tab, msg: String) {
        t.trBusy = false
        progress.reset()
        t.trGen++
        if (t.trDone == 0) {
            t.web.evaluateJavascript("window.__ngTr&&__ngTr.restore()", null)
            t.trTarget = ""
        }
        showToast(msg)
    }

    /** Text that shows up later (scrolling, single page apps) is translated too, as long as the page stays translated. */
    private fun trPollTick(t: Tab, gen: Int) {
        if (t.trGen != gen || t.trTarget.isEmpty() || !tabs.contains(t)) return
        if (t !== current || t.trBusy) {
            ui.postDelayed({ trPollTick(t, gen) }, 2000L)
            return
        }
        t.web.evaluateJavascript("window.__ngTr?__ngTr.pending():0") { raw ->
            if (t.trGen == gen) {
                val n = (raw ?: "0").trim().toIntOrNull() ?: 0
                if (n > 0) trNext(t, gen, trConfig(), false) else ui.postDelayed({ trPollTick(t, gen) }, 2000L)
            }
        }
    }

    private fun restoreTranslation(t: Tab) {
        t.trGen++
        t.trTarget = ""
        t.trBusy = false
        t.web.evaluateJavascript("window.__ngTr&&__ngTr.restore()", null)
        progress.reset()
        showToast("Original page restored")
    }

    // =====================================================================
    // pull to refresh
    // =====================================================================
    /** Called by pull.js at the start of every touch: may this drag pull to refresh? */
    private inner class PullBridge {
        @JavascriptInterface
        fun arm(ok: Boolean) {
            ptrOk = ok
        }
    }

    private fun setupPull() {
        touchSlopPx = ViewConfiguration.get(this).scaledTouchSlop
        ptrView = PullIndicator(this)
        content.addView(ptrView, FrameLayout.LayoutParams(dp(42), dp(42), Gravity.TOP or Gravity.CENTER_HORIZONTAL))
    }

    /** Runs for every touch on a web page. Returns true only while the pull gesture owns the touch. */
    private fun ptrTouch(t: Tab, v: View, ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                ptrOk = false
                ptrPulling = false
                ptrPast = false
                ptrStartX = ev.x
                ptrStartY = ev.y
                ptrArmed = stB("ptr", true) && !t.trusted && t === current && !tabsOpen && !ptrRefreshing &&
                    customView == null && v.scrollY == 0
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!ptrArmed) return false
                if (ev.pointerCount > 1) {
                    ptrArmed = false
                    if (ptrPulling) ptrCancel()
                    return false
                }
                val dy = ev.y - ptrStartY
                val dx = ev.x - ptrStartX
                if (!ptrPulling) {
                    if (v.scrollY != 0 || dy < -touchSlopPx) {
                        ptrArmed = false
                        return false
                    }
                    if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > touchSlopPx) {
                        ptrArmed = false
                        return false
                    }
                    if (dy > touchSlopPx && ptrOk) {
                        ptrPulling = true
                        // the page must not scroll or glow while we pull
                        val c = MotionEvent.obtain(ev)
                        c.action = MotionEvent.ACTION_CANCEL
                        v.onTouchEvent(c)
                        c.recycle()
                    } else {
                        return false
                    }
                }
                ptrDrag(dy - touchSlopPx)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val was = ptrPulling
                ptrArmed = false
                ptrPulling = false
                if (was) {
                    ptrRelease(ev.actionMasked == MotionEvent.ACTION_UP)
                    return true
                }
                return false
            }
        }
        return false
    }

    private fun ptrTrigger(): Float = dpf(64f)

    private fun ptrDrag(raw: Float) {
        val dist = Math.min(Math.max(raw, 0f) * 0.55f, dpf(120f))
        ptrDist = dist
        if (ptrView.visibility != View.VISIBLE) {
            ptrView.animate().cancel()
            ptrView.alpha = 1f
            ptrView.scaleX = 1f
            ptrView.scaleY = 1f
            ptrView.visibility = View.VISIBLE
        }
        ptrView.translationY = dist - dpf(42f)
        ptrView.setPull(dist / ptrTrigger())
        val past = dist >= ptrTrigger()
        if (past && !ptrPast) {
            ptrPast = true
            ptrView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } else if (!past) {
            ptrPast = false
        }
    }

    private fun ptrRelease(up: Boolean) {
        val t = current
        if (up && ptrDist >= ptrTrigger() && t != null && !t.trusted) {
            ptrRefreshing = true
            ptrView.startSpin()
            ptrView.animate().translationY(dpf(22f)).setDuration(d(150)).start()
            t.web.reload()
            ui.removeCallbacks(ptrStopRun)
            ui.postDelayed(ptrStopRun, 20000L)
        } else {
            ptrHide()
        }
        ptrDist = 0f
    }

    private fun ptrHide() {
        ptrView.animate().cancel()
        ptrView.animate().alpha(0f).scaleX(0.5f).scaleY(0.5f).translationY(-dpf(42f)).setDuration(d(180)).withEndAction {
            ptrView.visibility = View.GONE
            ptrView.stopSpin()
            ptrView.alpha = 1f
            ptrView.scaleX = 1f
            ptrView.scaleY = 1f
        }.start()
    }

    private fun ptrStop() {
        ui.removeCallbacks(ptrStopRun)
        if (!ptrRefreshing) return
        ptrRefreshing = false
        ptrHide()
    }

    /** A touch was interrupted (tab switch, second finger): put the indicator away without reloading. */
    private fun ptrCancel() {
        ptrArmed = false
        ptrPulling = false
        ptrDist = 0f
        if (ptrView.visibility == View.VISIBLE && !ptrRefreshing) ptrHide()
    }

    // =====================================================================
    // tab manager: a vertical list, one card per tab (the list recycles its rows, so 200 tabs cost the same as 6)
    // =====================================================================
    private fun mix(a: Int, b: Int, t: Float): Int {
        val r = (Color.red(a) * t + Color.red(b) * (1f - t)).toInt()
        val g = (Color.green(a) * t + Color.green(b) * (1f - t)).toInt()
        val bl = (Color.blue(a) * t + Color.blue(b) * (1f - t)).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), bl.coerceIn(0, 255))
    }

    private inner class TabHolder(val root: FrameLayout) {
        val card = LinearLayout(this@MainActivity)
        val iconBox = FrameLayout(this@MainActivity)
        val icon = ImageView(this@MainActivity)
        val letter = TextView(this@MainActivity)
        val title = TextView(this@MainActivity)
        val sub = TextView(this@MainActivity)
        val pin = ImageButton(this@MainActivity)
        val close = ImageButton(this@MainActivity)
        val preview = FrameLayout(this@MainActivity)
        val bars = LinearLayout(this@MainActivity)
        val shot = ImageView(this@MainActivity)
        val shine = View(this@MainActivity)
        val bgCard = GradientDrawable()
        val bgPrev = GradientDrawable()
        val bgLetter = GradientDrawable()
        val barBgs = ArrayList<GradientDrawable>()
        var tabId = ""
    }

    private inner class TabAdapter : BaseAdapter() {
        override fun getCount(): Int = shown.size

        override fun getItem(position: Int): Any = shown[position]

        override fun getItemId(position: Int): Long = shown[position].id.hashCode().toLong()

        override fun hasStableIds(): Boolean = true

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val h: TabHolder
            if (convertView == null) {
                h = makeHolder()
                h.root.tag = h
            } else {
                h = convertView.tag as TabHolder
            }
            bindHolder(h, shown[position])
            return h.root
        }
    }

    private fun tabById(id: String): Tab? {
        for (t in tabs) {
            if (t.id == id) return t
        }
        return null
    }

    /** One card, built once and then reused for other tabs while you scroll. */
    private fun makeHolder(): TabHolder {
        val h = TabHolder(FrameLayout(this))
        h.root.layoutParams = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        h.root.setPadding(dp(14), dp(5), dp(14), dp(5))
        h.card.orientation = LinearLayout.VERTICAL
        h.card.setPadding(dp(12), dp(12), dp(12), dp(12))
        h.bgCard.cornerRadius = dpf(24f)
        h.card.background = h.bgCard
        h.root.addView(h.card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        h.bgLetter.cornerRadius = dpf(10f)
        h.letter.gravity = Gravity.CENTER
        h.letter.textSize = 14f
        h.letter.typeface = Typeface.DEFAULT_BOLD
        h.letter.background = h.bgLetter
        h.icon.scaleType = ImageView.ScaleType.FIT_CENTER
        h.icon.setPadding(dp(3), dp(3), dp(3), dp(3))
        h.icon.visibility = View.GONE
        h.iconBox.addView(h.letter, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        h.iconBox.addView(h.icon, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        head.addView(h.iconBox, LinearLayout.LayoutParams(dp(32), dp(32)))
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(10), 0, dp(6), 0)
        h.title.textSize = 14f
        h.title.typeface = Typeface.DEFAULT_BOLD
        h.title.maxLines = 1
        h.title.ellipsize = TextUtils.TruncateAt.END
        h.sub.textSize = 11f
        h.sub.maxLines = 1
        h.sub.ellipsize = TextUtils.TruncateAt.END
        col.addView(h.title)
        col.addView(h.sub)
        head.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        h.close.setImageResource(R.drawable.ic_close)
        h.close.contentDescription = "Close tab"
        for (b in arrayOf(h.pin, h.close)) {
            b.setBackgroundResource(0)
            b.setPadding(dp(9), dp(9), dp(9), dp(9))
            b.scaleType = ImageView.ScaleType.FIT_CENTER
            head.addView(b, LinearLayout.LayoutParams(dp(38), dp(38)))
            press(b)
        }
        h.card.addView(head, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // preview area: the page picture, or the skeleton until there is one, and the shine of the open tab
        h.bgPrev.cornerRadius = dpf(16f)
        h.preview.background = h.bgPrev
        h.preview.clipToOutline = true
        h.bars.orientation = LinearLayout.VERTICAL
        h.bars.setPadding(dp(12), dp(12), dp(12), dp(12))
        val inner = dp(250)
        fun bar(frac: Float, hDp: Int, r: Float, top: Int) {
            val g = GradientDrawable()
            g.cornerRadius = dpf(r)
            h.barBgs.add(g)
            val v = View(this)
            v.background = g
            val w = if (frac >= 1f) ViewGroup.LayoutParams.MATCH_PARENT else (inner * frac).toInt()
            val lp = LinearLayout.LayoutParams(w, dp(hDp))
            lp.topMargin = top
            h.bars.addView(v, lp)
        }
        bar(0.6f, 14, 7f, 0)
        bar(1f, 8, 4f, dp(8))
        bar(0.8f, 8, 4f, dp(8))
        bar(1f, 8, 4f, dp(8))
        bar(0.45f, 8, 4f, dp(8))
        bar(1f, 46, 10f, dp(8))
        h.preview.addView(h.bars, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        h.shot.scaleType = ImageView.ScaleType.MATRIX
        h.shot.visibility = View.GONE
        h.shot.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            val dr = (v as ImageView).drawable
            if (dr is BitmapDrawable) fitTop(v, dr.bitmap)
        }
        h.preview.addView(h.shot, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        h.shine.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(0x00FFFFFF, 0x30FFFFFF, 0x00FFFFFF)
        )
        h.shine.visibility = View.GONE
        h.preview.addView(h.shine, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val plp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(168))
        plp.topMargin = dp(10)
        h.card.addView(h.preview, plp)

        h.card.setOnClickListener {
            val t = tabById(h.tabId)
            if (t != null) openTabFromList(t)
        }
        h.pin.setOnClickListener {
            val t = tabById(h.tabId)
            if (t != null) togglePin(t)
        }
        h.close.setOnClickListener {
            val t = tabById(h.tabId)
            if (t != null) requestClose(t, h.root)
        }
        return h
    }

    /** Fills a card from the real state of its tab: title, address, icon, pin, preview picture and which tab is open. */
    private fun bindHolder(h: TabHolder, t: Tab) {
        val p = pal()
        val cur = t === current
        h.tabId = t.id
        h.root.alpha = 1f
        h.root.translationX = 0f
        val name = when {
            t.trusted -> "NOMAN GO"
            t.title.isNotBlank() -> t.title
            t.url.isNotBlank() -> displayUrl(t.url)
            else -> "New tab"
        }
        val sub = when {
            t.incognito && t.trusted -> "Incognito"
            t.incognito -> "Incognito, " + displayUrl(t.url)
            t.trusted -> "Home"
            else -> displayUrl(t.url)
        }
        val accent = when {
            t.incognito -> c("#9A73FF")
            t.trusted -> c("#FF2D3F")
            else -> tabColor(t.url + name)
        }
        h.bgCard.setColor(p.card)
        h.bgCard.setStroke(dp(if (cur) 2 else 1), if (cur) p.accent else p.line)
        h.bgPrev.setColor(mix(accent, p.bg, 0.09f))
        h.bgLetter.setColor(mix(accent, p.card, 0.16f))
        val barColor = (accent and 0x00FFFFFF) or (0x4D shl 24)
        for (g in h.barBgs) g.setColor(barColor)
        h.title.text = name
        h.title.setTextColor(p.text)
        h.sub.text = sub
        h.sub.setTextColor(p.sub)
        h.letter.text = name.take(1).uppercase()
        h.letter.setTextColor(accent)

        val fav = faviconFor(t, h)
        if (fav != null) {
            h.icon.setImageBitmap(fav)
            h.icon.visibility = View.VISIBLE
            h.letter.visibility = View.GONE
        } else {
            h.icon.setImageDrawable(null)
            h.icon.visibility = View.GONE
            h.letter.visibility = View.VISIBLE
        }

        h.pin.setImageResource(if (t.pinned) R.drawable.ic_pin_fill else R.drawable.ic_pin)
        h.pin.setColorFilter(if (t.pinned) p.accent else p.sub, PorterDuff.Mode.SRC_IN)
        h.pin.contentDescription = if (t.pinned) "Unpin tab" else "Pin tab"
        h.close.setColorFilter(p.sub, PorterDuff.Mode.SRC_IN)

        val b = thumbCache.get(t.id)
        h.shot.alpha = 1f
        if (b != null) {
            h.shot.setImageBitmap(b)
            fitTop(h.shot, b)
            h.bars.visibility = View.GONE
            h.shot.visibility = View.VISIBLE
        } else {
            h.shot.setImageDrawable(null)
            h.shot.visibility = View.GONE
            h.bars.visibility = View.VISIBLE
            loadThumbFor(h, t)
        }

        if (cur) {
            h.shine.visibility = View.VISIBLE
            shineView = h.shine
        } else {
            h.shine.visibility = View.GONE
            if (shineView === h.shine) shineView = null
        }
    }

    /** The site icon from memory, or (once) from disk. A card that is reused for another tab ignores a late answer. */
    private fun faviconFor(t: Tab, h: TabHolder): Bitmap? {
        val f = t.favicon ?: iconCache.get(t.id)
        if (f != null) return f
        if (t.trusted || t.incognito || t.iconTried) return null
        t.iconTried = true
        val name = t.id
        thumbPool.execute {
            val file = iconFile(name)
            if (file.exists()) {
                val b = try {
                    BitmapFactory.decodeFile(file.absolutePath)
                } catch (e: Throwable) {
                    null
                }
                if (b != null) {
                    runOnUiThread {
                        iconCache.put(name, b)
                        val tab = tabById(name)
                        if (tab != null && tab.favicon == null) tab.favicon = b
                        if (h.tabId == name) {
                            h.icon.setImageBitmap(b)
                            h.icon.visibility = View.VISIBLE
                            h.letter.visibility = View.GONE
                        }
                    }
                }
            }
        }
        return null
    }

    private fun loadThumbFor(h: TabHolder, t: Tab) {
        if (t.incognito) return
        val name = t.id
        thumbPool.execute {
            val f = thumbFile(name)
            if (f.exists()) {
                val b = try {
                    BitmapFactory.decodeFile(f.absolutePath)
                } catch (e: Throwable) {
                    null
                }
                if (b != null) {
                    runOnUiThread {
                        thumbCache.put(name, b)
                        if (h.tabId == name) showThumb(h.shot, h.bars, b)
                    }
                }
            }
        }
    }

    private fun matchesTab(t: Tab, q: String): Boolean {
        if (q.isEmpty()) return true
        val title = if (t.trusted) "noman go home" else t.title.lowercase()
        return title.contains(q) || t.url.lowercase().contains(q)
    }

    /** Pinned tabs first, then the rest; only what matches the search. The list is touched only when something changed. */
    private fun applyTabFilter(force: Boolean) {
        val q = tabQuery.trim().lowercase()
        val out = ArrayList<Tab>()
        for (t in tabs) {
            if (t.pinned && matchesTab(t, q)) out.add(t)
        }
        for (t in tabs) {
            if (!t.pinned && matchesTab(t, q)) out.add(t)
        }
        var same = out.size == shown.size
        if (same) {
            for (i in out.indices) {
                if (out[i] !== shown[i]) {
                    same = false
                    break
                }
            }
        }
        if (!same || force) {
            shown.clear()
            shown.addAll(out)
            tabAdapter.notifyDataSetChanged()
        }
        tabsEmpty.text = if (q.isEmpty()) "No tabs" else "No tabs match your search"
        tabsEmpty.visibility = if (out.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showTabs() {
        dropFocus()
        tabsOpen = true
        refreshTabsButton()
        val now = current
        if (now != null) captureThumb(now)
        tabQuery = ""
        tabSearch.setText("")
        updateTabsTitle()
        applyTabFilter(true)
        val idx = if (now != null) shown.indexOf(now) else -1
        if (idx >= 0) tabsListView.setSelectionFromTop(idx, dp(6))
        tabsOverlay.animate().cancel()
        tabsOverlay.visibility = View.VISIBLE
        tabsOverlay.alpha = 0f
        tabsOverlay.scaleX = 1.08f
        tabsOverlay.scaleY = 1.08f
        tabsOverlay.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(d(420))
            .setInterpolator(PathInterpolator(0.2f, 0.9f, 0.25f, 1f)).start()
        startShine()
    }

    private fun hideTabs() {
        tabsOpen = false
        refreshTabsButton()
        stopShine()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(tabSearch.windowToken, 0)
        tabSearch.clearFocus()
        tabsOverlay.animate().cancel()
        tabsOverlay.animate().alpha(0f).scaleX(1.08f).scaleY(1.08f).setDuration(d(260))
            .withEndAction { tabsOverlay.visibility = View.GONE }.start()
    }

    /** The moving shine runs on the card of the open tab only, one view, one animator. */
    private fun startShine() {
        shineAnim?.cancel()
        shineAnim = null
        if (d(1L) == 0L) return
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 2600L
        a.repeatCount = ValueAnimator.INFINITE
        a.interpolator = AccelerateDecelerateInterpolator()
        a.addUpdateListener {
            val v = shineView
            if (v != null) v.translationX = (-1f + 2f * (it.animatedValue as Float)) * v.width.toFloat()
        }
        a.start()
        shineAnim = a
    }

    private fun stopShine() {
        shineAnim?.cancel()
        shineAnim = null
    }

    private fun openTabFromList(t: Tab) {
        if (tabs.contains(t)) switchTo(t, true)
    }

    private fun togglePin(t: Tab) {
        t.pinned = !t.pinned
        tabs.remove(t)
        var at = 0
        while (at < tabs.size && tabs[at].pinned) at++
        tabs.add(at, t)
        scheduleSessionSave()
        applyTabFilter(true)
        showToast(if (t.pinned) "Tab pinned" else "Tab unpinned")
    }

    private fun requestClose(t: Tab, row: View?) {
        if (t.pinned) {
            val label = if (t.title.isNotBlank()) t.title else displayUrl(t.url)
            showConfirm("Close pinned tab?", "\"" + label.take(60) + "\" is pinned. Close it anyway?", "Close tab", null) {
                closeFromList(t, row)
            }
        } else {
            closeFromList(t, row)
        }
    }

    private fun closeFromList(t: Tab, row: View?) {
        val work = Runnable {
            if (tabs.contains(t)) {
                keepTabsOpen = true
                try {
                    closeTab(t)
                } finally {
                    keepTabsOpen = false
                }
            }
            applyTabFilter(false)
            updateTabsTitle()
        }
        if (row != null && d(1L) > 0L) {
            row.animate().alpha(0f).translationX(dpf(70f)).setDuration(d(170)).withEndAction(work).start()
        } else {
            work.run()
        }
    }

    private fun closeAllDialog() {
        var pinned = 0
        for (t in tabs) {
            if (t.pinned) pinned++
        }
        val loose = tabs.size - pinned
        val msg = if (loose > 0) {
            "$loose " + (if (loose == 1) "tab" else "tabs") + " will be closed."
        } else {
            "Only pinned tabs are open. Pinned tabs stay unless you tick the box."
        }
        val box = if (pinned > 0) "Also close $pinned pinned " + (if (pinned == 1) "tab" else "tabs") else null
        showConfirm("Close all tabs?", msg, "Close all", box) { withPinned -> closeAllNow(withPinned) }
    }

    /** Closes every tab that is not pinned (and the pinned ones too when asked), switching to a surviving tab only once. */
    private fun closeAllNow(includePinned: Boolean) {
        val doomed = ArrayList<Tab>()
        for (t in tabs) {
            if (includePinned || !t.pinned) doomed.add(t)
        }
        if (doomed.isEmpty()) {
            showToast("Pinned tabs are kept. Tick the box to close them too.")
            return
        }
        var firstSurvivor: Tab? = null
        for (t in tabs) {
            if (!doomed.contains(t)) {
                firstSurvivor = t
                break
            }
        }
        val cur = current
        if (cur != null && doomed.contains(cur)) {
            doomed.remove(cur)
            doomed.add(cur)
        }
        keepTabsOpen = true
        try {
            val survivor = firstSurvivor
            if (cur != null && survivor != null && doomed.contains(cur)) switchTo(survivor, false)
            for (t in doomed) closeTab(t)
        } finally {
            keepTabsOpen = false
        }
        applyTabFilter(true)
        updateTabsTitle()
        showToast("Closed " + doomed.size + (if (doomed.size == 1) " tab" else " tabs"))
    }

    /** A small confirmation card in the same style as the other sheets. [onYes] gets the state of the optional box. */
    private fun showConfirm(title: String, message: String, yes: String, checkLabel: String?, onYes: (Boolean) -> Unit) {
        val p = pal()
        val dlg = Dialog(this)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.background = rounded(p.card, 22f, p.line, 1)
        box.setPadding(dp(18), dp(18), dp(18), dp(14))
        val tt = TextView(this)
        tt.text = title
        tt.textSize = 18f
        tt.typeface = Typeface.DEFAULT_BOLD
        tt.setTextColor(p.text)
        box.addView(tt)
        val mm = TextView(this)
        mm.text = message
        mm.textSize = 14f
        mm.setTextColor(p.sub)
        mm.setPadding(0, dp(8), 0, dp(4))
        box.addView(mm)
        var cb: CheckBox? = null
        if (checkLabel != null) {
            val c2 = CheckBox(this)
            c2.text = checkLabel
            c2.setTextColor(p.text)
            c2.buttonTintList = ColorStateList.valueOf(p.accent)
            box.addView(c2)
            cb = c2
        }
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.END
        val no = TextView(this)
        no.text = "Cancel"
        no.gravity = Gravity.CENTER
        no.textSize = 15f
        no.typeface = Typeface.DEFAULT_BOLD
        no.setTextColor(p.text)
        no.background = rounded(p.pill, 22f)
        no.setOnClickListener { dlg.dismiss() }
        val ok = TextView(this)
        ok.text = yes
        ok.gravity = Gravity.CENTER
        ok.textSize = 15f
        ok.typeface = Typeface.DEFAULT_BOLD
        ok.setTextColor(Color.WHITE)
        ok.background = rounded(p.accent, 22f)
        val box2 = cb
        ok.setOnClickListener {
            dlg.dismiss()
            onYes(box2 != null && box2.isChecked)
        }
        val l1 = LinearLayout.LayoutParams(dp(100), dp(44))
        val l2 = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(44))
        l2.leftMargin = dp(10)
        ok.setPadding(dp(22), 0, dp(22), 0)
        row.addView(no, l1)
        row.addView(ok, l2)
        val rlp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        rlp.topMargin = dp(14)
        box.addView(row, rlp)
        val wrap = FrameLayout(this)
        wrap.setPadding(dp(24), 0, dp(24), 0)
        wrap.addView(box, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        dlg.setContentView(wrap)
        val win = dlg.window
        if (win != null) {
            win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            win.setGravity(Gravity.CENTER)
            win.setDimAmount(0.55f)
        }
        dlg.setCanceledOnTouchOutside(true)
        dlg.show()
        box.alpha = 0f
        box.scaleX = 0.92f
        box.scaleY = 0.92f
        box.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(d(200)).setInterpolator(DecelerateInterpolator()).start()
    }

    // =====================================================================
    // home, voice search, toolbar animation
    // =====================================================================
    private fun goHome() {
        val t = current ?: return
        dropFocus()
        if (t.trusted && t.url == HOME) {
            t.web.evaluateJavascript("window.scrollTo({top:0,behavior:'smooth'})", null)
        } else {
            t.web.loadUrl(HOME)
        }
    }

    @Suppress("DEPRECATION")
    private fun applyRefreshRate() {
        try {
            val disp = windowManager.defaultDisplay
            var best = 60f
            if (!stB("eco", false)) {
                best = disp.refreshRate
                for (m in disp.supportedModes) if (m.refreshRate > best) best = m.refreshRate
            }
            if (best != lastRate) {
                lastRate = best
                val lp = window.attributes
                lp.preferredRefreshRate = best
                window.attributes = lp
            }
        } catch (e: Exception) {
        }
    }

    /** Optional: put Outfit-Bold.ttf in res/font/ as outfit_bold.ttf and the toolbar text uses it (matches the HTML). */
    private fun applyBrandFont() {
        if (Build.VERSION.SDK_INT < 26) return
        val id = resources.getIdentifier("outfit_bold", "font", packageName)
        if (id == 0) return
        try {
            val tf = resources.getFont(id)
            urlInput.typeface = tf
            tabCount.typeface = tf
            toastView.typeface = tf
        } catch (e: Exception) {
        }
    }

    private fun micAvailable(): Boolean = try {
        packageManager.queryIntentActivities(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0).isNotEmpty()
    } catch (e: Exception) {
        false
    }

    private fun applyMic() {
        btnMic.visibility = if (stB("mic", true) && micAvailable()) View.VISIBLE else View.GONE
    }

    private fun voiceLang(): String {
        val k = stS("vl", "").ifEmpty { stS("hl", "") }
        return when (k) {
            "bn" -> "bn-BD"
            "hi" -> "hi-IN"
            "ar" -> "ar-SA"
            "en" -> "en-US"
            else -> java.util.Locale.getDefault().toLanguageTag()
        }
    }

    private fun startVoice() {
        dropFocus()
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, voiceLang())
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search")
        try {
            startActivityForResult(i, REQ_VOICE)
        } catch (e: Exception) {
            showToast("Voice search is not available on this device")
        }
    }

    private fun playToolbarIntro() {
        if (d(1L) == 0L) return
        val views = arrayOf<View>(btnHome, urlPill, btnMic, btnTabs, btnMenu)
        for ((n, v) in views.withIndex()) {
            if (v.visibility != View.VISIBLE) continue
            v.alpha = 0f
            v.translationY = -dpf(14f)
            v.scaleX = 0.7f
            v.scaleY = 0.7f
            v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setStartDelay(d(80L + n * 55L)).setDuration(d(450))
                .setInterpolator(OvershootInterpolator(1.6f)).start()
        }
    }

    private fun bob(v: View) {
        v.animate().translationY(-dpf(3f)).scaleX(1.12f).scaleY(1.12f).setDuration(d(220)).withEndAction {
            v.animate().translationY(0f).scaleX(1f).scaleY(1f).setDuration(d(260))
                .setInterpolator(OvershootInterpolator(2f)).start()
        }.start()
    }

    private val idleTick = object : Runnable {
        override fun run() {
            if (d(1L) > 0L && menuPopup == null && tabsOverlay.visibility != View.VISIBLE && !urlInput.hasFocus()) {
                bob(btnHome)
                if (btnMic.visibility == View.VISIBLE) ui.postDelayed({ bob(btnMic) }, 700L)
            }
            ui.postDelayed(this, 4200L)
        }
    }

    private fun startIdle() {
        ui.removeCallbacks(idleTick)
        ui.postDelayed(idleTick, 3000L)
    }

    private fun stopIdle() {
        ui.removeCallbacks(idleTick)
    }

    // ---- menu icon animation: pop in, then a small loop that only runs while the menu is open
    private fun stopMenuAnims() {
        for (a in menuAnims) a.cancel()
        menuAnims.clear()
    }

    private fun animateMenuIcon(iv: ImageView, kind: String, order: Int) {
        if (d(1L) == 0L) return
        iv.scaleX = 0f
        iv.scaleY = 0f
        iv.rotation = -120f
        iv.animate().scaleX(1f).scaleY(1f).rotation(0f)
            .setStartDelay(d(120L + order * 45L)).setDuration(d(520))
            .setInterpolator(OvershootInterpolator(2.2f))
            .withEndAction { startIdleIcon(iv, kind) }.start()
    }

    private fun loopOf(a: ObjectAnimator, ms: Long, reverse: Boolean, delay: Long): ObjectAnimator {
        a.duration = ms
        a.startDelay = delay
        a.repeatCount = ValueAnimator.INFINITE
        if (reverse) a.repeatMode = ValueAnimator.REVERSE
        a.interpolator = if (reverse) AccelerateDecelerateInterpolator() else LinearInterpolator()
        return a
    }

    private fun startIdleIcon(iv: ImageView, kind: String) {
        if (menuPopup == null || d(1L) == 0L) return
        val a: ObjectAnimator = when (kind) {
            "gear" -> loopOf(ObjectAnimator.ofFloat(iv, View.ROTATION, 0f, 360f), 7000L, false, 0L)
            "plus" -> loopOf(ObjectAnimator.ofFloat(iv, View.ROTATION, 0f, 90f), 900L, true, 600L)
            "mask" -> loopOf(ObjectAnimator.ofFloat(iv, View.TRANSLATION_Y, 0f, -dpf(2.5f)), 1100L, true, 0L)
            "hist", "refresh" -> loopOf(ObjectAnimator.ofFloat(iv, View.ROTATION, 0f, 16f), 1000L, true, 0L)
            "star", "share" -> loopOf(
                ObjectAnimator.ofPropertyValuesHolder(
                    iv,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.18f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.18f)
                ), 900L, true, 0L
            )
            "dl" -> loopOf(ObjectAnimator.ofFloat(iv, View.TRANSLATION_Y, 0f, dpf(3f)), 650L, true, 0L)
            "desk" -> loopOf(ObjectAnimator.ofFloat(iv, View.TRANSLATION_Y, 0f, -dpf(2.5f)), 1100L, true, 0L)
            "tr" -> loopOf(ObjectAnimator.ofFloat(iv, View.ROTATION, 0f, -12f), 900L, true, 0L)
            "back" -> loopOf(ObjectAnimator.ofFloat(iv, View.TRANSLATION_X, 0f, -dpf(3f)), 700L, true, 0L)
            "fwd" -> loopOf(ObjectAnimator.ofFloat(iv, View.TRANSLATION_X, 0f, dpf(3f)), 700L, true, 0L)
            else -> return
        }
        a.start()
        menuAnims.add(a)
    }

    // =====================================================================
    // three-dot menu
    // =====================================================================
    private fun showMenu() {
        if (menuPopup?.isShowing == true) {
            dismissMenu(null)
            return
        }
        val t = current ?: return
        val p = pal()
        dropFocus()
        stopMenuAnims()

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.background = rounded(p.card, 16f, p.line, 1)
        col.setPadding(dp(6), dp(8), dp(6), dp(8))
        col.elevation = dpf(10f)

        var i = 0
        val iconRow = LinearLayout(this)
        iconRow.orientation = LinearLayout.HORIZONTAL

        fun divider() {
            val v = View(this)
            v.setBackgroundColor(p.line)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
            lp.setMargins(dp(8), dp(4), dp(8), dp(4))
            col.addView(v, lp)
        }

        // Chrome style icon row: Back, Forward, (Bookmark on web pages), Refresh / Stop
        fun cell(icon: Int, tint: Int, label: String, enabled: Boolean, kind: String, onClick: () -> Unit) {
            val cv = LinearLayout(this)
            cv.orientation = LinearLayout.VERTICAL
            cv.gravity = Gravity.CENTER
            cv.setPadding(0, dp(8), 0, dp(6))
            if (enabled) ripple(cv)
            val iv = ImageView(this)
            iv.setImageResource(icon)
            iv.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
            cv.addView(iv, LinearLayout.LayoutParams(dp(24), dp(24)))
            val tv = TextView(this)
            tv.text = label
            tv.textSize = 10f
            tv.setTextColor(p.text)
            tv.gravity = Gravity.CENTER
            tv.setPadding(0, dp(3), 0, 0)
            cv.addView(tv)
            cv.alpha = if (enabled) 1f else 0.35f
            if (enabled) cv.setOnClickListener { dismissMenu(onClick) }
            iconRow.addView(cv, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            animateMenuIcon(iv, kind, i)
            i++
        }

        fun row(icon: Int, tint: Int, label: String, kind: String, sw: Boolean? = null, onClick: () -> Unit) {
            val r = LinearLayout(this)
            r.orientation = LinearLayout.HORIZONTAL
            r.gravity = Gravity.CENTER_VERTICAL
            r.setPadding(dp(12), 0, dp(12), 0)
            ripple(r)
            val iv = ImageView(this)
            iv.setImageResource(icon)
            iv.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
            r.addView(iv, LinearLayout.LayoutParams(dp(22), dp(22)))
            val tv = TextView(this)
            tv.text = label
            tv.textSize = 15f
            tv.setTextColor(p.text)
            tv.setPadding(dp(14), 0, 0, 0)
            r.addView(tv, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val swv: CheckBox? = if (sw != null) {
                val s = CheckBox(this)
                s.isChecked = sw
                s.isClickable = false
                s.isFocusable = false
                s.buttonTintList = ColorStateList.valueOf(p.accent)
                r.addView(s)
                s
            } else {
                null
            }
            r.setOnClickListener {
                val s = swv
                if (s != null) {
                    s.isChecked = !s.isChecked
                    ui.postDelayed({ dismissMenu(onClick) }, 170L)
                } else {
                    dismissMenu(onClick)
                }
            }
            col.addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)))
            animateMenuIcon(iv, kind, i)
            r.alpha = 0f
            r.translationY = -dpf(8f)
            r.animate().alpha(1f).translationY(0f).setStartDelay(d(40L + i * 22L)).setDuration(d(200))
                .setInterpolator(DecelerateInterpolator()).start()
            i++
        }

        val isWeb = !t.trusted && t.url.startsWith("http")
        val busy = t.loading && !t.trusted
        val canBack = t.web.canGoBack() || (t.parent?.let { tabs.contains(it) } == true)
        cell(R.drawable.ic_back, c("#64748B"), "Back", canBack, "back") { navBack(t) }
        cell(R.drawable.ic_forward, c("#64748B"), "Forward", t.web.canGoForward(), "fwd") { t.web.goForward() }
        if (isWeb) {
            val marked = store.isBookmarked(t.url)
            cell(
                if (marked) R.drawable.ic_star_fill else R.drawable.ic_star, STAR,
                if (marked) "Saved" else "Bookmark", true, "star"
            ) { toggleBookmark(t) }
        }
        cell(
            if (busy) R.drawable.ic_close else R.drawable.ic_refresh, c("#16A56A"),
            if (busy) "Stop" else "Refresh", true, "refresh"
        ) {
            if (busy) {
                t.web.stopLoading()
                finishLoad(t)
            } else {
                t.web.reload()
            }
        }
        col.addView(iconRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        divider()

        row(R.drawable.ic_plus, c("#4F8BFF"), "New tab", "plus") { newTab(null) }
        row(R.drawable.ic_mask, c("#8B5CF6"), "New incognito tab", "mask") { newTab(null, true) }
        if (isWeb) row(R.drawable.ic_share, c("#4F8BFF"), "Share", "share") { sharePage(t) }
        val docKind = when {
            t.docMime.startsWith("image/") -> "image"
            t.docMime.startsWith("video/") -> "video"
            t.docMime.startsWith("audio/") -> "audio"
            else -> ""
        }
        if (isWeb && docKind.isNotEmpty()) {
            row(R.drawable.ic_download, c("#16A56A"), "Download $docKind", "dl") { downloadMedia(t, docKind, t.url) }
        }
        if (isWeb) {
            if (t.trTarget.isNotEmpty()) {
                row(R.drawable.ic_translate, c("#0FB5A5"), "Show original", "tr") { restoreTranslation(t) }
            } else {
                row(R.drawable.ic_translate, c("#0FB5A5"), "Translate", "tr") { showTranslateSheet(t) }
            }
        }
        // Desktop site is in the menu on every screen: per site on web pages, for all sites on the home / internal pages
        val deskOn = if (t.trusted) stB("deskAll", false) else t.desktop
        row(
            R.drawable.ic_desktop, c("#FF7A2F"),
            if (t.trusted) "Desktop site (all sites)" else "Desktop site", "desk", deskOn
        ) { toggleDesktop(t) }
        divider()
        row(R.drawable.ic_star, STAR, "Bookmarks", "star") { openInternal("bookmarks") }
        row(R.drawable.ic_history, c("#64748B"), "History", "hist") { openInternal("history") }
        row(R.drawable.ic_download, c("#16A56A"), "Downloads", "dl") { openInternal("downloads") }
        row(R.drawable.ic_gear, c("#64748B"), "Settings", "gear") { openInternal("settings") }

        val wrap = FrameLayout(this)
        wrap.setPadding(dp(8), dp(2), dp(8), dp(16))
        wrap.addView(col, FrameLayout.LayoutParams(dp(262), ViewGroup.LayoutParams.WRAP_CONTENT))
        val pw = PopupWindow(wrap, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        pw.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        pw.isOutsideTouchable = true
        pw.setOnDismissListener {
            menuPopup = null
            stopMenuAnims()
            btnMenu.animate().rotation(0f).setDuration(d(200)).start()
        }
        menuPopup = pw
        wrap.pivotX = dpf(252f)
        wrap.pivotY = 0f
        wrap.alpha = 0f
        wrap.scaleX = 0.85f
        wrap.scaleY = 0.85f
        pw.showAsDropDown(btnMenu, 0, -dp(6), Gravity.END)
        wrap.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(d(220))
            .setInterpolator(OvershootInterpolator(1.1f)).start()
        btnMenu.animate().rotation(90f).setDuration(d(220)).start()
    }

    private fun dismissMenu(after: (() -> Unit)?) {
        val pw = menuPopup
        if (pw == null || !pw.isShowing) {
            after?.invoke()
            return
        }
        menuPopup = null
        stopMenuAnims()
        pw.contentView.animate().alpha(0f).scaleX(0.9f).scaleY(0.9f).setDuration(d(130)).withEndAction {
            try {
                pw.dismiss()
            } catch (e: Exception) {
            }
            btnMenu.animate().rotation(0f).setDuration(d(200)).start()
            after?.invoke()
        }.start()
    }

    private fun toggleBookmark(t: Tab) {
        val on = store.toggleBookmark(t.title.ifBlank { t.url }, t.url)
        showToast(if (on) "Bookmark added" else "Bookmark removed")
    }

    private fun sharePage(t: Tab) {
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_TEXT, t.url)
        i.putExtra(Intent.EXTRA_SUBJECT, t.title)
        startActivity(Intent.createChooser(i, "Share page"))
    }

    private fun hostOf(url: String): String? = try {
        Uri.parse(url).host?.removePrefix("www.")
    } catch (e: Exception) {
        null
    }

    /** Chrome style: a per-site choice wins, otherwise the global "Request desktop site" setting. */
    private fun effectiveDesktop(host: String?): Boolean {
        if (host == null) return false
        return store.deskOverride(host) ?: stB("deskAll", false)
    }

    private fun modeFor(t: Tab, host: String?): Boolean {
        if (t.incognito && host != null && t.manualHost == host) return t.desktop
        return effectiveDesktop(host)
    }

    /** Sets the right user-agent for the target site first, then loads it. */
    private fun loadWithMode(t: Tab, url: String) {
        if (!url.startsWith(ASSET)) {
            val want = modeFor(t, hostOf(url))
            if (want != t.desktop) {
                t.desktop = want
                applyMode(t)
            }
        }
        t.web.loadUrl(url)
    }

    private fun toggleGlobalDesktop() {
        val now = !stB("deskAll", false)
        val o = store.settings()
        o.put("deskAll", now)
        store.set(Store.K_ST, o.toString())
        st = o
        for (tb in tabs) {
            if (tb.trusted) {
                tb.peek()?.evaluateJavascript("window.nomanRefresh&&nomanRefresh()", null)
            } else {
                val want = modeFor(tb, hostOf(tb.url))
                if (want != tb.desktop) {
                    tb.desktop = want
                    if (tb.peek() != null) applyMode(tb)
                }
            }
        }
        showToast(if (now) "Desktop site on for all sites" else "Desktop site off for all sites")
    }

    private fun toggleDesktop(t: Tab) {
        if (t.trusted) {
            toggleGlobalDesktop()
            return
        }
        val host = hostOf(t.url)
        val now = !t.desktop
        if (host != null) {
            if (t.incognito) {
                t.manualHost = host
            } else {
                store.setDeskSite(host, if (now == stB("deskAll", false)) null else now)
            }
        }
        t.desktop = now
        applyMode(t)
        t.web.reload()
        showToast(if (now) "Desktop site on" else "Mobile site on")
    }

    // =====================================================================
    // back button
    // =====================================================================
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (menuPopup?.isShowing == true) {
            dismissMenu(null)
            return
        }
        if (customView != null) {
            exitFullscreen()
            return
        }
        if (tabsOverlay.visibility == View.VISIBLE) {
            hideTabs()
            return
        }
        if (urlInput.hasFocus()) {
            dropFocus()
            return
        }
        val t = current
        if (t == null) {
            super.onBackPressed()
            return
        }
        if (t.trusted) {
            // let the home page close its own popups / sub pages first
            t.web.evaluateJavascript("(function(){return !!(window.nomanBack&&window.nomanBack())})()") { r ->
                if (r != "true") backOrExit(t)
            }
            return
        }
        backOrExit(t)
    }

    private fun backOrExit(t: Tab) {
        if (navBack(t)) return
        if (!t.trusted) {
            t.web.loadUrl(HOME)
            return
        }
        if (t.pinned) {
            moveTaskToBack(true)
        } else if (tabs.size > 1) {
            closeTab(t)
        } else {
            finish()
        }
    }

    // =====================================================================
    // web clients
    // =====================================================================
    private fun handleUrl(uri: Uri): Boolean {
        val u = uri.toString()
        val scheme = uri.scheme ?: ""
        if (scheme == "http" || scheme == "https") return false
        if (u.startsWith(ASSET)) return false
        if (scheme == "about" || scheme == "data" || scheme == "blob" || scheme == "javascript") return false
        if (scheme == "file") return true
        try {
            val i = if (scheme == "intent") Intent.parseUri(u, Intent.URI_INTENT_SCHEME) else Intent(Intent.ACTION_VIEW, uri)
            i.addCategory(Intent.CATEGORY_BROWSABLE)
            i.component = null
            i.selector = null
            startActivity(i)
        } catch (e: Exception) {
            showToast("No app can open this link")
        }
        return true
    }

    private fun errorHtml(url: String, desc: String): String {
        val p = pal()
        fun hex(v: Int): String = String.format("#%06X", 0xFFFFFF and v)
        return "<html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><style>" +
            "body{margin:0;height:100vh;display:flex;flex-direction:column;align-items:center;justify-content:center;" +
            "font-family:sans-serif;text-align:center;padding:24px;box-sizing:border-box;" +
            "background:" + hex(p.bg) + ";color:" + hex(p.text) + "}" +
            "h2{margin:0 0 8px}p{margin:0 0 22px;color:" + hex(p.sub) + "}" +
            "a{background:" + hex(p.accent) + ";color:#fff;padding:12px 26px;border-radius:99px;" +
            "text-decoration:none;font-weight:600}</style></head><body><h2>Can't reach this page</h2><p>" +
            esc(desc) + "</p><a href=\"" + esc(url) + "\">Try again</a></body></html>"
    }

    private inner class Client(private val t: Tab) : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            val scheme = uri.scheme ?: ""
            if (request.isForMainFrame && request.method == "GET" && (scheme == "http" || scheme == "https")) {
                val want = modeFor(t, hostOf(uri.toString()))
                if (want != t.desktop) {
                    // the next site wants a different mode: switch the user-agent, then load it again
                    t.desktop = want
                    applyMode(t)
                    view.loadUrl(uri.toString())
                    return true
                }
            }
            val consumed = handleUrl(uri)
            if (consumed && t.loading) finishLoad(t)
            return consumed
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            if (hostOf(t.url) != hostOf(url)) dropIcon(t)
            t.url = url
            t.trusted = url.startsWith(ASSET)
            t.docMime = ""
            t.trGen++
            t.trTarget = ""
            t.trBusy = false
            if (t.errorPending) {
                t.errorPending = false
                t.errorShown = true
            } else {
                t.errorShown = false
            }
            view.settings.textZoom = if (t.trusted) 100 else zoomPct()
            beginLoad(t)
            if (t === current) {
                updateUrlUi()
                updateNav()
            }
        }

        override fun onPageCommitVisible(view: WebView, url: String) {
            if (t.desktop && !t.trusted) view.evaluateJavascript(DESKTOP_JS, null)
            if (!t.trusted) {
                view.evaluateJavascript(HOOK_JS, null)
                view.evaluateJavascript(pullJs, null)
            }
        }

        override fun onPageFinished(view: WebView, url: String) {
            t.progress = 100
            t.url = url
            t.title = view.title ?: ""
            if (t.desktop && !t.trusted) view.evaluateJavascript(DESKTOP_JS, null)
            if (!t.trusted) {
                view.evaluateJavascript(HOOK_JS, null)
                view.evaluateJavascript(pullJs, null)
                view.evaluateJavascript("document.contentType") { v -> t.docMime = unquoteJs(v).lowercase() }
            }
            if (!t.trusted && !t.errorShown && !t.incognito && url.startsWith("http")) {
                store.set(Store.K_LAST, url)
                if (stB("hist", true)) store.addHistory(t.title, url)
            }
            if (!t.incognito) scheduleSessionSave()
            finishLoad(t)
            if (t === current) {
                updateUrlUi()
                updateNav()
            }
        }

        /** A bad certificate on the page itself shows the error page; on anything else the load is just refused. */
        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            handler.cancel()
            val failing = error.url ?: ""
            val mine = failing.isNotEmpty() && (failing == view.url || failing == t.url)
            if (mine) {
                t.errorPending = true
                view.loadDataWithBaseURL(
                    failing, errorHtml(failing, "The security certificate of this site is not valid."), "text/html", "UTF-8", failing
                )
            } else if (t.loading && t.progress >= 100) {
                finishLoad(t)
            }
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (request.isForMainFrame) {
                t.errorPending = true
                val failing = request.url.toString()
                val why = error.description?.toString() ?: "Check your connection and try again."
                view.loadDataWithBaseURL(failing, errorHtml(failing, why), "text/html", "UTF-8", failing)
            }
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
            if (t === current) updateNav()
        }
    }

    private inner class Chrome(private val t: Tab) : WebChromeClient() {

        override fun onReceivedIcon(view: WebView, icon: Bitmap?) {
            if (icon == null || t.trusted) return
            try {
                val small = Bitmap.createScaledBitmap(icon, 64, 64, true)
                t.favicon = small
                t.iconTried = true
                iconCache.put(t.id, small)
                if (!t.incognito) {
                    val name = t.id
                    thumbPool.execute {
                        try {
                            FileOutputStream(iconFile(name)).use { small.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        } catch (e: Exception) {
                        }
                    }
                }
                if (tabsOpen) tabAdapter.notifyDataSetChanged()
            } catch (e: Throwable) {
            }
        }

        override fun onProgressChanged(view: WebView, newProgress: Int) {
            t.progress = newProgress
            t.guard.activity(SystemClock.uptimeMillis())
            if (t === current && !t.trusted && t.loading) setProgress(newProgress)
            if (newProgress >= 100 && t.loading) {
                // normally onPageFinished arrives right after; if it never does, this ends the load
                ui.postDelayed({
                    if (t.loading && t.progress >= 100) finishLoad(t)
                }, 1500L)
            }
        }

        override fun onReceivedTitle(view: WebView, title: String?) {
            t.title = title ?: ""
        }

        override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
            if (!isUserGesture) return false
            val nt = newTab(null, t.incognito, true, t, false)
            nt.desktop = t.desktop
            applyMode(nt)
            val transport = resultMsg.obj as WebView.WebViewTransport
            transport.webView = nt.web
            resultMsg.sendToTarget()
            return true
        }

        override fun onCloseWindow(window: WebView) {
            ui.post { tabs.firstOrNull { it.web === window }?.let { closeTab(it) } }
        }

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: WebChromeClient.FileChooserParams
        ): Boolean {
            fileCb?.onReceiveValue(null)
            fileCb = filePathCallback
            return try {
                startActivityForResult(fileChooserParams.createIntent(), REQ_FILE)
                true
            } catch (e: Exception) {
                fileCb = null
                false
            }
        }

        override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
            enterFullscreen(view, callback)
        }

        override fun onHideCustomView() {
            exitFullscreen()
        }
    }

    @Suppress("DEPRECATION")
    private fun enterFullscreen(v: View, cb: WebChromeClient.CustomViewCallback) {
        if (customView != null) {
            cb.onCustomViewHidden()
            return
        }
        customView = v
        customCb = cb
        (window.decorView as ViewGroup).addView(v, FrameLayout.LayoutParams(-1, -1))
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    @Suppress("DEPRECATION")
    private fun exitFullscreen() {
        val v = customView ?: return
        (window.decorView as ViewGroup).removeView(v)
        customView = null
        customCb?.onCustomViewHidden()
        customCb = null
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        setLightBars(!pal().dark)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQ_VOICE) {
            if (resultCode == RESULT_OK && data != null) {
                val r = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val txt = if (r != null && r.isNotEmpty()) r[0] else null
                if (txt != null) navigateCurrent(txt)
            }
            return
        }
        if (requestCode == REQ_FILE) {
            val r = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            fileCb?.onReceiveValue(r)
            fileCb = null
            return
        }
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
    }

    // =====================================================================
    // downloads
    // =====================================================================
    private fun hasStoragePermission(): Boolean =
        Build.VERSION.SDK_INT > 28 ||
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    /** Entry point of the WebView download listener (files, PDFs, blob: and data: links). */
    private fun startDownload(t: Tab?, url: String, ua: String?, cd: String?, mime: String?) {
        val agent = if (!ua.isNullOrEmpty()) ua else (t?.peek()?.settings?.userAgentString ?: uaMobile)
        dispatchDownload(DlReq(url, agent, cd ?: "", mime ?: "", t?.url ?: "", ""), t)
    }

    private fun dispatchDownload(r: DlReq, t: Tab?) {
        val u = r.url
        val web = u.startsWith("http://") || u.startsWith("https://")
        val blob = u.startsWith("blob:")
        val data = u.startsWith("data:")
        if (!web && !blob && !data) {
            showToast("This kind of link cannot be downloaded")
            return
        }
        if (!hasStoragePermission()) {
            pendingReq = r
            pendingTab = t
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQ_STORAGE)
            return
        }
        when {
            web -> if (needsProbe(r)) probeThenEnqueue(r) else enqueueDownload(r)
            blob -> downloadBlob(t, r)
            else -> saveDataUrl(r)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_STORAGE) {
            val r = pendingReq
            val t = pendingTab
            pendingReq = null
            pendingTab = null
            if (r != null && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchDownload(r, if (t != null && tabs.contains(t)) t else null)
            } else {
                showToast("Storage permission is needed to download files")
            }
        }
    }

    // ---- file names and types
    private fun cdName(cd: String): String? {
        if (cd.isEmpty()) return null
        val star = Regex("filename\\*\\s*=\\s*([^']*)'[^']*'([^;]+)", RegexOption.IGNORE_CASE).find(cd)
        if (star != null) {
            try {
                val cs = star.groupValues[1].ifEmpty { "UTF-8" }
                val raw = star.groupValues[2].trim().trim('"').replace("+", "%2B")
                val v = java.net.URLDecoder.decode(raw, cs)
                if (v.isNotBlank()) return v
            } catch (e: Exception) {
            }
        }
        val plain = Regex("filename\\s*=\\s*\"?([^\";]+)\"?", RegexOption.IGNORE_CASE).find(cd)
        if (plain != null) {
            val v = plain.groupValues[1].trim()
            if (v.isNotBlank()) return v
        }
        return null
    }

    private fun cleanName(n0: String): String {
        var n = n0.substringAfterLast('/').substringAfterLast('\\')
        n = n.replace(Regex("[\\u0000-\\u001f\"*:<>?|]"), "_").trim().trim('.')
        if (n.length > 120) {
            val ext = n.substringAfterLast('.', "")
            n = if (ext.isNotEmpty() && ext.length <= 8) n.substring(0, 110) + "." + ext else n.substring(0, 120)
        }
        return if (n.isEmpty()) "download" else n
    }

    private fun extOfMime(m: String): String? = MimeTypeMap.getSingleton().getExtensionFromMimeType(m)

    private fun mimeForName(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext.isEmpty()) return ""
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: ""
    }

    private fun defaultMime(kind: String): String = when (kind) {
        "image" -> "image/jpeg"
        "video" -> "video/mp4"
        "audio" -> "audio/mpeg"
        else -> ""
    }

    private fun nameFromMime(mime: String, kind: String): String {
        val base = when (kind) {
            "image" -> "image"
            "video" -> "video"
            "audio" -> "audio"
            else -> "download"
        }
        val e = if (mime.isNotEmpty()) extOfMime(mime) else null
        return if (e != null) "$base.$e" else base
    }

    private fun fileNameFor(url: String, cd: String, mime: String, kind: String): String {
        var name = cdName(cd)
        if (name == null) {
            name = URLUtil.guessFileName(url, if (cd.isEmpty()) null else cd, if (mime.isEmpty()) null else mime)
        }
        var n = cleanName(name)
        val ext = n.substringAfterLast('.', "").lowercase()
        val known = ext.isNotEmpty() && mimeForName(n).isNotEmpty()
        if (!known || ext == "bin") {
            val m = if (mime.isNotEmpty() && mime != "application/octet-stream") mime else defaultMime(kind)
            val e = if (m.isNotEmpty()) extOfMime(m) else null
            if (e != null) n = (if (ext == "bin") n.substringBeforeLast('.') else n) + "." + e
        }
        return n
    }

    private fun needsProbe(r: DlReq): Boolean {
        if (r.kind.isNotEmpty()) return true
        if (cdName(r.cd) != null) return false
        if (r.mime.isNotEmpty() && r.mime != "application/octet-stream") return false
        val seg = Uri.parse(r.url).lastPathSegment ?: ""
        return mimeForName(seg).isEmpty()
    }

    /** Asks the server what the file really is (type and name) before the download starts. */
    private fun probeThenEnqueue(r: DlReq) {
        showToast("Starting download")
        io.execute {
            var mime = r.mime ?: ""
            var cd = r.cd
            var con: HttpURLConnection? = null
            try {
                con = URL(r.url).openConnection() as HttpURLConnection
                con.connectTimeout = 6000
                con.readTimeout = 6000
                con.instanceFollowRedirects = true
                con.setRequestProperty("Range", "bytes=0-0")
                con.setRequestProperty("User-Agent", r.ua)
                if (r.ref.startsWith("http")) con.setRequestProperty("Referer", r.ref)
                val ck = CookieManager.getInstance().getCookie(r.url)
                if (!ck.isNullOrEmpty()) con.setRequestProperty("Cookie", ck)
                val code = con.responseCode
                if (code in 200..299) {
                    val ct = con.contentType
                    if (ct != null) {
                        val m = ct.substringBefore(';').trim().lowercase()
                        if (m.isNotEmpty() && (!m.startsWith("text/html") || r.kind == "link")) mime = m
                    }
                    val h = con.getHeaderField("Content-Disposition")
                    if (h != null) cd = h
                }
            } catch (e: Exception) {
            } finally {
                try {
                    con?.disconnect()
                } catch (e: Exception) {
                }
            }
            val fixed = DlReq(r.url, r.ua, cd, mime, r.ref, r.kind)
            runOnUiThread { enqueueDownload(fixed) }
        }
    }

    // ---- normal http(s) downloads go through the system DownloadManager
    @Suppress("DEPRECATION")
    private fun enqueueDownload(r: DlReq) {
        try {
            val name = fileNameFor(r.url, r.cd, r.mime, r.kind)
            var mime = if (r.mime.isNotEmpty() && r.mime != "application/octet-stream") r.mime else mimeForName(name)
            if (mime.isEmpty()) mime = defaultMime(r.kind)
            val req = DownloadManager.Request(Uri.parse(r.url))
            if (mime.isNotEmpty()) req.setMimeType(mime)
            if (r.ua.isNotEmpty()) req.addRequestHeader("User-Agent", r.ua)
            if (r.ref.startsWith("http")) req.addRequestHeader("Referer", r.ref)
            val ck = CookieManager.getInstance().getCookie(r.url)
            if (!ck.isNullOrEmpty()) req.addRequestHeader("Cookie", ck)
            req.setTitle(name)
            req.setDescription(hostOf(r.url))
            req.allowScanningByMediaScanner()
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            try {
                req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
            } catch (e: Exception) {
                req.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, name)
            }
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val id = dm.enqueue(req)
            store.addDownload(id, name, r.url)
            showToast("Downloading $name")
        } catch (e: SecurityException) {
            showToast("Storage permission is needed to download files")
        } catch (e: Exception) {
            showToast("Download could not start")
        }
    }

    private fun registerDownloadReceiver() {
        if (dlReceiver != null) return
        val rc = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                val id = i?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (id < 0L) return
                val rec = store.findDownload(id) ?: return
                val name = rec.optString("n")
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val cur = dm.query(DownloadManager.Query().setFilterById(id))
                var known = false
                var ok = false
                if (cur != null) {
                    if (cur.moveToFirst()) {
                        known = true
                        ok = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) ==
                            DownloadManager.STATUS_SUCCESSFUL
                    }
                    cur.close()
                }
                if (known) showToast(if (ok) "Downloaded: $name" else "Download failed: $name")
            }
        }
        val f = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(rc, f, Context.RECEIVER_EXPORTED) else registerReceiver(rc, f)
        dlReceiver = rc
    }

    // ---- files the app writes itself: blob:, data: and the backup file
    private fun uniqueFile(dir: File, name: String): File {
        var f = File(dir, name)
        if (!f.exists()) return f
        val base = name.substringBeforeLast('.', name)
        val ext = if (name.contains('.')) "." + name.substringAfterLast('.') else ""
        var i = 1
        while (f.exists() && i < 1000) {
            f = File(dir, "$base ($i)$ext")
            i++
        }
        return f
    }

    private fun openTarget(name0: String, mime: String): SaveTarget? {
        val name = cleanName(name0)
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val v = ContentValues()
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                if (mime.isNotEmpty()) v.put(MediaStore.MediaColumns.MIME_TYPE, mime)
                v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                v.put(MediaStore.MediaColumns.IS_PENDING, 1)
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v) ?: return null
                val out = contentResolver.openOutputStream(uri)
                if (out == null) {
                    contentResolver.delete(uri, null, null)
                    return null
                }
                return SaveTarget(out, uri, null, name, mime)
            }
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            val f = uniqueFile(dir, name)
            return SaveTarget(FileOutputStream(f), null, f, f.name, mime)
        } catch (e: Exception) {
            return null
        }
    }

    private fun abortTarget(tg: SaveTarget) {
        try {
            tg.out.close()
        } catch (e: Exception) {
        }
        try {
            val u = tg.uri
            if (u != null) contentResolver.delete(u, null, null) else tg.file?.delete()
        } catch (e: Exception) {
        }
    }

    @Suppress("DEPRECATION")
    private fun finishTarget(tg: SaveTarget, size: Long, source: String) {
        try {
            tg.out.flush()
            tg.out.close()
        } catch (e: Exception) {
        }
        val id = System.currentTimeMillis()
        val uri = tg.uri
        val file = tg.file
        try {
            if (uri != null) {
                val v = ContentValues()
                v.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(uri, v, null, null)
                store.addLocalDownload(id, tg.name, source, uri.toString(), tg.mime, size)
            } else if (file != null) {
                MediaScannerConnection.scanFile(this, arrayOf(file.absolutePath), arrayOf(tg.mime.ifEmpty { null }), null)
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val m = if (tg.mime.isEmpty()) "application/octet-stream" else tg.mime
                val did = dm.addCompletedDownload(tg.name, tg.name, true, m, file.absolutePath, size, true)
                store.addDownload(did, tg.name, source)
            }
        } catch (e: Exception) {
        }
        runOnUiThread { showToast("Saved to Downloads: " + tg.name) }
    }

    private fun saveBytes(name: String, mime: String, bytes: ByteArray, source: String): Boolean {
        val tg = openTarget(name, mime)
        if (tg == null) {
            runOnUiThread { showToast("Could not save the file") }
            return false
        }
        try {
            tg.out.write(bytes)
        } catch (e: Exception) {
            abortTarget(tg)
            runOnUiThread { showToast("Could not save the file") }
            return false
        }
        finishTarget(tg, bytes.size.toLong(), source)
        return true
    }

    private fun saveDataUrl(r: DlReq) {
        io.execute {
            try {
                val url = r.url
                val comma = url.indexOf(',')
                if (comma < 5) throw IllegalArgumentException("bad data url")
                val head = url.substring(5, comma)
                val payload = url.substring(comma + 1)
                val mime = head.substringBefore(';').ifEmpty { "text/plain" }
                val bytes = if (head.contains(";base64", ignoreCase = true)) {
                    Base64.decode(payload, Base64.DEFAULT)
                } else {
                    Uri.decode(payload).toByteArray(Charsets.UTF_8)
                }
                saveBytes(nameFromMime(mime, r.kind), mime, bytes, "data")
            } catch (e: Exception) {
                runOnUiThread { showToast("This file could not be saved") }
            }
        }
    }

    private fun downloadBlob(t: Tab?, r: DlReq) {
        val tab = t ?: current
        if (tab == null || tab.peek() == null) {
            showToast("This file could not be saved")
            return
        }
        val tok = UUID.randomUUID().toString()
        blobJobs[tok] = BlobJob(r.kind, r.mime, r.url)
        val js = BLOB_JS.replace("__U__", JSONObject.quote(r.url)).replace("__T__", JSONObject.quote(tok))
        tab.web.evaluateJavascript(js, null)
        showToast("Saving file")
        ui.postDelayed({
            val j = blobJobs[tok]
            if (j != null && j.target == null) blobJobs.remove(tok)
        }, 600000L)
    }

    /** Called by the page (through NomanBlob) with a one-time token that only this app knows. */
    private inner class BlobBridge {
        @JavascriptInterface
        fun begin(tok: String, mime: String, name: String, size: String) {
            val job = blobJobs[tok] ?: return
            val m = if (mime.isNotEmpty()) mime else job.mime
            var n = if (name.isNotBlank()) cleanName(name) else nameFromMime(m, job.kind)
            if (name.isNotBlank() && mimeForName(n).isEmpty()) {
                val e = if (m.isNotEmpty()) extOfMime(m) else null
                if (e != null) n = "$n.$e"
            }
            val tg = openTarget(n, m)
            if (tg == null) {
                blobJobs.remove(tok)
                runOnUiThread { showToast("Could not save the file") }
                return
            }
            job.target = tg
        }

        @JavascriptInterface
        fun chunk(tok: String, b64: String) {
            val job = blobJobs[tok] ?: return
            val tg = job.target ?: return
            try {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                tg.out.write(bytes)
                job.written += bytes.size.toLong()
            } catch (e: Exception) {
                blobJobs.remove(tok)
                abortTarget(tg)
                runOnUiThread { showToast("Download failed") }
            }
        }

        @JavascriptInterface
        fun end(tok: String) {
            val job = blobJobs.remove(tok) ?: return
            val tg = job.target ?: return
            finishTarget(tg, job.written, job.source)
        }

        @JavascriptInterface
        fun fail(tok: String, msg: String) {
            val job = blobJobs.remove(tok) ?: return
            val tg = job.target
            if (tg != null) abortTarget(tg)
            runOnUiThread { showToast("This file could not be saved") }
        }
    }

    // ---- the Downloads page
    private fun downloadsJson(): String {
        val arr = store.downloads()
        val out = JSONArray()
        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optLong("id")
            val localUri = o.optString("uri", "")
            if (localUri.isNotEmpty()) {
                var alive = false
                try {
                    val ins = contentResolver.openInputStream(Uri.parse(localUri))
                    if (ins != null) {
                        ins.close()
                        alive = true
                    }
                } catch (e: Exception) {
                }
                if (!alive) continue
                out.put(
                    JSONObject().put("id", id.toString()).put("n", o.optString("n")).put("u", o.optString("u"))
                        .put("t", o.optLong("t")).put("s", "done").put("p", 100).put("z", o.optLong("z"))
                        .put("b", o.optLong("z")).put("m", o.optString("m"))
                )
                continue
            }
            var status = "missing"
            var pct = 0
            var size = 0L
            var soFar = 0L
            var mimeType = ""
            val cur = dm.query(DownloadManager.Query().setFilterById(id))
            if (cur != null) {
                if (cur.moveToFirst()) {
                    val s = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val total = cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    soFar = cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    mimeType = cur.getString(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE)) ?: ""
                    size = total
                    pct = if (total > 0) (soFar * 100 / total).toInt() else 0
                    status = when (s) {
                        DownloadManager.STATUS_SUCCESSFUL -> "done"
                        DownloadManager.STATUS_FAILED -> "failed"
                        DownloadManager.STATUS_PAUSED -> "paused"
                        DownloadManager.STATUS_PENDING -> "pending"
                        else -> "running"
                    }
                }
                cur.close()
            }
            if (status == "missing") continue
            out.put(
                JSONObject().put("id", id.toString()).put("n", o.optString("n")).put("u", o.optString("u"))
                    .put("t", o.optLong("t")).put("s", status).put("p", pct).put("z", size)
                    .put("b", soFar).put("m", mimeType)
            )
        }
        return out.toString()
    }

    private fun openDownloadFile(id: Long) {
        try {
            val rec = store.findDownload(id)
            val local = rec?.optString("uri", "") ?: ""
            if (local.isNotEmpty()) {
                val m = rec?.optString("m", "") ?: ""
                val vi = Intent(Intent.ACTION_VIEW)
                vi.setDataAndType(Uri.parse(local), if (m.isEmpty()) "*/*" else m)
                vi.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(vi)
                return
            }
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = dm.getUriForDownloadedFile(id)
            val mime = dm.getMimeTypeForDownloadedFile(id)
            if (uri == null) {
                showToast("The file is not ready yet")
                return
            }
            val i = Intent(Intent.ACTION_VIEW)
            i.setDataAndType(uri, mime)
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        } catch (e: Exception) {
            showToast("No app can open this file")
        }
    }

    // =====================================================================
    // long press on an image, video or audio: Download, Open, Copy link
    // =====================================================================
    private fun onWebLongPress(t: Tab): Boolean {
        if (t.trusted || t !== current) return false
        val w = t.web
        val hit = w.hitTestResult
        val extra = hit.extra ?: ""
        val webUrl = extra.startsWith("http://") || extra.startsWith("https://")
        when (hit.type) {
            WebView.HitTestResult.IMAGE_TYPE -> {
                if (extra.isNotEmpty()) {
                    showMediaMenu(t, "image", extra, "")
                    return true
                }
            }
            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                val h = Handler(Looper.getMainLooper(), Handler.Callback { m ->
                    val src = m.data.getString("src") ?: ""
                    val link = m.data.getString("url") ?: extra
                    if (src.isNotEmpty()) showMediaMenu(t, "image", src, link) else probeMedia(t, link)
                    true
                })
                w.requestFocusNodeHref(h.obtainMessage())
                return true
            }
            WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                if (webUrl) {
                    probeMedia(t, extra)
                    return true
                }
            }
            WebView.HitTestResult.UNKNOWN_TYPE -> {
                probeMedia(t, "")
                return false
            }
            else -> {
            }
        }
        return false
    }

    private fun unquoteJs(raw: String?): String {
        if (raw == null || raw == "null" || raw.length < 2) return ""
        return try {
            JSONArray("[" + raw + "]").optString(0, "")
        } catch (e: Exception) {
            ""
        }
    }

    /** Looks at every element under the finger, so a video or an image behind a transparent overlay is still found.
     *  If there is none and [fallbackLink] is a link, the link menu is shown instead. */
    @Suppress("DEPRECATION")
    private fun probeMedia(t: Tab, fallbackLink: String) {
        val sc = t.web.scale
        if (sc <= 0f) {
            if (fallbackLink.isNotEmpty()) showMediaMenu(t, "link", fallbackLink, "")
            return
        }
        val js = PROBE_JS.replace("__X__", (touchX / sc).toString()).replace("__Y__", (touchY / sc).toString())
        t.web.evaluateJavascript(js) { raw ->
            val txt = unquoteJs(raw)
            var shown = false
            if (txt.isNotEmpty() && tabs.contains(t)) {
                try {
                    val o = JSONObject(txt)
                    val found = o.optString("l")
                    showMediaMenu(t, o.optString("k"), o.optString("u"), if (found.isNotEmpty()) found else fallbackLink)
                    shown = true
                } catch (e: Exception) {
                }
            }
            if (!shown && fallbackLink.isNotEmpty() && tabs.contains(t)) showMediaMenu(t, "link", fallbackLink, "")
        }
    }

    private fun mediaOk(kind: String, url: String): Boolean {
        if (url.isEmpty()) return false
        val web = url.startsWith("http://") || url.startsWith("https://")
        if (kind == "image") return web || url.startsWith("data:image") || url.startsWith("blob:")
        return web
    }

    private fun copyText(text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("link", text))
        showToast("Link copied")
    }

    private fun downloadMedia(t: Tab, kind: String, url: String) {
        val ua = t.peek()?.settings?.userAgentString ?: uaMobile
        dispatchDownload(DlReq(url, ua, "", "", t.url, kind), t)
    }

    private fun showMediaMenu(t: Tab, kind0: String, url: String, link: String) {
        val kind = if (kind0 == "video" || kind0 == "audio" || kind0 == "link") kind0 else "image"
        if (!mediaOk(kind, url)) {
            if ((kind == "video" || kind == "audio") && url.startsWith("blob:")) {
                showToast("This is a live stream and cannot be saved")
            }
            return
        }
        if (!tabs.contains(t)) return
        val p = pal()
        val dlg = Dialog(this)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.background = rounded(p.card, 22f, p.line, 1)
        box.setPadding(dp(6), dp(10), dp(6), dp(8))

        val head = TextView(this)
        head.text = if (url.startsWith("http")) {
            val u = Uri.parse(url)
            val h = (u.host ?: "").removePrefix("www.")
            val last = u.lastPathSegment
            if (last.isNullOrEmpty()) h else "$h / $last"
        } else {
            "Image on this page"
        }
        head.textSize = 12f
        head.setTextColor(p.sub)
        head.maxLines = 1
        head.ellipsize = TextUtils.TruncateAt.MIDDLE
        head.setPadding(dp(14), dp(2), dp(14), dp(8))
        box.addView(head, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        fun item(icon: Int, tint: Int, text: String, action: () -> Unit) {
            val r = LinearLayout(this)
            r.orientation = LinearLayout.HORIZONTAL
            r.gravity = Gravity.CENTER_VERTICAL
            r.setPadding(dp(14), 0, dp(14), 0)
            ripple(r)
            val iv = ImageView(this)
            iv.setImageResource(icon)
            iv.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
            r.addView(iv, LinearLayout.LayoutParams(dp(22), dp(22)))
            val tv = TextView(this)
            tv.text = text
            tv.textSize = 15f
            tv.setTextColor(p.text)
            tv.setPadding(dp(14), 0, 0, 0)
            r.addView(tv, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            r.setOnClickListener {
                dlg.dismiss()
                action()
            }
            box.addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)))
        }

        fun linkItems(target: String) {
            item(R.drawable.ic_open, c("#4F8BFF"), "Open link in new tab") { newTab(target, t.incognito, true, t) }
            if (!t.incognito) {
                item(R.drawable.ic_mask, c("#8B5CF6"), "Open link in incognito tab") { newTab(target, true, true, t) }
            }
            item(R.drawable.ic_copy, c("#64748B"), "Copy link address") { copyText(target) }
            item(R.drawable.ic_download, c("#16A56A"), "Download link") { downloadMedia(t, "link", target) }
        }

        if (kind == "link") {
            linkItems(url)
        } else {
            val web = url.startsWith("http://") || url.startsWith("https://")
            item(R.drawable.ic_download, c("#16A56A"), "Download $kind") { downloadMedia(t, kind, url) }
            if (web || url.startsWith("data:")) {
                item(R.drawable.ic_open, c("#4F8BFF"), "Open $kind") { newTab(url, t.incognito, true, t) }
            }
            if (web) item(R.drawable.ic_copy, c("#64748B"), "Copy $kind link") { copyText(url) }
            if (link.startsWith("http") && link != url) linkItems(link)
        }

        val wrap = FrameLayout(this)
        wrap.setPadding(dp(10), 0, dp(10), dp(10))
        wrap.addView(box, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        dlg.setContentView(wrap)
        val win = dlg.window
        if (win != null) {
            win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            win.setGravity(Gravity.BOTTOM)
            win.setDimAmount(0.5f)
        }
        dlg.setCanceledOnTouchOutside(true)
        dlg.show()
        box.translationY = dpf(40f)
        box.alpha = 0f
        box.animate().translationY(0f).alpha(1f).setDuration(d(220)).setInterpolator(DecelerateInterpolator()).start()
    }

    // =====================================================================
    // JavaScript bridge used by assets/index.html (only for the bundled home page)
    // =====================================================================
    private inner class Bridge(private val t: Tab) {

        private fun ok(): Boolean = t.trusted

        @JavascriptInterface
        fun get(k: String): String? = if (ok() && Store.BRIDGE_KEYS.contains(k)) store.get(k) else null

        @JavascriptInterface
        fun set(k: String, v: String) {
            if (ok() && Store.BRIDGE_KEYS.contains(k)) store.set(k, v)
        }

        @JavascriptInterface
        fun clearAll() {
            if (!ok()) return
            store.clearAll()
            runOnUiThread { this@MainActivity.applySettings() }
        }

        @JavascriptInterface
        fun navigate(s: String) {
            if (!ok()) return
            runOnUiThread {
                if (tabs.contains(t)) {
                    dropFocus()
                    val q = s.trim()
                    if (q.isNotEmpty() && !looksLikeUrl(q) && !q.startsWith("http")) store.addRecent(q)
                    loadWithMode(t, resolve(q))
                }
            }
        }

        @JavascriptInterface
        fun newIncognito() {
            if (!ok()) return
            runOnUiThread { newTab(null, true) }
        }

        @JavascriptInterface
        fun closePage() {
            if (!ok()) return
            runOnUiThread {
                if (tabs.contains(t) && !navBack(t)) t.web.loadUrl(HOME)
            }
        }

        @JavascriptInterface
        fun applySettings() {
            if (!ok()) return
            runOnUiThread { this@MainActivity.applySettings() }
        }

        @JavascriptInterface
        fun deskSites(): String = if (ok()) store.deskSites().toString() else "{}"

        @JavascriptInterface
        fun setDeskSite(host: String, mode: String) {
            if (!ok()) return
            store.setDeskSite(host, when (mode) {
                "d" -> true
                "m" -> false
                else -> null
            })
        }

        @JavascriptInterface
        fun downloads(): String = if (ok()) downloadsJson() else "[]"

        @JavascriptInterface
        fun openDownload(id: String) {
            if (!ok()) return
            val n = id.toLongOrNull() ?: return
            runOnUiThread { openDownloadFile(n) }
        }

        @JavascriptInterface
        fun removeDownload(id: String) {
            if (!ok()) return
            val n = id.toLongOrNull() ?: return
            store.removeDownload(n)
        }

        /** Stops a running download and removes it (and its half-written file) from the system. */
        @JavascriptInterface
        fun cancelDownload(id: String): Boolean {
            if (!ok()) return false
            val n = id.toLongOrNull() ?: return false
            var removed = false
            try {
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                removed = dm.remove(n) > 0
            } catch (e: Exception) {
            }
            store.removeDownload(n)
            return removed
        }

        /** Deletes the file from the phone and takes it off the list. */
        @JavascriptInterface
        fun deleteDownload(id: String): Boolean {
            if (!ok()) return false
            val n = id.toLongOrNull() ?: return false
            var gone = false
            try {
                val rec = store.findDownload(n)
                val local = rec?.optString("uri", "") ?: ""
                gone = if (local.isNotEmpty()) {
                    contentResolver.delete(Uri.parse(local), null, null) > 0
                } else {
                    val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    dm.remove(n) > 0
                }
            } catch (e: Exception) {
            }
            store.removeDownload(n)
            return gone
        }

        @JavascriptInterface
        fun shareDownload(id: String): Boolean {
            if (!ok()) return false
            val n = id.toLongOrNull() ?: return false
            try {
                val rec = store.findDownload(n)
                val local = rec?.optString("uri", "") ?: ""
                val uri: Uri?
                val mime: String
                if (local.isNotEmpty()) {
                    uri = Uri.parse(local)
                    mime = rec?.optString("m", "") ?: ""
                } else {
                    val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    uri = dm.getUriForDownloadedFile(n)
                    mime = dm.getMimeTypeForDownloadedFile(n) ?: ""
                }
                if (uri == null) {
                    runOnUiThread { showToast("The file is not ready yet") }
                    return false
                }
                val send = Intent(Intent.ACTION_SEND)
                send.type = if (mime.isEmpty()) "*/*" else mime
                send.putExtra(Intent.EXTRA_STREAM, uri)
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val chooser = Intent.createChooser(send, "Share file")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runOnUiThread { startActivity(chooser) }
                return true
            } catch (e: Exception) {
                runOnUiThread { showToast("Could not share this file") }
                return false
            }
        }

        /** Starts a failed download again from the same address. */
        @JavascriptInterface
        fun retryDownload(id: String): Boolean {
            if (!ok()) return false
            val n = id.toLongOrNull() ?: return false
            val rec = store.findDownload(n) ?: return false
            val url = rec.optString("u", "")
            if (!(url.startsWith("http://") || url.startsWith("https://"))) {
                runOnUiThread { showToast("This file has to be downloaded again from its page") }
                return false
            }
            try {
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.remove(n)
            } catch (e: Exception) {
            }
            store.removeDownload(n)
            runOnUiThread { dispatchDownload(DlReq(url, uaMobile, "", "", "", ""), null) }
            return true
        }

        @JavascriptInterface
        fun openDownloadsFolder() {
            if (!ok()) return
            runOnUiThread {
                try {
                    val i = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(i)
                } catch (e: Exception) {
                    showToast("No app can open the Downloads folder")
                }
            }
        }

        @JavascriptInterface
        fun trKeyInfo(): String {
            if (!ok()) return ""
            val k = store.secretGet("tr.key") ?: ""
            return if (k.length >= 4) "..." + k.takeLast(4) else if (k.isNotEmpty()) "set" else ""
        }

        @JavascriptInterface
        fun trSetKey(key: String): Boolean {
            if (!ok()) return false
            store.secretSet("tr.key", key.trim())
            return true
        }

        /** Translates one short sentence with the saved settings and tells the Settings page what happened. */
        @JavascriptInterface
        fun trTest() {
            if (!ok()) return
            val cfg = trConfig()
            val saved = stS("trl", "bn")
            val target = if (saved == "en") "es" else saved
            trPool.execute {
                val good: Boolean
                val msg: String
                if (!cfg.ready()) {
                    good = false
                    msg = "Add the key first, then test"
                } else {
                    val r = Translator(cfg).translate(listOf("Hello, how are you?"), target)
                    good = r.ok
                    msg = if (r.ok) "Connected to " + cfg.providerName() + ": " + r.texts[0] else r.error
                }
                runOnUiThread {
                    t.peek()?.evaluateJavascript("window.__trTest&&__trTest(" + good + "," + JSONObject.quote(msg) + ")", null)
                }
            }
        }

        /** The home page saves its backup file through here (a blob: download would need the page to stream it). */
        @JavascriptInterface
        fun saveFile(name: String, mime: String, text: String): Boolean {
            if (!ok()) return false
            if (!hasStoragePermission()) {
                runOnUiThread {
                    showToast("Allow storage access, then try again")
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQ_STORAGE)
                }
                return false
            }
            val m = if (mime.isEmpty()) "application/octet-stream" else mime
            io.execute { saveBytes(name, m, text.toByteArray(Charsets.UTF_8), "app") }
            return true
        }

        @JavascriptInterface
        fun clearBrowsing(cookies: Boolean, cache: Boolean) {
            if (!ok()) return
            runOnUiThread {
                if (cookies) {
                    CookieManager.getInstance().removeAllCookies(null)
                    CookieManager.getInstance().flush()
                    WebStorage.getInstance().deleteAllData()
                }
                if (cache) {
                    for (tb in tabs) tb.peek()?.clearCache(true)
                }
                showToast("Browsing data cleared")
            }
        }
    }
}
