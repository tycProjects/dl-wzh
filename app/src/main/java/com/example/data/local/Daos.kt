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
