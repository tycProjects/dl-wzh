package com.example.playback

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.db.JnmfDatabase
import com.example.data.repository.RadioScheduleRepository
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver responsible for:
 * 1. Automatically starting scheduled Radio DJ Playlists when their configured time arrives.
 * 2. Restoring and synchronizing scheduled playback across device boots and clock/timezone changes.
 */
class RadioBroadcastAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "RadioBroadcastAlarmReceiver received action: $action")

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        scope.launch {
            try {
                when (action) {
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED -> {
                        rescheduleAllAutoSchedules(context)
                    }
                    ACTION_RADIO_SCHEDULE_TRIGGER -> {
                        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
                        if (scheduleId > 0L) {
                            handleScheduleTrigger(context, scheduleId)
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error handling alarm broadcast: ${t.message}", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleScheduleTrigger(context: Context, scheduleId: Long) {
        val db = JnmfDatabase.getDatabase(context)
        val repo = RadioScheduleRepository(context, db.jnmfDao())
        val schedule = repo.getScheduleById(scheduleId) ?: return
        if (!schedule.isAutoScheduled) return

        val itemEntities = repo.getItemsForScheduleSync(scheduleId)
        if (itemEntities.isEmpty()) return

        val items = repo.resolveItemsForUi(
            entities = itemEntities,
            songsById = emptyMap(),
            hasScannedLibrary = false
        )

        PlaybackService.startService(context)

        kotlinx.coroutines.withContext(Dispatchers.Main) {
            RadioPlaybackController.bindAppContext(context)
            if (schedule.timeAnnouncementEnabled) {
                val uri = if (schedule.timeAnnouncementAudioUri.isNotBlank()) {
                    android.net.Uri.parse(schedule.timeAnnouncementAudioUri)
                } else null
                RadioPlaybackController.configureTimeAnnouncement(
                    enabled = true,
                    hourly = schedule.hourlyAnnouncementEnabled,
                    customMinutes = schedule.customAnnouncementMinutes,
                    audioUri = uri,
                    title = schedule.timeAnnouncementTitle
                )
            }
            RadioPlaybackController.startRadioProgram(
                scheduleId = schedule.id,
                scheduleName = schedule.name,
                items = items,
                startIndex = 0
            )
        }

        // Reschedule for next day so recurring schedule stays active
        scheduleAutoPlayAlarm(context, schedule.id, schedule.startTimeSecondsOfDay)
    }

    companion object {
        private const val TAG = "JNMF_RadioAlarm"
        const val ACTION_RADIO_SCHEDULE_TRIGGER = "com.example.action.RADIO_SCHEDULE_TRIGGER"
        const val EXTRA_SCHEDULE_ID = "extra_radio_schedule_id"

        fun scheduleAutoPlayAlarm(context: Context, scheduleId: Long, startTimeSecondsOfDay: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, RadioBroadcastAlarmReceiver::class.java).apply {
                action = ACTION_RADIO_SCHEDULE_TRIGGER
                putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                scheduleId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, startTimeSecondsOfDay / 3600)
                set(Calendar.MINUTE, (startTimeSecondsOfDay % 3600) / 60)
                set(Calendar.SECOND, startTimeSecondsOfDay % 60)
                set(Calendar.MILLISECOND, 0)
            }

            // If target time already passed today, schedule for tomorrow
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pendingIntent)
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pendingIntent)
                    }
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, target.timeInMillis, pendingIntent)
                }
                Log.d(TAG, "Scheduled auto-play alarm for schedule $scheduleId at ${target.time}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to schedule exact alarm, falling back: ${e.message}")
                alarmManager.set(AlarmManager.RTC_WAKEUP, target.timeInMillis, pendingIntent)
            }
        }

        fun cancelAutoPlayAlarm(context: Context, scheduleId: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, RadioBroadcastAlarmReceiver::class.java).apply {
                action = ACTION_RADIO_SCHEDULE_TRIGGER
                putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                scheduleId.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d(TAG, "Cancelled auto-play alarm for schedule $scheduleId")
            }
        }

        suspend fun rescheduleAllAutoSchedules(context: Context) {
            val db = JnmfDatabase.getDatabase(context)
            val repo = RadioScheduleRepository(context, db.jnmfDao())
            val autoSchedules = repo.getAutoScheduledSchedules()
            for (schedule in autoSchedules) {
                scheduleAutoPlayAlarm(context, schedule.id, schedule.startTimeSecondsOfDay)
            }
        }
    }
}
