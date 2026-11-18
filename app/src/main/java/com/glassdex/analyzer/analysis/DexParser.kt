package com.glassdex.analyzer.analysis

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Result of reading one classes.dex. */
class DexResult(
    val classCount: Int,
    val methodCount: Int,
    val strings: List<String>,
    /** "Lpkg/Class;->method" for every method the dex references (own or external). */
    val methodRefs: Set<String>,
    val classNames: List<String>
)

/**
 * Minimal but real DEX reader. It decodes classes, fields, method signatures and the
 * string pool, and writes a readable class skeleton per class (smali-style headers).
 * It does not translate Dalvik bytecode into Java; method bodies are left out.
 */
object DexParser {

    private const val MAX_CLASS_FILES = 40_000

    fun parse(bytes: ByteArray, outDir: File?, writtenSoFar: IntArray): DexResult {
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(bytes.size > 0x70 && bytes[0] == 'd'.code.toByte() && bytes[1] == 'e'.code.toByte() && bytes[2] == 'x'.code.toByte()) {
            "Not a DEX file"
        }
        val strCount = b.getInt(0x38); val strOff = b.getInt(0x3C)
        val typeCount = b.getInt(0x40); val typeOff = b.getInt(0x44)
        val protoCount = b.getInt(0x48); val protoOff = b.getInt(0x4C)
        val fieldCount = b.getInt(0x50); val fieldOff = b.getInt(0x54)
        val methodCount = b.getInt(0x58); val methodOff = b.getInt(0x5C)
        val classCount = b.getInt(0x60); val classOff = b.getInt(0x64)

        // --- string pool ---
        val strings = ArrayList<String>(strCount)
        for (i in 0 until strCount) {
            var p = b.getInt(strOff + i * 4)
            // skip uleb128 utf16 length
            while (bytes[p].toInt() and 0x80 != 0) p++
            p++
            var e = p
            while (e < bytes.size && bytes[e].toInt() != 0) e++
            strings.add(decodeMutf8(bytes, p, e))
        }

        fun typeName(idx: Int): String = if (idx < 0 || idx >= typeCount) "?" else strings[b.getInt(typeOff + idx * 4)]

        fun typeListAt(off: Int): String {
            if (off == 0) return ""
            val n = b.getInt(off)
            val sb = StringBuilder()
            for (i in 0 until n) sb.append(typeName(b.getShort(off + 4 + i * 2).toInt() and 0xFFFF))
            return sb.toString()
        }

        fun protoDesc(idx: Int): String {
            val o = protoOff + idx * 12
            val ret = typeName(b.getInt(o + 4))
            val params = typeListAt(b.getInt(o + 8))
            return "($params)$ret"
        }

        fun methodSig(idx: Int): Triple<String, String, String> {
            val o = methodOff + idx * 8
            val cls = typeName(b.getShort(o).toInt() and 0xFFFF)
            val proto = protoDesc(b.getShort(o + 2).toInt() and 0xFFFF)
            val name = strings[b.getInt(o + 4)]
            return Triple(cls, name, proto)
        }

        val refs = HashSet<String>(methodCount)
        for (i in 0 until methodCount) {
            val (c, n, _) = methodSig(i)
            refs.add("$c->$n")
        }

        // --- classes ---
        val classNames = ArrayList<String>(classCount)
        val outRoot = outDir?.also { it.mkdirs() }

        for (ci in 0 until classCount) {
            val o = classOff + ci * 32
            val clsName = typeName(b.getInt(o))
            val access = b.getInt(o + 4)
            val superIdx = b.getInt(o + 8)
            val ifaceOff = b.getInt(o + 12)
            val srcIdx = b.getInt(o + 16)
            val dataOff = b.getInt(o + 24)
            classNames.add(clsName)

            if (outRoot == null || writtenSoFar[0] >= MAX_CLASS_FILES) continue

            val sb = StringBuilder(512)
            sb.append(".class ").append(accessFlags(access, true)).append(clsName).append('\n')
            if (superIdx != -1) sb.append(".super ").append(typeName(superIdx)).append('\n')
            if (srcIdx != -1 && srcIdx in 0 until strCount) sb.append(".source \"").append(strings[srcIdx]).append("\"\n")
            if (ifaceOff != 0) {
                val n = b.getInt(ifaceOff)
                for (i in 0 until n) sb.append(".implements ").append(typeName(b.getShort(ifaceOff + 4 + i * 2).toInt() and 0xFFFF)).append('\n')
            }
            sb.append('\n')

            if (dataOff != 0) {
                var p = dataOff
                fun uleb(): Int {
                    var r = 0; var s = 0
                    while (true) {
                        val v = bytes[p++].toInt() and 0xFF
                        r = r or ((v and 0x7F) shl s)
                        if (v and 0x80 == 0) break
                        s += 7
                    }
                    return r
                }
                val sf = uleb(); val inf = uleb(); val dm = uleb(); val vm = uleb()

                fun fields(count: Int, label: String) {
                    var idx = 0
                    for (i in 0 until count) {
                        idx += uleb(); val acc = uleb()
                        val fo = fieldOff + idx * 8
                        if (idx >= fieldCount) continue
                        val ftype = typeName(b.getShort(fo + 2).toInt() and 0xFFFF)
                        val fname = strings[b.getInt(fo + 4)]
                        sb.append(".field ").append(accessFlags(acc, false)).append(fname).append(':').append(ftype).append('\n')
                    }
                }
                fun methods(count: Int) {
                    var idx = 0
                    for (i in 0 until count) {
                        idx += uleb(); val acc = uleb(); val code = uleb()
                        if (idx >= methodCount) continue
                        val (_, name, desc) = methodSig(idx)
                        sb.append("\n.method ").append(accessFlags(acc, false)).append(name).append(desc).append('\n')
                        if (code != 0) {
                            val regs = b.getShort(code).toInt() and 0xFFFF
                            val insSize = b.getInt(code + 12)
                            sb.append("    .registers ").append(regs).append("   # bytecode units: ").append(insSize).append('\n')
                        } else sb.append("    # abstract / native\n")
                        sb.append(".end method\n")
                    }
                }
                if (sf + inf > 0) sb.append("# fields\n")
                fields(sf, "static"); fields(inf, "instance")
                methods(dm); methods(vm)
            }

            val rel = clsName.removePrefix("L").removeSuffix(";").replace("..", "_")
            val safe = rel.replace(Regex("[^A-Za-z0-9_/\$.\\-]"), "_")
            val f = File(outRoot, "$safe.smali.txt")
            try {
                f.parentFile?.mkdirs()
                f.writeText(sb.toString())
                writtenSoFar[0]++
            } catch (_: Throwable) { }
        }

        return DexResult(classCount, methodCount, strings, refs, classNames)
    }

