package com.glassdex.analyzer.analysis

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ln

/** Reads Windows PE (.exe / .dll) headers: sections, entropy, imports, packer hints. */
object PeParser {

    private val SUSPICIOUS_APIS = setOf(
        "VirtualAlloc", "VirtualAllocEx", "VirtualProtect", "WriteProcessMemory", "ReadProcessMemory",
        "CreateRemoteThread", "NtUnmapViewOfSection", "SetWindowsHookExA", "SetWindowsHookExW",
        "GetAsyncKeyState", "GetKeyState", "URLDownloadToFileA", "URLDownloadToFileW", "WinExec",
        "ShellExecuteA", "ShellExecuteW", "IsDebuggerPresent", "CheckRemoteDebuggerPresent",
        "RegSetValueExA", "RegSetValueExW", "CryptEncrypt", "InternetOpenA", "InternetOpenUrlA",
        "AdjustTokenPrivileges", "OpenProcessToken", "LoadLibraryA", "GetProcAddress", "CreateServiceA",
        "CreateServiceW", "NtQueryInformationProcess", "QueueUserAPC", "SetThreadContext", "ResumeThread"
    )

    fun parse(file: File): PeInfo? {
        RandomAccessFile(file, "r").use { raf ->
            val head = ByteArray(minOf(raf.length(), 4096L).toInt())
            raf.readFully(head)
            val h = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN)
            if (head.size < 0x40 || h.getShort(0).toInt() != 0x5A4D) return null
            val peOff = h.getInt(0x3C)
            if (peOff <= 0 || peOff + 256 > raf.length()) return null

            val hdr = ByteArray(1024.coerceAtMost((raf.length() - peOff).toInt()))
            raf.seek(peOff.toLong()); raf.readFully(hdr)
            val b = ByteBuffer.wrap(hdr).order(ByteOrder.LITTLE_ENDIAN)
            if (b.getInt(0) != 0x00004550) return null

            val machine = b.getShort(4).toInt() and 0xFFFF
            val nSections = b.getShort(6).toInt() and 0xFFFF
            val timestamp = b.getInt(8).toLong() and 0xFFFFFFFFL
            val optSize = b.getShort(20).toInt() and 0xFFFF
            val characteristics = b.getShort(22).toInt() and 0xFFFF
            val optOff = 24
            val magic = b.getShort(optOff).toInt() and 0xFFFF
            val is64 = magic == 0x20B
            val entry = b.getInt(optOff + 16).toLong() and 0xFFFFFFFFL
            val subsystem = b.getShort(optOff + 68).toInt() and 0xFFFF
            val ddOff = optOff + if (is64) 112 else 96
            val importRva = b.getInt(ddOff + 8).toLong() and 0xFFFFFFFFL
            val certDir = ddOff + 4 * 8
            val certSize = b.getInt(certDir + 4)
            val clrRva = b.getInt(ddOff + 14 * 8)

            // sections
            val secOff = peOff + optOff + optSize
            val sections = ArrayList<PeSection>()
            val rawSecs = ArrayList<LongArray>() // va, vsize, rawptr, rawsize
            raf.seek(secOff.toLong())
            val sh = ByteArray(40 * nSections)
            raf.readFully(sh)
            val sb = ByteBuffer.wrap(sh).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until nSections) {
                val o = i * 40
                val name = String(sh, o, 8, Charsets.ISO_8859_1).trimEnd('\u0000')
                val vsize = sb.getInt(o + 8).toLong() and 0xFFFFFFFFL
                val va = sb.getInt(o + 12).toLong() and 0xFFFFFFFFL
                val rsize = sb.getInt(o + 16).toLong() and 0xFFFFFFFFL
                val rptr = sb.getInt(o + 20).toLong() and 0xFFFFFFFFL
                val flags = sb.getInt(o + 36)
                rawSecs.add(longArrayOf(va, vsize, rptr, rsize))
                val ent = entropy(raf, rptr, rsize)
                sections.add(PeSection(name, vsize, rsize, ent, flags and 0x20000000 != 0, flags and 0x80000000.toInt() != 0))
            }

            fun rvaToOff(rva: Long): Long {
                for (s in rawSecs) if (rva >= s[0] && rva < s[0] + maxOf(s[1], s[3])) return rva - s[0] + s[2]
                return -1
            }

