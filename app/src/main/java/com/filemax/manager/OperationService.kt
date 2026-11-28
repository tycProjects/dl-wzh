package com.filemax.manager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Process-wide scope so long jobs survive the activity being backgrounded. */
object AppScope { val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main) }

/** Foreground service that keeps the process alive and shows progress/cancel while an archive or file job runs. */
class OperationService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) { TaskRunner.current?.ctl?.cancelled = true; return START_NOT_STICKY }
        val n = build(this, intent?.getStringExtra("t") ?: "File Max", -1f, "")
        runCatching {
            if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else startForeground(NOTIF_ID, n)
        }
        return START_NOT_STICKY
    }

    companion object {
        const val CH = "filemax_ops"; const val NOTIF_ID = 77; const val ACTION_CANCEL = "com.filemax.manager.CANCEL"

        private fun build(ctx: Context, title: String, fraction: Float, label: String): Notification {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CH) == null)
                nm.createNotificationChannel(NotificationChannel(CH, "File Max operations", NotificationManager.IMPORTANCE_LOW))
            val cancel = PendingIntent.getService(ctx, 0, Intent(ctx, OperationService::class.java).setAction(ACTION_CANCEL), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val open = PendingIntent.getActivity(ctx, 1, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
            return NotificationCompat.Builder(ctx, CH).setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle(tr(title)).setContentText(label)
                .setOngoing(true).setOnlyAlertOnce(true).setContentIntent(open)
                .setProgress(100, (fraction.coerceAtLeast(0f) * 100).toInt(), fraction < 0f)
                .addAction(0, tr("إلغاء"), cancel).build()
        }

        fun start(ctx: Context, title: String) { runCatching { ContextCompat.startForegroundService(ctx, Intent(ctx, OperationService::class.java).putExtra("t", title)) } }
        fun update(ctx: Context, title: String, fraction: Float, label: String) {
            runCatching { ctx.getSystemService(NotificationManager::class.java).notify(NOTIF_ID, build(ctx, title, fraction, label)) }
        }
        fun stop(ctx: Context) { runCatching { ctx.stopService(Intent(ctx, OperationService::class.java)) } }
    }
}
