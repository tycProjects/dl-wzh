package com.flipper.app.xposed

import java.io.BufferedWriter
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Versi TANPA ROOT dari mem_engine.c, ditulis dalam Kotlin. Bedanya penting:
 * ini TIDAK berjalan di proses Flipper, melainkan DI DALAM proses game target
 * sendiri -- dipicu lewat hook Xposed (lihat FlipperXposedHook.kt), yang
 * hanya bisa aktif kalau Flipper + game dijalankan di dalam VirtualXposed
 * (atau Xposed/LSPosed beneran).
 *
 * Karena kode ini jalan DI DALAM proses game itu sendiri, akses ke
 * /proc/self/mem SELALU diizinkan Android untuk proses manapun terhadap
 * dirinya sendiri -- tidak perlu ptrace, tidak perlu root sama sekali.
 *
 * Protokol teksnya sengaja dibuat semirip mungkin dengan mem_engine.c supaya
 * mudah dibandingkan/diaudit, walau beberapa perintah (ATTACH/DETACH/MAPS
 * dengan pid, PROCS) tidak relevan di sini karena targetnya selalu "diri
 * sendiri".
 *
 * Logika scan di bawah ini sudah diuji (lewat program Java terpisah dengan
 * algoritma identik, dijalankan di komputer biasa) sebelum ditaruh di sini:
 * cari nilai tanpa tahu alamatnya, saring sampai presisi, tulis nilai baru,
 * dan dibuktikan tidak menyentuh alamat lain yang kebetulan nilainya sama.
 * Yang BELUM pernah diuji adalah menjalankannya sungguhan di dalam proses
 * yang di-hook oleh VirtualXposed di HP Android asli.
 */
class InProcessEngine(private val mem: RandomAccessFile, private val out: BufferedWriter) {

    private var type = "i32"
    private var typeSize = 4
    private var isFloat = false

    private var addrs = ArrayList<Long>()
    private var raws = ArrayList<ByteArray>()

    companion object {
        private const val SCAN_CHUNK = 1 shl 20 // 1MB
        private const val MAX_REGION_SCAN = 200L * 1024 * 1024
        private const val MAX_CANDIDATES = 200_000
        private const val FLOAT_EPS = 0.0001f
    }

    /** Proses satu baris perintah. Return false kalau klien minta berhenti (EXIT). */
    fun processLine(line: String): Boolean {
        val l = line.trim()
        try {
            when {
                l == "PING" -> reply("PONG")
                l == "EXIT" -> return false
                l.startsWith("SCANNEW ") -> {
                    val parts = l.substring(8).split(" ", limit = 2)
                    if (parts.size < 2) reply("ERR format_salah") else cmdScanNew(parts[0], parts[1])
                }
                l.startsWith("SCANFILTER ") -> {
                    val parts = l.substring(11).split(" ", limit = 3)
                    if (parts.size < 2) reply("ERR format_salah")
                    else cmdScanFilter(parts[0], parts[1], parts.getOrNull(2))
                }
                l == "SCANCOUNT" -> reply("OK ${addrs.size}")
                l.startsWith("SCANPAGE ") -> {
                    val parts = l.substring(9).split(" ")
                    if (parts.size < 2) reply("ERR format_salah")
                    else cmdScanPage(parts[0].toIntOrNull() ?: 0, parts[1].toIntOrNull() ?: 0)
                }
                l == "SCANRESET" -> { addrs = ArrayList(); raws = ArrayList(); reply("OK") }
                l.startsWith("WRITEVAL ") -> {
                    val parts = l.substring(9).split(" ", limit = 3)
                    if (parts.size < 3) reply("ERR format_salah") else cmdWriteVal(parts[0], parts[1], parts[2])
                }
                l.isEmpty() -> {}
                else -> reply("ERR perintah_tidak_dikenal")
            }
        } catch (e: Exception) {
            reply("ERR exception:${e.message}")
        }
        return true
    }

    private fun reply(s: String) {
        out.write(s)
        out.write("\n")
        out.flush()
    }

    private fun setType(t: String): Boolean {
        when (t) {
            "i16" -> { typeSize = 2; isFloat = false }
            "i32" -> { typeSize = 4; isFloat = false }
            "i64" -> { typeSize = 8; isFloat = false }
            "f32" -> { typeSize = 4; isFloat = true }
            else -> return false
        }
        type = t
        return true
    }

    private fun parseValue(s: String): ByteArray? {
        return try {
            val b = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
            if (isFloat) b.putFloat(s.toFloat())
            else {
                val v = s.toLong()
                when (typeSize) {
                    2 -> b.putShort(v.toShort())
                    4 -> b.putInt(v.toInt())
                    else -> b.putLong(v)
                }
            }
            b.array().copyOf(8)
        } catch (e: Exception) {
            null
        }
    }

    private fun rawToI64(b: ByteArray): Long {
        val bb = ByteBuffer.wrap(b, 0, typeSize).order(ByteOrder.LITTLE_ENDIAN)
        return when (typeSize) {
            2 -> bb.short.toLong()
            4 -> bb.int.toLong()
            else -> bb.long
        }
    }

    private fun rawToF32(b: ByteArray): Float = ByteBuffer.wrap(b, 0, 4).order(ByteOrder.LITTLE_ENDIAN).float

    private fun rawToText(b: ByteArray): String = if (isFloat) rawToF32(b).toString() else rawToI64(b).toString()

