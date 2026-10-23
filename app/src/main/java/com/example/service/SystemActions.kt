package com.example.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.example.model.LaunchableApp
import java.net.URLEncoder

data class PendingActionConfirm(
    val title: String,
    val description: String,
    val onConfirm: () -> Unit
)

class SystemActionHelper(private val context: Context) {

    fun getInstalledLaunchableApps(): List<LaunchableApp> {
        val list = mutableListOf<LaunchableApp>()
        try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            for (info in resolveInfos) {
                val pkgName = info.activityInfo.packageName
                // Skip our own app
                if (pkgName == context.packageName) continue
                val label = info.loadLabel(pm).toString()
                list.add(LaunchableApp(packageName = pkgName, label = label))
            }
        } catch (e: Exception) {
            Log.e("SystemActionHelper", "Error getting apps", e)
        }

        // Add built-in essential shortcuts
        val builtins = listOf(
            LaunchableApp("com.google.android.youtube", "YouTube", true),
            LaunchableApp("com.whatsapp", "WhatsApp", true),
            LaunchableApp("com.openai.chatgpt", "ChatGPT", true),
            LaunchableApp("com.android.chrome", "Google Chrome", true),
            LaunchableApp("android.settings", "System Settings", true),
            LaunchableApp("system.camera", "System Camera", true)
        )

        val merged = (builtins + list).distinctBy { it.packageName }
        return merged.sortedBy { it.label }
    }

    fun launchAppByPackage(packageName: String): Boolean {
        try {
            if (packageName == "android.settings") {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return true
            }

            if (packageName == "system.camera") {
                val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            }

            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return true
            }

            // Fallback for known packages
            if (packageName == "com.google.android.youtube") {
                openWebUrl("https://www.youtube.com")
                return true
            } else if (packageName == "com.openai.chatgpt") {
                openWebUrl("https://chatgpt.com")
                return true
            } else if (packageName == "com.whatsapp") {
                openWebUrl("https://web.whatsapp.com")
                return true
            }

            return false
        } catch (e: Exception) {
            Log.e("SystemActionHelper", "Launch error", e)
            return false
        }
    }

    fun openChatGPT(): String {
        return try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage("com.openai.chatgpt")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                "Opening ChatGPT App..."
            } else {
                openWebUrl("https://chatgpt.com")
                "Opening ChatGPT in browser..."
            }
        } catch (e: Exception) {
            openWebUrl("https://chatgpt.com")
            "Opening ChatGPT in browser..."
        }
    }

    fun openWebUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("SystemActionHelper", "Failed to open URL $url", e)
        }
    }

    fun prepareWhatsAppMessage(phone: String, message: String) {
        try {
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
            val uri = if (cleanPhone.isNotBlank()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=$encodedMsg")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to standard share
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, message)
                type = "text/plain"
                `package` = "com.whatsapp"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(sendIntent)
            } catch (ex: Exception) {
                openWebUrl("https://web.whatsapp.com")
            }
        }
    }

    fun prepareSmsMessage(phone: String, message: String) {
        try {
            val uri = Uri.parse("smsto:${phone.trim()}")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("SystemActionHelper", "SMS launch error", e)
        }
    }

    fun openDialer(phoneNumber: String) {
        try {
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+*#]"), "")
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("SystemActionHelper", "Dialer launch error", e)
        }
    }

    fun handleVoiceCommand(
        command: String,
        onAppLaunch: (String) -> Unit
    ): VoiceCommandResult? {
        val lower = command.lowercase().trim()

        if (lower.startsWith("open youtube")) {
            launchAppByPackage("com.google.android.youtube")
            onAppLaunch("YouTube")
            return VoiceCommandResult("Opening YouTube for you now.", true)
        }

        if (lower.startsWith("open whatsapp")) {
            launchAppByPackage("com.whatsapp")
            onAppLaunch("WhatsApp")
            return VoiceCommandResult("Launching WhatsApp for you.", true)
        }

        if (lower.startsWith("open chatgpt") || lower.startsWith("launch chatgpt")) {
            openChatGPT()
            onAppLaunch("ChatGPT")
            return VoiceCommandResult("Opening ChatGPT.", true)
        }

        if (lower.startsWith("open chrome") || lower.startsWith("open browser")) {
            launchAppByPackage("com.android.chrome")
            onAppLaunch("Chrome")
            return VoiceCommandResult("Opening web browser.", true)
        }

        if (lower.startsWith("open settings")) {
            launchAppByPackage("android.settings")
            onAppLaunch("Settings")
            return VoiceCommandResult("Opening Android system settings.", true)
        }

        if (lower.startsWith("open camera")) {
            launchAppByPackage("system.camera")
            onAppLaunch("Camera")
            return VoiceCommandResult("Opening camera.", true)
        }

        if (lower.startsWith("hello arafat") || lower == "hi arafat") {
            return VoiceCommandResult(
                "Hello! I am ARAFAT AI, your personal AI assistant developed by ARAFAT MOJMUNDAR. How can I assist your work or code today?",
                false
            )
        }

        return null
    }
}

data class VoiceCommandResult(
    val responseText: String,
    val executedSystemAction: Boolean
)
