package com.alaoke.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Song::class, FavoriteSong::class, HistoryItem::class, Singer::class, QueueItem::class],
    version = 1,
    exportSchema = false
)
abstract class AlaokeDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun singerDao(): SingerDao
    abstract fun queueDao(): QueueDao

    companion object {
        @Volatile private var INSTANCE: AlaokeDatabase? = null

        fun getInstance(context: Context): AlaokeDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AlaokeDatabase::class.java,
                    "alaoke.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
