package com.alaoke.app.data

import com.alaoke.app.youtube.YouTubeSearchResult
import com.alaoke.app.youtube.YouTubeService
import kotlinx.coroutines.flow.Flow

class AlaokeRepository(
    private val db: AlaokeDatabase,
    private val youTubeService: YouTubeService
) {
    // ---- Song library ----
    fun observeSongs(): Flow<List<Song>> = db.songDao().observeAll()
    fun searchSongs(query: String): Flow<List<Song>> =
        if (query.isBlank()) db.songDao().observeAll() else db.songDao().search(query)

    fun songsForEvent(tag: String): Flow<List<Song>> = db.songDao().byEventTag(tag)

    suspend fun ensureSeeded() {
        if (db.songDao().count() == 0) {
            db.songDao().insertAll(SeedData.starterSongs)
        }
    }

    // ---- Favorites ----
    fun observeFavorites(): Flow<List<FavoriteSong>> = db.favoriteDao().observeAll()

    fun isFavorite(title: String, artist: String, videoId: String?): Flow<Boolean> =
        db.favoriteDao().isFavorite(title, artist, videoId)

    suspend fun toggleFavorite(title: String, artist: String, thumbnailUrl: String?, videoId: String?, currentlyFavorite: Boolean) {
        if (currentlyFavorite) {
            db.favoriteDao().removeByKey(title, artist, videoId)
        } else {
            db.favoriteDao().insert(
                FavoriteSong(songTitle = title, artist = artist, thumbnailUrl = thumbnailUrl, youtubeVideoId = videoId)
            )
        }
    }

    // ---- History ----
    fun observeHistory(): Flow<List<HistoryItem>> = db.historyDao().observeRecent()

    suspend fun recordPlayed(title: String, artist: String, thumbnailUrl: String?, videoId: String?) {
        db.historyDao().insert(
            HistoryItem(songTitle = title, artist = artist, thumbnailUrl = thumbnailUrl, youtubeVideoId = videoId)
        )
    }

    // ---- Singers ----
    fun observeSingers(): Flow<List<Singer>> = db.singerDao().observeAll()
    suspend fun addSinger(name: String) = db.singerDao().insert(Singer(name = name))
    suspend fun removeSinger(singer: Singer) = db.singerDao().delete(singer)

    // ---- Party Queue ----
    fun observeQueue(): Flow<List<QueueItem>> = db.queueDao().observeQueue()

    suspend fun addToQueue(title: String, artist: String, thumbnailUrl: String?, videoId: String?, singerName: String) {
        val nextPos = (db.queueDao().maxPosition() ?: -1) + 1
        db.queueDao().insert(
            QueueItem(
                songTitle = title,
                artist = artist,
                thumbnailUrl = thumbnailUrl,
                youtubeVideoId = videoId,
                singerName = singerName,
                position = nextPos
            )
        )
    }

    suspend fun removeFromQueue(item: QueueItem) = db.queueDao().delete(item)

    suspend fun reorderQueue(items: List<QueueItem>) {
        val reindexed = items.mapIndexed { index, item -> item.copy(position = index) }
        db.queueDao().updateAll(reindexed)
    }

    /** Marks [item] as Now Playing (clearing any previous flag) and records it in history. */
    suspend fun playNext(item: QueueItem) {
        db.queueDao().clearNowPlayingFlags()
        db.queueDao().update(item.copy(isNowPlaying = true))
        recordPlayed(item.songTitle, item.artist, item.thumbnailUrl, item.youtubeVideoId)
    }

    suspend fun clearQueue() = db.queueDao().clearAll()

    // ---- YouTube search ----
    suspend fun searchYouTube(query: String): Result<List<YouTubeSearchResult>> =
        youTubeService.search(query)
}
