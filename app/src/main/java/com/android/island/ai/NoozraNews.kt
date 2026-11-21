package com.android.island.ai

import java.net.HttpURLConnection
import java.net.URL
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Current-news source backed by Noozra's documented public per-category RSS feeds.
 * RSS is the keyless path; the JSON API is deliberately not called because it requires
 * a user API key and its terms limit the free key to personal/non-commercial use.
 * We only pass publisher-provided headline, publisher, canonical URL, date and description
 * to the model. Noozra itself says the publisher remains the source of record.
 */
internal object NoozraNews {
    private const val BASE = "https://noozra.com/rss/"
    // Feeds are fetched in parallel, so one slow publisher must not hold the whole answer: short timeouts
    // plus a hard deadline for the whole batch.
    private const val CONNECT_TIMEOUT_MS = 4_000
    private const val READ_TIMEOUT_MS = 5_000
    private const val BATCH_DEADLINE_MS = 7_000L
    private const val CACHE_TTL_MS = 90_000L
    private const val CACHE_MAX = 10
    private const val MAX_FEED_CHARS = 180_000
    private const val MAX_AGE_DAYS = 7L
    private val activeConnections = java.util.concurrent.ConcurrentHashMap.newKeySet<HttpURLConnection>()

    /** Four daemon workers that exit after 20 s idle, so nothing stays resident between news requests. */
    private val pool = java.util.concurrent.ThreadPoolExecutor(
        4, 4, 20L, java.util.concurrent.TimeUnit.SECONDS, java.util.concurrent.LinkedBlockingQueue<Runnable>()
    ) { r -> Thread(r, "news-feed").apply { isDaemon = true } }.apply { allowCoreThreadTimeOut(true) }

