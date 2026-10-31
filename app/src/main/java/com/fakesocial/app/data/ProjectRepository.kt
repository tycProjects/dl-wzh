package com.fakesocial.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProjectRepository(private val dao: ProjectDao) {
    val projects: Flow<List<MockData>> = dao.observeAll().map { list -> list.map { it.toData() } }

    suspend fun get(id: Long): MockData? = dao.getById(id)?.toData()

    /** Inserts or updates and returns the row id. */
    suspend fun save(data: MockData): Long = dao.upsert(data.toEntity())

    suspend fun delete(id: Long) = dao.deleteById(id)
}
