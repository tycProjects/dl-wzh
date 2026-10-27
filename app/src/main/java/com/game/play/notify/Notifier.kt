package com.game.play.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.game.play.MainActivity

object Notifier {
    private const val CH_DOWNLOAD = "downloads"
    private const val CH_INFO = "info"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CH_DOWNLOAD, "Unduhan", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_INFO, "Info & pembaruan", NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun idFor(key: String): Int = key.hashCode() and 0x7fffffff

    private fun openApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun post(context: Context, id: Int, notification: Notification) {
        if (!hasPermission(context)) return
        try { NotificationManagerCompat.from(context).notify(id, notification) } catch (e: SecurityException) { /* izin dicabut */ }
    }

    fun progress(context: Context, id: Int, title: String, percent: Int) {
        ensureChannels(context)
        val n = NotificationCompat.Builder(context, CH_DOWNLOAD)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText("Mengunduh $percent%")
            .setProgress(100, percent, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        post(context, id, n)
    }

    fun downloadDone(context: Context, id: Int, title: String, text: String, action: PendingIntent?) {
        ensureChannels(context)
        val n = NotificationCompat.Builder(context, CH_DOWNLOAD)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(action ?: openApp(context))
            .build()
        post(context, id, n)
    }

    fun info(context: Context, id: Int, title: String, text: String) {
        ensureChannels(context)
        val n = NotificationCompat.Builder(context, CH_INFO)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
            .build()
        post(context, id, n)
    }

    fun cancel(context: Context, id: Int) {
        try { NotificationManagerCompat.from(context).cancel(id) } catch (e: SecurityException) { }
    }
}
