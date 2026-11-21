package com.android.island.core

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Central reactive bus for island activities.
 * Priority: WhatsApp (20) > Media (10)
 * State preservation: Media state kept when WhatsApp arrives.
 *
 * Expanded media stays open until the user dismisses it (outside tap / swipe).
 * Media stop / track change / resume must NOT auto-collapse expanded.
 */
object IslandBus {
    /** Unread chats kept in the queue. Each one holds its avatar in memory, so the queue must not grow without limit. */
    private const val MAX_WHATSAPP_QUEUE = 20
    /** Cards older than this leave the queue so a forgotten notification never pins its avatar in memory. */
    private const val MAX_ITEM_AGE_MS = 12L * 60L * 60L * 1000L


    private val _state = MutableStateFlow(IslandUiState())
    val state: StateFlow<IslandUiState> = _state.asStateFlow()

    private val activityQueue = ConcurrentLinkedQueue<IslandActivity>()
    private var mediaSnapshot: IslandActivity.Media? = null
    /** AI remains in memory while a WhatsApp banner briefly takes visual priority. */

    private val replyOpenFlow = MutableStateFlow(false)
    /** Observable twin of [replyOpen]: collectors wake on the change instead of polling it. */
    val replyOpenState: StateFlow<Boolean> = replyOpenFlow.asStateFlow()
    var replyOpen: Boolean
        get() = replyOpenFlow.value
        set(value) { replyOpenFlow.value = value }
    private val _replyCloseRequests = MutableStateFlow(0L)
    val replyCloseRequests: StateFlow<Long> = _replyCloseRequests.asStateFlow()

    /** Request the active quick-reply editor to close after an outside tap. */
    fun requestCloseReply() {
        _replyCloseRequests.value = _replyCloseRequests.value + 1L
    }

    /**
     * True while the AI chat input has focus or the system file picker is over the island.
     * Guards the outside-tap collapse (the keyboard and picker are windows outside the
     * island) and lets the AI panel shrink its chat area while the keyboard is up.
     */
    private val aiTypingFlow = MutableStateFlow(false)
    val aiTyping: StateFlow<Boolean> = aiTypingFlow.asStateFlow()
    /** Only meaningful while the AI card is the current activity; a stale flag must never block outside-tap collapse. */
    val aiTypingActive: Boolean get() = aiTypingFlow.value && _state.value.current is IslandActivity.Ai
    fun setAiTyping(active: Boolean) { aiTypingFlow.value = active }

    /**
     * Counts AI windows opened on top of the overlay (composer, file picker). While any is open the
     * panel must not clear the typing flag, otherwise the next tap outside collapses it.
     */
    @Volatile private var externalAiCount = 0
    val externalAiActive: Boolean get() = externalAiCount > 0
    @Synchronized fun beginExternalAi() { externalAiCount++; aiTypingFlow.value = true }
    @Synchronized fun endExternalAi() { externalAiCount = (externalAiCount - 1).coerceAtLeast(0) }

    /**
     * Measured height (dp) of the expanded AI chat card; 0 until it has laid out.
     * Reported by the panel after layout.
     */
    fun setAiPanelHeight(dp: Int) {
        _state.update { if (it.aiPanelHeightDp == dp) it else it.copy(aiPanelHeightDp = dp) }
    }

    /**
     * Measured height (dp) of the WhatsApp/notification banner as it is actually drawn. The touch
     * window uses max(setting, measured) so a taller card (two-line text, "lihat seluruh pesan")
     * is never left without a touchable area.
     */
    fun setBannerMeasuredHeight(dp: Int) {
        _state.update { if (it.bannerMeasuredDp == dp) it else it.copy(bannerMeasuredDp = dp) }
    }

    fun init(context: Context) {
        // Ready for service binding
    }

    /**
     * The AI card only owns the foreground while it is actually on screen. A HIDDEN island that
     * still carries an Ai activity is stale state; treating it as foreground used to block every
     * later media capsule until the app was restarted.
     */
    private fun IslandUiState.aiForeground(): Boolean =
        current is IslandActivity.Ai && islandState != IslandState.HIDDEN

