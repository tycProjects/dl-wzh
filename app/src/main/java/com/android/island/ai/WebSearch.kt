package com.android.island.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

/** Free-first web lookup for AI actions: optional Brave context, then DuckDuckGo Lite. */
object WebSearch {
    data class Result(val query: String, val summary: String)

    @Volatile private var braveApiKey: String = ""

    /** Credentials are supplied by encrypted Android Keystore-backed settings. */
    fun setBraveApiKey(value: String) { braveApiKey = value.trim() }

    fun search(query: String, limit: Int = 5): Result {
        val q = query.trim()
        if (q.isBlank()) return Result(q, "Query kosong.")
        val key = braveApiKey
        if (key.isNotBlank()) return braveContext(q, key)
        NoozraNews.topicSearch(q, limit)?.let { return Result(q, "PENCARIAN WEB (judul & sumber penerbit):\n$it") }
        val fallback = runCatching { liteResults(q, limit) }.getOrNull().orEmpty()
        return Result(q, if (fallback.isNotBlank()) {
            "PENCARIAN ALTERNATIF TERBATAS (DuckDuckGo Lite; belum setara pencarian web terverifikasi):\n$fallback\n\nGunakan judul ini hanya untuk menemukan sumber. Jangan menyimpulkan fakta tanpa membuka dan memeriksa sumber aslinya."
        } else "Pencarian web utama belum dikonfigurasi. Hasil alternatif juga tidak tersedia; jangan menjawab seolah-olah sudah melakukan pencarian web.")
    }

    /**
     * Grounded news fetch that returns BOTH outputs from one network pass:
     * [NewsBundle.articles] feeds the deterministic NewsFormatter user list, [NewsBundle.summary]
     * stays available for the model path. Topic-named queries use Google News RSS by topic first;
     * generic requests use the verified publisher feeds (ANTARA, CNN Indonesia, Detikcom, CNA).
     */
    internal fun searchNews(query: String, limit: Int = 12): NewsBundle {
        val q = query.trim()
        if (q.isBlank()) return NewsBundle(q, emptyList(), "Query kosong.")
        // A named topic must be searched by topic; the latest-headline feeds almost never match a name.
        NoozraNews.topicSearchArticles(q, limit)?.let { articles ->
            return NewsBundle(q, articles, NoozraNews.topicSearch(q, limit).orEmpty())
        }
        val result = NoozraNews.latestWithFailures(q, limit)
        val summary = if (result.all.isEmpty()) NoozraNews.latest(q, limit)
        else buildString {
            appendLine("BERITA TERBARU")
            appendLine("Sampel berita terverifikasi dari feed penerbit; rentang publikasi maksimal 7 hari terakhir.")
            if (result.indonesia.isNotEmpty()) {
                appendLine("\nBERITA INDONESIA")
                result.indonesia.forEachIndexed { index, article ->
                    appendLine("${index + 1}. ${article.title}")
                    appendLine("   ${article.source} • ${article.published}")
                    if (article.description.isNotBlank()) appendLine("   ${article.description}")
                    appendLine("   Sumber: ${article.url}")
                }
            }
            if (result.international.isNotEmpty()) {
                appendLine("\nBERITA INTERNASIONAL")
                result.international.forEachIndexed { index, article ->
                    appendLine("${index + 1}. ${article.title}")
                    appendLine("   ${article.source} • ${article.published}")
                    if (article.description.isNotBlank()) appendLine("   ${article.description}")
                    appendLine("   Sumber: ${article.url}")
                }
            }
            append("Catatan cakupan: ini sampel dari feed yang berhasil diperiksa, bukan seluruh berita.")
        }
        return NewsBundle(q, result.all, summary, result.failures)
    }

