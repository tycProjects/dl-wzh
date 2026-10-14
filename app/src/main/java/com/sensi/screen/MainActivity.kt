package com.sensi.screen

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var tvStatus: TextView
    private lateinit var etW: EditText
    private lateinit var etH: EditText
    private lateinit var etD: EditText
    private lateinit var swEnable: SwitchCompat
    private lateinit var rgMode: RadioGroup
    private var rootState = "chưa kiểm tra"
    @Volatile private var sessionCheckInProgress = false

    private val permListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
        runOnUiThread { refresh() }
    }
    private val binderListener = Shizuku.OnBinderReceivedListener { runOnUiThread { refresh() } }
    private val deadListener = Shizuku.OnBinderDeadListener { runOnUiThread { refresh() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AuthSession.token(this).isNullOrBlank()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        val scroll = findViewById<android.widget.ScrollView>(android.R.id.content).getChildAt(0) as? android.widget.LinearLayout
        if (AuthSession.role(this) == "admin") {
            scroll?.addView(Button(this).apply {
                text = "Quản trị Admin · Quản lý tài khoản"
                setOnClickListener { startActivity(Intent(this@MainActivity, AdminActivity::class.java)) }
            }, 0, android.widget.LinearLayout.LayoutParams(-1, (48 * resources.displayMetrics.density).toInt()))
        }
        scroll?.addView(Button(this).apply {
            text = "Đăng xuất"
            setOnClickListener {
                AuthSession.clear(this@MainActivity)
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            }
        }, if (AuthSession.role(this) == "admin") 1 else 0, android.widget.LinearLayout.LayoutParams(-1, (44 * resources.displayMetrics.density).toInt()))
        prefs = Prefs(this)

        tvStatus = findViewById(R.id.tvStatus)
        etW = findViewById(R.id.etW)
        etH = findViewById(R.id.etH)
        etD = findViewById(R.id.etD)
        swEnable = findViewById(R.id.swEnable)
        rgMode = findViewById(R.id.rgMode)

        etW.setText(prefs.width.toString())
        etH.setText(prefs.height.toString())
        if (prefs.density > 0) etD.setText(prefs.density.toString())

        when (prefs.mode) {
            "shizuku" -> rgMode.check(R.id.rbShizuku)
            "root" -> rgMode.check(R.id.rbRoot)
            else -> rgMode.check(R.id.rbAuto)
        }
        rgMode.setOnCheckedChangeListener { _, id ->
            prefs.mode = when (id) {
                R.id.rbShizuku -> "shizuku"
                R.id.rbRoot -> "root"
                else -> "auto"
            }
            refresh()
        }

        swEnable.isChecked = prefs.enabled
        swEnable.setOnCheckedChangeListener { _, checked ->
            prefs.enabled = checked
            if (!checked) ScreenController.reset(this)
        }

        findViewById<Button>(R.id.btnShizuku).setOnClickListener { requestShizuku() }
        findViewById<Button>(R.id.btnRoot).setOnClickListener { checkRoot() }
        findViewById<Button>(R.id.btnApps).setOnClickListener {
            saveFields()
            startActivity(Intent(this, AppPickerActivity::class.java))
        }
        findViewById<Button>(R.id.btnA11y).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.btnTest).setOnClickListener {
            if (!saveFields()) return@setOnClickListener
            ScreenController.apply(
                this, "manual",
                Profile(prefs.width, prefs.height, prefs.density),
                isManual = true
            ) { r ->
                runOnUiThread {
                    toast(if (r.ok) "Đã áp dụng. Bấm \"Khôi phục\" để trả về như cũ" else "Lỗi: ${r.output}")
                }
            }
        }
        findViewById<Button>(R.id.btnReset).setOnClickListener {
            ScreenController.reset(this, force = true) { r ->
                runOnUiThread { toast(if (r.ok) "Đã khôi phục màn hình" else "Lỗi: ${r.output}") }
            }
        }

        try {
            Shizuku.addRequestPermissionResultListener(permListener)
            Shizuku.addBinderReceivedListener(binderListener)
            Shizuku.addBinderDeadListener(deadListener)
        } catch (_: Throwable) {
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
        validateSessionWithServer()
    }

    // A user may have been locked by an Admin while a JWT is still stored locally.
    // Ask the backend on each resume; a temporary network failure does not erase the session.
    private fun validateSessionWithServer() {
        if (sessionCheckInProgress || AuthSession.token(this).isNullOrBlank()) return
        val base = AuthSession.apiUrl(this).trimEnd('/')
        if (!base.startsWith("https://")) return
        sessionCheckInProgress = true
        Thread {
            var unauthorized = false
            var role: String? = null
            try {
                val conn = (java.net.URL(base + "/api/auth/me").openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "GET"; connectTimeout = 8000; readTimeout = 8000
                    setRequestProperty("Authorization", "Bearer ${AuthSession.token(this)}")
                }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val response = org.json.JSONObject(stream?.bufferedReader()?.use { it.readText() }.orEmpty().ifBlank { "{}" })
                if (code == 401) unauthorized = true
                if (code in 200..299) role = response.optJSONObject("user")?.optString("role", "user")
                conn.disconnect()
            } catch (_: Exception) { }
            runOnUiThread {
                sessionCheckInProgress = false
                if (unauthorized && !isFinishing) {
                    AuthSession.clear(this)
                    Toast.makeText(this, "Tài khoản đã bị khóa hoặc phiên đăng nhập hết hạn.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } else if (role != null) AuthSession.saveRole(this, role!!)
            }
        }.start()
    }

    override fun onDestroy() {
        try {
            Shizuku.removeRequestPermissionResultListener(permListener)
            Shizuku.removeBinderReceivedListener(binderListener)
            Shizuku.removeBinderDeadListener(deadListener)
        } catch (_: Throwable) {
        }
        super.onDestroy()
    }

    private fun requestShizuku() {
        try {
            if (!ShellRunner.shizukuRunning()) {
                toast("Shizuku chưa chạy. Mở app Shizuku và khởi động nó trước.")
                return
            }
            if (ShellRunner.shizukuReady()) {
                toast("Đã có quyền Shizuku")
            } else {
                Shizuku.requestPermission(100)
            }
        } catch (t: Throwable) {
            toast("Không tìm thấy Shizuku: ${t.message}")
        }
    }

    private fun checkRoot() {
        rootState = "đang kiểm tra…"
        refresh()
        Thread {
            val ok = ShellRunner.rootAvailable()
            rootState = if (ok) "có ✓" else "không có"
            runOnUiThread { refresh() }
        }.start()
    }

    /** Đọc 3 ô nhập, kiểm tra rồi lưu. Trả về false nếu giá trị sai. */
    private fun saveFields(): Boolean {
        val w = etW.text.toString().trim().toIntOrNull()
        val h = etH.text.toString().trim().toIntOrNull()
        val dText = etD.text.toString().trim()
        val d = if (dText.isEmpty()) 0 else dText.toIntOrNull()
        if (w == null || h == null || d == null || w !in 200..10000 || h !in 200..10000 || (d != 0 && d !in 72..800)) {
            toast("Rộng/Cao phải từ 200 đến 10000, density từ 72 đến 800 (hoặc để trống)")
            return false
        }
        prefs.width = w
        prefs.height = h
        prefs.density = d
        return true
    }

    private fun refresh() {
        val shizuku = when {
            ShellRunner.shizukuReady() -> "sẵn sàng ✓"
            ShellRunner.shizukuRunning() -> "đang chạy, chưa cấp quyền"
            else -> "chưa chạy"
        }
        val a11y = (Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: "").contains(packageName)
        tvStatus.text = "Shizuku: $shizuku\n" +
                "Root: $rootState\n" +
                "Dịch vụ Trợ năng: ${if (a11y) "dabat ✓" else "CHUAbat (canbatdetudong theo ungdung)"}\n" +
                "Ứng dụng đã chọn: ${prefs.apps().size}"
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
