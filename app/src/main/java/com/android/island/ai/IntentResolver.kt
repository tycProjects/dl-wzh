package com.android.island.ai

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Fallback for commands the keyword router did not recognize. Asks the configured model
 * to map the sentence onto a fixed set of device actions and returns them as [AiAction]s.
 * The model never gets shell access: only the actions listed in [SYSTEM] are accepted.
 * Any failure or a non-command answer returns null, and the message goes to chat as usual.
 */
object IntentResolver {

    private val SYSTEM = """
        You map a user's spoken or typed message to device actions. Reply with JSON only, no prose.
        Schema: {"actions":[ ... ]}. Each action is one of:
        {"type":"open_app","query":"<app name>"}
        {"type":"read_screen"}
        {"type":"search_in_app","app":"<app name>","query":"<contact or search term>"}
        {"type":"click","text":"<visible button or result label>"}
        {"type":"type","text":"<text to enter>"}
        {"type":"wait","ms":1200}
        {"type":"scroll","direction":"DOWN","times":1}
        {"type":"scroll_to","text":"<visible target>","direction":"DOWN","max_swipes":8}
        {"type":"swipe","direction":"LEFT|RIGHT|UP|DOWN"}
        {"type":"long_press","text":"<visible target>"}
        {"type":"drag","from":"<source label>","to":"<target label>"}
        {"type":"pinch","zoom_in":true|false}
        {"type":"back"}
        {"type":"app_home","app":"<app name, optional>"}
        {"type":"tap_first_result"}
        {"type":"toggle","setting":"WIFI|BLUETOOTH|MOBILE_DATA|AIRPLANE|AUTO_ROTATE|DND|FLASHLIGHT|LOCATION","on":true|false}
        {"type":"media","op":"PLAY|PAUSE|PLAY_PAUSE|NEXT|PREV"}
        {"type":"volume","op":"UP|DOWN|MUTE|UNMUTE"}
        {"type":"search","query":"<search terms>"}
        {"type":"search_deep","query":"<search terms>"}
        For a multi-step goal, return only the FIRST action. Prefer open_app first when the target app is not known to be open. Never claim the task is complete. If the message is ordinary chat or a question, reply {"actions":[]}.
        Understand casual Indonesian and English, typos, and filler words. Never invent other action types.
    """.trimIndent()

    private val SYSTEM_KEYS = setOf("HOME", "RECENTS", "NOTIFICATIONS", "QUICK_SETTINGS", "LOCK", "SCREENSHOT", "POWER")

