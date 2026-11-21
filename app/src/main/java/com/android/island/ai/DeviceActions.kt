package com.android.island.ai

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.provider.Settings
import com.android.island.core.Controls
import com.android.island.system.ShizukuShell
import java.util.Locale

/**
 * Instant, on-device actions the chat can trigger without ever calling the AI API — the same
 * "Galaxy AI"-style instant command as "buka YouTube" or "matikan WiFi". Nothing here is sent
 * anywhere; it is plain PackageManager / CameraManager / NotificationManager calls, with a
 * Shizuku shell command as the privileged fallback for toggles the app itself cannot flip
 * (WRITE_SECURE_SETTINGS-class), and opening the matching system Settings screen as the last
 * resort when Shizuku is not granted.
 */
object DeviceActions {

    enum class Setting(val label: String) {
        WIFI("WiFi"),
        BLUETOOTH("Bluetooth"),
        MOBILE_DATA("Data seluler"),
        AIRPLANE("Mode pesawat"),
        LOCATION("Lokasi"),
        AUTO_ROTATE("Rotasi otomatis"),
        FLASHLIGHT("Senter"),
        DND("Jangan ganggu")
    }

    data class Result(val message: String)

    // ---- links, taps, scrolling -------------------------------------------

    fun openUrl(context: Context, url: String): Result {
        val u = url.trim()
        if (!u.startsWith("https://") && !u.startsWith("http://")) return Result("Link tidak valid.")
        // No built-in browser: hand the page to the device's own browser (Chrome/Google).
        return runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, android.net.Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            Result("Membuka $u di browser perangkat.")
        }.getOrElse { Result("Tidak ada aplikasi yang bisa membuka link ini.") }
    }

    fun click(text: String): Result {
        val label = text.trim()
        if (label.startsWith("Pencarian ·", true) || label.startsWith("Search ·", true)) {
            return Result("Lewati \"$label\": itu saran pencarian, bukan kolom atau hasil utama. Ketik di kolom pencarian lalu tekan Enter.")
        }
        if (!com.android.island.service.IslandAccessibilityService.isRunning) {
            return Result("Accessibility Android Island belum aktif, jadi tidak bisa menekan layar.")
        }
        return if (com.android.island.service.IslandAccessibilityService.clickByText(text)) {
            Result("Menekan \"$text\".")
        } else {
            Result("Tidak menemukan \"$text\" di layar sekarang.")
        }
    }

    fun scroll(down: Boolean): Result {
        if (!com.android.island.service.IslandAccessibilityService.isRunning) {
            return Result("Accessibility Android Island belum aktif, jadi tidak bisa scroll.")
        }
        return if (com.android.island.service.IslandAccessibilityService.scroll(down)) {
            Result(if (down) "Scroll ke bawah." else "Scroll ke atas.")
        } else {
            Result("Tidak ada area yang bisa di-scroll.")
        }
    }

    fun swipe(direction: AiAction.Swipe.Direction): Result {
        if (!com.android.island.service.IslandAccessibilityService.isRunning) return Result("Accessibility Android Island belum aktif, jadi tidak bisa melakukan swipe.")
        val ok = com.android.island.service.IslandAccessibilityService.swipeDirection(direction.name)
        return Result(if (ok) "Swipe ${direction.name.lowercase()} dikirim." else "Swipe tidak dapat dijalankan pada layar ini.")
    }

    fun longPress(text: String): Result {
        if (!com.android.island.service.IslandAccessibilityService.isRunning) return Result("Accessibility Android Island belum aktif.")
        val ok = com.android.island.service.IslandAccessibilityService.longPressText(text)
        return Result(if (ok) "Tekan lama pada \"$text\" dikirim." else "Elemen \"$text\" tidak ditemukan atau tidak dapat ditekan lama.")
    }

    fun drag(from: String, to: String): Result {
        if (!com.android.island.service.IslandAccessibilityService.isRunning) return Result("Accessibility Android Island belum aktif.")
        val ok = com.android.island.service.IslandAccessibilityService.dragTextToText(from, to)
        return Result(if (ok) "Gerakan seret dari \"$from\" ke \"$to\" dikirim." else "Sumber atau tujuan drag tidak ditemukan.")
    }

    fun pinch(zoomIn: Boolean): Result {
        if (!com.android.island.service.IslandAccessibilityService.isRunning) return Result("Accessibility Android Island belum aktif.")
        val ok = com.android.island.service.IslandAccessibilityService.pinch(zoomIn)
        return Result(if (ok) if (zoomIn) "Gerakan zoom-in dikirim." else "Gerakan zoom-out dikirim." else "Gerakan pinch tidak dapat dijalankan.")
    }

    /** One scroll: a finger-like swipe through Shizuku when available, else the accessibility scroll. */
    private suspend fun scrollOnce(down: Boolean): Boolean {
        val svc = com.android.island.service.IslandAccessibilityService
        val size = svc.screenSize()
        if (size != null) {
            val human = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { ShizukuShell.humanScroll(down, size.first, size.second) }.getOrDefault(false)
            }
            if (human) return true
        }
        return svc.scroll(down)
    }

    suspend fun scrollToText(text: String, down: Boolean, maxSwipes: Int): Result {
        if (!com.android.island.service.IslandAccessibilityService.isRunning) return Result("Accessibility Android Island belum aktif.")
        val target = text.trim()
        if (target.isEmpty()) return Result("Target pencarian kosong.")
        val limit = maxSwipes.coerceIn(1, 12)
        for (step in 0..limit) {
            val visible = com.android.island.service.IslandAccessibilityService.visibleTexts()
            if (visible.any { it.contains(target, ignoreCase = true) }) return Result("Menemukan \"$target\" setelah $step kali scroll.")
            if (step == limit) break
            if (!scrollOnce(down)) break
            kotlinx.coroutines.delay(ScrollGeometry.GAP_MS)
        }
        return Result("Belum menemukan \"$target\" setelah pencarian scroll terbatas. Konten mungkin belum dimuat atau teksnya tidak tersedia di Accessibility.")
    }

    /**
     * Scrolls [times] times with a short pause between gestures, so a multi-step request
     * such as "scroll 2 kali" really scrolls twice. Stops early when nothing can scroll.
     */
    suspend fun scrollTimes(down: Boolean, times: Int): Result {
        if (!com.android.island.service.IslandAccessibilityService.isRunning) {
            return Result("Accessibility Android Island belum aktif, jadi tidak bisa scroll.")
        }
        val n = times.coerceIn(1, AiCommandRouter.MAX_SCROLL_TIMES)
        var done = 0
        for (i in 0 until n) {
            if (!scrollOnce(down)) break
            done++
            if (i < n - 1) kotlinx.coroutines.delay(com.android.island.ai.ScrollGeometry.GAP_MS)
        }
        if (done == 0) return Result("Tidak ada area yang bisa di-scroll.")
        val dir = if (down) "bawah" else "atas"
        return Result("Scroll ke $dir $done kali.")
    }

    /**
     * Opens [app], searches [query] inside it, taps the matching result and starts playback.
     * Falls back to the media play command if the app does not start the track by itself.
     */
    suspend fun playInApp(context: Context, app: String, query: String): String {
        val (searchOk, searched) = runSearch(context, app, query)
        if (!searchOk) return searched
        // Results load after the search; keep looking for the query (not the search box) for a few seconds.
        var tapped = false
        for (attempt in 0 until 6) {
            kotlinx.coroutines.delay(if (attempt == 0) 1200L else 700L)
            tapped = com.android.island.service.IslandAccessibilityService.clickByText(query)
            if (tapped) break
        }
        if (!tapped) return "$searched Hasil \"$query\" belum terlihat untuk diketuk. Coba sebut judul lagu atau nama artis yang lebih spesifik."
        kotlinx.coroutines.delay(1500)
        val play = media(AiAction.Media.Op.PLAY).message
        return "Memutar \"$query\" di $app. $play"
    }

    /**
     * Taps the first visible text in the content area of the app in front. The top and bottom of
     * the screen (search bar, navigation) are skipped. Waits briefly so the opened page can start.
     */
    suspend fun tapFirstResult(): Result {
        if (!com.android.island.service.IslandAccessibilityService.isRunning) {
            return Result("Accessibility Android Island belum aktif, jadi tidak bisa mengetuk.")
        }
        if (!com.android.island.service.IslandAccessibilityService.tapFirstResult()) {
            return Result("Tidak menemukan hasil untuk diketuk di layar sekarang.")
        }
        kotlinx.coroutines.delay(1200)
        return Result("Mengetuk hasil pertama.")
    }

    /** Waits for an app or page to load before the next step. Capped by the router. */
    suspend fun wait(ms: Long): Result {
        val capped = ms.coerceIn(0L, AiCommandRouter.MAX_WAIT_MS)
        kotlinx.coroutines.delay(capped)
        return Result("Menunggu ${capped / 1000.0} detik.")
    }

    /**
     * Plays a specific song through the standard media-search intent, which music apps such as
     * Spotify and YouTube Music answer. If no app takes it, the song is searched in the browser.
     */
    fun playSong(context: Context, query: String): Result {
        val q = query.trim()
        if (q.isBlank()) return Result("Lagu apa yang mau diputar?")
        val intent = Intent("android.media.action.MEDIA_PLAY_FROM_SEARCH").apply {
            putExtra("android.intent.extra.focus", "vnd.android.cursor.item/*")
            putExtra("query", q)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            Result("Memutar \"$q\" di aplikasi musik.")
        } catch (_: Exception) {
            val url = "https://music.youtube.com/search?q=" + java.net.URLEncoder.encode(q, "UTF-8")
            val opened = openUrl(context, url)
            Result("Tidak ada aplikasi musik yang mendukung pencarian langsung. ${opened.message}")
        }
    }

    // ---- system keys, timers, calls, messages, navigation, camera, volume, clipboard, text input ----

    private fun start(context: Context, intent: Intent, okMessage: String): Result =
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Result(okMessage)
        } catch (_: Exception) {
            Result("Tidak ada aplikasi yang bisa menangani perintah ini.")
        }

    fun systemKey(key: String): Result =
        if (com.android.island.service.IslandAccessibilityService.performSystemKey(key)) {
            Result("Menjalankan ${key.lowercase()}.")
        } else {
            Result("Tidak bisa menjalankan $key. Pastikan Accessibility Android Island aktif.")
        }

    fun timer(context: Context, seconds: Int): Result {
        val intent = Intent("android.intent.action.SET_TIMER").apply {
            putExtra("android.intent.extra.alarm.LENGTH", seconds)
            putExtra("android.intent.extra.alarm.SKIP_UI", true)
        }
        return start(context, intent, "Timer $seconds detik dibuat.")
    }

    fun alarm(context: Context, hour: Int, minute: Int, message: String): Result {
        val intent = Intent("android.intent.action.SET_ALARM").apply {
            putExtra("android.intent.extra.alarm.HOUR", hour)
            putExtra("android.intent.extra.alarm.MINUTES", minute)
            putExtra("android.intent.extra.alarm.MESSAGE", message)
            putExtra("android.intent.extra.alarm.SKIP_UI", true)
        }
        return start(context, intent, "Alarm jam %02d:%02d dibuat.".format(hour, minute))
    }

    /** Opens the dialer with the number filled in. The user still taps call. */
    fun call(context: Context, number: String): Result =
        start(context, Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:$number")), "Nomor $number sudah ada di dialer. Tekan panggil untuk menelepon.")

    /** Opens the messages app with the text filled in. The user still taps send. */
    fun sms(context: Context, number: String, text: String): Result {
        val intent = Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:$number")).apply {
            putExtra("sms_body", text)
        }
        return start(context, intent, "Pesan untuk $number sudah disiapkan. Tekan kirim untuk mengirim.")
    }

    fun navigate(context: Context, destination: String): Result {
        val q = java.net.URLEncoder.encode(destination.trim(), "UTF-8")
        val nav = start(context, Intent(Intent.ACTION_VIEW, android.net.Uri.parse("google.navigation:q=$q")), "Navigasi ke $destination dimulai.")
        return if (nav.message.startsWith("Tidak ada")) {
            start(context, Intent(Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=$q")), "Membuka peta untuk $destination.")
        } else nav
    }

    fun camera(context: Context, video: Boolean): Result {
        val action = if (video) "android.media.action.VIDEO_CAPTURE" else "android.media.action.IMAGE_CAPTURE"
        return start(context, Intent(action), if (video) "Kamera video dibuka." else "Kamera dibuka.")
    }

    fun volumeSet(context: Context, percent: Int): Result {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val max = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, percent * max / 100, 0)
        return Result("Volume media diatur ke $percent%.")
    }

    fun copy(context: Context, text: String): Result {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("AI Island", text))
        return Result("Teks disalin ke clipboard.")
    }

    fun share(context: Context, text: String): Result {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        return start(context, Intent.createChooser(send, "Bagikan"), "Pilih aplikasi untuk membagikan teks.")
    }

    /** Outcome of pressing Enter: [verified] is true only when the screen really changed afterwards. */
    data class SubmitOutcome(val verified: Boolean, val route: String?, val tried: List<String>)

    private fun visibleScreenTexts(): List<String> =
        runCatching { com.android.island.service.IslandAccessibilityService.visibleTexts() }.getOrDefault(emptyList())

    /**
     * Presses Enter / Search through every route, Shizuku first (a real key or touch event, like a person),
     * and accepts a route only when the screen changed afterwards ([SubmitVerifier]). A route that merely
     * "accepted" the key proves nothing: TikTok ignores ACTION_IME_ENTER yet the call returns true.
     */
    suspend fun submitEnter(): SubmitOutcome = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val svc = com.android.island.service.IslandAccessibilityService
        val routes = listOf<Pair<String, () -> Boolean>>(
            "tombol ENTER nyata lewat Shizuku" to { ShizukuShell.pressKey(66) },
            "ketukan jari pada tombol aksi keyboard lewat Shizuku" to {
                val key = svc.imeActionKeyCenter()
                val size = svc.screenSize()
                key != null && size != null && ShizukuShell.humanTap(key.first, key.second, size.first, size.second)
            },
            "tombol SEARCH nyata lewat Shizuku" to { ShizukuShell.pressKey(84) },
            "tombol aksi keyboard lewat Accessibility" to { svc.tapImeActionKey() },
            "Accessibility IME_ENTER" to { svc.pressEnter() }
        )
        val tried = mutableListOf<String>()
        var before = visibleScreenTexts()
        for ((label, attempt) in routes) {
            val accepted = runCatching { attempt() }.getOrDefault(false)
            if (!accepted) { tried += "$label (ditolak/tidak tersedia)"; continue }
            kotlinx.coroutines.delay(1200)
            val after = visibleScreenTexts()
            if (SubmitVerifier.verdict(before, after) == SubmitVerifier.Verdict.SUBMITTED) {
                return@withContext SubmitOutcome(true, label, tried + label)
            }
            tried += "$label (diterima tetapi layar tidak berubah)"
            before = after
        }
        SubmitOutcome(false, null, tried)
    }

    private fun submitNote(out: SubmitOutcome): String =
        if (out.verified) "Enter ditekan lewat ${out.route}; layar berubah (terverifikasi)."
        else "Enter belum terbukti berhasil. Sudah dicoba: ${out.tried.joinToString("; ").ifBlank { "tidak ada rute yang tersedia" }}."

    suspend fun typeText(text: String): Result =
        if (!AiCommandRouter.typeAllowed(com.android.island.service.IslandAccessibilityService.focusedIsPassword())) {
            Result("Kolom kata sandi terdeteksi. Isi kata sandinya sendiri ya, aku tidak mengetik kata sandi.")
        } else if (com.android.island.service.IslandAccessibilityService.typeIntoFocused(text) ||
            com.android.island.service.IslandAccessibilityService.typeText(text)) {
            // A field the agent focused itself (click_index) wins; the message-box picker is the fallback.
            // Typing into a search box is only half the job: submit it, and report only what was proven.
            if (com.android.island.service.IslandAccessibilityService.focusedIsSearchField()) {
                val out = submitEnter()
                Result("Teks dimasukkan. " + submitNote(out) +
                    if (out.verified) "" else " Teks tetap ada di kolom; coba click_index pada ikon cari atau tap x,y.")
            } else Result("Teks dimasukkan ke kolom yang sedang aktif.")
        } else {
            Result("Tidak ada kolom teks yang aktif di layar sekarang.")
        }

    /** The press_enter action. Success is claimed only when the screen changed ([submitEnter]). */
    suspend fun pressEnter(): Result {
        val svc = com.android.island.service.IslandAccessibilityService
        if (!svc.isRunning) return Result("Accessibility Android Island belum aktif, jadi tidak bisa menekan tombol.")
        if (svc.focusedIsPassword()) return Result("Enter tidak ditekan: kolom kata sandi sedang aktif.")
        val out = submitEnter()
        return if (out.verified) Result(submitNote(out) + " Baca layar untuk melihat hasilnya.")
        else Result(submitNote(out) + " Jangan klaim sudah terkirim. Coba click_index pada ikon cari/kirim, atau tap x,y.")
    }

    fun tapXY(x: Int, y: Int): Result {
        if (!com.android.island.service.IslandAccessibilityService.isRunning) {
            return Result("Accessibility Android Island belum aktif, jadi tidak bisa mengetuk layar.")
        }
        val svc = com.android.island.service.IslandAccessibilityService
        // Shizuku first: a real injected touch with jitter and hold time, like a finger. Accessibility gesture is the fallback.
        val size = svc.screenSize()
        val human = size != null && runCatching { ShizukuShell.humanTap(x, y, size.first, size.second) }.getOrDefault(false)
        val ok = human || svc.tapAt(x.toFloat(), y.toFloat())
        return if (ok) Result("Mengetuk $x,$y (${if (human) "sentuhan jari lewat Shizuku" else "gesture Accessibility"}). Baca layar untuk memastikan hasilnya.")
        else Result("Tidak berhasil mengetuk $x,$y: koordinat di luar layar atau gesture ditolak. Matikan lalu nyalakan lagi Accessibility Android Island agar izin gesture aktif.")
    }

    fun clickIndex(index: Int): Result {
        val e = ScreenReader.elementByIndex(index)
            ?: return Result("Tidak menemukan elemen nomor $index. Baca layar lagi untuk daftar terbaru.")
        val tapped = tapXY(e.centerX, e.centerY)
        return if (tapped.message.startsWith("Mengetuk")) Result("Menekan elemen $index \"${e.label}\" di ${e.centerX},${e.centerY}.")
        else tapped
    }

    fun openSettings(context: Context, page: String): Result {
        val action = when (page) {
            "wifi" -> android.provider.Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
            "display" -> android.provider.Settings.ACTION_DISPLAY_SETTINGS
            "sound" -> android.provider.Settings.ACTION_SOUND_SETTINGS
            "battery" -> android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS
            "apps" -> android.provider.Settings.ACTION_APPLICATION_SETTINGS
            "location" -> android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
            "accessibility" -> android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
            "date" -> android.provider.Settings.ACTION_DATE_SETTINGS
            "storage" -> android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS
            else -> android.provider.Settings.ACTION_SETTINGS
        }
        return start(context, Intent(action), "Membuka pengaturan $page.")
    }

    /** Launchable apps on the device, as text the model can read. */
    fun listAppsText(context: Context): String {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val labels = pm.queryIntentActivities(intent, 0).map { it.loadLabel(pm).toString() }.distinct().sorted()
        if (labels.isEmpty()) return "Tidak ada aplikasi yang terlihat."
        return "Aplikasi terpasang (${labels.size}): " + labels.take(200).joinToString(", ")
    }

    /** What the media session is playing right now. */
    fun mediaStatusText(): String {
        val c = Controls.mediaController ?: return "Tidak ada pemutar media yang aktif."
        val md = c.metadata
        val title = md?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
        val artist = md?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
        if (title.isNullOrBlank()) return "Pemutar media aktif, tapi tidak ada judul lagu."
        val playing = c.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
        val who = if (artist.isNullOrBlank()) "" else " - $artist"
        return "Sedang ${if (playing) "diputar" else "dijeda"}: $title$who."
    }

    /** Visible text of the app in front, for the model to choose what to tap. */
    fun readScreenText(): String {
        val texts = com.android.island.service.IslandAccessibilityService.visibleTexts()
        if (texts.isEmpty()) return "Tidak ada teks yang terbaca di layar. Pastikan Accessibility Android Island aktif."
        return "Teks di layar sekarang: " + texts.take(80).joinToString(" | ")
    }

    /** Screen text from accessibility; when an app exposes almost nothing, the screenshot is read with OCR. */
    suspend fun readScreenWithOcr(): String {
        val base = readScreenText()
        val service = com.android.island.service.IslandAccessibilityService
        if (service.visibleTexts().size >= 5) return base
        val jpeg = service.captureScreenImageJpeg() ?: return base
        val ocr = ScreenOcr.recognizeJpeg(jpeg).take(4000)
        if (ocr.isBlank()) return base
        return "$base\n\nTeks dari gambar layar (OCR):\n$ocr"
    }

    /** Screen content for "search what is on screen": OCR of the screenshot plus accessibility text. */
    suspend fun screenForSearch(): String {
        val service = com.android.island.service.IslandAccessibilityService
        val jpeg = service.captureScreenImageJpeg()
        val ocr = (jpeg?.let { ScreenOcr.recognizeJpeg(it) }.orEmpty()).take(1500)
        val texts = service.visibleTexts().take(60).joinToString(" | ").take(1500)
        if (jpeg == null && texts.isBlank()) {
            return "Layar tidak bisa dibaca. Pastikan Accessibility Android Island aktif."
        }
        return "Konten layar untuk dicari. Teks dari gambar (OCR): ${ocr.ifBlank { "(tidak ada)" }}. " +
            "Teks aksesibilitas: ${texts.ifBlank { "(tidak ada)" }}. " +
            "Pilih kata kunci paling relevan dari teks ini untuk web_search. Jika tidak ada teks, jelaskan apa yang terlihat dan sebutkan bahwa hasilnya belum pasti."
    }

    /** Only the app in front, not a list of running apps. */
    fun currentAppText(context: Context): String {
        val pkg = com.android.island.ai.ScreenReader.currentPackage()
        if (pkg.isBlank()) return "Aplikasi yang sedang aktif belum terdeteksi."
        val label = runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)
        return "Aplikasi yang sedang aktif: $label."
    }

    /** The app's own search link, when it is known. Opens the search result directly, no tapping. */
    private fun searchIntentFor(app: String, query: String): Intent? {
        val a = app.lowercase(Locale.getDefault())
        val q = java.net.URLEncoder.encode(query, "UTF-8")
        return when {
            "spotify" in a -> Intent(Intent.ACTION_VIEW, android.net.Uri.parse("spotify:search:$q"))
                .setPackage("com.spotify.music")
            "youtube music" in a || "ytmusic" in a || "yt music" in a ->
                Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://music.youtube.com/search?q=$q"))
                    .setPackage("com.google.android.apps.youtube.music")
            "youtube" in a -> Intent(Intent.ACTION_VIEW, android.net.Uri.parse("vnd.youtube://results?search_query=$q"))
                .setPackage("com.google.android.youtube")
            // "cari di google X" must show the real Google results page, not a text summary in chat.
            a == "google" || a == "google search" || a == "chrome" || a == "google chrome" ->
                Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/search?q=$q"))
            else -> null
        }
    }

    /** Opens an app, taps its search, types the query, presses enter, and returns what is on screen. */
    suspend fun searchInApp(context: Context, app: String, query: String): String = runSearch(context, app, query).second

    /** True when the requested installed app is already the foreground application. */
    fun isRequestedAppForeground(context: Context, query: String): Boolean = runCatching {
        val foreground = ScreenReader.currentPackage()
        if (foreground.isBlank()) return@runCatching false
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcher, 0)
        val labels = apps.map { it.loadLabel(pm).toString() }
        val index = AppMatcher.match(query, labels) ?: return@runCatching false
        apps.getOrNull(index)?.activityInfo?.packageName == foreground
    }.getOrDefault(false)

    /**
     * Runs the search flow and reports whether the query was actually entered. Each step is checked
     * on its own, so the reply names the step that failed instead of claiming a search happened.
     */
    private suspend fun runSearch(context: Context, app: String, query: String): Pair<Boolean, String> {
        val deep = searchIntentFor(app, query)
        if (deep != null) {
            val launched = runCatching { context.startActivity(deep.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
            if (launched) {
                kotlinx.coroutines.delay(2500)
                return true to "Membuka pencarian \"$query\" di $app. " + readScreenText()
            }
        }
        val alreadyForeground = isRequestedAppForeground(context, app)
        val opened = if (alreadyForeground) {
            Result("Aplikasi $app sudah aktif; melanjutkan pencarian tanpa membuka ulang.")
        } else {
            openApp(context, app)
        }
        if (!alreadyForeground && !opened.message.contains("berhasil dibuka", ignoreCase = true)) {
            return false to opened.message
        }
        // A foreground app only needs a short settle interval. Relaunching it here used to
        // reset its navigation state and made the agent appear to loop at the app launch.
        kotlinx.coroutines.delay(if (alreadyForeground) 250L else 650L)

        val service = com.android.island.service.IslandAccessibilityService
        if (!service.isRunning) {
            return false to "${opened.message} Accessibility Android Island belum aktif, jadi kolom pencarian tidak bisa diisi."
        }

        // The app may be on ANY page (inside a chat, a video, a profile). Reach a screen where the search
        // field or icon is visible first: back / home tab, relaunch only as the last resort.
        val reach = reachSearch(context, app, tapControl = true)
        if (!reach.ok) {
            return false to "${opened.message} ${reach.note} " + readScreenText()
        }

        // The search box may appear only after the search control is tapped, so poll for a few seconds.
        // When reachSearch already tapped the icon, do not tap it again: a second tap can close the box.
        var tappedSearch = reach.tappedControl
        var typed = false
        for (attempt in 0 until 6) {
            kotlinx.coroutines.delay(300)
            if (service.typeIntoSearch(query, allowAny = tappedSearch && attempt >= 2)) { typed = true; break }
            if (!tappedSearch) tappedSearch = service.tapSearchButton(allowBottomBar = true)
        }
        if (!typed) {
            val why = if (tappedSearch) "Kolom pencarian tidak muncul." else "Tombol cari tidak ditemukan di layar."
            return false to "${opened.message} ${reach.note} $why " + readScreenText()
        }

        // Enter through Shizuku first (a real key / finger), accepted only if the screen changed.
        kotlinx.coroutines.delay(300)
        val out = submitEnter()
        val note = if (out.verified) "Mencari \"$query\": " + submitNote(out)
        else "Teks \"$query\" sudah diketik, tetapi " + submitNote(out).replaceFirstChar { it.lowercase() }
        return out.verified to "${opened.message} ${reach.note} $note ".replace("  ", " ") + readScreenText()
    }

    private data class ReachResult(val ok: Boolean, val tappedControl: Boolean, val note: String)

    /**
     * Brings the foreground app to a screen where its search UI is usable. The policy lives in
     * [SearchNavigationPolicy] (unit tested); this only executes each move and re-observes after it.
     * With [tapControl] false it stops as soon as search is visible (used by [appHome]).
     */
    private suspend fun reachSearch(context: Context, app: String, tapControl: Boolean): ReachResult {
        val service = com.android.island.service.IslandAccessibilityService
        val startPackage = ScreenReader.currentPackage()
        val trail = mutableListOf<String>()
        var progress = SearchNavigationPolicy.Progress()
        var tapped = false
        var controlBroken = false
        while (true) {
            kotlinx.coroutines.delay(250)
            val probe = service.probeSearch()
            val inApp = if (app.isNotBlank()) isRequestedAppForeground(context, app)
            else startPackage.isBlank() || ScreenReader.currentPackage() == startPackage
            val seen = SearchNavigationPolicy.Observed(
                inTargetApp = inApp,
                hasSearchField = probe?.hasField == true,
                hasSearchControl = probe?.hasControl == true && !controlBroken,
                hasHomeTab = probe?.hasHomeTab == true,
                keyboardOpen = probe?.keyboardOpen == true
            )
            val move = SearchNavigationPolicy.next(seen, progress)
            when (move) {
                SearchNavigationPolicy.Move.USE_SEARCH_FIELD ->
                    return ReachResult(true, tapped, trail.summary("Kolom pencarian sudah tampil."))
                SearchNavigationPolicy.Move.TAP_SEARCH_CONTROL -> {
                    if (!tapControl) return ReachResult(true, false, trail.summary("Ikon cari terlihat."))
                    if (service.tapSearchButton(allowBottomBar = true)) {
                        tapped = true
                        trail += "mengetuk ikon cari"
                        kotlinx.coroutines.delay(600)
                        return ReachResult(true, true, trail.summary("Ikon cari diketuk."))
                    }
                    // The icon was visible but could not be tapped: count it as an attempt and try another route.
                    trail += "ikon cari tidak bisa diketuk"
                    controlBroken = true
                    progress = progress.copy(attempts = progress.attempts + 1)
                    continue
                }
                SearchNavigationPolicy.Move.TAP_HOME_TAB -> {
                    val ok = service.tapHomeTab()
                    trail += if (ok) "mengetuk tab beranda" else "tab beranda tidak bisa diketuk"
                    kotlinx.coroutines.delay(600)
                }
                SearchNavigationPolicy.Move.PRESS_BACK -> {
                    service.goBack()
                    trail += "kembali"
                    kotlinx.coroutines.delay(500)
                }
                SearchNavigationPolicy.Move.RELAUNCH_APP -> {
                    val relaunched = relaunchToRoot(context, app.ifBlank { startPackage })
                    trail += if (relaunched) "membuka ulang aplikasi dari halaman awal" else "gagal membuka ulang aplikasi"
                    kotlinx.coroutines.delay(1200)
                }
                SearchNavigationPolicy.Move.GIVE_UP ->
                    return ReachResult(false, tapped, trail.summary("Tidak berhasil menemukan kolom atau ikon cari di aplikasi ini."))
            }
            progress = SearchNavigationPolicy.progressAfter(move, progress)
        }
    }

    private fun List<String>.summary(tail: String): String =
        if (isEmpty()) tail else "Langkah navigasi: ${joinToString(", ")}. $tail"

    /** Brings the app's launcher activity to the front with its back stack cleared to the root page. */
    private fun relaunchToRoot(context: Context, appOrPackage: String): Boolean = runCatching {
        val pm = context.packageManager
        val pkg = if (appOrPackage.contains('.')) appOrPackage else {
            val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val apps = pm.queryIntentActivities(launcher, 0)
            val idx = AppMatcher.match(appOrPackage, apps.map { it.loadLabel(pm).toString() }) ?: return@runCatching false
            apps[idx].activityInfo.packageName
        }
        val intent = pm.getLaunchIntentForPackage(pkg) ?: return@runCatching false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    /**
     * Returns an app to its main page so its search icon or field is reachable. [app] blank means the app
     * in front. Never types or searches; it only navigates and reports what is visible afterwards.
     */
    suspend fun appHome(context: Context, app: String): Result {
        val service = com.android.island.service.IslandAccessibilityService
        if (!service.isRunning) {
            return Result("Accessibility Android Island belum aktif, jadi tidak bisa kembali ke halaman utama aplikasi.")
        }
        val name = app.trim()
        if (name.isNotBlank() && !isRequestedAppForeground(context, name)) {
            val opened = openApp(context, name)
            if (!opened.message.contains("berhasil dibuka", ignoreCase = true)) return opened
            kotlinx.coroutines.delay(700)
        }
        val reach = reachSearch(context, name, tapControl = false)
        val label = name.ifBlank { "aplikasi" }
        return if (reach.ok) Result("Halaman utama $label tercapai. ${reach.note} Baca layar untuk memastikan.")
        else Result("Tidak berhasil kembali ke halaman utama $label. ${reach.note}")
    }

    fun back(): Result =
        if (com.android.island.service.IslandAccessibilityService.goBack()) Result("Kembali.")
        else Result("Tidak bisa kembali sekarang.")

    // ---- open app --------------------------------------------------------

    /** Resolves an installed launcher app whose label best matches [query] and opens it. */
    suspend fun openApp(context: Context, query: String): Result {
        val q = query.trim().lowercase(Locale.getDefault())
        if (q.isBlank()) return Result("Aplikasi apa yang mau dibuka?")
        // "buka browser / internet / web" opens the device's default browser app (Chrome/Google).
        if (q in BROWSER_WORDS) {
            return runCatching {
                context.startActivity(
                    Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                Result("Browser perangkat dibuka.")
            }.getOrElse { Result("Browser perangkat gagal dibuka.") }
        }
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = runCatching { pm.queryIntentActivities(launcherIntent, 0) }.getOrDefault(emptyList())
        fun label(i: android.content.pm.ResolveInfo) = i.loadLabel(pm).toString().lowercase(Locale.getDefault())
        val labels = apps.map { it.loadLabel(pm).toString() }
        val idx = AppMatcher.match(query, labels)
            ?: return Result("Aplikasi \u201C$query\u201D tidak ditemukan. Panggil list_apps untuk melihat nama aplikasi yang tersedia, lalu coba lagi dengan nama yang tepat.")
        val match = apps[idx]
        val name = match.loadLabel(pm).toString()
        val pkg = match.activityInfo.packageName
        // Opening an already foreground app is an idempotent no-op. Re-launching its
        // launcher activity during an agent loop can reset the current screen/search state.
        if (ScreenReader.currentPackage() == pkg) {
            return Result("Aplikasi $name sudah aktif; tidak dibuka ulang.")
        }
        return runCatching {
            val intent = pm.getLaunchIntentForPackage(pkg)
                ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                    .setClassName(pkg, match.activityInfo.name)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            // A launch intent being accepted is not proof that the target reached foreground.
            // Poll the accessibility foreground cache for a bounded period; never claim success
            // merely because startActivity returned without throwing.
            var foreground = false
            for (attempt in 0 until 10) {
                kotlinx.coroutines.delay(350L)
                if (ScreenReader.currentPackage() == pkg) {
                    foreground = true
                    break
                }
            }
            if (foreground) Result("Aplikasi $name berhasil dibuka dan terdeteksi berada di depan.")
            else if (!com.android.island.service.IslandAccessibilityService.isRunning) {
                Result("Perintah membuka $name ($pkg) sudah dikirim Android, tetapi belum dapat diverifikasi karena Accessibility Android Island tidak aktif.")
            } else {
                Result("Android menerima perintah membuka $name ($pkg), tetapi aplikasi belum terdeteksi berada di depan setelah menunggu. Status pembukaan belum dapat dipastikan.")
            }
        }.getOrElse { error ->
            val reason = error.message?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName
            Result("Android menolak membuka $name ($pkg). Detail: $reason")
        }
    }

    // Words that map an "open app" request to the built-in browser.
    private val BROWSER_WORDS = setOf("browser", "web browser", "browser island", "internet", "penjelajah web", "web")

    // ---- media / volume ----------------------------------------------------

    fun media(op: AiAction.Media.Op): Result {
        val c = Controls.mediaController
        if (c == null) {
            return Result(
                "Belum ada musik yang jalan. Buka Spotify atau YouTube Music dulu, nanti aku bantu kontrol."
            )
        }
        return runCatching {
            when (op) {
                AiAction.Media.Op.PLAY -> {
                    c.transportControls.play()
                    Result("Oke, diputar.")
                }
                AiAction.Media.Op.PAUSE -> {
                    c.transportControls.pause()
                    Result("Oke, dijeda.")
                }
                AiAction.Media.Op.PLAY_PAUSE -> {
                    Controls.playPause()
                    Result("Oke.")
                }
                AiAction.Media.Op.NEXT -> {
                    c.transportControls.skipToNext()
                    Result("Oke, next.")
                }
                AiAction.Media.Op.PREV -> {
                    c.transportControls.skipToPrevious()
                    Result("Oke, previous.")
                }
            }
        }.getOrElse { Result("Gagal mengontrol media: ${it.message ?: "error"}") }
    }

    
fun volume(context: Context, op: AiAction.Volume.Op): Result {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return Result("AudioManager tidak tersedia.")
        return runCatching {
            when (op) {
                AiAction.Volume.Op.UP -> {
                    am.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    Result("Oke, volume naik.")
                }
                AiAction.Volume.Op.DOWN -> {
                    am.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )
                    Result("Oke, volume turun.")
                }
                AiAction.Volume.Op.MUTE -> {
                    am.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_MUTE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    Result("Oke, di-mute.")
                }
                AiAction.Volume.Op.UNMUTE -> {
                    am.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_UNMUTE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    Result("Oke, suara nyala lagi.")
                }
            }
        }.getOrElse { Result("Gagal mengatur volume.") }
    }

    // ---- toggle setting ----------------------------------------------------

    fun toggle(context: Context, setting: Setting, turnOn: Boolean): Result {
        val verb = if (turnOn) "menyalakan" else "mematikan"
        return when (setting) {
            Setting.FLASHLIGHT -> toggleFlashlight(context, turnOn)
            Setting.DND -> toggleDnd(context, turnOn)
            Setting.WIFI -> shellOrSettings(
                context, "svc wifi ${onOff(turnOn)}", Settings.ACTION_WIFI_SETTINGS, setting, verb
            )
            Setting.BLUETOOTH -> shellOrSettings(
                context, "svc bluetooth ${onOff(turnOn)}", Settings.ACTION_BLUETOOTH_SETTINGS, setting, verb
            )
            Setting.MOBILE_DATA -> shellOrSettings(
                context, "svc data ${onOff(turnOn)}", Settings.ACTION_DATA_ROAMING_SETTINGS, setting, verb
            )
            Setting.AIRPLANE -> shellOrSettings(
                context,
                "settings put global airplane_mode_on ${if (turnOn) 1 else 0} && " +
                    "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $turnOn",
                Settings.ACTION_AIRPLANE_MODE_SETTINGS, setting, verb
            )
            Setting.LOCATION -> shellOrSettings(
                context, "cmd location set-location-enabled $turnOn",
                Settings.ACTION_LOCATION_SOURCE_SETTINGS, setting, verb
            )
            Setting.AUTO_ROTATE -> shellOrSettings(
                context, "settings put system accelerometer_rotation ${if (turnOn) 1 else 0}",
                Settings.ACTION_DISPLAY_SETTINGS, setting, verb
            )
        }
    }

    private fun onOff(turnOn: Boolean) = if (turnOn) "enable" else "disable"

    /** Shizuku shell when granted (instant, no UI); otherwise open the matching Settings screen. */
    private fun shellOrSettings(
        context: Context,
        command: String,
        settingsAction: String,
        setting: Setting,
        verb: String
    ): Result {
        if (ShizukuShell.exec(command)) return Result("Siap, ${setting.label} sudah di$verb.")
        return runCatching {
            context.startActivity(Intent(settingsAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Result("Shizuku belum aktif, jadi membuka pengaturan ${setting.label} untuk diatur manual.")
        }.getOrElse { Result("Tidak bisa mengubah ${setting.label} di perangkat ini.") }
    }

    private fun toggleFlashlight(context: Context, turnOn: Boolean): Result {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return Result("Senter tidak tersedia di perangkat ini.")
        return runCatching {
            val id = manager.cameraIdList.firstOrNull { camId ->
                manager.getCameraCharacteristics(camId)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return Result("Perangkat ini tidak punya senter.")
            manager.setTorchMode(id, turnOn)
            Result(if (turnOn) "Siap, senter dinyalakan." else "Oke, senter dimatikan.")
        }.getOrElse { Result("Gagal mengubah senter.") }
    }

    /** Needs the one-time "Do Not Disturb access" grant; opens that screen the first time. */
    private fun toggleDnd(context: Context, turnOn: Boolean): Result {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return Result("Jangan Ganggu tidak tersedia di perangkat ini.")
        if (!nm.isNotificationPolicyAccessGranted) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            return Result("Butuh izin akses Jangan Ganggu sekali saja \u2014 aktifkan untuk \u201CAndroid Island\u201D lalu coba lagi.")
        }
        return runCatching {
            nm.setInterruptionFilter(
                if (turnOn) NotificationManager.INTERRUPTION_FILTER_PRIORITY
                else NotificationManager.INTERRUPTION_FILTER_ALL
            )
            Result(if (turnOn) "Oke, Jangan Ganggu nyala." else "Oke, Jangan Ganggu mati.")
        }.getOrElse { Result("Gagal mengubah Jangan Ganggu.") }
    }
}
