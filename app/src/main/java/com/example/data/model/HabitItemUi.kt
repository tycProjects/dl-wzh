package com.example.data.model

import com.example.data.local.HabitEntity

data class HabitItemUi(
    val habit: HabitEntity,
    val isCompletedToday: Boolean,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompletions: Int,
    val last7DaysCompletion: List<DayStatus> = emptyList()
)

data class DayStatus(
    val date: String,
    val dayName: String,
    val dayOfMonth: Int,
    val isCompleted: Boolean,
    val isToday: Boolean
)
