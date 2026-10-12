package com.zychocuwek.musify.data

import android.content.Context
import android.content.SharedPreferences
import com.zychocuwek.musify.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Penyimpanan lokal (setara localStorage di versi web): lagu disukai, playlist, URL server. */
object Store {
    private lateinit var sp: SharedPreferences

    private val _liked = MutableStateFlow<List<Song>>(emptyList())
    val liked: StateFlow<List<Song>> = _liked
    private val _playlists = MutableStateFlow<List<UserPlaylist>>(emptyList())
    val playlists: StateFlow<List<UserPlaylist>> = _playlists

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("musify", Context.MODE_PRIVATE)
        _liked.value = readSongs(sp.getString("liked", "[]") ?: "[]")
        _playlists.value = readPlaylists()
    }

    // ---- Server ----
    var baseUrl: String
        get() = (sp.getString("base_url", null)?.takeIf { it.isNotBlank() } ?: BuildConfig.BASE_URL).trim().trimEnd('/')
        set(v) { sp.edit().putString("base_url", v.trim().trimEnd('/')).apply() }

    // ---- Disukai ----
    fun isLiked(id: String) = _liked.value.any { it.videoId == id }

    fun toggleLike(s: Song) {
        val cur = _liked.value
        _liked.value = if (cur.any { it.videoId == s.videoId }) cur.filter { it.videoId != s.videoId } else listOf(s) + cur
        sp.edit().putString("liked", songsToJson(_liked.value).toString()).apply()
    }

    // ---- Playlist (maks. 15 lagu per playlist, sama seperti versi web) ----
    fun createPlaylist(name: String) {
        val id = "pl_" + System.currentTimeMillis()
        savePlaylists(_playlists.value + UserPlaylist(id, name.trim().ifBlank { "Playlist baru" }, emptyList()))
    }

    fun deletePlaylist(id: String) = savePlaylists(_playlists.value.filter { it.id != id })

    /** @return pesan untuk toast */
    fun addToPlaylist(playlistId: String, s: Song): String {
        val pl = _playlists.value.find { it.id == playlistId } ?: return "Playlist tidak ditemukan"
        if (pl.songs.any { it.videoId == s.videoId }) return "Sudah ada di playlist"
        if (pl.songs.size >= 15) return "Playlist penuh (maks 15)"
        savePlaylists(_playlists.value.map { if (it.id == playlistId) it.copy(songs = it.songs + s) else it })
        return "Ditambahkan ke ${pl.name}"
    }

    fun removeFromPlaylist(playlistId: String, videoId: String) =
        savePlaylists(_playlists.value.map { if (it.id == playlistId) it.copy(songs = it.songs.filter { s -> s.videoId != videoId }) else it })

    private fun savePlaylists(list: List<UserPlaylist>) {
        _playlists.value = list
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("id", it.id).put("name", it.name).put("songs", songsToJson(it.songs))) }
        sp.edit().putString("playlists", arr.toString()).apply()
    }

    // ---- JSON ----
    private fun songsToJson(list: List<Song>) = JSONArray().also { a ->
        list.forEach {
            a.put(JSONObject().put("videoId", it.videoId).put("title", it.title).put("artist", it.artist)
                .put("artistId", it.artistId).put("albumId", it.albumId).put("cover", it.cover).put("duration", it.duration))
        }
    }

    private fun readSongs(json: String): List<Song> = try {
        val a = JSONArray(json)
        (0 until a.length()).map { a.getJSONObject(it) }.map {
            Song(it.optString("videoId"), it.optString("title"), it.optString("artist"), it.optString("artistId"),
                it.optString("albumId"), it.optString("cover"), it.optString("duration"))
        }.filter { it.videoId.isNotBlank() }
    } catch (e: Exception) { emptyList() }

    private fun readPlaylists(): List<UserPlaylist> = try {
        val a = JSONArray(sp.getString("playlists", "[]"))
        (0 until a.length()).map { a.getJSONObject(it) }.map {
            UserPlaylist(it.optString("id"), it.optString("name"), readSongs(it.optJSONArray("songs")?.toString() ?: "[]"))
        }
    } catch (e: Exception) { emptyList() }
}
