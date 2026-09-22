package com.example.data.repository

import com.example.data.local.DictionaryDao
import com.example.data.local.HistoryDao
import com.example.data.local.InitialDictionaryData
import com.example.data.model.DictionaryEntry
import com.example.data.model.LanguageDirection
import com.example.data.model.SearchHistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class DictionaryRepository(
    private val dictionaryDao: DictionaryDao,
    private val historyDao: HistoryDao
) {
    val allEntries: Flow<List<DictionaryEntry>> = dictionaryDao.getAllEntries()
    val entryCount: Flow<Int> = dictionaryDao.getEntryCount()
    val verifiedCount: Flow<Int> = dictionaryDao.getVerifiedCount()
    val needsVerificationCount: Flow<Int> = dictionaryDao.getNeedsVerificationCount()
    val favorites: Flow<List<DictionaryEntry>> = dictionaryDao.getFavorites()
    val recentHistory: Flow<List<SearchHistoryItem>> = historyDao.getRecentHistory(limit = 20)

    fun search(query: String, direction: LanguageDirection): Flow<List<DictionaryEntry>> {
        val trimmed = query.trim()
        return if (direction == LanguageDirection.BISAYA_TO_SUBANEN) {
            dictionaryDao.searchBisaya(trimmed)
        } else {
            dictionaryDao.searchSubanen(trimmed)
        }
    }

    suspend fun ensureInitialData() = withContext(Dispatchers.IO) {
        val count = dictionaryDao.getEntryCount().first()
        if (count == 0) {
            dictionaryDao.insertAll(InitialDictionaryData.SAMPLE_TEST_ENTRIES)
        }
    }

    suspend fun toggleFavorite(id: Long, currentFavorite: Boolean) = withContext(Dispatchers.IO) {
        dictionaryDao.setFavorite(id, !currentFavorite)
    }

    suspend fun recordSearch(query: String, direction: LanguageDirection) = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            historyDao.deleteByQuery(trimmed)
            historyDao.insertSearch(
                SearchHistoryItem(
                    query = trimmed,
                    direction = direction,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteHistoryItem(id: Long) = withContext(Dispatchers.IO) {
        historyDao.deleteSearch(id)
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        historyDao.clearAll()
    }

    suspend fun addEntry(entry: DictionaryEntry): Long = withContext(Dispatchers.IO) {
        dictionaryDao.insertEntry(entry)
    }

    suspend fun updateEntry(entry: DictionaryEntry) = withContext(Dispatchers.IO) {
        dictionaryDao.updateEntry(entry)
    }

    suspend fun deleteEntry(id: Long) = withContext(Dispatchers.IO) {
        dictionaryDao.deleteEntry(id)
    }

    suspend fun resetToSampleData() = withContext(Dispatchers.IO) {
        dictionaryDao.deleteAllEntries()
        dictionaryDao.insertAll(InitialDictionaryData.SAMPLE_TEST_ENTRIES)
    }
}
