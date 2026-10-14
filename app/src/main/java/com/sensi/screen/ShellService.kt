package com.sensi.screen

import kotlin.system.exitProcess

/**
 * Chạy bên trong tiến trình do Shizuku khởi tạo (quyền shell/ADB, giống Brevent),
 * nên gọi được `wm size` / `wm density`.
 */
class ShellService : IShellService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(command: String): String {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val out = p.inputStream.bufferedReader().readText()
            val err = p.errorStream.bufferedReader().readText()
            val code = p.waitFor()
            if (code == 0) out.trim() else "ERR($code): ${(err + out).trim()}"
        } catch (e: Exception) {
            "ERR: ${e.message}"
        }
    }
}
