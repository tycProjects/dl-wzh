package com.android.island.ai

import java.net.HttpURLConnection
import java.net.URL

/** Reads article text and explicit publication metadata from a web page. */
object PageReader {
    data class PageContent(val excerpt: String, val publishedAt: String?)

    fun excerpt(url: String, maxChars: Int = 1500): String? = read(url, maxChars)?.excerpt

    fun read(url: String, maxChars: Int = 1500): PageContent? {
        val html = fetch(url) ?: return null
        val published = publicationDate(html)
        val cleaned = html
            .replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")
            .replace(Regex("(?is)<style[^>]*>.*?</style>"), " ")
            .replace(Regex("(?is)<noscript[^>]*>.*?</noscript>"), " ")
            .replace(Regex("(?is)<!--.*?-->"), " ")
        val paragraphs = Regex("(?is)<p[^>]*>(.*?)</p>").findAll(cleaned)
            .map { clean(it.groupValues[1]) }
            .filter { it.length > 40 }
            .toList()
        val text = if (paragraphs.isNotEmpty()) paragraphs.joinToString(" ") else clean(cleaned)
        val excerpt = text.take(maxChars).takeIf { it.isNotBlank() } ?: return null
        return PageContent(excerpt, published)
    }

    internal fun publicationDate(html: String): String? {
        val metaTags = Regex("(?is)<meta\\b[^>]*>").findAll(html).map { it.value }
        for (tag in metaTags) {
            val key = Regex("(?i)(?:property|name|itemprop)\\s*=\\s*['\\\"]([^'\\\"]+)['\\\"]")
                .find(tag)?.groupValues?.get(1)?.lowercase() ?: continue
            if (key !in setOf("article:published_time", "datepublished", "publishdate", "pubdate")) continue
            val value = Regex("(?i)content\\s*=\\s*['\\\"]([^'\\\"]+)['\\\"]")
                .find(tag)?.groupValues?.get(1)?.trim()
            if (!value.isNullOrBlank()) return value
        }
        Regex("(?i)['\\\"]datePublished['\\\"]\\s*:\\s*['\\\"]([^'\\\"]+)['\\\"]")
            .find(html)?.groupValues?.get(1)?.let { return it }
        Regex("(?i)<time\\b[^>]*datetime\\s*=\\s*['\\\"]([^'\\\"]+)['\\\"]")
            .find(html)?.groupValues?.get(1)?.let { return it }
        return null
    }

    private fun clean(s: String): String = s
        .replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun fetch(url: String): String? = runCatching {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
        }
        if (conn.responseCode !in 200..299) return@runCatching null
        conn.inputStream.use { input ->
            val out = StringBuilder()
            val buffer = CharArray(8192)
            var total = 0
            input.bufferedReader(Charsets.UTF_8).use { reader ->
                while (true) {
                    val n = reader.read(buffer)
                    if (n < 0) break
                    total += n
                    if (total > MAX_FETCH_CHARS) throw IllegalStateException("Halaman melebihi batas ukuran")
                    out.append(buffer, 0, n)
                }
            }
            out.toString()
        }.take(MAX_FETCH_CHARS)
    }.getOrNull()

    private const val MAX_FETCH_CHARS = 400_000
}
