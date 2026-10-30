package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM secret_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<MessageRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(record: MessageRecord): Long

    @Query("DELETE FROM secret_messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM secret_messages")
    suspend fun clearAll()
}
