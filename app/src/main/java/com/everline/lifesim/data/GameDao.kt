package com.everline.lifesim.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM save_slots ORDER BY updatedAt DESC")
    fun observeSlots(): Flow<List<SaveSlotEntity>>
    @Query("SELECT * FROM save_slots WHERE id = :id LIMIT 1")
    fun observeSlot(id: String): Flow<SaveSlotEntity?>
    @Query("SELECT * FROM save_slots WHERE id = :id LIMIT 1")
    suspend fun getSlot(id: String): SaveSlotEntity?
    @Query("SELECT * FROM save_slots ORDER BY updatedAt DESC LIMIT 1")
    suspend fun newestSlot(): SaveSlotEntity?
    @Insert suspend fun insertSlot(slot: SaveSlotEntity)
    @Update suspend fun updateSlot(slot: SaveSlotEntity)

    @Query("SELECT * FROM characters WHERE saveId = :saveId ORDER BY birthYear ASC, firstName ASC")
    fun observeCharacters(saveId: String): Flow<List<CharacterEntity>>
    @Query("SELECT * FROM characters WHERE saveId = :saveId ORDER BY birthYear ASC, firstName ASC")
    suspend fun getCharacters(saveId: String): List<CharacterEntity>
    @Query("SELECT * FROM characters WHERE id = :id LIMIT 1")
    suspend fun getCharacter(id: String): CharacterEntity?
    @Insert suspend fun insertCharacter(character: CharacterEntity)
    @Update suspend fun updateCharacter(character: CharacterEntity)

    @Query("SELECT * FROM relationships WHERE saveId = :saveId")
    fun observeRelationships(saveId: String): Flow<List<RelationshipEntity>>
    @Query("SELECT * FROM relationships WHERE saveId = :saveId")
    suspend fun getRelationships(saveId: String): List<RelationshipEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertRelationship(relation: RelationshipEntity): Long
    @Update suspend fun updateRelationship(relation: RelationshipEntity)

    @Query("SELECT COUNT(*) FROM life_events WHERE saveId = :saveId AND sourceKey = :sourceKey")
    suspend fun eventExists(saveId: String, sourceKey: String): Int

    @Query("SELECT * FROM life_events WHERE saveId = :saveId ORDER BY year DESC, month DESC, rowid DESC LIMIT 100")
    fun observeEvents(saveId: String): Flow<List<LifeEventEntity>>
    @Query("SELECT * FROM life_events WHERE saveId = :saveId")
    suspend fun getAllEvents(saveId: String): List<LifeEventEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertEvent(event: LifeEventEntity): Long

    @Query("SELECT * FROM financial_ledger WHERE characterId = :characterId ORDER BY year DESC, month DESC, rowid DESC LIMIT 100")
    fun observeLedger(characterId: String): Flow<List<LedgerEntryEntity>>
    @Query("SELECT * FROM financial_ledger WHERE saveId = :saveId")
    suspend fun getAllLedger(saveId: String): List<LedgerEntryEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertLedger(entry: LedgerEntryEntity)

    @Query("SELECT * FROM profile_records WHERE characterId = :characterId ORDER BY startYear DESC")
    fun observeRecords(characterId: String): Flow<List<ProfileRecordEntity>>
    @Query("SELECT * FROM profile_records WHERE saveId = :saveId")
    suspend fun getAllRecords(saveId: String): List<ProfileRecordEntity>
    @Insert suspend fun insertRecord(record: ProfileRecordEntity)

    @Query("SELECT * FROM edit_audit WHERE saveId = :saveId ORDER BY rowid DESC LIMIT 150")
    fun observeEdits(saveId: String): Flow<List<EditAuditEntity>>
    @Query("SELECT * FROM edit_audit WHERE saveId = :saveId")
    suspend fun getAllEdits(saveId: String): List<EditAuditEntity>
    @Insert suspend fun insertEdit(edit: EditAuditEntity)

    @Query("SELECT * FROM switch_log WHERE saveId = :saveId ORDER BY rowid DESC")
    fun observeSwitches(saveId: String): Flow<List<SwitchLogEntity>>
    @Query("SELECT * FROM switch_log WHERE saveId = :saveId")
    suspend fun getAllSwitches(saveId: String): List<SwitchLogEntity>
    @Insert suspend fun insertSwitch(log: SwitchLogEntity)

    @Query("DELETE FROM save_slots WHERE id = :saveId") suspend fun deleteSlot(saveId: String)
    @Query("DELETE FROM characters WHERE saveId = :saveId") suspend fun deleteCharacters(saveId: String)
    @Query("DELETE FROM relationships WHERE saveId = :saveId") suspend fun deleteRelationships(saveId: String)
    @Query("DELETE FROM life_events WHERE saveId = :saveId") suspend fun deleteEvents(saveId: String)
    @Query("DELETE FROM financial_ledger WHERE saveId = :saveId") suspend fun deleteLedger(saveId: String)
    @Query("DELETE FROM profile_records WHERE saveId = :saveId") suspend fun deleteRecords(saveId: String)
    @Query("DELETE FROM edit_audit WHERE saveId = :saveId") suspend fun deleteEdits(saveId: String)
    @Query("DELETE FROM switch_log WHERE saveId = :saveId") suspend fun deleteSwitches(saveId: String)
}
