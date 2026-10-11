package dev.tajim.jarvis.phone

sealed class Command {
    data class OpenApp(val name: String) : Command()
    data object Home : Command()
    data object Back : Command()
    data object Recents : Command()
    data object Notifications : Command()
    data object LockScreen : Command()
    data object UnlockScreen : Command()
    data object ScrollDown : Command()
    data object ScrollUp : Command()
    data object VolumeUp : Command()
    data object VolumeDown : Command()
    data class Flashlight(val on: Boolean) : Command()
    data class Call(val target: String) : Command()
    data class WhatsApp(val contact: String, val message: String) : Command()
    data class ClickText(val text: String) : Command()
    data class TypeText(val text: String) : Command()
    data class WebSearch(val query: String, val youtube: Boolean) : Command()
    data object Time : Command()
    data object Battery : Command()
    data object WeatherNow : Command()
}

sealed class ActionResult {
    data class Done(val message: String) : ActionResult()
    data class Failed(val message: String) : ActionResult()
    /** Consequential action: JARVIS asks first and runs [run] only after a yes. */
    data class Confirm(val prompt: String, val run: suspend () -> ActionResult) : ActionResult()
}
