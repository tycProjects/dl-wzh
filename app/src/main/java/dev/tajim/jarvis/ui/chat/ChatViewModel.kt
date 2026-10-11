package dev.tajim.jarvis.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.tajim.jarvis.AppContainer
import dev.tajim.jarvis.data.ChatMessage
import dev.tajim.jarvis.data.Role
import dev.tajim.jarvis.data.UserSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(private val container: AppContainer) : ViewModel() {
    val messages: StateFlow<List<ChatMessage>> = container.chatRepository.messages
    val settings: StateFlow<UserSettings> =
        container.settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    var sending by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var codeMode by mutableStateOf(false)
    private var job: Job? = null

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || sending) return
        launchRequest { container.aiService.ask(t, codeMode) }
    }

    fun retry(id: Long) {
        if (sending) return
        launchRequest { container.aiService.retry(id, codeMode) }
    }

    private fun launchRequest(block: suspend () -> Result<String>) {
        error = null
        sending = true
        job = viewModelScope.launch {
            try {
                block().onFailure { error = it.message ?: "The request failed." }
            } finally {
                sending = false
            }
        }
    }

    /** Stops the running request. The unanswered message is marked failed so it can be retried. */
    fun stop() {
        job?.cancel()
        viewModelScope.launch {
            container.chatRepository.messages.value.lastOrNull { it.role == Role.USER }?.let { m ->
                container.chatRepository.update(m.id) { it.copy(failed = true) }
            }
        }
        sending = false
    }

    fun newChat() { viewModelScope.launch { container.chatRepository.clear() }; error = null }
    fun dismissError() { error = null }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory { initializer { ChatViewModel(container) } }
    }
}
