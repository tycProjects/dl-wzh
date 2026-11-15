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
import android.os.Bundle
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

    companion object {
        @Volatile private var live: WhatsAppNotificationListener? = null

        /**
         * Called when the screen turns on. After a long idle Android may have dropped the listener and the
         * media-session callbacks may be stale: reconnect if needed, otherwise re-read the active sessions.
         */
        fun onScreenOn(context: android.content.Context) {
            val l = live
            if (l != null) {
                l.resyncMedia()
                return
            }
            // Not connected. After a long idle Android unbinds the listener, and a single rebind request
            // can be dropped, so nothing ever reconnected it (only force-stop did). Ask again a few times
            // until onListenerConnected sets `live`.
            val app = context.applicationContext
            requestListenerRebind(app)
            for (d in longArrayOf(3_000L, 15_000L, 60_000L)) {
                rebindHandler.postDelayed({ if (live == null) requestListenerRebind(app) }, d)
            }
        }

        private val rebindHandler by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }

        /** Called under system memory pressure (see IslandApplication.onTrimMemory). */
        fun onTrimMemory() {
            live?.lyricsRepo?.trimCache()
        }

        /** Called by the renderer when the explicit Low-End switch changes. */
        fun onLowEndModeChanged(enabled: Boolean) {
            live?.applyLowEndMode(enabled)
        }

        private fun requestListenerRebind(context: android.content.Context) {
            runCatching {
                android.service.notification.NotificationListenerService.requestRebind(
                    ComponentName(context, WhatsAppNotificationListener::class.java)
                )
            }
            IslandDiagnostics.record("LISTENER_REBIND", "requested connected=${live != null}")
        }
    }

    /** Re-reads the active media sessions now, so playback resumed after idle is shown again. */
    private fun resyncMedia() {
        val comp = ComponentName(this, WhatsAppNotificationListener::class.java)
        runCatching { handleSessions(mediaSessionManager?.getActiveSessions(comp)) }
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private val lyricsRepo = LyricsRepository()
    private val whatsAppPackages = setOf("com.whatsapp", "com.whatsapp.w4b")

    /** Notification keys already alerted in this listener session. Bounded to avoid unbounded growth. */
    private val alertedNotificationKeys = LinkedHashSet<String>()
    /** Last message-content fingerprint per notification key. */
    private val lastProcessedText = LinkedHashMap<String, String>(96, 0.75f, true)
    /** Message-level dedupe across notification-key changes/reposts, bounded for long sessions. */
    private val seenMessageFingerprints = LinkedHashSet<String>()

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null
    private var mediaUpdateJob: Job? = null
    private var pendingTrackKey: String? = null
    /** Track identity currently on screen. Background artwork is only published if it still matches. */
    private var currentTrackKey: String? = null
    private var cachedTrackKey: String? = null
    private var cachedLyrics: List<com.android.island.core.LyricLine> = emptyList()
    private var cachedAccentTrackKey: String? = null
    private var cachedAccent: Color? = null
    // Artwork is cached per track. MediaController.getMetadata() returns a NEW Bitmap
    // instance on every call (binder copy); passing those to Compose made the blurred
    // backdrop rebuild + cross-fade on every playback callback (the ~2 s flicker).
    private var cachedArtKey: String? = null
    private var cachedArt: Bitmap? = null
    private var appliedLowEndMode: Boolean = com.android.island.core.DeviceProfile.staticGlass
    private var lastPushed: IslandActivity.Media? = null
    private var lastPushedAt: Long = 0L

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        handleSessions(controllers)
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = updateMedia(activeController)
        override fun onPlaybackStateChanged(state: PlaybackState?) = updateMedia(activeController)
    }

    // Players do not always fire a callback when the user likes/unlikes in their own app. Three things
    // keep the heart honest without polling while collapsed:
    //  1. the player's media notification is watched (it is reposted when the heart changes),
    //  2. the rating / custom actions are re-read the moment the island expands (plus two short retries,
    //     because players update a beat after the change),
    //  3. while expanded and the screen is on, a slow 2.5 s check catches anything else.
    // Collapsed or hidden, nothing runs. Start/stop is driven by island state changes, not by a timer.
    private val refreshHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var stateJob: Job? = null
    private var repeatJob: Job? = null
    private val repeatHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var lastRepeatSeekAt = 0L
    /** Like state read from the active player's media notification; null when it shows none. */
    private var mediaNotifLiked: Boolean? = null
    private var mediaNotifPackage: String? = null

    private val refreshTick = object : Runnable {
        override fun run() {
            refreshLikeAndRepeat()
            refreshHandler.postDelayed(this, 2500L)
        }
    }
    private val refreshBurst = Runnable { refreshLikeAndRepeat() }

    /** Re-reads heart and repeat from the player. Pushes to the UI only when one of them changed. */
    private fun refreshLikeAndRepeat() {
        try {
            com.android.island.core.ScreenState.refresh(this)
            if (!com.android.island.core.ScreenState.interactive.value || com.android.island.core.ScreenState.locked.value) return
            val c = activeController ?: return
            Controls.refreshRepeat()
            val liked = currentLiked(c, c.metadata)
            if (liked != lastPushed?.userLiked) updateMedia(c)
        } catch (_: Exception) {
        }
    }

    private fun currentLiked(controller: MediaController, metadata: MediaMetadata?): Boolean? =
        Controls.readLiked(controller, metadata)
            ?: if (mediaNotifPackage == controller.packageName) mediaNotifLiked else null

    /** Restarts the track just before it ends, only while repeat-one is on and the player does not do it itself. */
    private val repeatRunnable = Runnable {
        val c = activeController ?: return@Runnable
        if (!Controls.repeatGuard.value) return@Runnable
        if (c.playbackState?.state != PlaybackState.STATE_PLAYING) return@Runnable
        val now = SystemClock.elapsedRealtime()
        if (now - lastRepeatSeekAt < 1500L) return@Runnable
        lastRepeatSeekAt = now
        c.transportControls.seekTo(0L)
    }

    private fun armRepeatGuard(controller: MediaController?, duration: Long) {
        repeatHandler.removeCallbacks(repeatRunnable)
        if (controller == null || !Controls.repeatGuard.value || duration < 3000L) return
        val st = controller.playbackState ?: return
        if (st.state != PlaybackState.STATE_PLAYING || st.position < 0L) return
        val speed = if (st.playbackSpeed > 0f) st.playbackSpeed else 1f
        val elapsed = (SystemClock.elapsedRealtime() - st.lastPositionUpdateTime).coerceAtLeast(0L)
        val pos = st.position + (elapsed * speed).toLong()
        val remainingMs = ((duration - pos) / speed).toLong()
        // One wake-up per track, 450 ms before the end. Nothing runs between tracks.
        repeatHandler.postDelayed(repeatRunnable, (remainingMs - 450L).coerceAtLeast(0L))
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        live = this
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager
        stateJob?.cancel()
        stateJob = scope.launch {
            com.android.island.core.IslandBus.state
                .map { it.islandState == com.android.island.core.IslandState.EXPANDED }
                .distinctUntilChanged()
                .collect { expanded ->
                    refreshHandler.removeCallbacks(refreshTick)
                    refreshHandler.removeCallbacks(refreshBurst)
                    if (expanded) {
                        // Opening the island always re-reads the heart: now, and twice more shortly after.
                        refreshHandler.post(refreshTick)
                        refreshHandler.postDelayed(refreshBurst, 450L)
                        refreshHandler.postDelayed(refreshBurst, 1300L)
                    }
                    PhoneStateMonitor.ensureRegistered(this@WhatsAppNotificationListener)
                }
        }
        repeatJob?.cancel()
        repeatJob = scope.launch {
            // Turning repeat-one on/off (or the player confirming it) re-plans the end-of-track restart.
            Controls.repeatGuard.collect { armRepeatGuard(activeController, activeController?.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L) }
        }
        try {
            val component = ComponentName(this, WhatsAppNotificationListener::class.java)
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionListener, component)
            handleSessions(mediaSessionManager?.getActiveSessions(component))
            // Notifications already in the tray when the listener (re)connects were alerted before.
            // Remember them so a reconnect never replays their banner.
            // A WhatsApp message that arrived while the listener was unbound is still in the tray. It is new
            // to the user (opening the chat removes it from the tray), so show it. Older tray entries are
            // marked as already seen, so a reconnect never replays old banners.
            val now = System.currentTimeMillis()
            val missed = ArrayList<android.service.notification.StatusBarNotification>()
            activeNotifications?.forEach { n ->
                if (n.packageName in whatsAppPackages && !n.isOngoing && now - n.postTime < 120_000L) {
                    missed.add(n)
                } else if (n.packageName in whatsAppPackages && !n.isOngoing) {
                    val extras = n.notification?.extras
                    val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                    val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                        ?: extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                    if (!title.isNullOrBlank() && !text.isNullOrBlank()) {
                        val fingerprint = messageFingerprint(n, title, text)
                        synchronized(alertedNotificationKeys) {
                            alertedNotificationKeys.add(n.key)
                            lastProcessedText[n.key] = fingerprint
                            seenMessageFingerprints.add(fingerprint)
                            while (seenMessageFingerprints.size > 300) {
                                seenMessageFingerprints.remove(seenMessageFingerprints.first())
                            }
                        }
                    }
                }
            }
            // Show the messages that were missed while unbound, oldest first. The usual dedupe still applies.
            missed.sortedBy { it.postTime }.forEach { onNotificationPosted(it) }
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
        watchMediaNotification(sbn)
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
            // Avatar decoding is intentionally deferred to Dispatchers.IO below. Notification
            // callbacks run on the service main thread; loading a drawable and scaling its bitmap
            // here can block the first banner frame on slower devices.
            avatar = null,
            messageCount = 1,
            conversationKey = sbn.key,
            replyAction = reply?.actionIntent,
            replyRemoteInputs = reply?.remoteInputs,
            markReadAction = read?.actionIntent,
            contentIntent = notification.contentIntent
        )

        // WhatsApp commonly reuses one StatusBarNotification key for an entire chat.
        // Compare the actual MessagingStyle message timestamps as well as text: two consecutive
        // messages can legitimately contain identical text (for example, "OK" followed by "OK").
        // Metadata-only reposts keep the same message fingerprint and therefore stay silent.
        val fingerprint = messageFingerprint(sbn, title, text)
        val (alertDecision, hasNewMessageContent) = synchronized(alertedNotificationKeys) {
            val seenForKey = sbn.key in alertedNotificationKeys
            val previousFingerprint = lastProcessedText[sbn.key]
            val hasNewMessageContent = previousFingerprint != null && previousFingerprint != fingerprint
            // Dedupe by message, not only by StatusBarNotification key. WhatsApp/OEMs can
            // recreate the notification with a new key after reply/read or a repost.
            val seenMessage = fingerprint in seenMessageFingerprints
            alertedNotificationKeys.add(sbn.key)
            while (alertedNotificationKeys.size > 200) alertedNotificationKeys.remove(alertedNotificationKeys.first())
            lastProcessedText[sbn.key] = fingerprint
            while (lastProcessedText.size > 200) lastProcessedText.remove(lastProcessedText.keys.first())
            if (!seenMessage) seenMessageFingerprints.add(fingerprint)
            while (seenMessageFingerprints.size > 300) seenMessageFingerprints.remove(seenMessageFingerprints.first())
            (seenMessage || (seenForKey && !hasNewMessageContent)) to hasNewMessageContent
        }
        if (alertDecision) {
            IslandBus.updateWhatsAppInBackground(activity)
            IslandDiagnostics.record("WA_SILENT_REFRESH", "key=${sbn.key.takeLast(32)} unchanged-content")
            return
        }
        if (hasNewMessageContent) {
            IslandDiagnostics.record("WA_REALERT", "key=${sbn.key.takeLast(32)} new-message-content")
        }
        IslandBus.pushActivity(activity)
        IslandDiagnostics.record("WA_ALERT", "key=${sbn.key.takeLast(32)}")
        IslandDiagnostics.recordResourceSnapshot("WA_ALERT")
        // Decode/scale the contact photo away from the main thread. The message banner is
        // published immediately with its fallback icon, then upgraded only if the same
        // notification identity is still represented in the queue.
        scope.launch(Dispatchers.IO) {
            val avatar = extractAvatar(notification)
            if (avatar != null && !avatar.isRecycled) {
                withContext(Dispatchers.Main.immediate) {
                    IslandBus.updateWhatsAppInBackground(activity.copy(avatar = avatar))
                }
            }
        }
        vibrateOnce()

        // Keep unanswered chats in the island queue. They leave only after an
        // explicit reply/dismiss action or when Android reports notification removal.
    }


    /**
     * Only reads the active player's own media notification (nothing is cancelled or altered). Its action
     * buttons carry the heart state, and players repost it the moment that state changes, so the island
     * learns about a like/unlike done inside the player app without any polling.
     */
    private fun watchMediaNotification(sbn: StatusBarNotification) {
        val c = activeController ?: return
        if (sbn.packageName != c.packageName) return
        val n = sbn.notification ?: return
        if (n.extras?.containsKey("android.mediaSession") != true) return
        val liked = Controls.likeFromNotification(n.actions, sbn.packageName)
        if (mediaNotifPackage == sbn.packageName && mediaNotifLiked == liked) return
        mediaNotifPackage = sbn.packageName
        mediaNotifLiked = liked
        IslandDiagnostics.record("LIKE_NOTIF", "pkg=${sbn.packageName} liked=$liked")
        if (liked != null && liked != lastPushed?.userLiked) updateMedia(c)
    }

    /** The player's notification may already be in the tray when its session appears; read it once. */
    private fun primeMediaNotification(controller: MediaController?) {
        controller ?: return
        try {
            activeNotifications?.firstOrNull {
                it.packageName == controller.packageName &&
                    it.notification?.extras?.containsKey("android.mediaSession") == true
            }?.let { watchMediaNotification(it) }
        } catch (_: Exception) {
        }
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

    /**
     * Cover art is almost always opaque. RGB_565 keeps the same look at half the memory of ARGB_8888, so an
     * opaque bitmap is converted. Any pixel with transparency keeps the original (a transparent PNG would
     * turn black otherwise). The scan runs on the bounded (about 320 px) bitmap only.
     */
    private fun opaqueToRgb565(src: Bitmap): Bitmap {
        if (src.isRecycled || src.config != Bitmap.Config.ARGB_8888) return src
        val px = IntArray(src.width * src.height)
        src.getPixels(px, 0, src.width, 0, 0, src.width, src.height)
        for (c in px) if ((c ushr 24) != 0xFF) return src
        // Never recycle here: when shrinkArt returned the caller's own bitmap, it still belongs to the media app.
        return src.copy(Bitmap.Config.RGB_565, false) ?: src
    }

    /** Keep notification artwork close to its on-screen pixel size to avoid retaining oversized textures. */
    private fun shrinkArt(src: Bitmap): Bitmap {
        if (src.isRecycled) return src
        val limit = com.android.island.core.DeviceProfile.sourceArtMaxPx
        val maxSide = maxOf(src.width, src.height)
        val needsCopy = src.config == Bitmap.Config.HARDWARE
        if (maxSide <= limit && !needsCopy) return src

        // Hardware bitmaps cannot be reconfigured. Avoid src.copy(): it allocates a full-size
        // ARGB_8888 bitmap only to shrink it immediately, creating a large native-heap spike.
        // Draw the hardware source directly into the final bounded destination instead.
        val scale = minOf(1f, limit.toFloat() / maxSide.toFloat())
        val width = (src.width * scale).toInt().coerceAtLeast(1)
        val height = (src.height * scale).toInt().coerceAtLeast(1)

        if (needsCopy) {
            val bounded = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
            android.graphics.Canvas(bounded).drawBitmap(
                src,
                android.graphics.Rect(0, 0, src.width, src.height),
                android.graphics.Rect(0, 0, width, height),
                paint
            )
            return bounded
        }
        if (maxSide <= limit) return src
        return Bitmap.createScaledBitmap(src, width, height, true)
    }

    /**
     * Stable fingerprint for a WhatsApp notification update. MessagingStyle exposes the latest
     * message timestamps in android.messages, allowing identical consecutive messages to be
     * distinguished without treating avatar/count-only refreshes as new arrivals. If WhatsApp
     * omits that payload, use the notification's post time as a conservative fallback.
     */
    private fun messageFingerprint(sbn: StatusBarNotification, title: String, text: String): String {
        val extras = sbn.notification?.extras
        val messages = extras?.getParcelableArray(Notification.EXTRA_MESSAGES)
            ?.mapNotNull { it as? Bundle }
            ?.takeLast(8)
            .orEmpty()
        if (messages.isNotEmpty()) {
            // MessagingStyle normally contains the conversation history. Fingerprint only its
            // newest incoming item: otherwise a reply/history refresh can look like a new message.
            val latest = messages.last()
            val sender = latest.getCharSequence("sender")?.toString().orEmpty()
            val body = latest.getCharSequence("text")?.toString().orEmpty()
            val timestamp = latest.getLong("time", 0L)
            return "$title|$timestamp|$sender|$body"
        }
        // postTime changes on metadata-only reposts, so it must not be part of the fallback key.
        return "$title|${text.trim()}"
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
        if (sbn.packageName == mediaNotifPackage && sbn.notification?.extras?.containsKey("android.mediaSession") == true) {
            mediaNotifLiked = null
        }
        if (synchronized(activeCallKeys) { activeCallKeys.remove(sbn.key) }) {
            IslandBus.clearCall(sbn.key)
            return
        }
        if (sbn.packageName !in whatsAppPackages) return

        // WhatsApp removes/reposts its source notification after inline reply and read actions.
        // The Island owns a separate in-memory banner model: source-notification lifecycle must
        // not delete that model or collapse an expanded stack. The user dismisses a card from
        // the Island itself; retaining the fingerprint also prevents reposts from replaying it.
        IslandDiagnostics.record("WA_SOURCE_REMOVED", "key=${sbn.key.takeLast(32)} kept-in-island")
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
        Controls.bindSession(this, activeController)
        if (activeController?.packageName != mediaNotifPackage) {
            mediaNotifLiked = null
            mediaNotifPackage = null
            primeMediaNotification(activeController)
        }
        activeController?.registerCallback(controllerCallback)
        updateMedia(activeController)
    }

    /**
     * Switch the retained media cover to the active renderer's memory budget. Low-End keeps only
     * one tiny source bitmap; Premium asks the active session for its normal bounded cover again.
     * The old bitmap is not forcibly recycled because a just-composed frame may still reference it.
     */
    private fun applyLowEndMode(enabled: Boolean) {
        if (appliedLowEndMode == enabled) return
        appliedLowEndMode = enabled
        // Drop the service's retained reference; never recycle a bitmap that a composed frame
        // may still be drawing. updateMedia() re-requests the active track and the existing
        // Dispatchers.Default pipeline rebuilds it at the newly selected bound.
        cachedArt = null
        cachedArtKey = null
        lastPushed = null
        IslandDiagnostics.recordResourceSnapshot(if (enabled) "LOW_END_ON" else "LOW_END_OFF")
        activeController?.let(::updateMedia)
    }

    private fun updateMedia(controller: MediaController?) {
        if (controller == null) {
            mediaUpdateJob?.cancel()
            pendingTrackKey = null
            currentTrackKey = null
            cachedArt = null; cachedArtKey = null; lastPushed = null
            repeatHandler.removeCallbacks(repeatRunnable)
            Controls.bindSession(this, null)
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
        // Cover art is copied and scaled here, which is too heavy for the main thread: a full-size hardware
        // copy plus a pixel scan stalled the island on every track change. The track is published now
        // without art, and the art is processed in the background and published once it is ready.
        currentTrackKey = trackKey
        val cachedHit = cachedArtKey == trackKey && cachedArt != null
        val rawArt: Bitmap? = if (cachedHit) null
            else metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val artwork: Bitmap? = if (cachedHit) cachedArt else null
        if (!cachedHit && rawArt != null) {
            val lowEndAtDecodeStart = com.android.island.core.DeviceProfile.staticGlass
            scope.launch(Dispatchers.Default) {
                // Track ownership explicitly: metadata's raw bitmap belongs to the media provider;
                // only intermediate/final copies created here may be recycled by this service.
                val bounded = runCatching { shrinkArt(rawArt) }.getOrNull() ?: return@launch
                val fetched = runCatching { opaqueToRgb565(bounded) }.getOrNull()
                if (fetched == null) {
                    if (bounded !== rawArt && !bounded.isRecycled) bounded.recycle()
                    return@launch
                }
                if (bounded !== rawArt && fetched !== bounded && !bounded.isRecycled) bounded.recycle()
                // Premium may prewarm its cached glass backdrop. Low-End draws the bounded source
                // directly and deliberately creates no second bitmap/cache entry.
                if (!com.android.island.core.DeviceProfile.staticGlass) {
                    runCatching { com.android.island.island.prewarmArtBackdrop(fetched) }
                }
                withContext(Dispatchers.Main) {
                    // Publish only if the same track is still showing and the controller has not changed.
                    if (currentTrackKey != trackKey || activeController == null ||
                        lowEndAtDecodeStart != com.android.island.core.DeviceProfile.staticGlass) {
                        if (fetched !== rawArt && !fetched.isRecycled) fetched.recycle()
                        // A mode switch while decoding invalidates this result. Re-read metadata
                        // under the new source-art bound rather than publishing an oversized image.
                        if (currentTrackKey == trackKey) activeController?.let(::updateMedia)
                        return@withContext
                    }
                    cachedArtKey = trackKey
                    cachedArt = fetched
                    updateMedia(activeController)
                }
            }
        }
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING
        val position = state?.position ?: 0L
        // The player's own like state, read fresh: rating, custom actions, then its media notification.
        val userLiked: Boolean? = currentLiked(controller, metadata)
        armRepeatGuard(controller, duration)

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
        if (src.config != Bitmap.Config.HARDWARE) {
            if (longest <= max) return src
            val k = max.toFloat() / longest
            return Bitmap.createScaledBitmap(src, (src.width * k).toInt().coerceAtLeast(1), (src.height * k).toInt().coerceAtLeast(1), true)
        }
        // Was previously `return src` for any hardware bitmap, skipping the cap entirely. A
        // notification large icon delivered as a hardware bitmap then also skipped RGB_565 in
        // opaqueToRgb565() below (which only runs on ARGB_8888 input), so the avatar silently kept
        // its full original resolution as a GPU-resident bitmap for as long as the banner showed.
        // Same technique as shrinkArt(): draw the hardware source directly into a small
        // ARGB_8888 destination, since hardware bitmaps cannot be reconfigured or scaled via
        // createScaledBitmap.
        val k = minOf(1f, max.toFloat() / longest)
        val w = (src.width * k).toInt().coerceAtLeast(1)
        val h = (src.height * k).toInt().coerceAtLeast(1)
        val bounded = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
        android.graphics.Canvas(bounded).drawBitmap(
            src,
            android.graphics.Rect(0, 0, src.width, src.height),
            android.graphics.Rect(0, 0, w, h),
            paint
        )
        return bounded
    }

    /** Draws any Drawable (bitmap, adaptive icon, vector) into a small software bitmap. */
    private fun drawableToBitmap(d: android.graphics.drawable.Drawable, max: Int): Bitmap? {
        (d as? BitmapDrawable)?.bitmap?.let { return shrinkTo(it, max) }
        val w = d.intrinsicWidth.takeIf { it > 0 } ?: max
        val h = d.intrinsicHeight.takeIf { it > 0 } ?: max
        val k = minOf(1f, max.toFloat() / maxOf(w, h))
        val bw = (w * k).toInt().coerceAtLeast(1)
        val bh = (h * k).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        d.setBounds(0, 0, bw, bh)
        d.draw(canvas)
        return bmp
    }

    private fun extractAvatar(notification: Notification): Bitmap? {
        return try {
            // Contact photos/avatars are almost always opaque, same as cover art; opaqueToRgb565
            // keeps the original if any pixel has transparency, so nothing can turn black.
            notification.getLargeIcon()?.loadDrawable(this)?.let { d -> drawableToBitmap(d, 96) }?.let { opaqueToRgb565(it) }
        } catch (_: Throwable) {
            null
        }
    }

    override fun onListenerDisconnected() {
        // Forget this instance: a stale `live` made onScreenOn believe the listener was still connected,
        // so it never asked for a rebind and WhatsApp notifications stopped until force-stop.
        if (live === this) live = null
        // Android does not reconnect a notification listener on its own. Without this, media sessions
        // and WhatsApp notifications stop arriving until the app is reopened.
        try { requestRebind(android.content.ComponentName(this, WhatsAppNotificationListener::class.java)) } catch (_: Exception) {}
        stateJob?.cancel()
        repeatJob?.cancel()
        refreshHandler.removeCallbacks(refreshTick)
        refreshHandler.removeCallbacks(refreshBurst)
        repeatHandler.removeCallbacks(repeatRunnable)
        PhoneStateMonitor.unregister(this)
        // A listener disconnect must not leave a stale lyric/network request running.
        mediaUpdateJob?.cancel()
        mediaUpdateJob = null
        pendingTrackKey = null
        cachedArt = null; cachedArtKey = null; lastPushed = null
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
        Controls.bindSession(this, null)
        try { mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionListener) } catch (_: Exception) {}
        mediaSessionManager = null
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        if (live === this) live = null
        refreshHandler.removeCallbacks(refreshTick)
        refreshHandler.removeCallbacks(refreshBurst)
        repeatHandler.removeCallbacks(repeatRunnable)
        PhoneStateMonitor.unregister(this)
        mediaUpdateJob?.cancel()
        pendingTrackKey = null
        cachedArt = null; cachedArtKey = null; lastPushed = null
        activeController?.unregisterCallback(controllerCallback)
        try { mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionListener) } catch (_: Exception) {}
        activeController = null
        mediaSessionManager = null
        Controls.bindSession(this, null)
        scope.cancel()
        synchronized(alertedNotificationKeys) {
            alertedNotificationKeys.clear()
            lastProcessedText.clear()
        }
        super.onDestroy()
    }
}
