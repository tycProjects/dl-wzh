package com.generated.aiaddi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.Executors
import kotlin.math.abs

class FloatingService : Service() {

    private lateinit var wm: WindowManager
    private val ui = Handler(Looper.getMainLooper())
    private val bg = Executors.newSingleThreadExecutor()

    private var bubble: TextView? = null
    private var panel: View? = null
    private var card: View? = null
    private var cardText: TextView? = null
    private var sw = 0
    private var sh = 0
    private var dens = 1f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        dens = resources.displayMetrics.density
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        sw = dm.widthPixels
        sh = dm.heightPixels
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
            ACTION_READ -> { if (running) ui.postDelayed({ startRead(false) }, 700); return START_NOT_STICKY }
            ACTION_ESSAY -> { if (running) ui.postDelayed({ startRead(true) }, 700); return START_NOT_STICKY }
        }
        startAsForeground()
        if (bubble == null) addBubble()
        running = true
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val ch = "floating"
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(ch, "Tombol melayang", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(
            this, 1, Intent(this, FloatingService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(this, ch)
            .setContentTitle("Addi AI aktif")
            .setContentText("Ketuk tombol robot untuk menjawab layar")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(open)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel), "Matikan", stop
                ).build()
            )
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    // ---------- baca teks layar -> AI ----------

    private fun startRead(essay: Boolean) {
        hidePanel()
        hideCard()
        val prefs = getSharedPreferences("ai", MODE_PRIVATE)
        val key = prefs.getString("key", "") ?: ""
        val provider = prefs.getString("provider", "1") ?: "1"
        if (key.isBlank()) {
            showCard("Isi dulu provider dan API key di Addi AI > Pengaturan AI.")
            return
        }
        val acc = AddiAccessibilityService.instance
        if (acc == null) {
            showCard("Aksesibilitas Addi AI mati. Aktifkan lagi di Pengaturan > Aksesibilitas > Addi AI.")
            return
        }
        val text = acc.readScreenText()
        if (text.isBlank()) {
            showCard("Tidak ada teks yang bisa dibaca dari layar ini. Buka halaman yang berisi teks, lalu ketuk lagi.")
            return
        }
        showCard("Menganalisis dengan AI...")
        askAi(provider, key, text, essay)
    }

    private fun askAi(provider: String, key: String, screenText: String, essay: Boolean) {
        val task = if (essay)
            "Teks berikut adalah pertanyaan esai dari layar. Jawab dengan lengkap, jelas, dan terstruktur."
        else
            "Teks berikut diambil dari layar. Jika ada soal pilihan ganda, jawab dengan huruf " +
                "pilihan yang benar di baris pertama (contoh: \"Jawaban: B\"), lalu beri penjelasan singkat. " +
                "Jika bukan soal, jelaskan atau ringkas isinya."
        val prompt = "$task Jawab dalam Bahasa Indonesia.\n\nTEKS LAYAR:\n$screenText"
        bg.execute {
            val reply = try { AiClient.ask(provider, key, prompt) } catch (e: Exception) { "Gagal: ${e.message}" }
            ui.post { lastResult = reply; showCard(reply) }
        }
    }

    // ---------- tampilan overlay ----------

    private fun dp(v: Int) = (v * dens).toInt()

    private fun round(color: Int, r: Int, stroke: Int = 0) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(r).toFloat()
        if (stroke != 0) setStroke(dp(1), stroke)
    }

    private fun overlayParams(w: Int, h: Int, flags: Int) = WindowManager.LayoutParams(
        w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or flags, PixelFormat.TRANSLUCENT
    )

    private fun chip(text: String, onClick: () -> Unit) = TextView(this).apply {
        this.text = text; textSize = 16f; setTextColor(Color.WHITE)
        setPadding(dp(16), dp(12), dp(16), dp(12))
        background = round(0xFF123B38.toInt(), 14)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
        setOnClickListener { onClick() }
    }

    private fun addBubble() {
        val size = dp(56)
        val v = TextView(this)
        v.text = "\uD83E\uDD16"; v.textSize = 26f; v.gravity = Gravity.CENTER; v.alpha = 0.92f
        v.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL; setColor(0xFF123B38.toInt()); setStroke(dp(2), 0xFF59E0C3.toInt())
        }
        val lp = overlayParams(size, size, 0)
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = dp(8); lp.y = dp(220)

        var sx = 0; var sy = 0; var tx = 0f; var ty = 0f
        var moved = false; var longDone = false
        val longPress = Runnable { longDone = true; togglePanel() }

        // ketuk = langsung jawab layar; tekan lama = menu; geser = pindahkan
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    sx = lp.x; sy = lp.y; tx = e.rawX; ty = e.rawY
                    moved = false; longDone = false
                    ui.postDelayed(longPress, 500)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - tx; val dy = e.rawY - ty
                    if (abs(dx) > dp(8) || abs(dy) > dp(8)) { moved = true; ui.removeCallbacks(longPress) }
                    if (moved) { lp.x = sx + dx.toInt(); lp.y = sy + dy.toInt(); wm.updateViewLayout(v, lp) }
                }
                MotionEvent.ACTION_UP -> {
                    ui.removeCallbacks(longPress)
                    if (longDone) {
                        // menu sudah dibuka
                    } else if (!moved) {
                        startRead(false)
                    } else {
                        lp.x = if (lp.x + size / 2 < sw / 2) 0 else sw - size
                        wm.updateViewLayout(v, lp)
                    }
                }
            }
            true
        }
        wm.addView(v, lp)
        bubble = v
    }

    private fun togglePanel() { if (panel != null) hidePanel() else showPanel() }

    private fun showPanel() {
        hideCard()
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(14), dp(14), dp(14), dp(14))
        box.background = round(0xF2071313.toInt(), 22, 0xFF173737.toInt())
        val title = TextView(this)
        title.text = "ADDI AI ASSIST"; title.textSize = 12f; title.setTextColor(0xFF59E0C3.toInt())
        box.addView(title)
        box.addView(chip("\uD83D\uDC41\uFE0F  Jawab Layar Ini") { startRead(false) })
        box.addView(chip("\uD83D\uDCDD  Mode Esai") { startRead(true) })
        box.addView(chip("\u2699\uFE0F  Pengaturan AI") { hidePanel(); openApp(true) })
        box.addView(chip("\uD83C\uDFE0  Buka Aplikasi") { hidePanel(); openApp() })
        box.addView(chip("\u274C  Matikan Tombol") { stopSelf() })
        val lp = overlayParams(dp(260), -2, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
        lp.gravity = Gravity.CENTER
        box.setOnTouchListener { _, e -> if (e.action == MotionEvent.ACTION_OUTSIDE) hidePanel(); false }
        wm.addView(box, lp)
        panel = box
    }

    private fun hidePanel() { panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }; panel = null }

    private fun showCard(text: String) {
        if (card == null) {
            val box = LinearLayout(this)
            box.orientation = LinearLayout.VERTICAL
            box.setPadding(dp(16), dp(14), dp(16), dp(14))
            box.background = round(0xF2071313.toInt(), 22, 0xFF173737.toInt())
            val title = TextView(this)
            title.text = "HASIL ADDI AI"; title.textSize = 12f; title.setTextColor(0xFF59E0C3.toInt())
            val t = TextView(this)
            t.textSize = 16f; t.setTextColor(Color.WHITE); t.setPadding(0, dp(8), 0, dp(8))
            val sc = ScrollView(this)
            sc.addView(t)
            box.addView(title)
            box.addView(sc, LinearLayout.LayoutParams(-1, (sh * 0.35).toInt()))
            box.addView(chip("Tutup") { hideCard() })
            val lp = overlayParams((sw * 0.92).toInt(), -2, 0)
            lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            lp.y = dp(48)
            wm.addView(box, lp)
            card = box
            cardText = t
        }
        cardText?.text = text
    }

    private fun hideCard() { card?.let { try { wm.removeView(it) } catch (_: Exception) {} }; card = null; cardText = null }

    private fun openApp(settings: Boolean = false) {
        val i = Intent(this, MainActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (settings) i.putExtra("open_settings", true)
        startActivity(i)
    }

    override fun onDestroy() {
        running = false
        hidePanel(); hideCard()
        bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        bubble = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.generated.aiaddi.STOP"
        const val ACTION_READ = "com.generated.aiaddi.READ"
        const val ACTION_ESSAY = "com.generated.aiaddi.ESSAY"
        @Volatile var running = false
        @Volatile var lastResult: String? = null
    }
}
