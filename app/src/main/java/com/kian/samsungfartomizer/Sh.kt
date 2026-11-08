package com.kian.samsungfartomizer

import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

data class ShResult(val code: Int, val text: String)

/** Runs shell commands through Shizuku. */
object Sh {
    fun up(): Boolean = try { Shizuku.pingBinder() } catch (_: Throwable) { false }

    fun granted(): Boolean = try {
        up() && !Shizuku.isPreV11() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) { false }

    suspend fun run(cmd: String): ShResult = withContext(Dispatchers.IO) {
        try {
            // Shizuku.newProcess is private in recent API versions, so call it by reflection
            val m = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            m.isAccessible = true
            val p = m.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            val out = p.inputStream.bufferedReader().readText()
            val err = p.errorStream.bufferedReader().readText()
            val code = p.waitFor()
            ShResult(code, (out + err).trim())
        } catch (t: Throwable) {
            ShResult(-1, t.message ?: t.javaClass.simpleName)
        }
    }
}