    @Synchronized
    fun pushActivity(activity: IslandActivity) {
        when (activity) {
            is IslandActivity.Media -> {
                mediaSnapshot = activity
                val cur = _state.value
                when {
                    // AI chat is the user's foreground: keep it open, media waits behind it
                    // and is restored by closeAi() (the latest metadata, never null).
                    cur.aiForeground() -> {
                        _state.update { it.copy(pendingMedia = activity) }
                    }
                    // WhatsApp banner on top — keep media pending
                    cur.current is IslandActivity.WhatsApp -> {
                        _state.update { it.copy(pendingMedia = activity) }
                    }
                    // Already expanded media — refresh content only, stay expanded
                    cur.islandState == IslandState.EXPANDED && cur.current is IslandActivity.Media -> {
                        _state.update { it.copy(current = activity, pendingMedia = null) }
                    }
                    // Expanded something else → switch to this media expanded
                    cur.islandState == IslandState.EXPANDED -> {
                        _state.update { it.copy(current = activity, pendingMedia = null) }
                    }
                    // Default: show compact immediately
                    else -> {
                        _state.update {
                            it.copy(
                                current = activity,
                                islandState = IslandState.COMPACT,
                                pendingMedia = null
                            )
                        }
                    }
                }
            }
            // AI is opened explicitly through pushAi(); it is not a notification event.
            is IslandActivity.Ai -> Unit
            is IslandActivity.Call -> pushCall(activity)
            is IslandActivity.WhatsApp -> {
                // A chat message must never stop the music; only calls do (see pushCall).
                val items = activityQueue.filterIsInstance<IslandActivity.WhatsApp>().toMutableList()
                // The same message from the same sender is shown only once. A different message,
                // or the same text from another sender, is allowed.
                val duplicate = items.any {
                    it.id != activity.id && it.packageName == activity.packageName &&
                        it.senderName == activity.senderName &&
                        it.message == activity.message && it.groupName == activity.groupName
                }
                if (duplicate) return
                val existingIndex = items.indexOfFirst { it.id == activity.id }
                if (existingIndex >= 0) items[existingIndex] = activity else items.add(activity)
                val cutoff = System.currentTimeMillis() - MAX_ITEM_AGE_MS
                items.removeAll { it.timestamp < cutoff && it.id != activity.id }
                // The list is oldest-first, so the front is dropped when the cap is exceeded.
                while (items.size > MAX_WHATSAPP_QUEUE) items.removeAt(0)
                activityQueue.clear()
                items.forEach { activityQueue.offer(it) }
                val queuedItems = items.toList()
        if (_state.value.current is IslandActivity.Call) return
                if (_state.value.aiForeground()) {
                    // The AI panel is the user's foreground and must never be closed by a notification:
                    // the new item is only queued (unread marker). It shows after the panel is closed.
                    _state.update { old -> old.copy(whatsAppItems = queuedItems) }
                    return
                }
                val showingOther = _state.value.islandState == IslandState.BANNER &&
                    (_state.value.current as? IslandActivity.WhatsApp)?.let { it.id != activity.id } == true
                if (showingOther) {
                    // One banner at a time: the newcomer waits its turn behind the visible banner.
                    if (activity.id !in bannerFifo) bannerFifo.addLast(activity.id)
                    _state.update { old -> old.copy(whatsAppItems = queuedItems) }
                    return
                }
                _state.update { old ->
                    // Newest message takes the banner; older messages stay in the stack.
                    // If some other card is EXPANDED, do NOT swap its content in one frame:
                    // morph down to the capsule first, then raise the chat banner (raiseBannerAfterMorph).
                    val active = activity
                    if (old.islandState == IslandState.EXPANDED && old.current !is IslandActivity.WhatsApp) {
                        old.copy(
                            current = active.copy(messageCount = queuedItems.count { it.packageName == activity.packageName }),
                            islandState = IslandState.COMPACT,
                            pendingMedia = mediaSnapshot,
                            whatsAppItems = queuedItems
                        )
                    } else old.copy(
                        current = active.copy(messageCount = queuedItems.count { it.packageName == activity.packageName }),
                        islandState = if (old.islandState == IslandState.EXPANDED &&
                            old.current is IslandActivity.WhatsApp) IslandState.EXPANDED else IslandState.BANNER,
                        pendingMedia = mediaSnapshot,
                        whatsAppItems = queuedItems
                    )
                }
                if (_state.value.islandState == IslandState.COMPACT && _state.value.current is IslandActivity.WhatsApp &&
                    (_state.value.current as IslandActivity.WhatsApp).id == activity.id) {
                    raiseBannerAfterMorph(activity.id)
                }
            }
        }
    }


