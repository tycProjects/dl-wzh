package com.android.island.system

import rikka.shizuku.Shizuku
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Executes one policy-validated Android shell command through Shizuku. */
object ShizukuShell {
    data class CommandResult(
        val stdout: String,
        val stderr: String,
        val exitCode: Int?,
        val timedOut: Boolean,
        /** True when the process produced more output than was kept. The kept part is incomplete. */
        val truncated: Boolean = false
    ) {
        val succeeded: Boolean get() = !timedOut && exitCode == 0
        fun asText(limit: Int = 50_000): String = buildString {
            appendLine("Status: ${if (timedOut) "timeout" else if (exitCode == 0) "berhasil" else "gagal"}")
            appendLine("Exit code: ${exitCode ?: "tidak tersedia"}")
            if (truncated) appendLine("PERINGATAN: output dipotong karena terlalu panjang. Bagian di bawah tidak lengkap; jangan menyimpulkan apa pun dari bagian yang hilang.")
            if (stdout.isNotBlank()) { appendLine("STDOUT:"); appendLine(stdout.trim()) }
            if (stderr.isNotBlank()) { appendLine("STDERR:"); appendLine(stderr.trim()) }
            if (stdout.isBlank() && stderr.isBlank()) appendLine("(perintah selesai tanpa output)")
            if (timedOut) appendLine("Proses dihentikan karena melewati batas waktu.")
        }.trim().take(limit)
    }

    private val activeProcesses = java.util.concurrent.ConcurrentHashMap.newKeySet<Process>()

    /** Immediately terminates shell processes when the user stops a request or clears its chat. */
    fun cancelActiveCommands() {
        activeProcesses.toList().forEach { process ->
            runCatching { process.destroy() }
            runCatching { process.destroyForcibly() }
            runCatching { process.inputStream.close() }
            runCatching { process.errorStream.close() }
        }
    }

