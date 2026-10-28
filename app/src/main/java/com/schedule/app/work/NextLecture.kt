package com.schedule.app.work

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.schedule.app.R
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.Prefs
import com.schedule.app.data.Slot
import com.schedule.app.util.*
import java.time.LocalDate
import java.time.LocalDateTime

/** محاضرة محسوبة بتاريخها الفعلي بعد تطبيق الأسابيع والاستثناءات والإجازات. */
data class DatedLecture(val slot: Slot, val date: LocalDate, val occ: Occurrence) {
    val start: LocalDateTime get() = date.atTime(occ.start / 60, occ.start % 60)
    val end: LocalDateTime get() = date.atTime(occ.end / 60, occ.end % 60)
    val room: String get() = occ.room ?: slot.room
}

/** كل محاضرات تاريخ معين مرتبة بالوقت (تشمل الملغاة للعرض). */
suspend fun lecturesOn(ctx: Context, date: LocalDate): List<DatedLecture> {
    val dao = AppDatabase.get(ctx).dao()
    val sem = LocalDate.ofEpochDay(Prefs(ctx).read().semesterStart)
    val excs = dao.allExceptions()
    val hol = dao.allHolidays().map { HolidayRange(it.startEpoch, it.endEpoch, it.title) }
    return dao.slotsOf(Prefs(ctx).read().activeSchedule).mapNotNull { s ->
        val ex = excs.filter { it.slotId == s.id }.map { ExcInfo(it.dateEpoch, it.cancelled, it.newStart, it.newEnd, it.newRoom) }
        occurrence(s.day, s.weekMode, s.startMin, s.endMin, date, sem, ex, hol, s.dateSet())?.let { DatedLecture(s, date, it) }
    }.sortedBy { it.occ.start }
}

/** الإشعار الدائم: المحاضرة الجارية أو القادمة مع عدّ تنازلي حي. */
object NextLecture {
    private const val ID = 900

    suspend fun refresh(ctx: Context) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!Prefs(ctx).read().ongoing || !Notifier.canPost(ctx)) { nm.cancel(ID); return }
        val now = LocalDateTime.now()
        var live: DatedLecture? = null
        var next: DatedLecture? = null
        for (offset in 0L..7L) {
            val items = lecturesOn(ctx, LocalDate.now().plusDays(offset)).filter { !it.occ.cancelled }
            if (live == null) live = items.firstOrNull { !now.isBefore(it.start) && now.isBefore(it.end) }
            if (next == null) next = items.firstOrNull { it.start.isAfter(now) }
            if (next != null) break
        }
        val target = live ?: next
        if (target == null) { nm.cancel(ID); return }
        Notifier.ensureChannels(ctx)
        val isLive = target === live
        val whenMs = Scheduler.toMillis(if (isLive) target.end else target.start)
        val text = buildString {
            append(formatRange(target.occ.start, target.occ.end))
            if (target.room.isNotBlank()) append(" - ").append(target.room)
            if (!isLive && target.date != LocalDate.now()) append(" - ").append(DAY_NAMES[dayIndex(target.date)])
        }
        val b = NotificationCompat.Builder(ctx, Notifier.CH_ONGOING).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle((if (isLive) "جارية الآن: " else "المحاضرة القادمة: ") + target.slot.title).setContentText(text)
            .setOngoing(true).setOnlyAlertOnce(true).setShowWhen(true).setWhen(whenMs)
            .setUsesChronometer(true).setChronometerCountDown(true)
            .setContentIntent(Notifier.openIntent(ctx, ID, Notifier.EXTRA_OPEN_SLOT, target.slot.id))
        runCatching { nm.notify(ID, b.build()) }
    }
}