    /** Refresh an already-alerted notification's metadata without changing the visible island state. */
    @Synchronized
    fun updateWhatsAppInBackground(activity: IslandActivity.WhatsApp) {
        val updated = activityQueue.map { item ->
            if (item is IslandActivity.WhatsApp && item.id == activity.id) {
                // Metadata-only refreshes may arrive before the async avatar decode completes.
                // Preserve the last valid foreground avatar instead of clearing it with null.
                activity.copy(avatar = activity.avatar ?: item.avatar)
            } else item
        }
        activityQueue.clear()
        updated.forEach { activityQueue.offer(it) }
        _state.update { old ->
            val current = old.current as? IslandActivity.WhatsApp
            old.copy(
                current = if (current?.id == activity.id) {
                    activity.copy(avatar = activity.avatar ?: current.avatar, messageCount = updated.size)
                } else old.current,
                whatsAppItems = old.whatsAppItems.map {
                    if (it.id == activity.id) activity.copy(avatar = activity.avatar ?: it.avatar) else it
                }
            )
        }
    }

    /** The call currently shown (banner or capsule). Null when no call is active. */
    private var activeCall: IslandActivity.Call? = null
    private val callHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val callBannerMs = 2500L

    /**
     * Delayed banner raise after an expanded card was interrupted. The morph-down runs first
     * (~0.55 s); raising the banner on the same frame made the expanded card vanish in one
     * frame (the "patah-patah" report) while the call/chat banner popped in on top of it.
     */
    private val bannerSwapHandler = android.os.Handler(android.os.Looper.getMainLooper())
    /**
     * Antrean FIFO banner, seperti Dynamic Island: hanya satu banner tampil; notifikasi yang datang
     * saat banner lain tampil menunggu giliran (urutan masuk), bukan menggantikan banner aktif.
     * Ketika banner menutup, notifikasi berikutnya naik setelah jeda BANNER_GAP_MS.
     */
    private val bannerFifo = ArrayDeque<String>()
    private val bannerNextHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private const val BANNER_GAP_MS = 110L
    private val BANNER_SWAP_DELAY_MS = 560L

    private fun raiseBannerAfterMorph(id: String) {
        bannerSwapHandler.removeCallbacksAndMessages(null)
        bannerSwapHandler.postDelayed({
            val st = _state.value
            val match = when (st.current) {
                is IslandActivity.Call -> (st.current as IslandActivity.Call).id == id
                is IslandActivity.WhatsApp -> (st.current as IslandActivity.WhatsApp).id == id
                else -> false
            }
            // Only complete the swap if nothing else (user tap, another call) moved the island meanwhile.
            if (match && st.islandState == IslandState.COMPACT) {
                _state.update { it.copy(islandState = IslandState.BANNER) }
            }
        }, BANNER_SWAP_DELAY_MS)
    }

    /** Music stops only when a phone call interrupts it. It is not resumed automatically. */
    private fun pauseMediaForInterruption() {
        if (mediaSnapshot?.isPlaying == true) {
            com.android.island.core.Controls.mediaController?.transportControls?.pause()
        }
    }

    /**
     * A call interrupts whatever is on screen: a short banner first, then it settles into the capsule
     * with the caller's photo and the elapsed time. Music pauses. WhatsApp messages keep queueing.
     */
    @Synchronized
    fun pushCall(call: IslandActivity.Call) {
        activeCall = call
        pauseMediaForInterruption()
        if (_state.value.current is IslandActivity.Ai) {
            // A call takes over the AI card: release its typing/picker flags so they cannot stay stuck.
            aiTypingFlow.value = false
            externalAiCount = 0
        }
        bannerSwapHandler.removeCallbacksAndMessages(null)
        if (_state.value.islandState == IslandState.EXPANDED) {
            // Smooth interrupt: morph the expanded card down to the capsule (which already shows
            // the caller), THEN raise the call banner. The old one-frame swap is gone.
            _state.update { it.copy(current = call, islandState = IslandState.COMPACT, pendingMedia = mediaSnapshot, aiPanelHeightDp = 0) }
            callHandler.removeCallbacksAndMessages(null)
            raiseBannerAfterMorph(call.id)
            callHandler.postDelayed({ settleCall(call.id) }, callBannerMs + BANNER_SWAP_DELAY_MS)
            return
        }
        _state.update { it.copy(current = call, islandState = IslandState.BANNER, pendingMedia = mediaSnapshot, aiPanelHeightDp = 0) }
        callHandler.removeCallbacksAndMessages(null)
        // Tapping the banner fires the call's own contentIntent (opens the app) rather than
        // expanding in-app, so nothing else moves the state off BANNER during this window;
        // settleCall re-checks activeCall/current by id, so a repost (e.g. ringing -> ongoing)
        // that calls pushCall again simply cancels and restarts this timer, same as the banner.
        callHandler.postDelayed({ settleCall(call.id) }, callBannerMs)
    }

