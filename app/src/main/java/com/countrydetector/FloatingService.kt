package com.countrydetector

import android.app.*
import android.content.*
import android.graphics.*
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.view.*
import android.view.WindowManager.LayoutParams.*
import android.widget.*
import androidx.core.app.NotificationCompat
import android.content.ClipboardManager
import android.content.ClipData

// ─── Country database (lightweight) ──────────────────────────────────────────
data class Country(val name: String, val code: String, val flag: String, val keywords: List<String>)

val COUNTRY_DB = listOf(
    Country("Malaysia",       "MY", "🇲🇾", listOf("malaysia","kuala lumpur","ringgit","petronas","kl","myr")),
    Country("Indonesia",      "ID", "🇮🇩", listOf("indonesia","jakarta","rupiah","idr","garuda","bali")),
    Country("Singapore",      "SG", "🇸🇬", listOf("singapore","singapura","sgd","merlion","changi")),
    Country("Thailand",       "TH", "🇹🇭", listOf("thailand","bangkok","thai baht","thb","phuket")),
    Country("Philippines",    "PH", "🇵🇭", listOf("philippines","manila","piso","php","pilipinas")),
    Country("Vietnam",        "VN", "🇻🇳", listOf("vietnam","hanoi","dong","vnd","ho chi minh","saigon")),
    Country("Japan",          "JP", "🇯🇵", listOf("japan","tokyo","yen","jpy","osaka","nippon")),
    Country("South Korea",    "KR", "🇰🇷", listOf("korea","seoul","won","krw","hangul","busan")),
    Country("China",          "CN", "🇨🇳", listOf("china","beijing","yuan","cny","rmb","shanghai")),
    Country("United States",  "US", "🇺🇸", listOf("united states","usa","america","dollar","usd","washington")),
    Country("United Kingdom", "GB", "🇬🇧", listOf("united kingdom","uk","england","london","pound","gbp")),
    Country("Australia",      "AU", "🇦🇺", listOf("australia","sydney","aud","melbourne","brisbane")),
    Country("India",          "IN", "🇮🇳", listOf("india","delhi","rupee","inr","mumbai","hindi")),
    Country("Germany",        "DE", "🇩🇪", listOf("germany","berlin","euro","eur","deutschland","frankfurt")),
    Country("France",         "FR", "🇫🇷", listOf("france","paris","euro","eur","lyon","marseille")),
    Country("Canada",         "CA", "🇨🇦", listOf("canada","toronto","cad","ottawa","vancouver","montreal")),
    Country("Saudi Arabia",   "SA", "🇸🇦", listOf("saudi","riyadh","riyal","sar","mecca","jeddah")),
    Country("UAE",            "AE", "🇦🇪", listOf("uae","dubai","dirham","aed","abu dhabi","emirates")),
    Country("Turkey",         "TR", "🇹🇷", listOf("turkey","istanbul","lira","try","ankara","turkiye")),
    Country("Brazil",         "BR", "🇧🇷", listOf("brazil","sao paulo","real","brl","rio","brasilia")),
    Country("Pakistan",       "PK", "🇵🇰", listOf("pakistan","islamabad","rupee","pkr","karachi","lahore")),
    Country("Bangladesh",     "BD", "🇧🇩", listOf("bangladesh","dhaka","taka","bdt","chittagong")),
    Country("Sri Lanka",      "LK", "🇱🇰", listOf("sri lanka","colombo","rupee","lkr","ceylon")),
    Country("Myanmar",        "MM", "🇲🇲", listOf("myanmar","yangon","kyat","mmk","burma","naypyidaw")),
    Country("Cambodia",       "KH", "🇰🇭", listOf("cambodia","phnom penh","riel","khr","angkor")),
    Country("Brunei",         "BN", "🇧🇳", listOf("brunei","bandar seri","bnd","borneo")),
    Country("Laos",           "LA", "🇱🇦", listOf("laos","vientiane","kip","lak","luang prabang")),
    Country("Taiwan",         "TW", "🇹🇼", listOf("taiwan","taipei","twd","new taiwan")),
    Country("Hong Kong",      "HK", "🇭🇰", listOf("hong kong","hkd","kowloon","cantonese")),
    Country("New Zealand",    "NZ", "🇳🇿", listOf("new zealand","wellington","nzd","auckland","kiwi")),
    Country("South Africa",   "ZA", "🇿🇦", listOf("south africa","johannesburg","rand","zar","cape town")),
    Country("Nigeria",        "NG", "🇳🇬", listOf("nigeria","lagos","naira","ngn","abuja","yoruba")),
    Country("Egypt",          "EG", "🇪🇬", listOf("egypt","cairo","pound","egp","nile","pharaoh")),
    Country("Morocco",        "MA", "🇲🇦", listOf("morocco","casablanca","dirham","mad","marrakech")),
    Country("Netherlands",    "NL", "🇳🇱", listOf("netherlands","amsterdam","euro","eur","dutch","holland")),
    Country("Spain",          "ES", "🇪🇸", listOf("spain","madrid","euro","eur","barcelona","espana")),
    Country("Italy",          "IT", "🇮🇹", listOf("italy","rome","euro","eur","milan","roma","italia")),
    Country("Russia",         "RU", "🇷🇺", listOf("russia","moscow","ruble","rub","kremlin")),
    Country("Iran",           "IR", "🇮🇷", listOf("iran","tehran","rial","irr","persian")),
    Country("Argentina",      "AR", "🇦🇷", listOf("argentina","buenos aires","peso","ars","mendoza")),
)