    private fun valueMatches(cur: ByteArray, prev: ByteArray, op: String, target: ByteArray?): Boolean {
        if (isFloat) {
            val c = rawToF32(cur); val p = rawToF32(prev); val t = target?.let { rawToF32(it) } ?: 0f
            return when (op) {
                "eq" -> kotlin.math.abs(c - t) < FLOAT_EPS
                "changed" -> kotlin.math.abs(c - p) > FLOAT_EPS
                "unchanged" -> kotlin.math.abs(c - p) <= FLOAT_EPS
                "inc" -> c > p + FLOAT_EPS
                "dec" -> c < p - FLOAT_EPS
                else -> false
            }
        } else {
            val c = rawToI64(cur); val p = rawToI64(prev); val t = target?.let { rawToI64(it) } ?: 0L
            return when (op) {
                "eq" -> c == t
                "changed" -> c != p
                "unchanged" -> c == p
                "inc" -> c > p
                "dec" -> c < p
                else -> false
            }
        }
    }

    private fun cmdScanNew(typeArg: String, valStr: String) {
        if (!setType(typeArg)) { reply("ERR tipe_tidak_valid"); return }
        val target = parseValue(valStr) ?: run { reply("ERR nilai_tidak_valid"); return }

        addrs = ArrayList()
        raws = ArrayList()

        val mapsFile = java.io.File("/proc/self/maps")
        if (!mapsFile.canRead()) { reply("ERR buka_maps_gagal"); return }

        val buf = ByteArray(SCAN_CHUNK)
        mapsFile.forEachLine { line ->
            if (addrs.size >= MAX_CANDIDATES) return@forEachLine
            val m = Regex("""^([0-9a-fA-F]+)-([0-9a-fA-F]+)\s+(\S+)""").find(line) ?: return@forEachLine
            val start = m.groupValues[1].toLong(16)
            val end = m.groupValues[2].toLong(16)
            val perm = m.groupValues[3]
            if (perm.length < 2 || perm[0] != 'r' || perm[1] != 'w') return@forEachLine
            val regionLen = end - start
            if (regionLen <= 0 || regionLen > MAX_REGION_SCAN) return@forEachLine

            var off = 0L
            while (off < regionLen && addrs.size < MAX_CANDIDATES) {
                val want = minOf((regionLen - off), SCAN_CHUNK.toLong()).toInt()
                val got = try {
                    mem.seek(start + off)
                    mem.read(buf, 0, want)
                } catch (e: Exception) {
                    -1
                }
                if (got <= 0) break
                var i = 0
                while (i + typeSize <= got) {
                    var eq = true
                    var k = 0
                    while (k < typeSize) { if (buf[i + k] != target[k]) { eq = false; break }; k++ }
                    if (eq) {
                        val raw = ByteArray(8)
                        System.arraycopy(buf, i, raw, 0, typeSize)
                        addrs.add(start + off + i)
                        raws.add(raw)
                        if (addrs.size >= MAX_CANDIDATES) break
                    }
                    i += typeSize
                }
                off += got
            }
        }
        reply("OK ${addrs.size}")
    }

    private fun cmdScanFilter(typeArg: String, op: String, valStrOrNull: String?) {
        if (addrs.isEmpty()) { reply("ERR tidak_ada_kandidat"); return }
        if (!setType(typeArg)) { reply("ERR tipe_tidak_valid"); return }
        var target: ByteArray? = null
        if (op == "eq") {
            target = valStrOrNull?.let { parseValue(it) }
            if (target == null) { reply("ERR nilai_tidak_valid"); return }
        }

        val newAddrs = ArrayList<Long>()
        val newRaws = ArrayList<ByteArray>()
        val cur = ByteArray(8)
        for (idx in addrs.indices) {
            val got = try {
                mem.seek(addrs[idx])
                mem.read(cur, 0, typeSize)
            } catch (e: Exception) {
                -1
            }
            if (got != typeSize) continue
            val curCopy = ByteArray(8)
            System.arraycopy(cur, 0, curCopy, 0, typeSize)
            if (valueMatches(curCopy, raws[idx], op, target)) {
                newAddrs.add(addrs[idx])
                newRaws.add(curCopy)
            }
        }
        addrs = newAddrs
        raws = newRaws
        reply("OK ${addrs.size}")
    }

    private fun cmdScanPage(offset: Int, count: Int) {
        val start = offset.coerceAtLeast(0)
        val end = (start + count.coerceAtLeast(0)).coerceAtMost(addrs.size)
        for (i in start until end) {
            reply("A ${java.lang.Long.toHexString(addrs[i])} ${rawToText(raws[i])}")
        }
        reply("END")
    }

    private fun cmdWriteVal(addrHex: String, typeArg: String, valStr: String) {
        if (!setType(typeArg)) { reply("ERR tipe_tidak_valid"); return }
        val v = parseValue(valStr) ?: run { reply("ERR nilai_tidak_valid"); return }
        val addr = try {
            java.lang.Long.parseUnsignedLong(addrHex, 16)
        } catch (e: Exception) {
            reply("ERR alamat_tidak_valid"); return
        }
        try {
            mem.seek(addr)
            mem.write(v, 0, typeSize)
            reply("OK")
        } catch (e: Exception) {
            reply("ERR tulis_gagal:${e.message}")
        }
    }
}
