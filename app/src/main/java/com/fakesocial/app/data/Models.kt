package com.fakesocial.app.data

import java.util.Locale

enum class Kind { POST, CHAT, COMMENTS }

/** accent is an ARGB color stored as Long so this file stays free of Compose dependencies. */
enum class Platform(val label: String, val kind: Kind, val accent: Long, val badge: String) {
    FACEBOOK("Facebook", Kind.POST, 0xFF1877F2, "f"),
    INSTAGRAM("Instagram", Kind.POST, 0xFFE1306C, "Ig"),
    WHATSAPP("WhatsApp", Kind.CHAT, 0xFF25D366, "W"),
    TWITTER("X / Twitter", Kind.POST, 0xFF14171A, "X"),
    TIKTOK("TikTok", Kind.COMMENTS, 0xFFFE2C55, "Tt"),
    SNAPCHAT("Snapchat", Kind.CHAT, 0xFFF5C400, "S"),
    TELEGRAM("Telegram", Kind.CHAT, 0xFF229ED9, "Tg"),
    YOUTUBE("YouTube", Kind.COMMENTS, 0xFFE62117, "Yt"),
    LINKEDIN("LinkedIn", Kind.POST, 0xFF0A66C2, "in"),
    MESSENGER("Messenger", Kind.CHAT, 0xFF0084FF, "M");

    companion object {
        fun fromName(name: String?): Platform =
            entries.firstOrNull { it.name == name } ?: FACEBOOK
    }
}

/** One chat message, or one reply/comment (author is used for comments). */
data class ChatItem(
    val author: String,
    val text: String,
    val mine: Boolean,
    val time: String
)

data class MockData(
    val id: Long = 0,
    val platform: Platform,
    /** 0 = default layout. Instagram: 1 = direct message layout. */
    val variant: Int = 0,
    val name: String,
    val handle: String,
    val avatarPath: String? = null,
    val text: String,
    val time: String,
    val likes: Int,
    val comments: Int,
    val status: String = "",
    val items: List<ChatItem> = emptyList(),
    val dark: Boolean = false,
    val rtl: Boolean = false,
    val watermark: Boolean = false,
    val updatedAt: Long = 0
) {
    /** Which layout family is actually rendered. */
    val effectiveKind: Kind
        get() = if (platform == Platform.INSTAGRAM && variant == 1) Kind.CHAT else platform.kind

    companion object {
        fun default(platform: Platform): MockData {
            val base = MockData(
                platform = platform,
                name = "Alex Morgan",
                handle = "alex_morgan",
                text = "Had an amazing day exploring the city. Highly recommend this place!",
                time = "2h",
                likes = 1240,
                comments = 86
            )
            return when (platform.kind) {
                Kind.POST -> base.copy(
                    items = listOf(
                        ChatItem("Sam Lee", "Looks great! Take me next time.", false, "1h"),
                        ChatItem("Jordan Kim", "Love this 😍", false, "45m")
                    )
                )
                Kind.CHAT -> base.copy(
                    text = "",
                    status = "online",
                    time = "9:41",
                    items = listOf(
                        ChatItem("", "Hey! Are we still on for tonight?", false, "9:38"),
                        ChatItem("", "Yes, see you at 8", true, "9:39"),
                        ChatItem("", "Perfect, I'll book a table", false, "9:40"),
                        ChatItem("", "Thanks!", true, "9:41")
                    )
                )
                Kind.COMMENTS -> base.copy(
                    text = "This video is so helpful, thanks for sharing!",
                    time = "3d",
                    likes = 532,
                    items = listOf(
                        ChatItem("Sam Lee", "Totally agree.", false, "2d"),
                        ChatItem("Jordan Kim", "Watched it twice already.", false, "1d")
                    )
                )
            }
        }
    }
}

/** Splits profile fields into pure helpers so they can be unit tested. */
object Format {
    /** 1200 -> "1.2K", 1000 -> "1K", 1_500_000 -> "1.5M". */
    fun count(n: Int): String {
        if (n < 1000) return n.toString()
        val (value, suffix) =
            if (n < 999_950) (n / 1000.0 to "K") else (n / 1_000_000.0 to "M")
        return String.format(Locale.US, "%.1f", value).removeSuffix(".0") + suffix
    }

    /** Keeps digits only, caps at 9 digits so it always fits an Int. */
    fun parseCount(input: String): Int =
        input.filter { it.isDigit() }.take(9).toIntOrNull() ?: 0

    /** Largest power-of-two sample size so that max(w,h)/size <= maxSide. */
    fun sampleSize(width: Int, height: Int, maxSide: Int): Int {
        if (width <= 0 || height <= 0 || maxSide <= 0) return 1
        var size = 1
        while (maxOf(width, height) / (size * 2) >= maxSide) size *= 2
        return size
    }

    fun initial(name: String): String =
        name.trim().firstOrNull()?.uppercase() ?: "?"
}
