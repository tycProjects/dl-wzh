package com.example.utils

import com.example.data.model.AppSettings
import com.example.data.model.BackupData
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.Tag
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

object TextFormatUtils {

    fun getWordCount(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
    }

    fun getCharCount(text: String): Int {
        return text.length
    }

    fun getReadingTime(words: Int): String {
        if (words <= 0) return "0 min"
        val minutes = ceil(words / 200.0).toInt().coerceAtLeast(1)
        return "$minutes min read"
    }

    fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val date = Date(timestamp)
        val nowCal = java.util.Calendar.getInstance()
        val dateCal = java.util.Calendar.getInstance().apply { time = date }

        val isToday = nowCal.get(java.util.Calendar.YEAR) == dateCal.get(java.util.Calendar.YEAR) &&
                nowCal.get(java.util.Calendar.DAY_OF_YEAR) == dateCal.get(java.util.Calendar.DAY_OF_YEAR)

        if (isToday) {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            return format.format(date)
        }

        val isSameYear = nowCal.get(java.util.Calendar.YEAR) == dateCal.get(java.util.Calendar.YEAR)
        val format = if (isSameYear) {
            SimpleDateFormat("MMM d", Locale.getDefault())
        } else {
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        }
        return format.format(date)
    }

    fun formatFullDateTime(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val format = SimpleDateFormat("MMM d, yyyy, HH:mm", Locale.getDefault())
        return format.format(Date(timestamp))
    }

    fun exportAsMarkdown(note: Note, folderName: String?, tagNames: List<String>): String {
        val sb = StringBuilder()
        sb.append("---\n")
        sb.append("title: \"${note.title}\"\n")
        if (!folderName.isNullOrEmpty()) {
            sb.append("folder: \"$folderName\"\n")
        }
        if (tagNames.isNotEmpty()) {
            sb.append("tags: [${tagNames.joinToString(", ") { "\"$it\"" }}]\n")
        }
        sb.append("created: \"${formatFullDateTime(note.createdAt)}\"\n")
        sb.append("updated: \"${formatFullDateTime(note.updatedAt)}\"\n")
        sb.append("---\n\n")
        sb.append("# ${note.title}\n\n")
        sb.append(note.content)
        return sb.toString()
    }

    fun exportAsPlainText(note: Note, folderName: String?, tagNames: List<String>): String {
        val sb = StringBuilder()
        sb.append(note.title).append("\n")
        sb.append("=".repeat(note.title.length.coerceAtLeast(8))).append("\n\n")
        if (!folderName.isNullOrEmpty()) {
            sb.append("Folder: $folderName\n")
        }
        if (tagNames.isNotEmpty()) {
            sb.append("Tags: ${tagNames.joinToString(", ")}\n")
        }
        sb.append("Modified: ${formatFullDateTime(note.updatedAt)}\n\n")
        sb.append("-".repeat(40)).append("\n\n")
        sb.append(note.content)
        return sb.toString()
    }

    fun toggleChecklistLine(content: String, lineIndex: Int): String {
        val lines = content.lines().toMutableList()
        var currentCheckboxCount = 0
        for (i in lines.indices) {
            val line = lines[i]
            if (line.trimStart().startsWith("[ ]") || line.trimStart().startsWith("[x]")) {
                if (currentCheckboxCount == lineIndex) {
                    val isChecked = line.trimStart().startsWith("[x]")
                    val indent = line.takeWhile { it.isWhitespace() }
                    val rest = if (isChecked) {
                        line.trimStart().removePrefix("[x]")
                    } else {
                        line.trimStart().removePrefix("[ ]")
                    }
                    lines[i] = if (isChecked) "$indent[ ]$rest" else "$indent[x]$rest"
                    break
                }
                currentCheckboxCount++
            }
        }
        return lines.joinToString("\n")
    }

    fun backupToJson(backup: BackupData): String {
        val root = JSONObject()
        root.put("version", backup.version)
        root.put("timestamp", backup.timestamp)

        // Notes
        val notesArray = JSONArray()
        for (note in backup.notes) {
            val nObj = JSONObject()
            nObj.put("id", note.id)
            nObj.put("title", note.title)
            nObj.put("content", note.content)
            nObj.put("folderId", note.folderId ?: JSONObject.NULL)
            nObj.put("isFavorite", note.isFavorite)
            nObj.put("isPinned", note.isPinned)
            nObj.put("isArchived", note.isArchived)
            nObj.put("inTrash", note.inTrash)
            nObj.put("trashedAt", note.trashedAt ?: JSONObject.NULL)
            nObj.put("createdAt", note.createdAt)
            nObj.put("updatedAt", note.updatedAt)
            notesArray.put(nObj)
        }
        root.put("notes", notesArray)

        // Folders
        val foldersArray = JSONArray()
        for (folder in backup.folders) {
            val fObj = JSONObject()
            fObj.put("id", folder.id)
            fObj.put("name", folder.name)
            fObj.put("parentId", folder.parentId ?: JSONObject.NULL)
            fObj.put("color", folder.color)
            fObj.put("icon", folder.icon)
            fObj.put("description", folder.description)
            fObj.put("createdAt", folder.createdAt)
            foldersArray.put(fObj)
        }
        root.put("folders", foldersArray)

        // Tags
        val tagsArray = JSONArray()
        for (tag in backup.tags) {
            val tObj = JSONObject()
            tObj.put("id", tag.id)
            tObj.put("name", tag.name)
            tObj.put("color", tag.color)
            tagsArray.put(tObj)
        }
        root.put("tags", tagsArray)

        // NoteTags
        val noteTagsArray = JSONArray()
        for (pair in backup.noteTags) {
            val ntObj = JSONObject()
            ntObj.put("noteId", pair.first)
            ntObj.put("tagId", pair.second)
            noteTagsArray.put(ntObj)
        }
        root.put("noteTags", noteTagsArray)

        // Settings
        val sObj = JSONObject()
        sObj.put("theme", backup.settings.theme)
        sObj.put("accent", backup.settings.accent)
        sObj.put("compactMode", backup.settings.compactMode)
        sObj.put("fontSize", backup.settings.fontSize)
        sObj.put("defaultSort", backup.settings.defaultSort)
        sObj.put("autosaveIntervalMs", backup.settings.autosaveIntervalMs)
        sObj.put("editorFontFamily", backup.settings.editorFontFamily)
        sObj.put("language", backup.settings.language)
        root.put("settings", sObj)

        return root.toString(2)
    }

    fun parseBackupJson(jsonString: String): BackupData {
        val root = JSONObject(jsonString)
        val version = root.optString("version", "1.0.0")
        val timestamp = root.optLong("timestamp", System.currentTimeMillis())

        val notesList = mutableListOf<Note>()
        val notesArray = root.optJSONArray("notes") ?: JSONArray()
        for (i in 0 until notesArray.length()) {
            val obj = notesArray.getJSONObject(i)
            notesList.add(
                Note(
                    id = obj.getString("id"),
                    title = obj.optString("title", "Untitled Note"),
                    content = obj.optString("content", ""),
                    folderId = if (obj.isNull("folderId")) null else obj.optString("folderId"),
                    isFavorite = obj.optBoolean("isFavorite", false),
                    isPinned = obj.optBoolean("isPinned", false),
                    isArchived = obj.optBoolean("isArchived", false),
                    inTrash = obj.optBoolean("inTrash", false),
                    trashedAt = if (obj.isNull("trashedAt")) null else obj.optLong("trashedAt"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        val foldersList = mutableListOf<Folder>()
        val foldersArray = root.optJSONArray("folders") ?: JSONArray()
        for (i in 0 until foldersArray.length()) {
            val obj = foldersArray.getJSONObject(i)
            foldersList.add(
                Folder(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    parentId = if (obj.isNull("parentId")) null else obj.optString("parentId"),
                    color = obj.optString("color", "#8E9096"),
                    icon = obj.optString("icon", "folder"),
                    description = obj.optString("description", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val tagsList = mutableListOf<Tag>()
        val tagsArray = root.optJSONArray("tags") ?: JSONArray()
        for (i in 0 until tagsArray.length()) {
            val obj = tagsArray.getJSONObject(i)
            tagsList.add(
                Tag(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    color = obj.optString("color", "#B7B9BE")
                )
            )
        }

        val noteTagsList = mutableListOf<Pair<String, String>>()
        val noteTagsArray = root.optJSONArray("noteTags") ?: JSONArray()
        for (i in 0 until noteTagsArray.length()) {
            val obj = noteTagsArray.getJSONObject(i)
            val noteId = obj.optString("noteId")
            val tagId = obj.optString("tagId")
            if (noteId.isNotEmpty() && tagId.isNotEmpty()) {
                noteTagsList.add(Pair(noteId, tagId))
            }
        }

        val settingsObj = root.optJSONObject("settings")
        val appSettings = if (settingsObj != null) {
            AppSettings(
                theme = settingsObj.optString("theme", "dark"),
                accent = settingsObj.optString("accent", "silver"),
                compactMode = settingsObj.optBoolean("compactMode", false),
                fontSize = settingsObj.optString("fontSize", "medium"),
                defaultSort = settingsObj.optString("defaultSort", "updated-desc"),
                autosaveIntervalMs = settingsObj.optLong("autosaveIntervalMs", 800L),
                editorFontFamily = settingsObj.optString("editorFontFamily", "sans"),
                language = settingsObj.optString("language", "uk")
            )
        } else {
            AppSettings()
        }

        return BackupData(
            version = version,
            timestamp = timestamp,
            notes = notesList,
            folders = foldersList,
            tags = tagsList,
            noteTags = noteTagsList,
            settings = appSettings
        )
    }
}
