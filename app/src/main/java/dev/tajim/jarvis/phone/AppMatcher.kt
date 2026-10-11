package dev.tajim.jarvis.phone

/** Picks the installed app whose label best matches what the user said. */
object AppMatcher {
    private val aliases = mapOf(
        "ইউটিউব" to "youtube", "হোয়াটসঅ্যাপ" to "whatsapp", "ফেসবুক" to "facebook", "ক্রোম" to "chrome",
        "ক্যামেরা" to "camera", "সেটিংস" to "settings", "গ্যালারি" to "gallery", "টিকটক" to "tiktok",
        "মেসেঞ্জার" to "messenger", "জিমেইল" to "gmail", "ম্যাপস" to "maps", "ক্যালকুলেটর" to "calculator",
    )

    private fun norm(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

    fun best(spoken: String, apps: List<Pair<String, String>>): String? {
        val q = norm(aliases[spoken.trim()] ?: spoken)
        if (q.isEmpty()) return null
        apps.firstOrNull { norm(it.first) == q }?.let { return it.second }
        apps.firstOrNull { norm(it.first).startsWith(q) }?.let { return it.second }
        apps.firstOrNull { norm(it.first).contains(q) }?.let { return it.second }
        return apps.firstOrNull { norm(it.first).length >= 3 && q.contains(norm(it.first)) }?.second
    }
}
