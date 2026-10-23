package com.lecturemanager.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.lecturemanager.app.MainActivity
import com.lecturemanager.app.R
import com.lecturemanager.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** ويدجت الشاشة الرئيسية: نسبة الإنجاز والمحاضرة التالية. */
class StudyWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { WidgetUpdater.update(context) } finally { pending.finish() }
        }
    }
}

object WidgetUpdater {
    suspend fun update(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, StudyWidget::class.java))
        if (ids.isEmpty()) return
        val dao = AppDatabase.get(context).dao()
        val lectures = dao.allLectures()
        val subjects = dao.allSubjects().associateBy { it.id }
        val done = lectures.count { it.isCompleted }
        val pct = if (lectures.isEmpty()) 0 else done * 100 / lectures.size
        val next = lectures.filter { !it.isCompleted }
            .sortedWith(compareBy({ subjects[it.subjectId]?.sortOrder ?: 0 }, { it.weekNumber }, { it.sortOrder }, { it.id }))
            .firstOrNull()

        val pi = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val rv = RemoteViews(context.packageName, R.layout.widget_study).apply {
            setTextViewText(R.id.w_percent, "$pct٪")
            setTextViewText(R.id.w_sub, "$done من ${lectures.size} محاضرة")
            setProgressBar(R.id.w_progress, 100, pct, false)
            setTextViewText(
                R.id.w_next,
                if (next != null) "التالية: ${next.title}" + (subjects[next.subjectId]?.let { " • ${it.title}" } ?: "")
                else if (lectures.isEmpty()) "أضف أول محاضرة" else "أنجزت كل المحاضرات 🎉",
            )
            setOnClickPendingIntent(R.id.widget_root, pi)
        }
        ids.forEach { mgr.updateAppWidget(it, rv) }
    }
}