            // imports
            val imports = LinkedHashMap<String, List<String>>()
            try {
                val io = rvaToOff(importRva)
                if (io > 0) {
                    var d = io
                    for (n in 0 until 200) {
                        raf.seek(d); val desc = ByteArray(20); raf.readFully(desc)
                        val db = ByteBuffer.wrap(desc).order(ByteOrder.LITTLE_ENDIAN)
                        val oft = db.getInt(0).toLong() and 0xFFFFFFFFL
                        val nameRva = db.getInt(12).toLong() and 0xFFFFFFFFL
                        val ft = db.getInt(16).toLong() and 0xFFFFFFFFL
                        if (nameRva == 0L && ft == 0L) break
                        val dll = readCString(raf, rvaToOff(nameRva))
                        val thunk = rvaToOff(if (oft != 0L) oft else ft)
                        val funcs = ArrayList<String>()
                        if (thunk > 0) {
                            val step = if (is64) 8 else 4
                            var t = thunk
                            for (k in 0 until 2000) {
                                raf.seek(t); val tb = ByteArray(step); raf.readFully(tb)
                                val tbb = ByteBuffer.wrap(tb).order(ByteOrder.LITTLE_ENDIAN)
                                val v = if (is64) tbb.getLong() else tbb.getInt().toLong() and 0xFFFFFFFFL
                                if (v == 0L) break
                                val ordinalFlag = if (is64) v < 0 else v and 0x80000000L != 0L
                                if (!ordinalFlag) {
                                    val no = rvaToOff(v and 0x7FFFFFFFL)
                                    if (no > 0) funcs.add(readCString(raf, no + 2))
                                } else funcs.add("#" + (v and 0xFFFF))
                                t += step
                            }
                        }
                        imports[dll] = funcs
                        d += 20
                    }
                }
            } catch (_: Throwable) { }

            val allFns = imports.values.flatten().toSet()
            val suspicious = SUSPICIOUS_APIS.filter { it in allFns }

            val packer = detectPacker(sections, allFns.size)
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm 'UTC'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

            return PeInfo(
                is64 = is64,
                machine = when (machine) { 0x14c -> "x86"; 0x8664 -> "x64"; 0x1c0 -> "ARM"; 0xaa64 -> "ARM64"; else -> "0x" + machine.toString(16) },
                subsystem = when (subsystem) { 2 -> "GUI"; 3 -> "Console"; 1 -> "Native"; else -> subsystem.toString() },
                isDll = characteristics and 0x2000 != 0,
                timestamp = if (timestamp == 0L) "n/a" else fmt.format(Date(timestamp * 1000)),
                entryPoint = "0x" + entry.toString(16),
                sections = sections,
                imports = imports,
                suspiciousImports = suspicious,
                packer = packer,
                signed = certSize > 0,
                dotNet = clrRva != 0
            )
        }
    }

    private fun readCString(raf: RandomAccessFile, off: Long): String {
        if (off < 0) return ""
        raf.seek(off)
        val sb = StringBuilder()
        for (i in 0 until 256) {
            val c = raf.read()
            if (c <= 0) break
            sb.append(c.toChar())
        }
        return sb.toString()
    }

    private fun entropy(raf: RandomAccessFile, off: Long, size: Long): Double {
        if (size <= 0 || off <= 0) return 0.0
        val len = minOf(size, 4L * 1024 * 1024).toInt()
        val data = ByteArray(len)
        raf.seek(off)
        val n = raf.read(data)
        if (n <= 0) return 0.0
        val freq = IntArray(256)
        for (i in 0 until n) freq[data[i].toInt() and 0xFF]++
        var e = 0.0
        for (f in freq) if (f > 0) { val p = f.toDouble() / n; e -= p * (ln(p) / ln(2.0)) }
        return e
    }

    private fun detectPacker(sections: List<PeSection>, importCount: Int): String? {
        val names = sections.map { it.name.lowercase() }
        return when {
            names.any { it.startsWith("upx") } -> "UPX"
            names.any { it == ".vmp0" || it == ".vmp1" } -> "VMProtect"
            names.any { it == ".themida" || it == ".winlice" } -> "Themida"
            names.any { it == ".aspack" || it == ".adata" } -> "ASPack"
            names.any { it == ".petite" } -> "Petite"
            names.any { it == ".mpress1" || it == ".mpress2" } -> "MPRESS"
            sections.any { it.entropy > 7.3 && it.executable } && importCount < 15 -> "Unknown packer (high entropy, few imports)"
            else -> null
        }
    }
}
