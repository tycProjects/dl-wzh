package com.everline.lifesim.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Schema v1. Ids are stable UUIDs; all money is in minor units of the save's country currency. */
@Serializable
@Entity(tableName = "save_slots")
data class SaveSlotEntity(
    @PrimaryKey val id: String,
    val title: String,
    val mode: String,
    val year: Int,
    val month: Int,
    val activeCharacterId: String,
    val countryCode: String,
    val seed: Long,
    val minSwitchAge: Int = 18,
    val guardianManagedMinors: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val schemaVersion: Int = 1
)

@Serializable
@Entity(tableName = "characters", indices = [Index("saveId"), Index(value = ["saveId", "id"], unique = true)])
data class CharacterEntity(
    @PrimaryKey val id: String,
    val saveId: String,
    val firstName: String,
    val lastName: String,
    val gender: String,
    val birthYear: Int,
    val birthMonth: Int,
    val birthDay: Int,
    val countryCode: String,
    val nationality: String,
    val city: String,
    val appearance: String = "Unspecified",
    val health: Int = 80,
    val intelligence: Int = 60,
    val charisma: Int = 60,
    val talent: Int = 60,
    val happiness: Int = 65,
    val personality: String = "Balanced",
    val skills: String = "",
    val interests: String = "",
    val school: String = "",
    val university: String = "",
    val degree: String = "",
    val major: String = "",
    val educationStage: String = "None",
    val enrollmentTarget: String = "",
    val educationMonths: Int = 0,
    val occupation: String = "Unemployed",
    val employer: String = "",
    val professionalRank: String = "",
    val careerMonths: Int = 0,
    val monthlySalaryCents: Long = 0,
    val militaryBranch: String = "",
    val militaryRank: String = "",
    val cashCents: Long = 0,
    val assetsCents: Long = 0,
    val debtCents: Long = 0,
    val livingCostCents: Long = 0,
    val isAlive: Boolean = true,
    val retired: Boolean = false
)

/** PARENT means fromId -> child toId. Other edges are explicit and never inferred as parents. */
@Serializable
@Entity(
    tableName = "relationships",
    indices = [Index("saveId"), Index(value = ["saveId", "fromId", "toId", "kind"], unique = true)]
)
data class RelationshipEntity(
    @PrimaryKey val id: String,
    val saveId: String,
    val fromId: String,
    val toId: String,
    val kind: String,
    val quality: Int = 70
)

@Serializable
@Entity(tableName = "life_events", indices = [Index("saveId"), Index(value = ["saveId", "sourceKey"], unique = true)])
data class LifeEventEntity(
    @PrimaryKey val id: String,
    val saveId: String,
    val characterId: String?,
    val year: Int,
    val month: Int,
    val category: String,
    val title: String,
    val description: String,
    val sourceKey: String
)

/** At most one entry per sourceKey, enforced by Room's unique index in addition to transactions. */
@Serializable
@Entity(tableName = "financial_ledger", indices = [Index("saveId"), Index("characterId"), Index(value = ["saveId", "sourceKey"], unique = true)])
data class LedgerEntryEntity(
    @PrimaryKey val id: String,
    val saveId: String,
    val characterId: String,
    val year: Int,
    val month: Int,
    val kind: String,
    val sourceKey: String,
    val cashDeltaCents: Long,
    val debtDeltaCents: Long,
    val note: String
)

/** Structured educational, work, military and other chronology; not a replacement for current state. */
@Serializable
@Entity(tableName = "profile_records", indices = [Index("saveId"), Index("characterId")])
data class ProfileRecordEntity(
    @PrimaryKey val id: String,
    val saveId: String,
    val characterId: String,
    val category: String,
    val title: String,
    val organization: String,
    val startYear: Int,
    val endYear: Int? = null,
    val details: String = ""
)

@Serializable
@Entity(tableName = "edit_audit", indices = [Index("saveId"), Index("targetCharacterId")])
data class EditAuditEntity(
    @PrimaryKey val id: String,
    val saveId: String,
    val targetCharacterId: String,
    val actorCharacterId: String,
    val year: Int,
    val month: Int,
    val field: String,
    val previousValue: String,
    val newValue: String,
    val sandboxOverride: Boolean
)

@Serializable
@Entity(tableName = "switch_log", indices = [Index("saveId")])
data class SwitchLogEntity(
    @PrimaryKey val id: String,
    val saveId: String,
    val fromCharacterId: String,
    val toCharacterId: String,
    val year: Int,
    val month: Int,
    val guardianManaged: Boolean
)
