package com.fakesocial.app.data

/**
 * Pure-Kotlin encoder for the list of chat items stored in a single Room column.
 * Uses ASCII record/unit separators, so it needs no JSON library and runs in plain unit tests.
 */
object ItemsCodec {
    private const val REC = '\u001E'
    private const val FLD = '\u001F'

    private fun clean(s: String) = s.replace(REC, ' ').replace(FLD, ' ')

    fun encode(items: List<ChatItem>): String =
        items.joinToString(REC.toString()) {
            listOf(clean(it.author), clean(it.text), if (it.mine) "1" else "0", clean(it.time))
                .joinToString(FLD.toString())
        }

    fun decode(raw: String): List<ChatItem> {
        if (raw.isEmpty()) return emptyList()
        return raw.split(REC).mapNotNull { rec ->
            val p = rec.split(FLD)
            if (p.size < 4) null else ChatItem(p[0], p[1], p[2] == "1", p[3])
        }
    }
}
