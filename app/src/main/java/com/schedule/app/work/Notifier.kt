package com.schedule.app.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.schedule.app.MainActivity
import com.schedule.app.R
import com.schedule.app.data.Note
import com.schedule.app.data.Settings
import com.schedule.app.data.Slot
import com.schedule.app.data.Task
import com.schedule.app.util.formatMinutes

object Notifier {
    const val CH_LECTURES = "lectures"
    const val CH_RECORDING = "recording"
    const val CH_ONGOING = "ongoing"
    const val CH_SUMMARY = "summary"
    const val EXTRA_RECORD_SLOT = "record_slot"
    const val EXTRA_OPEN_SLOT = "open_slot"
    const val ACTION_DISMISS = "com.schedule.app.DISMISS"
    const val ACTION_SNOOZE = "com.schedule.app.SNOOZE"
    const val ACTION_ATTEND = "com.schedule.app.ATTEND"
    const val SNOOZE_MINUTES = 5

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_LECTURES, "تنبيهات المحاضرات", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(CH_RECORDING, "تسجيل المحاضرة", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_ONGOING, "المحاضرة القادمة", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_SUMMARY, "الملخصات والامتحانات", NotificationManager.IMPORTANCE_DEFAULT))
    }

    /** قناة بنغمة خاصة بالمادة (نغمة القناة لا تتغير بعد إنشائها لذا يُشتق اسمها من النغمة). */
    fun channelFor(ctx: Context, slot: Slot): String {
        if (slot.sound.isBlank() || Build.VERSION.SDK_INT < 26) return CH_LECTURES
        val id = "lec_" + slot.sound.hashCode().toUInt().toString(16)
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(id) == null) {
            val ch = NotificationChannel(id, "تنبيهات بنغمة مخصصة", NotificationManager.IMPORTANCE_HIGH)
            runCatching {
                ch.setSound(android.net.Uri.parse(slot.sound), android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            }
            nm.createNotificationChannel(ch)
        }
        return id
    }

    fun canPost(ctx: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private const val IMMUTABLE_UPDATE = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun openIntent(ctx: Context, code: Int, key: String, slotId: Long): PendingIntent =
        PendingIntent.getActivity(
            ctx, code, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra(key, slotId),
            IMMUTABLE_UPDATE,
        )

    private fun broadcast(ctx: Context, code: Int, action: String, vararg extras: Pair<String, Long>): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java).setAction(action)
        extras.forEach { (k, v) -> i.putExtra(k, v) }
        return PendingIntent.getBroadcast(ctx, code, i, IMMUTABLE_UPDATE)
    }

    /** extend(WearableExtender) يجعل الإشعار وأزراره تظهر على الساعة الذكية المقترنة تلقائيا. */
    private fun base(ctx: Context, title: String, text: String, channel: String = CH_LECTURES) =
        NotificationCompat.Builder(ctx, channel).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title).setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_REMINDER).setAutoCancel(true)
            .extend(NotificationCompat.WearableExtender())

    private fun details(s: Slot, room: String? = null) = buildString {
        append(s.title)
        val r = room ?: s.room
        if (r.isNotBlank()) append(" - ").append(r)
        append(" - ").append(formatMinutes(s.startMin))
        if (s.bring.isNotBlank()) append("\nأحضر: ").append(s.bring)
    }

    private fun post(ctx: Context, id: Int, b: NotificationCompat.Builder) {
        if (!canPost(ctx)) return
        ensureChannels(ctx)
        runCatching { NotificationManagerCompat.from(ctx).notify(id, b.build()) }
    }

    fun notifId(slotId: Long, kind: Int) = (slotId * 4 + kind).toInt()

    /** kind 0: قبل البداية (مع تأجيل)، 1: البداية (مع سؤال التسجيل)، 2: النهاية (مع تسجيل الحضور)، 3: تذكير بعد التأجيل */
    fun lecture(ctx: Context, slot: Slot, kind: Int, s: Settings) {
        val id = notifId(slot.id, kind)
        val open = openIntent(ctx, id, EXTRA_OPEN_SLOT, slot.id)
        when (kind) {
            0, 3 -> {
                val b = base(ctx, if (kind == 0) "تبدأ المحاضرة بعد ${s.leadMinutes} دقيقة" else "تذكير: المحاضرة ستبدأ قريبا", details(slot), channelFor(ctx, slot)).setContentIntent(open)
                if (kind == 0) b.addAction(0, "ذكّرني بعد $SNOOZE_MINUTES دقائق", broadcast(ctx, id + 20_000, ACTION_SNOOZE, "slot_id" to slot.id, "notif_id" to id.toLong()))
                post(ctx, id, b)
            }
            1 -> {
                val b = base(ctx, "بدأت المحاضرة", if (s.askRecord) "${details(slot)}\nهل تريد تسجيل المحاضرة؟" else details(slot), channelFor(ctx, slot)).setContentIntent(open)
                if (s.askRecord) {
                    b.addAction(0, "تسجيل", openIntent(ctx, id + 10_000, EXTRA_RECORD_SLOT, slot.id))
                    b.addAction(0, "ليس الآن", broadcast(ctx, id + 50_000, ACTION_DISMISS, "notif_id" to id.toLong()))
                }
                post(ctx, id, b)
            }
            else -> {
                val recording = RecordService.state.value.takeIf { it.active && it.slotId == slot.id }
                val b = base(ctx, "انتهت المحاضرة", (if (recording != null) "${slot.title}\nالتسجيل ما زال يعمل." else "${slot.title}\nسجّل حضورك أو أضف ملاحظة.")).setContentIntent(open)
                b.addAction(0, "حاضر", broadcast(ctx, id + 30_000, ACTION_ATTEND, "slot_id" to slot.id, "status" to 0L, "notif_id" to id.toLong()))
                b.addAction(0, "غائب", broadcast(ctx, id + 40_000, ACTION_ATTEND, "slot_id" to slot.id, "status" to 1L, "notif_id" to id.toLong()))
                if (recording != null) b.addAction(0, "إيقاف التسجيل", RecordService.stopIntent(ctx))
                post(ctx, id, b)
            }
        }
    }

    fun note(ctx: Context, note: Note, slotTitle: String?) {
        val id = 2_000_000 + note.id.toInt()
        val open = openIntent(ctx, id, EXTRA_OPEN_SLOT, note.slotId)
        post(ctx, id, base(ctx, "تذكير بملاحظة غدا - ${slotTitle ?: "ملاحظةاليوم"}", note.text).setContentIntent(open))
    }

    fun task(ctx: Context, task: Task, slotTitle: String?) {
        val id = 3_000_000 + task.id.toInt()
        val open = openIntent(ctx, id, EXTRA_OPEN_SLOT, task.slotId)
        post(ctx, id, base(ctx, "واجب موعد تسليمه غدا${slotTitle?.let { " - $it" } ?: ""}", task.title).setContentIntent(open))
    }

    private fun openApp(ctx: Context, code: Int, route: String? = null): PendingIntent =
        PendingIntent.getActivity(ctx, code, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP).also { i -> route?.let { i.putExtra("go", it) } }, IMMUTABLE_UPDATE)

    fun summary(ctx: Context, id: Int, title: String, text: String) =
        post(ctx, id, base(ctx, title, text, CH_SUMMARY).setContentIntent(openApp(ctx, id, "calendar")))

    fun examAlert(ctx: Context, title: String, daysLeft: Int) {
        val when_ = when (daysLeft) { 1 -> "غدا"; 0 -> "اليوم"; else -> "بعد $daysLeft أيام" }
        post(ctx, 5_000_000 + title.hashCode().and(0xFFFF), base(ctx, "امتحان $when_", title, CH_SUMMARY).setContentIntent(openApp(ctx, 5_000_001, "study")))
    }

    fun forgotAttendance(ctx: Context, titles: List<String>) =
        post(ctx, 4_100_000, base(ctx, "لم تسجّل حضور اليوم", titles.joinToString("\n"), CH_SUMMARY).setContentIntent(openApp(ctx, 4_100_000, "calendar")))

    fun pomoDone(ctx: Context, breakPhase: Boolean) =
        post(ctx, 6_000_000, base(ctx, if (breakPhase) "انتهت الاستراحة" else "انتهت جلسة المذاكرة", if (breakPhase) "ابدأ جلسة جديدة." else "تم تسجيل الوقت. خذ استراحة قصيرة.", CH_SUMMARY).setContentIntent(openApp(ctx, 6_000_000, "study")))
}
