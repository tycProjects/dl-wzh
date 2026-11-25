package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val role: String, // "user", "maya", "system"
    val text: String,
    val sourceTag: String = "gemini", // "local", "gemini", "device"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "command_aliases")
data class CommandAliasEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val triggerPhrase: String,
    val targetPackageName: String,
    val targetAppLabel: String,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun observeSessions(): Flow<List<ChatSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity): Long

    @Query("UPDATE chat_sessions SET title = :title, updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun updateSessionTitle(sessionId: Long, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE chat_sessions SET updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun touchSession(sessionId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC, id ASC")
    fun observeMessages(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC, id ASC")
    suspend fun getMessagesSnapshot(sessionId: Long): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("UPDATE chat_messages SET text = :text WHERE id = :messageId")
    suspend fun updateMessageText(messageId: Long, text: String)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun clearSessionMessages(sessionId: Long)
}

@Dao
interface CommandAliasDao {
    @Query("SELECT * FROM command_aliases ORDER BY createdAt DESC")
    fun observeAliases(): Flow<List<CommandAliasEntity>>

    @Query("SELECT * FROM command_aliases WHERE enabled = 1")
    suspend fun getEnabledAliases(): List<CommandAliasEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlias(alias: CommandAliasEntity): Long

    @Update
    suspend fun updateAlias(alias: CommandAliasEntity)

    @Query("DELETE FROM command_aliases WHERE id = :id")
    suspend fun deleteAliasById(id: Long)
}

@Database(
    entities = [ChatSessionEntity::class, ChatMessageEntity::class, CommandAliasEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MayaDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun commandAliasDao(): CommandAliasDao

    companion object {
        @Volatile
        private var INSTANCE: MayaDatabase? = null

        fun getInstance(context: Context): MayaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MayaDatabase::class.java,
                    "maya_assistant.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
