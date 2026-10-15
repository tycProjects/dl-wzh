package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AppSettingsDao
import com.example.data.dao.KickedSongDao
import com.example.data.dao.PlaylistDao
import com.example.data.dao.SongDao
import com.example.data.entity.AppSettingsEntity
import com.example.data.entity.KickedSongEntity
import com.example.data.entity.PlaylistEntity
import com.example.data.entity.PlaylistSongCrossRef
import com.example.data.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        AppSettingsEntity::class,
        KickedSongEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun kickedSongDao(): KickedSongDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jnmf_music_player.db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
