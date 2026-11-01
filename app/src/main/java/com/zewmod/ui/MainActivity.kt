package com.zewmod.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.zewmod.app.R
import com.zewmod.core.ModService
import com.zewmod.core.WirelessAdbBridge
import com.zewmod.core.targetGamePackage
import com.zewmod.hack.HackState
import com.zewmod.hack.TargetBone
import kotlinx.coroutines.*

/**
 * ZewMod — MainActivity v3
 *
 * Flow Wireless ADB Pairing (tanpa Shizuku):
 *
 *   1. User tap tombol "GHÉP NỐI / CONNECT"
 *   2. App buka Developer Options → Wireless Debugging (Settings panel)
 *   3. User tap "Ghép nối thiết bị bằng mã ghép nối" di Wireless Debugging
 *   4. Sistem tampilkan dialog kode 6 digit + IP:port
 *   5. User ketik kode itu ke dialog di app ini
 *   6. App panggil WirelessAdbBridge.pairWithCode(ip, port, code)
 *   7. Setelah pairing → autoConnect() untuk establish shell session
 *
 * UI Status bar di app:
 *   - "● Wireless ADB: Searching..."
 *   - "● Wireless ADB: Connected"
 *   - "● Wireless ADB: Not Connected"
 */
class MainActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var tvAdbStatus: TextView
    private lateinit var btnPair: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvAdbStatus = findViewById(R.id.tv_shizuku_status)
        btnPair     = findViewById(R.id.btn_pair_adb)

        loadSettings()
        checkOverlayPermission()
        setupToggleListeners()

        // Auto-connect ADB saat app buka
        autoConnectAdb()
    }

    // ── AUTO CONNECT ─────────────────────────────────────────────────────────

    private fun autoConnectAdb() {
        tvAdbStatus.text  = "● Searching..."
        tvAdbStatus.setTextColor(0xFFFFAA00.toInt())

        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                WirelessAdbBridge.autoConnect()
            }
            updateAdbStatusUi(ok)
        }
    }

    private fun updateAdbStatusUi(connected: Boolean) {
        if (connected) {
            tvAdbStatus.text = "● Connected"
            tvAdbStatus.setTextColor(0xFF4CAF50.toInt())
            btnPair.text = "RECONNECT ADB"
        } else {
            tvAdbStatus.text = "● Not Connected"
            tvAdbStatus.setTextColor(0xFFFF4444.toInt())
            btnPair.text = "CONNECT / PAIR"
        }
    }

    // ── BUTTON: PAIR ADB ─────────────────────────────────────────────────────
    //
    // Flow sesuai ảnh 5 & ảnh 6:
    //   1. Buka Developer Options → Wireless Debugging
    //   2. User tap "Ghép nối thiết bị bằng mã ghép nối"
    //   3. Dialog muncul dengan kode 6 digit + IP:port
    //   4. User masukkan kode ke dialog app ini
    //   5. Pairing selesai → auto connect

    fun onPairAdbClicked(v: View) {
        if (WirelessAdbBridge.isConnected) {
            // Sudah connect → reconnect saja
            scope.launch {
                tvAdbStatus.text = "● Reconnecting..."
                tvAdbStatus.setTextColor(0xFFFFAA00.toInt())
                val ok = withContext(Dispatchers.IO) {
                    WirelessAdbBridge.disconnect()
                    WirelessAdbBridge.autoConnect()
                }
                updateAdbStatusUi(ok)
            }
            return
        }

        // Belum connect → buka Wireless Debugging di Developer Options
        openWirelessDebugging()
        showPairingCodeDialog()
    }

    /**
     * Buka Developer Options dengan Wireless Debugging scroll otomatis.
     * Intent ini sama dengan ketuk "Wireless Debugging" di Developer Options.
     */
    private fun openWirelessDebugging() {
        try {
            // Android 11+ (API 30+) — Wireless Debugging ada di Developer Options
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback ke Developer Options generic
            try {
                val intent = Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            } catch (_: Exception) {
                showToast("Buka Settings → Developer Options → Wireless Debugging secara manual")
            }
        }
    }

    /**
     * Dialog input kode pairing 6 digit.
     * Setelah user masukkan kode dari "Ghép nối thiết bị bằng mã ghép nối",
     * app akan pair via adb pair <ip:port> <code>.
     */
    private fun showPairingCodeDialog() {
        val dialogView = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val tvInfo = TextView(this).apply {
            text = "Di Wireless Debugging, tap:\n\"Ghép nối thiết bị bằng mã ghép nối\"\n\nLalu masukkan kode 6 digit + IP:port di bawah:"
            textSize = 13f
            setTextColor(0xFFCCCCCC.toInt())
            setPadding(0, 0, 0, 16)
        }

        val etIpPort = EditText(this).apply {
            hint    = "IP:Port  (vd: 192.168.1.124:46517)"
            textSize = 14f
            setSingleLine(true)
        }

        val etCode = EditText(this).apply {
            hint     = "Kode ghép nối (6 digit)"
            textSize = 18f
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        dialogView.addView(tvInfo)
        dialogView.addView(etIpPort)
        dialogView.addView(View(this).apply { minimumHeight = 12 })
        dialogView.addView(etCode)

        android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Ghép nối Wireless ADB")
            .setView(dialogView)
            .setPositiveButton("GHÉP NỐI") { _, _ ->
                val ipPort = etIpPort.text.toString().trim()
                val code   = etCode.text.toString().trim()
                if (ipPort.isNotBlank() && code.length == 6) {
                    doPairAndConnect(ipPort, code)
                } else {
                    showToast("IP:Port và kode không hợp lệ")
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    /**
     * Pair via `adb pair <ip:port> <code>` lalu connect.
     * WirelessAdbBridge.execPair() gọi adb server endpoint pairing.
     */
    private fun doPairAndConnect(ipPort: String, code: String) {
        tvAdbStatus.text = "● Pairing..."
        tvAdbStatus.setTextColor(0xFFFFAA00.toInt())

        scope.launch {
            val paired = withContext(Dispatchers.IO) {
                WirelessAdbBridge.pairWithCode(ipPort, code)
            }

            if (paired) {
                showToast("✓ Pairing berhasil — connecting...")
                val connected = withContext(Dispatchers.IO) {
                    WirelessAdbBridge.autoConnect()
                }
                updateAdbStatusUi(connected)
                if (!connected) showToast("Pair OK tapi connect gagal — coba lagi")
            } else {
                showToast("✗ Pairing gagal — pastikan kode benar")
                updateAdbStatusUi(false)
            }
        }
    }

    // ── MOD CONTROLS ─────────────────────────────────────────────────────────

    fun onStartClicked(v: View) {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission(); return
        }
        saveSettings()
        startService(Intent(this, ModService::class.java).apply {
            action = ModService.ACTION_START
        })
        showToast("ZewMod started")
    }

    fun onStopClicked(v: View) {
        startService(Intent(this, ModService::class.java).apply {
            action = ModService.ACTION_STOP
        })
        showToast("ZewMod stopped")
    }

    // ── TOGGLE LISTENERS ─────────────────────────────────────────────────────

    private fun setupToggleListeners() {
        val toggleEsp  = findViewById<ToggleButton?>(R.id.toggle_esp)
        val toggleSkel = findViewById<ToggleButton?>(R.id.toggle_esp_skeleton)
        val toggleHp   = findViewById<ToggleButton?>(R.id.toggle_esp_hp)
        val toggleName = findViewById<ToggleButton?>(R.id.toggle_esp_name)
        val toggleAim  = findViewById<ToggleButton?>(R.id.toggle_aimbot)
        val seekFov    = findViewById<SeekBar?>(R.id.seek_fov)
        val tvFovVal   = findViewById<TextView?>(R.id.tv_fov_val)
        val spinnerBone= findViewById<Spinner?>(R.id.spinner_bone)

        // Restore state dari HackState (sudah di-load di loadSettings)
        toggleEsp ?.isChecked = HackState.esp.enabled
        toggleSkel?.isChecked = HackState.esp.skeletonEsp
        toggleHp  ?.isChecked = HackState.esp.hpEsp
        toggleName?.isChecked = HackState.esp.nameEsp
        toggleAim ?.isChecked = HackState.aimbot.enabled
        seekFov   ?.progress  = HackState.aimbot.fovDegrees.toInt()
        tvFovVal  ?.text      = "${HackState.aimbot.fovDegrees.toInt()}°"

        toggleEsp ?.setOnCheckedChangeListener { _, c -> HackState.esp.enabled    = c }
        toggleSkel?.setOnCheckedChangeListener { _, c -> HackState.esp.skeletonEsp = c }
        toggleHp  ?.setOnCheckedChangeListener { _, c -> HackState.esp.hpEsp      = c }
        toggleName?.setOnCheckedChangeListener { _, c -> HackState.esp.nameEsp    = c }
        toggleAim ?.setOnCheckedChangeListener { _, c -> HackState.aimbot.enabled = c }

        seekFov?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                val fov = p.coerceAtLeast(5)
                HackState.aimbot.fovDegrees = fov.toFloat()
                tvFovVal?.text = "$fov°"
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        spinnerBone?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                HackState.aimbot.targetBone = TargetBone.values().getOrElse(pos) { TargetBone.HEAD }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    // ── SETTINGS ─────────────────────────────────────────────────────────────

    private fun loadSettings() {
        val p = getSharedPreferences("zewmod_settings", MODE_PRIVATE)
        with(HackState) {
            esp.enabled       = p.getBoolean("esp_enabled",    true)
            esp.lineEsp       = p.getBoolean("esp_line",       true)
            esp.nameEsp       = p.getBoolean("esp_name",       true)
            esp.hpEsp         = p.getBoolean("esp_hp",         true)
            esp.skeletonEsp   = p.getBoolean("esp_skeleton",   true)
            esp.boxEsp        = p.getBoolean("esp_box",        true)
            aimbot.enabled    = p.getBoolean("aim_enabled",    false)
            aimbot.fovDegrees = p.getFloat("aim_fov",          45f)
            proxy.enabled     = p.getBoolean("proxy_enabled",  false)
            targetGamePackage = p.getString("target_pkg",      null)
        }
    }

    private fun saveSettings() {
        getSharedPreferences("zewmod_settings", MODE_PRIVATE).edit().apply {
            with(HackState) {
                putBoolean("esp_enabled",   esp.enabled)
                putBoolean("esp_line",      esp.lineEsp)
                putBoolean("esp_name",      esp.nameEsp)
                putBoolean("esp_hp",        esp.hpEsp)
                putBoolean("esp_skeleton",  esp.skeletonEsp)
                putBoolean("esp_box",       esp.boxEsp)
                putBoolean("aim_enabled",   aimbot.enabled)
                putFloat("aim_fov",         aimbot.fovDegrees)
                putBoolean("proxy_enabled", proxy.enabled)
                putString("target_pkg",     targetGamePackage)
            }
            apply()
        }
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            showToast("Cần cấp quyền hiển thị trên ứng dụng khác")
        }
    }

    private fun requestOverlayPermission() {
        startActivity(Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        ))
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
