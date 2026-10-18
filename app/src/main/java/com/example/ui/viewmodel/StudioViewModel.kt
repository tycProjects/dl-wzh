package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.model.CreationItem
import com.example.data.model.StudioMode
import com.example.data.repository.CreativeEngineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StudioUiState(
    val selectedMode: StudioMode = StudioMode.ONE_TOUCH,
    val creations: List<CreationItem> = emptyList(),
    val activeSandboxItem: CreationItem? = null,
    val isGenerating: Boolean = false,
    val generationProgressMessage: String = "",
    val isOneTouchModalOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val promptInput: String = "",
    val selectedStyle: String = "افتراضي",
    val searchQuery: String = "",
    val isGeminiConnected: Boolean = false
)

class StudioViewModel(
    private val repository: CreativeEngineRepository = CreativeEngineRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StudioUiState(
            isGeminiConnected = GeminiClient.isApiKeyConfigured()
        )
    )
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.creations.collect { list ->
                _uiState.value = _uiState.value.copy(
                    creations = list,
                    isGeminiConnected = GeminiClient.isApiKeyConfigured()
                )
            }
        }
    }

    fun selectMode(mode: StudioMode) {
        _uiState.value = _uiState.value.copy(selectedMode = mode)
    }

    fun setPrompt(prompt: String) {
        _uiState.value = _uiState.value.copy(promptInput = prompt)
    }

    fun setStyle(style: String) {
        _uiState.value = _uiState.value.copy(selectedStyle = style)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun openSandbox(item: CreationItem) {
        _uiState.value = _uiState.value.copy(activeSandboxItem = item)
    }

    fun closeSandbox() {
        _uiState.value = _uiState.value.copy(activeSandboxItem = null)
    }

    fun openOneTouchModal() {
        _uiState.value = _uiState.value.copy(isOneTouchModalOpen = true)
    }

    fun closeOneTouchModal() {
        _uiState.value = _uiState.value.copy(isOneTouchModalOpen = false)
    }

    fun openSettings() {
        _uiState.value = _uiState.value.copy(isSettingsOpen = true)
    }

    fun closeSettings() {
        _uiState.value = _uiState.value.copy(isSettingsOpen = false)
    }

    fun toggleFavorite(id: String) {
        repository.toggleFavorite(id)
    }

    fun deleteItem(id: String) {
        repository.deleteItem(id)
    }

    fun generateCurrentPrompt() {
        val prompt = _uiState.value.promptInput.trim()
        if (prompt.isBlank()) return

        val mode = _uiState.value.selectedMode
        val style = _uiState.value.selectedStyle

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                generationProgressMessage = "جاري تشغيل محرك الذكاء الاصطناعي لتوليد ${mode.titleAr}..."
            )

            if (mode == StudioMode.ONE_TOUCH) {
                val results = repository.generateOneTouchBundle(prompt)
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    promptInput = "",
                    activeSandboxItem = results.firstOrNull()
                )
            } else {
                val result = repository.generateSingle(mode, prompt, style)
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    promptInput = "",
                    activeSandboxItem = result
                )
            }
        }
    }

    fun executeOneTouchIdea(idea: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                isOneTouchModalOpen = false,
                generationProgressMessage = "جاري إنشاء التطبيق، اللعبة، الموقع، والتصاميم بلمسة واحدة..."
            )
            val results = repository.generateOneTouchBundle(idea)
            _uiState.value = _uiState.value.copy(
                isGenerating = false,
                activeSandboxItem = results.firstOrNull()
            )
        }
    }
}
