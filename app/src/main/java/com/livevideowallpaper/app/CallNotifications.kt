package com.livevideowallpaper.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telecom.Call
import androidx.core.app.NotificationCompat

object CallNotifications {
    const val CHANNEL = "calls"
    const val ID = 4711
    const val ACTION_ANSWER = "com.livevideowallpaper.app.ANSWER"
    const val ACTION_DECLINE = "com.livevideowallpaper.app.DECLINE"

    fun ensureChannel(c: Context) {
        val ch = NotificationChannel(CHANNEL, "Calls", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null) // the phone system plays the ringtone
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        c.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    fun uiIntent(c: Context): Intent =
        Intent(c, InCallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    private fun action(c: Context, what: String, code: Int): PendingIntent {
        val i = Intent(c, CallActionReceiver::class.java).setAction(what)
        return PendingIntent.getBroadcast(c, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun show(c: Context, call: Call) {
        val ringing = call.state == Call.STATE_RINGING
        val number = CallHub.numberOf(call)
        val title = ContactsRepo.nameFor(c, number) ?: number.ifEmpty { "Unknown" }
        val content = PendingIntent.getActivity(
            c, 0, uiIntent(c), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val b = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_call)
            .setContentTitle(title)
            .setContentText(if (ringing) "Incoming call" else "Ongoing call")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(content)
        if (ringing) {
            b.setFullScreenIntent(content, true)
            b.addAction(0, "Decline", action(c, ACTION_DECLINE, 1))
            b.addAction(0, "Answer", action(c, ACTION_ANSWER, 2))
        } else {
            b.addAction(0, "Hang up", action(c, ACTION_DECLINE, 1))
        }
        c.getSystemService(NotificationManager::class.java).notify(ID, b.build())
    }

    fun cancel(c: Context) {
        c.getSystemService(NotificationManager::class.java).cancel(ID)
    }
}
