package com.example.data.repository

import com.example.data.local.FolderEntity
import com.example.data.local.NoteEntity
import com.example.data.local.NoteTagEntity
import com.example.data.local.SettingEntity
import com.example.data.local.TagEntity
import com.example.data.local.TumbaDatabase
import com.example.data.model.AppSettings
import com.example.data.model.BackupData
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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

    val allSettings: Flow<AppSettings> = db.settingDao().getAllSettings().map { list ->
        val settingsMap = list.associate { it.key to it.value }
        AppSettings(
            theme = settingsMap["theme"] ?: "dark",
            accent = settingsMap["accent"] ?: "silver",
            compactMode = settingsMap["compactMode"]?.toBooleanStrictOrNull() ?: false,
            fontSize = settingsMap["fontSize"] ?: "medium",
            defaultSort = settingsMap["defaultSort"] ?: "updated-desc",
            autosaveIntervalMs = settingsMap["autosaveIntervalMs"]?.toLongOrNull() ?: 800L,
            editorFontFamily = settingsMap["editorFontFamily"] ?: "sans",
            language = settingsMap["language"] ?: "uk"
        )
    }

    suspend fun createNote(
        folderId: String? = null,
        tagId: String? = null,
        isFavorite: Boolean = false
    ): Note = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val newId = "note-$now-${UUID.randomUUID().toString().take(6)}"
        val note = Note(
            id = newId,
            title = "Untitled Note",
            content = "",
            folderId = folderId,
            isFavorite = isFavorite,
            isPinned = false,
            isArchived = false,
            inTrash = false,
            trashedAt = null,
            createdAt = now,
            updatedAt = now
        )
        db.noteDao().insertNote(NoteEntity.fromDomain(note))
        if (!tagId.isNullOrEmpty()) {
            db.noteTagDao().insertNoteTags(listOf(NoteTagEntity(newId, tagId)))
        }
        note
    }

    suspend fun saveNote(note: Note) = withContext(Dispatchers.IO) {
        val toSave = note.copy(updatedAt = System.currentTimeMillis())
        db.noteDao().insertNote(NoteEntity.fromDomain(toSave))
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
        db.noteDao().deleteNoteById(noteId)
        db.noteTagDao().deleteByNoteId(noteId)
    }

    suspend fun emptyTrash(notesInTrash: List<Note>) = withContext(Dispatchers.IO) {
        val ids = notesInTrash.map { it.id }
        if (ids.isNotEmpty()) {
            db.noteDao().deleteNotesByIds(ids)
            db.noteTagDao().deleteByNoteIds(ids)
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

    suspend fun saveSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        db.settingDao().insertSetting(SettingEntity(key = key, value = value))
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        db.noteDao().clearAll()
        db.folderDao().clearAll()
        db.tagDao().clearAll()
        db.noteTagDao().clearAll()
        db.settingDao().clearAll()
    }

    suspend fun importBackup(backup: BackupData, mode: String) = withContext(Dispatchers.IO) {
        if (mode == "replace") {
            clearAllData()
        }
        // Insert folders
        db.folderDao().insertFolders(backup.folders.map { FolderEntity.fromDomain(it) })
        // Insert tags
        db.tagDao().insertTags(backup.tags.map { TagEntity.fromDomain(it) })
        // Insert notes
        db.noteDao().insertNotes(backup.notes.map { NoteEntity.fromDomain(it) })
        // Insert note tags
        val noteTags = backup.noteTags.map { NoteTagEntity(it.first, it.second) }
        db.noteTagDao().insertNoteTags(noteTags)
        // Settings
        saveSetting("theme", backup.settings.theme)
        saveSetting("accent", backup.settings.accent)
        saveSetting("compactMode", backup.settings.compactMode.toString())
        saveSetting("fontSize", backup.settings.fontSize)
        saveSetting("defaultSort", backup.settings.defaultSort)
        saveSetting("autosaveIntervalMs", backup.settings.autosaveIntervalMs.toString())
        saveSetting("editorFontFamily", backup.settings.editorFontFamily)
        saveSetting("language", backup.settings.language)
    }
}
