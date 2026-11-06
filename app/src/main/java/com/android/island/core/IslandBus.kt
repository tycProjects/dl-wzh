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

    private val _state = MutableStateFlow(IslandUiState())
    val state: StateFlow<IslandUiState> = _state.asStateFlow()

    private val activityQueue = ConcurrentLinkedQueue<IslandActivity>()
    private var mediaSnapshot: IslandActivity.Media? = null

    @Volatile var replyOpen: Boolean = false
    private val _replyCloseRequests = MutableStateFlow(0L)
    val replyCloseRequests: StateFlow<Long> = _replyCloseRequests.asStateFlow()

    /** Request the active quick-reply editor to close after an outside tap. */
    fun requestCloseReply() {
        _replyCloseRequests.value = _replyCloseRequests.value + 1L
    }

    fun init(context: Context) {
        // Ready for service binding
    }

    @Synchronized
    fun pushActivity(activity: IslandActivity) {
        when (activity) {
            is IslandActivity.Media -> {
                mediaSnapshot = activity
                val cur = _state.value
                when {
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
            is IslandActivity.Call -> pushCall(activity)
            is IslandActivity.WhatsApp -> {
                // FIFO notifications: keep the currently displayed contact stable.
                val items = activityQueue.filterIsInstance<IslandActivity.WhatsApp>().toMutableList()
                val existingIndex = items.indexOfFirst { it.id == activity.id }
                if (existingIndex >= 0) items[existingIndex] = activity else items.add(activity)
                activityQueue.clear()
                items.forEach { activityQueue.offer(it) }
                val queuedItems = items.toList()
                _state.update { old ->
                    val active = (old.current as? IslandActivity.WhatsApp)
                        ?.let { current -> queuedItems.firstOrNull { it.id == current.id } }
                        ?: queuedItems.first()
                    old.copy(
                        current = active.copy(messageCount = queuedItems.size),
                        islandState = if (old.islandState == IslandState.EXPANDED &&
                            old.current is IslandActivity.WhatsApp) IslandState.EXPANDED else IslandState.BANNER,
                        pendingMedia = mediaSnapshot,
                        whatsAppItems = queuedItems
                    )
                }
            }
        }
    }


    /** Refresh an already-alerted notification's metadata without changing the visible island state. */
    @Synchronized
    fun updateWhatsAppInBackground(activity: IslandActivity.WhatsApp) {
        val updated = activityQueue.map { item ->
            if (item is IslandActivity.WhatsApp && item.id == activity.id) activity else item
        }
        activityQueue.clear()
        updated.forEach { activityQueue.offer(it) }
        _state.update { old ->
            val current = old.current as? IslandActivity.WhatsApp
            old.copy(
                current = if (current?.id == activity.id) activity.copy(messageCount = updated.size) else old.current,
                whatsAppItems = old.whatsAppItems.map { if (it.id == activity.id) activity else it }
            )
        }
    }

    @Synchronized
    /** A call takes the foreground in expanded form until it ends. */
    fun pushCall(call: IslandActivity.Call) {
        _state.update { it.copy(current = call, islandState = IslandState.BANNER, pendingMedia = null) }
    }

    /** Ends the call card and returns to the paused or playing media, or hides the island. */
    fun clearCall(id: String) {
        val cur = _state.value.current
        if (cur !is IslandActivity.Call || cur.id != id) return
        val back = _state.value.pendingMedia ?: mediaSnapshot
        _state.update {
            if (back != null) it.copy(current = back, islandState = IslandState.COMPACT, pendingMedia = null)
            else it.copy(current = null, islandState = IslandState.HIDDEN)
        }
    }

    fun updateMedia(media: IslandActivity.Media) {
        mediaSnapshot = media
        val cur = _state.value
        when {
            cur.current is IslandActivity.Media -> {
                // Refresh content, keep the same islandState (COMPACT or EXPANDED)
                _state.update { it.copy(current = media) }
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
    fun clearWhatsApp(id: String? = null) {
        if (id != null) {
            activityQueue.removeIf { it is IslandActivity.WhatsApp && it.id == id }
        } else {
            activityQueue.clear()
        }
        val items = activityQueue.filterIsInstance<IslandActivity.WhatsApp>().toList()
        val previous = _state.value.current as? IslandActivity.WhatsApp
        val next = previous?.let { p -> items.firstOrNull { it.id == p.id } } ?: items.firstOrNull()
        if (next != null) {
            _state.update {
                it.copy(
                    current = next.copy(messageCount = items.size),
                    islandState = if (it.islandState == IslandState.EXPANDED) IslandState.EXPANDED else IslandState.BANNER,
                    pendingMedia = mediaSnapshot,
                    whatsAppItems = items
                )
            }
        } else {
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

    /** Mark a system-dismissed WhatsApp notification for a short visual exit before removal. */
    @Synchronized
    fun markWhatsAppSystemRemoving(id: String) {
        val updated = activityQueue.map { item ->
            if (item is IslandActivity.WhatsApp && item.id == id) item.copy(isSystemRemoving = true) else item
        }
        activityQueue.clear()
        updated.forEach { activityQueue.offer(it) }
        _state.update { old ->
            val current = old.current as? IslandActivity.WhatsApp
            old.copy(
                current = if (current?.id == id) current.copy(isSystemRemoving = true) else old.current,
                whatsAppItems = old.whatsAppItems.map { if (it.id == id) it.copy(isSystemRemoving = true) else it }
            )
        }
    }

    /** Keep unread WhatsApp items queued, but tuck the toast back into the capsule. */
    @Synchronized
    fun collapseWhatsAppBanner() {
        val items = activityQueue.filterIsInstance<IslandActivity.WhatsApp>().toList()
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
        }
    }

    /** Called by the expanded stack after layout so the panel and touch window match its content. */
    fun setWhatsAppStackHeight(dp: Int) {
        _state.update { if (it.whatsAppStackHeightDp == dp) it else it.copy(whatsAppStackHeightDp = dp) }
    }

    @Synchronized
    fun expandWhatsAppStack() {
        val items = activityQueue.filterIsInstance<IslandActivity.WhatsApp>().toList()
        if (items.isNotEmpty()) {
            _state.update {
                it.copy(
                    current = items.first(),
                    pendingMedia = mediaSnapshot,
                    whatsAppItems = items,
                    islandState = IslandState.EXPANDED
                )
            }
        }
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
        if (_state.value.current != null) {
            _state.update { it.copy(islandState = IslandState.EXPANDED) }
        }
    }

    @Synchronized
    fun collapse() {
        val current = _state.value.current
        val target = when (current) {
            is IslandActivity.WhatsApp -> IslandState.BANNER
            is IslandActivity.Call -> IslandState.COMPACT
            is IslandActivity.Media -> IslandState.COMPACT
            null -> IslandState.HIDDEN
        }
        _state.update { it.copy(islandState = target) }
    }

    @Synchronized
    fun hide() {
        _state.update { it.copy(islandState = IslandState.HIDDEN) }
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
    /** Measured height (dp) of the expanded WhatsApp stack; 0 until the stack has laid out. */
    val whatsAppStackHeightDp: Int = 0
)
