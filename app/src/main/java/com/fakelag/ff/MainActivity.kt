package com.fakelag.ff

import android.app.*
import android.content.*
import android.graphics.*
import android.net.Uri
import android.net.VpnService
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs
import java.io.*
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private val REQ_VPN = 1001
    private val REQ_OVERLAY = 1002
    private val REQ_STORAGE = 1003
    private val REQ_SHIZUKU = 1004

    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tv_status)

        findViewById<Button>(R.id.btn_enable).setOnClickListener { checkShizuku() }
        findViewById<Button>(R.id.btn_nhe_tam).setOnClickListener { caiNheTam() }
        findViewById<Button>(R.id.btn_aim_dau).setOnClickListener { caiAimDau() }
        findViewById<Button>(R.id.btn_menu_an).setOnClickListener { caiMenuAn() }
    }

    /* ========== CÀI NHẸ TÂM ========== */
    private fun caiNheTam() {
        tvStatus.text = "Đang cài Nhẹ Tâm..."
        thread {
            try {
                val targetDir = File(
                    Environment.getExternalStorageDirectory(),
                    "Android/data/com.dts.freefiremax/files"
                )
                if (!targetDir.exists()) targetDir.mkdirs()

                val files = listOf(
                    "NHETAM.dat",
                    "NHETAM2.dat",
                    "NHETAM3.dat.xml",
                    "FIXRUNG.dat.xml"
                )
                for (name in files) {
                    assets.open("nhe_tam/$name").use { input ->
                        File(targetDir, name).outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                runOnUiThread {
                    tvStatus.text = "Cài Nhẹ Tâm thành công!\n/sdcard/Android/data/com.dts.freefiremax/files/"
                    Toast.makeText(this, "Nhẹ Tâm đã cài 100%", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvStatus.text = "Lỗi cài Nhẹ Tâm: ${e.message}"
                    Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /* ========== CÀI AIM ĐẦU + ESP ========== */
    private fun caiAimDau() {
        tvStatus.text = "Đang cài Aim Đầu + ESP..."
        thread {
            try {
                val outDir = File(Environment.getExternalStorageDirectory(), "FFAimDau")
                if (!outDir.exists()) outDir.mkdirs()

                val files = listOf(
                    "Assembly-CSharp-patch.bytes",
                    "GameBand-Fix.json",
                    "localconfig.json",
                    "VIKAS BYPASS.json",
                    "_.kmteam-ffmax-state",
                    "README_TEN.txt"
                )
                for (name in files) {
                    assets.open("aim_dau/$name").use { input ->
                        File(outDir, name).outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                // Cũng copy vào data Free Fire Max nếu được
                try {
                    val ffDir = File(
                        Environment.getExternalStorageDirectory(),
                        "Android/data/com.dts.freefiremax/files"
                    )
                    if (!ffDir.exists()) ffDir.mkdirs()
                    for (name in files) {
                        if (name.endsWith(".txt")) continue
                        val src = File(outDir, name)
                        if (src.exists()) {
                            src.copyTo(File(ffDir, name), overwrite = true)
                        }
                    }
                } catch (_: Exception) {}

                runOnUiThread {
                    tvStatus.text = "Aim Đầu + ESP đã cài!\n/sdcard/FFAimDau\n+ data Free Fire Max"
                    Toast.makeText(this, "Aim Đầu + ESP cài xong", Toast.LENGTH_LONG).show()
                    moZArchiver(outDir)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvStatus.text = "Lỗi cài Aim: ${e.message}"
                    Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /* ========== CÀI MENU ẨN ========== */
    private fun caiMenuAn() {
        tvStatus.text = "Đang copy Menu Ẩn..."
        thread {
            try {
                val outDir = File(Environment.getExternalStorageDirectory(), "FFMenuAn")
                if (!outDir.exists()) outDir.mkdirs()

                val files = listOf(
                    "Assembly-CSharp-patch.bytes",
                    "localConfig.json",
                    "README.txt"
                )
                for (name in files) {
                    assets.open("menu_an/$name").use { input ->
                        File(outDir, name).outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                runOnUiThread {
                    tvStatus.text = "Đã copy vào /sdcard/FFMenuAn"
                    moZArchiver(outDir)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvStatus.text = "Lỗi: ${e.message}"
                }
            }
        }
    }

    private fun moZArchiver(dir: File) {
        val pkgs = listOf("ru.zdevs.zarchiver", "ru.zdevs.zarchiver.pro")
        for (pkg in pkgs) {
            try {
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    intent.putExtra("path", dir.absolutePath)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    return
                }
            } catch (_: Exception) {}
        }
    }

    private fun checkShizuku() {
        if (Shizuku.pingBinder()) {
            if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestVpn()
            } else {
                Shizuku.requestPermission(REQ_SHIZUKU)
            }
        } else {
            tvStatus.text = "Mở Shizuku và bấm Start trước"
            try {
                startActivity(packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api"))
            } catch (_: Exception) {}
        }
    }

    private fun requestVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) startActivityForResult(intent, REQ_VPN) else onVpnGranted()
    }

    private fun onVpnGranted() {
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                REQ_OVERLAY
            )
        } else requestStorage()
    }

    private fun requestStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                startActivityForResult(
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    },
                    REQ_STORAGE
                )
            } else startAll()
        } else startAll()
    }

    private fun startAll() {
        startService(Intent(this, LagVpnService::class.java))
        startService(Intent(this, OverlayService::class.java))
        MemoryEngine.bind(this)
        tvStatus.text = "Lag + Overlay đã bật (màu đen)"
        Toast.makeText(this, "Sẵn sàng - vào game lag 2 giây", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_VPN -> if (resultCode == Activity.RESULT_OK) onVpnGranted()
            REQ_OVERLAY -> if (Settings.canDrawOverlays(this)) requestStorage()
            REQ_STORAGE -> if (Environment.isExternalStorageManager()) startAll()
            REQ_SHIZUKU -> if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) requestVpn()
        }
    }
}

/* ===================== OVERLAY + SPIDER ===================== */

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: FrameLayout
    private lateinit var spider: SpiderView
    private var params: WindowManager.LayoutParams? = null
    private var size = 120
    private var lagOn = false

    private val handler = Handler(Looper.getMainLooper())
    private var introRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlay()

        // Hiện màu ĐEN, auto lag 2 giây rồi về ĐỎ (tắt)
        spider.setColorMode(SpiderView.MODE_BLACK)
        startIntroLag()
    }

    private fun startIntroLag() {
        setLag(true)
        MemoryEngine.activateLagKill(true)

        introRunnable = Runnable {
            setLag(false)
            MemoryEngine.activateLagKill(false)
            lagOn = false
            spider.setColorMode(SpiderView.MODE_RED)   // đỏ = tắt
        }
        handler.postDelayed(introRunnable!!, 2000)
    }

    private fun createOverlay() {
        overlayView = FrameLayout(this)
        spider = SpiderView(this)
        overlayView.addView(spider, FrameLayout.LayoutParams(size, size))

        params = WindowManager.LayoutParams(
            size, size,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }
        windowManager.addView(overlayView, params)

        var ix = 0; var iy = 0; var itx = 0f; var ity = 0f; var isz = size
        spider.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    ix = params!!.x; iy = params!!.y
                    itx = e.rawX; ity = e.rawY; isz = size
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (e.pointerCount == 1) {
                        params!!.x = ix + (e.rawX - itx).toInt()
                        params!!.y = iy + (e.rawY - ity).toInt()
                        windowManager.updateViewLayout(overlayView, params)
                    } else if (e.pointerCount == 2) {
                        val d = hypot(
                            (e.getX(0) - e.getX(1)).toDouble(),
                            (e.getY(0) - e.getY(1)).toDouble()
                        ).toFloat()
                        size = max(80, min(280, (isz * d / 200f).toInt()))
                        params!!.width = size
                        params!!.height = size
                        windowManager.updateViewLayout(overlayView, params)
                        spider.invalidate()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    onSpiderClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun onSpiderClick() {
        introRunnable?.let { handler.removeCallbacks(it) }
        introRunnable = null

        if (!lagOn) {
            // Đang tắt (đỏ) → bấm để BẬT → chuyển XANH + 999+
            lagOn = true
            spider.setColorMode(SpiderView.MODE_GREEN)
            setLag(true)
            MemoryEngine.activateLagKill(true)
        } else {
            // Đang bật (xanh) → bấm để TẮT → chuyển ĐỎ, hết 999+ ngay
            lagOn = false
            spider.setColorMode(SpiderView.MODE_RED)
            setLag(false)
            MemoryEngine.activateLagKill(false)
        }
    }

    private fun setLag(on: Boolean) {
        startService(Intent(this, LagVpnService::class.java).apply {
            action = if (on) LagVpnService.ACTION_START_LAG else LagVpnService.ACTION_STOP_LAG
        })
    }

    private fun buildNotification(): Notification {
        val id = "lag_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(
                    NotificationChannel(id, "Lag Overlay", NotificationManager.IMPORTANCE_LOW)
                )
        }
        return Notification.Builder(this, id)
            .setContentTitle("FF Lag Shizuku")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        introRunnable?.let { handler.removeCallbacks(it) }
        try { windowManager.removeView(overlayView) } catch (_: Exception) {}
    }
}

class SpiderView(ctx: Context) : View(ctx) {

    companion object {
        const val MODE_BLACK = 0
        const val MODE_GREEN = 1
        const val MODE_RED = 2
    }

    private val web = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private var mode = MODE_BLACK

    fun setColorMode(m: Int) {
        mode = m
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = min(cx, cy) * 0.9f

        when (mode) {
            MODE_BLACK -> {
                web.color = Color.argb(220, 40, 40, 40)
                fill.color = Color.argb(180, 20, 20, 20)
            }
            MODE_GREEN -> {
                web.color = Color.argb(220, 0, 255, 100)
                fill.color = Color.argb(100, 0, 255, 80)
            }
            MODE_RED -> {
                web.color = Color.argb(220, 255, 50, 50)
                fill.color = Color.argb(120, 255, 30, 30)
            }
        }

        c.drawCircle(cx, cy, r, fill)
        c.drawCircle(cx, cy, r, web)
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            c.drawLine(cx, cy, cx + r * Math.cos(a).toFloat(), cy + r * Math.sin(a).toFloat(), web)
        }
        for (ring in 1..3) {
            c.drawCircle(cx, cy, r * ring / 3.5f, web)
        }
    }
}

/* ===================== VPN LAG ===================== */

class LagVpnService : VpnService() {
    companion object {
        const val ACTION_START_LAG = "START_LAG"
        const val ACTION_STOP_LAG = "STOP_LAG"
        private const val DELAY_MS = 950L
    }

    private var iface: ParcelFileDescriptor? = null
    private val running = AtomicBoolean(false)
    private val lag = AtomicBoolean(false)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LAG -> lag.set(true)
            ACTION_STOP_LAG -> lag.set(false)
            else -> startVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (iface != null) return
        iface = Builder()
            .setSession("FF Lag")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .setMtu(1500)
            .setBlocking(true)
            .establish() ?: return
        running.set(true)
        thread(name = "vpn") {
            val input = FileInputStream(iface!!.fileDescriptor)
            val output = FileOutputStream(iface!!.fileDescriptor)
            val buf = ByteBuffer.allocate(32767)
            while (running.get()) {
                buf.clear()
                val n = input.read(buf.array())
                if (n > 0) {
                    if (lag.get()) Thread.sleep(DELAY_MS)
                    output.write(buf.array(), 0, n)
                }
            }
        }
    }

    override fun onDestroy() {
        running.set(false)
        iface?.close()
        iface = null
        super.onDestroy()
    }
}

