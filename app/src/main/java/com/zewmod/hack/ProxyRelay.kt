package com.zewmod.hack

import android.graphics.PointF
import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * ZewMod — ProxyRelay v3 — World Position + Camera Matrix
 *
 * Format packet BARU dari patch (dua loại):
 *
 *  1. CAMERA PACKET (dikirim tiap frame Unity OnPreRender / OnRenderObject):
 *     |CAM|VIEW|m00,m01,...,m15|PROJ|m00,...,m15|SW|1080|SH|1920|
 *
 *  2. ENEMY PACKET (dikirim tiap frame per enemy aktif):
 *     |ENEMY|id|HP|hp|TEAM|tid|SELF|0|NAME|nick|VIS|1|
 *     |ROOT_W|x,y,z|HEAD_W|x,y,z|SPINE_W|x,y,z|
 *     |SHLDR_W|x,y,z|HIPS_W|x,y,z|
 *     |ANKLE_L_W|x,y,z|ANKLE_R_W|x,y,z|
 *     |TOE_L_W|x,y,z|TOE_R_W|x,y,z|
 *
 * BACKWARD COMPAT: Packet lama (HEAD_SCREEN, ROOT_SCREEN, dll) tetap
 * diparsing sebagai fallback. hasWorldPos = false → render pakai legacy coords.
 *
 * PENTING: CameraProjection.updateMatrices() harus dipanggil dari packet
 * Unity SEBELUM enemy packet untuk frame yang sama → VP matrix selalu fresh.
 * Patch sudah diatur urutan: kirim CAM dulu, baru enemy list.
 */
class ProxyRelay(private val cfg: ProxyConfig) {

    companion object {
        private const val TAG      = "ZewMod.Proxy"
        private const val BUF_SIZE = 8192
    }

    private var socket: DatagramSocket? = null
    private var running = false
    private var thread: Thread? = null

    // Buffer per frame — dikumpul sebelum di-flush ke HackState
    private val frameEnemies = ArrayList<EnemyData>(32)
    private var lastCamFrame = 0L

    fun start() {
        if (running) return
        running = true
        thread = Thread(::runServer, "ZewProxyRelay").also {
            it.isDaemon   = true
            it.priority   = Thread.MAX_PRIORITY - 1
            it.start()
        }
        Log.i(TAG, "Proxy started port ${cfg.localPort}")
    }

    fun stop() {
        running = false
        socket?.close()
        thread?.interrupt()
    }

    private fun runServer() {
        socket = DatagramSocket(cfg.localPort)
        val buf = ByteArray(BUF_SIZE)
        while (running) {
            try {
                val pkt = DatagramPacket(buf, BUF_SIZE)
                socket!!.receive(pkt)
                val data = String(pkt.data, 0, pkt.length, Charsets.UTF_8)
                handlePacket(data)
            } catch (e: Exception) {
                if (running) Log.e(TAG, "recv: ${e.message}")
            }
        }
    }

    private fun handlePacket(raw: String) {
        val tokens = raw.split("|").filterNot { it.isBlank() }
        if (tokens.isEmpty()) return

        when (tokens[0]) {
            "CAM"   -> handleCameraPacket(tokens)
            "ENEMY" -> handleEnemyPacket(tokens)
            "FRAME_END" -> {
                // Patch mengirim FRAME_END di akhir tiap frame
                // Flush semua enemy yang dikumpulkan ke HackState
                HackState.setEnemies(ArrayList(frameEnemies))
                frameEnemies.clear()
            }
            else -> handleLegacyPacket(raw)   // format lama pipe-delimited
        }
    }

    // ── CAMERA PACKET ───────────────────────────────────────────────────────
    // |CAM|VIEW|m00,...,m15|PROJ|m00,...,m15|SW|1080|SH|1920|

