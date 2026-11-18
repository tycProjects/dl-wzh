package com.glassdex.analyzer.analysis

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Decodes Android binary XML (AndroidManifest.xml and res/*.xml inside an APK)
 * back into readable XML text, and exposes a lightweight element tree.
 */
class AxmlParser(data: ByteArray) {

    class Node(val name: String, val attrs: LinkedHashMap<String, String>) {
        val children = mutableListOf<Node>()
    }

    private val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
    private var strings: List<String> = emptyList()
    val root: Node?
    val xml: String

    private val sb = StringBuilder()

    init {
        var rootNode: Node? = null
        try {
            rootNode = parse()
        } catch (_: Throwable) {
        }
        root = rootNode
        xml = sb.toString()
    }

    private fun parse(): Node? {
        if (buf.limit() < 8 || buf.getShort(0).toInt() != 0x0003) return null
        buf.position(8)
        val stack = ArrayDeque<Node>()
        var root: Node? = null
        val nsMap = LinkedHashMap<String, String>()
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        var indent = 0
        var pendingNs = mutableListOf<Pair<String, String>>()

        while (buf.remaining() >= 8) {
            val start = buf.position()
            val type = buf.getShort().toInt() and 0xFFFF
            buf.getShort() // header size
            val size = buf.getInt()
            when (type) {
                0x0001 -> strings = readStringPool(start)
                0x0100 -> { // start namespace
                    buf.getInt(); buf.getInt()
                    val prefix = str(buf.getInt()); val uri = str(buf.getInt())
                    nsMap[uri] = prefix
                    pendingNs.add(prefix to uri)
                }
                0x0102 -> { // start element
                    buf.getInt(); buf.getInt()
                    buf.getInt() // ns
                    val name = str(buf.getInt())
                    buf.getShort(); buf.getShort() // attr start, size
                    val attrCount = buf.getShort().toInt() and 0xFFFF
                    buf.getShort(); buf.getShort(); buf.getShort()
                    val attrs = LinkedHashMap<String, String>()
                    val rendered = mutableListOf<String>()
                    for (i in 0 until attrCount) {
                        val ns = buf.getInt()
                        val an = str(buf.getInt())
                        val raw = buf.getInt()
                        buf.getShort(); buf.getByte()
                        val dtype = buf.getByte().toInt() and 0xFF
                        val data = buf.getInt()
                        val value = if (raw >= 0) str(raw) else typed(dtype, data)
                        val prefix = if (ns >= 0) nsMap[str(ns)]?.let { "$it:" } ?: "" else ""
                        attrs[an] = value
                        rendered.add("$prefix$an=\"${esc(value)}\"")
                    }
                    val node = Node(name, attrs)
                    stack.lastOrNull()?.children?.add(node)
                    if (root == null) root = node
                    stack.addLast(node)

                    sb.append("  ".repeat(indent)).append('<').append(name)
                    for ((p, u) in pendingNs) sb.append(" xmlns:$p=\"$u\"")
                    pendingNs = mutableListOf()
                    for (r in rendered) sb.append(' ').append(r)
                    sb.append(">\n")
                    indent++
                }
                0x0103 -> { // end element
                    buf.getInt(); buf.getInt(); buf.getInt()
                    val name = str(buf.getInt())
                    indent = (indent - 1).coerceAtLeast(0)
                    sb.append("  ".repeat(indent)).append("</").append(name).append(">\n")
                    if (stack.isNotEmpty()) stack.removeLast()
                }
            }
            buf.position(start + size)
        }
        return root
    }

    private fun readStringPool(chunkStart: Int): List<String> {
        val count = buf.getInt()
        buf.getInt() // style count
        val flags = buf.getInt()
        val stringsStart = buf.getInt()
        buf.getInt()
        val utf8 = flags and (1 shl 8) != 0
        val offsets = IntArray(count) { buf.getInt() }
        val base = chunkStart + stringsStart
        return List(count) { i ->
            var p = base + offsets[i]
            try {
                if (utf8) {
                    var l = buf.get(p).toInt() and 0xFF; p++
                    if (l and 0x80 != 0) { p++ }
                    var bl = buf.get(p).toInt() and 0xFF; p++
                    if (bl and 0x80 != 0) { bl = ((bl and 0x7F) shl 8) or (buf.get(p).toInt() and 0xFF); p++ }
                    val bytes = ByteArray(bl); for (j in 0 until bl) bytes[j] = buf.get(p + j)
                    String(bytes, Charsets.UTF_8)
                } else {
                    var len = buf.getShort(p).toInt() and 0xFFFF; p += 2
                    if (len and 0x8000 != 0) { len = ((len and 0x7FFF) shl 16) or (buf.getShort(p).toInt() and 0xFFFF); p += 2 }
                    val chars = CharArray(len) { j -> buf.getShort(p + j * 2).toInt().toChar() }
                    String(chars)
                }
            } catch (_: Throwable) { "" }
        }
    }

    private fun str(i: Int) = if (i in strings.indices) strings[i] else ""

    private fun typed(type: Int, data: Int): String = when (type) {
        0x01 -> "@0x" + Integer.toHexString(data)
        0x02 -> "?0x" + Integer.toHexString(data)
        0x03 -> str(data)
        0x04 -> java.lang.Float.intBitsToFloat(data).toString()
        0x10 -> data.toString()
        0x11 -> "0x" + Integer.toHexString(data)
        0x12 -> if (data != 0) "true" else "false"
        else -> if (type in 0x1c..0x1f) "#" + Integer.toHexString(data) else data.toString()
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;")
}
