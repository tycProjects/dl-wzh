package com.android.island.core

import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Commands the island sends to the system.
 * mediaController is set by WhatsAppNotificationListener when a session is active.
 */
object Controls {
    @Volatile
    var mediaController: MediaController? = null

    // ---- repeat one -------------------------------------------------------------------------
    // The framework MediaController has no public repeat API. Players built on MediaSessionCompat or
    // Media3 accept it through the compat controller, so that is tried first. If the player does not
    // confirm the mode, the island loops the track itself (see repeatGuard, used by the listener).
    private val _repeatOne = MutableStateFlow(false)
    /** Repeat-one as the user and the player currently have it. */
    val repeatOne: StateFlow<Boolean> = _repeatOne.asStateFlow()
    private val _repeatGuard = MutableStateFlow(false)
    /** True when repeat-one is on but the player did not confirm it, so the island restarts the track. */
    val repeatGuard: StateFlow<Boolean> = _repeatGuard.asStateFlow()
    @Volatile private var compat: MediaControllerCompat? = null
    @Volatile private var boundPackage: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val verifyRepeat = Runnable {
        val native = readRepeatOne()
        _repeatGuard.value = _repeatOne.value && native != true
    }

    /** Called by the listener when the active session changes. Repeat resets only for a different player. */
    fun bindSession(context: Context, controller: MediaController?) {
        mediaController = controller
        if (controller == null) {
            compat = null
            boundPackage = null
            mainHandler.removeCallbacks(verifyRepeat)
            _repeatOne.value = false
            _repeatGuard.value = false
            return
        }
        if (boundPackage == controller.packageName && compat != null) return
        boundPackage = controller.packageName
        _repeatOne.value = false
        _repeatGuard.value = false
        compat = runCatching {
            MediaControllerCompat(
                context.applicationContext,
                MediaSessionCompat.Token.fromToken(controller.sessionToken)
            )
        }.getOrNull()
    }

    /** The player's own repeat-one state, or null when the player does not report a repeat mode. */
    fun readRepeatOne(): Boolean? {
        val mode = runCatching { compat?.repeatMode }.getOrNull() ?: return null
        return when (mode) {
            PlaybackStateCompat.REPEAT_MODE_ONE -> true
            PlaybackStateCompat.REPEAT_MODE_NONE,
            PlaybackStateCompat.REPEAT_MODE_ALL,
            PlaybackStateCompat.REPEAT_MODE_GROUP -> false
            else -> null
        }
    }

    /** Adopts the player's repeat state when it reports one and the island is not looping by itself. */
    fun refreshRepeat() {
        if (_repeatGuard.value) return
        val native = readRepeatOne() ?: return
        if (native != _repeatOne.value) _repeatOne.value = native
    }

    fun setRepeatOne(on: Boolean) {
        _repeatOne.value = on
        mainHandler.removeCallbacks(verifyRepeat)
        if (!on) _repeatGuard.value = false
        runCatching {
            compat?.transportControls?.setRepeatMode(
                if (on) PlaybackStateCompat.REPEAT_MODE_ONE else PlaybackStateCompat.REPEAT_MODE_NONE
            )
        }
        // Players apply the mode a moment later; check what they report, then decide who loops.
        mainHandler.postDelayed(verifyRepeat, 450L)
    }

    // ---- like / heart -----------------------------------------------------------------------
    private val dislikeWords = listOf("dislike", "thumb down", "thumbs down", "tidak suka")
    private val removeWords = listOf("unlike", "un-like", "unfavorite", "unfavourite", "unsave", "remove", "hapus", "batal", "delete")
    private val addWords = listOf("add", "tambah", "save to", "simpan ke")
    private val stateWords = listOf("liked", "saved", "favorited", "disukai", "tersimpan")
    private val verbWords = listOf("like", "heart", "favorite", "favourite", "love", "save", "favorit", "suka", "simpan")

    /** Players seen to change an action's label when the like state changes (so a bare "Like" can be trusted). */
    private val togglingLabelPackages: MutableSet<String> = java.util.Collections.synchronizedSet(HashSet())

    private enum class LabelState { LIKED, NOT_LIKED, NOT_LIKED_BARE }

    private fun classify(label: CharSequence?): LabelState? {
        val t = label?.toString()?.lowercase()?.takeIf { it.isNotBlank() } ?: return null
        if (dislikeWords.any { it in t }) return null
        val about = verbWords.any { it in t } || stateWords.any { it in t }
        if (!about) return null
        // "Remove from Liked Songs" / "Unlike" is offered while the track IS liked.
        if (removeWords.any { it in t }) return LabelState.LIKED
        // "Add to Liked Songs" is offered while it is NOT liked.
        if (addWords.any { it in t }) return LabelState.NOT_LIKED
        if (stateWords.any { it in t }) return LabelState.LIKED
        return LabelState.NOT_LIKED_BARE
    }

    /**
     * Reads a like state out of an action label. Players show a different label for each state, so the
     * label is a real signal. A bare "Like" is only believed once this player has been seen to switch
     * labels; some apps keep one fixed label, and trusting it would show a wrong heart.
     * Null when the label says nothing about liking.
     */
    fun likeVerdict(label: CharSequence?, pkg: String?): Boolean? {
        val v = classify(label) ?: return null
        return when (v) {
            LabelState.LIKED -> { pkg?.let { togglingLabelPackages.add(it) }; true }
            LabelState.NOT_LIKED -> { pkg?.let { togglingLabelPackages.add(it) }; false }
            LabelState.NOT_LIKED_BARE -> if (pkg != null && pkg in togglingLabelPackages) false else null
        }
    }

    /** Verdict from a notification's action buttons (event driven: players repost it when the heart changes). */
    fun likeFromNotification(actions: Array<android.app.Notification.Action>?, pkg: String?): Boolean? {
        actions ?: return null
        for (a in actions) likeVerdict(a.title, pkg)?.let { return it }
        return null
    }

    /**
     * Current like state read fresh from the player, never from a cache. Order: the metadata rating
     * (explicit), then the player's custom actions. Null when neither reports anything.
     */
    fun readLiked(controller: MediaController, metadata: android.media.MediaMetadata?): Boolean? {
        metadata?.getRating(android.media.MediaMetadata.METADATA_KEY_USER_RATING)?.let { r ->
            when (r.ratingStyle) {
                android.media.Rating.RATING_HEART -> return r.hasHeart()
                android.media.Rating.RATING_THUMB_UP_DOWN -> return r.isThumbUp
                else -> {}
            }
        }
        controller.playbackState?.customActions?.forEach { a ->
            likeVerdict("${a.action} ${a.name}", controller.packageName)?.let { return it }
        }
        return null
    }

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
                    // A custom "like" action usually toggles, so only send it when the state really differs.
                    if (readLiked(c, c.metadata) == liked) return true
                    val action = c.playbackState?.customActions?.firstOrNull { a ->
                        classify("${a.action} ${a.name}") != null
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
