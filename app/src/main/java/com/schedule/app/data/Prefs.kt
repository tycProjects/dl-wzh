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
    /** الجدول (الفصل) النشط */
    val activeSchedule: Long = 1,
    val morningSummary: Boolean = false,
    val morningHour: Int = 7,
    val eveningSummary: Boolean = false,
    val eveningHour: Int = 21,
    /** تذكير مساء بمحاضرات اليوم التي لم يُسجَّل حضورها */
    val forgotAttendance: Boolean = true,
    /** تنبيهات الامتحان قبله بـ7 و3 و1 أيام */
    val examAlerts: Boolean = true,
    /** دقائق التنقل بين القاعات للتحذير؛ 0 = متوقف */
    val travelMinutes: Int = 0,
    /** هدف المذاكرة الأسبوعي بالدقائق */
    val weeklyGoalMin: Int = 600,
    /** لون التمييز: فهرس في ACCENTS */
    val accent: Int = 0,
    val fontCairo: Boolean = true,
    /** وضع داكن تلقائي من 6 مساء إلى 6 صباحا */
    val autoDark: Boolean = false,
    /** مجلد نسخ احتياطي خارجي (قد يكون مجلد جوجل درايف عبر مزوّد المستندات) */
    val extTree: String = "",
    val pomoWork: Int = 25,
    val pomoBreak: Int = 5,
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
            activeSchedule = sp.getLong("activeSchedule", 1), morningSummary = sp.getBoolean("morning", false), morningHour = sp.getInt("morningHour", 7),
            eveningSummary = sp.getBoolean("evening", false), eveningHour = sp.getInt("eveningHour", 21), forgotAttendance = sp.getBoolean("forgot", true),
            examAlerts = sp.getBoolean("examAlerts", true), travelMinutes = sp.getInt("travel", 0), weeklyGoalMin = sp.getInt("goal", 600),
            accent = sp.getInt("accent", 0), fontCairo = sp.getBoolean("fontCairo", true), autoDark = sp.getBoolean("autoDark", false),
            extTree = sp.getString("extTree", "") ?: "", pomoWork = sp.getInt("pomoWork", 25), pomoBreak = sp.getInt("pomoBreak", 5),
        )
    }

    fun write(s: Settings) = sp.edit().putInt("lead", s.leadMinutes).putBoolean("start", s.notifyStart)
        .putBoolean("end", s.notifyEnd).putBoolean("ask", s.askRecord).putBoolean("noteRem", s.noteReminder)
        .putString("name", s.profileName).putString("photo", s.photoPath).putBoolean("silent", s.silentMode)
        .putBoolean("ongoing", s.ongoing).putInt("autoBackup", s.autoBackup).putLong("lastBackup", s.lastBackup)
        .putLong("semester", s.semesterStart).putInt("absLimit", s.absenceLimit).putBoolean("light", s.lightTheme)
        .putBoolean("lock", s.lockEnabled).putBoolean("bio", s.biometric)
        .putLong("activeSchedule", s.activeSchedule).putBoolean("morning", s.morningSummary).putInt("morningHour", s.morningHour)
        .putBoolean("evening", s.eveningSummary).putInt("eveningHour", s.eveningHour).putBoolean("forgot", s.forgotAttendance)
        .putBoolean("examAlerts", s.examAlerts).putInt("travel", s.travelMinutes).putInt("goal", s.weeklyGoalMin)
        .putInt("accent", s.accent).putBoolean("fontCairo", s.fontCairo).putBoolean("autoDark", s.autoDark)
        .putString("extTree", s.extTree).putInt("pomoWork", s.pomoWork).putInt("pomoBreak", s.pomoBreak).apply()

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
