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

/** ودجت الواجبات: أقرب 5 واجبات غير منجزة. */
class TasksWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch { try { WidgetUpdater.update(context) } finally { pending.finish() } }
    }
}

/** ودجت صغير: المحاضرة القادمة فقط. */
class NextWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch { try { WidgetUpdater.update(context) } finally { pending.finish() } }
    }
}

object WidgetUpdater {
    private val taskRows = intArrayOf(R.id.t_l1, R.id.t_l2, R.id.t_l3, R.id.t_l4, R.id.t_l5)

    private fun open(ctx: Context, code: Int) = PendingIntent.getActivity(ctx, code, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private suspend fun updateTasks(ctx: Context, mgr: AppWidgetManager) {
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, TasksWidget::class.java)); if (ids.isEmpty()) return
        val tasks = com.schedule.app.data.AppDatabase.get(ctx).dao().allTasks().filter { !it.done }
            .sortedWith(compareBy({ it.dueEpoch == null }, { it.dueEpoch ?: 0L })).take(5)
        val rv = RemoteViews(ctx.packageName, R.layout.widget_tasks)
        rv.setOnClickPendingIntent(R.id.t_root, open(ctx, 1))
        rv.setTextViewText(R.id.t_title, "الواجبات")
        val today = LocalDate.now().toEpochDay()
        taskRows.forEachIndexed { i, id ->
            val t = tasks.getOrNull(i)
            if (t == null) rv.setViewVisibility(id, android.view.View.GONE) else {
                rv.setViewVisibility(id, android.view.View.VISIBLE)
                val due = t.dueEpoch?.let { "  (" + com.schedule.app.util.formatDateShort(it) + ")" }.orEmpty()
                rv.setTextViewText(id, t.title + due)
                rv.setTextColor(id, if (t.dueEpoch != null && t.dueEpoch < today) 0xFFEF4444.toInt() else 0xFFEDEDED.toInt())
            }
        }
        rv.setViewVisibility(R.id.t_empty, if (tasks.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE)
        rv.setTextViewText(R.id.t_empty, "لا توجد واجبات")
        mgr.updateAppWidget(ids, rv)
    }

    private suspend fun updateNext(ctx: Context, mgr: AppWidgetManager) {
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, NextWidget::class.java)); if (ids.isEmpty()) return
        val now = LocalDateTime.now()
        var found: DatedLecture? = null
        for (off in 0L..7L) {
            found = lecturesOn(ctx, LocalDate.now().plusDays(off)).firstOrNull { !it.occ.cancelled && it.end.isAfter(now) }
            if (found != null) break
        }
        val rv = RemoteViews(ctx.packageName, R.layout.widget_next)
        rv.setOnClickPendingIntent(R.id.n_root, open(ctx, 2))
        if (found == null) { rv.setTextViewText(R.id.n_title, "لا توجد محاضرات قادمة"); rv.setTextViewText(R.id.n_time, ""); rv.setTextViewText(R.id.n_room, "") }
        else {
            rv.setTextViewText(R.id.n_title, found.slot.title)
            rv.setTextViewText(R.id.n_time, DAY_NAMES[dayIndex(found.date)] + "  " + formatMinutes(found.occ.start))
            rv.setTextViewText(R.id.n_room, found.room)
        }
        mgr.updateAppWidget(ids, rv)
    }

    private val rows = intArrayOf(R.id.w_l1, R.id.w_l2, R.id.w_l3, R.id.w_l4, R.id.w_l5)

    suspend fun update(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        runCatching { updateTasks(ctx, mgr) }
        runCatching { updateNext(ctx, mgr) }
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
