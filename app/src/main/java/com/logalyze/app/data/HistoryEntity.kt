package com.logalyze.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val id: Long,
    val fileName: String,
    val resultJson: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "0") val resolved: Boolean = false
)
