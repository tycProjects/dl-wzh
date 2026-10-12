package com.zychocuwek.musify.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Klien untuk backend Musify (folder api/ pada proyek web). */
object Api {
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val searchCache = object : LinkedHashMap<String, SearchResult>(32, .75f, true) {
        override fun removeEldestEntry(e: MutableMap.MutableEntry<String, SearchResult>?) = size > 40
    }

    /** GET JSON (blocking). Dipanggil dari coroutine IO atau thread loader ExoPlayer. */
    fun getJsonBlocking(path: String, vararg q: Pair<String, String>): JSONObject {
        val base = Store.baseUrl
        if (base.isBlank() || base.contains("GANTI-DENGAN")) throw IOException("URL server belum diatur (Profil > Server)")
        val url = (base + path).toHttpUrl().newBuilder().apply { q.forEach { addQueryParameter(it.first, it.second) } }.build()
        http.newCall(Request.Builder().url(url).header("Accept", "application/json").build()).execute().use { r ->
            val body = r.body?.string().orEmpty()
            return try { JSONObject(body) } catch (e: Exception) { throw IOException("Respons server tidak valid (${r.code})") }
        }
    }

    private suspend fun getJson(path: String, vararg q: Pair<String, String>) =
        withContext(Dispatchers.IO) { getJsonBlocking(path, *q) }

    // ---------------- Pencarian ----------------
    suspend fun search(query: String, type: String = "all"): SearchResult {
        val key = "$type|${query.lowercase()}"
        synchronized(searchCache) { searchCache[key] }?.let { return it }
        val r = getJson("/api/search", "query" to query, "type" to type).optJSONObject("result")
            ?: return SearchResult(emptyList(), emptyList(), emptyList(), emptyList())
        val res = SearchResult(
            songs = r.arr("songs").objs().map {
                Song(it.optString("videoId"), it.optString("title"), it.optString("artist"), it.optString("artistId"),
                    it.optString("albumId"), it.optString("thumbnail"), it.optString("duration"))
            }.filter { it.videoId.isNotBlank() },
            albums = r.arr("albums").objs().map { Card(it.optString("id"), it.optString("title"), listOf(it.optString("artist"), it.optString("year")).filter { s -> s.isNotBlank() }.joinToString(" • "), it.optString("cover")) },
            playlists = r.arr("playlists").objs().map { Card(it.optString("id"), it.optString("title"), it.optString("artist"), it.optString("cover")) },
            artists = r.arr("artists").objs().map { Card(it.optString("id"), it.optString("title"), it.optString("artist"), it.optString("cover")) }
        )
        synchronized(searchCache) { searchCache[key] = res }
        return res
    }

    suspend fun suggest(q: String): List<String> = try {
        val base = Store.baseUrl
        val url = (base + "/api/suggest").toHttpUrl().newBuilder().addQueryParameter("q", q).build()
        withContext(Dispatchers.IO) {
            http.newCall(Request.Builder().url(url).build()).execute().use { r ->
                val a = JSONArray(r.body?.string().orEmpty())
                (0 until a.length()).mapNotNull { i ->
                    when (val v = a.get(i)) { is String -> v; is JSONObject -> v.optString("text").ifBlank { v.optString("query") }; else -> null }
                }.filter { it.isNotBlank() }.take(8)
            }
        }
    } catch (e: Exception) { emptyList() }

    // ---------------- Artis & album ----------------
    suspend fun artist(id: String): ArtistDetail? {
        val j = getJson("/api/artist", "id" to id)
        val r = j.optJSONObject("result") ?: return null
        fun cards(k: String) = r.arr(k).objs().map { Card(it.optString("browseId"), it.optString("name"), it.optString("artist"), it.thumb()) }
        return ArtistDetail(
            id = id, name = r.optString("name"), cover = r.thumb(),
            topSongs = r.arr("topSongs").objs().map { Song(it.optString("videoId"), it.optString("title"), it.optString("artist"), id, "", it.thumb()) }
                .filter { it.videoId.isNotBlank() },
            albums = cards("topAlbums"), singles = cards("topSingles"), similar = cards("similarArtists")
        )
    }

