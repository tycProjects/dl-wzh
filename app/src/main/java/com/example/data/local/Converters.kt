package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.LanguageDirection

class Converters {
    @TypeConverter
    fun fromLanguageDirection(direction: LanguageDirection?): String {
        return direction?.name ?: LanguageDirection.BISAYA_TO_SUBANEN.name
    }

    @TypeConverter
    fun toLanguageDirection(value: String?): LanguageDirection {
        return try {
            if (value != null) LanguageDirection.valueOf(value) else LanguageDirection.BISAYA_TO_SUBANEN
        } catch (e: Exception) {
            LanguageDirection.BISAYA_TO_SUBANEN
        }
    }
}
