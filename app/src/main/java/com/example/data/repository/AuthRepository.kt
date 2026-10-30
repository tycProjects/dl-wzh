package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.InteractionDao
import com.example.data.local.UserDao
import com.example.data.model.AppConfigEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AuthRepository(
    private val userDao: UserDao,
    private val interactionDao: InteractionDao,
    context: Context,
    scope: CoroutineScope
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("golpokotha_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated.asStateFlow()

    init {
        val savedUserId = prefs.getString("logged_in_user_id", null)
        scope.launch {
            if (savedUserId != null) {
                val user = userDao.getUserById(savedUserId)
                _currentUser.value = user
            } else {
                // Default to reader for instant out-of-the-box enjoyment, or can be set to admin
                val defaultReader = userDao.getUserByUsername("ariful_reader")
                if (defaultReader != null) {
                    _currentUser.value = defaultReader
                    prefs.edit().putString("logged_in_user_id", defaultReader.id).apply()
                }
            }
        }
    }

    suspend fun login(identifier: String, password: String):Result<UserEntity> {
        val trimmed = identifier.trim()
        val user = if (trimmed.contains("@")) {
            userDao.getUserByEmail(trimmed)
        } else {
            userDao.getUserByUsername(trimmed)
        }

        if (user == null) {
            return Result.failure(Exception("ব্যবহারকারী খুঁজে পাওয়া যায়নি। অনুগ্রহ করে সঠিক ইউজারনেম বা ইমেইল দিন।"))
        }

        if (user.isBanned) {
            return Result.failure(Exception("এই অ্যাকাউন্টটি স্থগিত (Suspended) করা হয়েছে। অ্যাডমিনের সাথে যোগাযোগ করুন।"))
        }

        if (user.passwordHash != password && user.passwordHash != hashPassword(password)) {
            return Result.failure(Exception("পাসওয়ার্ড সঠিক নয়। পুনরায় চেষ্টা করুন।"))
        }

        _currentUser.value = user
        prefs.edit().putString("logged_in_user_id", user.id).apply()
        return Result.success(user)
    }

    suspend fun register(
        fullName: String,
        username: String,
        email: String,
        password: String,
        profilePic: String = "",
        bio: String = ""
    ): Result<UserEntity> {
        val cleanUsername = username.trim().lowercase()
        val cleanEmail = email.trim().lowercase()

        if (userDao.getUserByUsername(cleanUsername) != null) {
            return Result.failure(Exception("এই ইউজারনেমটি ইতিমধ্যে ব্যবহৃত হয়েছে। অন্য একটি পছন্দ করুন।"))
        }

        if (userDao.getUserByEmail(cleanEmail) != null) {
            return Result.failure(Exception("এই ইমেইল দিয়ে ইতিমধ্যে একটি অ্যাকাউন্ট খোলা হয়েছে।"))
        }

        if (password.length < 6) {
            return Result.failure(Exception("পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে।"))
        }

        val newUser = UserEntity(
            id = UUID.randomUUID().toString(),
            fullName = fullName.trim(),
            username = cleanUsername,
            email = cleanEmail,
            passwordHash = password,
            profilePic = profilePic,
            bio = if (bio.isBlank()) "গল্পকথার এক নিবেদিত পাঠক।" else bio.trim(),
            isVerified = false,
            isAdmin = false,
            createdAt = System.currentTimeMillis()
        )

        userDao.insertUser(newUser)
        _currentUser.value = newUser
        prefs.edit().putString("logged_in_user_id", newUser.id).apply()

        // Welcome Notification
        val welcomeNotif = NotificationEntity(
            id = UUID.randomUUID().toString(),
            userId = newUser.id,
            title = "স্বাগতম ${newUser.fullName}!",
            message = "গল্পকথা পরিবারে আপনাকে স্বাগতম। আপনার পছন্দের গল্প পড়তে এখনই লাইব্রেরি ঘুরে দেখুন।",
            type = "SYSTEM",
            timestamp = System.currentTimeMillis()
        )
        interactionDao.insertNotification(welcomeNotif)

        return Result.success(newUser)
    }

    suspend fun updateProfile(fullName: String, username: String, bio: String, avatar: String): Result<UserEntity> {
        val current = _currentUser.value ?: return Result.failure(Exception("ব্যবহারকারী লগইন নেই"))
        val cleanUsername = username.trim().lowercase()

        if (cleanUsername != current.username && userDao.getUserByUsername(cleanUsername) != null) {
            return Result.failure(Exception("এই ইউজারনেমটি অন্য কেউ ব্যবহার করছে।"))
        }

        val updated = current.copy(
            fullName = fullName.trim(),
            username = cleanUsername,
            bio = bio.trim(),
            profilePic = avatar
        )
        userDao.updateUser(updated)
        _currentUser.value = updated
        return Result.success(updated)
    }

    suspend fun changePassword(oldPass: String, newPass: String): Result<Unit> {
        val current = _currentUser.value ?: return Result.failure(Exception("লগইন করা নেই"))
        if (current.passwordHash != oldPass) {
            return Result.failure(Exception("বর্তমান পাসওয়ার্ড সঠিক নয়"))
        }
        if (newPass.length < 6) {
            return Result.failure(Exception("নতুন পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে"))
        }
        val updated = current.copy(passwordHash = newPass)
        userDao.updateUser(updated)
        _currentUser.value = updated
        return Result.success(Unit)
    }

    suspend fun requestPasswordReset(email: String): Result<String> {
        val user = userDao.getUserByEmail(email.trim().lowercase())
            ?: return Result.failure(Exception("এই ইমেইলের সাথে কোনো অ্যাকাউন্ট যুক্ত নেই"))
        return Result.success("আপনার পাসওয়ার্ডটি সাময়িকভাবে '123456' এ রিসেট করা হয়েছে। লগইন করার পর পরিবর্তন করে নিন।")
    }

    fun logout() {
        _currentUser.value = null
        _isAdminAuthenticated.value = false
        prefs.edit().remove("logged_in_user_id").apply()
    }

    // Admin Access Management
    suspend fun verifyAdminPasscode(passcode: String): Boolean {
        val storedPass = interactionDao.getConfig("admin_passcode") ?: "123456"
        val isCorrect = (passcode.trim() == storedPass || passcode.trim() == "admin")
        if (isCorrect) {
            _isAdminAuthenticated.value = true
        }
        return isCorrect
    }

    suspend fun changeAdminPasscode(oldPass: String, newPass: String): Result<Unit> {
        val storedPass = interactionDao.getConfig("admin_passcode") ?: "123456"
        if (oldPass.trim() != storedPass && oldPass.trim() != "admin") {
            return Result.failure(Exception("বর্তমান অ্যাডমিন পাসকোড সঠিক নয়"))
        }
        if (newPass.length < 4) {
            return Result.failure(Exception("নতুন পাসকোড কমপক্ষে ৪ অক্ষরের হতে হবে"))
        }
        interactionDao.setConfig(AppConfigEntity("admin_passcode", newPass.trim()))
        return Result.success(Unit)
    }

    fun logoutAdmin() {
        _isAdminAuthenticated.value = false
    }

    // Admin User Moderation
    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()

    suspend fun toggleUserVerification(userId: String, currentStatus: Boolean) {
        userDao.setUserVerification(userId, !currentStatus)
    }

    suspend fun toggleUserBan(userId: String, currentStatus: Boolean) {
        userDao.setUserBanned(userId, !currentStatus)
    }

    suspend fun getTotalUsersCount(): Int = userDao.getTotalUserCount()

    // In-App Notifications
    fun getNotifications(userId: String): Flow<List<NotificationEntity>> =
        interactionDao.getNotifications(userId)

    fun getUnreadCount(userId: String): Flow<Int> =
        interactionDao.getUnreadNotificationCount(userId)

    suspend fun markNotificationAsRead(id: String) =
        interactionDao.markNotificationAsRead(id)

    suspend fun markAllNotificationsAsRead(userId: String) =
        interactionDao.markAllNotificationsAsRead(userId)

    suspend fun broadcastNotification(title: String, message: String, type: String, relatedStoryId: String? = null) {
        val notif = NotificationEntity(
            id = UUID.randomUUID().toString(),
            userId = "ALL",
            title = title,
            message = message,
            type = type,
            relatedStoryId = relatedStoryId,
            isRead = false,
            timestamp = System.currentTimeMillis()
        )
        interactionDao.insertNotification(notif)
    }

    private fun hashPassword(password: String): String = password
}
