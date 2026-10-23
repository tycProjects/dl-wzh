package com.schedule.app.work

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.schedule.app.MainActivity
import com.schedule.app.R
import com.schedule.app.data.Attachment
import com.schedule.app.data.AppDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class RecState(val active: Boolean = false, val slotId: Long = 0, val startedAt: Long = 0)

/** خدمة تسجيل صوتي تعمل في الخلفية (والشاشة مغلقة) وتحفظ التسجيل كمرفق للمحاضرة. */
class RecordService : Service() {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopRecording()
            else -> {
                // يجب استدعاء startForeground خلال ثوانٍ من بدء الخدمة، حتى لو فشل التسجيل لاحقاً
                promote(notification("جارٍ التحضير"))
                val id = intent?.getLongExtra("slot_id", 0) ?: 0
                if (state.value.active || id <= 0) { stopForeground(STOP_FOREGROUND_REMOVE); if (!state.value.active) stopSelf() }
                else startRecording(id)
            }
        }
        return START_NOT_STICKY
    }

    private fun notification(title: String): Notification {
        Notifier.ensureChannels(this)
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(Notifier.EXTRA_RECORD_SLOT, state.value.slotId), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, Notifier.CH_RECORDING).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("جارٍ تسجيل المحاضرة").setContentText(title).setOngoing(true).setContentIntent(open)
            .addAction(0, "إيقاف وحفظ", stopIntent(this)).build()
    }

    private fun promote(n: Notification) {
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        else startForeground(NOTIF_ID, n)
    }

    private fun startRecording(slotId: Long) {
        scope.launch {
            val slot = AppDatabase.get(applicationContext).dao().slot(slotId)
            withContext(Dispatchers.Main) {
                val title = slot?.title ?: "محاضرة"
                val dir = File(filesDir, "attachments").also { it.mkdirs() }
                val f = File(dir, "${UUID.randomUUID()}.m4a")
                try {
                    val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this@RecordService) else @Suppress("DEPRECATION") MediaRecorder()
                    r.setAudioSource(MediaRecorder.AudioSource.MIC)
                    r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    r.setAudioSamplingRate(44100); r.setAudioEncodingBitRate(96_000)
                    r.setOutputFile(f.absolutePath)
                    r.prepare()
                    state.value = RecState(true, slotId, SystemClock.elapsedRealtime())
                    promote(notification(title))
                    r.start()
                    recorder = r; file = f
                } catch (e: Exception) {
                    state.value = RecState()
                    runCatching { f.delete() }
                    stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
                }
            }
        }
    }

    private fun stopRecording() {
        val s = state.value
        val f = file
        val r = recorder
        recorder = null; file = null
        state.value = RecState()
        var ok = r != null
        runCatching { r?.stop() }.onFailure { ok = false }
        runCatching { r?.release() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (ok && f != null && f.exists() && f.length() > 0 && s.slotId > 0) {
            scope.launch {
                val dao = AppDatabase.get(applicationContext).dao()
                val title = dao.slot(s.slotId)?.title ?: "محاضرة"
                val stamp = SimpleDateFormat("yyyy-MM-dd HH.mm", Locale.US).format(Date())
                dao.insertAttachment(Attachment(slotId = s.slotId, name = "تسجيل $title $stamp.m4a", path = f.absolutePath,
                    mime = "audio/mp4", size = f.length(), kind = 1))
                stopSelf()
            }
        } else { f?.delete(); stopSelf() }
    }

    override fun onDestroy() {
        if (state.value.active) stopRecording()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.schedule.app.STOP_RECORD"
        private const val NOTIF_ID = 777
        val state = MutableStateFlow(RecState())

        fun start(ctx: Context, slotId: Long) =
            ContextCompat.startForegroundService(ctx, Intent(ctx, RecordService::class.java).putExtra("slot_id", slotId))

        fun stop(ctx: Context) { ctx.startService(Intent(ctx, RecordService::class.java).setAction(ACTION_STOP)) }

        fun stopIntent(ctx: Context): PendingIntent = PendingIntent.getService(
            ctx, 2, Intent(ctx, RecordService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
