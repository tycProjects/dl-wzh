package com.example.data.model

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val folderId: String? = null,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val inTrash: Boolean = false,
    val trashedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val color: String = "#202124"
)

data class Folder(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val color: String = "#8E9096",
    val icon: String = "folder",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Tag(
    val id: String,
    val name: String,
    val color: String = "#B7B9BE"
)

data class UserProfile(
    val nickname: String = "TUMBA User",
    val avatarPath: String? = null
)

data class AppSettings(
    val theme: String = "dark", // dark (obsidian), dim (slate), light (paper)
    val accent: String = "silver", // silver, titanium, platinum, gold, ice
    val compactMode: Boolean = false,
    val isGridView: Boolean = true,
    val fontSize: String = "medium", // small, medium, large
    val defaultSort: String = "updated-desc",
    val autosaveIntervalMs: Long = 800L,
    val editorFontFamily: String = "sans", // sans, serif, mono
    val language: String = "uk", // uk, ru, en
    val historyRetentionCount: Int = 25,
    val defaultTemplateId: String? = null,
    val profileNickname: String = "TUMBA User",
    val profileAvatarPath: String? = null
)

enum class ViewSection(val id: String, val title: String) {
    HOME("home", "Home"),
    ALL("all", "All Notes"),
    FAVORITES("favorites", "Favorites"),
    RECENT("recent", "Recent"),
    ARCHIVE("archive", "Archive"),
    TRASH("trash", "Trash"),
    FOLDERS("folders", "Folders"),
    CALENDAR("calendar", "Calendar"),
    GRAPH("graph", "Graph"),
    TEMPLATES("templates", "Templates"),
    ANALYTICS("analytics", "Analytics")
}

data class NoteLink(
    val id: String,
    val sourceNoteId: String,
    val targetNoteId: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class NoteRevision(
    val id: String,
    val noteId: String,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val revisionLabel: String = "Auto Save"
)

data class NoteTemplate(
    val id: String,
    val title: String,
    val description: String = "",
    val icon: String = "file-text",
    val content: String,
    val category: String = "General",
    val isDefault: Boolean = false,
    val isBuiltIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class Attachment(
    val id: String,
    val noteId: String,
    val fileName: String,
    val mimeType: String,
    val localPath: String,
    val fileSize: Long,
    val createdAt: Long = System.currentTimeMillis()
)

data class JournalEntry(
    val dateKey: String, // "YYYY-MM-DD"
    val noteId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class SearchHistoryItem(
    val id: String,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class BackupData(
    val version: String = "2.0.0",
    val timestamp: Long = System.currentTimeMillis(),
    val notes: List<Note>,
    val folders: List<Folder>,
    val tags: List<Tag>,
    val noteTags: List<Pair<String, String>>, // noteId to tagId
    val settings: AppSettings,
    val templates: List<NoteTemplate> = emptyList(),
    val noteRevisions: List<NoteRevision> = emptyList(),
    val links: List<NoteLink> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val journalEntries: List<JournalEntry> = emptyList()
)
