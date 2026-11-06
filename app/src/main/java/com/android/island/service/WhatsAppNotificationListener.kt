package com.android.island.service

import com.android.island.util.IslandDiagnostics

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import com.android.island.core.Controls
import com.android.island.core.IslandActivity
import com.android.island.core.IslandBus
import com.android.island.media.LyricsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.abs

class WhatsAppNotificationListener : NotificationListenerService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private val lyricsRepo = LyricsRepository()
    private val whatsAppPackages = setOf("com.whatsapp", "com.whatsapp.w4b")

    /** Notification keys already alerted in this listener session. Bounded to avoid unbounded growth. */
    private val alertedNotificationKeys = LinkedHashSet<String>()
    /** Last exact text per notification key; used to distinguish reposts from genuinely new messages. */
    private val lastProcessedText = LinkedHashMap<String, String>(96, 0.75f, true)

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null
    private var mediaUpdateJob: Job? = null
    private var pendingTrackKey: String? = null
    private var cachedTrackKey: String? = null
    private var cachedLyrics: List<com.android.island.core.LyricLine> = emptyList()
    private var cachedAccentTrackKey: String? = null
    private var cachedAccent: Color? = null
    // Artwork is cached per track. MediaController.getMetadata() returns a NEW Bitmap
    // instance on every call (binder copy); passing those to Compose made the blurred
    // backdrop rebuild + cross-fade on every playback callback (the ~2 s flicker).
    private var cachedArtKey: String? = null
    private var cachedArt: Bitmap? = null
    private var lastPushed: IslandActivity.Media? = null
    private var lastPushedAt: Long = 0L

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        handleSessions(controllers)
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = updateMedia(activeController)
        override fun onPlaybackStateChanged(state: PlaybackState?) = updateMedia(activeController)
    }

    // Players do not always fire a callback when the user likes/unlikes in their own app. So while the
    // island is expanded, the rating is re-read once per second. Collapsed or hidden, nothing runs.
    // The start/stop is driven by island state changes, not by a timer.
    private val refreshHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var stateJob: Job? = null
    private val refreshTick = object : Runnable {
        override fun run() {
            try { updateMedia(activeController) } catch (_: Exception) {}
            refreshHandler.postDelayed(this, 1000L)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager
        stateJob?.cancel()
        stateJob = scope.launch {
            com.android.island.core.IslandBus.state
                .map { it.islandState == com.android.island.core.IslandState.EXPANDED }
                .distinctUntilChanged()
                .collect { expanded ->
                    refreshHandler.removeCallbacks(refreshTick)
                    if (expanded) refreshHandler.post(refreshTick)
                    PhoneStateMonitor.ensureRegistered(this@WhatsAppNotificationListener)
                }
        }
        try {
            val component = ComponentName(this, WhatsAppNotificationListener::class.java)
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionListener, component)
            handleSessions(mediaSessionManager?.getActiveSessions(component))
            // Notifications already in the tray when the listener (re)connects were alerted before.
            // Remember them so a reconnect never replays their banner.
            activeNotifications?.forEach { n ->
                if (n.packageName in whatsAppPackages && !n.isOngoing) {
                    synchronized(alertedNotificationKeys) { alertedNotificationKeys.add(n.key) }
                }
            }
        } catch (_: SecurityException) {
        }
    }

    /**
     * Android Island intentionally mirrors WhatsApp notifications only.
     * Every other app is left entirely to Android/One UI: we do not mirror,
     * dismiss, cancel, suppress, or otherwise consume its notifications.
     * Media is handled separately through MediaSessionManager below.
     */
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (sbn.notification?.category == Notification.CATEGORY_CALL) {
            handleCall(sbn)
            return
        }
        // Strict allow-list: only WhatsApp and WhatsApp Business enter the Island UI.
        // Returning here does not cancel or alter the system notification.
        if (sbn.packageName !in whatsAppPackages) return
        if (sbn.isOngoing) return
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: return

        // Ignore WhatsApp's maintenance/sync status notices, not actual chats or calls.
        if (isBackgroundStatusNotice(title, text, sbn.isOngoing)) return

        val isGroup = title.contains(":") || extras.getBoolean("android.isGroupConversation", false)
        val senderName: String
        val groupName: String?
        if (isGroup) {
            val parts = title.split(":", limit = 2)
            groupName = parts.getOrNull(0)?.trim()
            senderName = parts.getOrNull(1)?.trim() ?: title
        } else {
            senderName = title
            groupName = null
        }

        val (reply, read) = extractActions(notification)

        val activity = IslandActivity.WhatsApp(
            id = sbn.key,
            senderName = senderName,
            message = text,
            isGroup = isGroup,
            groupName = groupName,
            avatar = extractAvatar(notification),
            messageCount = 1,
            conversationKey = sbn.key,
            replyAction = reply?.actionIntent,
            replyRemoteInputs = reply?.remoteInputs,
            markReadAction = read?.actionIntent,
            contentIntent = notification.contentIntent
        )

        // Alert once per notification. WhatsApp re-posts the same notification (edited text, updated
        // count, avatar refresh) and the listener can receive active notifications again after a
        // reconnect. Any key we already alerted on is refreshed silently: no banner, no vibration.
        val alreadyAlerted = synchronized(alertedNotificationKeys) {
            val seen = sbn.key in alertedNotificationKeys
            alertedNotificationKeys.add(sbn.key)
            while (alertedNotificationKeys.size > 400) alertedNotificationKeys.remove(alertedNotificationKeys.first())
            lastProcessedText[sbn.key] = text
            while (lastProcessedText.size > 160) lastProcessedText.remove(lastProcessedText.keys.first())
            seen
        }
        if (alreadyAlerted) {
            IslandBus.updateWhatsAppInBackground(activity)
            IslandDiagnostics.record("WA_SILENT_REFRESH", "key=${sbn.key.takeLast(32)} (sudah pernah ditampilkan)")
            return
        }
        IslandBus.pushActivity(activity)
        IslandDiagnostics.record("WA_ALERT", "key=${sbn.key.takeLast(32)}")
        vibrateOnce()

        // Keep unanswered chats in the island queue. They leave only after an
        // explicit reply/dismiss action or when Android reports notification removal.
    }


    private val activeCallKeys = HashSet<String>()

    private fun handleCall(sbn: StatusBarNotification) {
        val n = sbn.notification ?: return
        val extras = n.extras ?: return
        val name = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() } ?: "Panggilan"
        val sub = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        var answer: PendingIntent? = null
        var decline: PendingIntent? = null
        n.actions?.forEach { a ->
            val t = a.title?.toString()?.lowercase().orEmpty()
            if (answer == null && listOf("answer", "accept", "terima", "jawab").any { t.contains(it) }) answer = a.actionIntent
            else if (decline == null && listOf("decline", "reject", "hang", "end", "tolak", "akhiri").any { t.contains(it) }) decline = a.actionIntent
        }
        synchronized(activeCallKeys) { activeCallKeys.add(sbn.key) }
        IslandBus.pushCall(
            IslandActivity.Call(
                id = sbn.key,
                callerName = name,
                subtitle = sub,
                isIncoming = !sbn.isOngoing,
                avatar = extractAvatar(n),
                answerAction = answer,
                declineAction = decline,
                contentIntent = n.contentIntent
            )
        )
        IslandDiagnostics.record("CALL", "key=${sbn.key.takeLast(32)} incoming=${!sbn.isOngoing}")
    }

    /** Caps stored artwork at 480 px: the island draws it at about 46 dp, so the rest is wasted RAM. */
    private fun shrinkArt(src: Bitmap): Bitmap {
        val max = maxOf(src.width, src.height)
        if (max <= 480 || src.config == Bitmap.Config.HARDWARE) return src
        val k = 480f / max
        return Bitmap.createScaledBitmap(src, (src.width * k).toInt().coerceAtLeast(1), (src.height * k).toInt().coerceAtLeast(1), true)
    }

    private fun isBackgroundStatusNotice(title: String, text: String, ongoing: Boolean): Boolean {
        if (ongoing) return true
        // No regex/split/normalization pipeline on the notification callback.
        // WhatsApp's maintenance strings are stable enough for direct case-insensitive checks.
        val combined = (title + " " + text).lowercase()
        return combined.contains("checking for new messages") ||
            combined.contains("check for new messages") ||
            combined.contains("memeriksa pesan baru") ||
            combined.contains("mengecek pesan baru") ||
            combined.contains("memeriksa pesan") ||
            combined.contains("checking messages") ||
            combined.contains("syncing messages") ||
            combined.contains("menyinkronkan pesan") ||
            combined.contains("menyinkronisasi pesan")
    }

    private fun vibrateOnce() {
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val mgr = getSystemService(android.os.VibratorManager::class.java)
                mgr?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
            if (vibrator == null || !vibrator.hasVibrator()) return
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    android.os.VibrationEffect.createOneShot(45, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(45)
            }
        } catch (_: Exception) {
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (synchronized(activeCallKeys) { activeCallKeys.remove(sbn.key) }) {
            IslandBus.clearCall(sbn.key)
            return
        }
        if (sbn.packageName !in whatsAppPackages) return
        synchronized(alertedNotificationKeys) {
            alertedNotificationKeys.remove(sbn.key)
            lastProcessedText.remove(sbn.key)
        }
        // Let the overlay animate out before removing the notification model; an
        // immediate Compose/window resize was the source of the faint white flash.
        IslandBus.markWhatsAppSystemRemoving(sbn.key)
        // Fallback cleanup also covers queued notifications that are not currently composed.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            IslandBus.clearWhatsApp(sbn.key)
        }, 240L)
    }

    private data class Act(val actionIntent: PendingIntent?, val remoteInputs: Array<RemoteInput>?)

    private fun extractActions(n: Notification): Pair<Act?, Act?> {
        val actions = n.actions ?: return null to null
        var reply: Act? = null
        var read: Act? = null
        for (a in actions) {
            val inputs = a.remoteInputs
            val freeFormInputs = inputs?.filter { it.allowFreeFormInput }?.toTypedArray()
            if (!freeFormInputs.isNullOrEmpty() && a.actionIntent != null && reply == null) {
                reply = Act(a.actionIntent, freeFormInputs)
            } else if (read == null) {
                val title = a.title?.toString()?.lowercase() ?: ""
                if (title.contains("read") || title.contains("baca") || title.contains("mark")) {
                    read = Act(a.actionIntent, null)
                }
            }
        }
        if (read == null) {
            // fallback: first non-reply action
            for (a in actions) {
                if (a.remoteInputs.isNullOrEmpty()) {
                    read = Act(a.actionIntent, null)
                    break
                }
            }
        }
        return reply to read
    }

    private fun handleSessions(controllers: List<MediaController>?) {
        activeController?.unregisterCallback(controllerCallback)
        activeController = controllers?.firstOrNull {
            it.packageName !in listOf("com.android.systemui", "com.google.android.gms")
        }
        Controls.mediaController = activeController
        activeController?.registerCallback(controllerCallback)
        updateMedia(activeController)
    }

    private fun updateMedia(controller: MediaController?) {
        if (controller == null) {
            mediaUpdateJob?.cancel()
            pendingTrackKey = null
            cachedArt = null; cachedArtKey = null; lastPushed = null
            Controls.mediaController = null
            IslandBus.clearMedia()
            return
        }
        Controls.mediaController = controller
        val metadata = controller.metadata ?: return
        val state = controller.playbackState
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown"
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        val trackKey = "${controller.packageName}|$title|$artist|$album|$duration"
        val artwork: Bitmap? = if (cachedArtKey == trackKey && cachedArt != null) cachedArt else {
            val fetched = (metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART))?.let { shrinkArt(it) }
            if (fetched != null) {
                cachedArtKey = trackKey
                cachedArt = fetched
                // Build the blurred backdrop now, off the main thread, so expanding is instant.
                scope.launch(Dispatchers.Default) { com.android.island.island.prewarmArtBackdrop(fetched) }
            }
            fetched
        }
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING
        val position = state?.position ?: 0L
        // The player's own like state (heart or thumb). Null when the app exposes no rating.
        val userLiked: Boolean? = metadata.getRating(MediaMetadata.METADATA_KEY_USER_RATING)?.let { r ->
            when (r.ratingStyle) {
                android.media.Rating.RATING_HEART -> r.hasHeart()
                android.media.Rating.RATING_THUMB_UP_DOWN -> r.isThumbUp()
                else -> null
            }
        }

        // Playback-state callbacks are frequent. Reuse the current track's cached lyrics
        // and never restart a lyric request unless the actual track identity changes.
        val lyrics = if (cachedTrackKey == trackKey) cachedLyrics else emptyList()
        val accent = if (cachedAccentTrackKey == trackKey) cachedAccent else null
        val idx = lyrics.indexOfLast { it.timeMs <= position }.let { if (it < 0) 0 else it }
        val media = IslandActivity.Media(
            id = "media_${controller.packageName}",
            title = title,
            artist = artist,
            album = album,
            artwork = artwork,
            isPlaying = isPlaying,
            position = position,
            duration = duration,
            packageName = controller.packageName,
            accentColor = accent,
            lyrics = lyrics,
            currentLyricIndex = idx,
            userLiked = userLiked
        )
        // Skip no-op callbacks (position-only ticks from the player) so the UI is not
        // recomposed for nothing. Position drift under 1.5 s while playing is ignored.
        val prev = lastPushed
        val nowMs = SystemClock.elapsedRealtime()
        if (prev != null && prev.id == media.id && prev.title == title && prev.artist == artist &&
            prev.isPlaying == isPlaying && prev.duration == duration && prev.accentColor == accent &&
            prev.artwork === artwork && prev.lyrics === lyrics && prev.currentLyricIndex == idx && prev.userLiked == userLiked
        ) {
            val expected = if (isPlaying) prev.position + (nowMs - lastPushedAt) else prev.position
            if (abs(position - expected) < 1500L) return
        }
        lastPushed = media
        lastPushedAt = nowMs
        val st = IslandBus.state.value
        // Route by the actual foreground activity, not by the expanded flag alone.
        // A previously dismissed/stopped media session can leave the island HIDDEN;
        // when playback resumes, updateMedia() would only fill pendingMedia and the
        // capsule would remain invisible. pushActivity() restores the compact island.
        when {
            st.current is IslandActivity.WhatsApp -> IslandBus.updateMedia(media)
            st.current is IslandActivity.Media -> IslandBus.updateMedia(media)
            isPlaying -> IslandBus.pushActivity(media)
            st.pendingMedia != null -> IslandBus.updateMedia(media)
            else -> IslandBus.updateMedia(media)
        }

        if (cachedTrackKey != trackKey && pendingTrackKey != trackKey) {
            mediaUpdateJob?.cancel()
            pendingTrackKey = trackKey
            mediaUpdateJob = scope.launch {
                val fetchedLyrics = try {
                    lyricsRepo.fetchLyrics(title, artist, album, duration)
                } catch (_: Exception) {
                    listOf(com.android.island.core.LyricLine(0, "♪ $title"))
                }
                val fetchedAccent = artwork?.let { extractAccent(it) }
                if (pendingTrackKey == trackKey) {
                    cachedTrackKey = trackKey
                    cachedLyrics = fetchedLyrics
                    cachedAccentTrackKey = trackKey
                    cachedAccent = fetchedAccent
                    pendingTrackKey = null
                    // Refresh with the same controller; this time data comes from cache.
                    if (activeController === controller) updateMedia(controller)
                }
            }
        }
    }

    private suspend fun extractAccent(bitmap: Bitmap): Color? = withContext(Dispatchers.Default) {
        try {
            val palette = Palette.from(bitmap).generate()
            (palette.vibrantSwatch ?: palette.dominantSwatch)?.rgb?.let { Color(it) }
        } catch (_: Exception) {
            null
        }
    }

    /** Scales a bitmap down so its longest side is at most [max] px. Avatars are drawn at 20-46 dp. */
    private fun shrinkTo(src: Bitmap, max: Int): Bitmap {
        val longest = maxOf(src.width, src.height)
        if (longest <= max || src.config == Bitmap.Config.HARDWARE) return src
        val k = max.toFloat() / longest
        return Bitmap.createScaledBitmap(src, (src.width * k).toInt().coerceAtLeast(1), (src.height * k).toInt().coerceAtLeast(1), true)
    }

    private fun extractAvatar(notification: Notification): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notification.getLargeIcon()?.loadDrawable(this)?.let { d ->
                    (d as? BitmapDrawable)?.bitmap?.let { shrinkTo(it, 128) }
                }
            } else {
                @Suppress("DEPRECATION")
                notification.largeIcon?.let { shrinkTo(it, 128) }
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun onListenerDisconnected() {
        stateJob?.cancel()
        refreshHandler.removeCallbacks(refreshTick)
        PhoneStateMonitor.unregister(this)
        // A listener disconnect must not leave a stale lyric/network request running.
        mediaUpdateJob?.cancel()
        mediaUpdateJob = null
        pendingTrackKey = null
        cachedArt = null; cachedArtKey = null; lastPushed = null
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
        Controls.mediaController = null
        try { mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionListener) } catch (_: Exception) {}
        mediaSessionManager = null
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        refreshHandler.removeCallbacks(refreshTick)
        PhoneStateMonitor.unregister(this)
        mediaUpdateJob?.cancel()
        pendingTrackKey = null
        cachedArt = null; cachedArtKey = null; lastPushed = null
        activeController?.unregisterCallback(controllerCallback)
        try { mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionListener) } catch (_: Exception) {}
        activeController = null
        mediaSessionManager = null
        Controls.mediaController = null
        scope.cancel()
        synchronized(alertedNotificationKeys) {
            alertedNotificationKeys.clear()
            lastProcessedText.clear()
        }
        super.onDestroy()
    }
}
