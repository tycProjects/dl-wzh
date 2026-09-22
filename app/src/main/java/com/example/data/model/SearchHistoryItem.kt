package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val query: String,
    val direction: LanguageDirection = LanguageDirection.BISAYA_TO_SUBANEN,
    val timestamp: Long = System.currentTimeMillis()
)