    /** Search plus extracted context. Brave already supplies relevant page passages, so no scrape is needed. */
    fun deepSearch(query: String, pages: Int = 3): Result {
        val q = query.trim()
        if (q.isBlank()) return Result(q, "Query kosong.")
        val key = braveApiKey
        // Topic news ("berita tentang gibran terbaru") is searched by topic first. The old path only
        // filtered the latest headlines of a few feeds, so a specific name almost never matched.
        if (isCurrentNewsQuery(q)) {
            NoozraNews.topicSearch(q)?.let { return Result(q, "PENCARIAN BERITA BERBASIS SUMBER PENERBIT.\n$it") }
        }
        if (key.isNotBlank()) return braveContext(q, key)
        // Generic news request with no topic: publisher RSS of latest headlines.
        if (isCurrentNewsQuery(q)) return Result(q,
            "PENCARIAN BERITA BERBASIS RSS PENERBIT.\n${NoozraNews.latest(q)}\n\nFeed RSS adalah sumber judul/tanggal asli; jika feed tidak memuat berita relevan atau tanggal yang jelas, jangan mengarang berita.")

        val html = runCatching { httpGetLite("https://lite.duckduckgo.com/lite/?q=${enc(q)}") }.getOrNull().orEmpty()
        val hits = uniqueHits(parseHits(html)).take(pages.coerceIn(1, 5))
        if (hits.isEmpty()) {
            NoozraNews.topicSearch(q, 8)?.let { return Result(q, "PENCARIAN WEB (judul & sumber penerbit):\n$it") }
            return Result(q, "PENCARIAN ALTERNATIF TERBATAS: tidak ada hasil yang dapat diverifikasi. Jangan menjawab dari ingatan seolah-olah hasil web ditemukan.")
        }
        val blocks = hits.mapIndexed { i, h ->
            "${i + 1}. ${h.title}\n   URL sumber: ${h.url}\n   Isi dan tanggal halaman belum diverifikasi. Judul bukan bukti fakta."
        }
        return Result(q, "PENCARIAN ALTERNATIF TERBATAS (DuckDuckGo Lite hanya memberi tautan; bukan konteks halaman terverifikasi):\n" +
            blocks.joinToString("\n\n").take(3500) +
            "\n\nJangan menyimpulkan isi artikel atau tanggal publikasi dari judul. Jangan menganggap fallback ini lengkap.")
    }

    private fun braveContext(query: String, key: String): Result {
        val q = query.take(600).trim().split(Regex("\\s+")).take(75).joinToString(" ")
        val freshness = when {
            listOf("hari ini", "today", "breaking", "sekarang", "saat ini").any { it in q.lowercase(Locale.ROOT) } -> "pd"
            isCurrentNewsQuery(q) -> "pw"
            else -> ""
        }
        val params = linkedMapOf(
            "q" to q,
            "maximum_number_of_tokens" to "4096",
            "maximum_number_of_urls" to "8",
            "context_threshold_mode" to "strict",
            "safesearch" to "moderate",
            "enable_source_metadata" to "true"
        ).apply { if (freshness.isNotBlank()) put("freshness", freshness) }
        val url = "https://api.search.brave.com/res/v1/llm/context?" + params.entries.joinToString("&") {
            "${enc(it.key)}=${enc(it.value)}"
        }
        val response = requestBrave(url, key) ?: return Result(query,
            "Pencarian Brave gagal dihubungi atau responsnya tidak valid. Jangan mengklaim sudah menemukan sumber; periksa koneksi dan konfigurasi API key.")
        if (response.status !in 200..299) {
            val retry = response.retryAfter?.let { " Coba lagi setelah $it detik." }.orEmpty()
            val reason = when (response.status) {
                401, 403 -> "API key ditolak atau akses layanan tidak aktif."
                429 -> "Batas permintaan Brave tercapai."
                in 500..599 -> "Layanan Brave sedang mengalami gangguan."
                else -> "Brave mengembalikan HTTP ${response.status}."
            }
            return Result(query, "$reason$retry Tidak ada hasil yang boleh dianggap terverifikasi dari permintaan ini.")
        }
        val parsed = runCatching { parseBraveResponse(response.body) }.getOrNull()
            ?: return Result(query, "Respons Brave tidak sesuai format yang diharapkan. Hasil diblokir agar AI tidak mengarang data.")
        if (parsed.isBlank()) return Result(query,
            "Brave tidak menemukan konteks web yang relevan. Jangan menjawab seolah-olah pencarian menemukan bukti.")
        val receivedAt = java.time.Instant.now().toString()
        return Result(query, "Waktu hasil diterima Android Island (UTC): $receivedAt\n" + parsed)
    }

