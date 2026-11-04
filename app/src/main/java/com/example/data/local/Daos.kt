package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE title = :title AND inTrash = 0 LIMIT 1")
    suspend fun getNoteByExactTitle(title: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE id IN (:ids)")
    suspend fun getNotesByIds(ids: List<String>): List<NoteEntity>

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun getNoteCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deleteNotesByIds(ids: List<String>)

    @Query("UPDATE notes SET folderId = NULL WHERE folderId IN (:folderIds)")
    suspend fun resetFolderForNotes(folderIds: List<String>)

    @Query("DELETE FROM notes")
    suspend fun clearAll()
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders")
    suspend fun getAllFoldersOnce(): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<FolderEntity>)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolderById(id: String)

    @Query("DELETE FROM folders WHERE id IN (:ids)")
    suspend fun deleteFoldersByIds(ids: List<String>)

    @Query("DELETE FROM folders")
    suspend fun clearAll()
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TagEntity>)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTagById(id: String)

    @Query("DELETE FROM tags")
    suspend fun clearAll()
}

@Dao
interface NoteTagDao {
    @Query("SELECT * FROM note_tags")
    fun getAllNoteTags(): Flow<List<NoteTagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteTags(noteTags: List<NoteTagEntity>)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun deleteByNoteId(noteId: String)

    @Query("DELETE FROM note_tags WHERE tagId = :tagId")
    suspend fun deleteByTagId(tagId: String)

    @Query("DELETE FROM note_tags WHERE noteId IN (:noteIds)")
    suspend fun deleteByNoteIds(noteIds: List<String>)

    @Query("DELETE FROM note_tags")
    suspend fun clearAll()
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM settings")
    fun getAllSettings(): Flow<List<SettingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SettingEntity)

    @Query("DELETE FROM settings")
    suspend fun clearAll()
}

@Dao
interface LinkDao {
    @Query("SELECT * FROM links")
    fun getAllLinks(): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE sourceNoteId = :sourceNoteId")
    fun getOutgoingLinks(sourceNoteId: String): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE targetNoteId = :targetNoteId")
    fun getIncomingLinks(targetNoteId: String): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE sourceNoteId = :noteId OR targetNoteId = :noteId")
    suspend fun getLinksForNote(noteId: String): List<LinkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinks(links: List<LinkEntity>)

    @Query("DELETE FROM links WHERE sourceNoteId = :sourceNoteId")
    suspend fun deleteOutgoingLinks(sourceNoteId: String)

    @Query("DELETE FROM links WHERE sourceNoteId = :noteId OR targetNoteId = :noteId")
    suspend fun deleteLinksForNote(noteId: String)

    @Query("DELETE FROM links")
    suspend fun clearAll()
}

@Dao
interface NoteRevisionDao {
    @Query("SELECT * FROM note_revisions WHERE noteId = :noteId ORDER BY createdAt DESC")
    fun getRevisionsForNote(noteId: String): Flow<List<NoteRevisionEntity>>

    @Query("SELECT * FROM note_revisions WHERE noteId = :noteId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestRevision(noteId: String): NoteRevisionEntity?

    @Query("SELECT * FROM note_revisions")
    fun getAllRevisions(): Flow<List<NoteRevisionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevision(rev: NoteRevisionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevisions(revs: List<NoteRevisionEntity>)

    @Query("DELETE FROM note_revisions WHERE noteId = :noteId")
    suspend fun deleteRevisionsForNote(noteId: String)

    @Query("DELETE FROM note_revisions WHERE noteId = :noteId AND id NOT IN (SELECT id FROM note_revisions WHERE noteId = :noteId ORDER BY createdAt DESC LIMIT :keepCount)")
    suspend fun pruneRevisionsForNote(noteId: String, keepCount: Int)

    @Query("DELETE FROM note_revisions")
    suspend fun clearAll()
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY isDefault DESC, title ASC")
    fun getAllTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates")
    suspend fun getAllTemplatesOnce(): List<TemplateEntity>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getTemplateById(id: String): TemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(tpl: TemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(tpls: List<TemplateEntity>)

    @Update
    suspend fun updateTemplate(tpl: TemplateEntity)

    @Query("DELETE FROM templates WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteTemplateById(id: String)

    @Query("UPDATE templates SET isDefault = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setDefaultTemplate(id: String)

    @Query("DELETE FROM templates")
    suspend fun clearAll()
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments ORDER BY createdAt DESC")
    fun getAllAttachments(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY createdAt ASC")
    fun getAttachmentsForNote(noteId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun getAttachmentById(id: String): AttachmentEntity?

    @Query("SELECT * FROM attachments WHERE localPath = :localPath LIMIT 1")
    suspend fun getAttachmentByPath(localPath: String): AttachmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(att: AttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(atts: List<AttachmentEntity>)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteAttachmentById(id: String)

    @Query("DELETE FROM attachments WHERE noteId = :noteId")
    suspend fun deleteAttachmentsForNote(noteId: String)

    @Query("DELETE FROM attachments")
    suspend fun clearAll()
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY dateKey DESC")
    fun getAllJournalEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getEntryForDate(dateKey: String): JournalEntryEntity?

    @Query("SELECT * FROM journal_entries WHERE dateKey = :dateKey LIMIT 1")
    fun getEntryForDateFlow(dateKey: String): Flow<JournalEntryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<JournalEntryEntity>)

    @Query("DELETE FROM journal_entries WHERE dateKey = :dateKey")
    suspend fun deleteEntry(dateKey: String)

    @Query("DELETE FROM journal_entries")
    suspend fun clearAll()
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentQueries(limit: Int = 20): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuery(item: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearHistory()
}
