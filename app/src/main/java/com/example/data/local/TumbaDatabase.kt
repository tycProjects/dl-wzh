package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
        SettingEntity::class,
        LinkEntity::class,
        NoteRevisionEntity::class,
        TemplateEntity::class,
        AttachmentEntity::class,
        JournalEntryEntity::class,
        SearchHistoryEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class TumbaDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun noteTagDao(): NoteTagDao
    abstract fun settingDao(): SettingDao
    abstract fun linkDao(): LinkDao
    abstract fun noteRevisionDao(): NoteRevisionDao
    abstract fun templateDao(): TemplateDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun journalDao(): JournalDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: TumbaDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Table: links
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `links` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `sourceNoteId` TEXT NOT NULL,
                        `targetNoteId` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_links_sourceNoteId` ON `links` (`sourceNoteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_links_targetNoteId` ON `links` (`targetNoteId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_links_sourceNoteId_targetNoteId` ON `links` (`sourceNoteId`, `targetNoteId`)")

                // Table: note_revisions
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `note_revisions` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `noteId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `revisionLabel` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_revisions_noteId` ON `note_revisions` (`noteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_revisions_createdAt` ON `note_revisions` (`createdAt`)")

                // Table: templates
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `templates` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `icon` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `isDefault` INTEGER NOT NULL,
                        `isBuiltIn` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // Table: attachments
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `attachments` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `noteId` TEXT NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `localPath` TEXT NOT NULL,
                        `fileSize` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_noteId` ON `attachments` (`noteId`)")

                // Table: journal_entries
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `journal_entries` (
                        `dateKey` TEXT NOT NULL PRIMARY KEY,
                        `noteId` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_entries_noteId` ON `journal_entries` (`noteId`)")

                // Table: search_history
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `search_history` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `query` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_search_history_timestamp` ON `search_history` (`timestamp`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `color` TEXT NOT NULL DEFAULT '#202124'")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): TumbaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TumbaDatabase::class.java,
                    "tumba_notes.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(TumbaDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getBuiltInTemplates(): List<TemplateEntity> {
            val now = 1700000000000L
            return listOf(
                TemplateEntity(
                    id = "tpl-daily-journal",
                    title = "Daily Journal",
                    description = "Daily reflection, gratitude, and intentions",
                    icon = "book-open",
                    content = """
                        ## Daily Reflection
                        Date: [[Today]]
                        
                        ### Gratitude
                        • What am I grateful for today?
                        
                        ### Intentions & Priorities
                        [ ] Top priority task
                        [ ] Secondary objective
                        [ ] Personal wellness goal
                        
                        ### Notes & Thoughts
                        
                        ---
                        > "Focus on the step in front of you, not the whole staircase."
                    """.trimIndent(),
                    category = "Journal",
                    isDefault = true,
                    isBuiltIn = true,
                    createdAt = now
                ),
                TemplateEntity(
                    id = "tpl-meeting-notes",
                    title = "Meeting Notes",
                    description = "Attendees, agenda, discussion points, action items",
                    icon = "users",
                    content = """
                        ## Meeting Summary
                        **Topic:** 
                        **Date & Time:** 
                        **Attendees:** 
                        
                        ---
                        ### Agenda
                        1. Context & Objectives
                        2. Key Discussion Topics
                        3. Review & Consensus
                        
                        ### Discussion Points
                        • 
                        
                        ### Action Items
                        [ ] [Assignee] Action item description
                        [ ] [Assignee] Follow-up task
                    """.trimIndent(),
                    category = "Work",
                    isDefault = false,
                    isBuiltIn = true,
                    createdAt = now + 1
                ),
                TemplateEntity(
                    id = "tpl-project-planning",
                    title = "Project Planning",
                    description = "Scope, milestones, technical design, and deadlines",
                    icon = "layers",
                    content = """
                        ## Project Overview
                        **Goal:** 
                        **Target Completion:** 
                        
                        ### Scope & Deliverables
                        • In-Scope:
                        • Out-of-Scope:
                        
                        ### Architecture & Design
                        ```
                        [Client] <---> [Local SQLite Engine] <---> [Storage Access Framework]
                        ```
                        
                        ### Milestones
                        [ ] Milestone 1: Specifications & Schema
                        [ ] Milestone 2: Implementation & Tests
                        [ ] Milestone 3: QA & Verification
                    """.trimIndent(),
                    category = "Work",
                    isDefault = false,
                    isBuiltIn = true,
                    createdAt = now + 2
                ),
                TemplateEntity(
                    id = "tpl-programming-notes",
                    title = "Programming Notes",
                    description = "Architecture notes, code snippets, and debug log",
                    icon = "code",
                    content = """
                        ## Engineering Snippet / Problem
                        **Domain:** Kotlin / Jetpack Compose
                        
                        ### Problem Statement
                        
                        ### Implementation
                        ```kotlin
                        suspend fun executeSafely() = withContext(Dispatchers.IO) {
                            // atomic operation
                        }
                        ```
                        
                        ### References & Tradeoffs
                        • Performance considerations:
                        • Memory footprint:
                    """.trimIndent(),
                    category = "Engineering",
                    isDefault = false,
                    isBuiltIn = true,
                    createdAt = now + 3
                ),
                TemplateEntity(
                    id = "tpl-study-notes",
                    title = "Study Notes",
                    description = "Key concepts, summary, questions, and references",
                    icon = "graduation-cap",
                    content = """
                        ## Study Topic: 
                        **Subject / Course:** 
                        
                        ### Key Concepts
                        1. **Concept A:** Explanation
                        2. **Concept B:** Explanation
                        
                        ### Summary in Plain Words
                        
                        ### Self-Test Questions
                        - [ ] Can I explain this concept without looking at the notes?
                        - [ ] How does this relate to [[Other Subject]]?
                    """.trimIndent(),
                    category = "Personal",
                    isDefault = false,
                    isBuiltIn = true,
                    createdAt = now + 4
                ),
                TemplateEntity(
                    id = "tpl-book-notes",
                    title = "Book Notes",
                    description = "Author, summary, key takeaways, and favorite quotes",
                    icon = "bookmark",
                    content = """
                        ## Book: 
                        **Author:** 
                        **Rating:** ★★★★★
                        
                        ### Core Thesis
                        
                        ### Top 3 Takeaways
                        1. 
                        2. 
                        3. 
                        
                        ### Memorable Quotes
                        > "Quote here."
                    """.trimIndent(),
                    category = "Personal",
                    isDefault = false,
                    isBuiltIn = true,
                    createdAt = now + 5
                ),
                TemplateEntity(
                    id = "tpl-ideas",
                    title = "Ideas",
                    description = "Concept overview, problem solved, brainstorm list",
                    icon = "lightbulb",
                    content = """
                        ## Idea Concept
                        
                        ### The Problem It Solves
                        
                        ### Proposed Solution
                        
                        ### Pros & Cons
                        • Pro: 
                        • Con: 
                        
                        ### Next Actionable Experiments
                        [ ] Validate assumptions
                        [ ] Build minimal prototype
                    """.trimIndent(),
                    category = "Creative",
                    isDefault = false,
                    isBuiltIn = true,
                    createdAt = now + 6
                ),
                TemplateEntity(
                    id = "tpl-task-list",
                    title = "Task List",
                    description = "Prioritized checklist with high/medium/low buckets",
                    icon = "check-square",
                    content = """
                        ## Task Prioritization
                        
                        ### High Priority (Do Today)
                        [ ] Urgent item 1
                        [ ] Critical deliverable
                        
                        ### Medium Priority (This Week)
                        [ ] Code review
                        [ ] Documentation update
                        
                        ### Low Priority / Backlog
                        [ ] Minor polish
                    """.trimIndent(),
                    category = "Productivity",
                    isDefault = false,
                    isBuiltIn = true,
                    createdAt = now + 7
                ),
                TemplateEntity(
                    id = "tpl-research",
                    title = "Research",
                    description = "Hypothesis, methodology, findings, and conclusions",
                    icon = "file-text",
                    content = """
                        ## Research Title
                        
                        ### Hypothesis
                        
                        ### Methodology & Approach
                        
                        ### Findings & Data Points
                        • Observation 1
                        • Observation 2
                        
                        ### Conclusion & Recommendations
                    """.trimIndent(),
                    category = "Engineering",
                    isDefault = false,
                    isBuiltIn = true,
                    createdAt = now + 8
                ),
                TemplateEntity(
                    id = "tpl-blank-note",
                    title = "Blank Note",
                    description = "Clean slate for unrestricted thoughts",
                    icon = "edit",
                    content = "",
                    category = "General",
                    isDefault = false,
                    isBuiltIn = true,
                    createdAt = now + 9
                )
            )
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

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    // Check if templates need to be seeded (e.g. after migration)
                    val existing = database.templateDao().getAllTemplatesOnce()
                    if (existing.isEmpty()) {
                        database.templateDao().insertTemplates(getBuiltInTemplates())
                    }
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
                    • **Internal Links:** Connect your thoughts organically via [[Offline Architecture & Local Data Safety]] and [[Sprint Focus: Offline First Tooling]].
                    
                    ### Interactive Checklists
                    [x] Experience the Graphite monochrome palette
                    [x] 100% offline local-first storage
                    [ ] Explore the Knowledge Graph
                    [ ] Record today's Daily Journal
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
                updatedAt = now - 60000L,
                color = "#202124"
            )

            val note2 = NoteEntity(
                id = "note-architecture",
                title = "Offline Architecture & Local Data Safety",
                content = """
                    ## Engineered for Permanence
                    Unlike cloud note apps that charge subscriptions or suffer network outages, **TUMBA NOTES** is built around the modern *Local-First Software* paradigm.
                    
                    Refer back to [[Welcome to TUMBA NOTES]] for overall design guidelines.
                    
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
                updatedAt = now - 120000L,
                color = "#252D38"
            )

            val note3 = NoteEntity(
                id = "note-sprint",
                title = "Sprint Focus: Offline First Tooling",
                content = """
                    ## Weekly Objectives & Tasks
                    Tracking the completion of high-priority milestones for local productivity. Connected to [[Welcome to TUMBA NOTES]].
                    
                    [x] Room SQLite schema migration and validation
                    [x] Rich text formatting toolbar (H1-H3, bold, lists, quotes, code)
                    [x] Responsive adaptive layout with graphite design
                    [ ] Knowledge Graph visualization
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
                updatedAt = now - 45000L,
                color = "#223028"
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

            // Seed Built-In Templates
            database.templateDao().insertTemplates(getBuiltInTemplates())

            // Seed Initial Links
            val links = listOf(
                LinkEntity("l-1", "note-welcome", "note-architecture", now),
                LinkEntity("l-2", "note-welcome", "note-sprint", now),
                LinkEntity("l-3", "note-architecture", "note-welcome", now),
                LinkEntity("l-4", "note-sprint", "note-welcome", now)
            )
            database.linkDao().insertLinks(links)
        }
    }
}