    internal fun parseBraveResponse(body: String): String? = runCatching {
        val root = JSONObject(body)
        val grounding = root.optJSONObject("grounding") ?: return@runCatching null
        val generic = grounding.optJSONArray("generic") ?: JSONArray()
        if (generic.length() == 0) return@runCatching ""
        val sources = root.optJSONObject("sources") ?: JSONObject()
        val blocks = mutableListOf<String>()
        for (i in 0 until generic.length()) {
            val item = generic.optJSONObject(i) ?: continue
            val rawUrl = item.optString("url").trim()
            val safeUrl = safeSourceUrl(rawUrl) ?: continue
            val title = item.optString("title").trim().ifBlank { safeUrl }
            val snippets = item.optJSONArray("snippets") ?: JSONArray()
            val content = (0 until snippets.length()).mapNotNull { snippets.optString(it).trim().takeIf(String::isNotBlank) }
                .joinToString("\n")
            if (content.isBlank()) continue
            val meta = sources.optJSONObject(rawUrl) ?: JSONObject()
            val age = meta.optJSONArray("age")
            val date = age?.let { arr ->
                (0 until arr.length()).map { arr.optString(it).trim() }.firstOrNull { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
                    ?: (0 until arr.length()).map { arr.optString(it).trim() }.firstOrNull { it.isNotBlank() }
            }
            val fetched = meta.optLong("fetched_content_timestamp", 0L).takeIf { it > 0L }
            val metadata = buildList {
                date?.let { add("Tanggal sumber menurut metadata Brave: $it") }
                fetched?.let { add("Waktu pengambilan indeks Brave (Unix): $it") }
            }.joinToString("; ")
            blocks += "${blocks.size + 1}. $title\nURL sumber: $safeUrl" +
                (if (metadata.isNotBlank()) "\n$metadata" else "\nTanggal sumber tidak tersedia dalam metadata Brave") +
                "\nIsi relevan:\n$content"
        }
        blocks.joinToString("\n\n").take(18_000)
    }.getOrNull()

    internal fun safeSourceUrl(value: String): String? = runCatching {
        val uri = java.net.URI(value.trim())
        if (!uri.scheme.equals("https", true) || uri.host.isNullOrBlank() || uri.userInfo != null ||
            uri.host.equals("localhost", true) || uri.host.endsWith(".local", true)) null
        else uri.toASCIIString().takeIf { it.length <= 2048 }
    }.getOrNull()

    internal fun isCurrentNewsQuery(query: String): Boolean {
        val q = query.lowercase(Locale.ROOT)
        return listOf("berita", "terbaru", "terkini", "hari ini", "minggu ini", "today", "latest news", "breaking news")
            .any { it in q }
    }

    internal data class Hit(val title: String, val url: String)
    internal data class HttpResult(val status: Int, val body: String, val retryAfter: String?)

    /** Grounded news bundle shared by the clean user list (NewsFormatter) and the model summary. */
    internal data class NewsBundle(
        val query: String,
        val articles: List<NoozraNews.Article>,
        val summary: String,
        val failedFeeds: List<String> = emptyList()
    )

    internal fun uniqueHits(hits: List<Hit>): List<Hit> {
        val seenTitles = HashSet<String>()
        val seenUrls = HashSet<String>()
        return hits.filter { hit ->
            val titleKey = normalizeTitle(hit.title)
            val urlKey = canonicalUrl(hit.url)
            if (titleKey.isBlank() || titleKey in seenTitles) return@filter false
            if (urlKey.isNotBlank() && urlKey in seenUrls) return@filter false
            seenTitles.add(titleKey)
            if (urlKey.isNotBlank()) seenUrls.add(urlKey)
            true
        }
    }

    internal fun canonicalUrl(value: String): String = runCatching {
        val uri = java.net.URI(value.trim()).normalize()
        val host = uri.host?.lowercase(Locale.ROOT) ?: return@runCatching ""
        val tracking = setOf("fbclid", "gclid", "dclid", "mc_cid", "mc_eid", "ref")
        val query = uri.rawQuery.orEmpty().split('&').filter { it.isNotBlank() }.filterNot { part ->
            val key = part.substringBefore('=').lowercase(Locale.ROOT)
            key.startsWith("utm_") || key in tracking
        }.sorted().joinToString("&")
        val path = (uri.rawPath ?: "").trimEnd('/').ifBlank { "/" }
        buildString { append(host).append(path); if (query.isNotBlank()) append('?').append(query) }
    }.getOrDefault("")

    private fun liteResults(query: String, limit: Int): String {
        val html = httpGetLite("https://lite.duckduckgo.com/lite/?q=${enc(query)}") ?: return ""
        val re = Regex("""<a[^>]+class=['\"]result-link['\"][^>]*>(.*?)</a>""", RegexOption.IGNORE_CASE)
        return re.findAll(html).map { it.groupValues[1].replace(Regex("<[^>]+>"), " ").trim() }
            .filter { it.isNotBlank() }.take(limit.coerceIn(1, 10)).mapIndexed { i, t -> "${i + 1}. $t" }
            .joinToString("\n").take(2500)
    }

    private fun parseHits(html: String): List<Hit> {
        val anchor = Regex("<a([^>]*class=['\"]result-link['\"][^>]*)>(.*?)</a>", RegexOption.IGNORE_CASE)
        val href = Regex("href=['\"]([^'\"]+)['\"]", RegexOption.IGNORE_CASE)
        val out = mutableListOf<Hit>()
        for (m in anchor.findAll(html)) {
            val raw = href.find(m.groupValues[1])?.groupValues?.get(1) ?: continue
            val title = m.groupValues[2].replace(Regex("<[^>]+>"), " ").trim()
            val target = if (raw.startsWith("//")) "https:$raw" else raw
            val url = if (target.contains("uddg=")) URLDecoderSafe.decode(target.substringAfter("uddg=").substringBefore('&')) else target
            val safe = safeSourceUrl(url) ?: continue
            if (title.isNotBlank()) out += Hit(title, safe)
        }
        return out
    }

    private object URLDecoderSafe {
        fun decode(value: String): String = runCatching { java.net.URLDecoder.decode(value, StandardCharsets.UTF_8.name()) }.getOrDefault("")
    }

    private fun requestBrave(url: String, key: String): HttpResult? = runCatching {
        val parsed = URL(url)
        require(parsed.protocol == "https" && parsed.host == "api.search.brave.com")
        val conn = (parsed.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 15_000
            requestMethod = "GET"
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Encoding", "identity")
            setRequestProperty("X-Subscription-Token", key)
            setRequestProperty("User-Agent", "AndroidIsland/1.15.5")
        }
        try {
            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_RESPONSE_BYTES) throw IllegalStateException("Respons Brave melebihi batas ukuran")
                    out.write(buffer, 0, read)
                }
                out.toString("UTF-8")
            }.orEmpty()
            HttpResult(status, body, conn.getHeaderField("Retry-After"))
        } finally { conn.disconnect() }
    }.getOrNull()

    private fun httpGetLite(url: String): String? = runCatching {
        val parsed = URL(url)
        require(parsed.protocol == "https" && parsed.host in setOf("lite.duckduckgo.com"))
        val conn = (parsed.openConnection() as HttpURLConnection).apply {
            connectTimeout = 7_000; readTimeout = 8_000; requestMethod = "GET"
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", "AndroidIsland/1.15.5")
        }
        try {
            if (conn.responseCode !in 200..299) return@runCatching null
            conn.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192); var total = 0
                while (true) { val n = input.read(buffer); if (n < 0) break; total += n; if (total > MAX_RESPONSE_BYTES) return@runCatching null; out.write(buffer, 0, n) }
                out.toString("UTF-8")
            }
        } finally { conn.disconnect() }
    }.getOrNull()

    private fun normalizeTitle(value: String): String = value.lowercase(Locale.ROOT)
        .replace(Regex("&(?:amp|quot|#39);"), " ").replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim().replace(Regex("\\s+"), " ")

    /**
     * Multi-round deep research for "cari lebih dalam": the base search, then one or two sharpened
     * variant rounds, then full excerpts read directly from the strongest source pages. Every
     * section keeps its source labels, and the merged result is size-capped before it reaches the
     * model as one observation, so the follow-up answer can synthesize across rounds.
     */
    fun deepResearch(query: String): Result {
        val q = query.trim()
        if (q.isBlank()) return Result(q, "Query kosong.")
        val sections = mutableListOf<String>()
        sections += "RONDE 1 — pencarian utama:\n${deepSearch(q).summary}"
        var round = 2
        for (variant in ResearchPlanner.variants(q).take(2)) {
            if (SearchQueryPolicy.key(variant) == SearchQueryPolicy.key(q)) continue
            val found = deepSearch(variant)
            if (found.summary.isNotBlank()) {
                sections += "RONDE $round — kata kunci varian \"$variant\":\n${found.summary}"
                round++
            }
        }
        val pageReads = readTopSources(q)
        if (pageReads.isNotBlank()) {
            sections += "PEMBACAAN LANGSUNG HALAMAN SUMBER (kutipan isi halaman, bukan judul):\n$pageReads"
        }
        return Result(q, sections.joinToString("\n\n").take(20_000) +
            "\n\nGunakan gabungan ronde pencarian dan kutipan halaman ini. Sebut sumber per temuan; jangan menggabungkan sumber yang bertentangan tanpa menandai perbedaannya.")
    }

    /** Reads up to [ResearchPlanner.RESEARCH_MAX_PAGES] source pages found by the free fallback path. */
    private fun readTopSources(query: String): String {
        val html = runCatching { httpGetLite("https://lite.duckduckgo.com/lite/?q=${enc(query)}") }.getOrNull().orEmpty()
        if (html.isBlank()) return ""
        val seen = HashSet<String>()
        val candidates = uniqueHits(parseHits(html)).filter { hit ->
            val key = canonicalUrl(hit.url)
            key.isNotBlank() && seen.add(key)
        }.take(ResearchPlanner.RESEARCH_MAX_PAGES)
        if (candidates.isEmpty()) return ""
        val blocks = mutableListOf<String>()
        for (hit in candidates) {
            val page = runCatching { PageReader.read(hit.url, 1100) }.getOrNull() ?: continue
            blocks += buildString {
                append("${blocks.size + 1}. ${hit.title}\n   URL sumber: ${hit.url}")
                page.publishedAt?.let { append("\n   Tanggal publikasi menurut halaman: $it") }
                append("\n   Kutipan isi: ${page.excerpt}")
            }
        }
        return blocks.joinToString("\n\n")
    }

    private fun enc(s: String) = URLEncoder.encode(s, StandardCharsets.UTF_8.name())
    private const val MAX_RESPONSE_BYTES = 1_500_000
}
