package com.killcheat.app.service

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.killcheat.app.state.CheatState
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class VpnCoreService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null
    @Volatile private var running = false

    companion object {
        private const val TAG = "VpnCore"
        private const val VPN_ADDRESS = "10.0.0.2"
        private const val VPN_PREFIX = 32
        // FF/FFMAX game server UDP port range
        private val GAME_PORTS = setOf(10012, 10013, 10014, 10015, 17500)
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopVpn(); return START_NOT_STICKY }
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        try {
            val builder = Builder()
                .setSession("KillCheat")
                .addAddress(VPN_ADDRESS, VPN_PREFIX)
                .addRoute("0.0.0.0", 0)
                .setMtu(1500)
                .setBlocking(true)
                .addDisallowedApplication(packageName) // bypass self

            vpnInterface = builder.establish() ?: return
            running = true
            vpnThread = Thread(::packetLoop, "vpn-loop").also { it.start() }
        } catch (e: Exception) {
            Log.e(TAG, "start error: ${e.message}")
        }
    }

    private fun packetLoop() {
        val inStream  = FileInputStream(vpnInterface!!.fileDescriptor)
        val outStream = FileOutputStream(vpnInterface!!.fileDescriptor)
        val buf       = ByteArray(32767)

        while (running) {
            try {
                val len = inStream.read(buf)
                if (len <= 0) continue
                val raw = buf.copyOf(len)
                val out = processPacket(raw)
                if (out != null) outStream.write(out)
            } catch (e: Exception) {
                if (running) Log.e(TAG, "loop error: ${e.message}")
            }
        }
    }

    private fun processPacket(raw: ByteArray): ByteArray? {
        if (raw.size < 20) return raw
        if ((raw[0].toInt() and 0xF0) shr 4 != 4) return raw     // IPv4 only
        if (raw[9].toInt() and 0xFF != 17) return raw             // UDP only

        val ihl = (raw[0].toInt() and 0x0F) * 4
        if (raw.size < ihl + 8) return raw

        val dstPort = ((raw[ihl + 2].toInt() and 0xFF) shl 8) or (raw[ihl + 3].toInt() and 0xFF)
        val srcPort = ((raw[ihl].toInt() and 0xFF) shl 8) or (raw[ihl + 1].toInt() and 0xFF)

        if (dstPort !in GAME_PORTS && srcPort !in GAME_PORTS) return raw

        val payload = raw.copyOfRange(ihl + 8, raw.size).takeIf { it.isNotEmpty() } ?: return raw

        return when {
            CheatState.isTeleportEnabled -> applyTeleport(raw, ihl, payload)
            CheatState.isFreezeEnabled   -> applyFreeze(raw, ihl, payload, srcPort)
            CheatState.isGhostEnabled    -> applyGhost(raw, ihl, payload)
            else -> raw
        }
    }

    /**
     * Teleport: corrupt sequence bytes in outgoing position packets.
     * Server rolls back to last confirmed position → teleport snap effect.
     */
    private fun applyTeleport(raw: ByteArray, ihl: Int, payload: ByteArray): ByteArray {
        if (payload.size > 8) {
            payload[4] = (payload[4].toInt() xor 0x01).toByte()
            payload[5] = (payload[5].toInt() xor 0x80).toByte()
        }
        return rebuild(raw, ihl, payload)
    }

    /**
     * Freeze: zero position delta in INCOMING enemy state packets.
     * Own outgoing fire packets pass through — damage still registers.
     */
    private fun applyFreeze(raw: ByteArray, ihl: Int, payload: ByteArray, srcPort: Int): ByteArray {
        if (srcPort in GAME_PORTS && payload.size > 16) {
            for (i in 8 until minOf(16, payload.size)) payload[i] = 0x00
        }
        return rebuild(raw, ihl, payload)
    }

    /**
     * Ghost: drop outgoing visibility beacon packets (opcodes 0x11/0x12).
     * Gloo wall placements (0x2A) route normally — they use position anchors.
     */
    private fun applyGhost(raw: ByteArray, ihl: Int, payload: ByteArray): ByteArray? {
        if (payload.isEmpty()) return raw
        val opcode = payload[0].toInt() and 0xFF
        val isVisBeacon = opcode == 0x11 || opcode == 0x12
        if (isVisBeacon) return null  // drop → invisible
        return rebuild(raw, ihl, payload)
    }

    private fun rebuild(original: ByteArray, ihl: Int, newPayload: ByteArray): ByteArray {
        val result = ByteArray(ihl + 8 + newPayload.size)
        original.copyInto(result, 0, 0, ihl + 8)
        newPayload.copyInto(result, ihl + 8)
        // Fix IP total length
        val totalLen = result.size
        result[2] = (totalLen shr 8 and 0xFF).toByte()
        result[3] = (totalLen and 0xFF).toByte()
        recalcChecksum(result, ihl)
        return result
    }

    private fun recalcChecksum(packet: ByteArray, ihl: Int) {
        packet[10] = 0; packet[11] = 0
        var sum = 0
        for (i in 0 until ihl step 2) {
            sum += ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
        }
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        sum = sum.inv()
        packet[10] = (sum shr 8 and 0xFF).toByte()
        packet[11] = (sum and 0xFF).toByte()
    }

    private fun stopVpn() {
        running = false
        vpnThread?.interrupt()
        vpnInterface?.close()
        stopSelf()
    }

    override fun onDestroy() { stopVpn(); super.onDestroy() }
}
