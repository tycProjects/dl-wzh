package com.android.island.ai

/** Which actions need the user's explicit "ya" before they run. Pure logic, unit tested. */
object ConfirmPolicy {
    private val yes = setOf("ya", "iya", "y", "yes", "lanjut", "lanjutkan", "oke", "ok", "setuju", "konfirmasi")
    private val no = setOf("tidak", "batal", "jangan", "cancel", "no", "stop")

    fun needsConfirmation(action: AiAction): Boolean = when (action) {
        is AiAction.ForceStop, is AiAction.Call, is AiAction.Sms, is AiAction.ToggleSetting, is AiAction.Share -> true
        is AiAction.Shell -> ShellPolicy.needsConfirmation(action.command)
        else -> false
    }

    fun isYes(text: String): Boolean = text.trim().trimEnd('.', '!').lowercase() in yes
    fun isNo(text: String): Boolean = text.trim().trimEnd('.', '!').lowercase() in no

    fun describe(action: AiAction): String = when (action) {
        is AiAction.ForceStop -> "memaksa berhenti ${action.app}"
        is AiAction.Call -> "menelepon ${action.number}"
        is AiAction.Sms -> "mengirim SMS ke ${action.number}: \"${action.text}\""
        is AiAction.ToggleSetting -> "${action.setting.label} ${if (action.turnOn) "dinyalakan" else "dimatikan"}"
        is AiAction.Share -> "membagikan teks"
        is AiAction.Shell -> "menjalankan perintah shell berisiko: ${action.command.trim().take(120)}"
        else -> "tindakan"
    }
}
