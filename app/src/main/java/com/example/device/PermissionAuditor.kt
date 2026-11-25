package com.example.device

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

data class AuditedPermission(
    val id: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val statusLabel: String,
    val actionType: PermissionActionType
)

enum class PermissionActionType {
    RUNTIME_MIC,
    RUNTIME_NOTIFICATIONS,
    WRITE_SETTINGS_PANEL,
    MEDIA_PROJECTION_INFO,
    BATTERY_SETTINGS,
    ACCESSIBILITY_SETTINGS_INFO
}

class PermissionAuditor(private val context: Context) {

    fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun hasNotificationPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun canWriteSystemSettings(): Boolean = Settings.System.canWrite(context)

    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun auditAll(isScreenSharingActive: Boolean, isBackgroundServiceActive: Boolean): List<AuditedPermission> {
        val mic = hasMicrophonePermission()
        val notif = hasNotificationPermission()
        val writeSettings = canWriteSystemSettings()
        val batteryOk = isIgnoringBatteryOptimizations()

        return listOf(
            AuditedPermission(
                id = "mic",
                title = "মাইক্রোফোন (Microphone)",
                description = "বাংলা ভয়েস কমান্ড ও কথা শোনার জন্য প্রয়োজন। কখনোই গোপনে রেকর্ড করা হয় না।",
                isGranted = mic,
                statusLabel = if (mic) "অনুমোদিত" else "অনুমতি প্রয়োজন",
                actionType = PermissionActionType.RUNTIME_MIC
            ),
            AuditedPermission(
                id = "notifications",
                title = "নোটিফিকেশন (Notifications)",
                description = "ব্যাকগ্রাউন্ড অ্যাসিস্ট্যান্ট ও স্ক্রিন শেয়ার চলার সময় স্বচ্ছ নোটিফিকেশন দেখাতে।",
                isGranted = notif,
                statusLabel = if (notif) "অনুমোদিত" else "অনুমতি প্রয়োজন",
                actionType = PermissionActionType.RUNTIME_NOTIFICATIONS
            ),
            AuditedPermission(
                id = "brightness",
                title = "সিস্টেম ব্রাইটনেস (Write Settings)",
                description = "ফোনের স্ক্রিনের উজ্জ্বলতা সরাসরি বাড়াতে ও কমাতে প্রয়োজন।",
                isGranted = writeSettings,
                statusLabel = if (writeSettings) "অনুমোদিত" else "সেটিংস থেকে চালু করো",
                actionType = PermissionActionType.WRITE_SETTINGS_PANEL
            ),
            AuditedPermission(
                id = "screen_share",
                title = "স্ক্রিন শেয়ার (MediaProjection)",
                description = "প্রতিবার শুরু করার আগে Android সিস্টেম কনসেন্ট ডায়ালগ দেখায়।",
                isGranted = isScreenSharingActive,
                statusLabel = if (isScreenSharingActive) "সেশন চালু আছে" else "ব্যবহারকারীর সম্মতিতে চালু হয়",
                actionType = PermissionActionType.MEDIA_PROJECTION_INFO
            ),
            AuditedPermission(
                id = "background",
                title = "ব্যাকগ্রাউন্ড ও ব্যাটারি অপ্টিমাইজেশন",
                description = if (isBackgroundServiceActive) {
                    "ব্যাকগ্রাউন্ড সার্ভিস বর্তমানে চালু আছে।"
                } else {
                    "ব্যাকগ্রাউন্ডে স্থিতিশীল থাকতে ব্যাটারি অপ্টিমাইজেশন সেটিংস চেক করো।"
                },
                isGranted = batteryOk || isBackgroundServiceActive,
                statusLabel = if (batteryOk) "অবাধ (Unrestricted)" else "ব্যাটারি সেটিংস দেখো",
                actionType = PermissionActionType.BATTERY_SETTINGS
            ),
            AuditedPermission(
                id = "accessibility",
                title = "অ্যাক্সেসিবিলিটি (Creator Program-নিরাপদ মোড)",
                description = "Google Play Creator Program নীতিমালা অনুযায়ী মায়া গোপন ব্যাকগ্রাউন্ড স্ক্রিন সার্ভিস ছাড়াই নিরাপদ ইন-অ্যাপ কন্ট্রোল ও Android সেটিংস শর্টকাট ব্যবহার করে।",
                isGranted = true,
                statusLabel = "সুরক্ষিত (Compliant)",
                actionType = PermissionActionType.ACCESSIBILITY_SETTINGS_INFO
            )
        )
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openBatteryOptimizationSettings() {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