    @Synchronized
    private fun settleCall(id: String) {
        if (activeCall?.id != id) return
        val cur = _state.value.current
        if (cur is IslandActivity.Call && cur.id == id) {
            _state.update { it.copy(islandState = IslandState.COMPACT) }
        }
    }

    /** Ends the call card and returns to the paused or playing media, or hides the island. */
    fun clearCall(id: String) {
        if (activeCall?.id == id) activeCall = null
        val cur = _state.value.current
        if (cur !is IslandActivity.Call || cur.id != id) return
        callHandler.removeCallbacksAndMessages(null)
        val items = activityQueue.filterIsInstance<IslandActivity.WhatsApp>().toList()
        val back = _state.value.pendingMedia ?: mediaSnapshot
        _state.update {
            when {
                // Unread WhatsApp messages are still waiting: show them in the capsule.
                items.isNotEmpty() -> it.copy(
                    current = items.first().copy(messageCount = items.size),
                    islandState = IslandState.COMPACT,
                    pendingMedia = mediaSnapshot,
                    whatsAppItems = items
                )
                back != null -> it.copy(current = back, islandState = IslandState.COMPACT, pendingMedia = null)
                else -> it.copy(current = null, islandState = IslandState.HIDDEN)
            }
        }
    }

    fun updateMedia(media: IslandActivity.Media) {
        mediaSnapshot = media
        val cur = _state.value
        when {
            cur.current is IslandActivity.Media -> {
                // Refresh content, keep the same islandState (COMPACT or EXPANDED). If the
                // overlay was hidden while this media is still playing, bring the capsule back.
                _state.update {
                    it.copy(
                        current = media,
                        islandState = if (it.islandState == IslandState.HIDDEN && media.isPlaying)
                            IslandState.COMPACT else it.islandState
                    )
                }
            }
            cur.aiForeground() -> {
                // The AI chat must not be kicked off screen by a media tick; keep the latest
                // metadata queued so closing the chat restores the current song.
                _state.update { it.copy(pendingMedia = media) }
            }
            cur.current is IslandActivity.Ai -> {
                // Stale hidden AI state: show the media instead of dropping it.
                _state.update {
                    it.copy(
                        current = media,
                        islandState = if (media.isPlaying) IslandState.COMPACT else IslandState.HIDDEN,
                        pendingMedia = if (media.isPlaying) null else media
                    )
                }
            }
            cur.islandState == IslandState.EXPANDED && cur.current is IslandActivity.WhatsApp -> {
                // Keep the WhatsApp stack as the expanded foreground. Media updates
                // remain queued behind it and must never replace the stack mid-view.
                _state.update { it.copy(pendingMedia = media) }
            }
            cur.islandState == IslandState.EXPANDED -> {
                // User is still looking at expanded media UI
                _state.update { it.copy(current = media, pendingMedia = null) }
            }
            else -> {
                _state.update { it.copy(pendingMedia = media) }
            }
        }
    }

    @Synchronized
    fun clearWhatsApp(id: String? = null, packageName: String? = null) {
        if (id != null) {
            activityQueue.removeIf { it is IslandActivity.WhatsApp && it.id == id }
        } else if (packageName != null) {
            // Opening an app clears only that app's cards; other apps keep their own stacks.
            activityQueue.removeIf { it is IslandActivity.WhatsApp && it.packageName == packageName }
        } else {
            activityQueue.clear()
        }
        if (_state.value.aiForeground()) {
            val remaining = activityQueue.filterIsInstance<IslandActivity.WhatsApp>().toList()
            _state.update { it.copy(whatsAppItems = remaining) }
            return
        }
        val items = activityQueue.filterIsInstance<IslandActivity.WhatsApp>().toList()
        val previous = _state.value.current as? IslandActivity.WhatsApp
        val next = previous?.let { p -> items.firstOrNull { it.id == p.id } } ?: items.firstOrNull()
        if (next != null) {
            _state.update {
                it.copy(
                    current = next.copy(messageCount = items.count { it.packageName == next.packageName }),
                    islandState = if (it.islandState == IslandState.EXPANDED) IslandState.EXPANDED else IslandState.BANNER,
                    pendingMedia = mediaSnapshot,
                    whatsAppItems = items
                )
            }
        } else {
            val ongoing = activeCall
            if (ongoing != null) {
                // WhatsApp is gone while a call is still going: back to the call in the capsule.
                _state.update {
                    it.copy(current = ongoing, islandState = IslandState.COMPACT, pendingMedia = null, whatsAppItems = emptyList())
                }
                return
            }
            val media = mediaSnapshot
            val keepExpandedMedia = _state.value.islandState == IslandState.EXPANDED &&
                _state.value.current is IslandActivity.Media
            _state.update {
                it.copy(
                    current = media,
                    islandState = when {
                        media == null -> IslandState.HIDDEN
                        keepExpandedMedia -> IslandState.EXPANDED
                        else -> IslandState.COMPACT
                    },
                    pendingMedia = null,
                    whatsAppItems = emptyList()
                )
            }
        }
    }

