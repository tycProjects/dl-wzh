package com.qrstudio.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scan_history")
data class ScanRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val format: String,
    val timestamp: Long = System.currentTimeMillis(),
)

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ScanRecord>>

    @Insert
    suspend fun insert(record: ScanRecord)

    @Query("DELETE FROM scan_history")
    suspend fun clear()
}