    private fun handleCameraPacket(t: List<String>) {
        val map = tokenPairs(t, fromIdx = 1)
        val view = map["VIEW"]?.split(",")?.map { it.toFloatOrNull() ?: 0f }
        val proj = map["PROJ"]?.split(",")?.map { it.toFloatOrNull() ?: 0f }
        val sw   = map["SW"]?.toIntOrNull() ?: return
        val sh   = map["SH"]?.toIntOrNull() ?: return
        if (view == null || view.size != 16 || proj == null || proj.size != 16) return

        CameraProjection.updateMatrices(view.toFloatArray(), proj.toFloatArray(), sw, sh)
        HackState.screenWidth  = sw
        HackState.screenHeight = sh
        lastCamFrame = System.nanoTime()
    }

    // ── ENEMY PACKET ────────────────────────────────────────────────────────

    private fun handleEnemyPacket(t: List<String>) {
        val map = tokenPairs(t, fromIdx = 1)

        val id   = map["ENEMY"]?.toIntOrNull() ?: return
        val hp   = map["HP"]?.toIntOrNull() ?: 100
        val team = map["TEAM"]?.toIntOrNull() ?: 0
        val self = map["SELF"] == "1"
        val name = map["NAME"] ?: ""
        val vis  = map["VIS"] != "0"

        // World positions
        val rootW   = parseVec3(map["ROOT_W"])
        val headW   = parseVec3(map["HEAD_W"])
        val spineW  = parseVec3(map["SPINE_W"])
        val shldrW  = parseVec3(map["SHLDR_W"])
        val hipsW   = parseVec3(map["HIPS_W"])
        val ankLW   = parseVec3(map["ANKLE_L_W"])
        val ankRW   = parseVec3(map["ANKLE_R_W"])
        val toeLW   = parseVec3(map["TOE_L_W"])
        val toeRW   = parseVec3(map["TOE_R_W"])

        val hasWorld = rootW != null && headW != null

        // Local player tracking
        if (self) HackState.localPlayerId = id

        val enemy = EnemyData(
            id             = id,
            name           = name,
            teamId         = team,
            hp             = hp,
            isLocalPlayer  = self,
            rootWorld      = rootW   ?: floatArrayOf(0f, 0f, 0f),
            headWorld      = headW   ?: floatArrayOf(0f, 0f, 0f),
            spineWorld     = spineW  ?: floatArrayOf(0f, 0f, 0f),
            shoulderWorld  = shldrW  ?: floatArrayOf(0f, 0f, 0f),
            hipsWorld      = hipsW   ?: floatArrayOf(0f, 0f, 0f),
            ankleLeftWorld = ankLW   ?: floatArrayOf(0f, 0f, 0f),
            ankleRightWorld= ankRW   ?: floatArrayOf(0f, 0f, 0f),
            toeLeftWorld   = toeLW   ?: floatArrayOf(0f, 0f, 0f),
            toeRightWorld  = toeRW   ?: floatArrayOf(0f, 0f, 0f),
            hasWorldPos    = hasWorld,
            isVisible      = vis,
            rootPos        = rootW   ?: floatArrayOf(0f, 0f, 0f),
        )
        frameEnemies.add(enemy)
    }

    // ── LEGACY PACKET (backward compat dengan format lama) ──────────────────

