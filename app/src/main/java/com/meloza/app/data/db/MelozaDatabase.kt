package com.meloza.app.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class FavoriteEntity(val trackId: Long)

data class PlayEventEntity(
    val id: Long = 0,
    val trackId: Long,
    val playedAt: Long,
)

data class PlaylistEntity(
    val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

data class PlaylistTrackEntity(
    val playlistId: Long,
    val trackId: Long,
    val position: Int,
)

class FavoriteDao(private val h: MelozaDatabase) {
    fun all(): Flow<List<FavoriteEntity>> = h.observe { db ->
        db.rawQuery("SELECT trackId FROM favorites", null).use { c ->
            buildList { while (c.moveToNext()) add(FavoriteEntity(c.getLong(0))) }
        }
    }

    suspend fun toggle(trackId: Long) {
        h.write { db ->
            val exists = db.rawQuery("SELECT 1 FROM favorites WHERE trackId = ?", arrayOf(trackId.toString()))
                .use { it.moveToFirst() }
            if (exists) {
                db.delete("favorites", "trackId = ?", arrayOf(trackId.toString()))
            } else {
                db.insertWithOnConflict(
                    "favorites", null,
                    ContentValues().apply { put("trackId", trackId) },
                    SQLiteDatabase.CONFLICT_IGNORE,
                )
            }
        }
    }
}

class PlayEventDao(private val h: MelozaDatabase) {
    suspend fun insert(event: PlayEventEntity) {
        h.write { db ->
            db.insert(
                "play_events", null,
                ContentValues().apply {
                    put("trackId", event.trackId)
                    put("playedAt", event.playedAt)
                },
            )
        }
    }

    fun recent(limit: Int): Flow<List<PlayEventEntity>> = h.observe { db ->
        db.rawQuery(
            "SELECT id, trackId, playedAt FROM play_events ORDER BY playedAt DESC LIMIT ?",
            arrayOf(limit.toString()),
        ).use { c ->
            buildList { while (c.moveToNext()) add(PlayEventEntity(c.getLong(0), c.getLong(1), c.getLong(2))) }
        }
    }

    fun count(): Flow<Int> = h.observe { db ->
        db.rawQuery("SELECT COUNT(*) FROM play_events", null).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
    }
}

class PlaylistDao(private val h: MelozaDatabase) {
    fun all(): Flow<List<PlaylistEntity>> = h.observe { db ->
        db.rawQuery("SELECT id, name, createdAt FROM playlists ORDER BY createdAt DESC", null).use { c ->
            buildList { while (c.moveToNext()) add(PlaylistEntity(c.getLong(0), c.getString(1), c.getLong(2))) }
        }
    }

    suspend fun insertPlaylist(playlist: PlaylistEntity): Long = h.write { db ->
        db.insert(
            "playlists", null,
            ContentValues().apply {
                put("name", playlist.name)
                put("createdAt", playlist.createdAt)
            },
        )
    }

    suspend fun rename(id: Long, name: String) {
        h.write { db ->
            db.update("playlists", ContentValues().apply { put("name", name) }, "id = ?", arrayOf(id.toString()))
        }
    }

    suspend fun deletePlaylist(id: Long) {
        h.write { db -> db.delete("playlists", "id = ?", arrayOf(id.toString())) }
    }

    fun tracks(playlistId: Long): Flow<List<PlaylistTrackEntity>> = h.observe { db ->
        db.rawQuery(
            "SELECT playlistId, trackId, position FROM playlist_tracks WHERE playlistId = ? ORDER BY position ASC",
            arrayOf(playlistId.toString()),
        ).use { c ->
            buildList { while (c.moveToNext()) add(PlaylistTrackEntity(c.getLong(0), c.getLong(1), c.getInt(2))) }
        }
    }

    suspend fun nextPosition(playlistId: Long): Int = h.write { db ->
        db.rawQuery(
            "SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlistId = ?",
            arrayOf(playlistId.toString()),
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
    }

    suspend fun insertTrack(entry: PlaylistTrackEntity) {
        h.write { db -> putTrack(db, entry) }
    }

    suspend fun removeTrack(playlistId: Long, trackId: Long) {
        h.write { db ->
            db.delete(
                "playlist_tracks", "playlistId = ? AND trackId = ?",
                arrayOf(playlistId.toString(), trackId.toString()),
            )
        }
    }

    suspend fun reorder(playlistId: Long, orderedTrackIds: List<Long>) {
        h.write { db ->
            db.beginTransaction()
            try {
                db.delete("playlist_tracks", "playlistId = ?", arrayOf(playlistId.toString()))
                orderedTrackIds.forEachIndexed { i, id -> putTrack(db, PlaylistTrackEntity(playlistId, id, i)) }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    private fun putTrack(db: SQLiteDatabase, entry: PlaylistTrackEntity) {
        db.insertWithOnConflict(
            "playlist_tracks", null,
            ContentValues().apply {
                put("playlistId", entry.playlistId)
                put("trackId", entry.trackId)
                put("position", entry.position)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }
}

class MelozaDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, "meloza.db", null, 1) {

    private val changes = MutableStateFlow(0)
    private val favorites = FavoriteDao(this)
    private val playEvents = PlayEventDao(this)
    private val playlists = PlaylistDao(this)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS favorites (trackId INTEGER NOT NULL PRIMARY KEY)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS play_events (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "trackId INTEGER NOT NULL, playedAt INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS playlists (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT NOT NULL, createdAt INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS playlist_tracks (playlistId INTEGER NOT NULL, trackId INTEGER NOT NULL, " +
                "position INTEGER NOT NULL, PRIMARY KEY (playlistId, trackId))",
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun favoriteDao(): FavoriteDao = favorites
    fun playEventDao(): PlayEventDao = playEvents
    fun playlistDao(): PlaylistDao = playlists

    internal fun <T> observe(query: (SQLiteDatabase) -> T): Flow<T> =
        changes.map { withContext(Dispatchers.IO) { query(readableDatabase) } }

    internal suspend fun <T> write(block: (SQLiteDatabase) -> T): T {
        val result = withContext(Dispatchers.IO) { block(writableDatabase) }
        changes.update { it + 1 }
        return result
    }

    companion object {
        fun create(context: Context): MelozaDatabase = MelozaDatabase(context.applicationContext)
    }
}
