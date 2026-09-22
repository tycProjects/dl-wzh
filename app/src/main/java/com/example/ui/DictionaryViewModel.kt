package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.DictionaryEntry
import com.example.data.model.LanguageDirection
import com.example.data.model.SearchHistoryItem
import com.example.data.repository.DictionaryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DictionaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DictionaryRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DictionaryRepository(database.dictionaryDao(), database.historyDao())
        viewModelScope.launch {
            repository.ensureInitialData()
        }
    }

    private val _languageDirection = MutableStateFlow(LanguageDirection.BISAYA_TO_SUBANEN)
    val languageDirection: StateFlow<LanguageDirection> = _languageDirection.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    val entryCount: StateFlow<Int> = repository.entryCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val verifiedCount: StateFlow<Int> = repository.verifiedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val needsVerificationCount: StateFlow<Int> = repository.needsVerificationCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val favorites: StateFlow<List<DictionaryEntry>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentHistory: StateFlow<List<SearchHistoryItem>> = repository.recentHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResults: StateFlow<List<DictionaryEntry>> = combine(
        _searchQuery,
        _languageDirection
    ) { query, direction ->
        Pair(query, direction)
    }.flatMapLatest { (query, direction) ->
        if (query.isBlank()) {
            flowOf(emptyList())
        } else {
            repository.search(query, direction)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun swapLanguageDirection() {
        _languageDirection.value = _languageDirection.value.swapped()
    }

    fun toggleFavorite(entry: DictionaryEntry) {
        viewModelScope.launch {
            repository.toggleFavorite(entry.id, entry.isFavorite)
        }
    }

    fun submitSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            _searchQuery.value = trimmed
            viewModelScope.launch {
                repository.recordSearch(trimmed, _languageDirection.value)
            }
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    fun toggleDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
    }

    fun addWord(
        bisayaWord: String,
        subanenWord: String,
        bisayaMeaning: String,
        subanenMeaning: String,
        partOfSpeech: String,
        exampleSentence: String,
        notes: String,
        alternativeTranslations: String,
        verificationStatus: String
    ) {
        viewModelScope.launch {
            repository.addEntry(
                DictionaryEntry(
                    bisayaWord = bisayaWord.trim(),
                    subanenWord = subanenWord.trim(),
                    bisayaMeaning = bisayaMeaning.trim(),
                    subanenMeaning = subanenMeaning.trim(),
                    partOfSpeech = partOfSpeech.trim(),
                    exampleSentence = exampleSentence.trim(),
                    notes = notes.trim(),
                    alternativeTranslations = alternativeTranslations.trim(),
                    verificationStatus = verificationStatus
                )
            )
        }
    }

    fun resetToSampleData() {
        viewModelScope.launch {
            repository.resetToSampleData()
        }
    }
}
