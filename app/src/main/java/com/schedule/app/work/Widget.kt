package com.schedule.app.work

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.schedule.app.MainActivity
import com.schedule.app.R
import com.schedule.app.util.DAY_NAMES
import com.schedule.app.util.dayIndex
import com.schedule.app.util.formatMinutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/** ودجت الشاشة الرئيسية: محاضرات اليوم مع تمييز الجارية والملغاة. */
class ScheduleWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch { try { WidgetUpdater.update(context) } finally { pending.finish() } }
    }
}

object WidgetUpdater {
    private val rows = intArrayOf(R.id.w_l1, R.id.w_l2, R.id.w_l3, R.id.w_l4, R.id.w_l5)

    suspend fun update(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, ScheduleWidget::class.java))
        if (ids.isEmpty()) return
        val today = LocalDate.now()
        val now = LocalDateTime.now()
        val items = lecturesOn(ctx, today)
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val rv = RemoteViews(ctx.packageName, R.layout.widget_schedule)
        rv.setTextViewText(R.id.w_title, "${DAY_NAMES[dayIndex(today)]} ${today.dayOfMonth}/${today.monthValue}")
        rv.setOnClickPendingIntent(R.id.w_root, open)
        val holiday = items.firstOrNull { it.occ.holiday != null }?.occ?.holiday
        rows.forEachIndexed { i, id ->
            val d = items.getOrNull(i)
            if (d == null) {
                rv.setViewVisibility(id, android.view.View.GONE)
            } else {
                rv.setViewVisibility(id, android.view.View.VISIBLE)
                val live = !d.occ.cancelled && !now.isBefore(d.start) && now.isBefore(d.end)
                val mark = when { d.occ.cancelled -> "  (ملغاة)"; live -> "  (الآن)"; else -> "" }
                rv.setTextViewText(id, "${formatMinutes(d.occ.start)}   ${d.slot.title}$mark")
                rv.setTextColor(id, if (live) 0xFFE5486B.toInt() else if (d.occ.cancelled) 0xFF6B6B73.toInt() else 0xFFEDEDED.toInt())
            }
        }
        rv.setViewVisibility(R.id.w_empty, if (items.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE)
        rv.setTextViewText(R.id.w_empty, "لا توجد محاضرات اليوم")
        if (holiday != null) { rv.setViewVisibility(R.id.w_empty, android.view.View.VISIBLE); rv.setTextViewText(R.id.w_empty, "إجازة: $holiday") }
        mgr.updateAppWidget(ids, rv)
    }
}
