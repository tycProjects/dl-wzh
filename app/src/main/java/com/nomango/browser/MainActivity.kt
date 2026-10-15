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
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
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
    val parent: Int, val state: ByteArray?
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
        var title: String = ""
        var url: String = ""
        var desktop: Boolean = false
        var trusted: Boolean = false
        var loading: Boolean = false
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
    private lateinit var progress: View
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
    private lateinit var tabsList: LinearLayout
    private lateinit var carousel: Carousel
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
    private var shimmerAnim: ValueAnimator? = null
    private val cardRefs = ArrayList<CardRef>()
    private var cardW = 0
    private var cardGap = 0
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
        toastView = findViewById(R.id.toast)

        val def = WebSettings.getDefaultUserAgent(this)
        uaMobile = def.replace("; wv", "")
        val ver = Regex("Chrome/([\\d.]+)").find(def)?.groupValues?.get(1) ?: "120.0.0.0"
        uaDesktop = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$ver Safari/537.36"

        progress.pivotX = 0f
        progress.scaleX = 0f
        pillBg.cornerRadius = dpf(20f)
        tabBg.cornerRadius = dpf(8f)
        CookieManager.getInstance().setAcceptCookie(true)
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        setupToolbar()
        setupTabsOverlay()
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
        if (u != null) newTab(u)
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
        stopIdle()
        current?.web?.onPause()
    }

    override fun onStop() {
        super.onStop()
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
        shimmerAnim?.cancel()
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
                newTab(data)
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
                        home, o.optInt("parent", -1), state
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
        progress.setBackgroundColor(p.accent)
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

    private fun setProgress(p: Int) {
        progress.animate().cancel()
        if (p in 1..99) {
            progress.alpha = 1f
            progress.animate().scaleX(p / 100f).setDuration(d(220)).setInterpolator(DecelerateInterpolator()).start()
        } else if (p >= 100) {
            progress.animate().scaleX(1f).setDuration(d(160)).withEndAction {
                progress.animate().alpha(0f).setDuration(d(220)).withEndAction { progress.scaleX = 0f }.start()
            }.start()
        } else {
            progress.alpha = 0f
            progress.scaleX = 0f
        }
    }

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

    private fun suggestRow(icon: Int, tint: Int, title: String, sub: String?, fill: String, onClick: () -> Unit): View {
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
                    addRow(suggestRow(R.drawable.ic_history, p.sub, r, null, r) { navigateCurrent(r) })
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
        w.setDownloadListener { url, ua, cd, mime, _ -> startDownload(t, url, ua, cd, mime) }
        w.setOnTouchListener { _, ev ->
            if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
                touchX = ev.x
                touchY = ev.y
            }
            false
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
        if (old != null && old !== t && tabs.contains(old)) {
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
        progress.animate().cancel()
        progress.alpha = 0f
        progress.scaleX = 0f
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
        btnNewTab.setOnClickListener { hideTabs(); newTab(null) }
        btnNewInc.setOnClickListener { hideTabs(); newTab(null, true) }
        btnTabsMenu.setOnClickListener { showMenu() }
        btnTabsDone.setOnClickListener { hideTabs() }
        carousel = Carousel(this)
        carousel.isHorizontalScrollBarEnabled = false
        carousel.overScrollMode = View.OVER_SCROLL_NEVER
        carousel.clipToPadding = false
        tabsList = LinearLayout(this)
        tabsList.orientation = LinearLayout.HORIZONTAL
        carousel.addView(tabsList, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        tabsBody.addView(carousel, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        carousel.onScrolled = { updateCarousel() }
        carousel.onSettled = { snapCarousel() }
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
    // tab switcher: the same 3D carousel as the home page
    // =====================================================================
    private inner class Carousel(ctx: Context) : HorizontalScrollView(ctx) {
        var onScrolled: (() -> Unit)? = null
        var onSettled: (() -> Unit)? = null
        private var down = false
        private val settle = Runnable {
            if (!down) onSettled?.invoke()
        }

        override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
            super.onScrollChanged(l, t, oldl, oldt)
            onScrolled?.invoke()
            removeCallbacks(settle)
            postDelayed(settle, 90L)
        }

        override fun onTouchEvent(ev: MotionEvent): Boolean {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    down = true
                    removeCallbacks(settle)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    down = false
                    removeCallbacks(settle)
                    postDelayed(settle, 90L)
                }
            }
            return super.onTouchEvent(ev)
        }
    }

    private inner class CardRef(val card: View, val shimmer: View, val tab: Tab) {
        var closing = false
        var near = true
    }

    private fun mix(a: Int, b: Int, t: Float): Int {
        val r = (Color.red(a) * t + Color.red(b) * (1f - t)).toInt()
        val g = (Color.green(a) * t + Color.green(b) * (1f - t)).toInt()
        val bl = (Color.blue(a) * t + Color.blue(b) * (1f - t)).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), bl.coerceIn(0, 255))
    }

    private fun showTabs() {
        dropFocus()
        tabsOpen = true
        refreshTabsButton()
        buildTabCards()
        tabsOverlay.animate().cancel()
        tabsOverlay.visibility = View.VISIBLE
        tabsOverlay.alpha = 0f
        tabsOverlay.scaleX = 1.08f
        tabsOverlay.scaleY = 1.08f
        tabsOverlay.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(d(420))
            .setInterpolator(PathInterpolator(0.2f, 0.9f, 0.25f, 1f)).start()
        startShimmer()
    }

    private fun hideTabs() {
        tabsOpen = false
        refreshTabsButton()
        stopShimmer()
        tabsOverlay.animate().cancel()
        tabsOverlay.animate().alpha(0f).scaleX(1.08f).scaleY(1.08f).setDuration(d(260))
            .withEndAction { tabsOverlay.visibility = View.GONE }.start()
    }

    private fun startShimmer() {
        shimmerAnim?.cancel()
        shimmerAnim = null
        if (d(1L) == 0L) return
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 2600L
        a.repeatCount = ValueAnimator.INFINITE
        a.interpolator = AccelerateDecelerateInterpolator()
        a.addUpdateListener {
            val f = it.animatedValue as Float
            val w = (cardW - dp(28)).toFloat()
            for (ref in cardRefs) {
                if (ref.near) ref.shimmer.translationX = (-1f + 2f * f) * w
            }
        }
        a.start()
        shimmerAnim = a
    }

    private fun stopShimmer() {
        shimmerAnim?.cancel()
        shimmerAnim = null
    }

    /** Same maths as the home page: tilt, shrink and fade a card by its distance from the centre. */
    private fun updateCarousel() {
        if (cardRefs.isEmpty()) return
        val center = carousel.scrollX + carousel.width / 2f
        for (ref in cardRefs) {
            if (ref.closing) continue
            val v = ref.card
            val w = v.width
            if (w <= 0) continue
            val dist = (v.left + w / 2f - center) / w
            val a = minOf(Math.abs(dist), 2f)
            v.translationY = dpf(a * 10f)
            v.rotationY = maxOf(-1f, minOf(1f, dist)) * -36f
            val sc = 1f - minOf(a, 1.5f) * 0.1f
            v.scaleX = sc
            v.scaleY = sc
            v.alpha = 1f - a * 0.22f
            ref.near = a < 1.6f
        }
    }

    /** Like scroll-snap-type: x mandatory, the nearest card ends up in the middle. */
    private fun snapCarousel() {
        val n = cardRefs.size
        if (n == 0 || cardW <= 0) return
        val step = cardW + cardGap
        val idx = Math.round(carousel.scrollX.toFloat() / step).coerceIn(0, n - 1)
        val target = idx * step
        if (Math.abs(carousel.scrollX - target) > 1) carousel.smoothScrollTo(target, 0)
    }

    private fun refreshCardBorders() {
        val p = pal()
        for (ref in cardRefs) {
            val cur = ref.tab === current
            ref.card.background = rounded(p.card, 24f, if (cur) p.accent else p.line, if (cur) 2 else 1)
        }
    }

    private fun buildTabCards() {
        val p = pal()
        tabsList.removeAllViews()
        cardRefs.clear()
        updateTabsTitle()
        val vw = if (content.width > 0) content.width else resources.displayMetrics.widthPixels
        val vh = if (content.height > 0) content.height else resources.displayMetrics.heightPixels
        cardW = dp(180)
        cardGap = dp(14)
        val ch = (vh - dp(54) - dp(82) - dp(28)).coerceIn(dp(200), dp(290))
        tabsList.setPadding(maxOf((vw - cardW) / 2, dp(8)), 0, maxOf((vw - cardW) / 2, dp(8)), 0)
        var curIdx = 0
        for ((i, tab) in tabs.withIndex()) {
            if (tab === current) curIdx = i
            val ref = makeCard(tab, p, ch)
            val lp = LinearLayout.LayoutParams(cardW, ch)
            lp.gravity = Gravity.CENTER_VERTICAL
            lp.rightMargin = if (i < tabs.size - 1) cardGap else 0
            tabsList.addView(ref.card, lp)
            cardRefs.add(ref)
        }
        val target = curIdx * (cardW + cardGap)
        carousel.scrollTo(0, 0)
        carousel.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                carousel.viewTreeObserver.removeOnGlobalLayoutListener(this)
                carousel.scrollTo(target, 0)
                updateCarousel()
            }
        })
    }

    private fun makeCard(tab: Tab, p: Pal, ch: Int): CardRef {
        val isCur = tab === current
        val name = when {
            tab.trusted -> "NOMAN GO"
            tab.title.isNotBlank() -> tab.title
            tab.url.isNotBlank() -> displayUrl(tab.url)
            else -> "New tab"
        }
        val sub = when {
            tab.incognito && tab.trusted -> "Incognito"
            tab.incognito -> "Incognito, " + displayUrl(tab.url)
            tab.trusted -> "Home"
            else -> displayUrl(tab.url)
        }
        val accent = when {
            tab.incognito -> c("#9A73FF")
            tab.trusted -> c("#FF2D3F")
            else -> tabColor(tab.url + name)
        }

        val root = FrameLayout(this)
        root.background = rounded(p.card, 24f, if (isCur) p.accent else p.line, if (isCur) 2 else 1)
        root.cameraDistance = dpf(900f)

        val body = LinearLayout(this)
        body.orientation = LinearLayout.VERTICAL
        body.setPadding(dp(14), dp(14), dp(14), dp(14))
        root.addView(body, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val hdr = LinearLayout(this)
        hdr.orientation = LinearLayout.HORIZONTAL
        hdr.gravity = Gravity.CENTER_VERTICAL
        hdr.setPadding(0, 0, dp(24), 0)
        val fv = TextView(this)
        fv.text = name.take(1).uppercase()
        fv.gravity = Gravity.CENTER
        fv.textSize = 13f
        fv.typeface = Typeface.DEFAULT_BOLD
        fv.setTextColor(accent)
        fv.background = rounded(mix(accent, p.card, 0.16f), 10f)
        hdr.addView(fv, LinearLayout.LayoutParams(dp(30), dp(30)))
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(8), 0, 0, 0)
        val tt = TextView(this)
        tt.text = name
        tt.setTextColor(p.text)
        tt.textSize = 14f
        tt.typeface = Typeface.DEFAULT_BOLD
        tt.maxLines = 1
        tt.ellipsize = TextUtils.TruncateAt.END
        col.addView(tt)
        val su = TextView(this)
        su.text = sub
        su.setTextColor(p.sub)
        su.textSize = 11f
        su.maxLines = 1
        su.ellipsize = TextUtils.TruncateAt.END
        col.addView(su)
        hdr.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        body.addView(hdr, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // preview area: the same skeleton lines and moving shine as the home page card
        val pv = FrameLayout(this)
        pv.background = rounded(mix(accent, p.bg, 0.09f), 16f)
        pv.clipToOutline = true
        val bars = LinearLayout(this)
        bars.orientation = LinearLayout.VERTICAL
        bars.setPadding(dp(12), dp(12), dp(12), dp(12))
        val inner = cardW - dp(28) - dp(24)
        val barColor = (accent and 0x00FFFFFF) or (0x4D shl 24)
        fun bar(frac: Float, hDp: Int, r: Float, top: Int) {
            val v = View(this)
            v.background = rounded(barColor, r)
            val w = if (frac >= 1f) ViewGroup.LayoutParams.MATCH_PARENT else (inner * frac).toInt()
            val lp = LinearLayout.LayoutParams(w, dp(hDp))
            lp.topMargin = top
            bars.addView(v, lp)
        }
        bar(0.6f, 14, 7f, 0)
        bar(1f, 8, 4f, dp(8))
        bar(0.8f, 8, 4f, dp(8))
        bar(1f, 8, 4f, dp(8))
        bar(0.45f, 8, 4f, dp(8))
        bar(1f, 46, 10f, dp(8))
        pv.addView(bars, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val shine = View(this)
        shine.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(0x00FFFFFF, 0x30FFFFFF, 0x00FFFFFF)
        )
        shine.translationX = -(cardW - dp(28)).toFloat()
        pv.addView(shine, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val plp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        plp.topMargin = dp(12)
        body.addView(pv, plp)

        val x = ImageButton(this)
        x.setImageResource(R.drawable.ic_close)
        x.setColorFilter(p.sub, PorterDuff.Mode.SRC_IN)
        x.background = circle(p.line)
        x.setPadding(dp(6), dp(6), dp(6), dp(6))
        x.scaleType = ImageView.ScaleType.FIT_CENTER
        x.contentDescription = "Close tab"
        val xlp = FrameLayout.LayoutParams(dp(26), dp(26))
        xlp.gravity = Gravity.TOP or Gravity.END
        xlp.topMargin = dp(12)
        xlp.rightMargin = dp(12)
        root.addView(x, xlp)

        val ref = CardRef(root, shine, tab)
        root.setOnClickListener { switchTo(tab, true) }
        x.setOnClickListener { closeCard(ref) }
        return ref
    }

    /** Same exit as the home page: the card lifts up, shrinks and fades, then the tab is really closed. */
    private fun closeCard(ref: CardRef) {
        if (ref.closing) return
        ref.closing = true
        val tab = ref.tab
        val card = ref.card
        val finish = Runnable {
            tabsList.removeView(card)
            cardRefs.remove(ref)
            val wasLast = tabs.size <= 1
            keepTabsOpen = true
            try {
                closeTab(tab)
            } finally {
                keepTabsOpen = false
            }
            if (wasLast) {
                hideTabs()
            } else {
                updateTabsTitle()
                refreshCardBorders()
                updateCarousel()
                snapCarousel()
            }
        }
        if (d(1L) == 0L) {
            finish.run()
            return
        }
        card.animate().alpha(0f).translationY(-dpf(60f)).scaleX(0.7f).scaleY(0.7f).setDuration(d(450))
            .setInterpolator(PathInterpolator(0.5f, 0f, 0.8f, 0.4f)).withEndAction(finish).start()
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
        ) { if (busy) t.web.stopLoading() else t.web.reload() }
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
        if (tabs.size > 1) closeTab(t) else finish()
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
            return handleUrl(uri)
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            t.url = url
            t.trusted = url.startsWith(ASSET)
            t.loading = true
            t.progress = 5
            t.docMime = ""
            if (t.errorPending) {
                t.errorPending = false
                t.errorShown = true
            } else {
                t.errorShown = false
            }
            view.settings.textZoom = if (t.trusted) 100 else zoomPct()
            if (t === current) {
                setLoading(!t.trusted)
                if (!t.trusted) setProgress(5)
                updateUrlUi()
                updateNav()
            }
        }

        override fun onPageCommitVisible(view: WebView, url: String) {
            if (t.desktop && !t.trusted) view.evaluateJavascript(DESKTOP_JS, null)
            if (!t.trusted) view.evaluateJavascript(HOOK_JS, null)
        }

        override fun onPageFinished(view: WebView, url: String) {
            t.loading = false
            t.progress = 100
            t.url = url
            t.title = view.title ?: ""
            if (t.desktop && !t.trusted) view.evaluateJavascript(DESKTOP_JS, null)
            if (!t.trusted) {
                view.evaluateJavascript(HOOK_JS, null)
                view.evaluateJavascript("document.contentType") { v -> t.docMime = unquoteJs(v).lowercase() }
            }
            if (!t.trusted && !t.errorShown && !t.incognito && url.startsWith("http")) {
                store.set(Store.K_LAST, url)
                if (stB("hist", true)) store.addHistory(t.title, url)
            }
            if (!t.incognito) scheduleSessionSave()
            if (t === current) {
                setLoading(false)
                setProgress(100)
                updateUrlUi()
                updateNav()
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

        override fun onProgressChanged(view: WebView, newProgress: Int) {
            t.progress = newProgress
            if (t === current && !t.trusted && t.loading) setProgress(newProgress)
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
            var mime = r.mime
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
                )
                continue
            }
            var status = "missing"
            var pct = 0
            var size = 0L
            val cur = dm.query(DownloadManager.Query().setFilterById(id))
            if (cur != null) {
                if (cur.moveToFirst()) {
                    val s = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val total = cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val soFar = cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
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
