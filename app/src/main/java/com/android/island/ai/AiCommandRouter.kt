package com.android.island.ai

import java.util.Locale

/** Instant device / web / shell command recognized locally (no model call required). */
sealed class AiAction {
    data class OpenApp(val query: String) : AiAction()
    data class ToggleSetting(val setting: DeviceActions.Setting, val turnOn: Boolean) : AiAction()
    data class Media(val op: Op) : AiAction() {
        enum class Op { PLAY, PAUSE, PLAY_PAUSE, NEXT, PREV }
    }
    data class Volume(val op: Op) : AiAction() {
        enum class Op { UP, DOWN, MUTE, UNMUTE }
    }
    /** [deep] runs the multi-round research pipeline (variant rounds + page reads) for "cari lebih dalam". */
    data class WebSearch(val query: String, val deep: Boolean = false) : AiAction()
    /** Looks up API candidates in the bundled directory; authentication/pricing must be verified in docs. */
    data class ApiFind(val query: String) : AiAction()
    /** One GET to an HTTPS endpoint of a listed public API host. The response goes back to the model. */
    data class ApiGet(val url: String) : AiAction()
    data class Shell(val command: String, val label: String) : AiAction()
    /** Read-only diagnostics through Shizuku. The output is given back to the model to explain. */
    data class Diagnose(val kind: DiagnosticPolicy.Kind, val app: String?) : AiAction()
    /** Force-stop one app, only when the user asked for it by name and in those words. */
    data class ForceStop(val app: String) : AiAction()
    data class OpenUrl(val url: String) : AiAction()
    data class Click(val text: String) : AiAction()
    data class Scroll(val down: Boolean, val times: Int = 1) : AiAction()
    data class ScrollTo(val text: String, val down: Boolean = true, val maxSwipes: Int = 8) : AiAction()
    data class Swipe(val direction: Direction) : AiAction() {
        enum class Direction { UP, DOWN, LEFT, RIGHT }
    }
    data class LongPress(val text: String) : AiAction()
    data class Drag(val fromText: String, val toText: String) : AiAction()
    data class Pinch(val zoomIn: Boolean) : AiAction()
    data class Wait(val ms: Long) : AiAction()
    data class GenerateImage(val prompt: String) : AiAction()
    object Back : AiAction()
    data class PlaySong(val query: String) : AiAction()
    data class SystemKey(val key: String) : AiAction()
    data class Timer(val seconds: Int) : AiAction()
    data class Alarm(val hour: Int, val minute: Int, val message: String) : AiAction()
    data class Call(val number: String) : AiAction()
    data class Sms(val number: String, val text: String) : AiAction()
    data class Navigate(val destination: String) : AiAction()
    data class Camera(val video: Boolean) : AiAction()
    data class VolumeSet(val percent: Int) : AiAction()
    data class Copy(val text: String) : AiAction()
    data class Share(val text: String) : AiAction()
    data class TypeText(val text: String) : AiAction()
    data class OpenSettings(val page: String) : AiAction()
    object ListApps : AiAction()
    object MediaStatus : AiAction()
    object ReadScreen : AiAction()
    object CurrentApp : AiAction()
    object NotificationSummary : AiAction()
    object SearchScreen : AiAction()
    object TapFirstResult : AiAction()
    /** Presses the keyboard's Enter / Search / Go key in the focused field. */
    object PressEnter : AiAction()
    /** Taps raw screen coordinates, for icon-only buttons that have no text label. */
    data class TapXY(val x: Int, val y: Int) : AiAction()
    /** Taps element [index] from the numbered "Elemen interaktif" list of the latest observation. */
    data class ClickIndex(val index: Int) : AiAction()
    data class SearchInApp(val app: String, val query: String) : AiAction()
    /**
     * Returns the app to its main page: back-presses / home tab until a search entry or the main feed is
     * visible, relaunching only as a last resort. [app] may be blank to mean "the app in front".
     */
    data class AppHome(val app: String = "") : AiAction()
    data class PlayInApp(val app: String, val query: String) : AiAction()
}

/**
 * Intent-style router: understands casual Indonesian the way people actually talk.
 * Not a template list — if the wish is clear, we act. Unclear text goes to the model.
 */
object AiCommandRouter {

    /** Upper bound for one scroll step, so a typo cannot scroll a feed forever. */
    const val MAX_SCROLL_TIMES = 20

