package com.hyperstatusbar.pro.root

import com.hyperstatusbar.pro.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class RootState(val available: Boolean, val output: String = "")

class RootManager(private val logger: AppLogger? = null) {
    suspend fun checkRoot(): RootState = withContext(Dispatchers.IO) {
        runCatching {
            val p = ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start()
            if (!p.waitFor(8, TimeUnit.SECONDS)) {
                p.destroyForcibly()
                error("Тайм-аут проверки su")
            }
            val output = p.inputStream.bufferedReader().use { it.readText() }.trim()
            val ok = p.exitValue() == 0 && output.contains("uid=0")
            logger?.info("RootManager", "Проверка root: ${if (ok) "доступен" else "нетдоступа"}; exit=${p.exitValue()}")
            RootState(ok, output)
        }.getOrElse {
            logger?.error("RootManager", "Не удалось выполнить проверку root", it)
            RootState(false, it.message ?: "su недоступен")
        }
    }

    suspend fun restartSystemUi(): Result<String> = runFixed(
        "killall com.android.systemui",
        "Перезапуск SystemUI"
    )

    suspend fun moduleInstalled(): Result<String> = runFixed(
        "test -d /data/adb/modules/hyperstatusbarpro && echo installed",
        "Проверка Magisk-модуля"
    )

    suspend fun writeConfig(json: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // Base64 исключает попадание пользовательского JSON в синтаксис shell.
            val encoded = android.util.Base64.encodeToString(
                json.toByteArray(Charsets.UTF_8),
                android.util.Base64.NO_WRAP
            )
            val script = "mkdir -p /data/adb/modules/hyperstatusbarpro && " +
                "printf '%s' '$encoded' | base64 -d > /data/adb/modules/hyperstatusbarpro/config.json && " +
                "chmod 0644 /data/adb/modules/hyperstatusbarpro/config.json"
            val p = ProcessBuilder("su", "-c", script).redirectErrorStream(true).start()
            if (!p.waitFor(10, TimeUnit.SECONDS)) {
                p.destroyForcibly()
                error("Тайм-аут записи конфигурации")
            }
            val output = p.inputStream.bufferedReader().use { it.readText() }
            check(p.exitValue() == 0) { output.ifBlank { "Ошибка записи конфигурации" } }
            logger?.info("RootManager", "Конфигурация записана в каталог модуля")
            output.ifBlank { "Конфигурация сохранена" }
        }.onFailure { logger?.error("RootManager", "Ошибка записи конфигурации", it) }
    }

    private suspend fun runFixed(command: String, label: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                logger?.info("RootManager", "$label: запуск")
                val p = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
                if (!p.waitFor(10, TimeUnit.SECONDS)) {
                    p.destroyForcibly()
                    error("$label: тайм-аут")
                }
                val output = p.inputStream.bufferedReader().use { it.readText() }
                check(p.exitValue() == 0) { output.ifBlank { "$label: exit=${p.exitValue()}" } }
                logger?.info("RootManager", "$label: успешно")
                output
            }.onFailure { logger?.error("RootManager", "$label: ошибка", it) }
        }
}
