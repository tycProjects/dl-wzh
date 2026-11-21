package com.android.island.ai

/**
 * Decides whether a Shizuku-backed setting change may run. Pure logic, unit tested.
 * Only a short list of safe toggles is allowed, and only when the user's own message named that
 * setting. Anything the model invents on its own is refused, so the AI never changes a setting unasked.
 */
object ShizukuGuard {
    /** Lowercase words that show the user asked for this setting. */
    private val KEYWORDS: Map<DeviceActions.Setting, List<String>> = mapOf(
        DeviceActions.Setting.WIFI to listOf("wifi", "wi-fi", "wi fi"),
        DeviceActions.Setting.BLUETOOTH to listOf("bluetooth"),
        DeviceActions.Setting.MOBILE_DATA to listOf("data seluler", "mobile data", "internet seluler"),
        DeviceActions.Setting.AIRPLANE to listOf("pesawat", "airplane"),
        DeviceActions.Setting.LOCATION to listOf("lokasi", "gps", "location"),
        DeviceActions.Setting.AUTO_ROTATE to listOf("rotasi", "rotate", "putar layar"),
        DeviceActions.Setting.FLASHLIGHT to listOf("senter", "flashlight", "torch"),
        DeviceActions.Setting.DND to listOf("jangan ganggu", "dnd", "do not disturb")
    )

    fun allowedToggle(setting: DeviceActions.Setting, userText: String): Boolean {
        val text = userText.lowercase()
        val words = KEYWORDS[setting] ?: return false
        return words.any { text.contains(it) }
    }
}
