package com.logalyze.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.logalyze.app.data.AppDatabase
import com.logalyze.app.data.toEntity
import com.logalyze.app.data.toItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).historyDao()

    val items: StateFlow<List<HistoryItem>> = dao.observeAll()
        .map { list -> list.map { it.toItem() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(item: HistoryItem) {
        viewModelScope.launch { dao.insert(item.toEntity()) }
    }

    fun setResolved(id: Long, value: Boolean) {
        viewModelScope.launch { dao.setResolved(id, value) }
    }

    fun delete(item: HistoryItem) {
        viewModelScope.launch { dao.deleteById(item.id) }
    }

    fun clearAll() {
        viewModelScope.launch { dao.clearAll() }
    }
}
