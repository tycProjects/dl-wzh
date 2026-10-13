package com.flipper.app

import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket

/**
 * JALUR TANPA ROOT: menyambung lewat socket TCP (localhost) ke SelfMemServer
 * yang berjalan DI DALAM proses game target -- server itu hanya ada kalau
 * Flipper (sebagai modul Xposed) sudah diaktifkan untuk game itu di dalam
 * VirtualXposed. Kalau belum, connect() akan gagal dan OverlayService
 * menjelaskan langkah setup yang kurang ke pengguna.
 *
 * Port dihitung dari nama paket game dengan rumus yang SAMA dengan
 * SelfMemServer.portFor() di sisi hook -- lihat komentar di sana.
 *
 * CATATAN KEJUJURAN: jalur ini belum pernah diuji di HP Android/VirtualXposed
 * sungguhan (sandbox pengembangan tidak punya keduanya). Algoritma scan di
 * dalamnya (InProcessEngine.kt) sudah diuji identik lewat program Java
 * terpisah di komputer biasa, tapi hal-hal yang khas Android/VirtualXposed
 * (apakah hook benar-benar ter-load, apakah socket loopback bisa dijangkau
 * lintas proses virtual) baru akan ketahuan saat benar-benar dicoba.
 */
class MemEngineSocket(private val pkg: String) : MemBackend {

    companion object {
        private const val TAG = "MemEngineSocket"
        private const val CONNECT_TIMEOUT_MS = 2500
        private const val CMD_TIMEOUT_MS = 20000
    }

    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null

    /** Port yang dipakai untuk game ini (dihitung dari nama paket). */
    fun port(): Int = 20000 + (kotlin.math.abs(pkg.hashCode()) % 999)

    /** Coba sambung. True kalau berhasil dan server membalas PING dengan benar. */
    fun connect(): Boolean {
        return try {
            val s = Socket()
            s.connect(InetSocketAddress("127.0.0.1", port()), CONNECT_TIMEOUT_MS)
            s.soTimeout = CMD_TIMEOUT_MS
            socket = s
            reader = BufferedReader(InputStreamReader(s.getInputStream()))
            writer = BufferedWriter(OutputStreamWriter(s.getOutputStream()))
            send("PING") == "PONG"
        } catch (e: Exception) {
            Log.e(TAG, "connect() gagal untuk $pkg di port ${port()}: ${e.message}")
            close()
            false
        }
    }

    private fun send(cmd: String): String? {
        val w = writer ?: return null
        val r = reader ?: return null
        return try {
            w.write(cmd)
            w.newLine()
            w.flush()
            r.readLine()
        } catch (e: Exception) {
            Log.e(TAG, "send('$cmd') gagal: ${e.message}")
            null
        }
    }

    private fun sendMulti(cmd: String, prefix: String): List<String> {
        val w = writer ?: return emptyList()
        val r = reader ?: return emptyList()
        val out = ArrayList<String>()
        try {
            w.write(cmd)
            w.newLine()
            w.flush()
            while (true) {
                val line = r.readLine() ?: break
                if (line == "END") break
                if (line.startsWith(prefix)) out.add(line.substring(prefix.length))
                else if (line.startsWith("ERR")) {
                    Log.e(TAG, "$cmd gagal: $line")
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "$cmd error: ${e.message}")
        }
        return out
    }

    private fun parseCount(r: String?): Int? {
        if (r == null || !r.startsWith("OK")) {
            if (r != null) Log.e(TAG, "gagal: $r")
            return null
        }
        return r.substringAfter(' ', "").trim().toIntOrNull() ?: 0
    }

    override fun scanNew(type: String, value: String): Int? = parseCount(send("SCANNEW $type $value"))

    override fun scanFilter(type: String, op: String, value: String?): Int? {
        val cmd = if (op == "eq") "SCANFILTER $type $op ${value.orEmpty()}" else "SCANFILTER $type $op"
        return parseCount(send(cmd))
    }

    override fun scanCount(): Int? = parseCount(send("SCANCOUNT"))

    override fun scanPage(offset: Int, limit: Int): List<MemEngine.ScanRow> =
        sendMulti("SCANPAGE $offset $limit", "A ").mapNotNull { line ->
            val sp = line.indexOf(' ')
            if (sp < 0) null else MemEngine.ScanRow(line.substring(0, sp), line.substring(sp + 1))
        }

    override fun scanReset() {
        send("SCANRESET")
    }

    override fun writeValue(addrHex: String, type: String, value: String): Boolean =
        send("WRITEVAL $addrHex $type $value")?.startsWith("OK") == true

    override fun close() {
        try { send("EXIT") } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        socket = null; reader = null; writer = null
    }
}