    /** Keep unread WhatsApp items queued, but tuck the toast back into the capsule. */
    @Synchronized
    fun collapseWhatsAppBanner() {
        val items = activityQueue.filterIsInstance<IslandActivity.WhatsApp>().toList()
        activeCall?.let { call ->
            // A call is still going: the WhatsApp banner gives way back to the call in the capsule.
            _state.update { it.copy(current = call, islandState = IslandState.COMPACT, pendingMedia = null, whatsAppItems = items) }
            return
        }
        val current = _state.value.current as? IslandActivity.WhatsApp
        val active = current?.let { c -> items.firstOrNull { it.id == c.id } } ?: items.firstOrNull()
        if (active != null) {
            _state.update {
                it.copy(
                    current = active.copy(messageCount = items.size.coerceAtLeast(1)),
                    islandState = IslandState.COMPACT,
                    pendingMedia = mediaSnapshot,
                    whatsAppItems = items
                )
            }
            showNextQueuedBanner()
        }
    }

    /** Naikkan notifikasi berikutnya dari antrean FIFO setelah jeda singkat; id yang sudah hilang dilewati. */
    private fun showNextQueuedBanner() {
        bannerNextHandler.removeCallbacksAndMessages(null)
        bannerNextHandler.postDelayed({
            val st = _state.value
            if (st.islandState != IslandState.COMPACT || st.current is IslandActivity.Call || st.aiForeground()) return@postDelayed
            while (bannerFifo.isNotEmpty()) {
                val id = bannerFifo.removeFirst()
                val next = st.whatsAppItems.firstOrNull { it.id == id } ?: continue
                _state.update { it.copy(current = next, islandState = IslandState.COMPACT) }
                raiseBannerAfterMorph(id)
                return@postDelayed
            }
        }, BANNER_GAP_MS)
    }

    @Synchronized
    fun clearMedia() {
        val cur = _state.value
        if (cur.islandState == IslandState.EXPANDED && cur.current is IslandActivity.Media) {
            // Keep expanded with last known media; snapshot cleared so next session is fresh
            mediaSnapshot = cur.current as? IslandActivity.Media
            return
        }
        mediaSnapshot = null
        if (cur.current is IslandActivity.Media) {
            val next = activityQueue.peek()
            _state.update {
                it.copy(
                    current = next,
                    islandState = if (next != null) IslandState.BANNER else IslandState.HIDDEN,
                    pendingMedia = null
                )
            }
        } else {
            _state.update { it.copy(pendingMedia = null) }
        }
    }

    @Synchronized
    fun expand() {
        // Calls never open the expanded popup; they stay as banner and capsule.
        // A chat/notification never opens an expanded panel (no stack): it is only a banner and the unread marker.
        if (_state.value.current is IslandActivity.Call || _state.value.current is IslandActivity.WhatsApp) return
        if (_state.value.current != null) {
            _state.update { it.copy(islandState = IslandState.EXPANDED) }
        }
    }

    /**
     * Summon the AI assistant: expands the island into the AI chat card.
     * Music keeps playing, queued WhatsApp chats stay queued, and a call always wins:
     * summoning during a call is ignored (the call keeps the banner).
     */
    @Synchronized
    fun pushAi() {
        if (_state.value.current is IslandActivity.Call) return
        _state.update {
            it.copy(
                current = IslandActivity.Ai(),
                islandState = IslandState.EXPANDED,
                pendingMedia = it.pendingMedia ?: mediaSnapshot
            )
        }
    }

