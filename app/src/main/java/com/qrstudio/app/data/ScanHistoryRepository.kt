package com.qrstudio.app.data

import kotlinx.coroutines.flow.Flow

class ScanHistoryRepository(private val dao: ScanHistoryDao) {

    fun observe(): Flow<List<ScanRecord>> = dao.observeAll()

    suspend fun add(content: String, format: String) {
        if (content.isBlank()) return
        dao.insert(ScanRecord(content = content, format = format))
    }

    suspend fun clear() = dao.clear()
}
