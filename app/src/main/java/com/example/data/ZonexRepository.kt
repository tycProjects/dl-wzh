package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.AppConfig
import com.example.model.Category
import com.example.model.NotificationItem
import com.example.model.Sound
import com.example.model.UserProfile
import com.example.model.UserSound
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ZonexRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val tag = "ZonexRepository"

    /**
     * Realtime App Config & Maintenance settings listener
     */
    fun getAppConfigFlow(): Flow<AppConfig> = callbackFlow {
        val docRef = db.collection("settings").document("app_config")
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(tag, "Error listening to app config: ${error.message}")
                trySend(AppConfig())
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val config = AppConfig(
                    maintenance_mode = snapshot.getBoolean("maintenance_mode") ?: false,
                    maintenance_title = snapshot.getString("maintenance_title") ?: "রক্ষণাবেক্ষণ চলছে",
                    maintenance_message = snapshot.getString("maintenance_message") ?: "এখানে বর্তমানে কাজ চলছে। আপাতত ব্যবহার করা যাবে না। অনুগ্রহ করে পরে আবার আসুন।",
                    maintenance_image_url = snapshot.getString("maintenance_image_url") ?: "",
                    min_version = snapshot.getString("min_version") ?: "1.0",
                    latest_version = snapshot.getString("latest_version") ?: "1.0",
                    force_update = snapshot.getBoolean("force_update") ?: false,
                    update_title = snapshot.getString("update_title") ?: "Update Required",
                    update_description = snapshot.getString("update_description") ?: "Update to latest version for new gaming sounds",
                    update_url = snapshot.getString("update_url") ?: "https://play.google.com",
                    telegram_popup_enabled = snapshot.getBoolean("telegram_popup_enabled") ?: true,
                    telegram_message = snapshot.getString("telegram_message") ?: "নতুন নতুন আপডেট পেতে আমাদের Telegram-এ এখনই Join করুন।",
                    telegram_url = snapshot.getString("telegram_url") ?: "https://t.me/zonexvoice",
                    telegram_image_url = snapshot.getString("telegram_image_url") ?: "",
                    telegram_join_label = snapshot.getString("telegram_join_label") ?: "Join Us",
                    telegram_later_label = snapshot.getString("telegram_later_label") ?: "Later",
                    telegram_frequency = snapshot.getString("telegram_frequency") ?: "always",
                    user_uploads_enabled = snapshot.getBoolean("user_uploads_enabled") ?: true,
                    upload_size_limit_mb = snapshot.getDouble("upload_size_limit_mb") ?: 10.0,
                    ad_banner_enabled = snapshot.getBoolean("ad_banner_enabled") ?: true,
                    ad_interstitial_enabled = snapshot.getBoolean("ad_interstitial_enabled") ?: true,
                    ad_rewarded_enabled = snapshot.getBoolean("ad_rewarded_enabled") ?: true
                )
                trySend(config)
            } else {
                trySend(AppConfig())
            }
        }
        awaitClose { listener.remove() }
    }

    /**
     * Realtime dynamic categories listener
     */
    fun getCategoriesFlow(): Flow<List<Category>> = callbackFlow {
        val colRef = db.collection("categories")
        val listener = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(tag, "Error listening to categories: ${error.message}")
                trySend(getDefaultCategories())
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val isHidden = doc.getBoolean("is_hidden") ?: false
                    if (isHidden) null
                    else Category(
                        id = doc.id,
                        name = doc.getString("name") ?: "Category",
                        icon = doc.getString("icon") ?: "gamepad",
                        order = doc.getLong("order")?.toInt() ?: 0,
                        is_hidden = false,
                        created_at = doc.getLong("created_at") ?: System.currentTimeMillis()
                    )
                }.sortedBy { it.order }
                trySend(if (list.isEmpty()) getDefaultCategories() else list)
            } else {
                // If admin hasn't created any yet, provide starter gaming categories
                trySend(getDefaultCategories())
            }
        }
        awaitClose { listener.remove() }
    }

    /**
     * Realtime published sound library listener
     */
    fun getSoundsFlow(): Flow<List<Sound>> = callbackFlow {
        val colRef = db.collection("sounds")
        val listener = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(tag, "Error listening to sounds: ${error.message}")
                trySend(getDefaultSounds())
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val isPublished = doc.getBoolean("is_published") ?: true
                    if (!isPublished) null
                    else Sound(
                        id = doc.id,
                        title = doc.getString("title") ?: "Sound FX",
                        category_id = doc.getString("category_id") ?: "",
                        category_name = doc.getString("category_name") ?: "General",
                        audio_url = doc.getString("audio_url") ?: "",
                        duration_seconds = doc.getDouble("duration_seconds") ?: 2.5,
                        thumbnail_url = doc.getString("thumbnail_url") ?: "",
                        is_published = true,
                        download_count = doc.getLong("download_count") ?: 0,
                        play_count = doc.getLong("play_count") ?: 0,
                        requires_rewarded_ad = doc.getBoolean("requires_rewarded_ad") ?: false,
                        created_at = doc.getLong("created_at") ?: System.currentTimeMillis()
                    )
                }
                trySend(if (list.isEmpty()) getDefaultSounds() else list)
            } else {
                trySend(getDefaultSounds())
            }
        }
        awaitClose { listener.remove() }
    }

    /**
     * User's private sounds in My Sounds
     */
    fun getMySoundsFlow(userId: String): Flow<List<UserSound>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val colRef = db.collection("users").document(userId).collection("my_sounds")
        val listener = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(tag, "Error loading my sounds: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    UserSound(
                        id = doc.id,
                        user_id = userId,
                        title = doc.getString("title") ?: "My Audio",
                        audio_url = doc.getString("audio_url") ?: "",
                        duration_seconds = doc.getDouble("duration_seconds") ?: 2.0,
                        created_at = doc.getLong("created_at") ?: System.currentTimeMillis()
                    )
                }.sortedByDescending { it.created_at }
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    /**
     * User Profile & Favorites
     */
    fun getUserProfileFlow(userId: String): Flow<UserProfile?> = callbackFlow {
        if (userId.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val docRef = db.collection("users").document(userId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(null)
                return@addSnapshotListener
            }
            val favs = (snapshot.get("favorites") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val profile = UserProfile(
                uid = userId,
                email = snapshot.getString("email") ?: "",
                display_name = snapshot.getString("display_name") ?: "Zonex Gamer",
                photo_url = snapshot.getString("photo_url") ?: "",
                favorites = favs,
                last_active = snapshot.getLong("last_active") ?: System.currentTimeMillis()
            )
            trySend(profile)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Notifications Flow
     */
    fun getNotificationsFlow(): Flow<List<NotificationItem>> = callbackFlow {
        val colRef = db.collection("notifications")
        val listener = colRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(getDefaultNotifications())
                return@addSnapshotListener
            }
            val list = snapshot.documents.mapNotNull { doc ->
                NotificationItem(
                    id = doc.id,
                    title = doc.getString("title") ?: "Announcement",
                    message = doc.getString("message") ?: "",
                    type = doc.getString("type") ?: "announcement",
                    created_at = doc.getLong("created_at") ?: System.currentTimeMillis()
                )
            }.sortedByDescending { it.created_at }
            trySend(if (list.isEmpty()) getDefaultNotifications() else list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveUserSound(userId: String, title: String, audioUrl: String, duration: Double) {
        val colRef = db.collection("users").document(userId).collection("my_sounds")
        val data = hashMapOf(
            "user_id" to userId,
            "title" to title,
            "audio_url" to audioUrl,
            "duration_seconds" to duration,
            "created_at" to System.currentTimeMillis()
        )
        colRef.add(data).await()
    }

    suspend fun deleteUserSound(userId: String, soundId: String) {
        db.collection("users").document(userId).collection("my_sounds").document(soundId).delete().await()
    }

    suspend fun renameUserSound(userId: String, soundId: String, newTitle: String) {
        db.collection("users").document(userId).collection("my_sounds").document(soundId)
            .update("title", newTitle).await()
    }

    suspend fun toggleFavorite(userId: String, currentFavorites: List<String>, soundId: String) {
        val updated = if (currentFavorites.contains(soundId)) {
            currentFavorites - soundId
        } else {
            currentFavorites + soundId
        }
        db.collection("users").document(userId)
            .set(mapOf("favorites" to updated), SetOptions.merge()).await()
    }

    suspend fun ensureUserProfile(userId: String, email: String, displayName: String?, photoUrl: String?) {
        val data = hashMapOf(
            "uid" to userId,
            "email" to email,
            "display_name" to (displayName ?: "Zonex Gamer"),
            "photo_url" to (photoUrl ?: ""),
            "last_active" to System.currentTimeMillis()
        )
        db.collection("users").document(userId).set(data, SetOptions.merge()).await()
    }

    private fun getDefaultCategories(): List<Category> {
        return listOf(
            Category("cat_fps", "FPS Callouts", "target", 1),
            Category("cat_victory", "Victory & Hype", "trophy", 2),
            Category("cat_cyber", "Cyber & Synth", "radio", 3),
            Category("cat_troll", "Troll & Memes", "smile", 4),
            Category("cat_anime", "Anime Power FX", "zap", 5),
            Category("cat_weapons", "Weapon SFX", "shield", 6)
        )
    }

    private fun getDefaultSounds(): List<Sound> {
        return listOf(
            Sound("s_headshot", "Headshot Sniper", "cat_fps", "FPS Callouts", "synth://headshot", 1.2, "", true, 1420, 5800),
            Sound("s_rampage", "Unstoppable Rampage", "cat_victory", "Victory & Hype", "synth://rampage", 2.4, "", true, 2100, 8900),
            Sound("s_cyber_laser", "Neon Laser Beam", "cat_cyber", "Cyber & Synth", "synth://cyber_laser", 1.8, "", true, 980, 4200),
            Sound("s_airhorn", "MLG Airhorn Bass", "cat_troll", "Troll & Memes", "synth://airhorn", 2.0, "", true, 3400, 15000),
            Sound("s_kamehame", "Super Saiyan Charge", "cat_anime", "Anime Power FX", "synth://kamehame", 3.1, "", true, 1850, 7600),
            Sound("s_tactical_nuke", "Tactical Nuke Incoming", "cat_fps", "FPS Callouts", "synth://tactical_nuke", 2.8, "", true, 2900, 11200),
            Sound("s_victory_royale", "Victory Royale Fanfare", "cat_victory", "Victory & Hype", "synth://victory", 3.5, "", true, 4100, 16800),
            Sound("s_shield_break", "Armor Shield Shatter", "cat_weapons", "Weapon SFX", "synth://shield_break", 1.5, "", true, 1250, 5300),
            Sound("s_rage_quit", "Keyboard Smash Troll", "cat_troll", "Troll & Memes", "synth://rage_quit", 2.2, "", true, 2700, 9400),
            Sound("s_level_up", "Retro 8-Bit Level Up", "cat_cyber", "Cyber & Synth", "synth://level_up", 1.6, "", true, 1920, 6800)
        )
    }

    private fun getDefaultNotifications(): List<NotificationItem> {
        return listOf(
            NotificationItem(
                id = "notif_welcome",
                title = "Welcome to ZONEX VOICE!",
                message = "Explore gaming soundboards, multi-channel mixer, and floating soundboard overlay for your game sessions.",
                type = "announcement",
                created_at = System.currentTimeMillis() - 3600000
            ),
            NotificationItem(
                id = "notif_telegram",
                title = "Official Telegram Channel Live",
                message = "Join our community on Telegram for daily gaming sound packs and new version updates.",
                type = "sound",
                created_at = System.currentTimeMillis() - 7200000
            )
        )
    }
}
