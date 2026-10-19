package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "HEALTH",
    val iconName: String = "flame",
    val colorHex: Long = 0xFFFF5722,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val reminderDays: String = "1,2,3,4,5,6,7", // 1=Mon .. 7=Sun
    val targetDaysPerWeek: Int = 7,
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)
