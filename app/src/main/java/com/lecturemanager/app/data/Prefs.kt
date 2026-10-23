package com.lecturemanager.app.data

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

data class SettingsState(
    val themeMode: Int = 0,            // 0 النظام، 1 فاتح، 2 داكن
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val autoBackup: Int = 0,           // 0 إيقاف، 1 يومي، 2 أسبوعي
    val lastBackup: Long = 0,
    val lockEnabled: Boolean = false,
    val biometric: Boolean = false,
    val pomodoroWork: Int = 25,
    val pomodoroBreak: Int = 5,
)

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun read() = SettingsState(
        themeMode = sp.getInt("theme", 0),
        reminderEnabled = sp.getBoolean("reminder", false),
        reminderHour = sp.getInt("rHour", 20), reminderMinute = sp.getInt("rMin", 0),
        autoBackup = sp.getInt("autoBackup", 0), lastBackup = sp.getLong("lastBackup", 0),
        lockEnabled = sp.getBoolean("lock", false) && sp.getString("pinHash", null) != null,
        biometric = sp.getBoolean("bio", false),
        pomodoroWork = sp.getInt("pWork", 25), pomodoroBreak = sp.getInt("pBreak", 5),
    )

    fun write(s: SettingsState) {
        sp.edit()
            .putInt("theme", s.themeMode).putBoolean("reminder", s.reminderEnabled)
            .putInt("rHour", s.reminderHour).putInt("rMin", s.reminderMinute)
            .putInt("autoBackup", s.autoBackup).putLong("lastBackup", s.lastBackup)
            .putBoolean("lock", s.lockEnabled).putBoolean("bio", s.biometric)
            .putInt("pWork", s.pomodoroWork).putInt("pBreak", s.pomodoroBreak)
            .apply()
    }

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        sp.edit().putString("pinSalt", salt).putString("pinHash", hash(salt, pin)).apply()
    }

    fun clearPin() { sp.edit().remove("pinSalt").remove("pinHash").apply() }

    fun checkPin(pin: String): Boolean {
        val salt = sp.getString("pinSalt", null) ?: return false
        return sp.getString("pinHash", null) == hash(salt, pin)
    }

    private fun hash(salt: String, pin: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray()).joinToString("") { "%02x".format(it) }
}
