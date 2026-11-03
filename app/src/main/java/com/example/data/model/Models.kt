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
    val updatedAt: Long = System.currentTimeMillis()
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

data class AppSettings(
    val theme: String = "dark", // dark (obsidian), dim (slate), light (paper)
    val accent: String = "silver", // silver, titanium, platinum, gold, ice
    val compactMode: Boolean = false,
    val fontSize: String = "medium", // small, medium, large
    val defaultSort: String = "updated-desc",
    val autosaveIntervalMs: Long = 800L,
    val editorFontFamily: String = "sans", // sans, serif, mono
    val language: String = "uk" // uk, ru, en
)

enum class ViewSection(val id: String, val title: String) {
    HOME("home", "Home"),
    ALL("all", "All Notes"),
    FAVORITES("favorites", "Favorites"),
    RECENT("recent", "Recent"),
    ARCHIVE("archive", "Archive"),
    TRASH("trash", "Trash"),
    FOLDERS("folders", "Folders")
}

data class BackupData(
    val version: String = "1.0.0",
    val timestamp: Long = System.currentTimeMillis(),
    val notes: List<Note>,
    val folders: List<Folder>,
    val tags: List<Tag>,
    val noteTags: List<Pair<String, String>>, // noteId to tagId
    val settings: AppSettings
)