    /** Longest single wait. Longer waits are cut, so the assistant cannot stall the phone. */
    const val MAX_WAIT_MS = 10_000L

    private val SCROLL_WORDS = listOf("scroll", "gulir")
    private val SWIPE_WORDS = listOf("swipe", "geser")
    private val WAIT_WORDS = listOf("tunggu", "wait")
    private val NUMBER_WORDS = listOf(
        "sekali" to 1, "satu" to 1, "dua" to 2, "tiga" to 3, "empat" to 4, "lima" to 5,
        "enam" to 6, "tujuh" to 7, "delapan" to 8, "sembilan" to 9, "sepuluh" to 10
    )

    private val SETTING_KEYWORDS = listOf(
        "mode pesawat" to DeviceActions.Setting.AIRPLANE,
        "airplane mode" to DeviceActions.Setting.AIRPLANE,
        "data seluler" to DeviceActions.Setting.MOBILE_DATA,
        "paket data" to DeviceActions.Setting.MOBILE_DATA,
        "mobile data" to DeviceActions.Setting.MOBILE_DATA,
        "rotasi otomatis" to DeviceActions.Setting.AUTO_ROTATE,
        "auto rotate" to DeviceActions.Setting.AUTO_ROTATE,
        "jangan ganggu" to DeviceActions.Setting.DND,
        "do not disturb" to DeviceActions.Setting.DND,
        "lampu senter" to DeviceActions.Setting.FLASHLIGHT,
        "senter" to DeviceActions.Setting.FLASHLIGHT,
        "flashlight" to DeviceActions.Setting.FLASHLIGHT,
        "wifi" to DeviceActions.Setting.WIFI,
        "wi-fi" to DeviceActions.Setting.WIFI,
        "bluetooth" to DeviceActions.Setting.BLUETOOTH,
        "lokasi" to DeviceActions.Setting.LOCATION,
        "gps" to DeviceActions.Setting.LOCATION
    )

    fun parse(text: String): AiAction? = parseAll(text).firstOrNull()

    /** Typing is refused while a password field has focus; the user types passwords themselves. */
    fun typeAllowed(focusedIsPassword: Boolean): Boolean = !focusedIsPassword

    /**
     * Strips punctuation and polite filler so "tolong matiin wifi dong!" and
     * "matiin wifi" resolve the same way. Applied once, before any matcher,
     * so every substring index below refers to the same cleaned string.
     */
    private val FILLER = setOf(
        "dong", "donk", "ya", "yah", "tolong", "sih", "deh", "nih", "kak", "dong?",
        "please", "pls", "plis", "coba", "mohon", "gan", "bro"
    )

    private val SHELL_PREFIXES = listOf("jalankan shell:", "run shell:", "shell:", "shizuku:")

