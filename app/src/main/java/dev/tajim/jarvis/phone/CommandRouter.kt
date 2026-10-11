package dev.tajim.jarvis.phone

/**
 * Local rules for common phone commands (English and basic Bengali). Anything it does not recognise returns null
 * and goes to the AI instead. This is a rule list, not a language model: unusual phrasing will not match.
 */
class CommandRouter {

    fun normalize(text: String): String {
        var t = text.lowercase().trim().replace(Regex("[.!?,]+\$"), "")
        t = t.replace(Regex("^(hey |ok |okay )?jarvis[, ]*"), "").trim()
        t = t.replace(Regex("^(please |can you |could you |will you )"), "").trim()
        return t.replace(Regex("\\s+"), " ")
    }

    fun isSleep(n: String) =
        n == "sleep" || n == "go to sleep" || n.contains("go to sleep") || n.contains("sleep mode") || n.startsWith("sleep ") ||
            n.contains("ঘুমাও") || n.contains("ঘুমিয়ে") || n.contains("ঘুমাতে যাও")

    fun isYes(n: String) = n in setOf("yes", "yeah", "yep", "sure", "ok", "okay", "confirm", "do it", "go ahead") ||
        n.contains("হ্যাঁ") || n.contains("হাঁ") || n.contains("ঠিক আছে") || n == "haan"

    fun parse(text: String): Command? {
        val t = normalize(text)
        if (t.isEmpty()) return null

        // ---- Bengali (basic) ----
        Regex("^(.+?)\\s*(কে )?(কল করো|ফোন করো|কল দাও|কল কর)\$").find(t)?.let { return Command.Call(it.groupValues[1].trim()) }
        Regex("^(.+?)\\s*(খুলো|খোলো|ওপেন করো|ওপেন কর|চালু করো)\$").find(t)?.let { return Command.OpenApp(it.groupValues[1].trim()) }
        if (t.contains("আনলক") || t.contains("স্ক্রিন অন")) return Command.UnlockScreen
        if (t.contains("লক করো") || t.contains("ফোন লক") || t.contains("স্ক্রিন লক")) return Command.LockScreen
        if (t.contains("হোমে যাও") || t.contains("হোম স্ক্রিন")) return Command.Home
        if (t.contains("পিছনে যাও") || t == "ব্যাক") return Command.Back
        if (t.contains("কয়টা বাজে") || t.contains("সময় কত")) return Command.Time
        if (t.contains("আবহাওয়া")) return Command.WeatherNow
        if (t.contains("ব্যাটারি")) return Command.Battery

        // ---- English: system actions first ----
        if (t in setOf("home", "go home", "go to home", "home screen", "go to the home screen")) return Command.Home
        if (t in setOf("back", "go back", "press back")) return Command.Back
        if (t in setOf("recents", "recent apps", "show recent apps", "open recent apps", "show recents")) return Command.Recents
        if (t.contains("notification") && (t.startsWith("open") || t.startsWith("show") || t.startsWith("pull"))) return Command.Notifications
        if (t.contains("unlock") || t.contains("wake up the phone") || t.contains("turn on the screen") || t.contains("wake the screen")) return Command.UnlockScreen
        if (t.contains("lock") && (t.contains("phone") || t.contains("screen") || t.contains("device") || t == "lock")) return Command.LockScreen
        if (t.contains("scroll down")) return Command.ScrollDown
        if (t.contains("scroll up")) return Command.ScrollUp
        if (t.contains("volume up") || t.contains("increase the volume") || t.contains("increase volume") || t.contains("louder")) return Command.VolumeUp
        if (t.contains("volume down") || t.contains("decrease the volume") || t.contains("decrease volume") || t.contains("lower the volume") || t.contains("quieter")) return Command.VolumeDown
        if (t.contains("flashlight") || t.contains("torch")) return Command.Flashlight(on = !t.contains("off"))

        // ---- English: contacts and messaging ----
        Regex("^(?:send (?:a )?)?(?:whatsapp )?message (?:to )?(.+?) (?:saying|say|that says|that) (.+)\$").find(t)
            ?.let { return Command.WhatsApp(it.groupValues[1].trim(), it.groupValues[2].trim()) }
        Regex("^whatsapp (.+?) (?:saying|say) (.+)\$").find(t)
            ?.let { return Command.WhatsApp(it.groupValues[1].trim(), it.groupValues[2].trim()) }
        Regex("^(?:call|phone|ring|make a call to|make a phone call to) (.+)\$").find(t)?.let { return Command.Call(it.groupValues[1].trim()) }

        // ---- English: screen interaction ----
        Regex("^(?:click|tap|press) (?:on )?(?:the )?(.+)\$").find(t)?.let { return Command.ClickText(it.groupValues[1].trim()) }
        Regex("^type (.+)\$").find(t)?.let { return Command.TypeText(text.trim().substringAfter(' ').trim()) }

        // ---- English: web ----
        Regex("^play (.+?) on youtube\$").find(t)?.let { return Command.WebSearch(it.groupValues[1], youtube = true) }
        Regex("^search (?:for )?(.+?) on youtube\$").find(t)?.let { return Command.WebSearch(it.groupValues[1], youtube = true) }
        Regex("^(?:search|google|look up) (?:for )?(.+?)(?: on google)?\$").find(t)?.let { return Command.WebSearch(it.groupValues[1], youtube = false) }

        // ---- English: information ----
        if (t.contains("what time") || t.contains("what's the time") || t == "time") return Command.Time
        if (t.contains("battery")) return Command.Battery
        if (t.contains("weather") || t.contains("temperature outside")) return Command.WeatherNow

        // ---- English: open an app (after the more specific "open ..." forms above) ----
        Regex("^(?:open|launch|start|run) (?:the )?(.+?)(?: app| application)?\$").find(t)?.let { return Command.OpenApp(it.groupValues[1].trim()) }
        return null
    }
}