    /** Parsed feeds for [CACHE_TTL_MS]: a repeated ask, or the topic + summary double read, costs no network. */
    private val feedCache = object : LinkedHashMap<String, Pair<Long, List<Article>>>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Long, List<Article>>>?): Boolean = size > CACHE_MAX
    }

    fun trimCache() = synchronized(feedCache) { feedCache.clear() }

    /** Cached or freshly fetched and parsed feed; null when the download failed or was empty. */
    private fun fetchParsed(url: String): List<Article>? {
        synchronized(feedCache) {
            feedCache[url]?.let { (at, items) -> if (System.currentTimeMillis() - at < CACHE_TTL_MS) return items }
        }
        val xml = runCatching { fetch(url) }.getOrNull()
        if (xml.isNullOrBlank()) return null
        val parsed = parseFeed(xml)
        synchronized(feedCache) { feedCache[url] = System.currentTimeMillis() to parsed }
        return parsed
    }

    /** Fetches every url at once. The result keeps the input order; a failed or late feed is null. */
    private fun fetchAllParsed(urls: List<String>): List<List<Article>?> {
        if (urls.isEmpty()) return emptyList()
        val futures = runCatching {
            pool.invokeAll(urls.map { u -> java.util.concurrent.Callable { fetchParsed(u) } },
                BATCH_DEADLINE_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
        }.getOrNull() ?: return urls.map { null }
        return futures.map { f -> if (f.isCancelled) null else runCatching { f.get() }.getOrNull() }
    }

    /** Disconnects an in-flight RSS read when its chat request is stopped or cleared. */
    fun cancelActiveRequests() {
        activeConnections.toList().forEach { connection -> runCatching { connection.disconnect() } }
    }

    internal data class Article(
        val title: String,
        val url: String,
        val source: String,
        val published: String,
        val publishedEpoch: Long,
        val description: String
    )

    /** Structured latest-news result kept sectioned so both outputs render identically. */
    data class LatestResult(
        val indonesia: List<Article>,
        val international: List<Article>,
        val failures: List<String>
    ) {
        val all: List<Article> get() = indonesia + international
    }

    /**
     * Structured latest-news fetch: the cleaned articles plus the feed names that failed.
     * Used both by [latest] (model summary) and by the deterministic user list via NewsFormatter,
     * so both paths share one network pass and identical filtering rules.
     */
    fun latestWithFailures(query: String, limit: Int = 15): LatestResult {
        val categories = categoriesFor(query)
        val fetchedIndonesia = mutableListOf<Article>()
        val fetchedWorld = mutableListOf<Article>()
        val failures = mutableListOf<String>()
        // Verified publisher RSS endpoints in Indonesian; keep these separate and first.
        val indonesiaFeeds = listOf(
            "ANTARA" to "https://www.antaranews.com/rss/terkini.xml",
            "CNN Indonesia" to "https://www.cnnindonesia.com/rss/",
            "Detikcom" to "https://news.detik.com/berita/rss",
            "CNA Indonesia" to "https://www.cna.id/api/v1/rss-outbound-feed?_format=xml"
        )
        val fetched = fetchAllParsed(indonesiaFeeds.map { it.second } + categories.map { BASE + it + ".xml" })
        indonesiaFeeds.forEachIndexed { i, (publisher, _) ->
            val parsed = fetched.getOrNull(i)
            if (parsed == null) failures += publisher else {
                val items = parsed.map { if (it.source.isBlank()) it.copy(source = publisher) else it }
                if (items.isEmpty()) failures += "$publisher (format/tanggal tidak terbaca)"
                fetchedIndonesia += items
            }
        }
        categories.forEachIndexed { j, category ->
            val parsed = fetched.getOrNull(indonesiaFeeds.size + j)
            if (parsed == null) failures += category else fetchedWorld += parsed
        }

        val now = System.currentTimeMillis()
        val cutoff = now - MAX_AGE_DAYS * 24L * 60L * 60L * 1000L
        val terms = queryTerms(query)
        fun valid(items: List<Article>) = items.asSequence()
            .filter { it.publishedEpoch in cutoff..(now + 5 * 60 * 1000L) }
            .filter { article -> terms.isEmpty() || terms.any { term ->
                article.title.contains(term, ignoreCase = true) || article.description.contains(term, ignoreCase = true)
            } }
            .distinctBy { it.url.ifBlank { normalize(articleKey(it.title)) } }
            .sortedByDescending { it.publishedEpoch }
            .toList()
        // A generic request for Indonesian news must not be filtered by incidental query words.
        // Keep Indonesian publisher items first; topical filters only apply when the user names a topic.
        val isBroadIndonesia = query.lowercase(Locale.ROOT).let { q ->
            (q.contains("indonesia") || q.contains("nasional")) && queryTerms(query).isEmpty()
        }
        val indonesiaCandidates = if (isBroadIndonesia) fetchedIndonesia.asSequence()
            .filter { it.publishedEpoch in cutoff..(now + 5 * 60 * 1000L) }
            .distinctBy { it.url.ifBlank { normalize(articleKey(it.title)) } }
            .sortedByDescending { it.publishedEpoch }.toList()
        else valid(fetchedIndonesia)
        val indonesia = indonesiaCandidates.take((limit.coerceIn(1, 30) * 2 / 3).coerceAtLeast(5))
        val international = valid(fetchedWorld).take((limit.coerceIn(1, 30) - indonesia.size).coerceAtLeast(0))
        return LatestResult(indonesia, international, failures)
    }

    /** Fetch a small, relevant set of feeds; no generic model-generated headlines are allowed. */
    fun latest(query: String, limit: Int = 15): String {
        val result = latestWithFailures(query, limit)
        val failures = result.failures

        if (result.all.isEmpty()) {
            val failureText = if (failures.isEmpty()) "RSS berhasil diperiksa, tetapi tidak ada artikel dengan tanggal valid dalam 7 hari terakhir yang cocok." else
                "Feed gagal diambil untuk kategori: ${failures.joinToString(", ")}."
            return "Tidak ada berita terbaru yang dapat diverifikasi dari feed penerbit yang diperiksa (ANTARA, CNN Indonesia, Detikcom, CNA Indonesia, dan RSS internasional). $failureText Jangan membuat ringkasan dari ingatan model atau mengganti hasil dengan angka/peristiwa perkiraan."
        }

        return buildString {
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
            if (failures.isNotEmpty()) appendLine("\nCatatan sumber: sebagian feed tidak berhasil diambil (${failures.joinToString(", ")}).")
            append("Catatan cakupan: ini sampel dari feed yang berhasil diperiksa, bukan seluruh berita. Jangan menambah fakta, angka, kutipan, atau sebab-akibat yang tidak didukung deskripsi penerbit.")
        }.take(11_000)
    }

    /**
     * Topic search (e.g. "gibran") over Google News' keyless public RSS search, Indonesian edition.
     * Returns the structured articles, or null when nothing recent matches so callers can fall
     * back instead of inventing news. One network pass is shared by [topicSearch].
     */
    fun topicSearchArticles(query: String, limit: Int = 10): List<Article>? {
        val terms = queryTerms(query)
        if (terms.isEmpty()) return null
        val q = terms.joinToString(" ")
        val url = "https://news.google.com/rss/search?q=" +
            java.net.URLEncoder.encode(q, "UTF-8") + "&hl=id&gl=ID&ceid=ID:id"
        val parsed = fetchParsed(url) ?: return null
        val now = System.currentTimeMillis()
        val cutoff = now - MAX_AGE_DAYS * 24L * 60L * 60L * 1000L
        val items = parsed.asSequence()
            .filter { it.publishedEpoch in cutoff..(now + 5 * 60 * 1000L) }
            .distinctBy { normalize(articleKey(it.title)) }
            .sortedByDescending { it.publishedEpoch }
            .take(limit.coerceIn(1, 15)).toList()
        return items.ifEmpty { null }
    }

    /** Model-facing summary built on one [topicSearchArticles] pass. */
    fun topicSearch(query: String, limit: Int = 10): String? {
        val items = topicSearchArticles(query, limit) ?: return null
        val q = queryTerms(query).joinToString(" ")
        return buildString {
            appendLine("HASIL PENCARIAN BERITA untuk \"$q\" (Google News RSS; maksimal 7 hari terakhir)")
            items.forEachIndexed { i, a ->
                appendLine("${i + 1}. ${a.title}")
                appendLine("   ${a.source} • ${a.published}")
                if (a.description.isNotBlank()) appendLine("   ${a.description}")
                appendLine("   Sumber: ${a.url}")
            }
            append("Judul dan deskripsi berasal dari penerbit. Jangan menambah fakta di luar teks ini.")
        }.take(8_000)
    }

    internal fun categoriesFor(query: String): List<String> {
        val q = query.lowercase(Locale.ROOT)
        val selected = linkedSetOf<String>()
        if (listOf("sains", "science", "nasa", "mars", "teknologi", "technology", "tech", "ai", "gemini").any { q.contains(it) }) selected += if (q.contains("sains") || q.contains("science") || q.contains("nasa") || q.contains("mars")) "science" else "tech"
        if (listOf("ekonomi", "bisnis", "business", "finance", "finansial", "emas", "rupiah", "kurs", "saham", "ihsg").any { q.contains(it) }) selected += "finance"
        if (listOf("politik", "politics", "pemerintah", "kpk", "indonesia", "nasional").any { q.contains(it) }) selected += "politics"
        if (listOf("olahraga", "sports", "sepak bola", "football").any { q.contains(it) }) selected += "sports"
        if (listOf("dunia", "internasional", "world", "global", "perang", "ukraina", "rusia").any { q.contains(it) }) selected += "world"
        if (selected.isEmpty()) selected.addAll(listOf("world", "politics", "finance"))
        return selected.take(4)
    }

    private fun queryTerms(query: String): List<String> {
        val stop = setOf("berita", "terbaru", "terkini", "hari", "ini", "today", "latest", "news", "apa", "saja", "tentang", "tolong", "cari", "ringkasan", "per", "tanggal", "indonesia", "nasional", "dunia", "internasional", "januari", "februari", "maret", "april", "mei", "juni", "juli", "agustus", "september", "oktober", "november", "desember", "january", "february", "march", "may", "june", "july", "august", "september", "october", "november", "december")
        return Regex("[\\p{L}\\p{N}]{4,}").findAll(query.lowercase(Locale.ROOT))
            .map { it.value }.filterNot { it in stop || it.all { ch -> ch.isDigit() } }.distinct().take(5).toList()
    }

    internal fun parseFeed(xml: String): List<Article> {
        val itemRegex = Regex("(?is)<item\\b[^>]*>(.*?)</item>")
        val content = xml.take(MAX_FEED_CHARS)
        val rssItems = itemRegex.findAll(content).mapNotNull { match ->
            val block = match.groupValues[1]
            val title = tag(block, "title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val url = tag(block, "link").trim().takeIf(::isHttpsUrl) ?: return@mapNotNull null
            val published = sequenceOf("pubDate", "published", "date", "dc:date", "updated")
                .map { tag(block, it) }.firstOrNull { it.isNotBlank() } ?: return@mapNotNull null
            val epoch = parseEpoch(published) ?: return@mapNotNull null
            val source = tag(block, "source").ifBlank {
                runCatching { URI(url).host?.removePrefix("www.") }.getOrNull().orEmpty()
            }
            val description = tag(block, "description").ifBlank { tag(block, "content:encoded") }
            Article(decode(title).take(300), url, decode(source).take(100), published.take(100), epoch, decode(description).take(700))
        }.toList()
        if (rssItems.isNotEmpty()) return rssItems
        // Atom feeds use <entry>, <link href="..."/> and <published>/<updated>.
        val entryRegex = Regex("(?is)<entry\\b[^>]*>(.*?)</entry>")
        return entryRegex.findAll(content).mapNotNull { match ->
            val block = match.groupValues[1]
            val title = tag(block, "title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val linkTag = Regex("(?is)<link\\b([^>]*)/?>").find(block)?.groupValues?.get(1).orEmpty()
            val url = Regex("(?is)href\\s*=\\s*[\"']([^\"']+)[\"']").find(linkTag)?.groupValues?.get(1)?.trim()
                ?.takeIf(::isHttpsUrl) ?: return@mapNotNull null
            val published = sequenceOf("published", "updated", "date").map { tag(block, it) }
                .firstOrNull { it.isNotBlank() } ?: return@mapNotNull null
            val epoch = parseEpoch(published) ?: return@mapNotNull null
            val source = tag(block, "name").ifBlank { runCatching { URI(url).host.orEmpty() }.getOrDefault("") }
            val description = tag(block, "summary").ifBlank { tag(block, "content") }
            Article(decode(title).take(300), url, decode(source).take(100), published.take(100), epoch, decode(description).take(700))
        }.toList()
    }

    private fun tag(block: String, name: String): String {
        val safe = Regex.escape(name)
        val match = Regex("(?is)<$safe(?:\\s[^>]*)?>(.*?)</$safe\\s*>").find(block) ?: return ""
        return match.groupValues[1].replace(Regex("(?is)^\\s*<!\\[CDATA\\[(.*?)]]>\\s*\$"), "\$1").trim()
    }

    private fun decode(value: String): String = value
        .replace(Regex("(?is)<[^>]+>"), " ")
        .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
        .replace("&apos;", "'").replace("&lt;", "<").replace("&gt;", ">")
        .replace(Regex("&#(\\d+);")) { m -> m.groupValues[1].toIntOrNull()?.let { String(Character.toChars(it)) } ?: m.value }
        .replace(Regex("&#x([0-9a-fA-F]+);")) { m -> m.groupValues[1].toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: m.value }
        .replace(Regex("\\s+"), " ").trim()

    private fun parseEpoch(value: String): Long? = runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
        ?: runCatching { ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }.getOrNull()

    private fun isHttpsUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme.equals("https", true) && !uri.host.isNullOrBlank() && uri.userInfo == null
    }.getOrDefault(false)

    private fun fetch(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            requestMethod = "GET"
            // Publisher feeds commonly redirect to canonical RSS endpoints; follow HTTPS redirects.
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "AndroidIsland/1.15 (Noozra RSS reader)")
            setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml;q=0.9")
        }
        activeConnections.add(connection)
        try {
            val status = connection.responseCode
            if (status !in 200..299) return ""
            return connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText().take(MAX_FEED_CHARS) }
        } finally {
            activeConnections.remove(connection)
            connection.disconnect()
        }
    }

    private fun articleKey(title: String) = title.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
    private fun normalize(value: String) = value.lowercase(Locale.ROOT).trimEnd('/')
}