    private fun decodeMutf8(b: ByteArray, s: Int, e: Int): String {
        // fast path for ASCII
        var ascii = true
        for (i in s until e) if (b[i] < 0) { ascii = false; break }
        if (ascii) return String(b, s, e - s, Charsets.ISO_8859_1)
        val out = StringBuilder(e - s)
        var i = s
        while (i < e) {
            val c = b[i].toInt() and 0xFF
            when {
                c < 0x80 -> { out.append(c.toChar()); i++ }
                c and 0xE0 == 0xC0 && i + 1 < e -> { out.append((((c and 0x1F) shl 6) or (b[i + 1].toInt() and 0x3F)).toChar()); i += 2 }
                c and 0xF0 == 0xE0 && i + 2 < e -> { out.append((((c and 0x0F) shl 12) or ((b[i + 1].toInt() and 0x3F) shl 6) or (b[i + 2].toInt() and 0x3F)).toChar()); i += 3 }
                else -> { out.append('?'); i++ }
            }
        }
        return out.toString()
    }

    private fun accessFlags(f: Int, isClass: Boolean): String {
        val sb = StringBuilder()
        if (f and 0x1 != 0) sb.append("public ")
        if (f and 0x2 != 0) sb.append("private ")
        if (f and 0x4 != 0) sb.append("protected ")
        if (f and 0x8 != 0) sb.append("static ")
        if (f and 0x10 != 0) sb.append("final ")
        if (!isClass && f and 0x20 != 0) sb.append("synchronized ")
        if (f and 0x100 != 0) sb.append("native ")
        if (f and 0x200 != 0) sb.append("interface ")
        if (f and 0x400 != 0) sb.append("abstract ")
        if (f and 0x4000 != 0) sb.append("enum ")
        return sb.toString()
    }
}
