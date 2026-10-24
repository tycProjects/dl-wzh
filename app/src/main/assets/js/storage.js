/*
 * Storage
 * -------
 * Saved packs : localStorage "playerHeadPacks". This is the list shown in the Load Pack tab.
 *               A pack only gets in here when the user presses "Save Pack" (or imports a pack file).
 * Open pack   : localStorage "playerHeadDraft". The pack that is open in the editor (the "draft").
 *               Creating a pack and adding / editing / deleting heads only change the draft.
 *               It has its own key so the work survives Android closing the app (for example while
 *               the file picker is open) without ever appearing in Load Pack by itself.
 */
const Storage = {
    PACKS_KEY: 'playerHeadPacks',
    DRAFT_KEY: 'playerHeadDraft',
    LEGACY_ACTIVE_KEY: 'activePackId',

    _draft: null, // { pack: {...}, dirty: boolean }

    initialize: function () {
        if (!localStorage.getItem(this.PACKS_KEY)) {
            this._writeAllPacks({});
        }

        this._draft = this._readDraft();

        // Older versions only remembered the id of the open pack (which was always a saved pack).
        if (!this._draft) {
            const legacyId = localStorage.getItem(this.LEGACY_ACTIVE_KEY);
            const saved = legacyId ? this.getSavedPack(legacyId) : null;
            if (saved) {
                this._draft = { pack: this._clone(saved), dirty: false };
                this._writeDraft();
            }
        }
        localStorage.removeItem(this.LEGACY_ACTIVE_KEY);
    },

    _clone: function (value) {
        return JSON.parse(JSON.stringify(value));
    },

    // ---- Saved packs (the Load Pack list) -------------------------------------------------------

    getAllPacks: function () {
        try {
            const packs = JSON.parse(localStorage.getItem(this.PACKS_KEY) || '{}');
            return packs && typeof packs === 'object' ? packs : {};
        } catch (e) {
            console.error('Saved packs could not be read', e);
            return {};
        }
    },

    _writeAllPacks: function (packs) {
        try {
            localStorage.setItem(this.PACKS_KEY, JSON.stringify(packs));
            return true;
        } catch (e) {
            console.error('Saved packs could not be stored', e);
            return false;
        }
    },

    getSavedPack: function (packId) {
        return this.getAllPacks()[packId] || null;
    },

    isSaved: function (packId) {
        return !!this.getSavedPack(packId);
    },

    // ---- Open pack (draft) ----------------------------------------------------------------------

    _readDraft: function () {
        try {
            const raw = localStorage.getItem(this.DRAFT_KEY);
            if (!raw) return null;
            const data = JSON.parse(raw);
            if (data && data.pack && data.pack.id && Array.isArray(data.pack.heads)) {
                return { pack: data.pack, dirty: !!data.dirty };
            }
        } catch (e) {
            console.error('Open pack could not be read', e);
        }
        return null;
    },

    _writeDraft: function () {
        try {
            if (this._draft) {
                localStorage.setItem(this.DRAFT_KEY, JSON.stringify(this._draft));
            } else {
                localStorage.removeItem(this.DRAFT_KEY);
            }
            return true;
        } catch (e) {
            console.error('Open pack could not be stored', e);
            return false;
        }
    },

    // Stores the new state first and only then makes it the current one, so a full phone
    // storage can never leave the screen and the stored data out of sync.
    _commitDraft: function (pack, dirty) {
        const next = { pack: pack, dirty: dirty };
        try {
            localStorage.setItem(this.DRAFT_KEY, JSON.stringify(next));
        } catch (e) {
            console.error('Open pack could not be stored', e);
            return false;
        }
        this._draft = next;
        return true;
    },

    getDraft: function () {
        return this._draft ? this._draft.pack : null;
    },

    getActivePack: function () {
        return this._draft ? this._draft.pack.id : null;
    },

    // Edited since it was opened / last saved?
    isDirty: function () {
        return !!(this._draft && this._draft.dirty);
    },

    // True when closing or replacing the open pack would lose work that was never saved.
    hasUnsavedChanges: function () {
        if (!this._draft) return false;
        const pack = this._draft.pack;
        // A brand new pack with no heads yet has nothing worth saving.
        if (!this.isSaved(pack.id)) return pack.heads.length > 0;
        return this._draft.dirty;
    },

    // The pack as it is being edited: the open pack first, otherwise the saved copy.
    getPack: function (packId) {
        if (this._draft && this._draft.pack.id === packId) return this._draft.pack;
        return this.getSavedPack(packId);
    },

    // New pack. It becomes the open pack but is NOT added to the saved packs.
    createPack: function (packName) {
        const now = new Date().toISOString();
        const pack = {
            id: Date.now().toString(),
            name: packName,
            created: now,
            lastModified: now,
            heads: []
        };
        this._draft = { pack: pack, dirty: false };
        this._writeDraft();
        return pack.id;
    },

    // Opens a saved pack for editing. Changes go to a working copy until "Save Pack" is pressed.
    openPack: function (packId) {
        const saved = this.getSavedPack(packId);
        if (!saved) return false;
        this._draft = { pack: this._clone(saved), dirty: false };
        this._writeDraft();
        return true;
    },

    closeDraft: function () {
        this._draft = null;
        this._writeDraft();
    },

    // Old name kept so existing callers keep working.
    clearActivePack: function () {
        this.closeDraft();
    },

    _draftFor: function (packId) {
        return this._draft && this._draft.pack.id === packId ? this._draft : null;
    },

    // Returns { ok: true } or { ok: false, reason: 'no-pack' | 'duplicate' | 'storage' }
    addHead: function (packId, head) {
        const draft = this._draftFor(packId);
        if (!draft) return { ok: false, reason: 'no-pack' };
        if (draft.pack.heads.some(h => h.name === head.name)) {
            return { ok: false, reason: 'duplicate' };
        }

        const now = new Date().toISOString();
        const heads = draft.pack.heads.concat([{
            name: head.name,
            itemTexture: head.itemTexture,
            blockTexture: head.blockTexture,
            added: now
        }]);

        return this._commitDraft({ ...draft.pack, heads: heads, lastModified: now }, true)
            ? { ok: true }
            : { ok: false, reason: 'storage' };
    },

    updateHead: function (packId, headName, updateData) {
        const draft = this._draftFor(packId);
        if (!draft) return false;

        const index = draft.pack.heads.findIndex(h => h.name === headName);
        if (index === -1) return false;

        const heads = draft.pack.heads.slice();
        heads[index] = { ...heads[index], ...updateData, name: heads[index].name };

        return this._commitDraft(
            { ...draft.pack, heads: heads, lastModified: new Date().toISOString() }, true);
    },

    removeHead: function (packId, headName) {
        const draft = this._draftFor(packId);
        if (!draft) return false;

        const heads = draft.pack.heads.filter(h => h.name !== headName);
        if (heads.length === draft.pack.heads.length) return false;

        return this._commitDraft(
            { ...draft.pack, heads: heads, lastModified: new Date().toISOString() }, true);
    },

    // "Save Pack": copies the open pack into the saved packs (the Load Pack list).
    // This is the only place a pack gets saved.
    saveDraft: function () {
        if (!this._draft) return false;

        const pack = { ...this._draft.pack, lastModified: new Date().toISOString() };
        const packs = this.getAllPacks();
        packs[pack.id] = pack;
        if (!this._writeAllPacks(packs)) return false;

        this._draft = { pack: pack, dirty: false };
        this._writeDraft();
        return true;
    },

    // Removes the saved copy and, if it is the open pack, closes it too.
    deletePack: function (packId) {
        let removed = false;

        const packs = this.getAllPacks();
        if (packs[packId]) {
            delete packs[packId];
            removed = this._writeAllPacks(packs);
        }

        if (this._draft && this._draft.pack.id === packId) {
            this.closeDraft();
            removed = true;
        }
        return removed;
    },

    // Deletes every saved pack. The open pack stays open unless it was one of the saved packs.
    // Returns true when the open pack was closed.
    clearAllSaved: function () {
        const openId = this._draft ? this._draft.pack.id : null;
        const openWasSaved = openId !== null && this.isSaved(openId);

        this._writeAllPacks({});
        if (openWasSaved) this.closeDraft();
        return openWasSaved;
    },

    // Exports the SAVED copy of a pack (what the Load Pack list shows).
    exportPack: function (packId) {
        return this.getSavedPack(packId);
    },

    // Importing a pack file adds it straight to the saved packs.
    importPack: function (packData) {
        if (!packData || !packData.name || !Array.isArray(packData.heads)) {
            return null;
        }

        const packId = Date.now().toString();
        const packs = this.getAllPacks();

        packs[packId] = {
            ...packData,
            id: packId,
            created: packData.created || new Date().toISOString(),
            lastModified: new Date().toISOString()
        };

        return this._writeAllPacks(packs) ? packId : null;
    },

    getStorageUsage: function () {
        const packs = this.getAllPacks();
        const packsJson = JSON.stringify(packs);
        const used = new Blob([packsJson]).size;

        const total = 5 * 1024 * 1024;

        return {
            used,
            total,
            percentage: (used / total) * 100
        };
    }
};
