package com.android.island.ai

import java.util.Locale

/**
 * A deterministic second gate between model-proposed actions and device execution.
 * It always examines the user's original request, never assistant prose or screen text.
 * Generic UI navigation may advance an explicitly requested task; privileged or
 * side-effecting action families require matching words in that original request.
 */
object UserIntentActionGuard {
    private fun normalized(text: String): String = text.lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}+*#]+"), " ")
        .trim()

    private fun hasAny(text: String, vararg terms: String): Boolean = terms.any { term ->
        normalized(text).contains(normalized(term))
    }

    private fun containsTarget(text: String, target: String): Boolean {
        val t = normalized(target)
        if (t.isBlank()) return false
        val source = normalized(text)
        if (source.contains(t)) return true
        val targetDigits = target.filter { it.isDigit() }
        val sourceDigits = text.filter { it.isDigit() }
        return targetDigits.length >= 5 && sourceDigits.contains(targetDigits)
    }

    /** "chrome" / "browser" / "google" are the same thing to a user who said "cari di google". */
    private val BROWSER_ALIASES = setOf("google", "chrome", "browser", "peramban", "google chrome", "internet")

    private fun appMentioned(text: String, app: String): Boolean {
        if (containsTarget(text, app)) return true
        val a = normalized(app)
        return a in BROWSER_ALIASES && BROWSER_ALIASES.any { alias -> normalized(text).contains(alias) }
    }

    private val EXPLANATION_MARKERS = listOf(
        "jelaskan", "jelasin", "terangkan", "apa itu", "apa fungsi", "kenapa", "mengapa", "bagaimana", "gimana", "cara "
    )

    /** "Jelaskan fungsi YouTube" names an app but asks for an explanation, not for the app to be opened. */
    private fun isExplanationRequest(text: String): Boolean = EXPLANATION_MARKERS.any { normalized(text).contains(it) }

    /** True when every word of the typed text was said by the user (a name, a query), so it is not invented. */
    private fun typedTextComesFromUser(userText: String, typed: String): Boolean {
        val words = normalized(typed).split(' ').filter { it.length >= 2 }
        if (words.isEmpty()) return false
        val source = normalized(userText)
        return words.all { source.contains(it) }
    }

    fun allows(action: AiAction, userText: String, resolverConfirmed: Boolean = false): Boolean {
        val text = normalized(userText)
        if (text.isBlank()) return false

        // The resolver is called with the original user message and has a restricted schema.
        // Its output may be used as a language fallback, but still cannot bypass high-impact gates.
        return when (action) {
            is AiAction.ToggleSetting -> {
                val settingMentioned = when (action.setting) {
                    DeviceActions.Setting.WIFI -> hasAny(text, "wifi", "wi-fi")
                    DeviceActions.Setting.BLUETOOTH -> hasAny(text, "bluetooth")
                    DeviceActions.Setting.MOBILE_DATA -> hasAny(text, "data seluler", "paket data", "mobile data")
                    DeviceActions.Setting.AIRPLANE -> hasAny(text, "mode pesawat", "airplane mode")
                    DeviceActions.Setting.AUTO_ROTATE -> hasAny(text, "rotasi otomatis", "auto rotate")
                    DeviceActions.Setting.DND -> hasAny(text, "jangan ganggu", "do not disturb", "dnd")
                    DeviceActions.Setting.FLASHLIGHT -> hasAny(text, "senter", "flashlight")
                    DeviceActions.Setting.LOCATION -> hasAny(text, "lokasi", "gps", "location")
                }
                settingMentioned && hasAny(text, "nyalakan", "nyalain", "hidupkan", "aktifkan", "matikan", "matiin", "nonaktifkan", "turn on", "turn off", "enable", "disable")
            }
            is AiAction.ForceStop -> hasAny(text, "paksa berhenti", "force stop") && containsTarget(text, action.app)
            is AiAction.Call -> hasAny(text, "telepon", "telpon", "panggil", "call") && containsTarget(text, action.number)
            is AiAction.Sms -> hasAny(text, "sms", "pesan", "kirim pesan", "text") && containsTarget(text, action.number)
            is AiAction.Shell -> text.startsWith("/shell ") || text == "/shell" || hasAny(text, "jalankan shell", "run shell", "perintah terminal")
            is AiAction.Share -> hasAny(text, "bagikan", "share")
            is AiAction.Copy -> hasAny(text, "salin", "copy")
            is AiAction.Alarm -> hasAny(text, "alarm", "bangunkan")
            is AiAction.Timer -> hasAny(text, "timer", "pengatur waktu", "hitung mundur")
            is AiAction.OpenSettings -> hasAny(text, "pengaturan", "settings", "setelan") || containsTarget(text, action.page)
            is AiAction.GenerateImage -> hasAny(text, "buat gambar", "buatkan gambar", "generate image", "gambarkan", "ilustrasikan")
            is AiAction.Navigate -> hasAny(text, "navigasi", "arah", "rute", "navigate", "directions")
            is AiAction.Camera -> hasAny(text, "kamera", "foto", "ambil gambar", "video")
            is AiAction.PlaySong -> hasAny(text, "putar lagu", "mainkan lagu", "play song", "putar musik")
            is AiAction.Media -> hasAny(text, "putar", "mainkan", "play", "pause", "jeda", "lanjut", "berikutnya", "sebelumnya", "next", "previous")
            is AiAction.Volume, is AiAction.VolumeSet -> hasAny(text, "volume", "suara", "keras", "pelan", "mute", "bisukan")
            is AiAction.OpenApp ->
                (containsTarget(text, action.query) && !isExplanationRequest(text)) ||
                    resolverConfirmed && hasAny(text, "buka aplikasi", "open app")
            is AiAction.WebSearch, is AiAction.ApiFind -> hasAny(text, "cari", "search", "berita", "cuaca", "kurs", "harga", "terbaru")
            is AiAction.OpenUrl -> hasAny(text, "buka situs", "buka website", "kunjungi", "open website", "buka link") || containsTarget(text, action.url)
            is AiAction.SearchInApp ->
                (hasAny(text, "cari", "search", "temukan", "telusuri") && appMentioned(text, action.app)) ||
                    (resolverConfirmed && appMentioned(text, action.app))
            is AiAction.PlayInApp -> hasAny(text, "putar", "mainkan", "play", "cari", "search") && containsTarget(text, action.app)
            is AiAction.TypeText -> hasAny(text, "ketik", "tuliskan", "masukkan", "isi dengan", "type") || resolverConfirmed ||
                // Text taken from the user's own words ("buka kontak ummi faris" -> typing "ummi faris") is not invented.
                typedTextComesFromUser(userText, action.text) ||
                // A search or message task implies typing into the field the agent just focused.
                (IntentResolver.looksLikeCommand(userText) &&
                    hasAny(text, "cari", "search", "temukan", "telusuri", "kirim", "balas", "tulis", "pesan", "kontak", "chat"))
            is AiAction.Diagnose -> hasAny(text, "cek", "periksa", "diagnosa", "diagnosis", "memori", "ram", "baterai", "cpu", "penyimpanan", "storage")
            is AiAction.SystemKey -> when (action.key) {
                "HOME" -> hasAny(text, "home", "beranda", "layar utama")
                "RECENTS" -> hasAny(text, "recent", "aplikasi terakhir")
                "NOTIFICATIONS" -> hasAny(text, "notifikasi", "notifications")
                "QUICK_SETTINGS" -> hasAny(text, "quick settings", "pengaturan cepat")
                "LOCK" -> hasAny(text, "kunci layar", "lock screen")
                "SCREENSHOT" -> hasAny(text, "screenshot", "tangkapan layar")
                "POWER" -> hasAny(text, "menu daya", "power menu")
                else -> false
            }
            // These are task-navigation primitives. They are allowed only when the user
            // actually issued an imperative; they are not authorized by an AI answer alone.
            is AiAction.AppHome, is AiAction.Click, is AiAction.Scroll, is AiAction.ScrollTo, is AiAction.Swipe,
            is AiAction.LongPress, is AiAction.Drag, is AiAction.Pinch, is AiAction.Wait,
            AiAction.Back, AiAction.ReadScreen, AiAction.CurrentApp, AiAction.ListApps,
            AiAction.MediaStatus, AiAction.NotificationSummary, AiAction.SearchScreen,
            AiAction.TapFirstResult, AiAction.PressEnter, is AiAction.TapXY, is AiAction.ClickIndex ->
                IntentResolver.looksLikeCommand(userText) || resolverConfirmed
            is AiAction.ApiGet -> resolverConfirmed || hasAny(text, "api", "endpoint", "ambil data", "data dari", "cuaca", "kurs", "nilai tukar", "hari libur", "info negara", "trivia")
        }
    }
}