fun detectCountries(text: String): List<Pair<Country, Int>> {
    val lower = text.lowercase()
    return COUNTRY_DB.mapNotNull { c ->
        var score = 0
        for (kw in c.keywords) { if (lower.contains(kw)) score += kw.length }
        if (score > 0) c to minOf(97, 52 + score * 3) else null
    }.sortedByDescending { it.second }.take(5)
}

// ─────────────────────────────────────────────────────────────────────────────
class FloatingService : Service() {

    companion object {
        const val ACTION_SET_PROJECTION = "SET_PROJECTION"
        const val EXTRA_RESULT_CODE     = "resultCode"
        const val EXTRA_RESULT_DATA     = "resultData"
        const val CHANNEL_ID            = "country_detector_channel"
        const val NOTIF_ID              = 1
    }

    private lateinit var wm: WindowManager
    private var bubbleView: View? = null
    private var menuView:   View? = null

    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())

    private val lastResults  = mutableListOf<Pair<Country, Int>>()
    private val historyList  = mutableListOf<Country>()

    // ── Lifecycle ────────────────────────────────────────────────
    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        showBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SET_PROJECTION) {
            val code  = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
            val data  = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                        else
                            @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_RESULT_DATA)
            if (data != null) {
                val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjection = mgr.getMediaProjection(code, data)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        removeBubble()
        removeMenu()
        mediaProjection?.stop()
        imageReader?.close()
    }

    // ── Notification (required for foreground service) ───────────
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Country Detector", NotificationManager.IMPORTANCE_LOW)
            ch.description = "Floating bubble is active"
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0, Intent(this, FloatingService::class.java).apply { action = "STOP" },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Country Detector active")
            .setContentText("Tap bubble on screen to detect country")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .build()
    }

    // ── Floating BUBBLE ──────────────────────────────────────────
    private fun showBubble() {
        val params = WindowManager.LayoutParams(
            140, 140,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                TYPE_APPLICATION_OVERLAY else TYPE_PHONE,
            FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity    = Gravity.TOP or Gravity.START
            x = 80; y = 320
        }

        val bubble = LayoutInflater.from(this).inflate(R.layout.bubble_view, null)
        bubbleView = bubble

        var initX = 0; var initY = 0; var touchX = 0f; var touchY = 0f; var moved = false

        bubble.setOnTouchListener { v, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = params.x; initY = params.y
                    touchX = ev.rawX; touchY = ev.rawY
                    moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (ev.rawX - touchX).toInt()
                    val dy = (ev.rawY - touchY).toInt()
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) moved = true
                    params.x = initX + dx; params.y = initY + dy
                    wm.updateViewLayout(bubble, params)
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        vibrate()
                        toggleMenu(params.x + 150, params.y)
                    }
                }
            }
            true
        }

        wm.addView(bubble, params)
    }

    private fun removeBubble() {
        bubbleView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        bubbleView = null
    }

    // ── Tool MENU ────────────────────────────────────────────────
    private fun toggleMenu(anchorX: Int, anchorY: Int) {
        if (menuView != null) { removeMenu(); return }
        showMenu(anchorX, anchorY)
    }

    private fun showMenu(anchorX: Int, anchorY: Int) {
        val dm   = resources.displayMetrics
        val w    = (dm.widthPixels * 0.75f).toInt()

        val params = WindowManager.LayoutParams(
            w, WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                TYPE_APPLICATION_OVERLAY else TYPE_PHONE,
            FLAG_NOT_FOCUSABLE or FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = minOf(anchorX, dm.widthPixels - w - 16)
            y = minOf(anchorY, dm.heightPixels - 600)
        }

        val menu = LayoutInflater.from(this).inflate(R.layout.menu_view, null)
        menuView = menu

        // Close
        menu.findViewById<View>(R.id.btn_close).setOnClickListener { removeMenu() }

        // Detect
        menu.findViewById<View>(R.id.btn_detect).setOnClickListener {
            captureAndDetect(menu)
        }

        // Copy name
        menu.findViewById<View>(R.id.btn_copy_name).setOnClickListener {
            val top = lastResults.firstOrNull()?.first
            if (top != null) {
                copyToClipboard(top.name)
                showMenuToast(menu, "Copied: ${top.name}")
            } else {
                showMenuToast(menu, "Detect first")
            }
        }

        // Copy code
        menu.findViewById<View>(R.id.btn_copy_code).setOnClickListener {
            val top = lastResults.firstOrNull()?.first
            if (top != null) {
                copyToClipboard(top.code)
                showMenuToast(menu, "Copied: ${top.code}")
            } else {
                showMenuToast(menu, "Detect first")
            }
        }

        // Copy flag
        menu.findViewById<View>(R.id.btn_copy_flag).setOnClickListener {
            val top = lastResults.firstOrNull()?.first
            if (top != null) {
                copyToClipboard("${top.flag} ${top.name}")
                showMenuToast(menu, "Copied: ${top.flag} ${top.name}")
            } else {
                showMenuToast(menu, "Detect first")
            }
        }

        wm.addView(menu, params)
    }

    private fun removeMenu() {
        menuView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        menuView = null
    }

    // ── Detection ────────────────────────────────────────────────
    private fun captureAndDetect(menu: View) {
        val tvResult  = menu.findViewById<TextView>(R.id.tv_result)
        val tvStatus  = menu.findViewById<TextView>(R.id.tv_status)
        tvStatus.text = "🔍 Scanning…"
        tvResult.text = ""

        handler.postDelayed({
            if (mediaProjection == null) {
                tvStatus.text = "⚠ Screen capture is not active"
                tvResult.text = "Grant Screen capture again from the app."
                return@postDelayed
            }
            tvStatus.text = "📸 Screen capture ready"
            tvResult.text = "No country text detected yet."
        }, 300)
    }

    // ── Helpers ──────────────────────────────────────────────────
    private fun copyToClipboard(text: String) {
        val clip = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clip.setPrimaryClip(ClipData.newPlainText("country", text))
    }

    private fun showMenuToast(menu: View, msg: String) {
        val tv = menu.findViewById<TextView>(R.id.tv_status)
        tv.text = msg
    }

    private fun vibrate() {
        val vib = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") vib.vibrate(40)
        }
    }
}
