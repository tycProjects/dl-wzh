package com.android.island.ai

import android.content.Context
import android.content.Intent
import com.android.island.system.ShizukuShell

/** Runs the diagnostics through Shizuku and resolves app names to package names. */
object DeviceDiagnostics {

    private fun resolvePackage(context: Context, query: String?): Pair<String, String>? {
        val q = query?.trim().orEmpty()
        if (q.isBlank()) return null
        val pm = context.packageManager
        if (DiagnosticPolicy.safePackage(q)) {
            val info = runCatching { pm.getApplicationInfo(q, 0) }.getOrNull()
            if (info != null) return info.loadLabel(pm).toString() to q
        }
        val apps = runCatching {
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        }.getOrDefault(emptyList())
        val labels = apps.map { it.loadLabel(pm).toString() }
        val idx = AppMatcher.match(q, labels) ?: return null
        return labels[idx] to apps[idx].activityInfo.packageName
    }

    fun run(context: Context, kind: DiagnosticPolicy.Kind, app: String?): String {
        if (!ShizukuShell.available()) {
            return "Shizuku belum siap, jadi diagnosa tidak bisa dijalankan. Aktifkan Shizuku dulu."
        }
        val pkg = if (kind == DiagnosticPolicy.Kind.MEMINFO) {
            resolvePackage(context, app)
                ?: return "Aplikasi \u201C${app.orEmpty()}\u201D tidak ditemukan. Panggil list_apps lalu coba lagi dengan nama yang tepat."
        } else null
        val cmd = DiagnosticPolicy.command(kind, pkg?.second)
            ?: return "Perintah diagnosa tidak valid."
        // Larger buffer than the summary needs: the TOTAL lines sit near the end of a full meminfo dump.
        val result = ShizukuShell.captureResult(cmd, maxChars = 40_000, timeoutSeconds = 30)
        // A partial stdout is not a successful diagnostic. Keep the complete execution
        // metadata attached so the AI cannot mistake exit!=0, timeout, or truncation for success.
        if (!result.succeeded || result.truncated || result.stdout.isBlank()) {
            val reason = when {
                result.timedOut -> "perintah melewati batas waktu"
                result.exitCode != 0 -> "exit code bukan 0"
                result.truncated -> "output terpotong dan tidak lengkap"
                result.stdout.isBlank() -> "STDOUT kosong; tidak ada data diagnosa untuk ditafsirkan"
                else -> "eksekusi tidak berhasil"
            }
            return "Diagnosa ${kind.name} tidak dapat dinyatakan berhasil: $reason.\nPerintah: $cmd\n${result.asText().trim()}"
        }
        val body = if (kind == DiagnosticPolicy.Kind.MEMINFO) {
            DiagnosticPolicy.summarizeMeminfo(result.stdout)
        } else {
            result.stdout.trim().ifBlank { "(perintah selesai tanpa output)" }
        }
        val title = pkg?.first?.let { " untuk $it" }.orEmpty()
        return "Diagnosa ${kind.name}$title\nPerintah: $cmd\nStatus: berhasil\nExit code: ${result.exitCode}\nSTDERR: ${result.stderr.ifBlank { "(kosong)" }}\nHasil terstruktur dari STDOUT:\n$body\n\nBukti STDOUT asli:\n${result.stdout}"
    }

    fun forceStop(context: Context, app: String): String {
        if (!ShizukuShell.available()) return "Shizuku belum siap, jadi aplikasi tidak bisa dipaksa berhenti."
        val (label, pkg) = resolvePackage(context, app)
            ?: return "Aplikasi \u201C$app\u201D tidak ditemukan."
        val cmd = DiagnosticPolicy.forceStopCommand(pkg) ?: return "Nama paket tidak valid."
        return if (ShizukuShell.exec(cmd)) "Siap, $label sudah dipaksa berhenti." else "Gagal memaksa berhenti $label."
    }
}
