package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dictionary_entries",
    indices = [
        Index(value = ["bisayaWord"]),
        Index(value = ["subanenWord"]),
        Index(value = ["isFavorite"])
    ]
)
data class DictionaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val bisayaWord: String,
    val subanenWord: String,
    val bisayaMeaning: String = "",
    val subanenMeaning: String = "",
    val partOfSpeech: String = "",
    val exampleSentence: String = "",
    val notes: String = "",
    val alternativeTranslations: String = "",
    val verificationStatus: String = STATUS_NEEDS_VERIFICATION,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_VERIFIED = "Verified"
        const val STATUS_NEEDS_VERIFICATION = "Needs verification"
    }

    val isVerified: Boolean
        get() = verificationStatus.equals(STATUS_VERIFIED, ignoreCase = true)

    fun getSourceWord(direction: LanguageDirection): String {
        return when (direction) {
            LanguageDirection.BISAYA_TO_SUBANEN -> bisayaWord
            LanguageDirection.SUBANEN_TO_BISAYA -> subanenWord
        }
    }

    fun getTranslationWord(direction: LanguageDirection): String {
        return when (direction) {
            LanguageDirection.BISAYA_TO_SUBANEN -> subanenWord
            LanguageDirection.SUBANEN_TO_BISAYA -> bisayaWord
        }
    }

    fun getMeaning(direction: LanguageDirection): String {
        return when (direction) {
            LanguageDirection.BISAYA_TO_SUBANEN -> {
                if (bisayaMeaning.isNotBlank()) bisayaMeaning else subanenMeaning
            }
            LanguageDirection.SUBANEN_TO_BISAYA -> {
                if (subanenMeaning.isNotBlank()) subanenMeaning else bisayaMeaning
            }
        }
    }
}
