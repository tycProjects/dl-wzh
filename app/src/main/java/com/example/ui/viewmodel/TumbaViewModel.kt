package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.data.model.ViewSection
import com.example.data.repository.TumbaRepository
import com.example.utils.TextFormatUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

data class FolderModalState(
    val isOpen: Boolean = false,
    val folderToEdit: Folder? = null,
    val parentFolderId: String? = null
)

data class TagModalState(
    val isOpen: Boolean = false,
    val tagToEdit: Tag? = null
)

data class TemplateModalState(
    val isOpen: Boolean = false,
    val templateToEdit: NoteTemplate? = null
)

data class ConfirmModalState(
    val isOpen: Boolean = false,
    val title: String = "",
    val message: String = "",
    val confirmLabel: String = "Confirm",
    val isDestructive: Boolean = true,
    val onConfirm: () -> Unit = {}
)

@OptIn(ExperimentalCoroutinesApi::class)
class TumbaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = TumbaDatabase.getDatabase(application, viewModelScope)
    val repository = TumbaRepository(database)

    val notes: StateFlow<List<Note>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<Folder>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<Tag>> = repository.allTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val noteTagsMap: StateFlow<Map<String, List<String>>> = repository.allNoteTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val links: StateFlow<List<NoteLink>> = repository.allLinks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates: StateFlow<List<NoteTemplate>> = repository.allTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attachments: StateFlow<List<Attachment>> = repository.allAttachments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journalEntries: StateFlow<List<JournalEntry>> = repository.allJournalEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory: StateFlow<List<SearchHistoryItem>> = repository.searchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = repository.allSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _currentSection = MutableStateFlow(ViewSection.ALL)
    val currentSection: StateFlow<ViewSection> = _currentSection.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

    private val _selectedTagId = MutableStateFlow<String?>(null)
    val selectedTagId: StateFlow<String?> = _selectedTagId.asStateFlow()

    private val _activeNoteId = MutableStateFlow<String?>(null)
    val activeNoteId: StateFlow<String?> = _activeNoteId.asStateFlow()

    private val _mobileViewMode = MutableStateFlow("list")
    val mobileViewMode: StateFlow<String> = _mobileViewMode.asStateFlow()

    private val _isSearchOpen = MutableStateFlow(false)
    val isSearchOpen: StateFlow<Boolean> = _isSearchOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isProfileOpen = MutableStateFlow(false)
    val isProfileOpen: StateFlow<Boolean> = _isProfileOpen.asStateFlow()

    private val _folderModalState = MutableStateFlow(FolderModalState())
    val folderModalState: StateFlow<FolderModalState> = _folderModalState.asStateFlow()

    private val _tagModalState = MutableStateFlow(TagModalState())
    val tagModalState: StateFlow<TagModalState> = _tagModalState.asStateFlow()

    private val _templateModalState = MutableStateFlow(TemplateModalState())
    val templateModalState: StateFlow<TemplateModalState> = _templateModalState.asStateFlow()

    private val _confirmModalState = MutableStateFlow(ConfirmModalState())
    val confirmModalState: StateFlow<ConfirmModalState> = _confirmModalState.asStateFlow()

    private val _saveStatus = MutableStateFlow("saved") // saved, saving, unsaved, error
    val saveStatus: StateFlow<String> = _saveStatus.asStateFlow()

    private var autosaveJob: Job? = null

    // Active Note Revisions
    val activeNoteRevisions: StateFlow<List<NoteRevision>> = _activeNoteId.flatMapLatest { id ->
        if (id != null) repository.getRevisionsForNote(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Note Attachments
    val activeNoteAttachments: StateFlow<List<Attachment>> = _activeNoteId.flatMapLatest { id ->
        if (id != null) repository.getAttachmentsForNote(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Note Incoming Links
    val activeNoteIncomingLinks: StateFlow<List<NoteLink>> = _activeNoteId.flatMapLatest { id ->
        if (id != null) repository.getIncomingLinks(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Note Outgoing Links
    val activeNoteOutgoingLinks: StateFlow<List<NoteLink>> = _activeNoteId.flatMapLatest { id ->
        if (id != null) repository.getOutgoingLinks(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectSection(section: ViewSection) {
        _currentSection.value = section
        _selectedFolderId.value = null
        _selectedTagId.value = null
        _mobileViewMode.value = when (section) {
            ViewSection.HOME -> "list" // Notes First!
            ViewSection.FOLDERS -> "folders"
            ViewSection.CALENDAR -> "calendar"
            ViewSection.GRAPH -> "graph"
            ViewSection.TEMPLATES -> "templates"
            ViewSection.ANALYTICS -> "analytics"
            else -> "list"
        }
    }

    fun selectFolder(folderId: String?) {
        _selectedFolderId.value = folderId
        _selectedTagId.value = null
        _currentSection.value = ViewSection.ALL
        _mobileViewMode.value = "list"
    }

    fun selectTag(tagId: String?) {
        _selectedTagId.value = tagId
        _selectedFolderId.value = null
        _currentSection.value = ViewSection.ALL
        _mobileViewMode.value = "list"
    }

    fun selectNote(noteId: String) {
        _activeNoteId.value = noteId
        _mobileViewMode.value = "editor"
    }

    fun openNoteByTitle(title: String) {
        viewModelScope.launch {
            val note = repository.findOrCreateNoteByTitle(title)
            _activeNoteId.value = note.id
            _mobileViewMode.value = "editor"
        }
    }

    fun navigateBackFromEditor() {
        _mobileViewMode.value = when (_currentSection.value) {
            ViewSection.FOLDERS -> "folders"
            ViewSection.CALENDAR -> "calendar"
            ViewSection.GRAPH -> "graph"
            ViewSection.TEMPLATES -> "templates"
            ViewSection.ANALYTICS -> "analytics"
            else -> "list"
        }
    }

    fun openProfile() { _isProfileOpen.value = true }
    fun closeProfile() { _isProfileOpen.value = false }

    fun updateProfile(nickname: String, avatarPath: String?) {
        viewModelScope.launch {
            repository.updateProfile(nickname, avatarPath)
        }
    }

    fun saveAvatarFromUri(uri: Uri, nickname: String) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            try {
                val avatarFile = File(context.filesDir, "user_avatar.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(avatarFile).use { output ->
                        input.copyTo(output)
                    }
                }
                repository.updateProfile(nickname, avatarFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleGridView() {
        viewModelScope.launch {
            val current = settings.value.isGridView
            repository.saveSetting("isGridView", (!current).toString())
        }
    }

    fun updateNoteColor(noteId: String, color: String) {
        viewModelScope.launch {
            repository.updateNoteColor(noteId, color)
        }
    }

    fun moveNoteToFolder(noteId: String, folderId: String?) {
        viewModelScope.launch {
            repository.moveNoteToFolder(noteId, folderId)
        }
    }

    fun createNewNote(folderId: String? = _selectedFolderId.value) {
        viewModelScope.launch {
            val defaultTplId = settings.value.defaultTemplateId
            val tpl = if (defaultTplId != null) templates.value.find { it.id == defaultTplId } else null
            val note = if (tpl != null) {
                repository.createNoteFromTemplate(tpl, folderId = folderId)
            } else {
                repository.createNote(folderId = folderId, tagId = _selectedTagId.value)
            }
            _activeNoteId.value = note.id
            _mobileViewMode.value = "editor"
        }
    }

    fun createNoteFromTemplate(template: NoteTemplate) {
        viewModelScope.launch {
            val note = repository.createNoteFromTemplate(template, folderId = _selectedFolderId.value)
            _activeNoteId.value = note.id
            _mobileViewMode.value = "editor"
        }
    }

    fun openDailyJournal(dateKey: String) {
        viewModelScope.launch {
            val note = repository.getOrCreateJournalNote(dateKey)
            _activeNoteId.value = note.id
            _mobileViewMode.value = "editor"
        }
    }

    fun debouncedSaveNote(note: Note, debounceMs: Long) {
        _saveStatus.value = "saving"
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(debounceMs)
            try {
                repository.saveNote(note, createRevision = false)
                _saveStatus.value = "saved"
            } catch (e: Exception) {
                _saveStatus.value = "error"
            }
        }
    }

    fun restoreRevision(revision: NoteRevision) {
        val noteId = _activeNoteId.value ?: return
        viewModelScope.launch {
            repository.restoreRevision(noteId, revision)
            _saveStatus.value = "saved"
        }
    }

    fun saveActiveNoteSnapshot(note: Note, label: String = "Manual Snapshot") {
        viewModelScope.launch {
            _saveStatus.value = "saving"
            try {
                repository.saveNote(note, createRevision = true, label = label)
                _saveStatus.value = "saved"
            } catch (e: Exception) {
                _saveStatus.value = "error"
            }
        }
    }

    fun toggleFavorite(noteId: String) {
        viewModelScope.launch { repository.toggleFavorite(noteId) }
    }

    fun togglePin(noteId: String) {
        viewModelScope.launch { repository.togglePin(noteId) }
    }

    fun duplicateNote(note: Note) {
        viewModelScope.launch {
            val duplicated = repository.duplicateNote(note)
            _activeNoteId.value = duplicated.id
            _mobileViewMode.value = "editor"
        }
    }

    fun archiveNote(noteId: String) {
        viewModelScope.launch {
            repository.archiveNote(noteId)
            if (_activeNoteId.value == noteId) {
                navigateBackFromEditor()
            }
        }
    }

    fun unarchiveNote(noteId: String) {
        viewModelScope.launch { repository.unarchiveNote(noteId) }
    }

    fun trashNote(noteId: String) {
        viewModelScope.launch {
            repository.trashNote(noteId)
            if (_activeNoteId.value == noteId) {
                navigateBackFromEditor()
            }
        }
    }

    fun restoreNote(noteId: String) {
        viewModelScope.launch { repository.restoreNote(noteId) }
    }

    fun confirmPermanentDelete(note: Note) {
        _confirmModalState.value = ConfirmModalState(
            isOpen = true,
            title = "Delete Forever?",
            message = "Permanently delete \"${note.title.ifBlank { "Untitled Note" }}\"? This action cannot be reversed.",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModelScope.launch {
                    repository.permanentDeleteNote(note.id)
                    if (_activeNoteId.value == note.id) {
                        navigateBackFromEditor()
                    }
                }
            }
        )
    }

    fun confirmEmptyTrash(notesInTrash: List<Note>) {
        if (notesInTrash.isEmpty()) return
        _confirmModalState.value = ConfirmModalState(
            isOpen = true,
            title = "Empty Trash?",
            message = "Permanently delete all ${notesInTrash.size} notes in trash? This action cannot be reversed.",
            confirmLabel = "Empty All",
            isDestructive = true,
            onConfirm = {
                viewModelScope.launch {
                    repository.emptyTrash(notesInTrash)
                }
            }
        )
    }

    fun updateNoteTags(noteId: String, tagIds: List<String>) {
        viewModelScope.launch { repository.setNoteTags(noteId, tagIds) }
    }

    // Attachments Handling via SAF
    fun addAttachmentFromUri(uri: Uri, noteId: String? = _activeNoteId.value) {
        if (noteId == null) return
        val context = getApplication<Application>()
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

                // Extract filename
                var fileName = "attachment-${System.currentTimeMillis()}"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex)
                    }
                }

                // Copy to app internal storage
                val attachmentsDir = File(context.filesDir, "attachments").apply { mkdirs() }
                val targetFile = File(attachmentsDir, "${System.currentTimeMillis()}_$fileName")

                var bytesCopied = 0L
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesCopied += read
                        }
                    }
                }

                repository.addAttachment(
                    noteId = noteId,
                    fileName = fileName,
                    mimeType = mimeType,
                    localPath = targetFile.absolutePath,
                    fileSize = bytesCopied
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteAttachment(attachment: Attachment) {
        viewModelScope.launch {
            repository.deleteAttachment(attachment.id)
        }
    }

    // Folders
    fun openFolderModal(folderToEdit: Folder? = null, parentFolderId: String? = null) {
        _folderModalState.value = FolderModalState(
            isOpen = true,
            folderToEdit = folderToEdit,
            parentFolderId = parentFolderId
        )
    }

    fun closeFolderModal() {
        _folderModalState.value = FolderModalState(isOpen = false)
    }

    fun saveFolder(folder: Folder) {
        viewModelScope.launch {
            repository.saveFolder(folder)
            closeFolderModal()
        }
    }

    fun confirmDeleteFolder(folder: Folder) {
        _confirmModalState.value = ConfirmModalState(
            isOpen = true,
            title = "Delete Folder?",
            message = "Delete \"${folder.name}\" and all nested subfolders? Notes within these folders will be moved to root without being deleted.",
            confirmLabel = "Delete Folder",
            isDestructive = true,
            onConfirm = {
                viewModelScope.launch {
                    repository.deleteFolder(folder.id)
                }
            }
        )
    }

    // Tags
    fun openTagModal(tagToEdit: Tag? = null) {
        _tagModalState.value = TagModalState(isOpen = true, tagToEdit = tagToEdit)
    }

    fun closeTagModal() {
        _tagModalState.value = TagModalState(isOpen = false)
    }

    fun saveTag(tag: Tag) {
        viewModelScope.launch {
            repository.saveTag(tag)
            closeTagModal()
        }
    }

    fun confirmDeleteTag(tag: Tag) {
        _confirmModalState.value = ConfirmModalState(
            isOpen = true,
            title = "Delete Tag?",
            message = "Remove tag \"#${tag.name}\"? It will be unlinked from all notes.",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModelScope.launch {
                    repository.deleteTag(tag.id)
                }
            }
        )
    }

    // Templates
    fun openTemplateModal(templateToEdit: NoteTemplate? = null) {
        _templateModalState.value = TemplateModalState(isOpen = true, templateToEdit = templateToEdit)
    }

    fun closeTemplateModal() {
        _templateModalState.value = TemplateModalState(isOpen = false)
    }

    fun saveTemplate(template: NoteTemplate) {
        viewModelScope.launch {
            repository.saveTemplate(template)
            closeTemplateModal()
        }
    }

    fun deleteTemplate(template: NoteTemplate) {
        if (template.isBuiltIn) return
        _confirmModalState.value = ConfirmModalState(
            isOpen = true,
            title = "Delete Template?",
            message = "Delete template \"${template.title}\"?",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModelScope.launch {
                    repository.deleteTemplate(template.id)
                }
            }
        )
    }

    fun setDefaultTemplate(templateId: String) {
        viewModelScope.launch {
            repository.setDefaultTemplate(templateId)
        }
    }

    // Settings
    fun updateTheme(theme: String) {
        viewModelScope.launch { repository.saveSetting("theme", theme) }
    }

    fun updateAccent(accent: String) {
        viewModelScope.launch { repository.saveSetting("accent", accent) }
    }

    fun updateCompactMode(compact: Boolean) {
        viewModelScope.launch { repository.saveSetting("compactMode", compact.toString()) }
    }

    fun updateFontSize(fontSize: String) {
        viewModelScope.launch { repository.saveSetting("fontSize", fontSize) }
    }

    fun updateEditorFontFamily(font: String) {
        viewModelScope.launch { repository.saveSetting("editorFontFamily", font) }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch { repository.saveSetting("language", lang) }
    }

    fun updateAutosaveInterval(ms: Long) {
        viewModelScope.launch { repository.saveSetting("autosaveIntervalMs", ms.toString()) }
    }

    fun openSettings() { _isSettingsOpen.value = true }
    fun closeSettings() { _isSettingsOpen.value = false }
    fun openSearch() { _isSearchOpen.value = true }
    fun closeSearch() { _isSearchOpen.value = false }

    fun recordSearchQuery(query: String) {
        viewModelScope.launch { repository.addSearchQuery(query) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { repository.clearSearchHistory() }
    }

    // Backup & Restore
    fun generateBackupData(): BackupData {
        val currentNotes = notes.value
        val currentFolders = folders.value
        val currentTags = tags.value
        val currentNoteTags = mutableListOf<Pair<String, String>>()
        noteTagsMap.value.forEach { (noteId, tagIds) ->
            tagIds.forEach { tagId -> currentNoteTags.add(Pair(noteId, tagId)) }
        }
        return BackupData(
            version = "2.0.0",
            timestamp = System.currentTimeMillis(),
            notes = currentNotes,
            folders = currentFolders,
            tags = currentTags,
            noteTags = currentNoteTags,
            settings = settings.value,
            templates = templates.value,
            links = links.value,
            attachments = attachments.value,
            journalEntries = journalEntries.value
        )
    }

    fun writeBackupToUri(uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val jsonString = TextFormatUtils.backupToJson(generateBackupData())
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to write backup")
            }
        }
    }

    fun readBackupFromUri(uri: Uri, mode: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                } ?: throw IllegalArgumentException("Cannot read file")

                val backup = TextFormatUtils.parseBackupJson(content)
                repository.importBackup(backup, mode)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Invalid or corrupt backup file")
            }
        }
    }

    fun importBackupFromString(json: String, mode: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val backup = TextFormatUtils.parseBackupJson(json)
                repository.importBackup(backup, mode)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Invalid JSON backup")
            }
        }
    }

    fun confirmClearAllData() {
        _confirmModalState.value = ConfirmModalState(
            isOpen = true,
            title = "Wipe All Data?",
            message = "Are you sure you want to delete ALL notes, folders, tags, attachments, and settings? This cannot be undone.",
            confirmLabel = "Wipe All Data",
            isDestructive = true,
            onConfirm = {
                viewModelScope.launch {
                    repository.clearAllData()
                    _activeNoteId.value = null
                    _selectedFolderId.value = null
                    _selectedTagId.value = null
                    _mobileViewMode.value = "dashboard"
                }
            }
        )
    }

    fun closeConfirmModal() {
        _confirmModalState.value = ConfirmModalState(isOpen = false)
    }
}
