package com.android.island.system

import android.content.ComponentName
import android.util.Log
import com.android.island.util.IslandDiagnostics
import rikka.shizuku.Shizuku

/**
 * Hard restart through Shizuku: force-stop this app, then re-grant the accessibility permission that
 * the force-stop revokes, then reopen the app.
 *
 * Force-stop kills our own process, so nothing running inside it can do the re-grant. The whole
 * sequence is therefore one shell script started through Shizuku and detached from our process; the
 * Shizuku server (shell/root uid) keeps running it after we are gone.
 *
 * Callers must only use this when accessibility was already enabled before the restart.
 */
object ShizukuRestart {
    private const val TAG = "AndroidIslandRestart"

    // Shizuku API 13 made newProcess private (UserService is the "official" route); reflection is the
    // widely used way to run a one-off shell command. Kept by a rule in proguard-rules.pro.
    private val newProcess by lazy(LazyThreadSafetyMode.NONE) {
        Shizuku::class.java.getDeclaredMethod(
            "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java
        ).apply { isAccessible = true }
    }

    fun available(): Boolean {
        ShizukuState.refresh()
        return ShizukuState.status.value == ShizukuStatus.READY
    }

    /** Blocking binder call: run off the main thread. Returns true if the script was started. */
    fun forceStopRestoreAccessibilityAndRelaunch(
        packageName: String,
        userId: Int,
        accessibility: ComponentName,
        launcher: ComponentName
    ): Boolean {
        if (!available()) return false
        val script = buildScript(
            packageName, userId,
            accessibility.flattenToString(), accessibility.flattenToShortString(),
            launcher.flattenToShortString()
        )
        // Script goes in as $0 so it needs no shell quoting. It runs in its own session when setsid
        // exists, and in the background either way, so it outlives the short-lived outer shell (the
        // Shizuku server destroys that when our process dies).
        val outer = "if command -v setsid >/dev/null 2>&1; then " +
            "setsid sh -c \"\$0\" </dev/null >/dev/null 2>&1 & " +
            "else sh -c \"\$0\" </dev/null >/dev/null 2>&1 & fi"
        return runCatching {
            (newProcess.invoke(null, arrayOf("sh", "-c", outer, script), null, null) as? Process)?.let { ShizukuShell.drain(it) }
            IslandDiagnostics.record("SHIZUKU_RESTART_STARTED", "user=$userId")
            true
        }.getOrElse { error ->
            IslandDiagnostics.record("SHIZUKU_RESTART_FAILED", "${error.javaClass.simpleName}: ${error.message}")
            Log.w(TAG, "Unable to start Shizuku restart", error)
            false
        }
    }

    private fun buildScript(pkg: String, user: Int, svcFull: String, svcShort: String, launcher: String): String {
        val get = "settings --user $user get secure enabled_accessibility_services"
        val put = "settings --user $user put secure"
        return listOf(
            "sleep 0.4",
            "am force-stop --user $user $pkg",
            // Android removes the service from the enabled list asynchronously after a force-stop.
            // Wait (max ~6 s) for that, so we do not write the list just before it gets overwritten.
            "i=0",
            "while [ \$i -lt 24 ]; do",
            "  cur=\$($get)",
            "  case \":\$cur:\" in *\":$svcFull:\"*|*\":$svcShort:\"*) sleep 0.25; i=\$((i+1));; *) break;; esac",
            "done",
            "cur=\$($get)",
            "[ \"\$cur\" = null ] && cur=",
            "case \":\$cur:\" in",
            // Still listed (not removed): toggle the master switch so the system rebinds it.
            "  *\":$svcFull:\"*|*\":$svcShort:\"*) $put accessibility_enabled 0; sleep 0.3;;",
            "  *) if [ -z \"\$cur\" ]; then new=\"$svcFull\"; else new=\"\$cur:$svcFull\"; fi",
            "     $put enabled_accessibility_services \"\$new\";;",
            "esac",
            "$put accessibility_enabled 1",
            // Cache trim on every hard restart (requested): runs after the accessibility re-grant, before relaunch.
            "pm trim-caches 999G",
            "am start --user $user -n $launcher"
        ).joinToString("\n")
    }
}
