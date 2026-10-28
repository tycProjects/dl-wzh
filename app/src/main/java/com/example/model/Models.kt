package com.example.model

data class AppConfig(
    val maintenance_mode: Boolean = false,
    val maintenance_title: String = "রক্ষণাবেক্ষণ চলছে",
    val maintenance_message: String = "এখানে বর্তমানে কাজ চলছে। আপাতত ব্যবহার করা যাবে না। অনুগ্রহ করে পরে আবার আসুন।",
    val maintenance_image_url: String = "",
    val min_version: String = "1.0",
    val latest_version: String = "1.0",
    val force_update: Boolean = false,
    val update_title: String = "Update Required",
    val update_description: String = "A critical update is available with new sounds and performance enhancements.",
    val update_url: String = "https://play.google.com",
    val telegram_popup_enabled: Boolean = true,
    val telegram_message: String = "নতুন নতুন আপডেট পেতে আমাদের Telegram-এ এখনই Join করুন।",
    val telegram_url: String = "https://t.me/zonexvoice",
    val telegram_image_url: String = "",
    val telegram_join_label: String = "Join Us",
    val telegram_later_label: String = "Later",
    val telegram_frequency: String = "always",
    val user_uploads_enabled: Boolean = true,
    val upload_size_limit_mb: Double = 10.0,
    val ad_banner_enabled: Boolean = true,
    val ad_interstitial_enabled: Boolean = true,
    val ad_rewarded_enabled: Boolean = true,
    val banner_ad_id: String = "ca-app-pub-3724977789874593/7012438315",
    val interstitial_ad_id: String = "ca-app-pub-3724977789874593/2831766134",
    val rewarded_ad_id: String = "ca-app-pub-3724977789874593/2887781464"
)

data class Category(
    val id: String = "",
    val name: String = "",
    val icon: String = "gamepad",
    val order: Int = 0,
    val is_hidden: Boolean = false,
    val created_at: Long = System.currentTimeMillis()
)

data class Sound(
    val id: String = "",
    val title: String = "",
    val category_id: String = "",
    val category_name: String = "",
    val audio_url: String = "",
    val duration_seconds: Double = 0.0,
    val thumbnail_url: String = "",
    val is_published: Boolean = true,
    val download_count: Long = 0,
    val play_count: Long = 0,
    val requires_rewarded_ad: Boolean = false,
    val created_at: Long = System.currentTimeMillis()
)

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val display_name: String = "Zonex Gamer",
    val photo_url: String = "",
    val favorites: List<String> = emptyList(),
    val last_active: Long = System.currentTimeMillis()
)

data class UserSound(
    val id: String = "",
    val user_id: String = "",
    val title: String = "",
    val audio_url: String = "",
    val duration_seconds: Double = 0.0,
    val created_at: Long = System.currentTimeMillis()
)

data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "announcement",
    val created_at: Long = System.currentTimeMillis()
)

data class MixerChannel(
    val id: Int,
    val name: String,
    val sound: Sound? = null,
    val volume: Float = 0.8f,
    val isMuted: Boolean = false,
    val isPlaying: Boolean = false
)
