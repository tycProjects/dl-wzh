package com.schedule.app.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.Attendance
import com.schedule.app.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val nm = NotificationManagerCompat.from(ctx)
        when (intent.action) {
            Notifier.ACTION_DISMISS -> { nm.cancel(intent.getLongExtra("notif_id", -1).toInt()); return }
            Notifier.ACTION_SNOOZE -> {
                nm.cancel(intent.getLongExtra("notif_id", -1).toInt())
                Scheduler.snooze(ctx, intent.getLongExtra("slot_id", 0), Notifier.SNOOZE_MINUTES)
                return
            }
            Notifier.ACTION_ATTEND -> {
                val pending = goAsync(); val app = ctx.applicationContext
                val slotId = intent.getLongExtra("slot_id", 0); val status = intent.getLongExtra("status", 0).toInt()
                nm.cancel(intent.getLongExtra("notif_id", -1).toInt())
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val dao = AppDatabase.get(app).dao()
                        val day = LocalDate.now().toEpochDay()
                        val old = dao.attendanceOn(slotId, day)
                        if (old == null) dao.insertAttendance(Attendance(slotId = slotId, dateEpoch = day, status = status)) else dao.updateAttendance(old.copy(status = status))
                    } finally { pending.finish() }
                }
                return
            }
            Scheduler.ACTION_ALARM -> {}
            else -> return
        }
        val pending = goAsync()
        val app = ctx.applicationContext
        val slotId = intent.getLongExtra("slot_id", 0)
        val kind = intent.getIntExtra("kind", 0)
        val itemId = intent.getLongExtra("note_id", 0)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(app).dao()
                val settings = Prefs(app).read()
                when (kind) {
                    9 -> dao.note(itemId)?.let { n -> Notifier.note(app, n, n.slotId.takeIf { it > 0 }?.let { dao.slot(it)?.title }) }
                    8 -> dao.task(itemId)?.takeIf { !it.done }?.let { t -> Notifier.task(app, t, t.slotId.takeIf { it > 0 }?.let { dao.slot(it)?.title }) }
                    else -> dao.slot(slotId)?.let { slot ->
                        when (kind) {
                            1 -> { SilentMode.enter(app); if (settings.notifyStart) Notifier.lecture(app, slot, 1, settings) }
                            2 -> { SilentMode.exit(app); if (settings.notifyEnd) Notifier.lecture(app, slot, 2, settings) }
                            else -> Notifier.lecture(app, slot, kind, settings)
                        }
                        // الموعد نفسه في الأسبوع القادم (التنبيه المؤجَّل kind 3 لا يتكرر)
                        if (kind in 0..2) Scheduler.scheduleKind(app, slot, kind, settings.leadMinutes, LocalDateTime.now().plusMinutes(1))
                    }
                }
                Scheduler.refreshUi(app)
            } finally { pending.finish() }
        }
    }
}

/** بعد إعادة التشغيل أو تحديث التطبيق أو منح إذن المنبهات الدقيقة. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val pending = goAsync()
        val app = ctx.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try { Scheduler.rescheduleAll(app) } finally { pending.finish() }
        }
    }
}
