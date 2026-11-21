package com.android.island.util

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * In-app diagnostic recorder. Off by default. When off, [record] is a single volatile read.
 *
 * When on, it stays cheap: no per-call list copy (the old version copied up to 1800 lines on
 * EVERY record, many times a second during animations), compact date formatting, and the UI
 * snapshot is published at most every [PUBLISH_MS].
 */
object IslandDiagnostics {
    // Full history is append-only on disk; only a small tail is kept in RAM for the settings UI.
    private const val RAM_TAIL = 500
    private const val PUBLISH_MS = 500L
    private val lock = Any()
    private val entries = ArrayDeque<String>(RAM_TAIL)
    private val main = Handler(Looper.getMainLooper())
    private val diskWriter = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "IslandDiagnosticsWriter").apply { isDaemon = true }
    }
    @Volatile private var recordingEnabled: Boolean = false
    @Volatile private var publishScheduled = false
    @Volatile private var logFile: java.io.File? = null
    @Volatile private var historyLoaded = false
    private val _snapshot = MutableStateFlow<List<String>>(emptyList())
    val snapshot = _snapshot.asStateFlow()

    private val publishRunnable = Runnable {
        publishScheduled = false
        if (recordingEnabled) _snapshot.value = synchronized(lock) { entries.toList() }
    }

    fun init(context: android.content.Context) {
        // Initialization is deliberately inert. Do not create/read the log file, publish a
        // snapshot, or start disk work until the user enables Diagnostics in Settings.
        logFile = java.io.File(context.applicationContext.filesDir, "diagnostics/internal.log")
    }

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        if (recordingEnabled == enabled) return
        recordingEnabled = enabled
        if (!enabled) {
            publishScheduled = false
            main.removeCallbacks(publishRunnable)
            return
        }
        val file = logFile
        if (!historyLoaded && file != null) {
            historyLoaded = true
            runCatching {
                if (file.exists()) {
                    val tail = file.useLines { lines ->
                        // Stream the file and keep only the last lines: the whole log is never held in memory.
                        val keep = java.util.ArrayDeque<String>(RAM_TAIL)
                        for (line in lines) { if (keep.size >= RAM_TAIL) keep.removeFirst(); keep.addLast(line) }
                        keep.toList()
                    }
                    synchronized(lock) { entries.clear(); tail.forEach(entries::addLast) }
                }
            }
            main.post(publishRunnable)
        }
        record("DIAGNOSTICS_READY", "fileBacked=true history=append_only ramTail=$RAM_TAIL")
        record("DIAGNOSTICS_RECORDING", "verbose=true")
    }

    fun isEnabled(): Boolean = recordingEnabled

    inline fun recordLazy(event: String, detail: () -> String) {
        if (isEnabled()) record(event, detail())
    }

    /** Failures are recorded only while the user-enabled Diagnostics feature is active. */
    fun recordError(component: String, stage: String, error: Throwable, context: String = "") {
        if (!recordingEnabled) return
        val stack = error.stackTraceToString().take(12_000)
        val detail = buildString {
            append("component=").append(component)
            append(" stage=").append(stage)
            if (context.isNotBlank()) append(" context=").append(sanitize(context))
            append(" exception=").append(error.javaClass.name)
            append(" message=").append(sanitize(error.message ?: "(no message)"))
            append(" stack=").append(sanitize(stack))
        }
        writeLine("ERROR", detail)
    }

    fun record(event: String, detail: String = "") {
        if (!recordingEnabled) return
        writeLine(event, detail)
    }

    private fun sanitize(value: String): String = value
        .replace(Regex("(?i)(api[_-]?key|authorization|bearer)\\s*[:=]\\s*[^\\s,;]+"), "\$1=<redacted>")
        .replace(Regex("(?i)(key=)[^&\\s]+"), "\$1<redacted>")
        .take(12_000)

    private fun writeLine(event: String, detail: String) {
        if (!recordingEnabled) return
        val up = SystemClock.uptimeMillis()
        // Wall-clock date/time is recorded on every line so a log can be correlated with
        // the user's report. Uptime remains alongside it for measuring in-process intervals.
        val wall = System.currentTimeMillis()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(wall))
        val line = String.format(
            Locale.US, "%s +%dms | %s%s",
            timestamp, up, event, if (detail.isEmpty()) "" else " | $detail"
        )
        if (com.android.island.BuildConfig.DEBUG) Log.d("AndroidIsland", line)
        synchronized(lock) {
            if (entries.size >= RAM_TAIL) entries.removeFirst()
            entries.addLast(line)
        }
        val target = logFile
        if (target != null) diskWriter.execute {
            // A write queued before the user switched Diagnostics off must not run afterward.
            if (recordingEnabled) runCatching {
                target.parentFile?.mkdirs()
                target.appendText(line + "\n", Charsets.UTF_8)
            }.onFailure { if (recordingEnabled) Log.e("AndroidIsland", "Could not persist diagnostic log", it) }
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

    /** Flushes queued writes before reading the file, preventing the copy action from racing
     * the append-only writer and silently copying an empty or incomplete log. */
    suspend fun exportTextAsync(): String = withContext(Dispatchers.IO) {
        if (!recordingEnabled) return@withContext ""
        runCatching { diskWriter.submit(Runnable { }).get(3, TimeUnit.SECONDS) }
        val target = logFile
        if (target != null && target.exists()) {
            runCatching { target.readText(Charsets.UTF_8) }
                .getOrElse { synchronized(lock) { entries.joinToString("\n") } }
        } else synchronized(lock) { entries.joinToString("\n") }
    }

    fun clear() {
        synchronized(lock) { entries.clear() }
        // When disabled, clear only in-memory state: no background disk operation is allowed.
        if (recordingEnabled) {
            logFile?.let { file -> diskWriter.execute {
                if (recordingEnabled) runCatching { file.writeText("", Charsets.UTF_8) }
            } }
        }
        _snapshot.value = emptyList()
    }
}
