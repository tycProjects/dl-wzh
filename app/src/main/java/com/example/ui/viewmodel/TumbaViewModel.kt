package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TumbaDatabase
import com.example.data.model.AppSettings
import com.example.data.model.BackupData
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.Tag
import com.example.data.model.ViewSection
import com.example.data.repository.TumbaRepository
import com.example.utils.TextFormatUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FolderModalState(
    val isOpen: Boolean = false,
    val folderToEdit: Folder? = null,
    val parentFolderId: String? = null
)

data class TagModalState(
    val isOpen: Boolean = false,
    val tagToEdit: Tag? = null
)

data class ConfirmModalState(
    val isOpen: Boolean = false,
    val title: String = "",
    val message: String = "",
    val confirmLabel: String = "Confirm",
    val isDestructive: Boolean = true,
    val onConfirm: () -> Unit = {}
)

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

    val settings: StateFlow<AppSettings> = repository.allSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _currentSection = MutableStateFlow(ViewSection.HOME)
    val currentSection: StateFlow<ViewSection> = _currentSection.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

    private val _selectedTagId = MutableStateFlow<String?>(null)
    val selectedTagId: StateFlow<String?> = _selectedTagId.asStateFlow()

    private val _activeNoteId = MutableStateFlow<String?>(null)
    val activeNoteId: StateFlow<String?> = _activeNoteId.asStateFlow()

    private val _mobileViewMode = MutableStateFlow("dashboard") // dashboard, list, editor
    val mobileViewMode: StateFlow<String> = _mobileViewMode.asStateFlow()

    private val _isSearchOpen = MutableStateFlow(false)
    val isSearchOpen: StateFlow<Boolean> = _isSearchOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _folderModalState = MutableStateFlow(FolderModalState())
    val folderModalState: StateFlow<FolderModalState> = _folderModalState.asStateFlow()

    private val _tagModalState = MutableStateFlow(TagModalState())
    val tagModalState: StateFlow<TagModalState> = _tagModalState.asStateFlow()

    private val _confirmModalState = MutableStateFlow(ConfirmModalState())
    val confirmModalState: StateFlow<ConfirmModalState> = _confirmModalState.asStateFlow()

    private val _saveStatus = MutableStateFlow("saved") // saved, saving, unsaved
    val saveStatus: StateFlow<String> = _saveStatus.asStateFlow()

    private var autosaveJob: Job? = null

    fun selectSection(section: ViewSection) {
        _currentSection.value = section
        _selectedFolderId.value = null
        _selectedTagId.value = null
        _mobileViewMode.value = if (section == ViewSection.HOME) "dashboard" else if (section == ViewSection.FOLDERS) "folders" else "list"
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

    fun navigateBackFromEditor() {
        _mobileViewMode.value = if (_currentSection.value == ViewSection.HOME) "dashboard" else "list"
    }

    fun createNewNote() {
        viewModelScope.launch {
            val newNote = repository.createNote(
                folderId = _selectedFolderId.value,
                tagId = _selectedTagId.value,
                isFavorite = _currentSection.value == ViewSection.FAVORITES
            )
            _activeNoteId.value = newNote.id
            if (_currentSection.value == ViewSection.HOME) {
                _currentSection.value = ViewSection.ALL
            }
            _mobileViewMode.value = "editor"
        }
    }

    fun saveNoteImmediate(note: Note) {
        autosaveJob?.cancel()
        _saveStatus.value = "saving"
        viewModelScope.launch {
            repository.saveNote(note)
            _saveStatus.value = "saved"
        }
    }

    fun debouncedSaveNote(note: Note, intervalMs: Long = 800L) {
        autosaveJob?.cancel()
        _saveStatus.value = "saving"
        autosaveJob = viewModelScope.launch {
            delay(intervalMs)
            repository.saveNote(note)
            _saveStatus.value = "saved"
        }
    }

    fun toggleFavorite(noteId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(noteId)
        }
    }

    fun togglePin(noteId: String) {
        viewModelScope.launch {
            repository.togglePin(noteId)
        }
    }

    fun duplicateNote(note: Note) {
        viewModelScope.launch {
            val dup = repository.duplicateNote(note)
            // Duplicate tags
            val existingTags = noteTagsMap.value[note.id] ?: emptyList()
            if (existingTags.isNotEmpty()) {
                repository.setNoteTags(dup.id, existingTags)
            }
            _activeNoteId.value = dup.id
            _mobileViewMode.value = "editor"
        }
    }

    fun archiveNote(noteId: String) {
        viewModelScope.launch {
            repository.archiveNote(noteId)
        }
    }

    fun unarchiveNote(noteId: String) {
        viewModelScope.launch {
            repository.unarchiveNote(noteId)
        }
    }

    fun trashNote(noteId: String) {
        viewModelScope.launch {
            repository.trashNote(noteId)
            if (_activeNoteId.value == noteId) {
                _mobileViewMode.value = "list"
            }
        }
    }

    fun restoreNote(noteId: String) {
        viewModelScope.launch {
            repository.restoreNote(noteId)
        }
    }

    fun confirmPermanentDelete(note: Note) {
        val lang = settings.value.language
        val title = if (lang == "uk") "Видалити нотатку назавжди?" else if (lang == "ru") "Удалить заметку навсегда?" else "Permanently Delete Note?"
        val message = if (lang == "uk") "Ви впевнені, що хочете остаточно видалити «${note.title.ifBlank { "безназви" }}»? Цю дію неможливо скасувати." else if (lang == "ru") "Вы уверены, что хотите окончательно удалить «${note.title.ifBlank { "безназвания" }}»? Это действие нельзя отменить." else "Are you sure you want to permanently delete \"${note.title.ifBlank { "Untitled" }}\"? This action cannot be recovered."
        val confirmLabel = if (lang == "uk") "Видалити назавжди" else if (lang == "ru") "Удалить навсегда" else "Delete Forever"

        _confirmModalState.value = ConfirmModalState(
            isOpen = true,
            title = title,
            message = message,
            confirmLabel = confirmLabel,
            isDestructive = true,
            onConfirm = {
                viewModelScope.launch {
                    repository.permanentDeleteNote(note.id)
                    if (_activeNoteId.value == note.id) {
                        _activeNoteId.value = null
                        _mobileViewMode.value = "list"
                    }
                    dismissConfirmModal()
                }
            }
        )
    }

    fun confirmEmptyTrash(notesInTrash: List<Note>) {
        if (notesInTrash.isEmpty()) return
        val lang = settings.value.language
        val title = if (lang == "uk") "Очистити кошик?" else if (lang == "ru") "Очистить корзину?" else "Empty Trash?"
        val message = if (lang == "uk") "Остаточно видалити всі ${notesInTrash.size} нотатки з кошика? Цю дію неможливо скасувати." else if (lang == "ru") "Окончательно удалить все ${notesInTrash.size} заметки из корзины? Это действие нельзя отменить." else "Permanently delete all ${notesInTrash.size} notes currently in Trash? This cannot be undone."
        val confirmLabel = if (lang == "uk") "Очистити кошик" else if (lang == "ru") "Очистить корзину" else "Empty Trash"

        _confirmModalState.value = ConfirmModalState(
            isOpen = true,
            title = title,
            message = message,
            confirmLabel = confirmLabel,
            isDestructive = true,
            onConfirm = {
                viewModelScope.launch {
                    repository.emptyTrash(notesInTrash)
                    dismissConfirmModal()
                }
            }
        )
    }

    fun openFolderModal(folderToEdit: Folder? = null, parentFolderId: String? = null) {
        _folderModalState.value = FolderModalState(
            isOpen = true,
            folderToEdit = folderToEdit,
            parentFolderId = parentFolderId
        )
    }

    fun dismissFolderModal() {
        _folderModalState.value = FolderModalState(isOpen = false)
    }

    fun saveFolder(
        id: String?,
        name: String,
        parentId: String?,
        color: String,
        icon: String,
        description: String
    ) {
        viewModelScope.launch {
            val folder = Folder(
                id = id ?: "",
                name = name,
                parentId = parentId,
                color = color,
                icon = icon,
                description = description
            )
            repository.saveFolder(folder)
            dismissFolderModal()
        }
    }

    fun confirmDeleteFolder(folder: Folder) {
        val lang = settings.value.language
        val title = if (lang == "uk") "Видалити папку «${folder.name}»?" else if (lang == "ru") "Удалить папку «${folder.name}»?" else "Delete Folder \"${folder.name}\"?"
        val message = if (lang == "uk") "Видалення цієї папки перемістить її нотатки в корінь. Усі підпапки також будуть переміщені на рівень вище." else if (lang == "ru") "Удаление этой папки переместит её заметки в корень. Все подпапки также будут перемещены на уровень выше." else "Deleting this folder will move its notes to the root level. All subfolders will also be moved to the parent level."
        val confirmLabel = if (lang == "uk") "Видалити папку" else if (lang == "ru") "Удалить папку" else "Delete Folder"

        _confirmModalState.value = ConfirmModalState(
            isOpen = true,
            title = title,
            message = message,
            confirmLabel = confirmLabel,
            isDestructive = true,
            onConfirm = {
                viewModelScope.launch {
                    repository.deleteFolder(folder.id)
                    if (_selectedFolderId.value == folder.id) {
                        _selectedFolderId.value = null
                    }
                    dismissConfirmModal()
                }
            }
        )
    }

    fun openTagModal(tagToEdit: Tag? = null) {
        _tagModalState.value = TagModalState(
            isOpen = true,
            tagToEdit = tagToEdit
        )
    }

    fun dismissTagModal() {
        _tagModalState.value = TagModalState(isOpen = false)
    }

    fun saveTag(id: String?, name: String, color: String) {
        viewModelScope.launch {
            val cleanName = name.trim().lowercase().replace("\\s+".toRegex(), "-")
            val tag = Tag(
                id = id ?: "",
                name = cleanName,
                color = color
            )
            repository.saveTag(tag)
            dismissTagModal()
        }
    }

    fun confirmDeleteTag(tag: Tag) {
        val lang = settings.value.language
        val title = if (lang == "uk") "Видалити тег #${tag.name}?" else if (lang == "ru") "Удалить тег #${tag.name}?" else "Delete Tag #${tag.name}?"
        val message = if (lang == "uk") "Ви впевнені, що хочете видалити цей тег? Його буде видалено з усіх пов'язаних нотаток." else if (lang == "ru") "Вы уверены, что хотите удалить этот тег? Он будет удалён из всех связанных заметок." else "Are you sure you want to delete this tag? It will be removed from all associated notes."
        val confirmLabel = if (lang == "uk") "Видалити тег" else if (lang == "ru") "Удалить тег" else "Delete Tag"

        _confirmModalState.value = ConfirmModalState(
            isOpen = true,
            title = title,
            message = message,
            confirmLabel = confirmLabel,
            isDestructive = true,
            onConfirm = {
                viewModelScope.launch {
                    repository.deleteTag(tag.id)
                    if (_selectedTagId.value == tag.id) {
                        _selectedTagId.value = null
                    }
                    dismissConfirmModal()
                }
            }
        )
    }

    fun updateNoteTags(noteId: String, tagIds: List<String>) {
        viewModelScope.launch {
            repository.setNoteTags(noteId, tagIds)
        }
    }

    fun updateSetting(key: String, value: String) {
        viewModelScope.launch {
            repository.saveSetting(key, value)
        }
    }

    fun openSearch() {
        _isSearchOpen.value = true
    }

    fun closeSearch() {
        _isSearchOpen.value = false
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun dismissConfirmModal() {
        _confirmModalState.value = ConfirmModalState(isOpen = false)
    }

    fun exportBackupJson(): String {
        val currentNotes = notes.value
        val currentFolders = folders.value
        val currentTags = tags.value
        val currentNoteTags = mutableListOf<Pair<String, String>>()
        noteTagsMap.value.forEach { (noteId, tIds) ->
            tIds.forEach { tId ->
                currentNoteTags.add(Pair(noteId, tId))
            }
        }
        val currentSettings = settings.value

        val backup = BackupData(
            version = "1.0.0",
            timestamp = System.currentTimeMillis(),
            notes = currentNotes,
            folders = currentFolders,
            tags = currentTags,
            noteTags = currentNoteTags,
            settings = currentSettings
        )
        return TextFormatUtils.backupToJson(backup)
    }

    fun importBackupJson(jsonString: String, mode: String = "merge"): Result<String> {
        return try {
            val backup = TextFormatUtils.parseBackupJson(jsonString)
            viewModelScope.launch {
                repository.importBackup(backup, mode)
            }
            Result.success("Imported ${backup.notes.size} notes, ${backup.folders.size} folders, and ${backup.tags.size} tags successfully!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun confirmClearAllData() {
        val lang = settings.value.language
        val title = if (lang == "uk") "Стерти всі дані бази даних?" else if (lang == "ru") "Стереть все данные базы данных?" else "Wipe All Database Data?"
        val message = if (lang == "uk") "Ви дійсно хочете видалити всі нотатки, папки, теги та налаштування? Ця операція назавжди очищає локальне сховище." else if (lang == "ru") "Вы действительно хотите удалить все заметки, папки, теги и настройки? Эта операция навсегда очищает локальное хранилище." else "Are you completely sure you want to delete all notes, folders, tags, and settings? This operation permanently clears local SQLite storage and cannot be undone."
        val confirmLabel = if (lang == "uk") "Так, очистити все" else if (lang == "ru") "Да, очистить всё" else "Yes, Clear Everything"

        _confirmModalState.value = ConfirmModalState(
            isOpen = true,
            title = title,
            message = message,
            confirmLabel = confirmLabel,
            isDestructive = true,
            onConfirm = {
                viewModelScope.launch {
                    repository.clearAllData()
                    _activeNoteId.value = null
                    _selectedFolderId.value = null
                    _selectedTagId.value = null
                    _mobileViewMode.value = "dashboard"
                    dismissConfirmModal()
                }
            }
        )
    }
}
