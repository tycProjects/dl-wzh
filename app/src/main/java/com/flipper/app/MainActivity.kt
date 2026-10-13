package com.flipper.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONArray
import org.json.JSONObject
import java.net.ConnectException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.io.ByteArrayOutputStream
import android.hardware.ConsumerIrManager
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.view.accessibility.AccessibilityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import android.app.PendingIntent

/**
 * Menampilkan tampilan Flipper (assets/index.html) di dalam WebView.
 * Tombol "Scan WiFi Sekitar" di popup Sub-GHz memanggil AndroidWifi.scanWifi(),
 * lalu hasilnya dikirim balik ke halaman lewat window.onWifiResult / window.onWifiError.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val handler = Handler(Looper.getMainLooper())
    private var receiver: BroadcastReceiver? = null
    private var timeout: Runnable? = null
    private var scanning = false
    private val lanBusy = AtomicBoolean(false)
    private var pendingOverlayPkg: String? = null
    private var nfcAdapter: NfcAdapter? = null
    private var nfcListening = false // true hanya saat popup NFC di layar sedang terbuka
    private var bleScanning = false
    private var bleCallback: ScanCallback? = null
    private val bleSeen = java.util.concurrent.ConcurrentHashMap<String, Long>() // alamat -> waktu terakhir dilaporkan
    private var pendingOverlayName: String? = null
    @Volatile private var lanPool: ExecutorService? = null

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (!scanning) return@registerForActivityResult
            if (hasWifiPermission()) {
                startScan()
            } else {
                fail("Izin ditolak. Android butuh izin Lokasi / Perangkat sekitar untuk membaca daftar WiFi.")
            }
        }

    /** Modul 5: balik dari halaman izin "Tampil di atas aplikasi lain". */
    private val overlayPermLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            afterOverlayPermissionChecked()
        }

    /** Modul 5: hasil permintaan izin notifikasi (Android 13+). Lanjut jalan walau ditolak. */
    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            startOverlayServiceNow()
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        val root = FrameLayout(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.app_bg))
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        webView = WebView(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.app_bg))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.textZoom = 100
            settings.setSupportZoom(false)
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            webViewClient = object : WebViewClient() {
                // Hanya izinkan halaman bawaan aplikasi; link lain diblokir.
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    return !request.url.toString().startsWith("file:///android_asset/")
                }
            }
            // Satu objek Bridge yang sama didaftarkan dengan beberapa nama supaya
            // kodenya di index.html (AndroidWifi.., AndroidGames.., AndroidMem..)
            // sama-sama nyambung ke kelas Kotlin yang sama.
            val bridge = Bridge()
            addJavascriptInterface(bridge, "AndroidWifi")
            addJavascriptInterface(bridge, "AndroidGames")
            addJavascriptInterface(bridge, "AndroidMem")
            loadUrl("file:///android_asset/index.html")
        }
        root.addView(
            webView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)

        // Tombol Back: tutup popup dulu, kalau tidak ada popup baru keluar aplikasi.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript("(window.closeModal ? window.closeModal() : false)") { result ->
                    if (result != "true") {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        cleanup()
        lanPool?.shutdownNow()
        webView.destroy()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        val adapter = nfcAdapter ?: return
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Semua teknologi tag didaftarkan (tanpa filter techList) supaya kartu
        // apa pun yang ditempelkan langsung masuk ke onNewIntent, lalu diklasifikasi di sana.
        adapter.enableForegroundDispatch(this, pending, null, null)
    }

    override fun onPause() {
        nfcAdapter?.disableForegroundDispatch(this)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (!nfcListening) return // popup NFC sedang tidak terbuka -- abaikan
        val tag: Tag = getTagFromIntent(intent) ?: return
        Thread { processNfcTag(tag) }.start()
    }

    /** Dipanggil dari JavaScript sebagai AndroidWifi.scanWifi() (thread latar belakang). */
    inner class Bridge {
        @JavascriptInterface
        fun scanWifi() {
            runOnUiThread { beginScan() }
        }

        /** Pindai perangkat di WiFi yang SEDANG tersambung (bukan WiFi lain). */
        @JavascriptInterface
        fun scanDevices() {
            startDeviceScan()
        }

        /** Daftar aplikasi game offline yang terpasang di HP ini. */
        @JavascriptInterface
        fun listGames() {
            Thread { postGames() }.start()
        }

        /** Buka game yang dipilih, seperti menekan ikonnya dari launcher. */
        @JavascriptInterface
        fun launchGame(pkg: String) {
            runOnUiThread { launchApp(pkg) }
        }

        /** Buka alat edit (GameGuardian / VirtualXposed) kalau terpasang. */
        @JavascriptInterface
        fun openEditor() {
            runOnUiThread { openEditorApp() }
        }

        /**
         * MODUL 1 (uji coba): tes mesin akses memori terhadap proses aplikasi
         * ini sendiri. Membuktikan root + binary + ptrace + baca memori
         * benar-benar jalan, tanpa perlu UI lengkap (itu modul berikutnya).
         */
        @JavascriptInterface
        fun selfTestMemEngine() {
            Thread { runMemEngineSelfTest() }.start()
        }

        /**
         * MODUL 4+5: buka game [pkg] lalu nyalakan bubble overlay "Edit Koin"
         * (mengurus izin "tampil di atas aplikasi lain" dulu kalau belum ada).
         */
        @JavascriptInterface
        fun startOverlay(pkg: String, name: String) {
            runOnUiThread {
                pendingOverlayPkg = pkg
                pendingOverlayName = name
                launchApp(pkg)
                beginOverlayPermissionFlow()
            }
        }

        /** Infrared: true kalau HP ini punya IR blaster fisik. */
        @JavascriptInterface
        fun hasIrBlaster(): Boolean = irManager()?.hasIrEmitter() == true

        /**
         * Kirim kode NEC (protokol IR paling umum dipakai remote sederhana:
         * TV, kipas, sebagian AC portable satu-tombol). [hex] adalah kode
         * datanya dalam heksadesimal, misal "00FFA25D" -- cari kode yang
         * sesuai remote aslinya di database IR online kalau tidak yakin.
         */
        @JavascriptInterface
        fun sendNecIr(hex: String) {
            runOnUiThread { transmitNec(hex) }
        }

        /**
         * Kirim pola IR mentah: [freqHz] frekuensi pembawa (umumnya 38000),
         * [pattern] daftar durasi mikrodetik dipisah koma, berselang-seling
         * nyala/mati, dimulai dari nyala. Ini cara paling akurat kalau kamu
         * sudah punya kode presis dari database IR / hasil capture lain.
         */
        @JavascriptInterface
        fun sendRawIr(freqHz: Int, pattern: String) {
            runOnUiThread { transmitRaw(freqHz, pattern) }
        }

        /**
         * Kirim kode Sony SIRC -- protokol IR umum kedua (selain NEC) yang
         * dipakai banyak TV/soundbar/proyektor, termasuk sebagian merk lain
         * yang ikut protokol ini. [command] 7 bit, [device] 5 bit (bisa lebih
         * kalau [bits] diisi >12, mis. 15/20 untuk varian extended).
         */
        @JavascriptInterface
        fun sendSirc(command: Int, device: Int, bits: Int) {
            runOnUiThread { transmitSirc(command, device, if (bits <= 0) 12 else bits) }
        }

        /** true kalau HP ini punya chip NFC dan NFC-nya sedang menyala. */
        @JavascriptInterface
        fun hasNfc(): Boolean = nfcAdapter?.isEnabled == true

        /** Dipanggil saat popup NFC dibuka: mulai proses tag yang ditempelkan. */
        @JavascriptInterface
        fun startNfcListening() {
            nfcListening = true
        }

        /** Dipanggil saat popup NFC ditutup: berhenti memproses tag. */
        @JavascriptInterface
        fun stopNfcListening() {
            nfcListening = false
        }

        /** Pemindai IoT: scan WiFi yang tersambung, cari device tanpa password. */
        @JavascriptInterface
        fun scanIot() {
            Thread { runIotScan() }.start()
        }

        /** Hentikan apa pun yang sedang di-cast ke Chromecast di [ip] (lewat API DIAL resmi, tanpa auth). */
        @JavascriptInterface
        fun iotStopChromecast(ip: String) {
            stopChromecast(ip)
        }

        /** Nyala/matikan smart plug Tasmota di [ip]. */
        @JavascriptInterface
        fun iotToggleTasmota(ip: String, turnOn: Boolean) {
            toggleTasmota(ip, turnOn)
        }

        /** true kalau HP ini punya Bluetooth dan sedang menyala. */
        @JavascriptInterface
        fun hasBle(): Boolean {
            val a = (getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter
            return a?.isEnabled == true
        }

        /** Mulai dengerin sinyal Bluetooth di sekitar (dipanggil saat popup BLE dibuka). */
        @JavascriptInterface
        fun startBleScan() {
            runOnUiThread { beginBleScan() }
        }

        /** Berhenti dengerin (dipanggil saat popup BLE ditutup). */
        @JavascriptInterface
        fun stopBleScan() {
            runOnUiThread { endBleScan() }
        }

        /**
         * Audit Celah Keamanan HP: cek semua hal yang biasanya jadi incaran
         * pertama kalau HP jatuh ke tangan orang lain -- kunci layar, USB
         * debugging, sumber app tak dikenal, versi patch keamanan, dan
         * (penting) app dengan hak Accessibility/Device Admin yang aktif --
         * ini cara mendeteksi kalau ada spyware/stalkerware yang sudah
         * terpasang diam-diam di HP sendiri.
         */
        @JavascriptInterface
        fun runSecurityAudit() {
            Thread { doSecurityAudit() }.start()
        }
    }

    // ---------------------------------------------------------------- scan WiFi

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun hasNearbyPermission() =
        Build.VERSION.SDK_INT >= 33 && granted(Manifest.permission.NEARBY_WIFI_DEVICES)

    private fun hasWifiPermission() =
        granted(Manifest.permission.ACCESS_FINE_LOCATION) || hasNearbyPermission()

    private fun beginScan() {
        if (scanning) return
        scanning = true
        if (!hasWifiPermission()) {
            val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            permLauncher.launch(perms.toTypedArray())
            return
        }
        startScan()
    }

    private fun startScan() {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wm.isWifiEnabled) {
            fail("WiFi sedang mati. Nyalakan WiFi dulu, lalu coba lagi.")
            return
        }
        if (!hasNearbyPermission()) {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (!LocationManagerCompat.isLocationEnabled(lm)) {
                fail("Lokasi (GPS) sedang mati. Android mewajibkannya untuk scan WiFi. Nyalakan lokasi, lalu coba lagi.")
                return
            }
        }

        cleanup()
        val r = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                deliver(wm)
            }
        }
        receiver = r
        ContextCompat.registerReceiver(
            this, r,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
            ContextCompat.RECEIVER_EXPORTED
        )

        @Suppress("DEPRECATION")
        val started = try {
            wm.startScan()
        } catch (e: SecurityException) {
            false
        }

        if (!started) {
            // Android membatasi seberapa sering scan boleh dimulai -> tampilkan hasil terakhir.
            deliver(wm)
            return
        }
        // Jaga-jaga kalau broadcast hasil scan tidak datang.
        timeout = Runnable { deliver(wm) }.also { handler.postDelayed(it, 10_000L) }
    }

    private fun deliver(wm: WifiManager) {
        if (!scanning) return
        val results: List<ScanResult> = try {
            wm.scanResults
        } catch (e: SecurityException) {
            fail("Izin untuk membaca hasil scan ditolak.")
            return
        }
        cleanup()
        scanning = false

        @Suppress("DEPRECATION")
        val ci = try { wm.connectionInfo } catch (e: SecurityException) { null }
        val connBssid = ci?.bssid?.lowercase()
        val connSsid = ci?.ssid?.trim('"') ?: ""

        val arr = JSONArray()
        for (s in results) {
            arr.put(
                JSONObject().apply {
                    put("ssid", s.SSID ?: "")
                    put("bssid", s.BSSID ?: "")
                    put("level", s.level)
                    put("freq", s.frequency)
                    put("channel", channelOf(s.frequency))
                    put("security", securityOf(s.capabilities ?: ""))
                    put(
                        "connected",
                        (connBssid != null && connBssid == s.BSSID?.lowercase()) ||
                            (connSsid.isNotEmpty() && connSsid != "<unknown ssid>" && connSsid == s.SSID)
                    )
                }
            )
        }
        js("window.onWifiResult(${JSONObject.quote(arr.toString())})")
    }

    private fun fail(message: String) {
        cleanup()
        scanning = false
        js("window.onWifiError(${JSONObject.quote(message)})")
    }

    private fun cleanup() {
        timeout?.let { handler.removeCallbacks(it) }
        timeout = null
        receiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
            }
        }
        receiver = null
    }

    private fun js(code: String) {
        runOnUiThread { if (!isDestroyed) webView.evaluateJavascript(code, null) }
    }

    private fun channelOf(freq: Int): Int = when {
        freq == 2484 -> 14
        freq in 2412..2472 -> (freq - 2407) / 5
        freq in 5000..5900 -> (freq - 5000) / 5
        freq in 5925..7125 -> (freq - 5950) / 5
        else -> 0
    }

    private fun securityOf(capabilities: String): String {
        val c = capabilities.uppercase()
        return when {
            "SAE" in c && "PSK" in c -> "WPA2/WPA3"
            "SAE" in c || "WPA3" in c -> "WPA3"
            "WPA2" in c || "RSN" in c -> "WPA2"
            "WPA" in c -> "WPA"
            "WEP" in c -> "WEP"
            "OWE" in c -> "OWE"
            else -> "Terbuka"
        }
    }

    // ------------------------------------------------- perangkat di WiFi yang tersambung

    private fun startDeviceScan() {
        if (!lanBusy.compareAndSet(false, true)) return
        Thread {
            try {
                runDeviceScan()
            } catch (e: Exception) {
                devError("Gagal memindai jaringan: ${e.message ?: "kesalahan tidak diketahui"}")
            } finally {
                lanBusy.set(false)
            }
        }.start()
    }

    private fun runDeviceScan() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork
        val caps = net?.let { cm.getNetworkCapabilities(it) }
        if (net == null || caps == null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            devError("Kamu belum tersambung ke WiFi. Sambungkan dulu ke WiFi yang ingin dilihat.")
            return
        }
        val lp = cm.getLinkProperties(net)
        val addr = lp?.linkAddresses?.firstOrNull { it.address is Inet4Address }
        val ownIp = addr?.address?.hostAddress
        if (lp == null || addr == null || ownIp == null) {
            devError("Alamat IP WiFi tidak ditemukan.")
            return
        }

        // Batasi ke maksimal 254 alamat (/24) supaya cepat dan tidak membebani jaringan.
        val prefix = if (addr.prefixLength in 24..30) addr.prefixLength else 24
        val mask = (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
        val network = ipToLong(addr.address.address) and mask
        val broadcast = network or (mask.inv() and 0xFFFFFFFFL)
        val gateway = lp.routes
            .firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }
            ?.gateway?.hostAddress

        val ips = ((network + 1) until broadcast).map { longToIp(it) }
        val total = ips.size
        val found = ConcurrentLinkedQueue<JSONObject>()
        val done = AtomicInteger(0)
        val pool = Executors.newFixedThreadPool(40)
        lanPool = pool

        for (ip in ips) {
            pool.execute {
                try {
                    if (ip == ownIp) {
                        found.add(deviceJson(ip, ownDeviceName(), "self"))
                    } else if (ip == gateway || isAlive(ip)) {
                        found.add(deviceJson(ip, hostNameOf(ip), if (ip == gateway) "router" else "device"))
                    }
                } catch (e: Exception) {
                }
                val d = done.incrementAndGet()
                if (d % 10 == 0 || d == total) js("window.onDeviceProgress($d,$total)")
            }
        }
        pool.shutdown()
        pool.awaitTermination(45, TimeUnit.SECONDS)
        pool.shutdownNow()
        lanPool = null

        val arr = JSONArray()
        found.forEach { arr.put(it) }
        js("window.onDevicesResult(${JSONObject.quote(arr.toString())})")
    }

    private fun deviceJson(ip: String, name: String, role: String) =
        JSONObject().apply {
            put("ip", ip)
            put("name", name)
            put("role", role)
        }

    /** Nama HP ini seperti yang tertulis di Pengaturan, atau model HP kalau tidak ada. */
    private fun ownDeviceName(): String {
        val n = try {
            Settings.Global.getString(contentResolver, "device_name")
        } catch (e: Exception) {
            null
        }
        return if (n.isNullOrBlank()) Build.MODEL else n
    }

    /** Nama dari DNS router (sering berisi nama perangkat). Kosong kalau tidak ada. */
    private fun hostNameOf(ip: String): String {
        val h = try {
            InetAddress.getByName(ip).canonicalHostName
        } catch (e: Exception) {
            ip
        }
        if (h == ip) return ""
        return h.substringBefore('.')
    }

    /** Perangkat dianggap aktif jika membalas ping/TCP, termasuk jawaban "ditolak" (refused). */
    private fun isAlive(ip: String): Boolean {
        try {
            if (InetAddress.getByName(ip).isReachable(700)) return true
        } catch (e: Exception) {
        }
        for (port in intArrayOf(80, 443, 62078)) {
            try {
                Socket().use {
                    it.connect(InetSocketAddress(ip, port), 250)
                    return true
                }
            } catch (e: ConnectException) {
                if (e.message?.contains("refused", ignoreCase = true) == true) return true
            } catch (e: Exception) {
            }
        }
        return false
    }

    private fun devError(message: String) {
        js("window.onDevicesError(${JSONObject.quote(message)})")
    }

    // ------------------------------------------------- Pemindai IoT Tanpa Password

    /**
     * Scan jaringan WiFi yang sedang tersambung, cari perangkat IoT yang
     * kontrolnya TERBUKA TANPA PASSWORD by design (bukan dibobol, bukan
     * ditebak -- ini fakta dari desain pabrik perangkatnya):
     *  - Chromecast: API HTTP publik di port 8008 (protokol DIAL, dipakai
     *    resmi buat "cast" dari app lain, tidak ada otentikasi).
     *  - Smart plug Tasmota: panel web + API command di port 80, tanpa
     *    password kecuali pemiliknya mengatur sendiri.
     *  - Printer jaringan: port cetak mentah 9100 / IPP 631 -- HANYA
     *    dideteksi, TIDAK dikirimi apa pun (supaya tidak buang kertas
     *    orang lain tanpa sengaja).
     */
    private fun runIotScan() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork
        val caps = net?.let { cm.getNetworkCapabilities(it) }
        if (net == null || caps == null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            js("window.onIotError && window.onIotError(${JSONObject.quote("Kamu belum tersambung ke WiFi. Sambungkan dulu ke WiFi yang ingin diperiksa.")})")
            return
        }
        val lp = cm.getLinkProperties(net)
        val addr = lp?.linkAddresses?.firstOrNull { it.address is Inet4Address }
        val ownIp = addr?.address?.hostAddress
        if (lp == null || addr == null || ownIp == null) {
            js("window.onIotError && window.onIotError(${JSONObject.quote("Alamat IP WiFi tidak ditemukan.")})")
            return
        }

        val prefix = if (addr.prefixLength in 24..30) addr.prefixLength else 24
        val mask = (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
        val network = ipToLong(addr.address.address) and mask
        val broadcast = network or (mask.inv() and 0xFFFFFFFFL)
        val ips = ((network + 1) until broadcast).map { longToIp(it) }
        val total = ips.size

        val found = ConcurrentLinkedQueue<JSONObject>()
        val done = AtomicInteger(0)
        val pool = Executors.newFixedThreadPool(40)
        lanPool = pool

        for (ip in ips) {
            pool.execute {
                try {
                    if (ip != ownIp) probeIotDevice(ip)?.let { found.add(it) }
                } catch (e: Exception) {
                }
                val d = done.incrementAndGet()
                if (d % 10 == 0 || d == total) js("window.onIotProgress && window.onIotProgress($d,$total)")
            }
        }
        pool.shutdown()
        pool.awaitTermination(50, TimeUnit.SECONDS)
        pool.shutdownNow()
        lanPool = null

        val arr = JSONArray()
        found.forEach { arr.put(it) }
        js("window.onIotResult && window.onIotResult(${JSONObject.quote(arr.toString())})")
    }

    /** true kalau port TCP di [ip]:[port] menerima koneksi dalam [timeoutMs]. */
    private fun portOpen(ip: String, port: Int, timeoutMs: Int = 350): Boolean {
        return try {
            Socket().use { it.connect(InetSocketAddress(ip, port), timeoutMs); true }
        } catch (e: Exception) {
            false
        }
    }

    /** Ambil isi respons HTTP sederhana (tanpa library tambahan, GET biasa). */
    private fun httpGet(ip: String, port: Int, path: String, timeoutMs: Int = 900): String? {
        return try {
            Socket().use { sock ->
                sock.connect(InetSocketAddress(ip, port), timeoutMs)
                sock.soTimeout = timeoutMs
                sock.getOutputStream().write(
                    "GET $path HTTP/1.1\r\nHost: $ip\r\nConnection: close\r\n\r\n".toByteArray()
                )
                val resp = sock.getInputStream().bufferedReader().readText()
                resp
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun probeIotDevice(ip: String): JSONObject? {
        // 1) Chromecast / Google Cast -- API DIAL publik di port 8008, tanpa otentikasi.
        if (portOpen(ip, 8008, 300)) {
            val info = httpGet(ip, 8008, "/setup/eureka_info", 1200)
            if (info != null && (info.contains("\"name\"") || info.contains("cast") || info.contains("Chromecast"))) {
                val name = Regex("\"name\"\\s*:\\s*\"([^\"]*)\"").find(info)?.groupValues?.get(1) ?: "Chromecast"
                return iotJson(ip, "chromecast", name, "Kontrol cast terbuka tanpa password (API DIAL, port 8008) -- siapa pun di WiFi ini bisa hentikan/ganti apa yang sedang diputar.")
            }
        }
        // 2) Smart plug Tasmota -- panel web + API command di port 80, biasanya tanpa password.
        if (portOpen(ip, 80, 300)) {
            val html = httpGet(ip, 80, "/", 1200)
            if (html != null && (html.contains("Tasmota", true) || html.contains("cm?cmnd", true))) {
                return iotJson(ip, "tasmota", "Smart Plug/Switch (Tasmota)", "Panel kontrol & API perintah terbuka tanpa password -- siapa pun di WiFi ini bisa nyala/matikan perangkat ini.")
            }
        }
        // 3) Printer jaringan -- deteksi saja, TIDAK mengirim apa pun.
        if (portOpen(ip, 9100, 250)) {
            return iotJson(ip, "printer", "Printer Jaringan (port cetak mentah)", "Port cetak 9100 terbuka tanpa otentikasi -- siapa pun di WiFi ini bisa mengirim dokumen untuk dicetak tanpa izin. (Tidak ada yang dikirim oleh pemindaian ini.)")
        }
        if (portOpen(ip, 631, 250)) {
            return iotJson(ip, "printer", "Printer Jaringan (IPP)", "Port IPP 631 terbuka -- biasanya butuh konfirmasi, tapi tetap perlu dicek apakah otentikasinya aktif.")
        }
        return null
    }

    private fun iotJson(ip: String, kind: String, name: String, risk: String) =
        JSONObject().apply {
            put("ip", ip)
            put("kind", kind)
            put("name", name)
            put("risk", risk)
        }

    /** Chromecast: hentikan aplikasi yang sedang cast lewat protokol DIAL resmi (HTTP biasa, tanpa auth). */
    private fun stopChromecast(ip: String) {
        Thread {
            try {
                val apps = httpGet(ip, 8008, "/apps", 1500)
                // DIAL: cek aplikasi yang umum lagi jalan (YouTube/Netflix/default receiver), lalu DELETE sesinya.
                val candidates = listOf("YouTube", "Netflix", "CC1AD845") // CC1AD845 = default media receiver
                var stopped = false
                for (appId in candidates) {
                    val status = httpGet(ip, 8008, "/apps/$appId", 900) ?: continue
                    if (status.contains("200 OK") || status.contains("<service")) {
                        try {
                            Socket().use { sock ->
                                sock.connect(InetSocketAddress(ip, 8008), 1200)
                                sock.getOutputStream().write(
                                    "DELETE /apps/$appId HTTP/1.1\r\nHost: $ip\r\nConnection: close\r\n\r\n".toByteArray()
                                )
                                sock.getInputStream().bufferedReader().readText()
                            }
                            stopped = true
                        } catch (_: Exception) {
                        }
                    }
                }
                js("window.onIotActionResult && window.onIotActionResult(${JSONObject.quote(stopped.toString())}, ${JSONObject.quote(if (stopped) "Perintah stop terkirim ke Chromecast $ip." else "Tidak ada aplikasi yang sedang cast terdeteksi di $ip.")})")
            } catch (e: Exception) {
                js("window.onIotActionResult && window.onIotActionResult(false, ${JSONObject.quote("Gagal: ${e.message}")})")
            }
        }.start()
    }

    // ------------------------------------------------- BLE Scanner: sinyal Bluetooth di sekitar

    /**
     * Dengarkan siaran (advertisement) Bluetooth Low Energy di sekitar HP.
     * Ini PASIF -- cuma mendengarkan apa yang sudah dipancarkan perangkat lain
     * ke udara (sama legalnya dengan scan WiFi), tidak menyambung/meretas
     * perangkat mana pun. Dipakai juga oleh fitur "deteksi tracker" bawaan
     * Android/iPhone untuk anti-stalking.
     */
    private fun beginBleScan() {
        if (bleScanning) return
        val btAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter
        if (btAdapter == null || !btAdapter.isEnabled) {
            js("window.onBleError && window.onBleError(${JSONObject.quote("Bluetooth tidak tersedia atau sedang mati. Nyalakan Bluetooth, lalu buka lagi.")})")
            return
        }
        val needPerm = if (Build.VERSION.SDK_INT >= 31) {
            !granted(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            !granted(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (needPerm) {
            val perms = if (Build.VERSION.SDK_INT >= 31) {
                arrayOf(Manifest.permission.BLUETOOTH_SCAN)
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            blePermLauncher.launch(perms)
            return
        }
        val scanner = btAdapter.bluetoothLeScanner
        if (scanner == null) {
            js("window.onBleError && window.onBleError(${JSONObject.quote("Modul BLE tidak tersedia di HP ini.")})")
            return
        }
        bleSeen.clear()
        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                reportBleDevice(result)
            }
            override fun onScanFailed(errorCode: Int) {
                js("window.onBleError && window.onBleError(${JSONObject.quote("Scan BLE gagal (kode $errorCode).")})")
            }
        }
        bleCallback = cb
        try {
            val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
            scanner.startScan(null, settings, cb)
            bleScanning = true
        } catch (e: SecurityException) {
            js("window.onBleError && window.onBleError(${JSONObject.quote("Izin Bluetooth ditolak.")})")
        }
    }

    private fun endBleScan() {
        if (!bleScanning) return
        val btAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter
        val cb = bleCallback
        try {
            if (btAdapter != null && cb != null) btAdapter.bluetoothLeScanner?.stopScan(cb)
        } catch (_: Exception) {
        }
        bleScanning = false
        bleCallback = null
    }

    private val blePermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.values.all { it }) beginBleScan()
            else js("window.onBleError && window.onBleError(${JSONObject.quote("Izin Bluetooth/Lokasi ditolak -- dibutuhkan Android untuk memindai sinyal BLE.")})")
        }

    @SuppressLint("MissingPermission")
    private fun reportBleDevice(result: ScanResult) {
        val addr = result.device?.address ?: return
        val now = System.currentTimeMillis()
        val last = bleSeen[addr]
        if (last != null && now - last < 2500) return // jangan spam, laporkan per perangkat maks tiap 2.5 detik
        bleSeen[addr] = now

        val rssi = result.rssi
        val name = try { result.device?.name } catch (e: SecurityException) { null }
            ?: result.scanRecord?.deviceName

        val manuData = result.scanRecord?.manufacturerSpecificData
        var kind = "unknown"
        var note = ""
        if (manuData != null) {
            val apple = manuData.get(0x004C) // Apple Inc. company identifier (publik, dipakai di spesifikasi Bluetooth SIG)
            if (apple != null && apple.isNotEmpty() && (apple[0].toInt() and 0xFF) == 0x12) {
                kind = "tracker"
                note = "Kemungkinan perangkat/tracker Apple Find My (AirTag atau sejenis) -- siaran ini dipakai jaringan Find My untuk melacak lokasi lewat HP orang lain di sekitarnya."
            }
        }
        if (kind == "unknown" && name != null) {
            val n = name.lowercase()
            when {
                "airtag" in n -> { kind = "tracker"; note = "Nama perangkat menyebut AirTag." }
                "tile" in n -> { kind = "tracker"; note = "Kemungkinan tracker Tile." }
                "buds" in n || "earbud" in n || "headphone" in n || "airpods" in n -> kind = "audio"
                "band" in n || "watch" in n || "mi smart" in n -> kind = "wearable"
            }
        }

        // perkiraan jarak sangat kasar dari RSSI (bukan presisi, cuma estimasi umum)
        val distance = Math.pow(10.0, (-59.0 - rssi) / (10.0 * 2.0))

        val json = JSONObject().apply {
            put("addr", addr)
            put("name", name ?: JSONObject.NULL)
            put("rssi", rssi)
            put("distance", String.format("%.1f", distance))
            put("kind", kind)
            put("note", note)
        }
        js("window.onBleDevice && window.onBleDevice(${JSONObject.quote(json.toString())})")
    }

    /** Smart plug Tasmota: nyala/matikan lewat API command resmi (dijelaskan di README/skrip edukasi). */
    private fun toggleTasmota(ip: String, turnOn: Boolean) {
        Thread {
            try {
                val cmd = if (turnOn) "Power%20On" else "Power%20Off"
                val resp = httpGet(ip, 80, "/cm?cmnd=$cmd", 1500)
                val ok = resp != null && (resp.contains("ON", true) || resp.contains("OFF", true) || resp.contains("200"))
                js("window.onIotActionResult && window.onIotActionResult(${JSONObject.quote(ok.toString())}, ${JSONObject.quote(if (ok) "Perintah ${if (turnOn) "ON" else "OFF"} terkirim ke $ip." else "Tidak ada balasan dari $ip.")})")
            } catch (e: Exception) {
                js("window.onIotActionResult && window.onIotActionResult(false, ${JSONObject.quote("Gagal: ${e.message}")})")
            }
        }.start()
    }

    private fun ipToLong(b: ByteArray): Long =
        b.fold(0L) { acc, x -> (acc shl 8) or (x.toLong() and 0xFF) }

    private fun longToIp(v: Long): String =
        "${(v shr 24) and 255}.${(v shr 16) and 255}.${(v shr 8) and 255}.${v and 255}"

    // ------------------------------------------------- daftar & buka game offline

    private fun postGames() {
        val pm = packageManager
        val result = JSONArray()
        try {
            val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = pm.queryIntentActivities(launchIntent, 0)
            val seen = HashSet<String>()
            for (ri in resolved) {
                val pkg = ri.activityInfo?.packageName ?: continue
                if (!seen.add(pkg)) continue
                if (pkg == packageName) continue
                val info = try { pm.getApplicationInfo(pkg, 0) } catch (e: Exception) { continue }
                // Hanya tampilkan aplikasi yang kategorinya GAME, atau sudah pernah dikenali pakai kata kunci umum.
                val isGame = isGameApp(info, pm)
                if (!isGame) continue
                val name = pm.getApplicationLabel(info).toString()
                val icon = drawableToDataUri(pm.getApplicationIcon(info))
                result.put(
                    JSONObject().apply {
                        put("pkg", pkg)
                        put("name", name)
                        put("icon", icon)
                    }
                )
            }
        } catch (e: Exception) {
            js("window.onGamesError(${JSONObject.quote("Gagal memuat daftar game: " + (e.message ?: ""))})")
            return
        }
        js("window.onGamesResult(${JSONObject.quote(result.toString())})")
    }

    private fun isGameApp(info: ApplicationInfo, pm: PackageManager): Boolean {
        if (Build.VERSION.SDK_INT >= 26 && info.category == ApplicationInfo.CATEGORY_GAME) return true
        @Suppress("DEPRECATION")
        return (info.flags and ApplicationInfo.FLAG_IS_GAME) != 0
    }

    private fun drawableToDataUri(d: Drawable): String {
        val size = 96
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        d.setBounds(0, 0, size, size)
        d.draw(canvas)
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
        return "data:image/png;base64," + android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP)
    }

    private fun launchApp(pkg: String) {
        val intent = packageManager.getLaunchIntentForPackage(pkg) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    /**
     * Buka alat edit koin PIHAK KETIGA yang sudah terpasang (GameGuardian / VirtualXposed),
     * khusus untuk game offline milik pengguna sendiri. Aplikasi ini tidak membaca atau
     * mengubah memori game sendiri -- hanya membuka alat yang sudah dipasang pengguna.
     */
    private fun openEditorApp() {
        val candidates = listOf(
            "com.gameguardian.gg" to "GameGuardian",
            "org.gameguardian.app" to "GameGuardian",
            "com.ceshi.gameguardian" to "GameGuardian",
            "io.va.exposed" to "VirtualXposed",
            "io.virtualapp" to "VirtualApp"
        )
        for ((pkg, _) in candidates) {
            val i = packageManager.getLaunchIntentForPackage(pkg)
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
                js("window.onEditorOpened()")
                return
            }
        }
        js("window.onEditorMissing(${JSONObject.quote("https://gameguardian.net/download")})")
    }

    // ------------------------------------------------- modul 1: uji mesin akses memori

    private fun runMemEngineSelfTest() {
        fun report(ok: Boolean, message: String) {
            js("window.onMemSelfTestResult(${JSONObject.quote(ok.toString())},${JSONObject.quote(message)})")
        }

        val engine = MemEngine(applicationContext)
        if (!engine.prepare()) {
            report(false, "Binary mem_engine tidak ditemukan. Build project ini lewat Android Studio dulu (butuh NDK terpasang) supaya CMake mengompilenya, baru coba lagi.")
            return
        }
        if (!engine.start()) {
            report(false, "Gagal mendapat akses root (su). Pastikan HP sudah di-root dan izin root untuk aplikasi ini disetujui.")
            return
        }
        try {
            val myPid = android.os.Process.myPid()
            if (!engine.attach(myPid)) {
                report(false, "Root aktif, tapi gagal attach ke proses sendiri (pid=$myPid). Cek Logcat tag MemEngine untuk detail.")
                return
            }
            val maps = engine.listMaps(myPid)
            if (maps.isEmpty()) {
                report(false, "Attach berhasil, tapi /proc/$myPid/maps kosong/gagal dibaca.")
                return
            }
            // Ambil region pertama yang bisa dibaca ('r' di kolom izin) untuk tes baca.
            val readable = maps.firstOrNull { line ->
                val parts = line.split(" ", limit = 6)
                parts.size > 1 && parts[1].startsWith("r")
            }
            if (readable == null) {
                report(true, "Attach + baca daftar memori berhasil (${maps.size} region), tapi tidak ada region yang jelas 'readable' untuk dites baca.")
                return
            }
            val startHex = readable.substringBefore('-')
            val addr = startHex.toLong(16)
            val data = engine.readMemory(addr, 16)
            engine.detach()
            if (data == null) {
                report(false, "Attach + daftar memori sukses (${maps.size} region), tapi baca 16 byte pertama gagal.")
            } else {
                val hex = data.joinToString(" ") { String.format("%02x", it) }
                report(true, "SUKSES. Root aktif, attach ke pid $myPid, ditemukan ${maps.size} region memori. 16 byte pertama dari ${readable.substringBefore(' ')}: $hex")
            }
        } finally {
            engine.stop()
        }
    }

    // ------------------------------------------------- modul 5: izin & start overlay service

    private fun beginOverlayPermissionFlow() {
        if (!Settings.canDrawOverlays(this)) {
            js("window.onOverlayNeedsPermission && window.onOverlayNeedsPermission()")
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermLauncher.launch(intent)
        } else {
            afterOverlayPermissionChecked()
        }
    }

    private fun afterOverlayPermissionChecked() {
        if (!Settings.canDrawOverlays(this)) {
            js("window.onOverlayPermissionDenied && window.onOverlayPermissionDenied()")
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && !granted(Manifest.permission.POST_NOTIFICATIONS)) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startOverlayServiceNow()
        }
    }

    private fun startOverlayServiceNow() {
        val pkg = pendingOverlayPkg ?: return
        val name = pendingOverlayName ?: pkg
        val intent = Intent(this, OverlayService::class.java).apply {
            putExtra(OverlayService.EXTRA_PKG, pkg)
            putExtra(OverlayService.EXTRA_NAME, name)
        }
        ContextCompat.startForegroundService(this, intent)
        js("window.onOverlayStarted && window.onOverlayStarted()")
    }

    // ------------------------------------------------- Infrared: kirim sinyal IR

    private fun irManager(): ConsumerIrManager? =
        getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    /**
     * Susun pola NEC standar (header 9000/4500us, tiap bit 560us nyala +
     * 560us/1690us mati untuk 0/1, diakhiri 560us nyala) dari data heksadesimal,
     * lalu kirim lewat IR blaster. Protokol ini dipakai banyak remote sederhana;
     * untuk AC rumahan modern yang kodenya lebih rumit (bawa info suhu/mode
     * sekaligus), pakai sendRawIr dengan kode presis dari database IR.
     */
    private fun transmitNec(hex: String) {
        val irm = irManager()
        if (irm == null || !irm.hasIrEmitter()) {
            js("window.onIrResult && window.onIrResult(false, ${JSONObject.quote("HP ini tidak punya IR blaster, atau tidak terdeteksi sistem.")})")
            return
        }
        val clean = hex.trim().removePrefix("0x").removePrefix("0X")
        if (clean.isEmpty() || clean.length % 2 != 0 || !clean.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            js("window.onIrResult && window.onIrResult(false, ${JSONObject.quote("Kode hex tidak valid. Contoh yang benar: 00FFA25D")})")
            return
        }
        val bytes = clean.chunked(2).map { it.toInt(16) }
        val bits = ArrayList<Int>()
        for (b in bytes) for (i in 7 downTo 0) bits.add((b shr i) and 1)

        val pattern = ArrayList<Int>()
        pattern.add(9000); pattern.add(4500) // header
        for (bit in bits) {
            pattern.add(560)
            pattern.add(if (bit == 1) 1690 else 560)
        }
        pattern.add(560) // penutup

        try {
            irm.transmit(38000, pattern.toIntArray())
            js("window.onIrResult && window.onIrResult(true, ${JSONObject.quote("Sinyal IR (NEC) terkirim: $clean")})")
        } catch (e: Exception) {
            js("window.onIrResult && window.onIrResult(false, ${JSONObject.quote("Gagal mengirim: ${e.message}")})")
        }
    }

    /**
     * Susun pola Sony SIRC standar (header 2400/600us, tiap bit LSB-first
     * 1200us=1 / 600us=0 diikuti 600us jeda, carrier 40kHz). Dipakai banyak
     * TV/soundbar/proyektor Sony dan sebagian merk lain yang ikut protokol ini.
     */
    private fun transmitSirc(command: Int, device: Int, bits: Int) {
        val irm = irManager()
        if (irm == null || !irm.hasIrEmitter()) {
            js("window.onIrResult && window.onIrResult(false, ${JSONObject.quote("HP ini tidak punya IR blaster, atau tidak terdeteksi sistem.")})")
            return
        }
        if (bits < 8 || bits > 32) {
            js("window.onIrResult && window.onIrResult(false, ${JSONObject.quote("Jumlah bit tidak wajar (8-32).")})")
            return
        }
        val dataBits = ArrayList<Int>()
        for (i in 0 until 7) dataBits.add((command shr i) and 1)
        for (i in 0 until (bits - 7)) dataBits.add((device shr i) and 1)

        val pattern = ArrayList<Int>()
        pattern.add(2400); pattern.add(600) // header
        for (bit in dataBits) {
            pattern.add(if (bit == 1) 1200 else 600)
            pattern.add(600)
        }
        try {
            irm.transmit(40000, pattern.toIntArray())
            js("window.onIrResult && window.onIrResult(true, ${JSONObject.quote("Sinyal IR (Sony SIRC) terkirim: command=$command device=$device")})")
        } catch (e: Exception) {
            js("window.onIrResult && window.onIrResult(false, ${JSONObject.quote("Gagal mengirim: ${e.message}")})")
        }
    }

    private fun transmitRaw(freqHz: Int, patternStr: String) {
        val irm = irManager()
        if (irm == null || !irm.hasIrEmitter()) {
            js("window.onIrResult && window.onIrResult(false, ${JSONObject.quote("HP ini tidak punya IR blaster, atau tidak terdeteksi sistem.")})")
            return
        }
        val pattern = try {
            patternStr.split(",", " ", "\n").map { it.trim() }.filter { it.isNotEmpty() }.map { it.toInt() }.toIntArray()
        } catch (e: Exception) {
            null
        }
        if (pattern == null || pattern.isEmpty() || pattern.size % 2 != 0 || freqHz <= 0) {
            js("window.onIrResult && window.onIrResult(false, ${JSONObject.quote("Pola IR tidak valid. Harus angka dipisah koma, jumlahnya genap (nyala,mati,nyala,mati,...).")})")
            return
        }
        try {
            irm.transmit(freqHz, pattern)
            js("window.onIrResult && window.onIrResult(true, ${JSONObject.quote("Sinyal IR mentah terkirim (${pattern.size} durasi, ${freqHz}Hz).")})")
        } catch (e: Exception) {
            js("window.onIrResult && window.onIrResult(false, ${JSONObject.quote("Gagal mengirim: ${e.message}")})")
        }
    }

    // ------------------------------------------------- Audit Celah Keamanan HP

    private fun addCheck(arr: JSONArray, id: String, title: String, status: String, detail: String) {
        arr.put(JSONObject().apply {
            put("id", id); put("title", title); put("status", status); put("detail", detail)
        })
    }

    /**
     * Semua item ini dicek lewat API resmi Android, read-only, tidak ada yang
     * diubah. status: "ok" (aman) | "warn" (perlu perhatian) | "bad" (risiko
     * nyata). Daftar ini persis hal pertama yang dicek penyerang kalau HP
     * jatuh ke tangan mereka -- jadi juga persis yang pertama perlu
     * dibereskan buat nutup celahnya.
     */
    private fun doSecurityAudit() {
        val checks = JSONArray()
        var badCount = 0
        var warnCount = 0

        // 1) Kunci layar
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (km?.isDeviceSecure == true) {
            addCheck(checks, "lock", "Kunci Layar", "ok", "PIN/pola/password/biometrik sudah aktif.")
        } else {
            addCheck(checks, "lock", "Kunci Layar", "bad", "HP TIDAK terkunci. Siapa pun yang pegang HP ini langsung bisa akses semuanya.")
            badCount++
        }

        // 2) USB Debugging (ADB)
        val adbOn = try { Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0) == 1 } catch (e: Exception) { false }
        if (adbOn) {
            addCheck(checks, "adb", "USB Debugging", "bad", "Sedang AKTIF. Kalau dicolok ke komputer tak dipercaya, data bisa diakses tanpa kunci layar. Matikan di Opsi Developer kalau tidak sedang dipakai.")
            badCount++
        } else {
            addCheck(checks, "adb", "USB Debugging", "ok", "Tidak aktif.")
        }

        // 3) Opsi Developer
        val devOn = try { Settings.Global.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1 } catch (e: Exception) { false }
        addCheck(
            checks, "dev", "Opsi Developer", if (devOn) "warn" else "ok",
            if (devOn) "Aktif. Tidak berbahaya sendirian, tapi buka akses ke pengaturan lanjutan (termasuk USB debugging) -- matikan kalau tidak perlu."
            else "Tidak aktif."
        )
        if (devOn) warnCount++

        // 4) Sumber app tak dikenal (install dari luar Play Store)
        val canInstallUnknown = if (Build.VERSION.SDK_INT >= 26) packageManager.canRequestPackageInstalls() else {
            try { Settings.Secure.getInt(contentResolver, "install_non_market_apps", 0) == 1 } catch (e: Exception) { false }
        }
        addCheck(
            checks, "unknown", "Sumber Tak Dikenal", if (canInstallUnknown) "warn" else "ok",
            if (canInstallUnknown) "Izin install APK dari luar Play Store aktif untuk aplikasi ini (atau izin global, tergantung versi Android). Jalur paling umum masuknya malware."
            else "Tidak diizinkan."
        )
        if (canInstallUnknown) warnCount++

        // 5) Patch keamanan Android
        val patch = Build.VERSION.SECURITY_PATCH ?: "tidak diketahui"
        val patchOld = try {
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val patchDate = fmt.parse(patch)
            val months = ((System.currentTimeMillis() - (patchDate?.time ?: 0)) / (1000L * 60 * 60 * 24 * 30))
            months > 6
        } catch (e: Exception) { false }
        addCheck(
            checks, "patch", "Patch Keamanan Android", if (patchOld) "warn" else "ok",
            "Terakhir: $patch (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})." +
                if (patchOld) " Sudah lebih dari 6 bulan -- banyak celah yang sudah ditemukan publik belum ditambal." else ""
        )
        if (patchOld) warnCount++

        // 6) App dengan hak Accessibility aktif -- jalur favorit spyware/stalkerware
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val accessibilityApps = try {
            am?.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                ?.mapNotNull { it.resolveInfo?.serviceInfo?.packageName }
                ?.distinct() ?: emptyList()
        } catch (e: Exception) { emptyList() }
        if (accessibilityApps.isEmpty()) {
            addCheck(checks, "access", "Aplikasi dengan Akses Accessibility", "ok", "Tidak ada aplikasi pihak ketiga yang aktif memakainya saat ini.")
        } else {
            val names = accessibilityApps.joinToString(", ") { appLabelOf(it) }
            addCheck(checks, "access", "Aplikasi dengan Akses Accessibility", "warn", "Aktif untuk: $names. Izin ini sangat kuat (bisa membaca layar & mensimulasikan ketukan) -- pastikan kamu kenal & percaya semua aplikasi ini. Kalau ada yang tidak kamu kenal/pasang, itu tanda bahaya.")
            warnCount++
        }

        // 7) App Device Admin aktif -- jalur lain yang sering disalahgunakan stalkerware
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val admins = try { dpm?.activeAdmins?.map { it.packageName }?.distinct() ?: emptyList() } catch (e: Exception) { emptyList() }
        if (admins.isEmpty()) {
            addCheck(checks, "admin", "Aplikasi dengan Hak Device Admin", "ok", "Tidak ada yang aktif.")
        } else {
            val names = admins.joinToString(", ") { appLabelOf(it) }
            addCheck(checks, "admin", "Aplikasi dengan Hak Device Admin", "warn", "Aktif untuk: $names. Izin ini bisa dipakai buat mengunci/menghapus HP dari jarak jauh -- pastikan ini memang kamu yang pasang (mis. akun kantor/MDM resmi).")
            warnCount++
        }

        val result = JSONObject().apply {
            put("checks", checks)
            put("bad", badCount)
            put("warn", warnCount)
        }
        js("window.onAuditResult && window.onAuditResult(${JSONObject.quote(result.toString())})")
    }

    private fun appLabelOf(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }

    // ------------------------------------------------- NFC: baca kartu/tag

    @Suppress("DEPRECATION")
    private fun getTagFromIntent(intent: Intent): Tag? =
        if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

    /**
     * Baca semua info aman dari tag yang ditempelkan: UID, daftar teknologi,
     * data NDEF (kalau ada), dan uji kunci default MIFARE Classic (demo
     * keamanan klasik: banyak kartu lama masih pakai kunci bawaan pabrik
     * FFFFFFFFFFFF yang terkenal luas -- bukan brute force, cuma satu kunci
     * yang memang publik, dipakai buat menunjukkan kartu itu rentan atau tidak).
     */
    private fun processNfcTag(tag: Tag) {
        val uidHex = tag.id.joinToString(":") { String.format("%02X", it) }
        val techs = tag.techList.map { it.substringAfterLast('.') }

        var security = "Tidak diketahui"
        var securityLevel = "unknown" // weak | ok | unknown
        var ndefText: String? = null
        var mifareNote: String? = null

        if (techs.any { it == "MifareClassic" }) {
            val mc = MifareClassic.get(tag)
            try {
                mc.connect()
                val openSectors = ArrayList<Int>()
                val sectorsToTry = minOf(mc.sectorCount, 4) // cek beberapa sektor awal saja, cukup buat demo
                for (s in 0 until sectorsToTry) {
                    if (mc.authenticateSectorWithKeyA(s, MifareClassic.KEY_DEFAULT)) {
                        openSectors.add(s)
                    }
                }
                if (openSectors.isNotEmpty()) {
                    security = "LEMAH -- ${openSectors.size} dari $sectorsToTry sektor awal bisa dibuka dengan kunci default pabrik (FFFFFFFFFFFF)"
                    securityLevel = "weak"
                    mifareNote = "Sektor yang kebuka: ${openSectors.joinToString(", ")}. Ini kelemahan terkenal MIFARE Classic -- banyak kartu lama belum pernah ganti kunci dari bawaan pabrik."
                } else {
                    security = "Kunci default pabrik ditolak di $sectorsToTry sektor awal -- kemungkinan sudah diamankan kunci custom"
                    securityLevel = "ok"
                }
            } catch (e: Exception) {
                security = "MIFARE Classic terdeteksi, tapi gagal diuji (${e.message})"
            } finally {
                try { mc.close() } catch (_: Exception) {}
            }
        } else if (techs.any { it == "MifareUltralight" }) {
            security = "NFC Tag Type 2 (Ultralight) -- biasanya memang didesain terbuka/tidak terenkripsi untuk data publik, bukan untuk akses aman"
            securityLevel = "ok"
        } else if (techs.any { it == "IsoDep" }) {
            security = "ISO14443-4 (kemungkinan smart card lebih aman, mis. DESFire/e-KTP) -- tidak bisa dibaca sembarangan tanpa kunci/sertifikat yang sesuai"
            securityLevel = "ok"
        } else if (techs.any { it == "NfcV" }) {
            security = "ISO15693 (Vicinity) -- umum dipakai kartu akses/perpustakaan jarak baca lebih jauh"
            securityLevel = "unknown"
        }

        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                val msg: NdefMessage? = ndef.cachedNdefMessage ?: run {
                    try { ndef.connect(); val m = ndef.ndefMessage; ndef.close(); m } catch (e: Exception) { null }
                }
                if (msg != null) {
                    val parts = ArrayList<String>()
                    for (record in msg.records) {
                        val payload = record.payload
                        if (payload.isEmpty()) continue
                        try {
                            if (record.tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN &&
                                record.type.contentEquals(android.nfc.NdefRecord.RTD_TEXT)
                            ) {
                                val langLen = payload[0].toInt() and 0x3F
                                parts.add(String(payload, langLen + 1, payload.size - langLen - 1, Charsets.UTF_8))
                            } else if (record.tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN &&
                                record.type.contentEquals(android.nfc.NdefRecord.RTD_URI)
                            ) {
                                parts.add(String(payload, 1, payload.size - 1, Charsets.UTF_8))
                            } else {
                                parts.add(String(payload, Charsets.UTF_8))
                            }
                        } catch (e: Exception) {
                            parts.add("(data biner, ${payload.size} byte)")
                        }
                    }
                    if (parts.isNotEmpty()) ndefText = parts.joinToString(" | ")
                }
            }
        } catch (_: Exception) {
        }

        val json = JSONObject().apply {
            put("uid", uidHex)
            put("techs", org.json.JSONArray(techs))
            put("security", security)
            put("securityLevel", securityLevel)
            put("ndefText", ndefText ?: JSONObject.NULL)
            put("mifareNote", mifareNote ?: JSONObject.NULL)
        }
        js("window.onNfcTag && window.onNfcTag(${JSONObject.quote(json.toString())})")
    }
}
