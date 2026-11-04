package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.Attachment
import com.example.data.model.Folder
import com.example.data.model.JournalEntry
import com.example.data.model.Note
import com.example.data.model.NoteLink
import com.example.data.model.NoteRevision
import com.example.data.model.NoteTemplate
import com.example.data.model.SearchHistoryItem
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
    val updatedAt: Long,
    val color: String = "#202124"
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
        updatedAt = updatedAt,
        color = color
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
            updatedAt = note.updatedAt,
            color = note.color
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

@Entity(
    tableName = "links",
    indices = [
        Index("sourceNoteId"),
        Index("targetNoteId"),
        Index(value = ["sourceNoteId", "targetNoteId"], unique = true)
    ]
)
data class LinkEntity(
    @PrimaryKey val id: String,
    val sourceNoteId: String,
    val targetNoteId: String,
    val createdAt: Long
) {
    fun toDomain() = NoteLink(
        id = id,
        sourceNoteId = sourceNoteId,
        targetNoteId = targetNoteId,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(link: NoteLink) = LinkEntity(
            id = link.id,
            sourceNoteId = link.sourceNoteId,
            targetNoteId = link.targetNoteId,
            createdAt = link.createdAt
        )
    }
}

@Entity(
    tableName = "note_revisions",
    indices = [Index("noteId"), Index("createdAt")]
)
data class NoteRevisionEntity(
    @PrimaryKey val id: String,
    val noteId: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val revisionLabel: String
) {
    fun toDomain() = NoteRevision(
        id = id,
        noteId = noteId,
        title = title,
        content = content,
        createdAt = createdAt,
        revisionLabel = revisionLabel
    )

    companion object {
        fun fromDomain(rev: NoteRevision) = NoteRevisionEntity(
            id = rev.id,
            noteId = rev.noteId,
            title = rev.title,
            content = rev.content,
            createdAt = rev.createdAt,
            revisionLabel = rev.revisionLabel
        )
    }
}

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val content: String,
    val category: String,
    val isDefault: Boolean,
    val isBuiltIn: Boolean,
    val createdAt: Long
) {
    fun toDomain() = NoteTemplate(
        id = id,
        title = title,
        description = description,
        icon = icon,
        content = content,
        category = category,
        isDefault = isDefault,
        isBuiltIn = isBuiltIn,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(tpl: NoteTemplate) = TemplateEntity(
            id = tpl.id,
            title = tpl.title,
            description = tpl.description,
            icon = tpl.icon,
            content = tpl.content,
            category = tpl.category,
            isDefault = tpl.isDefault,
            isBuiltIn = tpl.isBuiltIn,
            createdAt = tpl.createdAt
        )
    }
}

@Entity(
    tableName = "attachments",
    indices = [Index("noteId")]
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val noteId: String,
    val fileName: String,
    val mimeType: String,
    val localPath: String,
    val fileSize: Long,
    val createdAt: Long
) {
    fun toDomain() = Attachment(
        id = id,
        noteId = noteId,
        fileName = fileName,
        mimeType = mimeType,
        localPath = localPath,
        fileSize = fileSize,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(att: Attachment) = AttachmentEntity(
            id = att.id,
            noteId = att.noteId,
            fileName = att.fileName,
            mimeType = att.mimeType,
            localPath = att.localPath,
            fileSize = att.fileSize,
            createdAt = att.createdAt
        )
    }
}

@Entity(
    tableName = "journal_entries",
    indices = [Index("noteId")]
)
data class JournalEntryEntity(
    @PrimaryKey val dateKey: String, // "YYYY-MM-DD"
    val noteId: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain() = JournalEntry(
        dateKey = dateKey,
        noteId = noteId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(entry: JournalEntry) = JournalEntryEntity(
            dateKey = entry.dateKey,
            noteId = entry.noteId,
            createdAt = entry.createdAt,
            updatedAt = entry.updatedAt
        )
    }
}

@Entity(
    tableName = "search_history",
    indices = [Index("timestamp")]
)
data class SearchHistoryEntity(
    @PrimaryKey val id: String,
    val query: String,
    val timestamp: Long
) {
    fun toDomain() = SearchHistoryItem(
        id = id,
        query = query,
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(item: SearchHistoryItem) = SearchHistoryEntity(
            id = item.id,
            query = item.query,
            timestamp = item.timestamp
        )
    }
}