    private val newProcess by lazy(LazyThreadSafetyMode.NONE) {
        Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java).apply { isAccessible = true }
    }

    fun available(): Boolean {
        ShizukuState.refresh()
        return ShizukuState.status.value == ShizukuStatus.READY
    }

    fun exec(command: String): Boolean {
        if (!available()) return false
        return runCatching {
            val process = newProcess.invoke(null, arrayOf("sh", "-c", command), null, null) as? Process ?: return false
            drain(process); true
        }.getOrDefault(false)
    }

    /**
     * Captures stdout, stderr and exit status. Always invoke from Dispatchers.IO.
     * Streams are read to the end before the exit status is requested, so output is not cut off.
     */
    fun captureResult(command: String, maxChars: Int = 6000, timeoutSeconds: Long = 20): CommandResult {
        if (!available()) return CommandResult("", "Gagal: Shizuku belum siap.", null, false)
        return runCatching {
            val process = newProcess.invoke(null, arrayOf("sh", "-c", command), null, null) as? Process
                ?: return CommandResult("", "Gagal: Shizuku tidak membuka proses shell.", null, false)
            activeProcesses.add(process)
            try {
            var stdout = ""; var stderr = ""
            val cut = AtomicBoolean(false)
            val outThread = Thread({
                val (text, over) = readBounded(process.inputStream, maxChars); stdout = text; if (over) cut.set(true)
            }, "island-shell-stdout")
            val errThread = Thread({
                val (text, over) = readBounded(process.errorStream, maxChars); stderr = text; if (over) cut.set(true)
            }, "island-shell-stderr")
            outThread.isDaemon = true; errThread.isDaemon = true
            outThread.start(); errThread.start()

            // Do not rely on remote Process.waitFor(timeout): some Shizuku Process wrappers
            // report completion before exitValue() is actually readable. Poll exitValue directly.
            val exit = awaitExitCode(process, timeoutSeconds)
            val timedOut = exit == null
            if (timedOut) {
                runCatching { process.destroy() }
                runCatching { process.destroyForcibly() }
                runCatching { process.inputStream.close() }
                runCatching { process.errorStream.close() }
            }
            outThread.join(1_500); errThread.join(1_500)
            CommandResult(stdout.take(maxChars), stderr.take(maxChars), exit, timedOut, cut.get())
            } finally {
                activeProcesses.remove(process)
            }
        }.getOrElse { e ->
            val detail = e.message ?: "tanpa pesan"
            CommandResult("", "Gagal: ${e.javaClass.simpleName}: $detail", null, false)
        }
    }

    /** Drain the complete stream to avoid blocking the remote process, but retain bounded output. */
    private fun readBounded(stream: java.io.InputStream, maxChars: Int): Pair<String, Boolean> = runCatching {
        val limit = maxChars.coerceAtLeast(0)
        val out = StringBuilder(minOf(limit, 8192))
        var over = false
        val buffer = CharArray(2048)
        stream.bufferedReader().use { reader ->
            while (true) {
                val count = reader.read(buffer)
                if (count < 0) break
                val remaining = limit - out.length
                if (remaining > 0) out.append(buffer, 0, minOf(count, remaining))
                if (count > remaining) over = true
            }
        }
        out.toString() to over
    }.getOrDefault("" to false)

    /**
     * Returns null only when the command genuinely fails to exit before the deadline.
     * Do NOT use Process.waitFor(timeout) here: Shizuku's remote Process wrappers do not support it reliably
     * (every command failed with it in stage110). exitValue() is polled, with a growing pause: fast commands
     * are noticed within ~10 ms, long ones are checked at most four times a second.
     */
    private fun awaitExitCode(process: Process, timeoutSeconds: Long): Int? {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds.coerceAtLeast(1))
        var pauseMs = 10L
        while (System.nanoTime() < deadline) {
            val code = try {
                process.exitValue()
            } catch (_: IllegalThreadStateException) {
                null
            } catch (_: RuntimeException) {
                null
            }
            if (code != null) return code
            try { Thread.sleep(pauseMs) } catch (_: InterruptedException) { Thread.currentThread().interrupt(); return null }
            pauseMs = (pauseMs * 2).coerceAtMost(250L)
        }
        // One final read handles the race where the process exits exactly at the deadline.
        return try { process.exitValue() } catch (_: Exception) { null }
    }

    /** Sends one fixed Android key event, e.g. 66 = ENTER. Internal use only, never from model text. */
    fun pressKey(keycode: Int): Boolean {
        if (keycode !in 1..300) return false
        return captureResult("input keyevent $keycode", maxChars = 200, timeoutSeconds = 5).succeeded
    }

    /** A finger-like tap (jittered point, real hold time) injected as a genuine touch event. Integers only. */
    fun humanTap(x: Int, y: Int, width: Int, height: Int): Boolean {
        val tap = com.android.island.ai.HumanInput.tap(x, y, width, height)
        return captureResult(com.android.island.ai.HumanInput.tapCommand(tap), maxChars = 200, timeoutSeconds = 5).succeeded
    }

    fun humanScroll(down: Boolean, width: Int, height: Int): Boolean {
        val swipe = com.android.island.ai.HumanInput.scroll(down, width, height)
        return captureResult(com.android.island.ai.HumanInput.swipeCommand(swipe), maxChars = 200, timeoutSeconds = 5).succeeded
    }

    /** Legacy diagnostic output: keeps stdout/stderr text free of metadata for existing parsers. */
    fun capture(command: String, maxChars: Int = 3000): String {
        val result = captureResult(command, maxChars)
        if (result.stderr.startsWith("Gagal:")) return result.stderr
        if (result.timedOut) return "Gagal: perintah melewati batas waktu."
        val output = listOf(result.stdout.trim(), result.stderr.trim()).filter { it.isNotBlank() }
            .joinToString("\n").take(maxChars)
        if (output.isNotBlank()) return output
        return if (result.exitCode == 0) "(perintah selesai tanpa output)" else "Gagal: exit code ${result.exitCode ?: "tidak tersedia"}."
    }

    fun drain(process: Process) {
        Thread {
            runCatching { process.inputStream.use { it.readBytes() } }
            runCatching { process.errorStream.use { it.readBytes() } }
            runCatching { process.outputStream.close() }
            runCatching { process.waitFor() }
        }.apply { isDaemon = true; name = "shizuku-drain"; start() }
    }
}
