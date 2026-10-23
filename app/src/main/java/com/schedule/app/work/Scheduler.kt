package com.schedule.app.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.*
import com.schedule.app.data.*
import com.schedule.app.util.ExcInfo
import com.schedule.app.util.HolidayRange
import com.schedule.app.util.nextTrigger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** يجدول منبهات المحاضرات (قبل/بداية/نهاية) مع احترام الأسابيع والإجازات والاستثناءات، ومنبهات الملاحظات والواجبات. */
object Scheduler {
    const val ACTION_ALARM = "com.schedule.app.ALARM"
    private const val NOTE_BASE = 1_000_000
    private const val TASK_BASE = 3_000_000

    private fun alarmManager(ctx: Context) = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canExact(ctx: Context) = Build.VERSION.SDK_INT < 31 || alarmManager(ctx).canScheduleExactAlarms()

    private fun pending(ctx: Context, code: Int, slotId: Long, kind: Int, itemId: Long = 0, create: Boolean = true): PendingIntent? {
        val i = Intent(ctx, AlarmReceiver::class.java).setAction(ACTION_ALARM)
            .putExtra("slot_id", slotId).putExtra("kind", kind).putExtra("note_id", itemId)
        val flags = PendingIntent.FLAG_IMMUTABLE or (if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE)
        return PendingIntent.getBroadcast(ctx, code, i, flags)
    }

    private fun setAt(ctx: Context, whenMillis: Long, pi: PendingIntent) {
        val am = alarmManager(ctx)
        if (canExact(ctx)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pi)
    }

    fun toMillis(t: LocalDateTime) = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private suspend fun excsOf(dao: AppDao, slotId: Long) = dao.allExceptions().filter { it.slotId == slotId }
        .map { ExcInfo(it.dateEpoch, it.cancelled, it.newStart, it.newEnd, it.newRoom) }

    private suspend fun holidays(dao: AppDao) = dao.allHolidays().map { HolidayRange(it.startEpoch, it.endEpoch, it.title) }

    /** يجدول موعد نوع واحد (kind) لمحاضرة بعد [now]. */
    suspend fun scheduleKind(ctx: Context, slot: Slot, kind: Int, lead: Int, now: LocalDateTime = LocalDateTime.now()) {
        if (kind == 0 && lead <= 0) return
        val dao = AppDatabase.get(ctx).dao()
        val sem = LocalDate.ofEpochDay(Prefs(ctx).read().semesterStart)
        val t = nextTrigger(slot.day, slot.weekMode, slot.startMin, slot.endMin, kind, lead, now, sem, excsOf(dao, slot.id), holidays(dao)) ?: return
        val pi = pending(ctx, Notifier.notifId(slot.id, kind), slot.id, kind) ?: return
        setAt(ctx, toMillis(t), pi)
    }

    fun cancelSlot(ctx: Context, slotId: Long) {
        for (k in 0..3) pending(ctx, Notifier.notifId(slotId, k), slotId, k, create = false)?.let { alarmManager(ctx).cancel(it); it.cancel() }
    }

    suspend fun scheduleSlot(ctx: Context, slot: Slot) {
        val s = Prefs(ctx).read()
        cancelSlot(ctx, slot.id)
        if (s.leadMinutes > 0) scheduleKind(ctx, slot, 0, s.leadMinutes)
        if (s.notifyStart || s.silentMode || s.askRecord) scheduleKind(ctx, slot, 1, 0)
        scheduleKind(ctx, slot, 2, 0) // النهاية دائما: لإشعار الحضور واستعادة وضع الصوت
    }

    /** تأجيل التنبيه: منبه لمرة واحدة بعد [minutes] دقيقة. */
    fun snooze(ctx: Context, slotId: Long, minutes: Int) {
        val pi = pending(ctx, Notifier.notifId(slotId, 3), slotId, 3) ?: return
        setAt(ctx, System.currentTimeMillis() + minutes * 60_000L, pi)
    }

    fun cancelNote(ctx: Context, noteId: Long) {
        pending(ctx, NOTE_BASE + noteId.toInt(), 0, 9, noteId, create = false)?.let { alarmManager(ctx).cancel(it); it.cancel() }
    }

    private fun scheduleDayBefore(ctx: Context, code: Int, slotId: Long, kind: Int, itemId: Long, dateEpoch: Long) {
        val at = LocalDate.ofEpochDay(dateEpoch - 1).atTime(9, 0)
        if (!at.isAfter(LocalDateTime.now())) return
        val pi = pending(ctx, code, slotId, kind, itemId) ?: return
        setAt(ctx, toMillis(at), pi)
    }

    /** تذكير الساعة 9 صباحاً من اليوم السابق لتاريخ الملاحظة. */
    fun scheduleNote(ctx: Context, note: Note) {
        cancelNote(ctx, note.id)
        val date = note.dateEpoch ?: return
        if (!Prefs(ctx).read().noteReminder) return
        scheduleDayBefore(ctx, NOTE_BASE + note.id.toInt(), note.slotId, 9, note.id, date)
    }

    fun cancelTask(ctx: Context, taskId: Long) {
        pending(ctx, TASK_BASE + taskId.toInt(), 0, 8, taskId, create = false)?.let { alarmManager(ctx).cancel(it); it.cancel() }
    }

    fun scheduleTask(ctx: Context, task: Task) {
        cancelTask(ctx, task.id)
        val date = task.dueEpoch ?: return
        if (task.done || !Prefs(ctx).read().noteReminder) return
        scheduleDayBefore(ctx, TASK_BASE + task.id.toInt(), task.slotId, 8, task.id, date)
    }

    /** إعادة جدولة كل شيء (بعد الإقلاع أو الاستيراد أو تغيير الإعدادات). */
    suspend fun rescheduleAll(ctx: Context) {
        val dao = AppDatabase.get(ctx).dao()
        dao.allSlots().forEach { scheduleSlot(ctx, it) }
        dao.allNotes().forEach { scheduleNote(ctx, it) }
        dao.allTasks().forEach { scheduleTask(ctx, it) }
        applyBackup(ctx, Prefs(ctx).read().autoBackup)
        refreshUi(ctx)
    }

    /** يحدّث الودجت والإشعار الدائم بعد أي تغيير في البيانات. */
    suspend fun refreshUi(ctx: Context) {
        runCatching { NextLecture.refresh(ctx) }
        runCatching { WidgetUpdater.update(ctx) }
    }

    // ---- النسخ الاحتياطي التلقائي
    fun applyBackup(ctx: Context, mode: Int) {
        val wm = WorkManager.getInstance(ctx)
        if (mode == 0) { wm.cancelUniqueWork("auto_backup"); return }
        val days = if (mode == 1) 1L else 7L
        wm.enqueueUniquePeriodicWork("auto_backup", ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<BackupWorker>(days, TimeUnit.DAYS).build())
    }
}
