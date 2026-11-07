package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        FavoriteSongEntity::class,
        HiddenSongEntity::class,
        SongClassificationEntity::class,
        RadioScheduleEntity::class,
        RadioScheduleItemEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class JnmfDatabase : RoomDatabase() {
    abstract fun jnmfDao(): JnmfDao

    companion object {
        const val DB_NAME = "jnmf_music_player.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `playlist_tracks` ADD COLUMN `savedTitle` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `playlist_tracks` ADD COLUMN `savedArtist` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `playlist_tracks` ADD COLUMN `savedUri` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `playlist_tracks` ADD COLUMN `savedFileName` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `radio_schedules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `startTimeSecondsOfDay` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `radio_schedule_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `scheduleId` INTEGER NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        `itemType` TEXT NOT NULL,
                        `songId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `subtitle` TEXT NOT NULL,
                        `contentUri` TEXT NOT NULL,
                        `durationMs` INTEGER NOT NULL,
                        `fileName` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_radio_schedule_items_scheduleId` ON `radio_schedule_items` (`scheduleId`)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `radio_schedule_items` ADD COLUMN `gainPercent` INTEGER NOT NULL DEFAULT 100")
                db.execSQL("ALTER TABLE `radio_schedules` ADD COLUMN `isAutoScheduled` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `radio_schedules` ADD COLUMN `timeAnnouncementEnabled` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `radio_schedules` ADD COLUMN `hourlyAnnouncementEnabled` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `radio_schedules` ADD COLUMN `timeAnnouncementAudioUri` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `radio_schedules` ADD COLUMN `timeAnnouncementTitle` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `radio_schedules` ADD COLUMN `timeAnnouncementDurationMs` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `radio_schedules` ADD COLUMN `customAnnouncementMinutes` TEXT NOT NULL DEFAULT '0'")
            }
        }

        @Volatile
        private var INSTANCE: JnmfDatabase? = null

        fun getDatabase(context: Context): JnmfDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    JnmfDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    // TRUNCATE journal mode avoids multi-megabyte -wal files on low-storage devices (Redmi 10C)
                    .setJournalMode(JournalMode.TRUNCATE)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        /**
         * Closes the active singleton connection without deleting the on-disk database file,
         * allowing deterministic verification of app closure, force-stop, and restart persistence.
         */
        internal fun closeAndResetForTesting() {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } catch (_: Exception) {
                }
                INSTANCE = null
            }
        }
    }
}
