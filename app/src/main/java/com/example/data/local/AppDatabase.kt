package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AppConfigEntity
import com.example.data.model.BookmarkEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.ChapterEntity
import com.example.data.model.CommentEntity
import com.example.data.model.FavoriteStoryEntity
import com.example.data.model.FollowAuthorEntity
import com.example.data.model.HighlightEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.ReadingProgressEntity
import com.example.data.model.ReviewEntity
import com.example.data.model.StoryEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        StoryEntity::class,
        ChapterEntity::class,
        CategoryEntity::class,
        BookmarkEntity::class,
        ReadingProgressEntity::class,
        HighlightEntity::class,
        CommentEntity::class,
        ReviewEntity::class,
        FavoriteStoryEntity::class,
        FollowAuthorEntity::class,
        NotificationEntity::class,
        AppConfigEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storyDao(): StoryDao
    abstract fun userDao(): UserDao
    abstract fun interactionDao(): InteractionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "golpokotha_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        DatabaseSeeder.seedInitialData(database)
                    }
                }
            }
        }
    }
}