    private fun handleLegacyPacket(raw: String) {
        val map = parsePipePacket(raw)
        val id  = map["ENEMY"]?.toIntOrNull() ?: return

        fun pf(key: String): PointF {
            val v = map[key]?.split(",") ?: return PointF(0f, 0f)
            return PointF(v.getOrNull(0)?.toFloatOrNull() ?: 0f,
                          v.getOrNull(1)?.toFloatOrNull() ?: 0f)
        }

        val enemy = EnemyData(
            id            = id,
            name          = map["NAME"] ?: "",
            teamId        = map["TEAM"]?.toIntOrNull() ?: 0,
            hp            = map["HP"]?.toIntOrNull() ?: 100,
            isLocalPlayer = map["SELF"] == "1",
            hasWorldPos   = false,
            rootScreen    = pf("ROOT_SCREEN"),
            headScreen    = pf("HEAD_SCREEN"),
            spineScreen   = pf("SPINE_SCREEN"),
            shoulderScreen= pf("SHOULDER_SCREEN"),
            hipsScreen    = pf("HIPS_SCREEN"),
            ankleLeftScreen = pf("LEFT_ANKLE_SCREEN"),
            ankleRightScreen= pf("RIGHT_ANKLE_SCREEN"),
            toeLeftScreen   = pf("LEFT_TOE_SCREEN"),
            toeRightScreen  = pf("RIGHT_TOE_SCREEN"),
            rootPos = map["ROOT_SCREEN"]?.split(",")
                ?.let { floatArrayOf(it.getOrNull(0)?.toFloatOrNull() ?: 0f,
                                     it.getOrNull(1)?.toFloatOrNull() ?: 0f, 0f) }
                ?: floatArrayOf(0f, 0f, 0f),
        )

        frameEnemies.add(enemy)

        // Legacy tidak ada FRAME_END — flush langsung per packet
        if (map["YUP"] == "1") {
            HackState.setEnemies(ArrayList(frameEnemies))
            frameEnemies.clear()
        }
    }

    // ── BROADCAST ────────────────────────────────────────────────────────────

    fun broadcastState(enemies: List<EnemyData>, frameCount: Int) {
        if (frameCount % cfg.frameInterval != 0) return
        val sock = socket ?: return
        val remote = InetSocketAddress(InetAddress.getByName(cfg.remoteHost), cfg.remotePort)
        enemies.forEach { e ->
            val payload = buildPacket(e).toByteArray(Charsets.UTF_8)
            try { sock.send(DatagramPacket(payload, payload.size, remote)) }
            catch (_: Exception) {}
        }
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private fun tokenPairs(tokens: List<String>, fromIdx: Int): Map<String, String> {
        val result = HashMap<String, String>(tokens.size / 2 + 1)
        var i = fromIdx
        while (i < tokens.size - 1) {
            result[tokens[i]] = tokens[i + 1]
            i += 2
        }
        return result
    }

    private fun parseVec3(s: String?): FloatArray? {
        val parts = s?.split(",") ?: return null
        if (parts.size < 3) return null
        return floatArrayOf(
            parts[0].toFloatOrNull() ?: return null,
            parts[1].toFloatOrNull() ?: return null,
            parts[2].toFloatOrNull() ?: return null
        )
    }

    fun buildPacket(e: EnemyData): String {
        return buildString {
            append("|ENEMY|${e.id}|HP|${e.hp}|TEAM|${e.teamId}")
            if (e.isLocalPlayer) append("|SELF|1")
            append("|NAME|${e.name}|YUP|1")
            append("|WIDTH|${HackState.screenWidth}|HEIGHT|${HackState.screenHeight}")
            append("|HEAD_SCREEN|${e.headScreen.x},${e.headScreen.y}")
            append("|ROOT_SCREEN|${e.rootScreen.x},${e.rootScreen.y}")
            append("|SPINE_SCREEN|${e.spineScreen.x},${e.spineScreen.y}")
            append("|SHOULDER_SCREEN|${e.shoulderScreen.x},${e.shoulderScreen.y}")
            append("|HIPS_SCREEN|${e.hipsScreen.x},${e.hipsScreen.y}")
            append("|LEFT_ANKLE_SCREEN|${e.ankleLeftScreen.x},${e.ankleLeftScreen.y}")
            append("|RIGHT_ANKLE_SCREEN|${e.ankleRightScreen.x},${e.ankleRightScreen.y}")
            append("|LEFT_TOE_SCREEN|${e.toeLeftScreen.x},${e.toeLeftScreen.y}")
            append("|RIGHT_TOE_SCREEN|${e.toeRightScreen.x},${e.toeRightScreen.y}")
        }
    }

    fun parsePipePacket(raw: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val parts  = raw.split("|").filter { it.isNotBlank() }
        var i = 0
        while (i < parts.size - 1) { result[parts[i]] = parts[i + 1]; i += 2 }
        return result
    }
}
