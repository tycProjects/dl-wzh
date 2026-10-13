package com.flipper.app

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
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * MODUL 5: bubble beneran yang mengambang DI ATAS aplikasi lain (termasuk saat
 * sedang main game), pakai izin SYSTEM_ALERT_WINDOW -- izin yang sama dipakai
 * fitur "chat head" Messenger atau tombol melayang aplikasi perekam layar.
 *
 * Tap bubble -> buka/tutup panel kecil berisi pencarian & penulisan nilai
 * (modul 2+3), semuanya native (bukan WebView) supaya ringan dan bisa
 * digambar di atas aplikasi lain.
 *
 * WAJIB: HP sudah di-root (MemEngine butuh itu) DAN pengguna sudah memberi
 * izin "Tampil di atas aplikasi lain" untuk aplikasi ini.
 */
class OverlayService : Service() {

    companion object {
        const val EXTRA_PKG = "pkg"
        const val EXTRA_NAME = "name"
        private const val CHANNEL_ID = "flipper_overlay"
        private const val NOTIF_ID = 42
    }

    private lateinit var wm: WindowManager
    private val main = Handler(Looper.getMainLooper())
    private val bg: ExecutorService = Executors.newSingleThreadExecutor()
    private var engine: MemBackend? = null
    private var attachedPid: Int = -1
    private var usingRoot = false
    private var targetPkg: String = ""
    private var targetName: String = ""

    private var bubbleView: FrameLayout? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var panelView: LinearLayout? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var panelVisible = false

    private lateinit var statusText: TextView
    private lateinit var countText: TextView
    private lateinit var valueInput: EditText
    private lateinit var typeToggle: Button
    private lateinit var resultsBox: LinearLayout
    private lateinit var filterRow: LinearLayout
    private lateinit var writeAllBtn: Button

    private var isFloat = false
    private var hasScanned = false
    private var selectedAddr: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        targetPkg = intent?.getStringExtra(EXTRA_PKG) ?: targetPkg
        targetName = intent?.getStringExtra(EXTRA_NAME) ?: targetPkg

        startForeground(NOTIF_ID, buildNotification(), foregroundType())

        if (bubbleView == null) {
            buildBubble()
            buildPanel()
        }
        if (engine == null) {
            beginEngineSession()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        bg.execute {
            try {
                (engine as? MemEngine)?.detach() // hanya relevan di mode root; mode virtual cukup close()
            } catch (_: Exception) {
            }
            engine?.close()
        }
        bg.shutdown()
        try {
            bubbleView?.let { wm.removeView(it) }
        } catch (_: Exception) {
        }
        try {
            panelView?.let { wm.removeView(it) }
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    // ------------------------------------------------- notifikasi (wajib untuk foreground service)

    private fun foregroundType(): Int =
        if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= 26) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Flipper - Edit Koin", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
        val stopIntent = Intent(this, OverlayService::class.java).setAction("STOP")
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Flipper - Edit Koin aktif")
            .setContentText("Target: $targetName. Ketuk bubble oranye di layar untuk membuka.")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Hentikan", stopPending)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // biarkan tetap jalan walau app launcher di-swipe; berhenti lewat tombol notifikasi/panel
        super.onTaskRemoved(rootIntent)
    }

    // ------------------------------------------------- koneksi ke mesin memori

    private fun beginEngineSession() {
        setStatus("Menyiapkan mesin\u2026")
        bg.execute {
            if (tryRootMode()) return@execute
            if (trySameUidMode()) return@execute
            if (tryVirtualMode()) return@execute
            setStatus(
                "Tidak bisa tersambung ke \"$targetName\". Perlu salah satu: (1) HP di-root, " +
                    "(2) Flipper DAN game sama-sama di-clone ke app cloner yang sama (BlackBox / " +
                    "Parallel Space), atau (3) Flipper diaktifkan sebagai modul di VirtualXposed " +
                    "untuk game ini. Lihat README. Ketuk bubble untuk coba lagi."
            )
        }
    }

