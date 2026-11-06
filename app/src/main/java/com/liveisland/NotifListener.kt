package com.liveisland

import android.app.Notification
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.media.session.MediaSession

/**
 * Ловит звонки, таймеры, музыку и уведомления из ВСЕХ приложений.
 * Звонки: по категории CALL, а также по приложениям звонков (Meet, Zoom, Teams, WhatsApp...).
 * Таймеры: по категориям ALARM / STOPWATCH и по приложениям часов.
 */
class NotifListener : NotificationListenerService() {

  companion object {
    /** Приложения звонков и видеовстреч */
    private val CALL_APPS = setOf(
      "com.google.android.apps.meetings", "com.google.android.apps.tachyon", "com.google.android.dialer",
      "com.android.incallui", "com.android.server.telecom", "com.samsung.android.incallui",
      "com.whatsapp", "com.whatsapp.w4b", "org.telegram.messenger", "org.telegram.messenger.web",
      "us.zoom.videomeetings", "com.microsoft.teams", "com.skype.raider", "com.discord",
      "com.viber.voip", "com.facebook.orca", "com.facebook.mlite", "org.thoughtcrime.securesms",
      "com.snapchat.android", "com.google.android.apps.googlevoice", "com.slack", "com.yandex.telemost"
    )
    /** Приложения часов и таймеров */
    private val TIMER_APPS = setOf(
      "com.google.android.deskclock", "com.android.deskclock", "com.sec.android.app.clockpackage",
      "com.miui.clock", "com.oneplus.deskclock", "com.huawei.deskclock", "com.coloros.alarmclock"
    )
    private val CALL_WORDS = listOf("звонок", "звоним", "входящ", "вызов", "call", "meet", "встреча", "conference", "video", "видео", "голос")
    private val TIMER_WORDS = listOf("таймер", "timer", "секундомер", "stopwatch", "осталось", "remaining")
  }

  private fun appLabel(pkg: String): String = try {
    packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
  } catch (_: Exception) { "" }

  /** Определяем тип события: call | timer | music | notify или null — не показывать */
  private fun kindOf(sbn: StatusBarNotification): String? {
    val n = sbn.notification
    val e = n.extras
    val pkg = sbn.packageName
    val lower = ((e.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "") + " " +
      (e.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "")).lowercase()
    val ongoing = sbn.isOngoing
    return when {
      n.category == Notification.CATEGORY_CALL -> "call"
      pkg in CALL_APPS && ongoing && CALL_WORDS.any { lower.contains(it) } -> "call"
      pkg in CALL_APPS && n.category == Notification.CATEGORY_CALL -> "call"
      n.category == Notification.CATEGORY_ALARM || n.category == Notification.CATEGORY_STOPWATCH -> "timer"
      pkg in TIMER_APPS && (ongoing || TIMER_WORDS.any { lower.contains(it) }) -> "timer"
      ongoing && TIMER_WORDS.any { lower.contains(it) } -> "timer"
      e.containsKey(Notification.EXTRA_MEDIA_SESSION) || n.category == Notification.CATEGORY_TRANSPORT -> "music"
      else -> "notify"
    }
  }

  override fun onNotificationPosted(sbn: StatusBarNotification) {
    if (sbn.packageName == packageName) return
    val n = sbn.notification
    val e = n.extras
    if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return   // сводки групп не нужны
    val kind = kindOf(sbn) ?: return
    if (kind == "notify" && sbn.isOngoing) return                    // «приложение работает», загрузки и т.п.

    val label = appLabel(sbn.packageName)
    val title = (e.getCharSequence(Notification.EXTRA_TITLE)
      ?: e.getCharSequence(Notification.EXTRA_TITLE_BIG)
      ?: e.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE))?.toString()?.takeIf { it.isNotBlank() } ?: label
    if (title.isBlank()) return
    val text = (e.getCharSequence(Notification.EXTRA_TEXT)
      ?: e.getCharSequence(Notification.EXTRA_BIG_TEXT)
      ?: e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.lastOrNull())?.toString() ?: ""

    val actions = (n.actions ?: emptyArray())
      .filter { a -> a.actionIntent != null && !a.title.isNullOrBlank() && a.remoteInputs.isNullOrEmpty() }
      .take(2)
      .map { IslandAction(it.title.toString(), it.actionIntent) }
    val acts = if (actions.isEmpty() && n.contentIntent != null)
      listOf(IslandAction(getString(R.string.action_open), n.contentIntent)) else actions

    val icon = if (kind == "notify") try { n.smallIcon?.loadDrawable(this) } catch (_: Exception) { null } else null
    val caption = if (kind == "music") "" else label

    IslandService.instance?.submit(IslandContent(
      sbn.key, kind, title, text, caption = caption, actions = acts, icon = icon,
      mediaToken = if (kind == "music") mediaToken(e) else null, openIntent = n.contentIntent))
  }

  @Suppress("DEPRECATION")
  private fun mediaToken(e: Bundle): MediaSession.Token? =
    if (Build.VERSION.SDK_INT >= 33)
      e.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
    else e.getParcelable(Notification.EXTRA_MEDIA_SESSION) as? MediaSession.Token

  // Любое удаление уведомления: если это был активный звонок/таймер/музыка — убираем остров
  override fun onNotificationRemoved(sbn: StatusBarNotification) {
    IslandService.instance?.removeKey(sbn.key)
  }

  // Система иногда отключает слушатель — просим подключить обратно
  override fun onListenerDisconnected() {
    super.onListenerDisconnected()
    requestRebind(android.content.ComponentName(this, NotifListener::class.java))
  }
}
