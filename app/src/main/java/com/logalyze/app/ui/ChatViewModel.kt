package com.logalyze.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logalyze.app.chat.ChatClient
import com.logalyze.app.chat.ChatMessage
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    val messages = mutableStateListOf<ChatMessage>()
    var sending by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var currentId: Long? = null

    fun start(id: Long) {
        if (currentId != id) {
            currentId = id
            messages.clear()
            error = null
        }
    }

    fun send(text: String, system: String) {
        val clean = text.trim()
        if (clean.isEmpty() || sending) return
        messages.add(ChatMessage(true, clean))
        sending = true
        error = null
        viewModelScope.launch {
            try {
                val reply = ChatClient.ask(system, messages.toList())
                messages.add(ChatMessage(false, reply.trim()))
            } catch (e: Exception) {
                error = e.message ?: "Error"
            } finally {
                sending = false
            }
        }
    }
}
