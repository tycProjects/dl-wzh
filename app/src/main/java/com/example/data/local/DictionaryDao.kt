package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DictionaryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DictionaryDao {

    @Query("SELECT * FROM dictionary_entries ORDER BY bisayaWord ASC")
    fun getAllEntries(): Flow<List<DictionaryEntry>>

    @Query("SELECT COUNT(*) FROM dictionary_entries")
    fun getEntryCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM dictionary_entries WHERE verificationStatus = 'Verified'")
    fun getVerifiedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM dictionary_entries WHERE verificationStatus = 'Needs verification'")
    fun getNeedsVerificationCount(): Flow<Int>

    @Query("SELECT * FROM dictionary_entries WHERE isFavorite = 1 ORDER BY bisayaWord ASC")
    fun getFavorites(): Flow<List<DictionaryEntry>>

    @Query("SELECT * FROM dictionary_entries WHERE id = :id LIMIT 1")
    fun getEntryById(id: Long): Flow<DictionaryEntry?>

    // Search Bisaya -> Subanen: prioritizes exact matches, then prefix matches, then substring matches
    @Query("""
        SELECT * FROM dictionary_entries 
        WHERE bisayaWord LIKE '%' || :query || '%' 
           OR bisayaMeaning LIKE '%' || :query || '%'
           OR alternativeTranslations LIKE '%' || :query || '%'
        ORDER BY 
            CASE 
                WHEN LOWER(bisayaWord) = LOWER(:query) THEN 0 
                WHEN LOWER(bisayaWord) LIKE LOWER(:query) || '%' THEN 1 
                ELSE 2 
            END,
            bisayaWord ASC
    """)
    fun searchBisaya(query: String): Flow<List<DictionaryEntry>>

    // Search Subanen -> Bisaya: prioritizes exact matches, then prefix matches, then substring matches
    @Query("""
        SELECT * FROM dictionary_entries 
        WHERE subanenWord LIKE '%' || :query || '%' 
           OR subanenMeaning LIKE '%' || :query || '%'
           OR alternativeTranslations LIKE '%' || :query || '%'
        ORDER BY 
            CASE 
                WHEN LOWER(subanenWord) = LOWER(:query) THEN 0 
                WHEN LOWER(subanenWord) LIKE LOWER(:query) || '%' THEN 1 
                ELSE 2 
            END,
            subanenWord ASC
    """)
    fun searchSubanen(query: String): Flow<List<DictionaryEntry>>

    @Query("UPDATE dictionary_entries SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DictionaryEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<DictionaryEntry>)

    @Update
    suspend fun updateEntry(entry: DictionaryEntry)

    @Query("DELETE FROM dictionary_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Query("DELETE FROM dictionary_entries")
    suspend fun deleteAllEntries()
}
