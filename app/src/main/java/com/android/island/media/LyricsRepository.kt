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

    /** Misses are remembered only for a while, so a song added to LRCLIB later is found again. */
    private val missUntil = HashMap<String, Long>()

    // Read live (not captured at construction) so toggling Low-End immediately shrinks future
    // caching; existing entries are not evicted early, only the bound for new ones changes.
    private val cacheMax: Int get() = if (com.android.island.core.DeviceProfile.tier == 2) 3 else 8
    private val missCap: Int get() = if (com.android.island.core.DeviceProfile.tier == 2) 60 else 200

    private val cache = object : LinkedHashMap<String, FoundLyrics?>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, FoundLyrics?>?) = size > cacheMax
    }

    /**
     * Called under system memory pressure (ComponentCallbacks2.onTrimMemory). Keeps only the
     * most recently used entry — the song currently playing or just shown — and drops the rest,
     * including remembered misses. accessOrder=true means the least-recently-used key is first.
     */
    fun trimCache() {
        synchronized(cache) { while (cache.size > 1) cache.remove(cache.keys.first()) }
        synchronized(missUntil) { missUntil.clear() }
    }

    private val timeTag = Regex("""\[(\d{1,3}):(\d{1,2}(?:[.:]\d{1,3})?)]""")
    private val offsetTag = Regex("""\[offset:\s*([+-]?\d+)]""", RegexOption.IGNORE_CASE)
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
            synchronized(missUntil) { if ((missUntil[key] ?: 0L) > System.currentTimeMillis()) return@withContext null }

            // Providers are tried in order; the first verified result wins. If the full title finds nothing,
            // retry once with the part before " - " (remaster and version suffixes).
            val core = t.substringBefore(" - ").trim().takeIf { it.isNotBlank() && it != t }
            var failed = false
            var found: FoundLyrics? = null
            for (variant in listOfNotNull(t, core)) {
                var plain: String? = null
                for (provider in providers) {
                    found = attempt({ failed = true }) {
                        provider(variant, mainArtist, album, durationMs) { plain = plain ?: it }
                    }
                    if (found != null) break
                }
                found = found ?: plain?.let { spread(it, durationMs) }
                if (found != null) break
            }

            val maxLines = if (com.android.island.core.DeviceProfile.tier == 2) MAX_CACHED_LINES_LOW_END else MAX_CACHED_LINES
            val bounded = found?.let { result ->
                if (result.lines.size <= maxLines) result
                else result.copy(lines = result.lines.take(maxLines))
            }
            val now = System.currentTimeMillis()
            if (bounded != null) synchronized(cache) { cache[key] = bounded }
            else synchronized(missUntil) {
                missUntil[key] = now + (if (failed) MISS_FAILED_MS else MISS_TTL_MS)
                if (missUntil.size > missCap) missUntil.entries.removeAll { it.value <= now }
            }
            bounded
        }

    /** Verified providers only. Add a source here after its accuracy and terms of use are checked. */
    private val providers: List<(String, String, String?, Long, (String) -> Unit) -> FoundLyrics?> = listOf(::lrclib, ::lyrica)

    /**
     * Self-hosted Lyrica server (https://codeberg.org/willooper/Lyrica), e.g. "http://192.168.1.10:9999".
     * Empty = disabled. Lyrica is unofficial and its response shape is not verified here, so a result is
     * used only when its title and artist fields are present and match the request.
     */
    private val lyricaBase: String = ""

    private fun lyrica(title: String, artist: String, album: String?, durationMs: Long, onPlain: (String) -> Unit): FoundLyrics? {
        if (lyricaBase.isBlank()) return null
        val body = get("$lyricaBase/lyrics/?" + encode(mapOf("artist" to artist, "song" to title, "timestamps" to "true"))) ?: return null
        val o = runCatching { org.json.JSONObject(body) }.getOrNull() ?: return null
        val gotTitle = o.optString("trackName", o.optString("title", ""))
        val gotArtist = o.optString("artistName", o.optString("artist", ""))
        if (gotTitle.isBlank() || gotArtist.isBlank()) return null          // identity not confirmed
        if (!sameName(gotTitle, title) || !sameName(gotArtist, artist)) return null
        val synced = listOf("syncedLyrics", "lrc", "lyrics").firstNotNullOfOrNull { k -> o.optString(k, "").takeIf { it.isNotBlank() } }
        if (synced != null && synced.contains("[")) {
            return FoundLyrics(parse(synced), true)
        }
        o.optString("plainLyrics", "").takeIf { it.isNotBlank() }?.let { onPlain(it) }
        return null
    }

    /** Loose name match: ignores case, spacing and punctuation. Used to reject look-alike results. */
    private fun sameName(a: String, b: String): Boolean {
        val x = a.lowercase().filter { it.isLetterOrDigit() }
        val y = b.lowercase().filter { it.isLetterOrDigit() }
        return x.isNotEmpty() && y.isNotEmpty() && (x == y || x.contains(y) || y.contains(x))
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
        pickLrclib(get("https://lrclib.net/api/search?" + encode(byFields)), title, artist, durationMs, onPlain)?.let { return it }
        // Free text search
        val q = mapOf("q" to "$artist $title")
        return pickLrclib(get("https://lrclib.net/api/search?" + encode(q)), title, artist, durationMs, onPlain)
    }

    private fun pickLrclib(body: String?, title: String, artist: String, durationMs: Long, onPlain: (String) -> Unit): FoundLyrics? {
        if (body.isNullOrBlank()) return null
        val arr = JSONArray(body)
        var best: String? = null
        var bestDiff = Double.MAX_VALUE
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            // Verify identity: the duration alone can match the wrong song.
            if (!sameName(o.optString("trackName"), title) || !sameName(o.optString("artistName"), artist)) continue
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
        // Some LRC files carry a global [offset:+-ms] tag (a positive value means every line
        // should play that many ms EARLIER). Ignoring it was why certain tracks lagged a fixed
        // amount behind playback: the raw timestamps were used as-is instead of being shifted.
        val offsetMs = offsetTag.find(lrc)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val out = ArrayList<LyricLine>()
        lrc.lineSequence().forEach { raw ->
            val tags = timeTag.findAll(raw).toList()
            if (tags.isEmpty()) return@forEach
            val text = raw.substring(tags.last().range.last + 1).trim()
            if (text.isBlank()) return@forEach
            tags.forEach { m ->
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].replace(':', '.').toDoubleOrNull() ?: return@forEach
                val lineMs = min * 60_000 + (sec * 1000).toLong()
                out += LyricLine((lineMs - offsetMs).coerceAtLeast(0L), text)
            }
        }
        out.sortBy { it.timeMs }
        return out
    }

    companion object {
        private const val MAX_DIFF_S = 12.0
        private const val MISS_TTL_MS = 10 * 60 * 1000L   // a real miss: retry after 10 minutes
        private const val MISS_FAILED_MS = 60 * 1000L      // a network error: retry after 1 minute
        // Protect heap usage from malformed or unusually long lyric responses.
        private const val MAX_CACHED_LINES = 600
        // Low-End: 240 lines covers essentially every real song lyric; only guards pathological ones.
        private const val MAX_CACHED_LINES_LOW_END = 240
    }
}
