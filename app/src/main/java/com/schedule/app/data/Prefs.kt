package com.schedule.app.data

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDate

data class Settings(
    /** عدد الدقائق قبل بداية المحاضرة لإشعار التنبيه المسبق؛ 0 = متوقف */
    val leadMinutes: Int = 10,
    val notifyStart: Boolean = true,
    val notifyEnd: Boolean = true,
    /** سؤال "هل تريد تسجيل المحاضرة؟" مع إشعار البداية */
    val askRecord: Boolean = true,
    /** تذكير قبل تاريخ الملاحظة والواجب بيوم */
    val noteReminder: Boolean = true,
    val profileName: String = "",
    val photoPath: String = "",
    /** تحويل الهاتف للاهتزاز أثناء المحاضرة */
    val silentMode: Boolean = false,
    /** إشعار دائم بالمحاضرة القادمة مع عدّ تنازلي */
    val ongoing: Boolean = false,
    /** 0 معطّل، 1 يومي، 2 أسبوعي */
    val autoBackup: Int = 0,
    val lastBackup: Long = 0,
    /** بداية الفصل (أسبوع 1) كيوم عصر يونكس؛ يبدأ تلقائيا بسبت الأسبوع الحالي */
    val semesterStart: Long = 0,
    /** حد الغياب المسموح لكل مادة */
    val absenceLimit: Int = 4,
    val lightTheme: Boolean = false,
    val lockEnabled: Boolean = false,
    val biometric: Boolean = false,
)

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun read(): Settings {
        var sem = sp.getLong("semester", 0)
        if (sem == 0L) { // أول تشغيل: أسبوع 1 = الأسبوع الحالي (يبدأ السبت)
            val today = LocalDate.now()
            sem = today.minusDays(((today.dayOfWeek.value + 1) % 7).toLong()).toEpochDay()
            sp.edit().putLong("semester", sem).apply()
        }
        return Settings(
            leadMinutes = sp.getInt("lead", 10), notifyStart = sp.getBoolean("start", true),
            notifyEnd = sp.getBoolean("end", true), askRecord = sp.getBoolean("ask", true),
            noteReminder = sp.getBoolean("noteRem", true),
            profileName = sp.getString("name", "") ?: "", photoPath = sp.getString("photo", "") ?: "",
            silentMode = sp.getBoolean("silent", false), ongoing = sp.getBoolean("ongoing", false),
            autoBackup = sp.getInt("autoBackup", 0), lastBackup = sp.getLong("lastBackup", 0), semesterStart = sem,
            absenceLimit = sp.getInt("absLimit", 4), lightTheme = sp.getBoolean("light", false),
            lockEnabled = sp.getBoolean("lock", false), biometric = sp.getBoolean("bio", false),
        )
    }

    fun write(s: Settings) = sp.edit().putInt("lead", s.leadMinutes).putBoolean("start", s.notifyStart)
        .putBoolean("end", s.notifyEnd).putBoolean("ask", s.askRecord).putBoolean("noteRem", s.noteReminder)
        .putString("name", s.profileName).putString("photo", s.photoPath).putBoolean("silent", s.silentMode)
        .putBoolean("ongoing", s.ongoing).putInt("autoBackup", s.autoBackup).putLong("lastBackup", s.lastBackup)
        .putLong("semester", s.semesterStart).putInt("absLimit", s.absenceLimit).putBoolean("light", s.lightTheme)
        .putBoolean("lock", s.lockEnabled).putBoolean("bio", s.biometric).apply()

    // ---- رمز القفل: SHA-256 مع salt عشوائي
    private fun hash(pin: String, salt: String) = MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray()).joinToString("") { "%02x".format(it) }
    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        sp.edit().putString("pinSalt", salt).putString("pinHash", hash(pin, salt)).apply()
    }
    fun clearPin() = sp.edit().remove("pinSalt").remove("pinHash").apply()
    fun checkPin(pin: String): Boolean {
        val salt = sp.getString("pinSalt", null) ?: return false
        return hash(pin, salt) == sp.getString("pinHash", null)
    }

    // ---- حفظ حالة وضع الصامت لاستعادتها بعد المحاضرة (-1 = لا شيء محفوظ)
    var savedRinger: Int
        get() = sp.getInt("savedRinger", -1)
        set(v) { sp.edit().putInt("savedRinger", v).apply() }
}