    suspend fun album(id: String): AlbumDetail? {
        val r = getJson("/api/album", "id" to id).optJSONObject("result") ?: return null
        return AlbumDetail(id, r.optString("title"), r.thumb(),
            r.arr("songs").objs().map {
                Song(it.optString("videoId"), it.optString("title"), it.optString("artist"), it.optString("artistId"), id, it.thumb(), it.optString("duration"))
            }.filter { it.videoId.isNotBlank() })
    }

    // ---------------- Lirik ----------------
    suspend fun lyrics(s: Song): Lyrics? {
        val r = getJson("/api/lyrics", "id" to s.videoId, "title" to s.title, "artist" to s.artist).optJSONObject("result") ?: return null
        val ly = r.optJSONObject("lyrics") ?: return null
        val lines = ly.arr("lines").let { a -> (0 until a.length()).map { i ->
            val o = a.optJSONObject(i)
            if (o != null) LyricLine(o.optDouble("time", -1.0), o.optString("text")) else LyricLine(-1.0, a.optString(i))
        } }.filter { it.text.isNotBlank() }
        if (lines.isEmpty()) return null
        return Lyrics(ly.optString("type") == "synced", lines)
    }

    // ---------------- Audio ----------------
    data class Resolved(val url: String, val source: String)

    /**
     * Meminta link audio ke /api/ytplay lalu membungkusnya lewat /api/proxy-audio bila perlu
     * (URL googlevideo terikat IP server, sama seperti audioSrcFor() di player.js).
     */
    fun resolveAudioBlocking(videoId: String, fresh: Boolean = false, avoid: String = ""): Resolved {
        var last: Exception? = null
        repeat(3) { attempt ->
            try {
                val args = mutableListOf("id" to videoId, "opus" to "1")
                if (fresh || attempt > 0) args += "fresh" to "1"
                if (avoid.isNotBlank()) args += "avoid" to avoid
                val dl = getJsonBlocking("/api/ytplay", *args.toTypedArray()).optJSONObject("result")?.optJSONObject("download")
                val audio = dl?.optString("audio").orEmpty()
                if (audio.isNotBlank()) {
                    val needProxy = dl!!.optBoolean("proxy") || audio.contains("googlevideo.com")
                    val url = if (needProxy) Store.baseUrl + "/api/proxy-audio?url=" + Uri.encode(audio) + "&vid=" + videoId + "&o=1" else audio
                    return Resolved(url, dl.optString("source"))
                }
                last = IOException("link audio kosong")
            } catch (e: Exception) { last = e }
            Thread.sleep(500L * (attempt + 1))
        }
        throw IOException("Gagal memuat audio: ${last?.message}")
    }

    /** Cari versi lain lagu yang sama (video asli diblokir / tanpa audio). */
    fun resolveAlternativeBlocking(title: String, artist: String, excludeId: String): Resolved {
        val j = getJsonBlocking("/api/ytplay", "query" to "$title $artist".trim(), "exclude" to excludeId, "opus" to "1")
        val dl = j.optJSONObject("result")?.optJSONObject("download")
        val audio = dl?.optString("audio").orEmpty()
        if (audio.isBlank()) throw IOException("tidak ada versi lain")
        val vid = j.optJSONObject("result")?.optString("videoId").orEmpty().ifBlank { excludeId }
        val needProxy = dl!!.optBoolean("proxy") || audio.contains("googlevideo.com")
        return Resolved(if (needProxy) Store.baseUrl + "/api/proxy-audio?url=" + Uri.encode(audio) + "&vid=" + vid + "&o=1" else audio, dl.optString("source"))
    }

    // ---------------- util JSON ----------------
    private fun JSONObject.arr(k: String): JSONArray = optJSONArray(k) ?: JSONArray()
    private fun JSONArray.objs(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
    /** Ambil thumbnail terbesar dari array "thumbnails":[{url}] */
    private fun JSONObject.thumb(): String = arr("thumbnails").objs().lastOrNull()?.optString("url").orEmpty()
}
