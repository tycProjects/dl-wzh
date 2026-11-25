package com.example.data

import kotlinx.coroutines.flow.Flow

class MayaRepository(private val database: MayaDatabase) {
    private val chatDao = database.chatDao()
    private val aliasDao = database.commandAliasDao()

    val sessions: Flow<List<ChatSessionEntity>> = chatDao.observeSessions()
    val aliases: Flow<List<CommandAliasEntity>> = aliasDao.observeAliases()

    fun observeMessages(sessionId: Long): Flow<List<ChatMessageEntity>> =
        chatDao.observeMessages(sessionId)

    suspend fun getMessagesSnapshot(sessionId: Long): List<ChatMessageEntity> =
        chatDao.getMessagesSnapshot(sessionId)

    suspend fun createSession(title: String = "নতুন কথোপকথন"): Long =
        chatDao.insertSession(ChatSessionEntity(title = title))

    suspend fun renameSession(sessionId: Long, title: String) =
        chatDao.updateSessionTitle(sessionId, title.take(42))

    suspend fun deleteSession(sessionId: Long) {
        chatDao.clearSessionMessages(sessionId)
        chatDao.deleteSession(sessionId)
    }

    suspend fun addMessage(
        sessionId: Long,
        role: String,
        text: String,
        sourceTag: String = "gemini"
    ): Long {
        chatDao.touchSession(sessionId)
        return chatDao.insertMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = role,
                text = text,
                sourceTag = sourceTag
            )
        )
    }

    suspend fun updateMessageText(messageId: Long, text: String) {
        chatDao.updateMessageText(messageId, text)
    }

    suspend fun clearMessages(sessionId: Long) {
        chatDao.clearSessionMessages(sessionId)
    }

    suspend fun getEnabledAliases(): List<CommandAliasEntity> =
        aliasDao.getEnabledAliases()

    suspend fun saveAlias(alias: CommandAliasEntity): Long =
        aliasDao.insertAlias(alias)

    suspend fun updateAlias(alias: CommandAliasEntity) =
        aliasDao.updateAlias(alias)

    suspend fun deleteAlias(id: Long) =
        aliasDao.deleteAliasById(id)
}