/* ===================== MEMORY ENGINE ===================== */

object MemoryEngine {
    private var svc: IMemoryService? = null

    fun bind(ctx: Context) {
        val args = UserServiceArgs(
            ComponentName(ctx.packageName, MemoryUserService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("mem")
            .version(1)
            .tag("ff-mem")

        Shizuku.bindUserService(args, object : ServiceConnection {
            override fun onServiceConnected(n: ComponentName?, b: IBinder?) {
                svc = IMemoryService.Stub.asInterface(b)
            }
            override fun onServiceDisconnected(n: ComponentName?) {
                svc = null
            }
        })
    }

    fun activateLagKill(enable: Boolean) {
        val s = svc ?: return
        var pid = s.findPid("com.dts.freefireth")
        if (pid <= 0) pid = s.findPid("com.dts.freefiremax")
        if (pid > 0) {
            s.freezeEntities(pid, enable)
            if (!enable) s.forceKillAfterDamage(pid, 200)
        }
    }
}

class MemoryUserService : Service() {
    override fun onBind(intent: Intent?): IBinder = binder

    private val binder = object : IMemoryService.Stub() {
        override fun findPid(pkg: String): Int {
            return try {
                File("/proc").listFiles()?.forEach { dir ->
                    val pid = dir.name.toIntOrNull() ?: return@forEach
                    val cmd = File(dir, "cmdline")
                        .takeIf { it.exists() }
                        ?.readText()
                        ?.trim('\u0000') ?: return@forEach
                    if (cmd.contains(pkg)) return pid
                }
                -1
            } catch (_: Exception) {
                -1
            }
        }

        override fun freezeEntities(pid: Int, enable: Boolean): Boolean {
            return NativeMemory.freezeRemoteEntities(pid, enable)
        }

        override fun forceKillAfterDamage(pid: Int, dmg: Int): Boolean {
            return NativeMemory.forceKill(pid, dmg)
        }
    }
}
