package com.android.island.media

import com.android.island.core.LyricLine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

data class FoundLyrics(
    val lines: List<LyricLine>,
    val synced: Boolean,
    val validation: LyricsValidation? = null,
    val document: LyricsDocument? = null,
    val source: String = "LRCLIB"
)

/**
 * Lyrics from LRCLIB (lrclib.net) — same approach as the reference Dynamic Island.
 */
class LyricsRepository {

    /** Misses are remembered only for a while, so a song added to LRCLIB later is found again. */
    private val missUntil = HashMap<String, Long>()
    /** Short-lived marker for "providers were unreachable"; callers see an IOException, not an empty result. */
    private val failUntil = HashMap<String, Long>()

    // Same bounds in every mode. Existing entries are not evicted early.
    private val cacheMax: Int get() = 8
    private val missCap: Int get() = 200

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
        synchronized(failUntil) { failUntil.clear() }
    }

    private val timeTag = Regex("""\[(\d{1,3}):(\d{1,2}(?:[.:]\d{1,3})?)]""")
    /** Enhanced-LRC inline word timestamps such as <00:12.34>. */
    private val wordTag = Regex("""<\d{1,3}:\d{1,2}(?:[.:]\d{1,3})?>""")
    private val offsetTag = Regex("""\[offset:\s*([+-]?\d+)]""", RegexOption.IGNORE_CASE)
    private val noise = Regex(
        """\s*[(\[][^)\]]*(official|video|lyric|audio|clip|remaster|visuali[sz]er|hd|4k|mv|prod\.?)[^)\]]*[)\]]""",
        RegexOption.IGNORE_CASE
    )
    private val feat = Regex("""\s*[(\[]?\s*(feat\.?|ft\.?)\s.*$""", RegexOption.IGNORE_CASE)

    /** Empty list = confirmed no synced lyrics. Throws IOException when providers were unreachable. */
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
            synchronized(failUntil) {
                if ((failUntil[key] ?: 0L) > System.currentTimeMillis()) throw java.io.IOException("Penyedia lirik tidak dapat dihubungi")
            }

            // Providers run in parallel and the highest-priority verified result wins, so the wait is
            // the slowest single provider instead of the sum of all of them. If the full title finds
            // nothing, retry once with the part before " - " (remaster and version suffixes).
            val core = t.substringBefore(" - ").trim().takeIf { it.isNotBlank() && it != t }
            var anyCompleted = false
            var found: FoundLyrics? = null
            val bare = t.replace(Regex("\\s*[(\\[][^)\\]]*[)\\]]\\s*\$"), "").trim()
                .takeIf { it.isNotBlank() && it != t && it != core }
            for (variant in listOfNotNull(t, core, bare)) {
                // Playback lyrics must carry real provider timestamps. Plain text is deliberately
                // ignored: estimating timestamps spreads lines evenly and de-syncs the karaoke view.
                val outcome = queryProviders(variant, mainArtist, album, durationMs)
                if (outcome.completed) anyCompleted = true
                found = outcome.found
                if (found != null) break
            }

            val maxLines = MAX_CACHED_LINES
            val bounded = found?.let { result ->
                if (result.lines.size <= maxLines) result
                else result.copy(
                    lines = result.lines.take(maxLines),
                    document = result.document?.copy(lines = result.document.lines.take(maxLines))
                )
            }
            val now = System.currentTimeMillis()
            if (bounded != null) {
                synchronized(cache) { cache[key] = bounded }
                synchronized(failUntil) { failUntil.remove(key) }
            } else if (!anyCompleted) {
                // Every provider errored (offline, timeout, 5xx). This is NOT "no lyrics": report it so the
                // caller does not cache an empty result, and retry soon.
                synchronized(failUntil) {
                    failUntil[key] = now + MISS_FAILED_MS
                    if (failUntil.size > missCap) failUntil.entries.removeAll { it.value <= now }
                }
                throw java.io.IOException("Semua penyedia lirik gagal dihubungi")
            } else {
                synchronized(missUntil) {
                    missUntil[key] = now + MISS_TTL_MS
                    if (missUntil.size > missCap) missUntil.entries.removeAll { it.value <= now }
                }
            }
            bounded
        }

    private class ProviderOutcome(val found: FoundLyrics?, val completed: Boolean)

    /**
     * [authoritative] providers always perform a real lookup, so only their clean completion proves
     * "no lyrics exist". Providers that skip the network (e.g. no album known) must not count.
     */
    private class Provider(
        val authoritative: Boolean,
        val run: (String, String, String?, Long, (String) -> Unit) -> FoundLyrics?
    )

    /** Query every provider at once; results are consumed in priority order under one overall deadline. */
    private suspend fun queryProviders(title: String, artist: String, album: String?, durationMs: Long): ProviderOutcome {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val jobs = providers.map { provider ->
                scope.async {
                    try {
                        ProviderOutcome(
                            provider.run(title, artist, album, durationMs) { /* untimed lyrics rejected */ },
                            provider.authoritative
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        ProviderOutcome(null, false)
                    }
                }
            }
            val deadline = System.currentTimeMillis() + OVERALL_DEADLINE_MS
            var completed = false
            for (job in jobs) {
                val left = (deadline - System.currentTimeMillis()).coerceAtLeast(1L)
                val r = withTimeoutOrNull(left) { job.await() } ?: continue
                if (r.completed) completed = true
                if (r.found != null) return ProviderOutcome(r.found, true)
            }
            return ProviderOutcome(null, completed)
        } finally {
            // Blocking sockets finish on their own short timeouts; nothing waits for them.
            scope.cancel()
        }
    }

    /** Verified providers only. Add a source here after its accuracy and terms of use are checked. */
    private val providers: List<Provider> = listOf(
        Provider(false, ::betterLyrics),
        Provider(false, ::betterLyricsQq),
        Provider(false, ::betterLyricsKugou),
        Provider(true, ::lrclib),
        // STAGE 74: keyless synced-lyrics enrichment. NetEase exposes its search + lyric endpoints
        // publicly (no API key, no registration), which widens coverage for Chinese/Asian
        // catalogs that LRCLIB and the BetterLyrics caches often miss. Results still pass the
        // same validation gate (identity match, duration match, monotonic timestamps, quality
        // score) before they are ever shown.
        Provider(false, ::netEase),
        Provider(false, ::lyrica)
    )

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
            return syncedResult(synced, title, artist, durationMs, "Lyrica")
        }
        o.optString("plainLyrics", "").takeIf { it.isNotBlank() }?.let { onPlain(it) }
        return null
    }


    /**
     * Better Lyrics public cache endpoint. Cached responses are available without an API key;
     * cache misses may return 401 because fresh upstream lookups require a key that is not
     * currently issued to new clients. Therefore this is a best-effort first provider only.
     * TTML is parsed locally and validated before it can be shown.
     */
    private fun betterLyrics(
        title: String,
        artist: String,
        album: String?,
        durationMs: Long,
        onPlain: (String) -> Unit
    ): FoundLyrics? {
        val durationSec = (durationMs / 1000L).takeIf { it > 0L } ?: return null
        val albumName = album?.takeIf { it.isNotBlank() } ?: return null
        val params = mapOf("s" to title, "a" to artist, "al" to albumName, "d" to durationSec.toString())
        val body = get("https://api.betterlyrics.org/getLyrics?" + encode(params), 2500, 3500)
            ?: return null
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val ttml = json.optString("ttml", "").takeIf { it.isNotBlank() } ?: return null
        return ttmlResult(ttml, title, artist, durationMs, "BetterLyrics")
    }

    /**
     * STAGE 74: NetEase Cloud Music provider — free and keyless.
     * Search: https://music.163.com/api/search/pc (GET, no key) -> result.songs[] with id, name,
     * duration (ms) and artists[]. Lyric: https://music.163.com/api/song/lyric?os=pc&id=..&lv=-1&kv=-1&tv=-1
     * -> lrc.lyric carries standard [mm:ss.xx] timestamps. No API key is involved, matching the
     * request for free/keyless providers. Strict identity + duration matching first; the parsed
     * LRC then goes through the SAME syncedResult validation as every other provider
     * (monotonic timestamps, start-to-end coverage, quality score).
     */
    private fun netEase(
        title: String,
        artist: String,
        album: String?,
        durationMs: Long,
        onPlain: (String) -> Unit
    ): FoundLyrics? {
        val q = if (artist.isNotBlank()) "$artist $title" else title
        val searchBody = get(
            "https://music.163.com/api/search/pc?" + encode(mapOf("s" to q, "type" to "1", "limit" to "5", "offset" to "0")),
            2500, 3500
        ) ?: return null
        val json = runCatching { JSONObject(searchBody) }.getOrNull() ?: return null
        val songs = json.optJSONObject("result")?.optJSONArray("songs") ?: return null
        var bestId = -1L
        var bestDiff = Double.MAX_VALUE
        for (i in 0 until songs.length()) {
            val song = songs.optJSONObject(i) ?: continue
            val name = song.optString("name")
            if (!sameName(name, title)) continue
            val artists = song.optJSONArray("artists")?.let { arr ->
                (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("name") }
            }.orEmpty()
            if (artist.isNotBlank() && artists.none { sameName(it, artist) }) continue
            val dur = song.optLong("duration", 0L)
            val diff = if (durationMs > 0 && dur > 0) abs(dur - durationMs) / 1000.0 else 0.0
            if (durationMs > 0 && dur > 0 && diff > MAX_DIFF_S) continue
            if (diff < bestDiff) {
                bestDiff = diff
                bestId = song.optLong("id", -1L)
            }
        }
        if (bestId <= 0) return null
        val lyricBody = get(
            "https://music.163.com/api/song/lyric?os=pc&id=$bestId&lv=-1&kv=-1&tv=-1", 2500, 3500
        ) ?: return null
        val lyricJson = runCatching { JSONObject(lyricBody) }.getOrNull() ?: return null
        val lrc = lyricJson.optJSONObject("lrc")?.optString("lyric").orEmpty()
        if (!lrc.contains('[')) return null
        return syncedResult(lrc, title, artist, durationMs, "NetEase")
    }

    /** QQ provider: QRC includes absolute word-level timestamps inside LyricContent XML attribute. */
    private fun betterLyricsQq(
        title: String, artist: String, album: String?, durationMs: Long, onPlain: (String) -> Unit
    ): FoundLyrics? {
        val durationSec = (durationMs / 1000L).takeIf { it > 0L } ?: return null
        val albumName = album?.takeIf { it.isNotBlank() } ?: return null
        val params = mapOf("s" to title, "a" to artist, "al" to albumName, "d" to durationSec.toString())
        val body = get("https://api.betterlyrics.org/qq/getLyrics?" + encode(params), 2500, 3500) ?: return null
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val qrc = json.optString("lyrics", "").takeIf { it.isNotBlank() } ?: return null
        return qrcResult(qrc, title, artist, durationMs)
    }

    /** Kugou provider: line-synced LRC fallback; this endpoint does not require an API key. */
    private fun betterLyricsKugou(
        title: String, artist: String, album: String?, durationMs: Long, onPlain: (String) -> Unit
    ): FoundLyrics? {
        val durationSec = (durationMs / 1000L).takeIf { it > 0L } ?: return null
        val albumName = album?.takeIf { it.isNotBlank() } ?: return null
        val params = mapOf("s" to title, "a" to artist, "al" to albumName, "d" to durationSec.toString())
        val body = get("https://api.betterlyrics.org/kugou/getLyrics?" + encode(params), 2500, 3500) ?: return null
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val lrc = json.optString("lyrics", "").takeIf { it.isNotBlank() } ?: return null
        return syncedResult(lrc, title, artist, durationMs, "BetterLyrics · Kugou")
    }

    /** Parse QQ QRC without a DOM: keep absolute word timing and reject metadata/credit-only lines. */
    private fun qrcResult(xml: String, title: String, artist: String, durationMs: Long): FoundLyrics? {
        if (xml.length > MAX_RESPONSE_CHARS) return null
        val content = Regex("""(?is)LyricContent="([\s\S]*?)"\s*/?>""")
            .find(xml)?.groupValues?.get(1)?.let(::decodeXml) ?: return null
        val header = Regex("""^\[(\d+),(\d+)\]""")
        val wordToken = Regex("""\((\d+),(\d+)\)""")
        val cues = ArrayList<LyricCue>()
        val lyricLines = ArrayList<LyricLine>()
        for (raw in content.lineSequence()) {
            val line = raw.trim()
            val h = header.find(line) ?: continue
            val start = h.groupValues[1].toLongOrNull() ?: continue
            val length = h.groupValues[2].toLongOrNull() ?: 0L
            if (start < 0L || (durationMs > 0 && start > durationMs + 2000L)) continue
            val rest = line.substring(h.range.last + 1)
            val words = ArrayList<LyricWord>()
            var cursor = 0
            for (token in wordToken.findAll(rest)) {
                val text = rest.substring(cursor, token.range.first)
                val wordStart = token.groupValues[1].toLongOrNull() ?: continue
                val wordLength = token.groupValues[2].toLongOrNull() ?: 0L
                if (text.isNotEmpty()) words += LyricWord(text, wordStart, wordStart + wordLength)
                cursor = token.range.last + 1
            }
            val text = (if (words.isNotEmpty()) joinWordSegments(words.map { it.text }) else rest).trim()
            if (text.isBlank() || text.startsWith("[") || Regex("(?i)^(lyrics by|composed by|arranged by|producer)[:：]").containsMatchIn(text)) continue
            val normalizedText = text.lowercase().filter { it.isLetterOrDigit() }
            val normalizedTitle = title.lowercase().filter { it.isLetterOrDigit() }
            val normalizedArtist = artist.lowercase().filter { it.isLetterOrDigit() }
            if (normalizedTitle.isNotEmpty() && normalizedArtist.isNotEmpty() &&
                normalizedText.contains(normalizedTitle) && normalizedText.contains(normalizedArtist)) continue
            val cueWords = words.filter { it.text.isNotBlank() }
            cues += LyricCue(text, start, (start + length).takeIf { length > 0 }, cueWords)
            lyricLines += LyricLine(start, text, words = cueWords.map { com.android.island.core.LyricWordCue(it.text, it.startMs, it.endMs) })
            if (cues.size >= MAX_CACHED_LINES) break
        }
        if (cues.size < 2) return null
        val document = LyricsDocument(title, artist, durationMs = durationMs.takeIf { it > 0 }, lines = cues, source = "BetterLyrics · QQ")
        val report = LyricsFileCodec.validate(document, durationMs.takeIf { it > 0 })
        if (!report.valid || report.score < MIN_SCORE) return null
        return FoundLyrics(lyricLines, true, report, document, "BetterLyrics · QQ")
    }

    /** Parses TTML paragraph/word timing without retaining a DOM tree (keeps memory use low). */
    private fun ttmlResult(ttml: String, title: String, artist: String, durationMs: Long, source: String): FoundLyrics? {
        if (ttml.length > MAX_RESPONSE_CHARS) return null
        val paragraph = Regex("""(?is)<p\b([^>]*)>(.*?)</p>""")
        val attr = Regex("""([\w:-]+)\s*=\s*["']([^"']+)["']""")
        val span = Regex("""(?is)<span\b([^>]*)>(.*?)</span>""")
        val cues = ArrayList<LyricCue>()
        for (match in paragraph.findAll(ttml)) {
            val attrs = attr.findAll(match.groupValues[1]).associate { it.groupValues[1].substringAfter(':') to it.groupValues[2] }
            val start = parseTtmlTime(attrs["begin"] ?: attrs["start"]) ?: continue
            val content = match.groupValues[2]
            val words = span.findAll(content).mapNotNull { wordMatch ->
                val wa = attr.findAll(wordMatch.groupValues[1]).associate { it.groupValues[1].substringAfter(':') to it.groupValues[2] }
                val ws = parseTtmlTime(wa["begin"]) ?: return@mapNotNull null
                val we = parseTtmlTime(wa["end"])
                val wt = decodeXml(wordMatch.groupValues[2].replace(Regex("(?is)<[^>]+>"), "")).trim()
                if (wt.isBlank()) null else LyricWord(wt, ws, we)
            }.toList()
            // Preserve whitespace between TTML spans. Joining individual timed spans directly
            // can turn a correctly spaced sentence into "wordwordword".
            val rawText = decodeXml(content.replace(Regex("(?is)<[^>]+>"), "")).trim()
            val text = if (rawText.isNotBlank() && rawText.any { it.isWhitespace() }) rawText
                else joinWordSegments(words.map { it.text }).ifBlank { rawText }
            if (text.isBlank()) continue
            val end = parseTtmlTime(attrs["end"])
            cues += LyricCue(text, start, end, words)
            if (cues.size >= MAX_CACHED_LINES) break
        }
        if (cues.size < 2) return null
        val document = LyricsDocument(title, artist, durationMs = durationMs.takeIf { it > 0 }, lines = cues, source = source)
        val validation = LyricsFileCodec.validate(document, durationMs.takeIf { it > 0 })
        if (!validation.valid || validation.score < MIN_SCORE) return null
        val lines = cues.map { cue ->
            LyricLine(cue.startMs, cue.text, words = cue.words.map { com.android.island.core.LyricWordCue(it.text, it.startMs, it.endMs) })
        }
        return FoundLyrics(lines, true, validation, document, source)
    }

    /** Rebuilds missing word boundaries from word-timed segments without damaging CJK text. */
    private fun joinWordSegments(parts: List<String>): String {
        if (parts.isEmpty()) return ""
        val out = StringBuilder(parts.first())
        for (part in parts.drop(1)) {
            val previous = out.lastOrNull()
            val next = part.firstOrNull()
            val cjkPair = previous != null && next != null &&
                (previous.code in 0x2E80..0x9FFF || previous.code in 0xF900..0xFAFF || previous.code in 0x3040..0x30FF || previous.code in 0xAC00..0xD7AF) &&
                (next.code in 0x2E80..0x9FFF || next.code in 0xF900..0xFAFF || next.code in 0x3040..0x30FF || next.code in 0xAC00..0xD7AF)
            if (previous != null && next != null && previous.isLetterOrDigit() && next.isLetterOrDigit() &&
                !previous.isWhitespace() && !next.isWhitespace() && !cjkPair) out.append(' ')
            out.append(part)
        }
        return out.toString()
    }

    private fun parseTtmlTime(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        val v = value.trim()
        return runCatching {
            when {
                v.endsWith("ms") -> v.removeSuffix("ms").toDouble().toLong()
                v.endsWith("s") -> (v.removeSuffix("s").toDouble() * 1000.0).toLong()
                v.contains(':') -> {
                    val parts = v.split(':')
                    when (parts.size) {
                        3 -> ((parts[0].toDouble() * 3600 + parts[1].toDouble() * 60 + parts[2].toDouble()) * 1000).toLong()
                        2 -> ((parts[0].toDouble() * 60 + parts[1].toDouble()) * 1000).toLong()
                        else -> null
                    }
                }
                else -> null
            }
        }.getOrNull()?.takeIf { it >= 0L }
    }

    private fun decodeXml(value: String): String = value
        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&apos;", "'")
        .replace(Regex("&#(\\d+);")) { it.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: it.value }

    /** Loose name match: ignores case, spacing and punctuation. Used to reject look-alike results. */
    private fun sameName(a: String, b: String): Boolean {
        val x = a.lowercase().filter { it.isLetterOrDigit() }
        val y = b.lowercase().filter { it.isLetterOrDigit() }
        return x.isNotEmpty() && y.isNotEmpty() && (x == y || x.contains(y) || y.contains(x))
    }

    private inline fun attempt(onError: () -> Unit, block: () -> FoundLyrics?): FoundLyrics? =
        try { block() } catch (e: CancellationException) { throw e } catch (_: Exception) { onError(); null }

    private fun lrclib(
        title: String,
        artist: String,
        album: String?,
        durationMs: Long,
        onPlain: (String) -> Unit
    ): FoundLyrics? {
        val durationSec = if (durationMs > 0) (durationMs / 1000.0) else null
        val hasArtist = artist.isNotBlank()
        if (hasArtist) {
            // Exact get
            val params = mutableMapOf("track_name" to title, "artist_name" to artist)
            if (!album.isNullOrBlank()) params["album_name"] = album
            if (durationSec != null) params["duration"] = durationSec.toInt().toString()
            get("https://lrclib.net/api/get?" + encode(params))?.let { body ->
                val o = JSONObject(body)
                // LRCLIB's newer "lyricsfile" field carries the server's authoritative, word-level
                // Lyricsfile YAML; per LRCLIB's own precedence rule it wins over syncedLyrics/plainLyrics.
                lyricsfileField(o)?.let { yaml ->
                    fromLyricsFile(yaml, title, artist, durationMs, "LRCLIB")?.let { return it }
                }
                val exactSynced = synced(o)
                if (exactSynced != null) {
                    syncedResult(exactSynced, title, artist, durationMs, "LRCLIB")?.let { return it }
                }
                plain(o)?.let { onPlain(it) }
            }
            // Search by fields
            val byFields = mutableMapOf("track_name" to title, "artist_name" to artist)
            pickLrclib(get("https://lrclib.net/api/search?" + encode(byFields)), title, artist, durationMs, onPlain)?.let { return it }
        }
        // Free text search (the only lookup when the player reports no artist)
        val q = mapOf("q" to if (hasArtist) "$artist $title" else title)
        return pickLrclib(get("https://lrclib.net/api/search?" + encode(q)), title, artist, durationMs, onPlain)
    }

    private fun pickLrclib(body: String?, title: String, artist: String, durationMs: Long, onPlain: (String) -> Unit): FoundLyrics? {
        if (body.isNullOrBlank()) return null
        val arr = JSONArray(body)
        var best: String? = null
        var bestRank = Double.MAX_VALUE
        // Lyricsfile candidates are tracked separately and win over plain-LRC candidates
        // whenever at least one valid one exists (see fromLyricsFile for why).
        var bestFile: FoundLyrics? = null
        var bestFileRank = Double.MAX_VALUE
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            // Verify identity: the duration alone can match the wrong song.
            if (!sameName(o.optString("trackName"), title)) continue
            val dur = o.optDouble("duration", -1.0)
            val diff = if (durationMs > 0 && dur > 0) abs(dur * 1000 - durationMs) / 1000.0 else 0.0
            if (diff > MAX_DIFF_S) continue
            // Artist spelling differs between catalogs (transliteration, "A & B" vs "A"): only insist
            // on it when the duration does not already vouch for the track.
            val artistOk = artist.isBlank() || sameName(o.optString("artistName"), artist)
            if (!artistOk && !(durationMs > 0 && dur > 0 && diff <= 4.0)) continue
            lyricsfileField(o)?.let { yaml ->
                fromLyricsFile(yaml, title, artist, durationMs, "LRCLIB")?.let { candidate ->
                    val qualityPenalty = candidate.validation?.score?.let { (100 - it) / 100.0 } ?: 1.0
                    val rank = diff + qualityPenalty * 4.0
                    if (rank < bestFileRank) {
                        bestFile = candidate
                        bestFileRank = rank
                    }
                }
            }
            val syn = synced(o)
            if (syn != null) {
                val candidate = syncedResult(syn, title, artist, durationMs, "LRCLIB")
                val qualityPenalty = candidate?.validation?.score?.let { (100 - it) / 100.0 } ?: 1.0
                val rank = diff + qualityPenalty * 4.0
                if (candidate != null && rank < bestRank) {
                    best = syn
                    bestRank = rank
                }
            } else {
                plain(o)?.let { onPlain(it) }
            }
        }
        bestFile?.let { return it }
        return best?.let { syncedResult(it, title, artist, durationMs, "LRCLIB") }
    }

    private fun synced(o: JSONObject): String? =
        if (o.isNull("syncedLyrics")) null else o.optString("syncedLyrics").takeIf { it.isNotBlank() }

    private fun plain(o: JSONObject): String? =
        if (o.isNull("plainLyrics")) null else o.optString("plainLyrics").takeIf { it.isNotBlank() }

    private fun lyricsfileField(o: JSONObject): String? =
        if (o.isNull("lyricsfile")) null else o.optString("lyricsfile").takeIf { it.isNotBlank() }

    /**
     * LRCLIB's own precedence rule: when a track has a Lyricsfile, it is the authoritative,
     * word-level source and the derived syncedLyrics/plainLyrics should be ignored. We reuse the
     * existing LyricsFileCodec (already spec-complete) instead of re-deriving line cues from LRC.
     */
    private fun fromLyricsFile(yaml: String, title: String, artist: String, durationMs: Long, source: String): FoundLyrics? {
        val document = runCatching { LyricsFileCodec.parse(yaml) }.getOrNull() ?: return null
        if (document.instrumental || document.lines.isEmpty()) return null
        val lines = document.lines.map { cue ->
            LyricLine(
                cue.startMs,
                cue.text,
                words = cue.words.map { com.android.island.core.LyricWordCue(it.text, it.startMs, it.endMs) }
            )
        }
        if (lines.size < 2) return null
        val report = LyricsFileCodec.validate(document, durationMs.takeIf { it > 0 })
        // STAGE 74: unify with the other providers — invalid or low-quality (partial coverage,
        // heavy duplicates, mistimed words) lyricsfile documents are rejected outright.
        if (!report.valid || report.score < MIN_SCORE) return null
        return FoundLyrics(lines, true, report, document, source)
    }

    private val creditLine = Regex(
        """(?i)^\s*(作词|作曲|编曲|制作人?|演唱|歌手|歌名|专辑|词|曲|混音|母带|lyrics?(\s+by)?|lyricist|composer|composed(\s+by)?|written(\s+by)?|arranged(\s+by)?|arrangement|producer|produced(\s+by)?|music(\s+by)?|vocals?|mixing|mastering|title|artist|album|translator)\s*[:：]"""
    )

    /** Drops credit/metadata rows that some providers put inside the timed lyrics (only near the edges). */
    private fun stripCredits(lines: List<LyricLine>, title: String, artist: String): List<LyricLine> {
        fun norm(v: String) = v.lowercase().filter { it.isLetterOrDigit() }
        val nt = norm(title)
        val na = norm(artist)
        val n = lines.size
        return lines.filterIndexed { i, line ->
            if (i >= 10 && i < n - 6) return@filterIndexed true
            val nl = norm(line.text)
            val titleArtistHeader = nt.isNotEmpty() && na.isNotEmpty() && nl.contains(nt) && nl.contains(na) &&
                nl.length <= nt.length + na.length + 8
            !(creditLine.containsMatchIn(line.text) || titleArtistHeader)
        }
    }

    private fun syncedResult(lrc: String, title: String, artist: String, durationMs: Long, source: String): FoundLyrics? {
        val parsed = stripCredits(parse(lrc), title, artist)
        if (parsed.size < 2) return null
        if (parsed.any { it.timeMs < 0L } || parsed.zipWithNext().any { (a, b) -> b.timeMs < a.timeMs }) return null
        if (durationMs > 0L && parsed.count { it.timeMs <= durationMs + 2_000L } < 2) return null
        // STAGE 77: no coverage hard gate any more. Timestamped lyrics that start late or stop
        // early are still real, usable synced lyrics, so they are shown; coverage only costs a
        // few quality points inside LyricsFileCodec.validate.
        val document = LyricsDocument(
            title = title,
            artist = artist,
            durationMs = durationMs.takeIf { it > 0 },
            lines = parsed.map { LyricCue(it.text, it.timeMs) },
            source = source
        )
        val report = LyricsFileCodec.validate(document, durationMs.takeIf { it > 0 })
        // Structural problems reject a provider response; warnings remain visible in the report
        // and lower confidence without destroying otherwise useful synced lyrics.
        if (!report.valid) return null
        if (report.score < MIN_SCORE) return null
        return FoundLyrics(parsed, true, report, document, source)
    }

    /**
     * Import a Lyricsfile document into the playback model without discarding word-level data.
     * Only documents with real timed lines are accepted; a plain-text-only file has no timestamps
     * and is rejected instead of being spread evenly over the song.
     */
    fun importLyricsFile(yamlText: String): FoundLyrics? = runCatching {
        val document = LyricsFileCodec.parse(yamlText)
        if (document.lines.isEmpty()) return@runCatching null
        val report = LyricsFileCodec.validate(document)
        val lines = document.lines.map { cue ->
            LyricLine(cue.startMs, cue.text, words = cue.words.map { com.android.island.core.LyricWordCue(it.text, it.startMs, it.endMs) })
        }
        FoundLyrics(lines, true, report, document, "lyricsfile")
    }.getOrNull()

    private fun encode(params: Map<String, String>) =
        params.entries.joinToString("&") { (k, v) -> "$k=" + URLEncoder.encode(v, "UTF-8") }

    /**
     * Returns the body, or null for a definitive "nothing here" (404/other 4xx/oversize).
     * Network errors, timeouts, 429 and 5xx THROW so callers can tell "unreachable" from "no lyrics".
     */
    private fun get(url: String, connectTimeoutMs: Int = 3000, readTimeoutMs: Int = 4000): String? {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = connectTimeoutMs
            c.readTimeout = readTimeoutMs
            c.setRequestProperty("User-Agent", "Android Island/1.15.8 (com.android.island; Android app)")
            val code = c.responseCode
            if (code == 429 || code >= 500) throw java.io.IOException("HTTP $code")
            if (code != 200) return null
            // Stream with a hard size cap so a malformed response cannot blow up the heap.
            return c.inputStream.use { input ->
                val out = StringBuilder()
                val buffer = CharArray(8192)
                var total = 0
                input.bufferedReader(Charsets.UTF_8).use { reader ->
                    while (true) {
                        val n = reader.read(buffer)
                        if (n < 0) break
                        total += n
                        if (total > MAX_RESPONSE_CHARS) return null
                        out.append(buffer, 0, n)
                    }
                }
                out.toString()
            }
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
            val text = raw.substring(tags.last().range.last + 1)
                .replace(wordTag, "")
                .replace(Regex("\\s{2,}"), " ")
                .trim()
            if (text.isBlank()) return@forEach
            tags.forEach tagLoop@{ m ->
                val min = m.groupValues[1].toLongOrNull() ?: return@tagLoop
                val sec = m.groupValues[2].replace(':', '.').toDoubleOrNull() ?: return@tagLoop
                val lineMs = min * 60_000 + (sec * 1000).toLong()
                out += LyricLine((lineMs - offsetMs).coerceAtLeast(0L), text)
            }
        }
        out.sortBy { it.timeMs }
        return out
    }

    companion object {
        private const val MAX_DIFF_S = 20.0
        // Quality floor for timestamped lyrics (was 65: too many usable sheets were thrown away).
        private const val MIN_SCORE = 40
        private const val MISS_TTL_MS = 10 * 60 * 1000L   // a real miss: retry after 10 minutes
        private const val MISS_FAILED_MS = 20 * 1000L      // all providers unreachable: retry after 20 s
        private const val OVERALL_DEADLINE_MS = 9_000L     // total wait for all parallel providers
        // Protect heap usage from malformed or unusually long lyric responses.
        private const val MAX_RESPONSE_CHARS = 2_000_000
        private const val MAX_CACHED_LINES = 600
        // Low-End: 240 lines covers essentially every real song lyric; only guards pathological ones.
        private const val MAX_CACHED_LINES_LOW_END = 240
    }
}
