package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.HabitDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object WidgetUpdater {

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, HabitStreakWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = HabitDatabase.getDatabase(context)
            val dao = db.habitDao()

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val today = dateFormat.format(Date())

            val activeHabits = dao.getActiveHabitsDirect()
            val todayCompletions = dao.getCompletionsForDateDirect(today)
            val allCompletions = dao.getAllCompletionsDirect()

            val completedCount = todayCompletions.size
            val totalHabitsCount = activeHabits.size
            val progressPercent = if (totalHabitsCount > 0) {
                ((completedCount.toFloat() / totalHabitsCount) * 100).toInt().coerceIn(0, 100)
            } else 0

            // Calculate overall streak
            val completedDates = allCompletions.map { it.date }.toSet()
            val streak = calculateStreak(completedDates, today)

            for (appWidgetId in appWidgetIds) {
                updateSingleWidget(
                    context = context,
                    appWidgetManager = appWidgetManager,
                    appWidgetId = appWidgetId,
                    streak = streak,
                    completedCount = completedCount,
                    totalCount = totalHabitsCount,
                    progressPercent = progressPercent
                )
            }
        }
    }

    fun updateSingleWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        streak: Int,
        completedCount: Int,
        totalCount: Int,
        progressPercent: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_habit_streak)

        val streakText = if (streak > 0) "🔥 $streak ${getDaysWord(streak).uppercase()}" else "🔥 ПОЧНИ СЕРІЮ"
        val tierText = when {
            streak == 0 -> "Час запалити свій вогник!"
            streak in 1..3 -> "Іскра горить! Тільки вперед"
            streak in 4..7 -> "Впевнене полум'я! Продовжуй"
            streak in 8..14 -> "Гаряче багаття дисципліни!"
            streak in 15..29 -> "Супернова! Звичка закріплена"
            else -> "Легендарний Вогник!"
        }

        views.setTextViewText(R.id.widget_streak_count, streakText)
        views.setTextViewText(R.id.widget_streak_tier, tierText)
        views.setTextViewText(R.id.widget_progress_text, "Сьогодні: $completedCount з $totalCount звичок")
        views.setTextViewText(R.id.widget_percentage_text, "$progressPercent%")
        views.setProgressBar(R.id.widget_progress_bar, 100, progressPercent, false)

        // Launch MainActivity on click
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun calculateStreak(completedDates: Set<String>, today: String): Int {
        if (completedDates.isEmpty()) return 0
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        var streak = 0

        if (completedDates.contains(today)) {
            streak++
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = dateFormat.format(cal.time)
            if (!completedDates.contains(yesterday)) {
                return 0
            }
        }

        while (true) {
            val dateStr = dateFormat.format(cal.time)
            if (completedDates.contains(dateStr)) {
                if (dateStr != today) streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    private fun getDaysWord(days: Int): String {
        val rem10 = days % 10
        val rem100 = days % 100
        return when {
            rem100 in 11..19 -> "днів"
            rem10 == 1 -> "день"
            rem10 in 2..4 -> "дні"
            else -> "днів"
        }
    }
}
