package com.zewmod.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.PointF
import android.util.Log
import com.zewmod.hack.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * ZewMod — Patch Broadcast Receiver
 *
 * IPC: IL2CPP patch (Unity) ──broadcast──► Android App ──► HackState
 */
class PatchReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REINJECT    = "com.zewmod.ACTION_REINJECT"
        const val ACTION_RELOAD_CFG  = "com.zewmod.ACTION_RELOAD_CFG"
        const val ACTION_KILLSWITCH  = "com.zewmod.ACTION_KILLSWITCH"
        const val ACTION_ESP_UPDATE  = "com.zewmod.ACTION_ESP_UPDATE"
        private const val TAG = "ZewMod.Receiver"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REINJECT   -> handleReinject(context)
            ACTION_RELOAD_CFG -> handleReloadConfig(context)
            ACTION_KILLSWITCH -> handleKillSwitch()
            ACTION_ESP_UPDATE -> handleEspUpdate(intent)
            else              -> Log.w(TAG, "Unknown action: ${intent.action}")
        }
    }

    private fun handleReinject(context: Context) {
        Log.i(TAG, "Re-inject triggered")
        scope.launch {
            val patcher = ILFixPatch(context)
            val patch   = patcher.loadBundledPatch()
            val pkg     = targetGamePackage
            if (pkg != null && WirelessAdbBridge.isConnected) {
                val ok = WirelessAdbBridge.injectPatchToGame(patch, pkg)
                Log.i(TAG, "Re-inject result: $ok")
            }
        }
    }

    private fun handleReloadConfig(context: Context) {
        scope.launch { com.zewmod.net.ConfigManager(context).fetchAndApply() }
    }

    private fun handleKillSwitch() {
        Log.w(TAG, "KILLSWITCH")
        HackState.apply {
            esp.enabled    = false
            aimbot.enabled = false
            proxy.enabled  = false
            reset()
        }
    }

    /**
     * Parse enemy update dari IL2CPP patch via LocalBroadcast.
     * Packet format: pipe-delimited, satu per baris di "enemy_data".
     * Sekarang: AtomicReference swap via HackState.setEnemies()
     */
    private fun handleEspUpdate(intent: Intent) {
        val raw    = intent.getStringExtra("enemy_data") ?: return
        val relay  = ProxyRelay(HackState.proxy)
        val parsed = raw.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line -> parseEnemyFromPacket(relay.parsePipePacket(line)) }

        // FIX: gunakan setEnemies() — bukan .enemies.clear()/.addAll()
        HackState.setEnemies(parsed)
    }

    private fun parseEnemyFromPacket(p: Map<String, String>): EnemyData? {
        val id   = p["ENEMY"]?.toIntOrNull() ?: return null
        val hp   = p["HP"]?.toIntOrNull() ?: 100
        val team = p["TEAM"]?.toIntOrNull() ?: 0
        val name = p["NAME"] ?: "Unknown"
        val self = p.containsKey("SELF")

        fun pf(key: String): PointF {
            val v = p[key]?.split(",") ?: return PointF()
            return PointF(v.getOrNull(0)?.toFloatOrNull() ?: 0f,
                          v.getOrNull(1)?.toFloatOrNull() ?: 0f)
        }

        fun v3(key: String): FloatArray {
            val v = p[key]?.split(",") ?: return floatArrayOf(0f, 0f, 0f)
            return floatArrayOf(
                v.getOrNull(0)?.toFloatOrNull() ?: 0f,
                v.getOrNull(1)?.toFloatOrNull() ?: 0f,
                v.getOrNull(2)?.toFloatOrNull() ?: 0f
            )
        }

        // FIX: pakai headWorld/rootWorld — headPos sudah dihapus di v4
        return EnemyData(
            id               = id,
            name             = name,
            teamId           = team,
            hp               = hp,
            isLocalPlayer    = self,
            // World positions (dari packet baru)
            rootWorld        = v3("ROOT_W"),
            headWorld        = v3("HEAD_W"),
            spineWorld       = v3("SPINE_W"),
            shoulderWorld    = v3("SHLDR_W"),
            hipsWorld        = v3("HIPS_W"),
            ankleLeftWorld   = v3("ANKLE_L_W"),
            ankleRightWorld  = v3("ANKLE_R_W"),
            toeLeftWorld     = v3("TOE_L_W"),
            toeRightWorld    = v3("TOE_R_W"),
            // Screen coords fallback (format lama)
            rootScreen       = pf("ROOT_SCREEN"),
            headScreen       = pf("HEAD_SCREEN"),
            spineScreen      = pf("SPINE_SCREEN"),
            shoulderScreen   = pf("SHOULDER_SCREEN"),
            hipsScreen       = pf("HIPS_SCREEN"),
            ankleLeftScreen  = pf("LEFT_ANKLE_SCREEN"),
            ankleRightScreen = pf("RIGHT_ANKLE_SCREEN"),
            toeLeftScreen    = pf("LEFT_TOE_SCREEN"),
            toeRightScreen   = pf("RIGHT_TOE_SCREEN"),
            hasWorldPos      = p.containsKey("HEAD_W"),
            isVisible        = p["VIS"] != "0",
            isPrepareAttack  = false,
            rootPos          = v3("ROOT_W").takeIf { it[0] != 0f } ?: floatArrayOf(0f, 0f, 0f),
            forwardVec       = floatArrayOf(0f, 0f, 1f),
        )
    }
}