    /**
     * MODE SAMA-UID (tanpa root, tanpa Xposed): untuk saat Flipper dan game
     * sama-sama di-clone ke dalam app cloner yang sama (BlackBox, Parallel
     * Space, dkk) -- mekanisme yang sama dipakai GameGuardian untuk mode
     * "tanpa root"-nya. Hanya berhasil kalau Flipper yang sedang jalan ini
     * memang instance yang di-clone itu (dibuka dari dalam cloner-nya),
     * bukan Flipper biasa di luar.
     */
    private fun trySameUidMode(): Boolean {
        setStatus("Root tidak ada. Mencoba mode sama-UID (BlackBox / Parallel Space)\u2026")
        val eng = MemEngine(applicationContext)
        if (!eng.prepare() || !eng.startSameUid()) {
            eng.stop()
            return false
        }
        usingRoot = false
        engine = eng
        setStatus("Mode sama-UID aktif. Mencari proses \"$targetName\"\u2026")
        var pid = -1
        var tries = 0
        while (pid < 0 && tries < 16) { // ~8 detik
            val procs = eng.listProcesses()
            val hit = procs.firstOrNull { (_, name) -> name == targetPkg || name.endsWith("/$targetPkg") }
            if (hit != null) pid = hit.first else Thread.sleep(500)
            tries++
        }
        if (pid < 0) {
            setStatus("Mode sama-UID aktif tapi proses \"$targetName\" belum terdeteksi. Pastikan game juga di-clone & dibuka di cloner yang sama, lalu ketuk bubble lagi.")
            return true
        }
        if (!eng.attach(pid)) {
            setStatus(
                "PID $pid ditemukan tapi gagal attach (kemungkinan Flipper & game TIDAK ada di cloner " +
                    "yang sama, atau UID-nya ternyata beda). Coba jalur VirtualXposed sebagai gantinya."
            )
            return false // izinkan fallback ke tryVirtualMode()
        }
        attachedPid = pid
        setStatus("Siap (mode sama-UID, tanpa root). $targetName (PID $pid). Masukkan nilai sekarang lalu tekan Cari.")
        return true
    }

    /** Coba jalur ROOT (su + mem_engine.c). True kalau prosesnya berhasil dimulai (walau proses game belum ketemu). */
    private fun tryRootMode(): Boolean {
        setStatus("Mencoba akses root\u2026")
        val eng = MemEngine(applicationContext)
        if (!eng.prepare() || !eng.start()) {
            eng.stop()
            return false
        }
        usingRoot = true
        engine = eng
        setStatus("Root aktif. Mencari proses \"$targetName\"\u2026")
        var pid = -1
        var tries = 0
        while (pid < 0 && tries < 20) { // ~10 detik
            val procs = eng.listProcesses()
            val hit = procs.firstOrNull { (_, name) -> name == targetPkg || name.endsWith("/$targetPkg") }
            if (hit != null) pid = hit.first else Thread.sleep(500)
            tries++
        }
        if (pid < 0) {
            setStatus("Root aktif tapi proses \"$targetName\" belum terdeteksi. Buka game-nya, lalu ketuk bubble untuk coba lagi.")
            return true // jalur root tetap dipakai; jangan fallback ke virtual
        }
        if (!eng.attach(pid)) {
            setStatus("Root aktif, PID $pid ditemukan, tapi attach gagal. Lihat Logcat tag MemEngine.")
            return true
        }
        attachedPid = pid
        setStatus("Siap (mode root). $targetName (PID $pid). Masukkan nilai sekarang lalu tekan Cari.")
        return true
    }

    /**
     * Coba jalur TANPA ROOT (VirtualXposed). Hanya berhasil kalau Flipper
     * sudah diaktifkan sebagai modul Xposed untuk [targetPkg] di dalam
     * VirtualXposed, dan game itu sudah/sedang berjalan di sana.
     */
    private fun tryVirtualMode(): Boolean {
        setStatus("Root tidak tersedia. Mencoba lewat VirtualXposed\u2026")
        val sock = MemEngineSocket(targetPkg)
        var connected = false
        var tries = 0
        while (!connected && tries < 16) { // ~24 detik, kasih waktu game+hook siap
            connected = sock.connect()
            if (!connected) Thread.sleep(1500)
            tries++
        }
        if (!connected) {
            setStatus(
                "Belum nyambung ke VirtualXposed untuk \"$targetName\". Pastikan: (1) game ini dibuka DI " +
                    "DALAM VirtualXposed (bukan langsung dari launcher biasa), dan (2) modul Flipper sudah " +
                    "diaktifkan untuk game ini di menu VirtualXposed. Lihat README bagian \"Jalan Tanpa Root\"."
            )
            return false
        }
        usingRoot = false
        engine = sock
        attachedPid = 0 // tidak relevan di mode virtual (selalu "diri sendiri")
        setStatus("Siap (mode VirtualXposed, tanpa root). $targetName. Masukkan nilai sekarang lalu tekan Cari.")
        return true
    }

