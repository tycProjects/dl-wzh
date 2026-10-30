package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "secret_messages")
data class MessageRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "ENCODED" or "DECODED"
    val format: String, // "emoji", "character", "aes256"
    val originalText: String,
    val resultText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isProtected: Boolean = false
)
