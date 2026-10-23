package com.alaoke.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun observeAll(): Flow<List<Song>>

    @Query(
        "SELECT * FROM songs WHERE title LIKE '%' || :query || '%' " +
            "OR artist LIKE '%' || :query || '%' ORDER BY title ASC"
    )
    fun search(query: String): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE eventTags LIKE '%' || :tag || '%'")
    fun byEventTag(tag: String): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<Song>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: Song): Long

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun count(): Int
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteSong>>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM favorites WHERE songTitle = :title AND artist = :artist " +
            "AND (youtubeVideoId = :videoId OR (:videoId IS NULL AND youtubeVideoId IS NULL)))"
    )
    fun isFavorite(title: String, artist: String, videoId: String?): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteSong)

    @Query(
        "DELETE FROM favorites WHERE songTitle = :title AND artist = :artist " +
            "AND (youtubeVideoId = :videoId OR (:videoId IS NULL AND youtubeVideoId IS NULL))"
    )
    suspend fun removeByKey(title: String, artist: String, videoId: String?)

    @Delete
    suspend fun delete(favorite: FavoriteSong)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT 50")
    fun observeRecent(): Flow<List<HistoryItem>>

    @Insert
    suspend fun insert(item: HistoryItem)

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Dao
interface SingerDao {
    @Query("SELECT * FROM singers ORDER BY name ASC")
    fun observeAll(): Flow<List<Singer>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(singer: Singer): Long

    @Delete
    suspend fun delete(singer: Singer)
}

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue_items ORDER BY position ASC")
    fun observeQueue(): Flow<List<QueueItem>>

    @Insert
    suspend fun insert(item: QueueItem): Long

    @Update
    suspend fun update(item: QueueItem)

    @Update
    suspend fun updateAll(items: List<QueueItem>)

    @Delete
    suspend fun delete(item: QueueItem)

    @Query("SELECT MAX(position) FROM queue_items")
    suspend fun maxPosition(): Int?

    @Query("SELECT * FROM queue_items WHERE isNowPlaying = 1 LIMIT 1")
    suspend fun currentNowPlaying(): QueueItem?

    @Query("UPDATE queue_items SET isNowPlaying = 0")
    suspend fun clearNowPlayingFlags()

    @Query("DELETE FROM queue_items")
    suspend fun clearAll()
}
