package com.android.island.util

import android.content.ComponentCallbacks2
import android.os.SystemClock
import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * The overlay is always "visible" to Android, so the system never sends the UI-hidden trim that normally
 * empties the hardware renderer's caches (GPU resource cache, glyph/font caches). This asks for that trim
 * ourselves whenever the island goes quiet (capsule only, hidden, or screen off). Nothing is lost: the caches
 * are rebuilt on the next draw. It is event-driven and rate limited; it never runs in a loop.
 */
object NativeTrim {
    private const val MIN_TRIM_GAP_MS = 20_000L
    private const val MIN_GC_GAP_MS = 5 * 60_000L
    private var lastTrimAt = 0L
    private var lastGcAt = 0L

    fun trimWhenIdle(reason: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastTrimAt < MIN_TRIM_GAP_MS) return
        lastTrimAt = now
        val ok = runCatching {
            HiddenApiBypass.invoke(
                Class.forName("android.graphics.HardwareRenderer"), null, "trimMemory",
                ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
            )
        }.isSuccess
        // Java heap: one collection, then hand the free pages back to the OS (VMRuntime.trimHeap). The heap
        // keeps its committed size after a GC, so without this the free half still counts in PSS. Off the
        // main thread because trimming can take tens of milliseconds.
        var gc = false
        if (now - lastGcAt >= MIN_GC_GAP_MS) {
            lastGcAt = now
            gc = true
            Thread({
                Runtime.getRuntime().gc()
                runCatching {
                    val vm = Class.forName("dalvik.system.VMRuntime")
                    HiddenApiBypass.invoke(vm, HiddenApiBypass.invoke(vm, null, "getRuntime"), "trimHeap")
                }
            }, "island-trim").apply { isDaemon = true; priority = Thread.MIN_PRIORITY }.start()
        }
        // Drop small caches that only help while the user is busy: news feeds (90 s TTL anyway) and the
        // notification-app icon bitmaps. Both refill on demand.
        com.android.island.ai.NoozraNews.trimCache()
        com.android.island.island.clearAppIconCache()
        IslandDiagnostics.record("IDLE_TRIM", "reason=$reason renderer=$ok gc=$gc")
    }
}
