package com.android.island.core

import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Bundle

/**
 * Commands the island sends to the system.
 * mediaController is set by WhatsAppNotificationListener when a session is active.
 */
object Controls {
    @Volatile
    var mediaController: MediaController? = null

    fun playPause() {
        val c = mediaController ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) {
            c.transportControls.pause()
        } else {
            c.transportControls.play()
        }
    }

    fun next() {
        mediaController?.transportControls?.skipToNext()
    }

    fun prev() {
        mediaController?.transportControls?.skipToPrevious()
    }

    fun seekTo(ms: Long) {
        mediaController?.transportControls?.seekTo(ms)
    }

    fun openApp(context: Context, pkg: String): Boolean {
        val launch = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(launch); true }.getOrDefault(false)
    }

    /**
     * Opens one WhatsApp chat from its notification intent. Android 14+ blocks background activity
     * starts from notification intents unless the sender opts in, so the start mode is requested
     * explicitly. If the intent cannot be sent, WhatsApp itself is launched instead.
     */
    fun openChat(context: Context, contentIntent: PendingIntent?, pkg: String): Boolean {
        val sent = contentIntent != null && runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                val options = android.app.ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(
                        android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    ).toBundle()
                contentIntent.send(context, 0, null, null, null, null, options)
            } else {
                contentIntent.send()
            }
            true
        }.getOrDefault(false)
        val opened = sent || openApp(context, pkg)
        com.android.island.util.IslandDiagnostics.record("WA_OPEN", "pkg=$pkg intentSent=$sent opened=$opened")
        return opened
    }

    /**
     * Send a quick reply through the notification's RemoteInput action.
     */
    fun sendReply(context: Context, action: PendingIntent?, remoteInputs: Array<RemoteInput>?, text: String): Boolean {
        if (action == null || remoteInputs.isNullOrEmpty() || text.isBlank()) return false
        return try {
            val fill = Intent().apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val results = Bundle()
            for (ri in remoteInputs) {
                results.putCharSequence(ri.resultKey, text.trim())
            }
            RemoteInput.addResultsToIntent(remoteInputs, fill, results)
            // Also set results on the intent extras for older OEM handlers
            fill.putExtra("android.intent.extra.TEXT", text.trim())
            action.send(context, 0, fill)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun fireAction(context: Context, action: PendingIntent?): Boolean {
        if (action == null) return false
        return try {
            action.send()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Try to heart/favorite the current track in the player.
     * Uses MediaController rating if the session supports it; otherwise false.
     */
    fun toggleFavorite(liked: Boolean): Boolean {
        val c = mediaController ?: return false
        return try {
            val controls = c.transportControls
            when (c.ratingType) {
                android.media.Rating.RATING_HEART -> controls.setRating(android.media.Rating.newHeartRating(liked))
                android.media.Rating.RATING_THUMB_UP_DOWN -> controls.setRating(android.media.Rating.newThumbRating(liked))
                android.media.Rating.RATING_5_STARS -> controls.setRating(android.media.Rating.newStarRating(5, if (liked) 5f else 0f))
                android.media.Rating.RATING_4_STARS -> controls.setRating(android.media.Rating.newStarRating(4, if (liked) 4f else 0f))
                android.media.Rating.RATING_3_STARS -> controls.setRating(android.media.Rating.newStarRating(3, if (liked) 3f else 0f))
                else -> {
                    val action = c.playbackState?.customActions?.firstOrNull { a ->
                        val n = "${a.action} ${a.name}".lowercase()
                        "like" in n || "heart" in n || "favorite" in n || "love" in n
                    } ?: return false
                    controls.sendCustomAction(action.action, null)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
