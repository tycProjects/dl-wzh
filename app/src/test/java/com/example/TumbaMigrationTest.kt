package com.example

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.example.data.local.TumbaDatabase
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TumbaMigrationTest {

    @Test
    fun testMigration1To2CreatesNewTables() {
        val tempDbFile = File.createTempFile("test_migration_", ".db")
        tempDbFile.deleteOnExit()

        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(
            androidx.test.core.app.ApplicationProvider.getApplicationContext()
        )
            .name(tempDbFile.absolutePath)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create v1 schema tables
                    db.execSQL("""
                        CREATE TABLE `notes` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `title` TEXT NOT NULL,
                            `content` TEXT NOT NULL,
                            `folderId` TEXT,
                            `isFavorite` INTEGER NOT NULL,
                            `isPinned` INTEGER NOT NULL,
                            `isArchived` INTEGER NOT NULL,
                            `inTrash` INTEGER NOT NULL,
                            `trashedAt` INTEGER,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("""
                        CREATE TABLE `folders` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `name` TEXT NOT NULL,
                            `parentId` TEXT,
                            `color` TEXT NOT NULL,
                            `icon` TEXT NOT NULL,
                            `description` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("""
                        CREATE TABLE `tags` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `name` TEXT NOT NULL,
                            `color` TEXT NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("""
                        CREATE TABLE `note_tags` (
                            `noteId` TEXT NOT NULL,
                            `tagId` TEXT NOT NULL,
                            PRIMARY KEY(`noteId`, `tagId`)
                        )
                    """.trimIndent())
                    db.execSQL("""
                        CREATE TABLE `settings` (
                            `key` TEXT NOT NULL PRIMARY KEY,
                            `value` TEXT NOT NULL
                        )
                    """.trimIndent())

                    // Insert sample v1 note
                    db.execSQL("INSERT INTO `notes` VALUES ('v1-note', 'Old Note', 'Hello', NULL, 0, 0, 0, 0, NULL, 1000, 2000)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 1 && newVersion == 2) {
                        TumbaDatabase.MIGRATION_1_2.migrate(db)
                    }
                }
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        // Execute migration from 1 to 2
        TumbaDatabase.MIGRATION_1_2.migrate(db)

        // Verify v1 data is preserved
        val cursor = db.query("SELECT * FROM notes WHERE id = 'v1-note'")
        assertTrue(cursor.moveToFirst())
        cursor.close()

        // Verify all 6 new tables exist
        val tables = listOf("links", "note_revisions", "templates", "attachments", "journal_entries", "search_history")
        for (table in tables) {
            val checkCursor = db.query("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='$table'")
            assertTrue(checkCursor.moveToFirst())
            val count = checkCursor.getInt(0)
            checkCursor.close()
            assertTrue("Table $table should exist after migration 1->2", count > 0)
        }

        db.close()
    }
}
