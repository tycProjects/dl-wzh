package com.fakesocial.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fakesocial.app.data.ChatItem
import com.fakesocial.app.data.Kind
import com.fakesocial.app.data.MockData
import com.fakesocial.app.data.Platform
import com.fakesocial.app.data.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditorViewModel(
    private val repository: ProjectRepository,
    platform: Platform,
    private val projectId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(MockData.default(platform))
    val state: StateFlow<MockData> = _state.asStateFlow()

    init {
        if (projectId > 0) {
            viewModelScope.launch {
                repository.get(projectId)?.let { loaded -> _state.value = loaded }
            }
        }
    }

    fun edit(block: (MockData) -> MockData) = _state.update(block)

    fun updateItem(index: Int, block: (ChatItem) -> ChatItem) = _state.update { s ->
        if (index !in s.items.indices) s
        else s.copy(items = s.items.toMutableList().also { it[index] = block(it[index]) })
    }

    fun addItem() = _state.update { s ->
        val isChat = s.effectiveKind == Kind.CHAT
        val next = ChatItem(
            author = if (isChat) "" else "New user",
            text = "",
            mine = isChat && (s.items.lastOrNull()?.mine == false),
            time = s.items.lastOrNull()?.time ?: s.time
        )
        s.copy(items = s.items + next)
    }

    fun removeItem(index: Int) = _state.update { s ->
        if (index !in s.items.indices) s else s.copy(items = s.items.filterIndexed { i, _ -> i != index })
    }

    /** Persists the current state and returns the project id. */
    suspend fun save(): Long {
        val id = repository.save(_state.value)
        _state.update { it.copy(id = id) }
        return id
    }

    companion object {
        fun factory(repository: ProjectRepository, platform: Platform, projectId: Long) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EditorViewModel(repository, platform, projectId) as T
            }
    }
}
