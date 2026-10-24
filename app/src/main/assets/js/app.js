const UI = {
    currentPackId: null,
    previewPackColor: null,
    previewPackIconIndex: null,
    currentTab: 'tab-create',

    initialize: function () {
        Storage.initialize();

        // Re-open the pack that was open last time (saved or not). A pack only appears in
        // Load Pack after "Save Pack" is pressed.
        if (Storage.getDraft()) {
            this.openDraftInEditor();
        } else {
            this.showWelcomeScreen();
        }

        this.setupNavigation();
        this.setupEventListeners();
        this.updateSettingsStats();
    },

    setupNavigation: function () {
        const navItems = document.querySelectorAll('.bottom-nav .nav-item');
        navItems.forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                const tabId = item.dataset.tab;
                this.switchTab(tabId);
            });
        });
    },

    switchTab: function (tabId) {
        this.currentTab = tabId;

        // Update Nav UI
        document.querySelectorAll('.bottom-nav .nav-item').forEach(item => {
            if (item.dataset.tab === tabId) {
                item.classList.add('active');
            } else {
                item.classList.remove('active');
            }
        });

        // Update Views
        document.querySelectorAll('.tab-view').forEach(view => {
            if (view.id === `view-${tabId.replace('tab-', '')}`) {
                view.classList.add('active');
            } else {
                view.classList.remove('active');
            }
        });

        // Trigger tab specific refresh
        if (tabId === 'tab-load') {
            this.renderSavedPacksList();
        } else if (tabId === 'tab-downloads') {
            this.loadDownloadedFiles();
        } else if (tabId === 'tab-settings') {
            this.updateSettingsStats();
        }
    },

    setupEventListeners: function () {
        // Welcome screen buttons
        const createPackBtn = document.getElementById('create-pack-btn');
        if (createPackBtn) {
            createPackBtn.addEventListener('click', () => this.showModal('create-pack-modal'));
        }

        const loadPackBtn = document.getElementById('load-pack-btn');
        if (loadPackBtn) {
            loadPackBtn.addEventListener('click', () => this.switchTab('tab-load'));
        }

        // Pack editor sidebar / actions
        const addHeadBtn = document.getElementById('add-head-btn');
        if (addHeadBtn) {
            addHeadBtn.addEventListener('click', () => this.showModal('add-head-modal'));
        }

        const generatePackBtn = document.getElementById('generate-pack-btn');
        if (generatePackBtn) {
            generatePackBtn.addEventListener('click', () => this.showGenerateModal());
        }

        const savePackBtn = document.getElementById('save-pack-btn');
        if (savePackBtn) {
            savePackBtn.addEventListener('click', () => this.saveCurrentPack());
        }

        const deletePackBtn = document.getElementById('delete-pack-btn');
        if (deletePackBtn) {
            deletePackBtn.addEventListener('click', () => this.confirmDeletePack());
        }

        const backToMenuBtn = document.getElementById('back-to-menu-btn');
        if (backToMenuBtn) {
            backToMenuBtn.addEventListener('click', () => this.backToMenu());
        }

        // Forms
        const createPackForm = document.getElementById('create-pack-form');
        if (createPackForm) {
            createPackForm.addEventListener('submit', (e) => {
                e.preventDefault();
                const packName = document.getElementById('pack-name').value.trim();
                if (packName) {
                    // Do not silently replace a pack that still has unsaved work
                    this.askUnsavedThen(() => this.createNewPack(packName));
                }
            });
        }

        const addHeadForm = document.getElementById('add-head-form');
        if (addHeadForm) {
            addHeadForm.addEventListener('submit', (e) => {
                e.preventDefault();
                if (addHeadForm.dataset.processing === 'true') return;
                addHeadForm.dataset.processing = 'true';

                this.addHead()
                    .then((added) => {
                        // Keep the dialog open when the skin or name was rejected, so nothing is lost
                        if (!added) return;
                        this.hideModal('add-head-modal');
                        addHeadForm.reset();
                        this.resetFileButtonText('skin-file');
                    })
                    .catch(error => {
                        console.error('Error adding head:', error);
                        this.showToast(error.message || 'Failed to add head', 'error');
                    })
                    .finally(() => {
                        addHeadForm.dataset.processing = 'false';
                    });
            });
        }

        const editHeadForm = document.getElementById('edit-head-form');
        if (editHeadForm) {
            editHeadForm.addEventListener('submit', (e) => {
                e.preventDefault();
                if (editHeadForm.dataset.processing === 'true') return;
                editHeadForm.dataset.processing = 'true';

                this.updateHead()
                    .then((updated) => {
                        if (!updated) return;
                        this.hideModal('edit-head-modal');
                        editHeadForm.reset();
                        this.resetFileButtonText('edit-skin-file');
                    })
                    .catch(error => {
                        console.error('Error updating head:', error);
                        this.showToast(error.message || 'Failed to update head', 'error');
                    })
                    .finally(() => {
                        editHeadForm.dataset.processing = 'false';
                    });
            });
        }

        const deleteHeadBtn = document.getElementById('delete-head-btn');
        if (deleteHeadBtn) {
            deleteHeadBtn.addEventListener('click', () => {
                const headName = document.getElementById('edit-head-name').value;
                if (headName) {
                    this.confirmDeleteHead(headName);
                }
            });
        }

        const searchHeads = document.getElementById('search-heads');
        if (searchHeads) {
            searchHeads.addEventListener('input', (e) => {
                this.filterHeads(e.target.value.trim().toLowerCase());
            });
        }

        const generateForm = document.getElementById('generate-form');
        if (generateForm) {
            generateForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.generateAddon();
            });
        }

        // Import Pack
        const importBtn = document.getElementById('import-pack-btn');
        if (importBtn) {
            importBtn.addEventListener('click', () => {
                if (importBtn.dataset.processing === 'true') return;

                const fileInput = document.getElementById('import-pack-file');
                if (!fileInput || !fileInput.files || !fileInput.files[0]) {
                    this.showToast('Please select a .json file to import', 'error');
                    return;
                }

                // The imported pack opens in the editor, so protect unsaved work first
                this.askUnsavedThen(() => {
                    importBtn.dataset.processing = 'true';

                    this.importPackFromFile()
                        .then(() => {
                            fileInput.value = '';
                            this.resetFileButtonText('import-pack-file');
                        })
                        .catch(error => {
                            console.error('Error importing pack:', error);
                            this.showToast(error.message || 'Failed to import pack', 'error');
                        })
                        .finally(() => {
                            importBtn.dataset.processing = 'false';
                        });
                });
            });
        }

        // Close Modal buttons
        document.querySelectorAll('.close-modal').forEach(button => {
            button.addEventListener('click', () => {
                const modal = button.closest('.modal');
                if (modal) this.hideModal(modal.id);
            });
        });

        const confirmCancel = document.getElementById('confirm-cancel');
        if (confirmCancel) {
            confirmCancel.addEventListener('click', () => {
                this.hideModal('confirm-modal');
            });
        }

        // File previews
        const skinFile = document.getElementById('skin-file');
        if (skinFile) {
            skinFile.addEventListener('change', (e) => {
                if (e.target.files && e.target.files[0]) {
                    this.previewSkinFile(e.target.files[0], 'skin-preview-canvas', 'head-preview-faces');
                }
            });
        }

        const editSkinFile = document.getElementById('edit-skin-file');
        if (editSkinFile) {
            editSkinFile.addEventListener('change', (e) => {
                if (e.target.files && e.target.files[0]) {
                    this.previewSkinFile(e.target.files[0], null, 'new-head-faces');
                }
            });
        }

        // Downloads screen actions
        const refreshDownloadsBtn = document.getElementById('refresh-downloads-btn');
        if (refreshDownloadsBtn) {
            refreshDownloadsBtn.addEventListener('click', () => {
                this.loadDownloadedFiles();
                this.showToast('Downloads list refreshed', 'info');
            });
        }

        const openFolderBtn = document.getElementById('open-downloads-folder-btn');
        if (openFolderBtn) {
            openFolderBtn.addEventListener('click', () => {
                if (window.Android && window.Android.openDownloadFolder) {
                    window.Android.openDownloadFolder();
                } else {
                    this.showToast('Folder: /storage/emulated/0/Download/PlayerHead', 'info');
                }
            });
        }

        const grantPermissionBtn = document.getElementById('grant-permission-btn');
        if (grantPermissionBtn) {
            grantPermissionBtn.addEventListener('click', () => {
                if (window.Android && window.Android.requestStoragePermission) {
                    window.Android.requestStoragePermission();
                }
            });
        }

        // Settings screen actions
        const settingsReqPermBtn = document.getElementById('settings-req-perm-btn');
        if (settingsReqPermBtn) {
            settingsReqPermBtn.addEventListener('click', () => {
                if (window.Android && window.Android.requestStoragePermission) {
                    window.Android.requestStoragePermission();
                }
            });
        }

        const exportBackupBtn = document.getElementById('export-backup-btn');
        if (exportBackupBtn) {
            exportBackupBtn.addEventListener('click', () => this.exportAllPacksBackup());
        }

        const clearAllDataBtn = document.getElementById('clear-all-data-btn');
        if (clearAllDataBtn) {
            clearAllDataBtn.addEventListener('click', () => this.confirmClearAllData());
        }

        this.setupFileInputHandlers();
    },

    setupFileInputHandlers: function () {
        const fileSelectBtns = document.querySelectorAll('.file-select-btn');
        fileSelectBtns.forEach(btn => {
            btn.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();

                const label = this.closest('label.file-input-label');
                if (label) {
                    const inputId = label.getAttribute('for');
                    if (inputId) {
                        const input = document.getElementById(inputId);
                        if (input) input.click();
                    }
                }
            });
        });

        const fileInputs = document.querySelectorAll('input[type="file"]');
        fileInputs.forEach(input => {
            input.addEventListener('change', () => {
                this.updateFileButtonText(input.id);
            });
        });
    },

    updateFileButtonText: function (inputId) {
        const input = document.getElementById(inputId);
        const label = document.querySelector(`label[for="${inputId}"]`);
        if (!input || !label) return;
        const btn = label.querySelector('.file-select-btn');
        if (!btn) return;

        if (input.files && input.files.length > 0) {
            const fileName = input.files[0].name;
            const icon = btn.querySelector('.material-symbols-outlined');
            btn.innerHTML = '';
            if (icon) btn.appendChild(icon);

            const fileNameSpan = document.createElement('span');
            fileNameSpan.className = 'file-name';
            fileNameSpan.textContent = ' ' + fileName;
            btn.appendChild(fileNameSpan);
        }
    },

    resetFileButtonText: function (inputId) {
        const label = document.querySelector(`label[for="${inputId}"]`);
        if (!label) return;
        const btn = label.querySelector('.file-select-btn');
        if (!btn) return;
        const icon = btn.querySelector('.material-symbols-outlined');

        btn.innerHTML = '';
        if (icon) btn.appendChild(icon);

        let defaultText = 'Choose File';
        if (inputId === 'skin-file' || inputId === 'edit-skin-file') {
            defaultText = 'Choose Skin File (.png)';
        } else if (inputId === 'import-pack-file') {
            defaultText = 'Choose Pack File (.json)';
        }

        btn.appendChild(document.createTextNode(' ' + defaultText));
    },

    showWelcomeScreen: function () {
        const welcomeScreen = document.getElementById('welcome-screen');
        const packEditor = document.getElementById('pack-editor');
        if (welcomeScreen) welcomeScreen.classList.remove('hidden');
        if (packEditor) packEditor.classList.add('hidden');
        this.currentPackId = null;
        Storage.closeDraft();

        // Update header pack name
        const headerPackName = document.getElementById('header-pack-name');
        if (headerPackName) headerPackName.textContent = 'No Pack Open';
    },

    showPackEditor: function () {
        const welcomeScreen = document.getElementById('welcome-screen');
        const packEditor = document.getElementById('pack-editor');
        if (welcomeScreen) welcomeScreen.classList.add('hidden');
        if (packEditor) packEditor.classList.remove('hidden');
    },

    // Opens a SAVED pack (from Load Pack, or right after an import) in the editor.
    loadPack: function (packId) {
        if (!Storage.openPack(packId)) {
            this.showToast('Pack not found', 'error');
            return;
        }
        this.openDraftInEditor();
    },

    // Shows whichever pack is currently open (saved or not) in the editor.
    openDraftInEditor: function () {
        const pack = Storage.getDraft();
        if (!pack) {
            this.showWelcomeScreen();
            return;
        }

        this.currentPackId = pack.id;

        const packTitle = document.getElementById('pack-title');
        if (packTitle) packTitle.textContent = pack.name;

        const headsCount = document.getElementById('heads-count');
        if (headsCount) headsCount.textContent = `Heads: ${pack.heads.length}`;

        const headerPackName = document.getElementById('header-pack-name');
        if (headerPackName) headerPackName.textContent = pack.name;

        this.refreshHeadsList();
        this.showPackEditor();
        this.updateSaveState();
        this.switchTab('tab-create');
    },

    // Shows whether the open pack is already in Load Pack. While there is something to save,
    // the badge turns orange and the Save Pack button turns green.
    updateSaveState: function () {
        const pack = Storage.getDraft();
        if (!pack) return;

        const isSaved = Storage.isSaved(pack.id);
        const needsSave = !isSaved || Storage.isDirty();

        const badge = document.getElementById('save-state');
        if (badge) {
            badge.textContent = !isSaved ? 'Not saved' : (needsSave ? 'Unsaved changes' : 'Saved');
            badge.classList.toggle('unsaved', needsSave);
            badge.classList.toggle('saved', !needsSave);
        }

        const saveBtn = document.getElementById('save-pack-btn');
        if (saveBtn) {
            saveBtn.classList.toggle('primary', needsSave);
            saveBtn.classList.toggle('secondary', !needsSave);
        }
    },

    createNewPack: function (packName) {
        // The new pack is only OPEN. It is not in Load Pack until "Save Pack" is pressed.
        Storage.createPack(packName);
        this.hideModal('create-pack-modal');
        const packNameInput = document.getElementById('pack-name');
        if (packNameInput) packNameInput.value = '';
        this.openDraftInEditor();
        this.showToast('Pack created! Press Save Pack to keep it in Load Pack.', 'success');
        this.updateSettingsStats();
    },

    renderSavedPacksList: function () {
        const savedPacksList = document.getElementById('saved-packs-list');
        if (!savedPacksList) return;
        savedPacksList.innerHTML = '';

        const packs = Storage.getAllPacks();
        const packIds = Object.keys(packs);

        if (packIds.length === 0) {
            const openPack = Storage.getDraft();

            const emptyCard = document.createElement('div');
            emptyCard.className = 'welcome-card';
            emptyCard.style.marginTop = '0';

            const emptyText = document.createElement('p');
            const emptyBtn = document.createElement('button');
            emptyBtn.className = 'button primary';

            if (openPack) {
                // Make it clear why the list is empty: the open pack just has not been saved yet
                emptyText.textContent = `No saved packs yet. "${openPack.name}" is open but not saved. ` +
                    'Press Save Pack on the Create Pack tab and it will appear here.';
                emptyBtn.textContent = 'Go to Open Pack';
                emptyBtn.addEventListener('click', () => this.switchTab('tab-create'));
            } else {
                emptyText.textContent = 'No saved packs found yet.';
                emptyBtn.textContent = 'Create Your First Pack';
                emptyBtn.addEventListener('click', () => {
                    this.switchTab('tab-create');
                    this.showModal('create-pack-modal');
                });
            }

            emptyCard.appendChild(emptyText);
            emptyCard.appendChild(emptyBtn);
            savedPacksList.appendChild(emptyCard);
            return;
        }

        for (const packId of packIds) {
            const pack = packs[packId];
            const packItem = document.createElement('div');
            packItem.className = 'saved-pack-item';

            const packInfo = document.createElement('div');
            packInfo.className = 'saved-pack-info';

            const packName = document.createElement('h3');
            packName.textContent = pack.name;

            const packDetails = document.createElement('p');
            packDetails.textContent = `Heads: ${pack.heads.length} | Modified: ${new Date(pack.lastModified).toLocaleDateString()}`;

            packInfo.appendChild(packName);
            packInfo.appendChild(packDetails);

            const packActions = document.createElement('div');
            packActions.className = 'saved-pack-actions';

            const loadBtn = document.createElement('button');
            loadBtn.textContent = 'Load';
            loadBtn.className = 'button secondary';
            loadBtn.addEventListener('click', () => {
                this.askUnsavedThen(() => this.loadPack(packId));
            });

            const exportBtn = document.createElement('button');
            exportBtn.textContent = 'Export';
            exportBtn.className = 'button secondary';
            exportBtn.addEventListener('click', () => {
                this.exportPack(packId);
            });

            const deleteBtn = document.createElement('button');
            deleteBtn.textContent = 'Delete';
            deleteBtn.className = 'button delete';
            deleteBtn.addEventListener('click', () => {
                this.confirmDeletePackFromList(packId, packItem);
            });

            packActions.appendChild(loadBtn);
            packActions.appendChild(exportBtn);
            packActions.appendChild(deleteBtn);

            packItem.appendChild(packInfo);
            packItem.appendChild(packActions);
            savedPacksList.appendChild(packItem);
        }
    },

    importPackFromFile: function () {
        return new Promise((resolve, reject) => {
            const fileInput = document.getElementById('import-pack-file');
            if (!fileInput || !fileInput.files || !fileInput.files[0]) {
                reject(new Error('Please select a .json file to import'));
                return;
            }

            const file = fileInput.files[0];
            const reader = new FileReader();

            reader.onload = (e) => {
                try {
                    const packData = JSON.parse(e.target.result);
                    const packId = Storage.importPack(packData);

                    if (packId) {
                        this.loadPack(packId);
                        this.showToast('Pack imported successfully!', 'success');
                        this.updateSettingsStats();
                        resolve();
                    } else {
                        reject(new Error('Invalid pack data format'));
                    }
                } catch (error) {
                    console.error('Import error:', error);
                    reject(new Error('Failed to import pack: ' + error.message));
                }
            };

            reader.onerror = () => {
                reject(new Error('Failed to read file'));
            };

            reader.readAsText(file);
        });
    },

    exportPack: function (packId) {
        const pack = Storage.exportPack(packId || this.currentPackId);
        if (!pack) {
            this.showToast('No pack to export', 'error');
            return;
        }

        const packJson = JSON.stringify(pack, null, 2);
        const fileName = `${this.toSafeFileName(pack.name)}_pack.json`;

        if (window.Android && window.Android.saveJsonPack) {
            try {
                const res = JSON.parse(window.Android.saveJsonPack(packJson, fileName));
                if (res.success) {
                    this.showToast(`Saved to /storage/emulated/0/Download/PlayerHead/${fileName}!`, 'success');
                    return;
                }
            } catch (err) {
                console.warn('Android saveJsonPack error:', err);
            }
        }

        // Web fallback
        const blob = new Blob([packJson], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        this.showToast('Pack exported!', 'success');
    },

    // "Save Pack": the only way a pack gets into the Load Pack list.
    saveCurrentPack: function () {
        if (!this.currentPackId || !Storage.getDraft()) {
            this.showToast('No active pack to save', 'error');
            return false;
        }

        if (!Storage.saveDraft()) {
            this.showToast('Could not save the pack (phone storage may be full)', 'error');
            return false;
        }

        this.updateSaveState();
        this.updateSettingsStats();
        this.showToast('Pack saved! You can open it again from Load Pack.', 'success');
        return true;
    },

    // Resolves to true when the head was added, or false when it was rejected
    // (an error toast explains why and the dialog stays open).
    async addHead() {
        if (!this.currentPackId) throw new Error('No active pack');

        const headNameInput = document.getElementById('head-name');
        const headName = headNameInput ? headNameInput.value.trim() : '';
        const skinFileInput = document.getElementById('skin-file');
        const skinFile = skinFileInput && skinFileInput.files ? skinFileInput.files[0] : null;

        if (!headName) throw new Error('Please enter a head name');
        if (headName.length > 16) throw new Error('Head name must be 16 characters or less');
        if (!skinFile) throw new Error('Please select a skin file (.png)');

        const validation = await Utils.validateSkinFile(skinFile);
        if (!validation.valid) {
            this.showToast(validation.message, 'error');
            return false;
        }

        const skinImage = validation.image;
        const itemTexture = await Utils.createItemTexture(skinImage);
        const blockTexture = await Utils.createBlockTexture(skinImage);

        const result = Storage.addHead(this.currentPackId, {
            name: headName,
            itemTexture: await this.blobToBase64(itemTexture),
            blockTexture: await this.blobToBase64(blockTexture),
        });

        if (!result.ok) {
            this.showToast(
                result.reason === 'duplicate'
                    ? 'A head with this name already exists in this pack'
                    : 'Could not add the head (phone storage may be full)',
                'error'
            );
            return false;
        }

        this.hideModal('add-head-modal');
        if (headNameInput) headNameInput.value = '';
        if (skinFileInput) skinFileInput.value = '';

        const skinPreviewCanvas = document.getElementById('skin-preview-canvas');
        if (skinPreviewCanvas) {
            const ctx = skinPreviewCanvas.getContext('2d');
            ctx.clearRect(0, 0, skinPreviewCanvas.width, skinPreviewCanvas.height);
        }

        const headFaces = document.querySelectorAll('#head-preview-faces > div');
        headFaces.forEach(face => {
            face.style.backgroundImage = '';
            face.classList.remove('width-ratio-0-25', 'width-ratio-0-5');
        });

        this.refreshHeadsList();
        const pack = Storage.getPack(this.currentPackId);
        const headsCount = document.getElementById('heads-count');
        if (headsCount && pack) headsCount.textContent = `Heads: ${pack.heads.length}`;

        this.updateSaveState();
        this.showToast('Head added successfully!', 'success');
        this.updateSettingsStats();
        return true;
    },

    // Resolves to true when the head was updated, or false when it was rejected.
    async updateHead() {
        if (!this.currentPackId) throw new Error('No active pack');

        const headName = document.getElementById('edit-head-name').value;
        const skinFileInput = document.getElementById('edit-skin-file');
        const skinFile = skinFileInput && skinFileInput.files ? skinFileInput.files[0] : null;

        if (!headName) throw new Error('Missing head name');
        if (!skinFile) throw new Error('Please select a skin file');

        const validation = await Utils.validateSkinFile(skinFile);
        if (!validation.valid) {
            this.showToast(validation.message, 'error');
            return false;
        }

        const skinImage = validation.image;
        const itemTexture = await Utils.createItemTexture(skinImage);
        const blockTexture = await Utils.createBlockTexture(skinImage);

        const updateData = {
            itemTexture: await this.blobToBase64(itemTexture),
            blockTexture: await this.blobToBase64(blockTexture),
        };

        const success = Storage.updateHead(this.currentPackId, headName, updateData);

        if (!success) {
            this.showToast('Failed to update head', 'error');
            return false;
        }

        this.hideModal('edit-head-modal');
        if (skinFileInput) skinFileInput.value = '';

        const currentFaces = document.querySelectorAll('#current-head-faces > div');
        const newFaces = document.querySelectorAll('#new-head-faces > div');

        currentFaces.forEach(face => {
            face.style.backgroundImage = '';
            if (!face.classList.contains('width-ratio-0-25')) {
                face.classList.remove('width-ratio-0-5');
            }
        });

        newFaces.forEach(face => {
            face.style.backgroundImage = '';
            face.classList.remove('width-ratio-0-25', 'width-ratio-0-5');
        });

        this.refreshHeadsList();
        this.updateSaveState();
        this.showToast('Head updated successfully!', 'success');
        return true;
    },

    showEditHeadModal(headName) {
        if (!this.currentPackId) return;

        const pack = Storage.getPack(this.currentPackId);
        const head = pack.heads.find(h => h.name === headName);

        if (!head) {
            this.showToast('Head not found', 'error');
            return;
        }

        document.getElementById('edit-head-name').value = head.name;

        const faces = document.querySelectorAll('#current-head-faces > div');
        faces.forEach(face => {
            face.style.backgroundImage = `url(${head.blockTexture})`;
        });

        const newFaces = document.querySelectorAll('#new-head-faces > div');
        newFaces.forEach(face => {
            face.style.backgroundImage = '';
        });

        this.showModal('edit-head-modal');
    },

    deleteHead(headName) {
        if (!this.currentPackId) {
            this.showToast('No active pack', 'error');
            return;
        }

        const success = Storage.removeHead(this.currentPackId, headName);

        if (success) {
            this.hideModal('confirm-modal');
            this.hideModal('edit-head-modal');

            this.refreshHeadsList();
            const headsCount = document.getElementById('heads-count');
            if (headsCount) {
                headsCount.textContent = `Heads: ${Storage.getPack(this.currentPackId).heads.length}`;
            }

            this.updateSaveState();
            this.showToast('Head deleted successfully!', 'success');
            this.updateSettingsStats();
        } else {
            this.showToast('Failed to delete head', 'error');
        }
    },

    confirmDeleteHead(headName) {
        this.openConfirm({
            title: 'Delete Head',
            message: `Are you sure you want to delete "${headName}"?`,
            onOk: () => this.deleteHead(headName)
        });
    },

    deletePack() {
        if (!this.currentPackId) {
            this.showToast('No active pack to delete', 'error');
            return;
        }

        const success = Storage.deletePack(this.currentPackId);

        if (success) {
            this.hideModal('confirm-modal');
            this.showWelcomeScreen();
            this.showToast('Pack deleted successfully!', 'success');
            this.updateSettingsStats();
        } else {
            this.showToast('Failed to delete pack', 'error');
        }
    },

    confirmDeletePack() {
        if (!this.currentPackId) return;

        const pack = Storage.getPack(this.currentPackId);
        this.openConfirm({
            title: 'Delete Pack',
            message: Storage.isSaved(this.currentPackId)
                ? `Are you sure you want to delete "${pack.name}"? It will also be removed from Load Pack. This action cannot be undone.`
                : `Are you sure you want to delete "${pack.name}"? It has not been saved, so this action cannot be undone.`,
            onOk: () => this.deletePack()
        });
    },

    confirmDeletePackFromList(packId, packElement) {
        const pack = Storage.getSavedPack(packId);
        this.openConfirm({
            title: 'Delete Pack',
            message: `Are you sure you want to delete "${pack ? pack.name : 'Pack'}"?`,
            onOk: () => {
                const success = Storage.deletePack(packId);
                if (success) {
                    if (packElement) packElement.remove();

                    if (packId === this.currentPackId) {
                        this.showWelcomeScreen();
                    }

                    this.renderSavedPacksList();
                    this.showToast('Pack deleted successfully!', 'success');
                    this.updateSettingsStats();
                } else {
                    this.showToast('Failed to delete pack', 'error');
                }
            }
        });
    },

    showGenerateModal() {
        if (!this.currentPackId) return;

        const pack = Storage.getPack(this.currentPackId);
        if (!pack || pack.heads.length === 0) {
            this.showToast('Add at least one head before generating an add-on', 'error');
            return;
        }

        document.getElementById('addon-name').value = pack.name;
        document.getElementById('preview-head-count').textContent = `Heads: ${pack.heads.length}`;

        const previewCanvas = document.getElementById('pack-icon-preview');
        this.previewPackColor = Utils.getRandomColor();
        this.previewPackIconIndex = Math.random() <= 0.01 ? 8 : Math.floor(Math.random() * 8);

        Utils.createPackIcon(this.previewPackColor, this.previewPackIconIndex).then(blob => {
            const url = URL.createObjectURL(blob);
            const img = new Image();
            img.onload = () => {
                const ctx = previewCanvas.getContext('2d');
                ctx.clearRect(0, 0, previewCanvas.width, previewCanvas.height);
                ctx.drawImage(img, 0, 0, previewCanvas.width, previewCanvas.height);
                URL.revokeObjectURL(url);
            };
            img.src = url;
        });

        this.showModal('generate-modal');
    },

    async generateAddon() {
        if (!this.currentPackId) {
            this.showToast('No active pack', 'error');
            return;
        }

        const pack = Storage.getPack(this.currentPackId);
        if (!pack || pack.heads.length === 0) {
            this.showToast('Pack has no heads to generate. Add at least one head!', 'error');
            return;
        }

        const addonName = document.getElementById('addon-name').value.trim();
        if (!addonName) {
            this.showToast('Please enter an add-on name', 'error');
            return;
        }

        try {
            this.showToast('Generating Minecraft add-on...', 'info');

            const packData = {
                addonName: addonName,
                heads: pack.heads,
                color: this.previewPackColor || Utils.getRandomColor(),
                iconIndex: this.previewPackIconIndex
            };

            const zip = await Utils.generatePackZip(packData);
            const fileName = `${this.toSafeFileName(addonName)}_${pack.heads.length}_Heads.mcaddon`;

            // If Android bridge is available, save directly into /storage/emulated/0/Download/PlayerHead
            if (window.Android && window.Android.saveAddon) {
                const base64Data = await zip.generateAsync({ type: 'base64' });
                const resJson = window.Android.saveAddon(base64Data, fileName);
                const res = JSON.parse(resJson);

                if (res.success) {
                    this.hideModal('generate-modal');
                    this.showToast('Add-on downloaded! Find it in /storage/emulated/0/Download/PlayerHead', 'success');
                    // Refresh downloads tab if already active or user switches
                    this.loadDownloadedFiles();
                } else {
                    throw new Error(res.message || 'Failed to save add-on to storage');
                }
            } else {
                // Fallback for standard browser
                const blob = await zip.generateAsync({ type: 'blob' });
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = fileName;
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                URL.revokeObjectURL(url);

                this.hideModal('generate-modal');
                this.showToast('Add-on downloaded!', 'success');
            }
        } catch (error) {
            console.error('Error generating add-on:', error);
            this.showToast('Failed to generate add-on: ' + error.message, 'error');
        }
    },

    refreshHeadsList() {
        if (!this.currentPackId) return;

        const pack = Storage.getPack(this.currentPackId);
        const headsContainer = document.getElementById('heads-container');
        if (!headsContainer) return;
        headsContainer.innerHTML = '';

        if (!pack || pack.heads.length === 0) {
            const noHeadsMessage = document.createElement('div');
            noHeadsMessage.className = 'no-heads-message';
            noHeadsMessage.style.gridColumn = '1 / -1';
            noHeadsMessage.innerHTML = '<p>No heads in this pack yet.<br>Click "Add Head" to get started!</p>';
            headsContainer.appendChild(noHeadsMessage);
            return;
        }

        for (const head of pack.heads) {
            const headItem = document.createElement('div');
            headItem.className = 'head-item';
            headItem.dataset.name = head.name;

            const headImage = document.createElement('img');
            headImage.className = 'head-image';
            headImage.src = head.itemTexture;
            headImage.alt = head.name;

            const headName = document.createElement('div');
            headName.className = 'head-name';
            headName.textContent = head.name;

            headItem.appendChild(headImage);
            headItem.appendChild(headName);

            headItem.addEventListener('click', () => {
                this.showEditHeadModal(head.name);
            });

            headsContainer.appendChild(headItem);
        }
    },

    filterHeads(query) {
        const headItems = document.querySelectorAll('#heads-container .head-item');
        headItems.forEach(item => {
            const name = item.dataset.name.toLowerCase();
            if (name.includes(query)) {
                item.style.display = '';
            } else {
                item.style.display = 'none';
            }
        });
    },

    async previewSkinFile(file, skinCanvasId, headFacesId) {
        if (!file) return;

        try {
            const validation = await Utils.validateSkinFile(file);
            if (!validation.valid) {
                this.showToast(validation.message, 'error');
                return;
            }

            const skinImage = validation.image;

            if (skinCanvasId) {
                const skinCanvas = document.getElementById(skinCanvasId);
                if (skinCanvas) {
                    const skinCtx = skinCanvas.getContext('2d');
                    skinCtx.clearRect(0, 0, skinCanvas.width, skinCanvas.height);

                    const scale = Math.min(skinCanvas.width / skinImage.width, skinCanvas.height / skinImage.height);
                    const x = (skinCanvas.width - skinImage.width * scale) / 2;
                    const y = (skinCanvas.height - skinImage.height * scale) / 2;

                    skinCtx.imageSmoothingEnabled = false;
                    skinCtx.drawImage(skinImage, x, y, skinImage.width * scale, skinImage.height * scale);
                }
            }

            if (headFacesId) {
                const tempCanvas = document.createElement('canvas');
                tempCanvas.width = skinImage.width;
                tempCanvas.height = skinImage.height;
                const tempCtx = tempCanvas.getContext('2d');
                tempCtx.drawImage(skinImage, 0, 0);
                const skinUrl = tempCanvas.toDataURL('image/png');

                const faces = document.querySelectorAll(`#${headFacesId} > div`);

                let widthRatioClass = '';
                if (skinImage.height === 64 && skinImage.width === 64) {
                    widthRatioClass = '';
                } else if (skinImage.height === 32 && skinImage.width === 64) {
                    widthRatioClass = 'width-ratio-0-5';
                } else if (skinImage.width === 128 && skinImage.height === 128) {
                    widthRatioClass = '';
                } else if (skinImage.width === 128 && skinImage.height === 64) {
                    widthRatioClass = 'width-ratio-0-5';
                } else if (skinImage.height !== skinImage.width) {
                    const ratio = skinImage.height / skinImage.width;
                    widthRatioClass = `width-ratio-${ratio}`;
                }

                faces.forEach(face => {
                    face.style.backgroundImage = `url(${skinUrl})`;
                    face.classList.remove('width-ratio-0-25', 'width-ratio-0-5');
                    if (widthRatioClass) {
                        face.classList.add(widthRatioClass);
                    }
                });
            }
        } catch (error) {
            console.error('Error previewing skin:', error);
            this.showToast('Failed to preview skin: ' + error.message, 'error');
        }
    },

    blobToBase64(blob) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => resolve(reader.result);
            reader.onerror = reject;
            reader.readAsDataURL(blob);
        });
    },

    // Downloads View Logic
    loadDownloadedFiles: function () {
        const listContainer = document.getElementById('downloaded-files-list');
        const emptyMessage = document.getElementById('downloads-empty-message');
        const permBanner = document.getElementById('downloads-perm-banner');
        if (!listContainer) return;

        // Check permission
        const isPermitted = window.Android && window.Android.isStoragePermissionGranted ?
            window.Android.isStoragePermissionGranted() : true;

        if (permBanner) {
            permBanner.style.display = isPermitted ? 'none' : 'block';
        }

        let files = [];
        if (window.Android && window.Android.getDownloadedAddons) {
            try {
                const filesJson = window.Android.getDownloadedAddons();
                files = JSON.parse(filesJson || '[]');
            } catch (e) {
                console.error('Error fetching downloaded files:', e);
            }
        }

        listContainer.innerHTML = '';

        if (!files || files.length === 0) {
            if (emptyMessage) emptyMessage.style.display = 'block';
            return;
        }

        if (emptyMessage) emptyMessage.style.display = 'none';

        files.forEach(file => {
            const card = document.createElement('div');
            card.className = 'download-card';

            const header = document.createElement('div');
            header.className = 'download-card-header';

            const iconDiv = document.createElement('div');
            iconDiv.className = 'download-card-icon';
            const iconSpan = document.createElement('span');
            iconSpan.className = 'material-symbols-outlined';
            iconSpan.textContent = file.isAddon ? 'extension' : 'folder_zip';
            iconDiv.appendChild(iconSpan);

            const details = document.createElement('div');
            details.className = 'download-card-details';

            const title = document.createElement('div');
            title.className = 'download-card-title';
            title.textContent = file.name;

            const meta = document.createElement('div');
            meta.className = 'download-card-meta';
            meta.textContent = `${file.formattedSize || ''} • ${file.date || ''}`;

            details.appendChild(title);
            details.appendChild(meta);

            header.appendChild(iconDiv);
            header.appendChild(details);

            const buttons = document.createElement('div');
            buttons.className = 'download-card-buttons';

            // Open button
            const openBtn = document.createElement('button');
            openBtn.className = 'button primary';
            openBtn.innerHTML = '<span class="material-symbols-outlined" style="font-size:18px;">open_in_new</span> Open';
            openBtn.addEventListener('click', () => {
                if (window.Android && window.Android.openAddon) {
                    const opened = window.Android.openAddon(file.name);
                    if (!opened) {
                        this.showToast('Could not open file with Minecraft', 'error');
                    }
                } else {
                    this.showToast(`Open file: ${file.name}`, 'info');
                }
            });

            // Share button
            const shareBtn = document.createElement('button');
            shareBtn.className = 'button secondary';
            shareBtn.innerHTML = '<span class="material-symbols-outlined" style="font-size:18px;">share</span> Share';
            shareBtn.addEventListener('click', () => {
                if (window.Android && window.Android.shareAddon) {
                    window.Android.shareAddon(file.name);
                } else {
                    this.showToast('Sharing ' + file.name, 'info');
                }
            });

            // Delete button
            const deleteBtn = document.createElement('button');
            deleteBtn.className = 'button danger';
            deleteBtn.innerHTML = '<span class="material-symbols-outlined" style="font-size:18px;">delete</span> Delete';
            deleteBtn.addEventListener('click', () => {
                this.confirmDeleteDownloadedFile(file.name, card);
            });

            buttons.appendChild(openBtn);
            buttons.appendChild(shareBtn);
            buttons.appendChild(deleteBtn);

            card.appendChild(header);
            card.appendChild(buttons);
            listContainer.appendChild(card);
        });
    },

    confirmDeleteDownloadedFile: function (fileName, cardElement) {
        this.openConfirm({
            title: 'Delete File',
            message: `Are you sure you want to delete "${fileName}" from /storage/emulated/0/Download/PlayerHead?`,
            onOk: () => {
                if (window.Android && window.Android.deleteAddon) {
                    const deleted = window.Android.deleteAddon(fileName);
                    if (deleted) {
                        if (cardElement) cardElement.remove();
                        this.showToast('File deleted successfully!', 'success');
                        this.loadDownloadedFiles();
                    } else {
                        this.showToast('Failed to delete file', 'error');
                    }
                } else {
                    if (cardElement) cardElement.remove();
                    this.showToast('File deleted', 'info');
                }
            }
        });
    },

    // Settings View Logic
    updateSettingsStats: function () {
        const packs = Storage.getAllPacks();
        const packKeys = Object.keys(packs);
        let totalHeads = 0;
        packKeys.forEach(k => {
            if (packs[k].heads) totalHeads += packs[k].heads.length;
        });

        const statPacks = document.getElementById('stat-total-packs');
        if (statPacks) statPacks.textContent = packKeys.length;

        const statHeads = document.getElementById('stat-total-heads');
        if (statHeads) statHeads.textContent = totalHeads;

        // Storage usage
        const usage = Storage.getStorageUsage();
        const statStorage = document.getElementById('stat-storage-usage');
        if (statStorage) {
            const kbUsed = (usage.used / 1024).toFixed(1);
            statStorage.textContent = `${kbUsed} KB / 5 MB`;
        }

        // Permission status
        const permStatus = document.getElementById('stat-permission-status');
        if (permStatus) {
            const isPermitted = window.Android && window.Android.isStoragePermissionGranted ?
                window.Android.isStoragePermissionGranted() : true;
            permStatus.textContent = isPermitted ? 'Granted' : 'Permission Required';
            permStatus.style.color = isPermitted ? '#55ff55' : '#ff5555';
        }
    },

    exportAllPacksBackup: function () {
        const packs = Storage.getAllPacks();
        const backupJson = JSON.stringify(packs, null, 2);
        const fileName = `PlayerHeads_AllPacks_Backup_${Date.now()}.json`;

        if (window.Android && window.Android.saveJsonPack) {
            try {
                const res = JSON.parse(window.Android.saveJsonPack(backupJson, fileName));
                if (res.success) {
                    this.showToast(`Backup saved to /storage/emulated/0/Download/PlayerHead!`, 'success');
                    return;
                }
            } catch (e) {}
        }

        const blob = new Blob([backupJson], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        this.showToast('Backup exported!', 'success');
    },

    confirmClearAllData: function () {
        this.openConfirm({
            title: 'Clear All Data',
            message: 'Are you sure you want to delete ALL saved packs? This cannot be undone.',
            onOk: () => {
                // The open pack is closed only if it was one of the saved packs
                const closedOpenPack = Storage.clearAllSaved();
                if (closedOpenPack) {
                    this.showWelcomeScreen();
                } else {
                    this.updateSaveState();
                }
                this.updateSettingsStats();
                this.showToast('All saved packs cleared!', 'success');
            }
        });
    },

    // Names typed by the user become file names. A "/" (or ":", "?", ...) in a name made saving
    // to the Download folder fail, so those characters are replaced.
    toSafeFileName: function (name) {
        return String(name).replace(/[\\/:*?"<>|\x00-\x1f]+/g, '_').replace(/\s+/g, '_');
    },

    // Shared confirm dialog. Passing altText adds a third button (Save Pack) between Cancel and
    // the main button. Every field is set on every call, so nothing leaks from a previous use.
    openConfirm: function ({ title, message, okText = 'Confirm', onOk = null, altText = null, onAlt = null }) {
        document.getElementById('confirm-title').textContent = title;
        document.getElementById('confirm-message').textContent = message;

        const okBtn = document.getElementById('confirm-ok');
        okBtn.textContent = okText;
        okBtn.onclick = () => {
            this.hideModal('confirm-modal');
            if (onOk) onOk();
        };

        const altBtn = document.getElementById('confirm-alt');
        if (altBtn) {
            if (altText) {
                altBtn.textContent = altText;
                altBtn.classList.remove('hidden');
                altBtn.onclick = () => {
                    this.hideModal('confirm-modal');
                    if (onAlt) onAlt();
                };
            } else {
                altBtn.classList.add('hidden');
                altBtn.onclick = null;
            }
        }

        this.showModal('confirm-modal');
    },

    // Runs `proceed` straight away when nothing would be lost. Otherwise asks whether to save the
    // open pack first, discard it, or cancel.
    askUnsavedThen: function (proceed) {
        if (!Storage.hasUnsavedChanges()) {
            proceed();
            return;
        }

        const pack = Storage.getDraft();
        this.openConfirm({
            title: 'Unsaved Pack',
            message: Storage.isSaved(pack.id)
                ? `"${pack.name}" has changes that are not saved yet.`
                : `"${pack.name}" is not saved yet. If you continue without saving, it will be lost.`,
            okText: 'Discard',
            onOk: proceed,
            altText: 'Save Pack',
            onAlt: () => {
                if (this.saveCurrentPack()) proceed();
            }
        });
    },

    // Modal Helpers
    showModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) modal.classList.add('show');
    },

    hideModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) modal.classList.remove('show');
    },

    backToMenu() {
        if (Storage.hasUnsavedChanges()) {
            this.askUnsavedThen(() => this.showWelcomeScreen());
            return;
        }

        this.openConfirm({
            title: 'Return to Menu',
            message: 'Are you sure you want to close this pack and return to the main menu?',
            onOk: () => this.showWelcomeScreen()
        });
    },

    // Toast
    showToast(message, type = 'info') {
        const toast = document.getElementById('toast');
        if (!toast) return;
        toast.textContent = message;
        toast.className = 'toast';
        toast.classList.add(type);
        toast.classList.add('show');

        if (this._toastTimer) clearTimeout(this._toastTimer);
        this._toastTimer = setTimeout(() => {
            toast.classList.remove('show');
        }, 3000);

        // Also notify native Android toast if bridge available
        if (window.Android && window.Android.showToast && type === 'error') {
            window.Android.showToast(message);
        }
    },

    // Handle Android Hardware Back Button
    handleBackButton: function () {
        // If a modal is open, close it
        const openModal = document.querySelector('.modal.show');
        if (openModal) {
            this.hideModal(openModal.id);
            return true;
        }

        // If on Pack Editor view in tab-create, close pack and return to welcome screen
        if (this.currentTab === 'tab-create' && this.currentPackId) {
            this.askUnsavedThen(() => this.showWelcomeScreen());
            return true;
        }

        // If on another tab, switch back to tab-create
        if (this.currentTab !== 'tab-create') {
            this.switchTab('tab-create');
            return true;
        }

        // Return false to allow Android to exit app
        return false;
    },

    onPermissionsUpdated: function () {
        this.updateSettingsStats();
        if (this.currentTab === 'tab-downloads') {
            this.loadDownloadedFiles();
        }
    }
};

document.addEventListener('DOMContentLoaded', function () {
    UI.initialize();
});
