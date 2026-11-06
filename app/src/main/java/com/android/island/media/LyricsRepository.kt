package com.android.island.media

import com.android.island.core.LyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

data class FoundLyrics(val lines: List<LyricLine>, val synced: Boolean)

/**
 * Lyrics from LRCLIB (lrclib.net) — same approach as the reference Dynamic Island.
 */
class LyricsRepository {

    private val cache = object : LinkedHashMap<String, FoundLyrics?>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, FoundLyrics?>?) = size > 40
    }

    private val timeTag = Regex("""\[(\d{1,3}):(\d{1,2}(?:[.:]\d{1,3})?)]""")
    private val noise = Regex(
        """\s*[(\[][^)\]]*(official|video|lyric|audio|clip|remaster|visuali[sz]er|hd|4k|mv|prod\.?)[^)\]]*[)\]]""",
        RegexOption.IGNORE_CASE
    )
    private val feat = Regex("""\s*[(\[]?\s*(feat\.?|ft\.?)\s.*$""", RegexOption.IGNORE_CASE)

    suspend fun fetchLyrics(title: String, artist: String, album: String? = null, durationMs: Long = 0L): List<LyricLine> =
        withContext(Dispatchers.IO) {
            find(title, artist, album, durationMs)?.lines.orEmpty()
        }

    suspend fun find(title: String, artist: String, album: String?, durationMs: Long): FoundLyrics? =
        withContext(Dispatchers.IO) {
            var t = title.trim()
            var a = artist.trim()
            if (a.isBlank() && " - " in t) {
                a = t.substringBefore(" - ").trim()
                t = t.substringAfter(" - ").trim()
            }
            t = t.replace(noise, "").replace(feat, "").trim()
            val mainArtist = a.split(",", "&", " x ", " feat", " ft.").first().trim()
            if (t.isBlank()) return@withContext null
            val key = "${mainArtist.lowercase()}|${t.lowercase()}"
            synchronized(cache) { if (cache.containsKey(key)) return@withContext cache[key] }

            var failed = false
            var plain: String? = null
            val found = attempt({ failed = true }) {
                lrclib(t, mainArtist, album, durationMs) { plain = plain ?: it }
            } ?: plain?.let { spread(it, durationMs) }

            val bounded = found?.let { result ->
                if (result.lines.size <= MAX_CACHED_LINES) result
                else result.copy(lines = result.lines.take(MAX_CACHED_LINES))
            }
            if (bounded != null || !failed) synchronized(cache) { cache[key] = bounded }
            bounded
        }

    private inline fun attempt(onError: () -> Unit, block: () -> FoundLyrics?): FoundLyrics? =
        try { block() } catch (_: Exception) { onError(); null }

    private fun lrclib(
        title: String,
        artist: String,
        album: String?,
        durationMs: Long,
        onPlain: (String) -> Unit
    ): FoundLyrics? {
        val durationSec = if (durationMs > 0) (durationMs / 1000.0) else null
        // Exact get
        val params = mutableMapOf("track_name" to title, "artist_name" to artist)
        if (!album.isNullOrBlank()) params["album_name"] = album
        if (durationSec != null) params["duration"] = durationSec.toInt().toString()
        get("https://lrclib.net/api/get?" + encode(params))?.let { body ->
            val o = JSONObject(body)
            synced(o)?.let { return FoundLyrics(parse(it), true) }
            plain(o)?.let { onPlain(it) }
        }
        // Search by fields
        val byFields = mutableMapOf("track_name" to title, "artist_name" to artist)
        pickLrclib(get("https://lrclib.net/api/search?" + encode(byFields)), durationMs, onPlain)?.let { return it }
        // Free text search
        val q = mapOf("q" to "$artist $title")
        return pickLrclib(get("https://lrclib.net/api/search?" + encode(q)), durationMs, onPlain)
    }

    private fun pickLrclib(body: String?, durationMs: Long, onPlain: (String) -> Unit): FoundLyrics? {
        if (body.isNullOrBlank()) return null
        val arr = JSONArray(body)
        var best: String? = null
        var bestDiff = Double.MAX_VALUE
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val dur = o.optDouble("duration", -1.0)
            val diff = if (durationMs > 0 && dur > 0) abs(dur * 1000 - durationMs) / 1000.0 else 0.0
            if (diff > MAX_DIFF_S) continue
            val syn = synced(o)
            if (syn != null && diff < bestDiff) {
                best = syn
                bestDiff = diff
            } else if (syn == null) {
                plain(o)?.let { onPlain(it) }
            }
        }
        return best?.let { FoundLyrics(parse(it), true) }
    }

    private fun synced(o: JSONObject): String? =
        if (o.isNull("syncedLyrics")) null else o.optString("syncedLyrics").takeIf { it.isNotBlank() }

    private fun plain(o: JSONObject): String? =
        if (o.isNull("plainLyrics")) null else o.optString("plainLyrics").takeIf { it.isNotBlank() }

    private fun spread(plain: String, durationMs: Long): FoundLyrics? {
        val lines = plain.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty() || durationMs <= 0) {
            if (lines.isEmpty()) return null
            // No duration — equal spacing every 4s
            return FoundLyrics(lines.mapIndexed { i, t -> LyricLine(i * 4000L, t) }, false)
        }
        val start = durationMs * 0.08
        val step = durationMs * 0.84 / lines.size
        return FoundLyrics(lines.mapIndexed { i, t -> LyricLine((start + step * i).toLong(), t) }, false)
    }

    private fun encode(params: Map<String, String>) =
        params.entries.joinToString("&") { (k, v) -> "$k=" + URLEncoder.encode(v, "UTF-8") }

    private fun get(url: String): String? {
        val c = URL(url).openConnection() as HttpURLConnection
        return try {
            c.connectTimeout = 6000
            c.readTimeout = 8000
            c.setRequestProperty("User-Agent", "DynamicIslandAndroid/1.0")
            if (c.responseCode == 404) return null
            if (c.responseCode != 200) return null
            c.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            c.disconnect()
        }
    }

    fun parse(lrc: String): List<LyricLine> {
        val out = ArrayList<LyricLine>()
        lrc.lineSequence().forEach { raw ->
            val tags = timeTag.findAll(raw).toList()
            if (tags.isEmpty()) return@forEach
            val text = raw.substring(tags.last().range.last + 1).trim()
            if (text.isBlank()) return@forEach
            tags.forEach { m ->
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].replace(':', '.').toDoubleOrNull() ?: return@forEach
                out += LyricLine(min * 60_000 + (sec * 1000).toLong(), text)
            }
        }
        out.sortBy { it.timeMs }
        return out
    }

    companion object {
        private const val MAX_DIFF_S = 12.0
        // Protect heap usage from malformed or unusually long lyric responses.
        private const val MAX_CACHED_LINES = 600
    }
}
