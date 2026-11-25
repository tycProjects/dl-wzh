package com.example.device

import com.example.data.CommandAliasEntity

sealed interface LocalIntentAction {
    data class LaunchApp(val query: String, val explicitPackage: String? = null) : LocalIntentAction
    data class Flashlight(val turnOn: Boolean?) : LocalIntentAction // null = toggle
    data class VolumeChange(val deltaPercent: Int? = null, val targetPercent: Int? = null, val mute: Boolean? = null) : LocalIntentAction
    data class BrightnessChange(val deltaPercent: Int? = null, val targetPercent: Int? = null) : LocalIntentAction
    data object QueryBattery : LocalIntentAction
    data object QueryStorage : LocalIntentAction
    data object QueryNetwork : LocalIntentAction
    data object QueryDeviceInfo : LocalIntentAction
    data class OpenSettings(val target: String, val labelBn: String) : LocalIntentAction
    data class InAppNavigation(val destination: String, val scrollDirection: String? = null) : LocalIntentAction
    data object StartScreenShare : LocalIntentAction
    data object StopScreenShare : LocalIntentAction
    data class SensitiveConfirmationRequired(val actionSummaryBn: String, val actionType: String, val rawText: String) : LocalIntentAction
    data class AutomationSequence(val steps: List<LocalIntentAction>, val rawText: String) : LocalIntentAction
    data object FallbackToGemini : LocalIntentAction
}

object LocalCommandEngine {

    fun parse(input: String, customAliases: List<CommandAliasEntity> = emptyList()): LocalIntentAction {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return LocalIntentAction.FallbackToGemini

        val normalized = trimmed.lowercase()

        // 0. Check multi-step automation sequence ("তারপর", "then", "এবং তারপর")
        val sequenceSeparators = listOf(" তারপর ", " এরপর ", " then ", " and then ")
        for (sep in sequenceSeparators) {
            if (normalized.contains(sep)) {
                val parts = trimmed.split(Regex("তারপর|এরপর|then|and then", RegexOption.IGNORE_CASE))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                if (parts.size >= 2) {
                    val parsedSteps = parts.map { parseSingle(it, customAliases) }
                    if (parsedSteps.none { it is LocalIntentAction.FallbackToGemini }) {
                        return LocalIntentAction.AutomationSequence(parsedSteps, trimmed)
                    }
                }
            }
        }

        return parseSingle(trimmed, customAliases)
    }

