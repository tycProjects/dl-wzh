package com.everline.lifesim.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.everline.lifesim.EverlineApplication
import com.everline.lifesim.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GameUiState(
    val slot: SaveSlotEntity? = null,
    val characters: List<CharacterEntity> = emptyList(),
    val relationships: List<RelationshipEntity> = emptyList(),
    val events: List<LifeEventEntity> = emptyList(),
    val edits: List<EditAuditEntity> = emptyList(),
    val switches: List<SwitchLogEntity> = emptyList()
) {
    val active: CharacterEntity? get() = characters.firstOrNull { it.id == slot?.activeCharacterId }
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    val repo = (application as EverlineApplication).repository
    private var pendingExportSlotId: String? = null
    fun prepareExport(slotId: String) { pendingExportSlotId = slotId }
    private val selectedSlot = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    val slots = repo.observeSlots().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val game: StateFlow<GameUiState> = selectedSlot.flatMapLatest { id ->
        if (id == null) flowOf(GameUiState()) else {
            val core = combine(repo.observeSlot(id), repo.observeCharacters(id),
                repo.observeRelationships(id), repo.observeEvents(id)) { slot, people, links, events ->
                GameUiState(slot = slot, characters = people, relationships = links, events = events)
            }
            combine(core, repo.observeEdits(id), repo.observeSwitches(id)) { state, edits, switches ->
                state.copy(edits = edits, switches = switches)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GameUiState())

    fun selectSave(id: String) { selectedSlot.value = id }
    fun clearMessage() { message.value = null }
    fun showError(text: String) { message.value = text }
    private fun runAction(action: suspend () -> Unit, success: (() -> Unit)? = null) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            try {
                action()
                success?.invoke()
            } catch (e: Exception) {
                message.value = e.message ?: "An operation could not be completed. Save remains unchanged."
            } finally { busy.value = false }
        }
    }
    fun createLife(draft: NewLifeDraft, onCreated: () -> Unit) = runAction({
        val id = repo.createLife(draft)
        selectSave(id)
    }, onCreated)
    fun advanceMonth() { game.value.slot?.id?.let { id -> runAction({ repo.advanceMonth(id) }) } }
    fun applyJob(jobId: String) { game.value.slot?.id?.let { runAction({ repo.applyForJob(it, jobId) }) } }
    fun resign() { game.value.slot?.id?.let { runAction({ repo.resign(it) }) } }
    fun retire() { game.value.slot?.id?.let { runAction({ repo.retire(it) }) } }
    fun enroll(target: String) { game.value.slot?.id?.let { runAction({ repo.enroll(it, target) }) } }
    fun doActivity(activity: String) { game.value.slot?.id?.let { runAction({ repo.doActivity(it, activity) }) } }
    fun socialize(targetId: String) { game.value.slot?.id?.let { runAction({ repo.socialize(it, targetId) }) } }
    fun switchCharacter(targetId: String) { game.value.slot?.id?.let { runAction({ repo.switchCharacter(it, targetId) }) } }
    fun saveEditedCharacter(character: CharacterEntity, override: Boolean, onSaved: () -> Unit) {
        game.value.slot?.id?.let { id -> runAction({ repo.updateCharacter(id, character, override) }, onSaved) }
    }
    fun addRelative(anchorId: String, relation: String, name: String, year: Int, override: Boolean, onDone: () -> Unit) {
        game.value.slot?.id?.let { id -> runAction({ repo.addRelative(id, anchorId, relation, name, year, override) }, onDone) }
    }
    fun editBasicSandbox(health: Int, happiness: Int, cashCents: Long) {
        game.value.slot?.id?.let { id -> runAction({ repo.updateBasicSandbox(id, health, happiness, cashCents) }) }
    }
    fun setQuality(relationId: String, quality: Int) {
        game.value.slot?.id?.let { id -> runAction({ repo.setRelationshipQuality(id, relationId, quality) }) }
    }
    fun addProfileRecord(characterId: String, category: String, title: String, organization: String,
        startYear: Int, endYear: Int?, details: String) {
        game.value.slot?.id?.let { id -> runAction({ repo.addProfileRecord(id, characterId, category,
            title, organization, startYear, endYear, details) }) }
    }
    fun switchPolicy(age: Int, guardian: Boolean) {
        game.value.slot?.id?.let { id -> runAction({ repo.setSwitchPolicy(id, age, guardian) }) }
    }
    fun manualSave() { game.value.slot?.id?.let { id -> runAction({ repo.manualSave(id) }, { message.value = "Save validated and stored locally." }) } }
    fun exportTo(uri: Uri?) {
        val id = pendingExportSlotId ?: game.value.slot?.id
        pendingExportSlotId = null
        if (uri == null || id == null) return
        runAction({
            val json = repo.exportBackup(id)
            withContext(Dispatchers.IO) {
                val stream = getApplication<Application>().contentResolver.openOutputStream(uri)
                    ?: error("Cannot open export destination.")
                stream.bufferedWriter().use { it.write(json) }
            }
        }, { message.value = "Backup exported." })
    }
    fun importFrom(uri: Uri?, onImported: () -> Unit) {
        if (uri == null) return
        runAction({
            val text = withContext(Dispatchers.IO) {
                val stream = getApplication<Application>().contentResolver.openInputStream(uri)
                    ?: error("Cannot read selected file.")
                stream.bufferedReader().use { it.readText() }
            }
            val id = repo.importBackup(text)
            selectSave(id)
        }, onImported)
    }
    fun deleteSave(id: String, onDeleted: () -> Unit) = runAction({
        repo.deleteSave(id)
        if (selectedSlot.value == id) selectedSlot.value = null
    }, onDeleted)
}
