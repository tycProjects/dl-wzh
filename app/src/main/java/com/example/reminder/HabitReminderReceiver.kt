package com.example.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.HabitDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(ReminderScheduler.EXTRA_HABIT_ID, -1L)
        val habitTitle = intent.getStringExtra(ReminderScheduler.EXTRA_HABIT_TITLE) ?: "Твоя звичка"

        if (habitId == -1L) return

        showNotification(context, habitId, habitTitle)

        // Reschedule for next day if habit is still active with reminder
        CoroutineScope(Dispatchers.IO).launch {
            val db = HabitDatabase.getDatabase(context)
            val habit = db.habitDao().getHabitByIdDirect(habitId)
            if (habit != null && habit.reminderEnabled && !habit.isArchived) {
                ReminderScheduler.scheduleReminder(context, habit)
            }
        }
    }

    private fun showNotification(context: Context, habitId: Long, habitTitle: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            habitId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_flame_orange)
            .setContentTitle("🔥 Час для звички: $habitTitle")
            .setContentText("Зроби крок до мети та підтримай свій вогник палаючим!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Звичка «$habitTitle» чекає на виконання. Не дай вогнику згаснути!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(habitId.toInt(), notification)
    }
}
