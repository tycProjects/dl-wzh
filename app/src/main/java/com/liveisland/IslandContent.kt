package com.liveisland

import android.app.PendingIntent
import android.graphics.drawable.Drawable
import android.app.PendingIntent
import android.media.session.MediaSession

class IslandAction(val label: String, val intent: PendingIntent?)

/** Что показывает остров. kind: call | music | charge | notify */
class IslandContent(
  val key: String,
  val kind: String,
  val title: String,
  val sub: String,
  val caption: String = "",
  val actions: List<IslandAction> = emptyList(),
  val progress: Int = -1,
  val icon: Drawable? = null,
  val mediaToken: MediaSession.Token? = null,
  val openIntent: PendingIntent? = null
) {
  /** Подпись содержимого: если не изменилась — перерисовывать остров не нужно */
  val sig: String get() = "$kind|$title|$sub|$caption|$progress|" + actions.joinToString(",") { it.label }
}
