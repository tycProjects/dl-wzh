package com.android.island.core

import android.app.PendingIntent
import android.app.RemoteInput
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color

sealed class IslandActivity {
    abstract val id: String
    abstract val priority: Int
    abstract val timestamp: Long

    data class Media(
        override val id: String,
        override val priority: Int = 10,
        override val timestamp: Long = System.currentTimeMillis(),
        val title: String,
        val artist: String,
        val album: String? = null,
        val artwork: Bitmap? = null,
        val isPlaying: Boolean = false,
        val position: Long = 0L,
        val duration: Long = 0L,
        val packageName: String = "",
        val accentColor: Color? = null,
        val lyrics: List<LyricLine> = emptyList(),
        val currentLyricIndex: Int = -1,
        /** Like/favorite state reported by the player (heart or thumb). Null when the app reports none. */
        val userLiked: Boolean? = null
    ) : IslandActivity()

    data class WhatsApp(
        override val id: String,
        override val priority: Int = 20,
        override val timestamp: Long = System.currentTimeMillis(),
        val senderName: String,
        val message: String,
        val isGroup: Boolean = false,
        val groupName: String? = null,
        val avatar: Bitmap? = null,
        val messageCount: Int = 1,
        val conversationKey: String = "",
        val replyAction: PendingIntent? = null,
        val replyRemoteInputs: Array<RemoteInput>? = null,
        val markReadAction: PendingIntent? = null,
        val contentIntent: PendingIntent? = null,
        val isSystemRemoving: Boolean = false
    ) : IslandActivity()

    /** An incoming or active phone call. Actions are the notification's own PendingIntents. */
    data class Call(
        override val id: String,
        override val priority: Int = 100,
        override val timestamp: Long = System.currentTimeMillis(),
        val callerName: String,
        val subtitle: String = "",
        val isIncoming: Boolean = true,
        val avatar: Bitmap? = null,
        val answerAction: PendingIntent? = null,
        val declineAction: PendingIntent? = null,
        val contentIntent: PendingIntent? = null
    ) : IslandActivity()
}

data class LyricLine(
    val timeMs: Long,
    val text: String
)

enum class IslandState {
    HIDDEN,
    COMPACT,
    EXPANDED,
    BANNER
}

data class CutoutInfo(
    val type: CutoutType = CutoutType.NONE,
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val centerX: Float = 0.5f
)

enum class CutoutType {
    NONE, NOTCH, WATERDROP, PUNCH_HOLE, CENTER_CUTOUT
}

data class IslandLayout(
    val x: Int = 0,
    val y: Int = 0,
    val width: Int = 120,
    val height: Int = 36,
    val cornerRadius: Float = 18f
)

data class GlassConfig(
    val transparency: Float = 0.62f,
    val refraction: Float = 0.22f,
    val edgeHighlight: Float = 0.82f,
    val chromaticAberration: Float = 0.10f,
    val ambientReflection: Float = 0.72f,
    /** 0 = no white frost (pure see-through over album art), 1 = strong white veil */
    val whiteOpacity: Float = 0.45f,
    /** How strong the app-drawn frost layers are (buram), independent of white color */
    val frostStrength: Float = 0.62f
)


enum class TapAction(val label: String) {
    EXPAND("Buka"),
    OPEN_APP("Buka app"),
    PLAY_PAUSE("Putar/Jeda"),
    NEXT_TRACK("Berikutnya"),
    NOTHING("Tidak ada")
}

enum class ExpandStyle {
    SPRING, SMOOTH, INSTANT
}

enum class Elasticity(val damping: Float) {
    NONE(1f), LOW(0.8f), MEDIUM(0.62f), HIGH(0.48f)
}
