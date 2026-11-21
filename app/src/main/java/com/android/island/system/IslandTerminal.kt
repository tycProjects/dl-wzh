package com.android.island.system

import com.android.island.ai.ShellPolicy
import com.android.island.util.IslandDiagnostics

/**
 * Terminal Android yang tidak terlihat pengguna. Hanya AI yang mengirim perintah ke sini
 * (lewat aksi shell), lalu AI menjelaskan hasilnya.
 *
 * Diport dari ShellCommandExecutor aShellYou: direktori kerja (cwd) bertahan antar perintah
 * lewat `cd`, output dibaca sampai habis, dan eksekusi lewat Shizuku. Satu sumber aturan:
 * ShellPolicy (hanya perintah fatal ditolak, yang berisiko minta "ya", sisanya bebas).
 */
object IslandTerminal {
    private const val START_DIR = "/storage/emulated/0"
    private const val MAX_OUTPUT = 12_000
    private const val MAX_DUMPSYS_OUTPUT = 24_000
    private val PURE_CD = Regex("""^cd(\s+[^\s;&|<>`$(){}]+)?$""")

    @Volatile
    private var cwd: String = START_DIR

    sealed class Outcome {
        data class Refused(val reason: String) : Outcome()
        data class NeedsConfirmation(val command: String, val reason: String) : Outcome()
        data class Executed(val command: String, val result: ShizukuShell.CommandResult) : Outcome()
    }

    /**
     * Returns the target of a standalone `cd` ("" for bare `cd`), or null when the command
     * is not a single cd. Compound commands such as "cd /x && ls" run inside the current dir.
     */
    fun cdTarget(command: String): String? {
        val cmd = command.trim()
        if (!PURE_CD.matches(cmd)) return null
        return cmd.removePrefix("cd").trim()
    }

    fun currentDir(): String = cwd

    /**
     * Runs one AI-proposed command. Must be called from Dispatchers.IO.
     * @param confirmed true only after the user answered "ya" for this exact command.
     */
    fun run(command: String, confirmed: Boolean): Outcome {
        val cmd = command.trim()
        ShellPolicy.refusal(cmd)?.let { reason ->
            IslandDiagnostics.record("SHELL_REFUSED", "cmd=${cmd.take(120)}")
            return Outcome.Refused(reason)
        }
        if (!confirmed && ShellPolicy.needsConfirmation(cmd)) {
            return Outcome.NeedsConfirmation(cmd, "Perintah ini berisiko (mengubah sistem atau menghapus data).")
        }
        if (!ShizukuShell.available()) {
            return Outcome.Refused("Shizuku belum siap. Buka Shizuku dan beri izin ke aplikasi ini, lalu coba lagi.")
        }

        val dir = quote(cwd)
        val target = cdTarget(cmd)
        val full = if (target != null) {
            "cd $dir && cd ${target.ifBlank { quote(START_DIR) }} && pwd"
        } else {
            "cd $dir && $cmd"
        }
        // Diagnostic dumps are large and slow (meminfo, gfxinfo), so they get more room and time.
        val dump = ShellPolicy.isDumpsys(cmd)
        val result = ShizukuShell.captureResult(
            full,
            maxChars = if (dump) MAX_DUMPSYS_OUTPUT else MAX_OUTPUT,
            timeoutSeconds = if (dump) 60L else 30L
        )

        if (target != null && result.succeeded) {
            result.stdout.lines().lastOrNull { it.isNotBlank() }?.let { cwd = it.trim() }
        }
        IslandDiagnostics.record(
            "SHELL_RUN",
            "exit=${result.exitCode ?: "none"} timedOut=${result.timedOut} " +
                "stdoutChars=${result.stdout.length} stderrChars=${result.stderr.length} cmd=${cmd.take(120)}"
        )
        return Outcome.Executed(cmd, result)
    }

    private fun quote(path: String): String = "'" + path.replace("'", "'\\''") + "'"
}
