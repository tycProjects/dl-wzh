package com.dyland.app
import android.app.*
import android.content.*
import android.graphics.drawable.Drawable
import android.os.*
import android.service.notification.*

data class Ev(val title: String, val text: String, val icon: Drawable?, val intent: PendingIntent? = null, val tint: Boolean = false)
object Bus { var sink: ((Ev) -> Unit)? = null; fun post(e: Ev) { sink?.invoke(e) } }
object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("d", 0)
    fun top(c: Context) = sp(c).getInt("top", 4)
    fun setTop(c: Context, v: Int) = sp(c).edit().putInt("top", v).apply()
}
class NotifService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName || sbn.isOngoing) return
        val x = sbn.notification.extras
        val t = x.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val body = x.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val ic = try { packageManager.getApplicationIcon(sbn.packageName) } catch (e: Exception) { null }
        Handler(Looper.getMainLooper()).post { Bus.post(Ev(t, body, ic, sbn.notification.contentIntent)) }
    }
}