    fun clean(text: String): String {
        val trimmed = text.trim()
        // Shell payloads are passed through untouched: filler/punctuation removal would alter the command.
        if (SHELL_PREFIXES.any { trimmed.lowercase(Locale.getDefault()).startsWith(it) }) return trimmed
        return trimmed.trimEnd('!', '?', '.', ',', ';', ':')
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it.lowercase(Locale.getDefault()) !in FILLER }
            .joinToString(" ")
            .trim()
    }

    fun parseAll(text: String): List<AiAction> {
        // Politeness is read from the original text: cleaning strips it, but a soft ask still means "turn on".
        val softAsk = SOFT_ASK.containsMatchIn(text.lowercase(Locale.getDefault()))
        val raw = clean(text)
        if (raw.isBlank()) return emptyList()
        val lower = raw.lowercase(Locale.getDefault())

        // Single full-message intents (cleanup etc.)
        shellFrom(lower, raw)?.let { return listOf(it) }

        // Split sequential wishes: "buka youtube terus google", "matiin wifi dan nyalain senter"
        val parts = splitIntents(raw)
        val actions = mutableListOf<AiAction>()
        for (part in parts) {
            val p = part.trim()
            if (p.isBlank()) continue
            val l = p.lowercase(Locale.getDefault())
            val one = waitFrom(l)
                ?: scrollFrom(l)
                ?: swipeFrom(l)
                ?: mediaFrom(l)
                ?: volumeFrom(l)
                ?: toggleFrom(l, softAsk)
                ?: inAppSearchFrom(p)
                ?: searchFrom(l, p)
                ?: openAppFrom(l, p)
                ?: shellFrom(l, p)
            if (one != null) actions += one
        }
        return actions
    }

    private fun splitIntents(raw: String): List<String> {
        // Keep "cari X dan Y" as one search; only split when it looks like two commands.
        val lower = raw.lowercase(Locale.getDefault())
        // A sentence that STARTS with a search verb keeps "dan", "sama", "&" and "," inside the query
        // ("cari di google resep nasi dan telur"); only real sequencing words start a new step.
        if (SEARCH_VERB_START.containsMatchIn(lower)) {
            return raw.split(Regex("""\s+(?:lalu|terus|kemudian|sekaligus)\s+""", RegexOption.IGNORE_CASE))
                .map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { listOf(raw) }
        }
        if (SEARCH_HINTS.any { lower.startsWith(it) || lower.contains(" $it ") }) {
            // don't split pure search queries on "dan"
            if (!OPEN_HINTS.any { lower.contains(it) } && !TOGGLE_ON.any { lower.contains(it) } &&
                !TOGGLE_OFF.any { lower.contains(it) } && !MEDIA_HINTS.any { lower.contains(it) }
            ) return listOf(raw)
        }
        return raw.split(
            Regex("""\s*(?:,|\/|&|\+|lalu|terus|kemudian|sekaligus|bareng(?:an)?|sama|dan)\s+""", RegexOption.IGNORE_CASE)
        ).map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { listOf(raw) }
    }

    private val SEARCH_VERB_START = Regex("""^(?:cari|carikan|cariin|search|telusuri)\b""")

    private val OPEN_HINTS = listOf("buka", "bukain", "bukakan", "jalankan", "jalanin", "buka dong", "open", "launch", "start")
    private val TOGGLE_ON = listOf(
        "nyalakan", "nyalain", "nyalain dong", "idupin", "hidupkan", "aktifkan", "aktifin",
        "turn on", "enable", "kasih nyala", "tolong nyalain"
    )
    private val TOGGLE_OFF = listOf(
        "matikan", "matiin", "matiin dong", "padamkan", "nonaktifkan", "nonaktifin",
        "turn off", "disable", "tolong matiin"
    )
    private val MEDIA_HINTS = listOf("lagu", "musik", "music", "spotify", "putar", "play", "pause", "jeda")
    private val SEARCH_HINTS = listOf(
        "cariin", "cariin tentang", "carikan", "cari tentang", "cari di internet",
        "cari di web", "telusuri", "search", "search for", "apa itu", "siapa itu", "jelaskan tentang"
    )

    /** Semantic content scrolling; swipe/geser is handled separately as a directional gesture. */
    private fun scrollFrom(lower: String): AiAction.Scroll? {
        if (SCROLL_WORDS.none { lower.contains(it) }) return null
        val up = Regex("""\b(atas|up)\b""").containsMatchIn(lower)
        return AiAction.Scroll(down = !up, times = countFrom(lower))
    }

    private fun swipeFrom(lower: String): AiAction.Swipe? {
        if (SWIPE_WORDS.none { lower.contains(it) }) return null
        val direction = when {
            Regex("\\b(kiri|left)\\b").containsMatchIn(lower) -> AiAction.Swipe.Direction.LEFT
            Regex("\\b(kanan|right)\\b").containsMatchIn(lower) -> AiAction.Swipe.Direction.RIGHT
            Regex("\\b(atas|up)\\b").containsMatchIn(lower) -> AiAction.Swipe.Direction.UP
            else -> AiAction.Swipe.Direction.DOWN
        }
        return AiAction.Swipe(direction)
    }

    private fun countFrom(lower: String): Int {
        Regex("""(\d+)\s*kali""").find(lower)?.groupValues?.get(1)?.toIntOrNull()
            ?.let { return it.coerceIn(1, MAX_SCROLL_TIMES) }
        NUMBER_WORDS.firstOrNull { (word, _) -> Regex("""\b$word\b""").containsMatchIn(lower) }
            ?.let { return it.second }
        return 1
    }

    /** "tunggu 3 detik", "tunggu 1 menit" (capped), a bare "tunggu" waits 2 seconds. */
    private fun waitFrom(lower: String): AiAction.Wait? {
        if (WAIT_WORDS.none { lower.startsWith(it) }) return null
        val m = Regex("""(\d+)\s*(detik|second|sec|menit|minute|min)""").find(lower)
            ?: return AiAction.Wait(2_000L)
        val amount = m.groupValues[1].toLong()
        val unit = m.groupValues[2]
        val ms = if (unit == "menit" || unit == "minute" || unit == "min") amount * 60_000L else amount * 1_000L
        return AiAction.Wait(ms.coerceIn(0L, MAX_WAIT_MS))
    }

    private fun mediaFrom(lower: String): AiAction.Media? {
        val aboutMedia = MEDIA_HINTS.any { lower.contains(it) } ||
            lower.contains("track") || lower.contains("lagunya")
        when {
            lower.contains("berikutnya") || lower.contains("next") || lower.contains("skip") ||
                lower.contains("lancar") && aboutMedia ->
                if (aboutMedia || lower.contains("next") || lower.contains("skip"))
                    return AiAction.Media(AiAction.Media.Op.NEXT)
            lower.contains("sebelumnya") || lower.contains("previous") || lower.contains("prev") ->
                if (aboutMedia || lower.contains("previous") || lower.contains("prev"))
                    return AiAction.Media(AiAction.Media.Op.PREV)
            lower.contains("jeda") || lower.contains("pause") || lower.contains("stop musik") ||
                lower.contains("stop music") || lower.contains("hentikan lagu") ||
                lower.contains("berhentiin lagu") || lower.contains("berhentiin musik") ->
                return AiAction.Media(AiAction.Media.Op.PAUSE)
            // "nyalain musik", "putar lagu dong", "play music", "lanjut lagunya"
            (lower.contains("putar") || lower.contains("play") || lower.contains("lanjut") ||
                TOGGLE_ON.any { lower.contains(it) }) && aboutMedia ->
                return AiAction.Media(AiAction.Media.Op.PLAY)
            lower in listOf("play", "putar", "play pause") ->
                return AiAction.Media(AiAction.Media.Op.PLAY_PAUSE)
        }
        return null
    }

    private fun volumeFrom(lower: String): AiAction.Volume? = when {
        lower.contains("volume up") || lower.contains("perbesar volume") || lower.contains("keraskan") ||
            lower.contains("naikkan volume") || lower.contains("kencengin") || lower.contains("lebih kencang") ||
            lower == "volume+" || lower == "vol+" -> AiAction.Volume(AiAction.Volume.Op.UP)
        lower.contains("volume down") || lower.contains("kecilkan volume") || lower.contains("pelanin") ||
            lower.contains("turunkan volume") || lower.contains("lebih pelan") ||
            lower == "volume-" || lower == "vol-" -> AiAction.Volume(AiAction.Volume.Op.DOWN)
        lower.contains("bisukan") || lower.contains("mute") || lower == "silent" || lower.contains("diemkan") ->
            AiAction.Volume(AiAction.Volume.Op.MUTE)
        lower.contains("unmute") || lower.contains("suarakan") ->
            AiAction.Volume(AiAction.Volume.Op.UNMUTE)
        else -> null
    }

    private val SOFT_ASK = Regex("""\b(dong|tolong|ya)\b""")

    private fun toggleFrom(lower: String, softAsk: Boolean): AiAction.ToggleSetting? {
        val setting = SETTING_KEYWORDS.firstOrNull { (kw, _) -> lower.contains(kw) }?.second ?: return null
        val turnOn = when {
            TOGGLE_ON.any { lower.contains(it) } -> true
            TOGGLE_OFF.any { lower.contains(it) } -> false
            // bare "senter" / "wifi" with soft asks still counts as on
            softAsk -> true
            else -> return null
        }
        return AiAction.ToggleSetting(setting, turnOn)
    }

    private val IN_APP_TARGET = Regex(
        "(?i)\\b(?:di|pada|lewat|via)\\s+(youtube|tiktok|instagram|spotify|google maps|maps|play store|shopee|tokopedia|lazada|whatsapp|chrome|google|gmail|telegram|netflix|x|twitter|facebook|pinterest)\\b"
    )

    /** "carikan kucing di YouTube" must open the app and search there, not run a web search. */
    internal fun isInAppSearch(text: String): Boolean = IN_APP_TARGET.containsMatchIn(text)

    private const val APP_NAMES =
        "google maps|play store|youtube music|google|chrome|youtube|tiktok|instagram|spotify|maps|shopee|tokopedia|lazada|" +
            "whatsapp|facebook|twitter|pinterest|telegram|netflix|gmail"
    private val SEARCH_APP_FIRST = Regex(
        "^(?:cari|carikan|cariin|search|telusuri)\\s+(?:di|pada|lewat|via)\\s+($APP_NAMES)\\s+(.+)\$", RegexOption.IGNORE_CASE
    )
    private val SEARCH_QUERY_FIRST = Regex(
        "^(?:cari|carikan|cariin|search|telusuri)\\s+(.+?)\\s+(?:di|pada|lewat|via)\\s+($APP_NAMES)\$", RegexOption.IGNORE_CASE
    )

    private fun displayApp(name: String): String =
        name.split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

    /** "cari di google mediator markaz quran" / "carikan kucing di youtube" -> search inside that app. */
    private fun inAppSearchFrom(original: String): AiAction.SearchInApp? {
        val text = original.trim()
        SEARCH_APP_FIRST.matchEntire(text)?.let { m ->
            val q = m.groupValues[2].trim()
            if (q.isNotBlank()) return AiAction.SearchInApp(displayApp(m.groupValues[1].lowercase(Locale.getDefault())), q.take(160))
        }
        SEARCH_QUERY_FIRST.matchEntire(text)?.let { m ->
            val q = m.groupValues[1].trim()
            if (q.isNotBlank()) return AiAction.SearchInApp(displayApp(m.groupValues[2].lowercase(Locale.getDefault())), q.take(160))
        }
        return null
    }

    private fun searchFrom(lower: String, original: String): AiAction.WebSearch? {
        if (isInAppSearch(original)) return null
        for (hint in SEARCH_HINTS) {
            val idx = lower.indexOf(hint)
            if (idx < 0) continue
            // leading or mid-sentence search intent
            var rest = original.substring(idx + hint.length).trim()
            rest = rest.removePrefix(":").removePrefix("-").removePrefix("tentang").trim()
            if (rest.isBlank()) continue
            if (rest.length > 160) rest = rest.take(160)
            // "cari lebih dalam X" / "riset mendalam X" goes straight to the deep research pipeline.
            return AiAction.WebSearch(rest, deep = ResearchPlanner.isDeepResearchQuery(lower))
        }
        return null
    }

    private fun openAppFrom(lower: String, original: String): AiAction.OpenApp? {
        for (verb in OPEN_HINTS) {
            val idx = lower.indexOf(verb)
            if (idx < 0) continue
            // verb should be near the start of this fragment
            if (idx > 12) continue
            var rest = original.substring(idx + verb.length).trim()
            rest = rest.removePrefix("aplikasi").removePrefix("app").removePrefix("dong").trim()
            if (rest.isBlank() || rest.length > 40) continue
            val rl = rest.lowercase(Locale.getDefault())
            if (MEDIA_HINTS.any { rl.contains(it) } || SETTING_KEYWORDS.any { rl.contains(it.first) }) continue
            // strip trailing politeness
            rest = rest.replace(Regex("""\s+(dong|ya|please|pls|tolong)\s*$""", RegexOption.IGNORE_CASE), "").trim()
            if (rest.isBlank()) continue
            return AiAction.OpenApp(rest)
        }
        return null
    }

    private val HELP_QUESTIONS = listOf(
        "perintah apa", "bisa apa", "apa saja yang bisa", "daftar perintah", "fitur apa", "kamu bisa apa",
        "apa aja yang bisa", "list perintah", "daftar fitur", "bisa ngapain"
    )

    /** True when the user asks what the assistant can do. Answered locally with the full command list. */
    fun isHelpQuestion(text: String): Boolean {
        val lower = clean(text).lowercase(Locale.getDefault())
        return HELP_QUESTIONS.any { lower.contains(it) }
    }

    /** Fixed command for the cache cleanup intent. Only this exact action is reachable from chat text. */
    const val CLEANUP_COMMAND = "pm trim-caches 999G"

    private fun shellFrom(lower: String, original: String): AiAction.Shell? {
        if (
            lower.contains("bersihkan perangkat") || lower.contains("bersihin perangkat") ||
            lower.contains("bersihkan hp") || lower.contains("bersihin hp") ||
            lower.contains("bersihkan cache") || lower.contains("bersihin cache") ||
            lower.contains("kosongkan cache") || lower.contains("trim cache") ||
            lower.contains("bersih-bersih hp") || lower.contains("bersih bersih hp")
        ) {
            return AiAction.Shell(CLEANUP_COMMAND, "membersihkan cache perangkat")
        }
        return null
    }

}
