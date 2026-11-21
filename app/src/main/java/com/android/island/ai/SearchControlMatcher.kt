package com.android.island.ai

import java.util.Locale

/**
 * Decides, from labels only, whether an on-screen control is the app's search entry point.
 * Pure logic (no Android types) so it is unit tested. The accessibility service and
 * [ScreenStateAnalyzer] both use it, so "what counts as a search control" is defined once.
 *
 * Real apps label their magnifier icon in many ways: "Cari", "Search", "Telusuri", "Pencarian",
 * "Search Button", "Cari kontak", a view id such as `menuitem_search`, or only a hint like
 * "Search or type URL". Related-search chips ("Pencarian · ...") and voice/lens/history controls
 * are NOT the search entry and are rejected.
 */
object SearchControlMatcher {
    private val SEARCH_WORDS = listOf("cari", "search", "telusuri", "pencarian", "magnif", "kaca pembesar", "find")

    /** Words that mean "something near search, but not the search entry". Checked on label and id. */
    private val NOISE = listOf(
        "suara", "voice", "mic", "lens", "kamera", "camera", "gambar", "image", "riwayat", "history",
        "hapus", "clear", "tutup", "close", "batal", "cancel", "filter", "saran", "suggest",
        "terkini", "trending", "populer", "result", "hasil", "recycler", "list"
    )

    private val EXACT = setOf(
        "cari", "search", "telusuri", "pencarian", "find", "search button", "tombol cari",
        "tombol pencarian", "cari dan jelajahi", "search and explore"
    )

    private val HOME_TABS = setOf(
        "beranda", "home", "halaman utama", "utama", "chat", "chats", "obrolan", "for you", "untuk anda"
    )

    private fun lower(s: String) = s.trim().lowercase(Locale.ROOT)

    /** Related-search suggestions that feeds show before the real results. Never the main control. */
    fun isSuggestionChip(label: String): Boolean {
        val l = lower(label)
        return l.startsWith("pencarian ·") || l.startsWith("search ·") || l.startsWith("cari ·") ||
            l.contains("related search") || l.contains("pencarian terkait")
    }

    /**
     * @param text visible text, @param description content description, @param hint field hint,
     * @param viewId resource name such as `com.whatsapp:id/menuitem_search` (package prefix is ignored).
     */
    fun isSearchControl(text: String, description: String = "", hint: String = "", viewId: String = ""): Boolean {
        // ScreenReader labels an unlabelled control as "id:<view id>"; that is an id, not visible text.
        val idFromLabel = text.trim().takeIf { it.startsWith("id:") }.orEmpty()
        val label = lower(if (idFromLabel.isNotEmpty()) description else text.ifBlank { description })
        val h = lower(hint)
        val id = lower((viewId.ifBlank { idFromLabel }).substringAfter('/').removePrefix("id:"))
        if (label.isBlank() && h.isBlank() && id.isBlank()) return false
        if (isSuggestionChip(label)) return false
        val noisy = NOISE.any { label.contains(it) || id.contains(it) }
        if (noisy) return false
        val idHit = id.isNotBlank() && SEARCH_WORDS.any { id.contains(it) }
        val hintHit = h.isNotBlank() && SEARCH_WORDS.any { h.contains(it) }
        val exact = label in EXACT
        // "Cari kontak", "Search or type URL", "Telusuri atau ketik URL": short labels that START with the word.
        val shortLead = label.length in 3..32 && SEARCH_WORDS.any { label.startsWith(it) }
        return idHit || hintHit || exact || shortLead
    }

    /** Bottom-bar or top-tab entry that returns to the app's main page. */
    fun isHomeTab(label: String): Boolean =
        // Tabs are often announced as "Chat, tab 1 dari 4"; only the part before the comma names the tab.
        lower(label).substringBefore(',').trim() in HOME_TABS