    /**
     * Collapse from the AI card, restoring whatever was on screen before the summon:
     * the paused/playing media in the capsule, an unread WhatsApp banner, or hiding.
     */
    @Synchronized
    fun closeAi() {
        if (_state.value.current !is IslandActivity.Ai) return
        // Closing the panel must leave no AI state behind, or the capsule and banner stay hidden
        // and the window keeps the panel's size.
        aiTypingFlow.value = false
        externalAiCount = 0
        _state.update { it.copy(aiPanelHeightDp = 0) }
        val back = _state.value.pendingMedia ?: mediaSnapshot
        val items = activityQueue.filterIsInstance<IslandActivity.WhatsApp>().toList()
        when {
            // Unread chats were queued while the AI card was open: show the newest as the banner,
            // with media kept pending behind it. Media alone must not hide them.
            items.isNotEmpty() -> _state.update {
                it.copy(
                    current = items.last().copy(messageCount = items.size),
                    islandState = IslandState.BANNER,
                    pendingMedia = back,
                    whatsAppItems = items
                )
            }
            back != null -> _state.update {
                it.copy(current = back, islandState = IslandState.COMPACT, pendingMedia = null)
            }
            else -> _state.update {
                // Nothing to restore: drop the AI activity itself, not just hide the window.
                it.copy(current = null, islandState = IslandState.HIDDEN, pendingMedia = null)
            }
        }
    }

    @Synchronized
    fun collapse() {
        if (_state.value.current is IslandActivity.Call) return
        if (_state.value.current is IslandActivity.Ai) {
            closeAi()
            return
        }
        val current = _state.value.current
        val target = when (current) {
            is IslandActivity.WhatsApp -> IslandState.BANNER
            is IslandActivity.Call -> IslandState.COMPACT
            is IslandActivity.Media -> IslandState.COMPACT
            is IslandActivity.Ai -> IslandState.EXPANDED
            null -> IslandState.HIDDEN
        }
        _state.update { it.copy(islandState = target) }
    }

    @Synchronized
    fun hide() {
        _state.update {
            // Never leave a hidden Ai activity behind; it would block later capsules/banners.
            if (it.current is IslandActivity.Ai) it.copy(current = null, islandState = IslandState.HIDDEN, aiPanelHeightDp = 0)
            else it.copy(islandState = IslandState.HIDDEN)
        }
    }


    /** Force-dismiss media (swipe-right on expanded). Clears snapshot and hides. */

    /** User tapped compact media while WA banner is showing → expand media, keep WA in queue. */
    @Synchronized
    fun expandMediaFromBanner() {
        val media = mediaSnapshot ?: _state.value.pendingMedia ?: return
        _state.update {
            it.copy(
                current = media,
                pendingMedia = null,
                islandState = IslandState.EXPANDED
            )
        }
    }

    @Synchronized
    fun forceDismissMedia() {
        mediaSnapshot = null
        activityQueue.removeIf { it is IslandActivity.Media }
        if (_state.value.aiForeground()) {
            // Media was dismissed while the AI chat is open: keep the chat, only clear the pending restore.
            _state.update { it.copy(pendingMedia = null) }
            return
        }
        val next = activityQueue.peek()
        _state.update {
            it.copy(
                current = next,
                pendingMedia = null,
                islandState = when {
                    next != null -> IslandState.BANNER
                    else -> IslandState.HIDDEN
                }
            )
        }
    }

    fun setLayout(layout: IslandLayout) {
        _state.update { it.copy(layout = layout) }
    }

    fun setCutout(cutout: CutoutInfo) {
        _state.update { it.copy(cutout = cutout) }
    }

    fun setGlassConfig(config: GlassConfig) {
        _state.update { it.copy(glassConfig = config) }
    }
}

data class IslandUiState(
    val current: IslandActivity? = null,
    val pendingMedia: IslandActivity.Media? = null,
    val whatsAppItems: List<IslandActivity.WhatsApp> = emptyList(),
    val islandState: IslandState = IslandState.HIDDEN,
    val layout: IslandLayout = IslandLayout(),
    val cutout: CutoutInfo = CutoutInfo(),
    val glassConfig: GlassConfig = GlassConfig(),
    /** Measured height (dp) of the expanded AI chat card; 0 until it has laid out. */
    val aiPanelHeightDp: Int = 0,
    /** Measured height (dp) of the drawn banner; 0 until it has laid out. */
    val bannerMeasuredDp: Int = 0
) {
    /** Cards of the app that owns the current banner. Stacks never mix apps. */
    fun stackItems(): List<IslandActivity.WhatsApp> {
        val pkg = (current as? IslandActivity.WhatsApp)?.packageName ?: return whatsAppItems
        return whatsAppItems.filter { it.packageName == pkg }
    }
}
