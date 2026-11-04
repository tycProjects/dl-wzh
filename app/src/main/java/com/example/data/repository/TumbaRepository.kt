package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AttachmentEntity
import com.example.data.local.FolderEntity
import com.example.data.local.JournalEntryEntity
import com.example.data.local.LinkEntity
import com.example.data.local.NoteEntity
import com.example.data.local.NoteRevisionEntity
import com.example.data.local.NoteTagEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.SettingEntity
import com.example.data.local.TagEntity
import com.example.data.local.TemplateEntity
import com.example.data.local.TumbaDatabase
import com.example.data.model.AppSettings
import com.example.data.model.Attachment
import com.example.data.model.BackupData
import com.example.data.model.Folder
import com.example.data.model.JournalEntry
import com.example.data.model.Note
import com.example.data.model.NoteLink
import com.example.data.model.NoteRevision
import com.example.data.model.NoteTemplate
import com.example.data.model.SearchHistoryItem
import com.example.data.model.Tag
import com.example.utils.TextFormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class TumbaRepository(private val db: TumbaDatabase) {

    val allNotes: Flow<List<Note>> = db.noteDao().getAllNotes().map { list ->
        list.map { it.toDomain() }
    }

    val allFolders: Flow<List<Folder>> = db.folderDao().getAllFolders().map { list ->
        list.map { it.toDomain() }
    }

    val allTags: Flow<List<Tag>> = db.tagDao().getAllTags().map { list ->
        list.map { it.toDomain() }
    }

    val allNoteTags: Flow<Map<String, List<String>>> = db.noteTagDao().getAllNoteTags().map { list ->
        val map = mutableMapOf<String, MutableList<String>>()
        list.forEach { item ->
            map.getOrPut(item.noteId) { mutableListOf() }.add(item.tagId)
        }
        map
    }

    val allLinks: Flow<List<NoteLink>> = db.linkDao().getAllLinks().map { list ->
        list.map { it.toDomain() }
    }

    val allTemplates: Flow<List<NoteTemplate>> = db.templateDao().getAllTemplates().map { list ->
        list.map { it.toDomain() }
    }

    val allAttachments: Flow<List<Attachment>> = db.attachmentDao().getAllAttachments().map { list ->
        list.map { it.toDomain() }
    }

    val allJournalEntries: Flow<List<JournalEntry>> = db.journalDao().getAllJournalEntries().map { list ->
        list.map { it.toDomain() }
    }

    val searchHistory: Flow<List<SearchHistoryItem>> = db.searchHistoryDao().getRecentQueries(20).map { list ->
        list.map { it.toDomain() }
    }

    val allSettings: Flow<AppSettings> = db.settingDao().getAllSettings().map { list ->
        val settingsMap = list.associate { it.key to it.value }
        AppSettings(
            theme = settingsMap["theme"] ?: "dark",
            accent = settingsMap["accent"] ?: "silver",
            compactMode = settingsMap["compactMode"]?.toBooleanStrictOrNull() ?: false,
            isGridView = settingsMap["isGridView"]?.toBooleanStrictOrNull() ?: true,
            fontSize = settingsMap["fontSize"] ?: "medium",
            defaultSort = settingsMap["defaultSort"] ?: "updated-desc",
            autosaveIntervalMs = settingsMap["autosaveIntervalMs"]?.toLongOrNull() ?: 800L,
            editorFontFamily = settingsMap["editorFontFamily"] ?: "sans",
            language = settingsMap["language"] ?: "uk",
            historyRetentionCount = settingsMap["historyRetentionCount"]?.toIntOrNull() ?: 25,
            defaultTemplateId = settingsMap["defaultTemplateId"],
            profileNickname = settingsMap["profileNickname"] ?: "TUMBA User",
            profileAvatarPath = settingsMap["profileAvatarPath"]?.takeIf { it.isNotBlank() }
        )
    }

    fun getRevisionsForNote(noteId: String): Flow<List<NoteRevision>> {
        return db.noteRevisionDao().getRevisionsForNote(noteId).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getAttachmentsForNote(noteId: String): Flow<List<Attachment>> {
        return db.attachmentDao().getAttachmentsForNote(noteId).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getIncomingLinks(targetNoteId: String): Flow<List<NoteLink>> {
        return db.linkDao().getIncomingLinks(targetNoteId).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getOutgoingLinks(sourceNoteId: String): Flow<List<NoteLink>> {
        return db.linkDao().getOutgoingLinks(sourceNoteId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun createNote(
        folderId: String? = null,
        tagId: String? = null,
        isFavorite: Boolean = false,
        initialTitle: String = "Untitled Note",
        initialContent: String = "",
        color: String = "#202124"
    ): Note = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val newId = "note-$now-${UUID.randomUUID().toString().take(6)}"
        val note = Note(
            id = newId,
            title = initialTitle,
            content = initialContent,
            folderId = folderId,
            isFavorite = isFavorite,
            isPinned = false,
            isArchived = false,
            inTrash = false,
            trashedAt = null,
            createdAt = now,
            updatedAt = now,
            color = color
        )
        db.noteDao().insertNote(NoteEntity.fromDomain(note))
        if (!tagId.isNullOrEmpty()) {
            db.noteTagDao().insertNoteTags(listOf(NoteTagEntity(newId, tagId)))
        }

        // Save initial revision if content is non-empty
        if (initialContent.isNotBlank() || initialTitle != "Untitled Note") {
            db.noteRevisionDao().insertRevision(
                NoteRevisionEntity(
                    id = "rev-$now-${UUID.randomUUID().toString().take(6)}",
                    noteId = newId,
                    title = initialTitle,
                    content = initialContent,
                    createdAt = now,
                    revisionLabel = "Initial creation"
                )
            )
        }

        // Parse any links in initial content
        updateInternalLinks(newId, initialContent)

        note
    }

    suspend fun saveNote(note: Note, createRevision: Boolean = false, label: String = "Auto Save") = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val toSave = note.copy(updatedAt = now)
        db.noteDao().insertNote(NoteEntity.fromDomain(toSave))

        // Update links
        updateInternalLinks(note.id, note.content)

        // Revision snapshot if requested or significant
        val latest = db.noteRevisionDao().getLatestRevision(note.id)
        val shouldSnapshot = createRevision || (latest == null) ||
                (latest.content != note.content && Math.abs(latest.content.length - note.content.length) >= 15) ||
                (latest.title != note.title && note.title.isNotBlank()) ||
                (now - (latest?.createdAt ?: 0L) > 60000L && latest.content != note.content)

        if (shouldSnapshot && (note.content.isNotBlank() || note.title.isNotBlank())) {
            val revId = "rev-$now-${UUID.randomUUID().toString().take(6)}"
            db.noteRevisionDao().insertRevision(
                NoteRevisionEntity(
                    id = revId,
                    noteId = note.id,
                    title = note.title,
                    content = note.content,
                    createdAt = now,
                    revisionLabel = label
                )
            )
            // Prune excess revisions to respect retention limit
            db.noteRevisionDao().pruneRevisionsForNote(note.id, 25)
        }
    }

    suspend fun restoreRevision(noteId: String, revision: NoteRevision) = withContext(Dispatchers.IO) {
        val current = db.noteDao().getNoteById(noteId) ?: return@withContext
        val restoredNote = current.copy(
            title = revision.title,
            content = revision.content,
            updatedAt = System.currentTimeMillis()
        )
        db.noteDao().insertNote(restoredNote)
        // Record the restore as a new revision so previous history isn't lost
        val now = System.currentTimeMillis()
        db.noteRevisionDao().insertRevision(
            NoteRevisionEntity(
                id = "rev-$now-${UUID.randomUUID().toString().take(6)}",
                noteId = noteId,
                title = revision.title,
                content = revision.content,
                createdAt = now,
                revisionLabel = "Restored from ${TextFormatUtils.formatDate(revision.createdAt)}"
            )
        )
        updateInternalLinks(noteId, revision.content)
    }

    private suspend fun updateInternalLinks(sourceNoteId: String, content: String) {
        val targetTitles = TextFormatUtils.extractInternalLinks(content)
        db.linkDao().deleteOutgoingLinks(sourceNoteId)

        if (targetTitles.isEmpty()) return

        val linksToInsert = mutableListOf<LinkEntity>()
        val now = System.currentTimeMillis()

        for (title in targetTitles) {
            val targetNote = db.noteDao().getNoteByExactTitle(title)
            if (targetNote != null && targetNote.id != sourceNoteId) {
                linksToInsert.add(
                    LinkEntity(
                        id = "link-${sourceNoteId}-${targetNote.id}",
                        sourceNoteId = sourceNoteId,
                        targetNoteId = targetNote.id,
                        createdAt = now
                    )
                )
            }
        }

        if (linksToInsert.isNotEmpty()) {
            db.linkDao().insertLinks(linksToInsert)
        }
    }

    suspend fun findOrCreateNoteByTitle(title: String): Note = withContext(Dispatchers.IO) {
        val existing = db.noteDao().getNoteByExactTitle(title.trim())
        if (existing != null) {
            return@withContext existing.toDomain()
        }
        createNote(initialTitle = title.trim(), initialContent = "")
    }

    suspend fun toggleFavorite(noteId: String) = withContext(Dispatchers.IO) {
        val note = db.noteDao().getNoteById(noteId) ?: return@withContext
        db.noteDao().insertNote(note.copy(isFavorite = !note.isFavorite))
    }

    suspend fun togglePin(noteId: String) = withContext(Dispatchers.IO) {
        val note = db.noteDao().getNoteById(noteId) ?: return@withContext
        db.noteDao().insertNote(note.copy(isPinned = !note.isPinned))
    }

    suspend fun duplicateNote(note: Note): Note = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val newId = "note-$now-${UUID.randomUUID().toString().take(6)}"
        val duplicated = note.copy(
            id = newId,
            title = "${note.title.ifBlank { "Untitled" }} (Copy)",
            isPinned = false,
            createdAt = now,
            updatedAt = now
        )
        db.noteDao().insertNote(NoteEntity.fromDomain(duplicated))

        // Copy note tags
        val existingTags = db.noteTagDao().getAllNoteTags()
        // Simple duplicate tag bindings
        updateInternalLinks(newId, duplicated.content)
        duplicated
    }

    suspend fun archiveNote(noteId: String) = withContext(Dispatchers.IO) {
        val note = db.noteDao().getNoteById(noteId) ?: return@withContext
        db.noteDao().insertNote(note.copy(isArchived = true, isPinned = false))
    }

    suspend fun unarchiveNote(noteId: String) = withContext(Dispatchers.IO) {
        val note = db.noteDao().getNoteById(noteId) ?: return@withContext
        db.noteDao().insertNote(note.copy(isArchived = false))
    }

    suspend fun trashNote(noteId: String) = withContext(Dispatchers.IO) {
        val note = db.noteDao().getNoteById(noteId) ?: return@withContext
        db.noteDao().insertNote(
            note.copy(
                inTrash = true,
                trashedAt = System.currentTimeMillis(),
                isPinned = false
            )
        )
    }

    suspend fun restoreNote(noteId: String) = withContext(Dispatchers.IO) {
        val note = db.noteDao().getNoteById(noteId) ?: return@withContext
        db.noteDao().insertNote(note.copy(inTrash = false, trashedAt = null))
    }

    suspend fun permanentDeleteNote(noteId: String) = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.noteDao().deleteNoteById(noteId)
            db.noteTagDao().deleteByNoteId(noteId)
            db.linkDao().deleteLinksForNote(noteId)
            db.noteRevisionDao().deleteRevisionsForNote(noteId)
            db.attachmentDao().deleteAttachmentsForNote(noteId)
        }
    }

    suspend fun emptyTrash(notesInTrash: List<Note>) = withContext(Dispatchers.IO) {
        val ids = notesInTrash.map { it.id }
        if (ids.isNotEmpty()) {
            db.withTransaction {
                db.noteDao().deleteNotesByIds(ids)
                db.noteTagDao().deleteByNoteIds(ids)
                ids.forEach { noteId ->
                    db.linkDao().deleteLinksForNote(noteId)
                    db.noteRevisionDao().deleteRevisionsForNote(noteId)
                    db.attachmentDao().deleteAttachmentsForNote(noteId)
                }
            }
        }
    }

    suspend fun moveNoteToFolder(noteId: String, folderId: String?) = withContext(Dispatchers.IO) {
        val note = db.noteDao().getNoteById(noteId) ?: return@withContext
        db.noteDao().insertNote(note.copy(folderId = folderId))
    }

    suspend fun saveFolder(folder: Folder) = withContext(Dispatchers.IO) {
        val id = folder.id.ifBlank { "folder-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(6)}" }
        db.folderDao().insertFolder(FolderEntity.fromDomain(folder.copy(id = id)))
    }

    suspend fun deleteFolder(folderId: String) = withContext(Dispatchers.IO) {
        val allFolders = db.folderDao().getAllFoldersOnce()
        val childIds = getAllChildFolderIds(folderId, allFolders)
        val allIds = listOf(folderId) + childIds

        // Reset folderId on notes that were in this folder and subfolders
        db.noteDao().resetFolderForNotes(allIds)
        // Delete folders
        db.folderDao().deleteFoldersByIds(allIds)
    }

    private fun getAllChildFolderIds(parentId: String, allFolders: List<FolderEntity>): List<String> {
        val result = mutableListOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(parentId)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val children = allFolders.filter { it.parentId == current }.map { it.id }
            result.addAll(children)
            queue.addAll(children)
        }
        return result
    }

    suspend fun saveTag(tag: Tag) = withContext(Dispatchers.IO) {
        val id = tag.id.ifBlank { "tag-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(6)}" }
        db.tagDao().insertTag(TagEntity.fromDomain(tag.copy(id = id)))
    }

    suspend fun deleteTag(tagId: String) = withContext(Dispatchers.IO) {
        db.tagDao().deleteTagById(tagId)
        db.noteTagDao().deleteByTagId(tagId)
    }

    suspend fun setNoteTags(noteId: String, tagIds: List<String>) = withContext(Dispatchers.IO) {
        db.noteTagDao().deleteByNoteId(noteId)
        val refs = tagIds.map { NoteTagEntity(noteId = noteId, tagId = it) }
        if (refs.isNotEmpty()) {
            db.noteTagDao().insertNoteTags(refs)
        }
    }

    suspend fun updateNoteColor(noteId: String, color: String) = withContext(Dispatchers.IO) {
        val note = db.noteDao().getNoteById(noteId) ?: return@withContext
        db.noteDao().insertNote(note.copy(color = color, updatedAt = System.currentTimeMillis()))
    }

    suspend fun saveSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        db.settingDao().insertSetting(SettingEntity(key = key, value = value))
    }

    suspend fun updateProfile(nickname: String, avatarPath: String?) = withContext(Dispatchers.IO) {
        saveSetting("profileNickname", nickname)
        if (avatarPath != null) {
            saveSetting("profileAvatarPath", avatarPath)
        } else {
            saveSetting("profileAvatarPath", "")
        }
    }

    // Templates
    suspend fun saveTemplate(template: NoteTemplate) = withContext(Dispatchers.IO) {
        val id = template.id.ifBlank { "tpl-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(6)}" }
        db.templateDao().insertTemplate(TemplateEntity.fromDomain(template.copy(id = id)))
    }

    suspend fun deleteTemplate(templateId: String) = withContext(Dispatchers.IO) {
        db.templateDao().deleteTemplateById(templateId)
    }

    suspend fun setDefaultTemplate(templateId: String) = withContext(Dispatchers.IO) {
        db.templateDao().setDefaultTemplate(templateId)
        saveSetting("defaultTemplateId", templateId)
    }

    suspend fun createNoteFromTemplate(template: NoteTemplate, folderId: String? = null): Note = withContext(Dispatchers.IO) {
        val title = if (template.id == "tpl-daily-journal") {
            "Daily Journal — ${TextFormatUtils.formatDate(System.currentTimeMillis())}"
        } else {
            template.title
        }
        createNote(
            folderId = folderId,
            initialTitle = title,
            initialContent = template.content
        )
    }

    // Attachments
    suspend fun addAttachment(
        noteId: String,
        fileName: String,
        mimeType: String,
        localPath: String,
        fileSize: Long
    ): Attachment = withContext(Dispatchers.IO) {
        val id = "att-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(6)}"
        val att = Attachment(
            id = id,
            noteId = noteId,
            fileName = fileName,
            mimeType = mimeType,
            localPath = localPath,
            fileSize = fileSize,
            createdAt = System.currentTimeMillis()
        )
        db.attachmentDao().insertAttachment(AttachmentEntity.fromDomain(att))
        att
    }

    suspend fun deleteAttachment(attachmentId: String) = withContext(Dispatchers.IO) {
        val att = db.attachmentDao().getAttachmentById(attachmentId)
        if (att != null) {
            try {
                val file = File(att.localPath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {}
            db.attachmentDao().deleteAttachmentById(attachmentId)
        }
    }

    // Journal
    suspend fun getOrCreateJournalNote(dateKey: String): Note = withContext(Dispatchers.IO) {
        val existingEntry = db.journalDao().getEntryForDate(dateKey)
        if (existingEntry != null) {
            val existingNote = db.noteDao().getNoteById(existingEntry.noteId)
            if (existingNote != null) {
                return@withContext existingNote.toDomain()
            }
        }

        // Find or create daily journal folder
        val journalFolder = db.folderDao().getAllFoldersOnce().find { it.name == "Daily Journal" }

        val displayDate = TextFormatUtils.formatDisplayDate(dateKey)
        val now = System.currentTimeMillis()
        val template = db.templateDao().getTemplateById("tpl-daily-journal")
        val content = template?.content ?: "## Daily Journal\nDate: $displayDate\n\n### Thoughts & Notes\n"

        val note = createNote(
            folderId = journalFolder?.id,
            initialTitle = "Daily Journal: $displayDate",
            initialContent = content
        )

        db.journalDao().insertEntry(
            JournalEntryEntity(
                dateKey = dateKey,
                noteId = note.id,
                createdAt = now,
                updatedAt = now
            )
        )
        note
    }

    // Search History
    suspend fun addSearchQuery(query: String) = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length >= 2) {
            db.searchHistoryDao().insertQuery(
                SearchHistoryEntity(
                    id = "sh-${UUID.randomUUID()}",
                    query = trimmed,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteSearchQuery(query: String) = withContext(Dispatchers.IO) {
        db.searchHistoryDao().deleteQuery(query)
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        db.searchHistoryDao().clearHistory()
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.noteDao().clearAll()
            db.folderDao().clearAll()
            db.tagDao().clearAll()
            db.noteTagDao().clearAll()
            db.settingDao().clearAll()
            db.linkDao().clearAll()
            db.noteRevisionDao().clearAll()
            db.templateDao().clearAll()
            db.attachmentDao().clearAll()
            db.journalDao().clearAll()
            db.searchHistoryDao().clearHistory()
        }
    }

    suspend fun importBackup(backup: BackupData, mode: String) = withContext(Dispatchers.IO) {
        db.withTransaction {
            if (mode == "replace") {
                clearAllData()
            }
            // Folders
            db.folderDao().insertFolders(backup.folders.map { FolderEntity.fromDomain(it) })
            // Tags
            db.tagDao().insertTags(backup.tags.map { TagEntity.fromDomain(it) })
            // Notes
            db.noteDao().insertNotes(backup.notes.map { NoteEntity.fromDomain(it) })
            // Note tags
            val noteTags = backup.noteTags.map { NoteTagEntity(it.first, it.second) }
            db.noteTagDao().insertNoteTags(noteTags)

            // Templates
            if (backup.templates.isNotEmpty()) {
                db.templateDao().insertTemplates(backup.templates.map { TemplateEntity.fromDomain(it) })
            } else {
                // Ensure default templates exist if backup didn't have them
                db.templateDao().insertTemplates(TumbaDatabase.getBuiltInTemplates())
            }

            // Revisions
            if (backup.noteRevisions.isNotEmpty()) {
                db.noteRevisionDao().insertRevisions(backup.noteRevisions.map { NoteRevisionEntity.fromDomain(it) })
            }

            // Links
            if (backup.links.isNotEmpty()) {
                db.linkDao().insertLinks(backup.links.map { LinkEntity.fromDomain(it) })
            }

            // Attachments
            if (backup.attachments.isNotEmpty()) {
                db.attachmentDao().insertAttachments(backup.attachments.map { AttachmentEntity.fromDomain(it) })
            }

            // Journal
            if (backup.journalEntries.isNotEmpty()) {
                db.journalDao().insertEntries(backup.journalEntries.map { JournalEntryEntity.fromDomain(it) })
            }

            // Settings
            saveSetting("theme", backup.settings.theme)
            saveSetting("accent", backup.settings.accent)
            saveSetting("compactMode", backup.settings.compactMode.toString())
            saveSetting("isGridView", backup.settings.isGridView.toString())
            saveSetting("fontSize", backup.settings.fontSize)
            saveSetting("defaultSort", backup.settings.defaultSort)
            saveSetting("autosaveIntervalMs", backup.settings.autosaveIntervalMs.toString())
            saveSetting("editorFontFamily", backup.settings.editorFontFamily)
            saveSetting("language", backup.settings.language)
            saveSetting("historyRetentionCount", backup.settings.historyRetentionCount.toString())
            backup.settings.defaultTemplateId?.let { saveSetting("defaultTemplateId", it) }
            saveSetting("profileNickname", backup.settings.profileNickname)
            backup.settings.profileAvatarPath?.let { saveSetting("profileAvatarPath", it) }
        }
    }
}
