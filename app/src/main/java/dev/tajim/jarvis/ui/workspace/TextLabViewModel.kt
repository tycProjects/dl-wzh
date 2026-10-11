package dev.tajim.jarvis.ui.workspace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.tajim.jarvis.AppContainer
import dev.tajim.jarvis.data.DraftStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Persistence policy: the text is saved automatically ~0.4 s after each edit and again when the workspace
 * closes, so closing and reopening restores it. The lock flag is not persisted (it resets to unlocked).
 */
class TextLabViewModel(private val store: DraftStore) : ViewModel() {
    /** null until the saved draft has loaded. */
    var text by mutableStateOf<String?>(null)
        private set
    var locked by mutableStateOf(false)
        private set

    private var saveJob: Job? = null

    init { viewModelScope.launch { text = store.load() } }

    fun onTextChange(new: String) {
        if (locked) return
        text = new
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400)
            store.save(new)
        }
    }

    fun toggleLock() { locked = !locked }

    fun clear() { onTextChange("") }

    /** Writes the current text immediately and survives this ViewModel being cleared. */
    fun flush() {
        val t = text ?: return
        saveJob?.cancel()
        viewModelScope.launch(NonCancellable) { store.save(t) }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { TextLabViewModel(container.draftStore) }
        }
    }
}
