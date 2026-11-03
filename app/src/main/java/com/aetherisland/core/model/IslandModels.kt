package com.aetherisland.core.model

enum class IslandActivity { NONE, MEDIA, WHATSAPP }
enum class IslandPresentation { COMPACT, EXPANDED, NOTIFICATION_BANNER }

data class IslandState(
    val activity: IslandActivity = IslandActivity.NONE,
    val expanded: Boolean = false,
    val whatsappBannerVisible: Boolean = false,
    val mediaSnapshot: MediaSnapshot? = null
)

data class MediaSnapshot(
    val packageName: String,
    val title: String?,
    val artist: String?,
    val durationMs: Long?,
    val positionMs: Long,
    val isPlaying: Boolean,
    val artworkKey: String? = null
)

data class WhatsAppMessage(
    val notificationKey: String,
    val packageName: String,
    val sender: String?,
    val conversationTitle: String?,
    val messagePreview: String?,
    val isGroup: Boolean,
    val receivedAtMillis: Long
)
