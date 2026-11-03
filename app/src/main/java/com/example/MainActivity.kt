package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ViewSection
import com.example.ui.components.TumbaBottomNav
import com.example.ui.components.TumbaNavigationDrawer
import com.example.ui.screens.ConfirmDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FolderDialog
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.NoteListScreen
import com.example.ui.screens.SearchDialog
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.TagDialog
import com.example.ui.theme.TumbaTheme
import com.example.ui.viewmodel.TumbaViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: TumbaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            TumbaTheme(
                themeName = settings.theme,
                accentName = settings.accent,
                language = settings.language
            ) {
                TumbaApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TumbaApp(viewModel: TumbaViewModel) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val noteTagsMap by viewModel.noteTagsMap.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val currentSection by viewModel.currentSection.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    val selectedTagId by viewModel.selectedTagId.collectAsStateWithLifecycle()
    val activeNoteId by viewModel.activeNoteId.collectAsStateWithLifecycle()
    val mobileViewMode by viewModel.mobileViewMode.collectAsStateWithLifecycle()
    val isSearchOpen by viewModel.isSearchOpen.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val folderModalState by viewModel.folderModalState.collectAsStateWithLifecycle()
    val tagModalState by viewModel.tagModalState.collectAsStateWithLifecycle()
    val confirmModalState by viewModel.confirmModalState.collectAsStateWithLifecycle()
    val saveStatus by viewModel.saveStatus.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val selectedFolder = folders.find { it.id == selectedFolderId }
    val selectedTag = tags.find { it.id == selectedTagId }
    val activeNote = notes.find { it.id == activeNoteId }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            TumbaNavigationDrawer(
                currentSection = currentSection,
                selectedFolderId = selectedFolderId,
                selectedTagId = selectedTagId,
                notes = notes,
                folders = folders,
                tags = tags,
                noteTagsMap = noteTagsMap,
                onSelectSection = { section ->
                    viewModel.selectSection(section)
                },
                onSelectFolder = { fId ->
                    viewModel.selectFolder(fId)
                },
                onSelectTag = { tId ->
                    viewModel.selectTag(tId)
                },
                onNewNote = { viewModel.createNewNote() },
                onNewFolder = { viewModel.openFolderModal(parentFolderId = null) },
                onNewTag = { viewModel.openTagModal() },
                onOpenSearch = { viewModel.openSearch() },
                onOpenSettings = { viewModel.openSettings() },
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            bottomBar = {
                if (mobileViewMode != "editor") {
                    TumbaBottomNav(
                        currentSection = currentSection,
                        onSelectSection = { sec ->
                            viewModel.selectSection(sec)
                        },
                        onOpenSettings = { viewModel.openSettings() }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (mobileViewMode) {
                    "dashboard" -> {
                        DashboardScreen(
                            notes = notes,
                            folders = folders,
                            tags = tags,
                            onSelectNote = { nId -> viewModel.selectNote(nId) },
                            onSelectFolder = { fId -> viewModel.selectFolder(fId) },
                            onSelectSection = { sec -> viewModel.selectSection(sec) },
                            onNewNote = { viewModel.createNewNote() },
                            onNewFolder = { viewModel.openFolderModal() },
                            onOpenSearch = { viewModel.openSearch() },
                            onOpenDrawer = {
                                scope.launch { drawerState.open() }
                            }
                        )
                    }

                    "folders" -> {
                        FoldersScreen(
                            folders = folders,
                            tags = tags,
                            notes = notes,
                            noteTagsMap = noteTagsMap,
                            onSelectFolder = { fId -> viewModel.selectFolder(fId) },
                            onSelectTag = { tId -> viewModel.selectTag(tId) },
                            onNewFolder = { pId -> viewModel.openFolderModal(parentFolderId = pId) },
                            onEditFolder = { folder -> viewModel.openFolderModal(folderToEdit = folder) },
                            onDeleteFolder = { folder -> viewModel.confirmDeleteFolder(folder) },
                            onNewTag = { viewModel.openTagModal() },
                            onEditTag = { tag -> viewModel.openTagModal(tagToEdit = tag) },
                            onDeleteTag = { tag -> viewModel.confirmDeleteTag(tag) },
                            onOpenDrawer = {
                                scope.launch { drawerState.open() }
                            }
                        )
                    }

                    "list" -> {
                        NoteListScreen(
                            currentSection = currentSection,
                            selectedFolder = selectedFolder,
                            selectedTag = selectedTag,
                            notes = notes,
                            folders = folders,
                            tags = tags,
                            noteTagsMap = noteTagsMap,
                            activeNoteId = activeNoteId,
                            isCompactMode = settings.compactMode,
                            onSelectNote = { nId -> viewModel.selectNote(nId) },
                            onNewNote = { viewModel.createNewNote() },
                            onToggleFavorite = { nId -> viewModel.toggleFavorite(nId) },
                            onTogglePin = { nId -> viewModel.togglePin(nId) },
                            onDuplicateNote = { n -> viewModel.duplicateNote(n) },
                            onArchiveNote = { nId -> viewModel.archiveNote(nId) },
                            onUnarchiveNote = { nId -> viewModel.unarchiveNote(nId) },
                            onTrashNote = { nId -> viewModel.trashNote(nId) },
                            onRestoreNote = { nId -> viewModel.restoreNote(nId) },
                            onPermanentDeleteNote = { n -> viewModel.confirmPermanentDelete(n) },
                            onEmptyTrash = {
                                val trashNotes = notes.filter { it.inTrash }
                                viewModel.confirmEmptyTrash(trashNotes)
                            },
                            onOpenDrawer = {
                                scope.launch { drawerState.open() }
                            }
                        )
                    }

                    "editor" -> {
                        NoteEditorScreen(
                            note = activeNote,
                            folders = folders,
                            tags = tags,
                            noteTagIds = activeNoteId?.let { noteTagsMap[it] } ?: emptyList(),
                            settings = settings,
                            saveStatus = saveStatus,
                            onBack = { viewModel.navigateBackFromEditor() },
                            onSaveNote = { updated ->
                                viewModel.debouncedSaveNote(updated, settings.autosaveIntervalMs)
                            },
                            onUpdateNoteTags = { nId, tIds ->
                                viewModel.updateNoteTags(nId, tIds)
                            },
                            onToggleFavorite = { nId -> viewModel.toggleFavorite(nId) },
                            onTogglePin = { nId -> viewModel.togglePin(nId) },
                            onDuplicateNote = { n -> viewModel.duplicateNote(n) },
                            onArchiveNote = { nId -> viewModel.archiveNote(nId) },
                            onTrashNote = { nId -> viewModel.trashNote(nId) }
                        )
                    }
                }
            }
        }
    }

    // Omni Search Dialog
    SearchDialog(
        isOpen = isSearchOpen,
        notes = notes,
        folders = folders,
        tags = tags,
        noteTagsMap = noteTagsMap,
        onSelectNote = { nId ->
            viewModel.selectNote(nId)
        },
        onClose = { viewModel.closeSearch() }
    )

    // Settings & Backup Dialog
    SettingsDialog(
        isOpen = isSettingsOpen,
        settings = settings,
        totalNotesCount = notes.size,
        totalFoldersCount = folders.size,
        onUpdateSetting = { key, value ->
            viewModel.updateSetting(key, value)
        },
        onExportBackupJson = {
            viewModel.exportBackupJson()
        },
        onImportBackupJson = { json, mode ->
            viewModel.importBackupJson(json, mode)
        },
        onClearAllData = {
            viewModel.confirmClearAllData()
        },
        onClose = { viewModel.closeSettings() }
    )

    // Folder Create/Edit Dialog
    FolderDialog(
        isOpen = folderModalState.isOpen,
        folderToEdit = folderModalState.folderToEdit,
        parentFolderId = folderModalState.parentFolderId,
        existingFolders = folders,
        onSave = { id, name, parentId, color, icon, description ->
            viewModel.saveFolder(id, name, parentId, color, icon, description)
        },
        onClose = { viewModel.dismissFolderModal() }
    )

    // Tag Create/Edit Dialog
    TagDialog(
        isOpen = tagModalState.isOpen,
        tagToEdit = tagModalState.tagToEdit,
        onSave = { id, name, color ->
            viewModel.saveTag(id, name, color)
        },
        onClose = { viewModel.dismissTagModal() }
    )

    // Confirmation Alert Dialog
    ConfirmDialog(
        isOpen = confirmModalState.isOpen,
        title = confirmModalState.title,
        message = confirmModalState.message,
        confirmLabel = confirmModalState.confirmLabel,
        isDestructive = confirmModalState.isDestructive,
        onConfirm = confirmModalState.onConfirm,
        onCancel = { viewModel.dismissConfirmModal() }
    )
}
