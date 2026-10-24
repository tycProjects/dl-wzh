package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        FavoriteSongEntity::class,
        HiddenSongEntity::class,
        SongClassificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class JnmfDatabase : RoomDatabase() {
    abstract fun jnmfDao(): JnmfDao

    companion object {
        @Volatile
        private var INSTANCE: JnmfDatabase? = null

        fun getDatabase(context: Context): JnmfDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JnmfDatabase::class.java,
                    "jnmf_music_player.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
