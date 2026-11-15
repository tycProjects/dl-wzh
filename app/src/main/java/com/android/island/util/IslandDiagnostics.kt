package com.android.island.util

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque

/**
 * In-app diagnostic recorder. Off by default. When off, [record] is a single volatile read.
 *
 * When on, it stays cheap: no per-call list copy (the old version copied up to 1800 lines on
 * EVERY record, many times a second during animations), no per-call date formatting, and the
 * UI snapshot is published at most every [PUBLISH_MS].
 */
object IslandDiagnostics {
    private const val MAX_ENTRIES = 600
    private const val PUBLISH_MS = 700L
    private val lock = Any()
    private val entries = ArrayDeque<String>(MAX_ENTRIES)
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var recordingEnabled: Boolean = false
    @Volatile private var publishScheduled = false
    private val startWall = System.currentTimeMillis()
    private val startUptime = SystemClock.uptimeMillis()

    private val _snapshot = MutableStateFlow<List<String>>(emptyList())
    val snapshot = _snapshot.asStateFlow()

    private val publishRunnable = Runnable {
        publishScheduled = false
        _snapshot.value = synchronized(lock) { entries.toList() }
    }

    fun setEnabled(enabled: Boolean) {
        recordingEnabled = enabled
        if (!enabled) {
            // Turning diagnostics off must release the log: up to 600 lines were kept in memory forever before.
            synchronized(lock) { entries.clear() }
            main.post(publishRunnable)
        }
    }

    fun isEnabled(): Boolean = recordingEnabled

    /** Hot-path variant: the detail string is only built when recording is on (no allocation otherwise). */
    inline fun recordLazy(event: String, detail: () -> String) {
        if (isEnabled()) record(event, detail())
    }

    fun record(event: String, detail: String = "") {
        if (!recordingEnabled) return
        val up = SystemClock.uptimeMillis()
        val wall = startWall + (up - startUptime)
        val s = (wall / 1000) % 86_400
        val line = String.format(
            "%02d:%02d:%02d.%03d +%dms | %s%s",
            s / 3600, (s / 60) % 60, s % 60, wall % 1000, up, event,
            if (detail.isEmpty()) "" else " | $detail"
        )
        if (com.android.island.BuildConfig.DEBUG) Log.d("AndroidIsland", line)
        synchronized(lock) {
            if (entries.size >= MAX_ENTRIES) entries.removeFirst()
            entries.addLast(line)
        }
        if (!publishScheduled) {
            publishScheduled = true
            main.postDelayed(publishRunnable, PUBLISH_MS)
        }
    }


    /**
     * Lightweight on-demand memory/CPU sample for field diagnostics. This is deliberately
     * event-driven (mode switch / notification), never sampled on every animation frame.
     * Native PSS is reported separately; GPU composition still requires gfxinfo/Perfetto.
     */
    fun recordResourceSnapshot(reason: String) {
        if (!recordingEnabled) return
        val runtime = Runtime.getRuntime()
        val javaUsedKb = (runtime.totalMemory() - runtime.freeMemory()) / 1024L
        val javaMaxKb = runtime.maxMemory() / 1024L
        val info = android.os.Debug.MemoryInfo()
        android.os.Debug.getMemoryInfo(info)
        val cpuMs = android.os.Process.getElapsedCpuTime()
        record(
            "RESOURCE_SNAPSHOT",
            "reason=$reason javaUsedKb=$javaUsedKb javaMaxKb=$javaMaxKb " +
                "dalvikPssKb=${info.dalvikPss} nativePssKb=${info.nativePss} " +
                "otherPssKb=${info.otherPss} totalPssKb=${info.totalPss} processCpuMs=$cpuMs"
        )
    }

    fun exportText(): String = synchronized(lock) { entries.joinToString("\n") }

    fun clear() {
        synchronized(lock) { entries.clear() }
        _snapshot.value = emptyList()
    }
}