    private fun parseSingle(input: String, customAliases: List<CommandAliasEntity>): LocalIntentAction {
        val lower = input.lowercase().trim()

        // 1. Custom Command Aliases first (fastest local override)
        val matchedAlias = customAliases.firstOrNull { alias ->
            alias.enabled && (lower == alias.triggerPhrase.lowercase().trim() ||
                lower.contains(alias.triggerPhrase.lowercase().trim()))
        }
        if (matchedAlias != null) {
            return LocalIntentAction.LaunchApp(
                query = matchedAlias.targetAppLabel,
                explicitPackage = matchedAlias.targetPackageName
            )
        }

        // 2. Safety check for sensitive actions (never auto-execute without confirmation)
        val sensitiveKeywords = listOf(
            "টাকা পাঠাও", "পেমেন্ট", "কিনে ফেল", "পাসওয়ার্ড", "ডিলিট", "মুছে ফেল",
            "ফাইল মুছ", "মেসেজ পাঠাও", "কল করো", "send message", "delete", "pay", "purchase", "transfer money"
        )
        if (sensitiveKeywords.any { lower.contains(it) }) {
            return LocalIntentAction.SensitiveConfirmationRequired(
                actionSummaryBn = "তুমি একটি সংবেদনশীল কাজ করতে বলছো: \"$input\"। নিশ্চিত হয়ে এগিয়ে যেতে চাও?",
                actionType = "sensitive_action",
                rawText = input
            )
        }

        // 3. Screen Share commands
        if (lower.contains("স্ক্রিন শেয়ার বন্ধ") || lower.contains("stop screen share")) {
            return LocalIntentAction.StopScreenShare
        }
        if (lower.contains("স্ক্রিন শেয়ার") || lower.contains("screen share")) {
            return LocalIntentAction.StartScreenShare
        }

        // 4. Flashlight commands
        if (lower.contains("ফ্ল্যাশলাইট") || lower.contains("টর্চ") || lower.contains("লাইট জ্বালা") ||
            lower.contains("লাইট বন্ধ") || lower.contains("flashlight") || lower.contains("torch")
        ) {
            return when {
                lower.contains("বন্ধ") || lower.contains("অফ") || lower.contains("off") ->
                    LocalIntentAction.Flashlight(false)
                lower.contains("চালু") || lower.contains("অন") || lower.contains("জ্বালা") || lower.contains("on") ->
                    LocalIntentAction.Flashlight(true)
                else -> LocalIntentAction.Flashlight(null)
            }
        }

        // 5. Volume commands
        if (lower.contains("ভলিউম") || lower.contains("সাউন্ড") || lower.contains("আওয়াজ") ||
            lower.contains("volume") || lower.contains("মিউট") || lower.contains("mute")
        ) {
            val pct = extractNumber(lower)
            return when {
                lower.contains("আনমিউট") || lower.contains("unmute") ->
                    LocalIntentAction.VolumeChange(mute = false)
                lower.contains("মিউট") || lower.contains("mute") ->
                    LocalIntentAction.VolumeChange(mute = true)
                pct != null && (lower.contains("%") || lower.contains("পার্সেন্ট") || lower.contains("percent") || lower.contains("করো")) ->
                    LocalIntentAction.VolumeChange(targetPercent = pct)
                lower.contains("বাড়া") || lower.contains("বেশি") || lower.contains("up") || lower.contains("increase") ->
                    LocalIntentAction.VolumeChange(deltaPercent = 15)
                lower.contains("কমা") || lower.contains("কম") || lower.contains("down") || lower.contains("decrease") ->
                    LocalIntentAction.VolumeChange(deltaPercent = -15)
                else -> LocalIntentAction.VolumeChange(deltaPercent = 0)
            }
        }

        // 6. Brightness commands
        if (lower.contains("ব্রাইটনেস") || lower.contains("আলো") || lower.contains("brightness")) {
            val pct = extractNumber(lower)
            return when {
                pct != null && (lower.contains("%") || lower.contains("পার্সেন্ট") || lower.contains("percent") || lower.contains("করো")) ->
                    LocalIntentAction.BrightnessChange(targetPercent = pct)
                lower.contains("বাড়া") || lower.contains("up") || lower.contains("increase") ->
                    LocalIntentAction.BrightnessChange(deltaPercent = 20)
                lower.contains("কমা") || lower.contains("down") || lower.contains("decrease") ->
                    LocalIntentAction.BrightnessChange(deltaPercent = -20)
                else -> LocalIntentAction.BrightnessChange(deltaPercent = 0)
            }
        }

        // 7. Battery, Storage, Network, Device Info
        if (lower.contains("ব্যাটারি") || lower.contains("চার্জ") || lower.contains("battery")) {
            return LocalIntentAction.QueryBattery
        }
        if (lower.contains("স্টোরেজ") || lower.contains("মেমোরি") || lower.contains("জায়গা খালি") || lower.contains("storage")) {
            return LocalIntentAction.QueryStorage
        }
        if (lower.contains("ইন্টারনেট") || lower.contains("নেটওয়ার্ক") || lower.contains("ওয়াইফাই কানেক্ট") ||
            lower.contains("wi-fi কানেক্ট") || lower.contains("wifi connected") || lower.contains("network")
        ) {
            return LocalIntentAction.QueryNetwork
        }
        if (lower.contains("ফোনের তথ্য") || lower.contains("ডিভাইস ইনফো") || lower.contains("র‍্যাম") || lower.contains("device info")) {
            return LocalIntentAction.QueryDeviceInfo
        }

        // 8. System Settings shortcuts
        if (lower.contains("ওয়াইফাই সেটিংস") || lower.contains("wifi settings") || lower.contains("ইন্টারনেট সেটিংস")) {
            return LocalIntentAction.OpenSettings("internet", "ইন্টারনেট ও Wi-Fi")
        }
        if (lower.contains("ব্লুটুথ") || lower.contains("bluetooth")) {
            return LocalIntentAction.OpenSettings("bluetooth", "Bluetooth")
        }
        if (lower.contains("ডিসপ্লে সেটিংস") || lower.contains("display settings")) {
            return LocalIntentAction.OpenSettings("display", "Display")
        }
        if (lower.contains("অ্যাক্সেসিবিলিটি") || lower.contains("accessibility")) {
            return LocalIntentAction.OpenSettings("accessibility", "Accessibility")
        }

        // 9. In-app Screen Navigation
        when {
            lower.contains("হোম স্ক্রিন") || lower == "হোম" || lower == "home" ->
                return LocalIntentAction.InAppNavigation("home")
            lower.contains("ব্যাক করো") || lower == "ব্যাক" || lower == "back" ->
                return LocalIntentAction.InAppNavigation("back")
            lower.contains("নিচে স্ক্রল") || lower.contains("scroll down") ->
                return LocalIntentAction.InAppNavigation("scroll", "down")
            lower.contains("উপরে যাও") || lower.contains("উপরে স্ক্রল") || lower.contains("scroll up") ->
                return LocalIntentAction.InAppNavigation("scroll", "up")
            lower.contains("ডিভাইস পেজ") || lower.contains("ডিভাইসে যাও") ->
                return LocalIntentAction.InAppNavigation("device")
            lower.contains("চ্যাটে যাও") || lower.contains("চ্যাট খোলো") ->
                return LocalIntentAction.InAppNavigation("chat")
            lower.contains("টুলস খোলো") || lower.contains("সেটিংস পেজ") ->
                return LocalIntentAction.InAppNavigation("tools")
        }

        // 10. Universal App Launch triggers
        val launchTriggers = listOf("খোলো", "খুলো", "খুলে দাও", "চালু করো", "ওপেন করো", "ওপেন", "open ", "launch ", "start ")
        if (launchTriggers.any { lower.contains(it) }) {
            return LocalIntentAction.LaunchApp(query = input)
        }

        return LocalIntentAction.FallbackToGemini
    }

    private fun extractNumber(text: String): Int? {
        val bnDigits = mapOf(
            '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
            '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
        )
        val normalized = text.map { bnDigits[it] ?: it }.joinToString("")
        val match = Regex("\\d{1,3}").find(normalized)
        return match?.value?.toIntOrNull()?.coerceIn(0, 100)
    }
}