    /**
     * STAGE 72: where the search entry point lives in well-known apps. The user asked the agent to
     * "cari dulu di internet biasanya letak ikon pencarian yang dibutuhkan ada di mana" before
     * tapping anything. Shipping the common layouts as built-in knowledge gives every provider the
     * same prior instantly, offline, without an extra network round-trip. These are stable, widely
     * documented UI facts, not guesses.
     */
    private val APP_SEARCH_KNOWLEDGE: Map<String, String> = mapOf(
        "tiktok" to "TikTok: bar bawah TIDAK punya tab Cari (isinya Beranda, Toko, +, Kotak Masuk, Profil). " +
            "Dari Beranda (For You), cari dijangkau lewat halaman profil: ketuk tab Profil di kanan bawah, lalu ikon kaca pembesar di KANAN ATAS. " +
            "Label 'Cari' di daftar saran/hasil adalah saran, bukan tombol utama.",
        "instagram" to "Instagram: ikon kaca pembesar ada di BAR BAWAH (tab Jelajahi, urutan kedua dari kiri); setelah diketuk, kolom pencarian muncul di atas. Jangan tekan label 'Cari' di dalam feed.",
        "youtube" to "YouTube: ikon kaca pembesar di KANAN ATAS app bar. Di halaman hasil, kolom cari sudah berisi kueri — jangan tekan ulang.",
        "whatsapp" to "WhatsApp: kolom pencarian di bawah header daftar chat (ikon kaca pembesar di action bar atas). Cari di dalam chat bukan entry pencarian utama.",
        "telegram" to "Telegram: ikon kaca pembesar di KANAN ATAS daftar chat.",
        "facebook" to "Facebook: ikon kaca pembesar di BAR ATAS (kiri/kiri-tengah); halaman utama memuatnya di paling atas.",
        "x" to "X (Twitter): tab kaca pembesar di BAR BAWAH (Telusuri); kolom cari di atas halaman itu.",
        "twitter" to "X (Twitter): tab kaca pembesar di BAR BAWAH (Telusuri); kolom cari di atas halaman itu.",
        "shopee" to "Shopee: BAR PENCARIAN besar di tengah atas halaman utama (ada hint 'cari di Shopee'); ketuk barnya langsung.",
        "tokopedia" to "Tokopedia: BAR PENCARIAN di tengah atas halaman utama; ketuk barnya langsung.",
        "lazada" to "Lazada: BAR PENCARIAN di tengah atas halaman utama.",
        "spotify" to "Spotify: tab Telusuri/Cari di BAR BAWAH; kolom 'Apa yang ingin kamu dengarkan' muncul di atas.",
        "play store" to "Play Store: BAR PENCARIAN di paling atas halaman utama.",
        "gmail" to "Gmail: BAR PENCARIAN di atas (ikon kaca pembesar kiri-tengah atas)."
    )

    /**
     * STAGE 72: returns the search-location knowledge block for the app named in a user command
     * like "buka tiktok lalu cari preset", or null when the app is unknown. Injected as an extra
     * system message so the model checks WHERE the real search affordance is BEFORE tapping, and
     * never confuses a suggestion chip, a bottom-nav tab, or a differently-scoped search button
     * with the entry point the task needs.
     */
    fun searchKnowledgeForUserText(userText: String): String? {
        val text = lower(userText)
        // "buka tiktok", "di tiktok", "di dalam tiktok" — take the token after buka/di.
        val m = Regex("(?:buka(?:kan|in)?|open|di|dari)\\s+((?:aplikasi\\s+)?[a-z0-9._-]{3,24})")
            .find(text) ?: return null
        val app = m.groupValues[1]
            .replace(Regex("^aplikasi\\s+"), "")
            .trim()
        if (app.isBlank()) return null
        val hit = APP_SEARCH_KNOWLEDGE.entries.firstOrNull { app.contains(it.key) || it.key.contains(app) }
            ?: return null
        return "PENGETAHUAN LOKASI PENCARIAN ${hit.key.uppercase(Locale.ROOT)}: ${hit.value} " +
            "Gunakan ini untuk memilih kandidat yang benar; verifikasi layar setelah mengetuk dan " +
            "jangan mengulang ketukan pada target yang sama."
    }
}
