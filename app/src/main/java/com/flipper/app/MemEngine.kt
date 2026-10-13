package com.flipper.app

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * MODUL 1: jembatan ke "mem_engine", program kecil (lihat app/src/main/cpp/mem_engine.c)
 * yang dikompile lewat CMake dan dijalankan sebagai root (lewat `su`) untuk membaca
 * dan menulis memori proses lain -- sesuatu yang aplikasi Android biasa tidak diizinkan.
 *
 * HP WAJIB SUDAH DI-ROOT. Tanpa root, prepare()/start() akan gagal dan fungsi
 * lain tidak melakukan apa-apa selain mengembalikan null/false.
 *
 * Kelas ini baru "mesin"-nya saja (modul 1). Belum ada logic cari-alamat-koin
 * (itu modul 2) ataupun UI (modul 4) -- pakai uji lewat Logcat dulu, lihat
 * fungsi selfTest() di MainActivity.
 */
class MemEngine(private val context: Context) : MemBackend {

    companion object {
        private const val TAG = "MemEngine"
        private const val CMD_TIMEOUT_MS = 4000L
    }

    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var readerThread: Thread? = null
    private val lineQueue = LinkedBlockingQueue<String>()
    private var binaryPath: String? = null
    private var attachedPid: Int = -1

    /** Salin binary mem_engine (sesuai ABI HP) dari assets ke folder privat aplikasi. */
    fun prepare(): Boolean {
        val abi = Build.SUPPORTED_ABIS.firstOrNull { it == "arm64-v8a" || it == "armeabi-v7a" }
            ?: Build.SUPPORTED_ABIS.firstOrNull()
        if (abi == null) {
            Log.e(TAG, "ABI HP tidak dikenali")
            return false
        }
        val assetPath = "memengine/$abi/memengine"
        val outFile = File(context.filesDir, "memengine")
        return try {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            }
            outFile.setExecutable(true, false)
            outFile.setReadable(true, false)
            binaryPath = outFile.absolutePath
            true
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Binary mem_engine untuk ABI $abi tidak ditemukan di assets. " +
                    "Pastikan project sudah di-build lewat Android Studio (CMake) minimal sekali, " +
                    "bukan hanya dibuka. (${e.message})"
            )
            false
        }
    }

    /**
     * Mulai sesi root: membuka shell `su`, lalu menjalankan binary mem_engine di
     * dalamnya. Akan memicu dialog izin root (Magisk dll) kalau belum pernah disetujui.
     */
    fun start(): Boolean = startInternal(withSu = true)

    /**
     * MODE SAMA-UID (tanpa su, tanpa root): menjalankan mem_engine LANGSUNG,
     * tanpa dibungkus `su`. Ini hanya bisa menempel (ptrace) ke proses lain
     * yang kebetulan berjalan dengan UID Android yang SAMA dengan Flipper
     * saat ini -- itulah yang terjadi kalau Flipper dan game sama-sama
     * di-clone ke dalam app cloner yang sama (BlackBox, Parallel Space, dkk),
     * karena cloner semacam itu menjalankan semua app yang di-clone di
     * bawah satu UID real yang sama. Ini mekanisme yang sama yang dipakai
     * GameGuardian untuk mode "tanpa root"-nya, BUKAN akses root sungguhan.
     */
    fun startSameUid(): Boolean = startInternal(withSu = false)

    private fun startInternal(withSu: Boolean): Boolean {
        val path = binaryPath
        if (path == null) {
            Log.e(TAG, "start() dipanggil sebelum prepare() berhasil")
            return false
        }
        return try {
            val cmd = if (withSu) listOf("su", "-c", path) else listOf(path)
            val p = ProcessBuilder(cmd)
                .redirectErrorStream(false)
                .start()
            process = p
            writer = BufferedWriter(OutputStreamWriter(p.outputStream))
            lineQueue.clear()
            readerThread = Thread {
                try {
                    BufferedReader(InputStreamReader(p.inputStream)).forEachLine { line ->
                        lineQueue.put(line)
                    }
                } catch (_: Exception) {
                    // stream ditutup -- wajar saat stop()/proses root berakhir
                }
            }.apply {
                isDaemon = true
                start()
            }
            val pong = sendCommand("PING", 3000)
            if (pong != "PONG") {
                Log.e(TAG, "Mesin tidak membalas PING (dapat: $pong). Root mungkin ditolak.")
                stop()
                false
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menjalankan sesi root: ${e.message}")
            false
        }
    }

    /** Tempel ke proses lain lewat PID-nya. Butuh start() berhasil duluan. */
    fun attach(pid: Int): Boolean {
        val r = sendCommand("ATTACH $pid", CMD_TIMEOUT_MS) ?: return false
        return if (r == "OK") {
            attachedPid = pid
            true
        } else {
            Log.e(TAG, "attach($pid) gagal: $r")
            false
        }
    }

    fun detach() {
        sendCommand("DETACH", 2000)
        attachedPid = -1
    }

    /** Baca [len] byte dari alamat [addr] di proses yang sedang di-attach. Maks 64KB. */
    fun readMemory(addr: Long, len: Int): ByteArray? {
        if (attachedPid < 0) {
            Log.e(TAG, "readMemory dipanggil tanpa attach")
            return null
        }
        val r = sendCommand("READ ${toHexAddr(addr)} $len", CMD_TIMEOUT_MS) ?: return null
        if (!r.startsWith("OK ")) {
            Log.e(TAG, "readMemory gagal: $r")
            return null
        }
        return hexToBytes(r.substring(3).trim())
    }

    /** Tulis [data] ke alamat [addr] di proses yang sedang di-attach. Maks 64KB. */
    fun writeMemory(addr: Long, data: ByteArray): Boolean {
        if (attachedPid < 0) {
            Log.e(TAG, "writeMemory dipanggil tanpa attach")
            return false
        }
        val hex = bytesToHex(data)
        val r = sendCommand("WRITE ${toHexAddr(addr)} $hex", CMD_TIMEOUT_MS) ?: return false
        if (!r.startsWith("OK")) {
            Log.e(TAG, "writeMemory gagal: $r")
            return false
        }
        return true
    }

    /** Daftar region memori (dari /proc/pid/maps) milik [pid]. */
    fun listMaps(pid: Int): List<String> = readMultiLine("MAPS $pid", "M ", 8000)

    /** Daftar semua proses yang sedang berjalan: Pair(pid, cmdline/nama). */
    fun listProcesses(): List<Pair<Int, String>> =
        readMultiLine("PROCS", "P ", 6000).mapNotNull { line ->
            val sp = line.indexOf(' ')
            if (sp < 0) return@mapNotNull null
            val pid = line.substring(0, sp).toIntOrNull() ?: return@mapNotNull null
            pid to line.substring(sp + 1)
        }

    private fun readMultiLine(cmd: String, prefix: String, timeoutMs: Long): List<String> {
        val w = writer ?: return emptyList()
        lineQueue.clear()
        val out = ArrayList<String>()
        try {
            w.write(cmd)
            w.newLine()
            w.flush()
            val deadline = System.currentTimeMillis() + timeoutMs
            while (true) {
                val remain = deadline - System.currentTimeMillis()
                if (remain <= 0) break
                val line = lineQueue.poll(remain, TimeUnit.MILLISECONDS) ?: break
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

    // ------------------------------------------------- modul 2+3: scan & tulis nilai

    /** Satu baris hasil scan: alamat (heksadesimal, tanpa "0x") dan nilainya sebagai teks. */
    data class ScanRow(val addrHex: String, val value: String)

    /** Mulai pencarian baru dari NOL untuk [type] (i16/i32/i64/f32) bernilai [value]. */
    override fun scanNew(type: String, value: String): Int? {
        val r = sendCommand("SCANNEW $type $value", 20000) ?: return null
        return parseCount(r)
    }

    /**
     * Saring hasil pencarian yang sudah ada. [op] = eq | changed | unchanged | inc | dec.
     * [value] hanya dipakai (dan wajib) untuk op "eq".
     */
    override fun scanFilter(type: String, op: String, value: String?): Int? {
        val cmd = if (op == "eq") "SCANFILTER $type $op ${value.orEmpty()}" else "SCANFILTER $type $op"
        val r = sendCommand(cmd, 10000) ?: return null
        return parseCount(r)
    }

    override fun scanCount(): Int? = parseCount(sendCommand("SCANCOUNT", 3000))

    /** Ambil sebagian hasil scan (untuk ditampilkan di layar), mulai dari [offset] sebanyak [limit]. */
    override fun scanPage(offset: Int, limit: Int): List<ScanRow> =
        readMultiLine("SCANPAGE $offset $limit", "A ", 5000).mapNotNull { line ->
            val sp = line.indexOf(' ')
            if (sp < 0) return@mapNotNull null
            ScanRow(line.substring(0, sp), line.substring(sp + 1))
        }

    override fun scanReset() {
        sendCommand("SCANRESET", 3000)
    }

    /** Tulis satu [value] bertipe [type] langsung ke alamat heksadesimal [addrHex]. */
    override fun writeValue(addrHex: String, type: String, value: String): Boolean =
        sendCommand("WRITEVAL $addrHex $type $value", 4000)?.startsWith("OK") == true

    private fun parseCount(r: String?): Int? {
        if (r == null || !r.startsWith("OK")) {
            if (r != null) Log.e(TAG, "scan gagal: $r")
            return null
        }
        return r.substringAfter(' ', "").trim().toIntOrNull() ?: 0
    }

    fun stop() {
        try {
            sendCommand("EXIT", 1000)
        } catch (_: Exception) {
        }
        readerThread?.interrupt()
        readerThread = null
        try {
            writer?.close()
        } catch (_: Exception) {
        }
        writer = null
        process?.destroy()
        process = null
        attachedPid = -1
    }

    /** Alias supaya MemEngine bisa dipakai lewat interface MemBackend (lihat OverlayService). */
    override fun close() = stop()

    private fun sendCommand(cmd: String, timeoutMs: Long): String? {
        val w = writer ?: return null
        lineQueue.clear()
        return try {
            w.write(cmd)
            w.newLine()
            w.flush()
            lineQueue.poll(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            Log.e(TAG, "sendCommand('$cmd') error: ${e.message}")
            null
        }
    }

    private fun toHexAddr(addr: Long): String = java.lang.Long.toHexString(addr)

    private fun bytesToHex(b: ByteArray): String {
        val sb = StringBuilder(b.size * 2)
        for (x in b) sb.append(String.format("%02x", x))
        return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray? {
        if (hex.isEmpty()) return ByteArray(0)
        if (hex.length % 2 != 0) return null
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            val h = Character.digit(hex[i * 2], 16)
            val l = Character.digit(hex[i * 2 + 1], 16)
            if (h < 0 || l < 0) return null
            out[i] = ((h shl 4) or l).toByte()
        }
        return out
    }
}
