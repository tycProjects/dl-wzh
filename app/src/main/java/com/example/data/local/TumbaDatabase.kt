package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        NoteEntity::class,
        FolderEntity::class,
        TagEntity::class,
        NoteTagEntity::class,
        SettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TumbaDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun noteTagDao(): NoteTagDao
    abstract fun settingDao(): SettingDao

    companion object {
        @Volatile
        private var INSTANCE: TumbaDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): TumbaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TumbaDatabase::class.java,
                    "tumba_notes.db"
                )
                    .addCallback(TumbaDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class TumbaDatabaseCallback(
        private val scope: CoroutineScope
    ) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    seedDatabase(database)
                }
            }
        }

        private suspend fun seedDatabase(database: TumbaDatabase) {
            val now = System.currentTimeMillis()

            // Folders
            val folders = listOf(
                FolderEntity("f-personal", "Personal", null, "#8E9096", "user", "Personal journal, goals, and thoughts", now - 1000000L),
                FolderEntity("f-journal", "Daily Journal", "f-personal", "#B7B9BE", "book-open", "Reflections and logs", now - 900000L),
                FolderEntity("f-ideas", "Ideas", "f-personal", "#B7B9BE", "lightbulb", "Concepts and creative sparks", now - 850000L),
                FolderEntity("f-goals", "Goals", "f-personal", "#B7B9BE", "target", "Long-term milestones", now - 800000L),
                FolderEntity("f-programming", "Programming", null, "#8E9096", "code", "Engineering notes and snippets", now - 750000L),
                FolderEntity("f-webdev", "Web Development", "f-programming", "#B7B9BE", "globe", "Frontend, backend, offline apps", now - 700000L),
                FolderEntity("f-projects", "Projects", "f-programming", "#B7B9BE", "layers", "Software project specs", now - 650000L),
                FolderEntity("f-work", "Work", null, "#8E9096", "briefcase", "Professional tasks & meetings", now - 600000L)
            )
            database.folderDao().insertFolders(folders)

            // Tags
            val tags = listOf(
                TagEntity("t-guide", "Guide", "#B7B9BE"),
                TagEntity("t-productivity", "Productivity", "#92959B"),
                TagEntity("t-engineering", "Engineering", "#8E9096"),
                TagEntity("t-offline", "Offline", "#B7B9BE"),
                TagEntity("t-checklist", "Checklist", "#92959B")
            )
            database.tagDao().insertTags(tags)

            // Notes
            val note1 = NoteEntity(
                id = "note-welcome",
                title = "Welcome to TUMBA NOTES",
                content = """
                    ## Minimalist Graphite Productivity
                    TUMBA NOTES is an uncompromising, local-first personal knowledge repository crafted with an ultra-clean monochrome aesthetic.
                    
                    ---
                    ### Core Principles
                    • **100% Offline & Private:** All notes reside securely in your device SQLite database. Zero servers, zero telemetry, zero cloud lock-in.
                    • **Instant & Responsive:** Millisecond search, instant transitions, and background autosave.
                    • **Hierarchical Organization:** Unlimited nested folders, multi-tag filtering, and quick pinning.
                    
                    ### Interactive Checklists
                    [x] Experience the Graphite monochrome palette
                    [x] 100% offline local-first storage
                    [ ] Create your first custom nested folder
                    [ ] Try full-screen focus writing mode
                    [ ] Export a complete JSON backup in Settings
                    
                    > "Simplicity is prerequisite for reliability." — Edsger W. Dijkstra
                """.trimIndent(),
                folderId = "f-journal",
                isFavorite = true,
                isPinned = true,
                isArchived = false,
                inTrash = false,
                trashedAt = null,
                createdAt = now - 300000L,
                updatedAt = now - 60000L
            )

            val note2 = NoteEntity(
                id = "note-architecture",
                title = "Offline Architecture & Local Data Safety",
                content = """
                    ## Engineered for Permanence
                    Unlike cloud note apps that charge subscriptions or suffer network outages, **TUMBA NOTES** is built around the modern *Local-First Software* paradigm.
                    
                    ```kotlin
                    // Client-side transactional persistence via Room
                    db.withTransaction {
                        noteDao.insertNote(note)
                        updateFolderCounts(note.folderId)
                    }
                    ```
                    
                    ### Key architectural safeguards:
                    1. **Atomic Transactions:** Saves and batch movements execute atomically.
                    2. **Safe Soft Deletion:** Deleting a note moves it to Trash first. Permanent deletion requires explicit confirmation.
                    3. **High-Fidelity JSON Backup:** Export your entire database anytime with intact hierarchy, timestamps, and tag linkages.
                    4. **Markdown & TXT Exports:** Single-click export of individual notes or share via Android system share.
                """.trimIndent(),
                folderId = "f-webdev",
                isFavorite = true,
                isPinned = false,
                isArchived = false,
                inTrash = false,
                trashedAt = null,
                createdAt = now - 250000L,
                updatedAt = now - 120000L
            )

            val note3 = NoteEntity(
                id = "note-sprint",
                title = "Sprint Focus: Offline First Tooling",
                content = """
                    ## Weekly Objectives & Tasks
                    Tracking the completion of high-priority milestones for local productivity.
                    
                    [x] Room SQLite schema migration and validation
                    [x] Rich text formatting toolbar (H1-H3, bold, lists, quotes, code)
                    [x] Responsive adaptive layout with graphite design
                    [ ] Folder hierarchy reorganization
                    [ ] Review backup file format against JSON schemas
                    
                    Note: Keep all client state strictly synchronized with local storage.
                """.trimIndent(),
                folderId = "f-projects",
                isFavorite = false,
                isPinned = false,
                isArchived = false,
                inTrash = false,
                trashedAt = null,
                createdAt = now - 180000L,
                updatedAt = now - 45000L
            )

            database.noteDao().insertNotes(listOf(note1, note2, note3))

            // Note tags
            val noteTags = listOf(
                NoteTagEntity("note-welcome", "t-guide"),
                NoteTagEntity("note-welcome", "t-productivity"),
                NoteTagEntity("note-welcome", "t-offline"),
                NoteTagEntity("note-architecture", "t-engineering"),
                NoteTagEntity("note-architecture", "t-offline"),
                NoteTagEntity("note-sprint", "t-checklist"),
                NoteTagEntity("note-sprint", "t-productivity")
            )
            database.noteTagDao().insertNoteTags(noteTags)
        }
    }
}
