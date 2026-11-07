package com.example.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.RadioTimelineCalculator

/**
 * Persistent named Radio DJ program schedule (e.g., "JNMF Morning Radio", "JNMF Night Program").
 * Stored independently from normal [PlaylistEntity] tables so normal playlists are never altered.
 */
@Entity(tableName = "radio_schedules")
data class RadioScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startTimeSecondsOfDay: Int = RadioTimelineCalculator.DEFAULT_START_TIME_SECONDS,
    val isAutoScheduled: Boolean = false,
    val timeAnnouncementEnabled: Boolean = false,
    val hourlyAnnouncementEnabled: Boolean = false,
    val timeAnnouncementAudioUri: String = "",
    val timeAnnouncementTitle: String = "",
    val timeAnnouncementDurationMs: Long = 0L,
    val customAnnouncementMinutes: String = "0",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Persistent ordered slot inside a [RadioScheduleEntity].
 * Uses an auto-generated row [id] so the same jingle, station ID, advertisement, or song can
 * appear at multiple distinct positions within the same radio broadcast schedule.
 */
@Entity(
    tableName = "radio_schedule_items",
    indices = [Index(value = ["scheduleId"])]
)
data class RadioScheduleItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scheduleId: Long,
    val orderIndex: Int,
    val itemType: String,
    val songId: Long,
    val title: String,
    val subtitle: String,
    val contentUri: String,
    val durationMs: Long,
    val fileName: String,
    val gainPercent: Int = 100
)
