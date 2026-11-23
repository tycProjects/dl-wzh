package com.anjo.adb.core

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
// AUTO-REPAIR: disabled -- unresolved reference 'ShizukuProcess' (was: import rikka.shizuku.ShizukuProcess)
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Motor central do Anjo ADB.
 *
 * Toda função que mexe no sistema (limpar RAM, trocar textura, travar FPS, etc)
 * passa por aqui. Sem Shizuku pareado e autorizado, NADA roda de verdade —
 * em vez disso cada tela mostra "requer Shizuku ou ADB" e desativa o botão.
 *
 * Como o Shizuku funciona por trás: o usuário ativa o app Shizuku (via ADB
 * wireless debugging ou root), nosso app pede permissão pelo Shizuku.requestPermission,
 * e depois disso Shizuku.shell().execute() deixa a gente rodar "adb shell <comando>"
 * com privilégio de shell, sem precisar de root nem de cabo.
 */
object ShellEngine {

    const val REQUEST_CODE = 7001

    enum class State { NAO_INSTALADO, NAO_AUTORIZADO, PRONTO }

    fun state(): State {
        if (!Shizuku.pingBinder()) return State.NAO_INSTALADO
        return if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            State.PRONTO
        } else {
            State.NAO_AUTORIZADO
        }
    }

    fun isReady(): Boolean = state() == State.PRONTO

    fun requestPermission() {
        if (Shizuku.isPreV11()) return
        if (!Shizuku.pingBinder()) return
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }

    /** Roda um comando de shell com privilégio ADB/Shizuku. Devolve a saída (stdout). */
    fun run(cmd: String): Result<String> {
        if (!isReady()) return Result.failure(IllegalStateException("Shizuku não autorizado"))
        return try {
            // AUTO-REPAIR: disabled -- unresolved reference 'shell' (was: val process: ShizukuProcess = Shizuku.shell().execute(arrayOf("sh", "-c", cmd)))
            val out = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val err = BufferedReader(InputStreamReader(process.errorStream)).readText()
            process.waitFor()
            if (process.exitValue() == 0 || out.isNotBlank()) {
                Result.success(out)
            } else {
                Result.failure(RuntimeException(err.ifBlank { "comando falhou: $cmd" }))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /** Roda vários comandos em sequência, parando no primeiro erro. */
    fun runAll(vararg cmds: String): Result<Unit> {
        for (c in cmds) {
            val r = run(c)
            if (r.isFailure) return Result.failure(r.exceptionOrNull()!!)
        }
        return Result.success(Unit)
    }
}
