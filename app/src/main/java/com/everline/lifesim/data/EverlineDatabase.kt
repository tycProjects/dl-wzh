package com.everline.lifesim.data

import androidx.room.Database
import androidx.room.RoomDatabase

/** Initial Room schema. Future migrations must be explicit; destructive fallback is prohibited. */
@Database(
    entities = [SaveSlotEntity::class, CharacterEntity::class, RelationshipEntity::class,
        LifeEventEntity::class, LedgerEntryEntity::class, ProfileRecordEntity::class,
        EditAuditEntity::class, SwitchLogEntity::class],
    version = 1,
    exportSchema = true
)
abstract class EverlineDatabase : RoomDatabase() {
    abstract fun dao(): GameDao
}
