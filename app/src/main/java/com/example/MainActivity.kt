package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Folder
import com.example.data.model.Tag
import com.example.data.model.ViewSection
import com.example.ui.components.TumbaBottomNav
import com.example.ui.components.TumbaNavigationDrawer
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.ConfirmDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FolderDialog
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.KnowledgeGraphScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.NoteListScreen
import com.example.ui.screens.ProfileDialog
import com.example.ui.screens.SearchDialog
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.TagDialog
import com.example.ui.screens.TemplateDialog
import com.example.ui.screens.TemplatesScreen
import com.example.ui.theme.TumbaTheme
import com.example.ui.viewmodel.TumbaViewModel
import com.example.utils.TextFormatUtils
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
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val noteTagsMap by viewModel.noteTagsMap.collectAsStateWithLifecycle()
    val links by viewModel.links.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val attachments by viewModel.attachments.collectAsStateWithLifecycle()
    val journalEntries by viewModel.journalEntries.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val currentSection by viewModel.currentSection.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    val selectedTagId by viewModel.selectedTagId.collectAsStateWithLifecycle()
    val activeNoteId by viewModel.activeNoteId.collectAsStateWithLifecycle()
    val mobileViewMode by viewModel.mobileViewMode.collectAsStateWithLifecycle()
    val isSearchOpen by viewModel.isSearchOpen.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val isProfileOpen by viewModel.isProfileOpen.collectAsStateWithLifecycle()
    val folderModalState by viewModel.folderModalState.collectAsStateWithLifecycle()
    val tagModalState by viewModel.tagModalState.collectAsStateWithLifecycle()
    val templateModalState by viewModel.templateModalState.collectAsStateWithLifecycle()
    val confirmModalState by viewModel.confirmModalState.collectAsStateWithLifecycle()
    val saveStatus by viewModel.saveStatus.collectAsStateWithLifecycle()

    val activeNoteRevisions by viewModel.activeNoteRevisions.collectAsStateWithLifecycle()
    val activeNoteAttachments by viewModel.activeNoteAttachments.collectAsStateWithLifecycle()
    val activeNoteIncomingLinks by viewModel.activeNoteIncomingLinks.collectAsStateWithLifecycle()
    val activeNoteOutgoingLinks by viewModel.activeNoteOutgoingLinks.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val selectedFolder = folders.find { it.id == selectedFolderId }
    val selectedTag = tags.find { it.id == selectedTagId }
    val activeNote = notes.find { it.id == activeNoteId }

    // SAF Activity Launchers
    var pendingImportMode by remember { mutableStateOf("merge") }

    val exportSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.writeBackupToUri(
                uri = uri,
                onSuccess = { Toast.makeText(context, "Backup exported successfully", Toast.LENGTH_SHORT).show() },
                onError = { err -> Toast.makeText(context, "Export error: $err", Toast.LENGTH_LONG).show() }
            )
        }
    }

    val importSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.readBackupFromUri(
                uri = uri,
                mode = pendingImportMode,
                onSuccess = { Toast.makeText(context, "Backup restored successfully", Toast.LENGTH_SHORT).show() },
                onError = { err -> Toast.makeText(context, "Import error: $err", Toast.LENGTH_LONG).show() }
            )
        }
    }

    val attachmentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.addAttachmentFromUri(uri)
        }
    }

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

                    "calendar" -> {
                        CalendarScreen(
                            notes = notes,
                            journalEntries = journalEntries,
                            onSelectNote = { nId -> viewModel.selectNote(nId) },
                            onOpenDailyJournal = { dateKey -> viewModel.openDailyJournal(dateKey) },
                            onCreateNoteForDate = { dateKey ->
                                viewModel.createNewNote()
                            },
                            onOpenDrawer = {
                                scope.launch { drawerState.open() }
                            }
                        )
                    }

                    "graph" -> {
                        KnowledgeGraphScreen(
                            notes = notes,
                            links = links,
                            folders = folders,
                            tags = tags,
                            noteTagsMap = noteTagsMap,
                            onSelectNote = { nId -> viewModel.selectNote(nId) },
                            onOpenDrawer = {
                                scope.launch { drawerState.open() }
                            }
                        )
                    }

                    "templates" -> {
                        TemplatesScreen(
                            templates = templates,
                            defaultTemplateId = settings.defaultTemplateId,
                            onCreateNoteFromTemplate = { tpl -> viewModel.createNoteFromTemplate(tpl) },
                            onNewTemplate = { viewModel.openTemplateModal() },
                            onEditTemplate = { tpl -> viewModel.openTemplateModal(templateToEdit = tpl) },
                            onDeleteTemplate = { tpl -> viewModel.deleteTemplate(tpl) },
                            onSetDefaultTemplate = { tplId -> viewModel.setDefaultTemplate(tplId) },
                            onOpenDrawer = {
                                scope.launch { drawerState.open() }
                            }
                        )
                    }

                    "analytics" -> {
                        AnalyticsScreen(
                            notes = notes,
                            folders = folders,
                            tags = tags,
                            links = links,
                            attachments = attachments,
                            noteTagsMap = noteTagsMap,
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
                            isGridView = settings.isGridView,
                            profileNickname = settings.profileNickname,
                            profileAvatarPath = settings.profileAvatarPath,
                            onSelectNote = { nId -> viewModel.selectNote(nId) },
                            onNewNote = { viewModel.createNewNote() },
                            onToggleFavorite = { nId -> viewModel.toggleFavorite(nId) },
                            onTogglePin = { nId -> viewModel.togglePin(nId) },
                            onUpdateNoteColor = { nId, color -> viewModel.updateNoteColor(nId, color) },
                            onMoveNoteToFolder = { nId, fId -> viewModel.moveNoteToFolder(nId, fId) },
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
                            onToggleGridView = { viewModel.toggleGridView() },
                            onOpenProfile = { viewModel.openProfile() },
                            onOpenSearch = { viewModel.openSearch() },
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
                            allNotes = notes,
                            noteTagIds = activeNoteId?.let { noteTagsMap[it] } ?: emptyList(),
                            attachments = activeNoteAttachments,
                            revisions = activeNoteRevisions,
                            incomingLinks = activeNoteIncomingLinks,
                            outgoingLinks = activeNoteOutgoingLinks,
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
                            onTrashNote = { nId -> viewModel.trashNote(nId) },
                            onRestoreRevision = { rev -> viewModel.restoreRevision(rev) },
                            onAddAttachment = {
                                attachmentPickerLauncher.launch(
                                    arrayOf(
                                        "image/*",
                                        "application/pdf",
                                        "text/*",
                                        "application/zip",
                                        "application/msword",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                    )
                                )
                            },
                            onDeleteAttachment = { att -> viewModel.deleteAttachment(att) },
                            onOpenLinkedNote = { title -> viewModel.openNoteByTitle(title) }
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
        attachments = attachments,
        links = links,
        searchHistory = searchHistory,
        noteTagsMap = noteTagsMap,
        onSelectNote = { nId ->
            viewModel.selectNote(nId)
        },
        onRecordQuery = { q -> viewModel.recordSearchQuery(q) },
        onClearHistory = { viewModel.clearSearchHistory() },
        onClose = { viewModel.closeSearch() }
    )

    // Settings & Backup Dialog
    SettingsDialog(
        isOpen = isSettingsOpen,
        settings = settings,
        totalNotesCount = notes.size,
        totalFoldersCount = folders.size,
        onUpdateSetting = { key, value ->
            when (key) {
                "theme" -> viewModel.updateTheme(value)
                "accent" -> viewModel.updateAccent(value)
                "compactMode" -> viewModel.updateCompactMode(value.toBoolean())
                "fontSize" -> viewModel.updateFontSize(value)
                "editorFontFamily" -> viewModel.updateEditorFontFamily(value)
                "language" -> viewModel.updateLanguage(value)
                "autosaveIntervalMs" -> viewModel.updateAutosaveInterval(value.toLongOrNull() ?: 800L)
            }
        },
        onExportBackupJson = {
            TextFormatUtils.backupToJson(viewModel.generateBackupData())
        },
        onImportBackupJson = { json, mode ->
            var result: Result<String> = Result.success("Import completed")
            viewModel.importBackupFromString(
                json = json,
                mode = mode,
                onSuccess = { result = Result.success("Database restored successfully") },
                onError = { err -> result = Result.failure(Exception(err)) }
            )
            result
        },
        onExportSaf = {
            val dateStr = TextFormatUtils.formatDateKey(System.currentTimeMillis())
            exportSafLauncher.launch("tumba_backup_$dateStr.json")
        },
        onImportSaf = { mode ->
            pendingImportMode = mode
            importSafLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
        },
        onClearAllData = {
            viewModel.confirmClearAllData()
        },
        onClose = { viewModel.closeSettings() }
    )

    // Folder Dialog
    FolderDialog(
        isOpen = folderModalState.isOpen,
        folderToEdit = folderModalState.folderToEdit,
        parentFolderId = folderModalState.parentFolderId,
        existingFolders = folders,
        onSave = { id, name, parentId, color, icon, description ->
            val folder = Folder(
                id = id ?: "",
                name = name,
                parentId = parentId,
                color = color,
                icon = icon,
                description = description
            )
            viewModel.saveFolder(folder)
        },
        onClose = { viewModel.closeFolderModal() }
    )

    // Tag Dialog
    TagDialog(
        isOpen = tagModalState.isOpen,
        tagToEdit = tagModalState.tagToEdit,
        onSave = { id, name, color ->
            val tag = Tag(
                id = id ?: "",
                name = name,
                color = color
            )
            viewModel.saveTag(tag)
        },
        onClose = { viewModel.closeTagModal() }
    )

    // Template Dialog
    TemplateDialog(
        isOpen = templateModalState.isOpen,
        templateToEdit = templateModalState.templateToEdit,
        onDismiss = { viewModel.closeTemplateModal() },
        onSave = { tpl -> viewModel.saveTemplate(tpl) }
    )

    // Confirm Dialog
    ConfirmDialog(
        isOpen = confirmModalState.isOpen,
        title = confirmModalState.title,
        message = confirmModalState.message,
        confirmLabel = confirmModalState.confirmLabel,
        isDestructive = confirmModalState.isDestructive,
        onConfirm = {
            confirmModalState.onConfirm()
            viewModel.closeConfirmModal()
        },
        onCancel = { viewModel.closeConfirmModal() }
    )

    // Local Profile Dialog
    ProfileDialog(
        isOpen = isProfileOpen,
        currentNickname = settings.profileNickname,
        currentAvatarPath = settings.profileAvatarPath,
        onSaveProfile = { nick, path -> viewModel.updateProfile(nick, path) },
        onAvatarUriSelected = { uri, nick -> viewModel.saveAvatarFromUri(uri, nick) },
        onClose = { viewModel.closeProfile() }
    )
}