    private fun cleanNumber(s: String): String = s.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }

    /** Longest wait for the model before the message goes to chat instead. */
    const val TIMEOUT_MS = 3000L

    /** True only for a short, single-step request to open an app. Multi-step goals must use the agent loop. */
    fun isSimpleOpenAppRequest(text: String): Boolean {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size !in 1..4) return false
        val continuation = Regex("(?i)\\b(lalu|kemudian|setelah|terus|cari|ketik|enter|pilih|pertama|berikutnya|scroll|putar|mainkan|buka video)\\b")
        return !continuation.containsMatchIn(text)
    }

    /** A message is only sent to the model if it contains one of these device words. */
    private val DEVICE_WORDS = listOf(
        "scroll", "gulir", "geser", "swipe", "tunggu", "klik", "tekan", "tahan", "seret", "drag", "zoom", "perbesar", "perkecil",
        "nyala", "nyalain", "nyalakan", "matiin", "matikan", "hidupkan", "aktifkan", "nonaktifkan",
        "buka", "bukain", "putar", "lagu", "musik", "volume", "keraskan", "kencengin", "pelanin",
        "senter", "wifi", "wi-fi", "bluetooth", "data", "lokasi", "gps", "rotasi", "pesawat",
        "lanjut", "berikutnya", "sebelumnya", "jeda", "bisukan", "mute", "cari",
        "play", "pause", "next", "open", "turn", "search"
    )

    private val SETTINGS = setOf(
        "WIFI", "BLUETOOTH", "MOBILE_DATA", "AIRPLANE", "AUTO_ROTATE", "DND", "FLASHLIGHT", "LOCATION"
    )
    private val MEDIA_OPS = setOf("PLAY", "PAUSE", "PLAY_PAUSE", "NEXT", "PREV")
    private val VOLUME_OPS = setOf("UP", "DOWN", "MUTE", "UNMUTE")

    suspend fun resolve(
        text: String,
        chat: suspend (List<ApiChatMessage>) -> String
    ): List<AiAction>? {
        // Use the caller's routed chat function so intent recovery respects the same
        // primary-key rotation and configured backup providers as the main agent.
        val raw = runCatching {
            chat(listOf(ApiChatMessage.system(SYSTEM), ApiChatMessage.user(text)))
        }.getOrNull() ?: return null
        return parse(raw)
    }

    /** Pure parser, separated so it can be unit tested without a network call. */
    fun parse(raw: String): List<AiAction>? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val arr = runCatching { JSONObject(raw.substring(start, end + 1)).optJSONArray("actions") }
            .getOrNull() ?: return null
        return actionsFrom(arr).takeIf { it.isNotEmpty() }
    }

    /** Validates a JSON array of actions. Anything outside the allowed set is dropped. */
    fun actionsFrom(arr: JSONArray): List<AiAction> {
        val out = mutableListOf<AiAction>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            when (item.optString("type")) {
                "open_app" -> item.optString("query").trim().takeIf { it.isNotBlank() }
                    ?.let { out += AiAction.OpenApp(it) }
                "search" -> item.optString("query").trim().takeIf { it.isNotBlank() }
                    ?.let { out += AiAction.WebSearch(it) }
                "search_deep" -> item.optString("query").trim().takeIf { it.isNotBlank() }
                    ?.let { out += AiAction.WebSearch(it, deep = true) }
                "api_find" -> item.optString("query").trim().takeIf { it.isNotBlank() }
                    ?.let { out += AiAction.ApiFind(it) }
                "api_get" -> item.optString("url").trim().takeIf { it.startsWith("https://") }
                    ?.let { out += AiAction.ApiGet(it) }
                "toggle" -> {
                    val name = item.optString("setting").uppercase()
                    if (name in SETTINGS) {
                        runCatching { DeviceActions.Setting.valueOf(name) }.getOrNull()?.let {
                            out += AiAction.ToggleSetting(it, item.optBoolean("on", true))
                        }
                    }
                }
                "open_url" -> item.optString("url").trim().takeIf { it.startsWith("http") }?.let {
                    out += AiAction.OpenUrl(it)
                }
                "click" -> if (item.has("index") && item.optString("text").isBlank()) {
                    item.optInt("index", -1).takeIf { it >= 0 }?.let { out += AiAction.ClickIndex(it) }
                } else {
                    (item.optString("text").trim().ifBlank { item.optString("query").trim() })
                        .takeIf { it.isNotBlank() }?.let { out += AiAction.Click(it) }
                }
                "click_index" -> item.optInt("index", -1).takeIf { it >= 0 }?.let { out += AiAction.ClickIndex(it) }
                "tap" -> if (item.has("x") && item.has("y")) {
                    val x = item.optInt("x", -1)
                    val y = item.optInt("y", -1)
                    if (x >= 0 && y >= 0) out += AiAction.TapXY(x, y)
                }
                "press_enter" -> out += AiAction.PressEnter
                "app_home" -> out += AiAction.AppHome(item.optString("app").trim())
                "scroll" -> out += AiAction.Scroll(
                    down = !item.optString("direction").equals("UP", true),
                    times = item.optInt("times", 1).coerceIn(1, AiCommandRouter.MAX_SCROLL_TIMES)
                )
                "scroll_to" -> item.optString("text").trim().takeIf { it.isNotBlank() }?.let {
                    out += AiAction.ScrollTo(it, !item.optString("direction").equals("UP", true), item.optInt("max_swipes", 8).coerceIn(1, 12))
                }
                "swipe" -> runCatching { AiAction.Swipe(AiAction.Swipe.Direction.valueOf(item.optString("direction").uppercase())) }.getOrNull()?.let { out += it }
                "long_press" -> item.optString("text").trim().takeIf { it.isNotBlank() }?.let { out += AiAction.LongPress(it) }
                "drag" -> {
                    val from = item.optString("from").trim()
                    val to = item.optString("to").trim()
                    if (from.isNotBlank() && to.isNotBlank()) out += AiAction.Drag(from, to)
                }
                "pinch" -> out += AiAction.Pinch(item.optBoolean("zoom_in", true))
                "wait" -> out += AiAction.Wait(item.optLong("ms", 2000L).coerceIn(0L, AiCommandRouter.MAX_WAIT_MS))
                "back" -> out += AiAction.Back
                "list_apps" -> out += AiAction.ListApps
                "media_status" -> out += AiAction.MediaStatus
                "read_screen" -> out += AiAction.ReadScreen
                "notification_summary" -> out += AiAction.NotificationSummary
                "search_screen" -> out += AiAction.SearchScreen
                "current_app" -> out += AiAction.CurrentApp
                "search_in_app" -> {
                    val app = item.optString("app").trim()
                    val q = item.optString("query").trim()
                    if (app.isNotBlank() && q.isNotBlank()) out += AiAction.SearchInApp(app, q)
                }
                "play_in_app" -> {
                    val app = item.optString("app").trim()
                    val q = item.optString("query").trim()
                    if (app.isNotBlank() && q.isNotBlank()) out += AiAction.PlayInApp(app, q)
                }
                "generate_image" -> item.optString("prompt").trim().takeIf { it.isNotBlank() }?.let {
                    out += AiAction.GenerateImage(it)
                }
                "tap_first_result" -> out += AiAction.TapFirstResult
                "system_key" -> item.optString("key").uppercase().takeIf { it in SYSTEM_KEYS }?.let {
                    out += AiAction.SystemKey(it)
                }
                "timer" -> item.optInt("seconds", 0).takeIf { it > 0 }?.let {
                    out += AiAction.Timer(it.coerceAtMost(86_400))
                }
                "alarm" -> {
                    val h = item.optInt("hour", -1)
                    val m = item.optInt("minute", 0)
                    if (h in 0..23 && m in 0..59) out += AiAction.Alarm(h, m, item.optString("message").trim())
                }
                "call" -> cleanNumber(item.optString("number")).takeIf { it.isNotBlank() }?.let {
                    out += AiAction.Call(it)
                }
                "sms" -> cleanNumber(item.optString("number")).takeIf { it.isNotBlank() }?.let {
                    out += AiAction.Sms(it, item.optString("text").trim())
                }
                "navigate" -> item.optString("destination").trim().takeIf { it.isNotBlank() }?.let {
                    out += AiAction.Navigate(it)
                }
                "camera" -> out += AiAction.Camera(item.optString("mode").equals("VIDEO", true))
                "volume_set" -> out += AiAction.VolumeSet(item.optInt("percent", 50).coerceIn(0, 100))
                "copy" -> item.optString("text").takeIf { it.isNotBlank() }?.let { out += AiAction.Copy(it) }
                "share" -> item.optString("text").takeIf { it.isNotBlank() }?.let { out += AiAction.Share(it) }
                "type" -> item.optString("text").takeIf { it.isNotBlank() }?.let { out += AiAction.TypeText(it) }
                "shell" -> item.optString("command").trim().takeIf { it.isNotBlank() }?.let {
                    out += AiAction.Shell(it, "perintah shell")
                }
                "diagnose" -> DiagnosticPolicy.kindFrom(item.optString("kind"))?.let {
                    out += AiAction.Diagnose(it, item.optString("app").trim().takeIf { a -> a.isNotBlank() })
                }
                "force_stop" -> item.optString("app").trim().takeIf { it.isNotBlank() }?.let {
                    out += AiAction.ForceStop(it)
                }
                "open_settings" -> item.optString("page").trim().lowercase().takeIf { it.isNotBlank() }?.let {
                    out += AiAction.OpenSettings(it)
                }
                "play_song" -> item.optString("query").trim().takeIf { it.isNotBlank() }?.let {
                    out += AiAction.PlaySong(it)
                }
                "media" -> item.optString("op").uppercase().takeIf { it in MEDIA_OPS }?.let {
                    out += AiAction.Media(AiAction.Media.Op.valueOf(it))
                }
                "volume" -> item.optString("op").uppercase().takeIf { it in VOLUME_OPS }?.let {
                    out += AiAction.Volume(AiAction.Volume.Op.valueOf(it))
                }
            }
        }
        return out
    }

    /** Only short, non-question messages are worth a model call; ordinary chat keeps its speed.
     * Uses token matching instead of a large nested regular expression so malformed grouping can
     * never crash the main thread while the composer classifies a message.
     */
    fun looksLikeCommand(text: String): Boolean {
        val t = text.trim().lowercase(Locale.ROOT)
        if (t.length !in 2..500 || t.endsWith("?") || t.endsWith("？")) return false
        val tokens = t.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.size > 60) return false

        val explanationMarkers = listOf(
            "jelasin", "jelaskan", "terangkan", "apa itu", "kenapa", "mengapa",
            "bagaimana", "gimana", "cara ", "maksudnya", "could you explain", "explain",
            "why ", "how do i", "how to ", "what is ", "menurut kamu", "apa pendapat"
        )
        if (explanationMarkers.any(t::contains)) return false

        val polite = setOf("tolong", "mohon", "please", "pls", "coba", "bisa", "silakan", "ayo", "yuk")
        var index = 0
        while (index < tokens.size && index < 3 && tokens[index] in polite) index++
        if (index + 1 < tokens.size) {
            val requestPrefix = "${tokens[index]} ${tokens[index + 1]}"
            if (requestPrefix in setOf("aku mau", "saya mau", "tolong bantu", "bantu saya")) index += 2
        }
        if (index >= tokens.size) return false

        val first = tokens[index]
        val firstTwo = if (index + 1 < tokens.size) "$first ${tokens[index + 1]}" else first
        val singleWordCommands = setOf(
            "buka", "bukakan", "bukain", "jalankan", "launch", "open", "start",
            "scroll", "gulir", "swipe", "geser", "klik", "click", "tekan", "tap",
            "tahan", "seret", "drag", "pinch", "zoom", "cari", "carikan", "cariin",
            "search", "ketik", "type", "masukkan", "enter", "pilih", "putar", "mainkan",
            "play", "pause", "jeda", "lanjutkan", "berikutnya", "sebelumnya", "nyalakan",
            "nyalain", "hidupkan", "aktifkan", "matikan", "matiin", "nonaktifkan", "naikkan",
            "turunkan", "besarkan", "kecilkan", "atur", "setel", "kembali", "back", "timer",
            "alarm", "navigasi"
        )
        if (first in singleWordCommands) return true
        val phraseCommands = setOf(
            "long press", "baca layar", "baca isi", "ambil screenshot", "paksa berhenti",
            "force stop", "buatkan gambar", "buat gambar"
        )
        return firstTwo in phraseCommands ||
            (first == "baca" && index + 2 < tokens.size && tokens[index + 1] == "isi" && tokens[index + 2] == "layar")
    }
}
