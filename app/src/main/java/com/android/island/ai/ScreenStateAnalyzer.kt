package com.android.island.ai

/**
 * Turns the numbered element list into a short navigation hint for the model, so it can tell
 * "I am on the app's main page and can search" from "I am deep inside a chat/video and must go
 * back first". Deterministic and cheap: no extra model call. Pure logic, unit tested.
 */
object ScreenStateAnalyzer {
    data class State(
        val searchField: ScreenElements.Element?,
        val searchControl: ScreenElements.Element?,
        val homeTab: ScreenElements.Element?,
        val keyboardLikely: Boolean
    ) {
        val searchReachable: Boolean get() = searchField != null || searchControl != null
    }

    fun analyze(elements: List<ScreenElements.Element>): State {
        val field = elements.firstOrNull { it.editable && (SearchControlMatcher.isSearchControl(it.label) || it.focused) }
        val control = elements.firstOrNull { !it.editable && SearchControlMatcher.isSearchControl(it.label) }
        val home = elements.firstOrNull { !it.editable && SearchControlMatcher.isHomeTab(it.label) }
        return State(field, control, home, keyboardLikely = elements.any { it.editable && it.focused })
    }

    fun hint(elements: List<ScreenElements.Element>): String {
        if (elements.isEmpty()) return ""
        val s = analyze(elements)
        return when {
            s.searchField != null ->
                "Petunjuk navigasi: kolom pencarian sudah tampil (elemen ${s.searchField.index}). Isi dengan type lalu press_enter."
            s.searchControl != null ->
                "Petunjuk navigasi: kontrol cari ada di elemen ${s.searchControl.index} (\"${s.searchControl.label.take(30)}\"). " +
                    "Ketuk dulu dengan click_index, tunggu kolom teks fokus, baru type lalu press_enter."
            else ->
                "Petunjuk navigasi: TIDAK ada kolom atau ikon cari di layar ini, jadi kemungkinan kamu berada di halaman dalam " +
                    "(chat, video, profil, pengaturan). Gunakan app_home untuk kembali ke halaman utama aplikasi, lalu cari ikon atau kolom cari." +
                    (s.homeTab?.let { " Tab beranda ada di elemen ${it.index}." } ?: "")
        }
    }
}
