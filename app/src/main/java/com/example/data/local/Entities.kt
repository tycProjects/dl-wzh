package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.Tag

@Entity(
    tableName = "notes",
    indices = [Index("folderId"), Index("isFavorite"), Index("isPinned"), Index("inTrash")]
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val folderId: String?,
    val isFavorite: Boolean,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val inTrash: Boolean,
    val trashedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain() = Note(
        id = id,
        title = title,
        content = content,
        folderId = folderId,
        isFavorite = isFavorite,
        isPinned = isPinned,
        isArchived = isArchived,
        inTrash = inTrash,
        trashedAt = trashedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(note: Note) = NoteEntity(
            id = note.id,
            title = note.title,
            content = note.content,
            folderId = note.folderId,
            isFavorite = note.isFavorite,
            isPinned = note.isPinned,
            isArchived = note.isArchived,
            inTrash = note.inTrash,
            trashedAt = note.trashedAt,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt
        )
    }
}

@Entity(
    tableName = "folders",
    indices = [Index("parentId")]
)
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val parentId: String?,
    val color: String,
    val icon: String,
    val description: String,
    val createdAt: Long
) {
    fun toDomain() = Folder(
        id = id,
        name = name,
        parentId = parentId,
        color = color,
        icon = icon,
        description = description,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(folder: Folder) = FolderEntity(
            id = folder.id,
            name = folder.name,
            parentId = folder.parentId,
            color = folder.color,
            icon = folder.icon,
            description = folder.description,
            createdAt = folder.createdAt
        )
    }
}

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String
) {
    fun toDomain() = Tag(
        id = id,
        name = name,
        color = color
    )

    companion object {
        fun fromDomain(tag: Tag) = TagEntity(
            id = tag.id,
            name = tag.name,
            color = tag.color
        )
    }
}

@Entity(
    tableName = "note_tags",
    primaryKeys = ["noteId", "tagId"],
    indices = [Index("noteId"), Index("tagId")]
)
data class NoteTagEntity(
    val noteId: String,
    val tagId: String
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
