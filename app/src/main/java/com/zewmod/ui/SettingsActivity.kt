package com.zewmod.ui

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.zewmod.hack.*

/**
 * ZewMod — Settings UI
 *
 * Persistent settings via SharedPreferences
 * (pattern yang ditemukan di DEX: putBoolean, SharedPreferences.Editor)
 *
 * Settings yang tersedia:
 *
 * ═══ ESP ═══════════════════════════════════════
 *  [x] Box ESP           → boxEsp
 *  [x] Skeleton ESP      → skeletonEsp
 *  [x] Name ESP          → nameEsp
 *  [x] HP Bar            → hpEsp
 *  [x] Distance          → distanceEsp
 *  [ ] Snaplines         → lineEsp
 *  [ ] Chams             → chams
 *  Color: Enemy / Team   (via ColorPicker popup)
 *  Line Width: 1–5       (SeekBar)
 *
 * ═══ AIMBOT ════════════════════════════════════
 *  [x] Enabled           → aimbot.enabled
 *  FOV: 5–180°           → fovDegrees (SeekBar)
 *  Smooth: 0.0–1.0       → smoothing  (SeekBar)
 *  Bone: HEAD/CHEST/HIPS → targetBone (Spinner)
 *  [x] Visible only      → visibleOnly
 *  [x] Team check        → teamCheck
 *  [ ] Predict movement  → predictMovement
 *
 * ═══ PROXY ═════════════════════════════════════
 *  [x] Enabled           → proxy.enabled
 *  Local port: 1024–9999 → localPort
 *  Frame interval: 1–10  → frameInterval
 *
 * ═══ PATCH ═════════════════════════════════════
 *  Target package        → ModService.targetGamePackage
 *  [btn] Re-inject patch
 *  [btn] Download latest patch
 *
 * Semua setting disimpan ke SharedPreferences "zewmod_settings"
 * dan di-load ulang saat ModService start.
 */
class SettingsActivity : AppCompatActivity() {

    private val prefs by lazy {
        getSharedPreferences("zewmod_settings", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_settings)
        loadAllSettings()
    }

    override fun onPause() {
        super.onPause()
        saveAllSettings()
    }

    // ─── LOAD ─────────────────────────────────────────────────

    private fun loadAllSettings() {
        with(HackState) {
            // ESP
            esp.enabled       = prefs.getBoolean("esp_enabled",    false)
            esp.boxEsp        = prefs.getBoolean("esp_box",        true)
            esp.skeletonEsp   = prefs.getBoolean("esp_skeleton",   true)
            esp.nameEsp       = prefs.getBoolean("esp_name",       true)
            esp.hpEsp         = prefs.getBoolean("esp_hp",         true)
            esp.distanceEsp   = prefs.getBoolean("esp_dist",       true)
            esp.lineEsp       = prefs.getBoolean("esp_line",       false)
            esp.chams         = prefs.getBoolean("esp_chams",      false)
            esp.lineWidth     = prefs.getFloat("esp_linewidth",    2f)
            esp.colorEnemy    = prefs.getInt("esp_color_enemy",    android.graphics.Color.RED)
            esp.colorTeam     = prefs.getInt("esp_color_team",     android.graphics.Color.GREEN)

            // Aimbot
            aimbot.enabled         = prefs.getBoolean("aim_enabled",    false)
            aimbot.fovDegrees      = prefs.getFloat("aim_fov",          45f)
            aimbot.smoothing       = prefs.getFloat("aim_smooth",        0.3f)
            aimbot.visibleOnly     = prefs.getBoolean("aim_visonly",     true)
            aimbot.teamCheck       = prefs.getBoolean("aim_teamcheck",   true)
            aimbot.predictMovement = prefs.getBoolean("aim_predict",     false)
            val boneIdx = prefs.getInt("aim_bone", TargetBone.HEAD.ordinal)
            aimbot.targetBone = TargetBone.values().getOrElse(boneIdx) { TargetBone.HEAD }

            // Proxy
            proxy.enabled       = prefs.getBoolean("proxy_enabled",  false)
            proxy.localPort     = prefs.getInt("proxy_port",         7777)
            proxy.frameInterval = prefs.getInt("proxy_interval",     1)
        }
    }

    // ─── SAVE ─────────────────────────────────────────────────

    private fun saveAllSettings() {
        prefs.edit().apply {
            with(HackState) {
                // ESP
                putBoolean("esp_enabled",      esp.enabled)
                putBoolean("esp_box",          esp.boxEsp)
                putBoolean("esp_skeleton",     esp.skeletonEsp)
                putBoolean("esp_name",         esp.nameEsp)
                putBoolean("esp_hp",           esp.hpEsp)
                putBoolean("esp_dist",         esp.distanceEsp)
                putBoolean("esp_line",         esp.lineEsp)
                putBoolean("esp_chams",        esp.chams)
                putFloat("esp_linewidth",      esp.lineWidth)
                putInt("esp_color_enemy",      esp.colorEnemy)
                putInt("esp_color_team",       esp.colorTeam)

                // Aimbot
                putBoolean("aim_enabled",      aimbot.enabled)
                putFloat("aim_fov",            aimbot.fovDegrees)
                putFloat("aim_smooth",         aimbot.smoothing)
                putBoolean("aim_visonly",      aimbot.visibleOnly)
                putBoolean("aim_teamcheck",    aimbot.teamCheck)
                putBoolean("aim_predict",      aimbot.predictMovement)
                putInt("aim_bone",             aimbot.targetBone.ordinal)

                // Proxy
                putBoolean("proxy_enabled",    proxy.enabled)
                putInt("proxy_port",           proxy.localPort)
                putInt("proxy_interval",       proxy.frameInterval)
            }
            apply()
        }
    }
}
