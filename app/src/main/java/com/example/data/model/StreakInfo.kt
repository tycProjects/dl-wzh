package com.example.data.model

data class StreakInfo(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalCompletions: Int = 0,
    val completedToday: Int = 0,
    val totalTodayHabits: Int = 0,
    val todayCompletionRate: Float = 0f,
    val isStreakActiveToday: Boolean = false
) {
    val fireTierTitle: String
        get() = when {
            currentStreak == 0 -> "Вогник чекає на тебе"
            currentStreak in 1..3 -> "Маленька іскра"
            currentStreak in 4..7 -> "Впевнене полум'я"
            currentStreak in 8..14 -> "Гаряче багаття"
            currentStreak in 15..29 -> "Супернова звичок"
            else -> "Легендарний Вогник"
        }

    val fireTierEmoji: String
        get() = when {
            currentStreak == 0 -> "🕯️"
            currentStreak in 1..3 -> "🔥"
            currentStreak in 4..7 -> "🔥"
            currentStreak in 8..14 -> "🔥🔥"
            currentStreak in 15..29 -> "⚡🔥"
            else -> "👑🔥"
        }

    val fireLevelNumber: Int
        get() = when {
            currentStreak == 0 -> 0
            currentStreak in 1..3 -> 1
            currentStreak in 4..7 -> 2
            currentStreak in 8..14 -> 3
            currentStreak in 15..29 -> 4
            else -> 5
        }

    val motivationalQuote: String
        get() = when {
            currentStreak == 0 -> "Виконай сьогоднішні звички, щоб запалити вогник!"
            currentStreak in 1..3 -> "Чудовий початок! Іскра вже горить, підтримуй її!"
            currentStreak in 4..7 -> "Твій вогник розгоряється дедалі яскравіше!"
            currentStreak in 8..14 -> "Неймовірна дисципліна! Ти на правильному шляху!"
            currentStreak in 15..29 -> "Супер-серія! Звичка стає твоєю другою натурою!"
            else -> "Легендарний результат! Ти володар вогню самодисципліни!"
        }

    val nextMilestoneDays: Int
        get() = when {
            currentStreak < 3 -> 3
            currentStreak < 7 -> 7
            currentStreak < 14 -> 14
            currentStreak < 21 -> 21
            currentStreak < 30 -> 30
            currentStreak < 50 -> 50
            currentStreak < 100 -> 100
            else -> currentStreak + 50
        }
}
