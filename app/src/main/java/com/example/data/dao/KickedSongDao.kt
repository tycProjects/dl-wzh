package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.KickedSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KickedSongDao {
    @Query("SELECT * FROM kicked_songs ORDER BY dateKicked DESC")
    fun getAllKickedSongs(): Flow<List<KickedSongEntity>>

    @Query("SELECT uriString FROM kicked_songs")
    fun getAllKickedUris(): Flow<List<String>>

    @Query("SELECT * FROM kicked_songs WHERE id = :id LIMIT 1")
    suspend fun getKickedById(id: Long): KickedSongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKickedSong(kicked: KickedSongEntity)

    @Update
    suspend fun updateKickedSong(kicked: KickedSongEntity)

    @Query("DELETE FROM kicked_songs WHERE id = :id")
    suspend fun deleteKickedSongById(id: Long)

    @Query("SELECT COUNT(*) FROM kicked_songs")
    fun getKickedCount(): Flow<Int>
}