    private fun retryAttachIfNeeded() {
        if (attachedPid >= 0) return
        beginEngineSession()
    }

    // ------------------------------------------------- bubble (ikon melayang)

    private fun dp(v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun buildBubble() {
        val size = dp(56)
        val bg2 = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#FF8200"))
        }
        val label = TextView(this).apply {
            text = "\u20A2" // simbol koin generik
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
        }
        val frame = FrameLayout(this).apply {
            background = bg2
            addView(label, FrameLayout.LayoutParams(size, size))
            elevation = dp(6).toFloat()
        }
        bubbleView = frame

        val params = WindowManager.LayoutParams(
            size, size,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(160)
        }
        bubbleParams = params

        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var moved = false
        frame.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX; downY = ev.rawY
                    startX = params.x; startY = params.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (ev.rawX - downX).toInt()
                    val dy = (ev.rawY - downY).toInt()
                    if (kotlin.math.abs(dx) + kotlin.math.abs(dy) > dp(4)) moved = true
                    params.x = startX + dx
                    params.y = startY + dy
                    try {
                        wm.updateViewLayout(frame, params)
                    } catch (_: Exception) {
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) togglePanel()
                    true
                }
                else -> false
            }
        }
        try {
            wm.addView(frame, params)
        } catch (e: Exception) {
            // izin overlay belum diberikan, atau WM menolak -- service tetap jalan,
            // tapi bubble tidak tampil. MainActivity sudah mengurus alur minta izin
            // sebelum menyalakan service ini, jadi ini seharusnya jarang terjadi.
        }
    }

    private fun togglePanel() {
        panelVisible = !panelVisible
        panelView?.visibility = if (panelVisible) View.VISIBLE else View.GONE
        if (panelVisible) retryAttachIfNeeded()
    }

    // ------------------------------------------------- panel (scan + tulis)

    private fun styledButton(text: String, onClick: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            textSize = 12f
            setPadding(dp(8), dp(4), dp(8), dp(4))
            minWidth = 0
            minimumWidth = 0
            setOnClickListener { onClick() }
        }

    private fun buildPanel() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F5FFFFFF"))
                cornerRadius = dp(14).toFloat()
            }
            visibility = View.GONE
        }

        val title = TextView(this).apply {
            text = "Edit Koin"
            setTextColor(Color.BLACK)
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        statusText = TextView(this).apply {
            text = "Menyiapkan\u2026"
            setTextColor(Color.DKGRAY)
            textSize = 11.5f
            setPadding(0, dp(4), 0, dp(8))
        }

        val inputRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        valueInput = EditText(this).apply {
            hint = "nilai sekarang"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            setTextColor(Color.BLACK)
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        typeToggle = styledButton("I32") { onToggleType() }
        inputRow.addView(valueInput)
        inputRow.addView(typeToggle)

        val searchBtn = styledButton("Cari (Scan Baru)") { onScanNew() }
        countText = TextView(this).apply {
            text = ""
            setTextColor(Color.DKGRAY)
            textSize = 11.5f
            setPadding(0, dp(6), 0, dp(4))
        }

        filterRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
            addView(styledButton("Berubah") { onFilter("changed") })
            addView(styledButton("Tetap") { onFilter("unchanged") })
            addView(styledButton("Naik") { onFilter("inc") })
            addView(styledButton("Turun") { onFilter("dec") })
        }

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(140))
        }
        resultsBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(resultsBox)

        writeAllBtn = styledButton("Tulis Nilai ke SEMUA Hasil") { onWriteAll() }.apply {
            visibility = View.GONE
        }
        val resetBtn = styledButton("Scan Ulang / Reset") { onReset() }
        val closeBtn = styledButton("Tutup Panel") { togglePanel() }
        val stopBtn = styledButton("Hentikan Overlay") { stopSelf() }

        val bottomRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bottomRow.addView(closeBtn)
        bottomRow.addView(stopBtn)

        root.addView(title)
        root.addView(statusText)
        root.addView(inputRow)
        root.addView(searchBtn)
        root.addView(countText)
        root.addView(filterRow)
        root.addView(scroll)
        root.addView(writeAllBtn)
        root.addView(resetBtn)
        root.addView(bottomRow)

        panelView = root
        val params = WindowManager.LayoutParams(
            dp(280), WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(226)
        }
        panelParams = params
        try {
            wm.addView(root, params)
        } catch (_: Exception) {
        }
    }

    private fun onToggleType() {
        isFloat = !isFloat
        typeToggle.text = if (isFloat) "F32" else "I32"
    }

    private fun currentType(): String = if (isFloat) "f32" else "i32"

    private fun onScanNew() {
        val v = valueInput.text.toString().trim()
        if (v.isEmpty()) {
            setStatus("Isi dulu nilai koin yang sekarang terlihat di game.")
            return
        }
        setStatus("Memindai seluruh memori\u2026 (beberapa detik)")
        bg.execute {
            val n = engine?.scanNew(currentType(), v)
            main.post {
                if (n == null) {
                    setStatus("Scan gagal. Pastikan sudah attach ke proses (lihat status di atas).")
                } else {
                    hasScanned = true
                    filterRow.visibility = View.VISIBLE
                    writeAllBtn.visibility = View.VISIBLE
                    countText.text = "$n kandidat ditemukan"
                    setStatus("Ubah nilai koin di dalam game, lalu tekan salah satu tombol di bawah.")
                    refreshResults()
                }
            }
        }
    }

    private fun onFilter(op: String) {
        if (!hasScanned) return
        setStatus("Menyaring\u2026")
        bg.execute {
            val n = engine?.scanFilter(currentType(), op)
            main.post {
                if (n == null) {
                    setStatus("Gagal menyaring.")
                } else {
                    countText.text = "$n kandidat tersisa"
                    setStatus(
                        if (n in 1..20) "Tinggal $n -- cukup kecil untuk ditulis langsung di bawah."
                        else "Masih $n. Ubah nilai koin lagi lalu saring lagi (Naik/Turun/Berubah)."
                    )
                    refreshResults()
                }
            }
        }
    }

    private fun refreshResults() {
        bg.execute {
            val rows = engine?.scanPage(0, 25) ?: emptyList()
            main.post {
                resultsBox.removeAllViews()
                for (row in rows) {
                    val b = Button(this).apply {
                        text = "0x${row.addrHex} = ${row.value}"
                        textSize = 10.5f
                        isAllCaps = false
                        setPadding(dp(4), dp(2), dp(4), dp(2))
                        setOnClickListener {
                            selectedAddr = row.addrHex
                            valueInput.setText(row.value)
                            setStatus("Alamat 0x${row.addrHex} dipilih. Ubah nilai di kotak atas lalu tekan \"Tulis\" di bawahnya, atau tulis ke semua hasil.")
                        }
                    }
                    resultsBox.addView(b)
                }
            }
        }
    }

    private fun onWriteAll() {
        val v = valueInput.text.toString().trim()
        if (v.isEmpty()) {
            setStatus("Isi dulu nilai baru yang mau ditulis.")
            return
        }
        setStatus("Menulis ke semua hasil\u2026")
        bg.execute {
            val total = engine?.scanCount() ?: 0
            var ok = 0
            var offset = 0
            val pageSize = 200
            while (offset < total) {
                val rows = engine?.scanPage(offset, pageSize) ?: emptyList()
                if (rows.isEmpty()) break
                for (row in rows) {
                    if (engine?.writeValue(row.addrHex, currentType(), v) == true) ok++
                }
                offset += pageSize
            }
            main.post { setStatus("Selesai: $ok dari $total alamat berhasil ditulis nilai $v.") }
        }
    }

    private fun onReset() {
        bg.execute { engine?.scanReset() }
        hasScanned = false
        filterRow.visibility = View.GONE
        writeAllBtn.visibility = View.GONE
        countText.text = ""
        resultsBox.removeAllViews()
        selectedAddr = null
        setStatus(if (attachedPid >= 0) "Siap. Masukkan nilai sekarang lalu tekan Cari." else "Belum tersambung ke proses.")
    }

    private fun setStatus(s: String) {
        main.post { if (::statusText.isInitialized) statusText.text = s }
    }
}
