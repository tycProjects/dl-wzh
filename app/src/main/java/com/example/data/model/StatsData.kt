package com.example.data.model

import androidx.compose.ui.graphics.Color

data class StatsData(
    val streakInfo: StreakInfo = StreakInfo(),
    val weeklyCompletionRate: Float = 0f,
    val monthlyCompletionRate: Float = 0f,
    val allTimeCompletionRate: Float = 0f,
    val weeklyChartBars: List<DailyBarData> = emptyList(),
    val monthlyHeatmapDays: List<HeatmapDayData> = emptyList(),
    val categoryStats: List<CategoryStatData> = emptyList(),
    val habitStreaks: List<HabitStreakRow> = emptyList(),
    val milestones: List<MilestoneData> = emptyList()
)

data class DailyBarData(
    val date: String,
    val label: String, // e.g. "Пн", "Вт"
    val completedCount: Int,
    val totalCount: Int,
    val completionRatio: Float // 0f .. 1f
)

data class HeatmapDayData(
    val date: String,
    val dayNumber: Int,
    val completionRatio: Float, // 0f, 0.5f, 1f etc
    val completedCount: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean
)

data class CategoryStatData(
    val category: HabitCategory,
    val habitCount: Int,
    val completedCount: Int,
    val color: Color
)

data class HabitStreakRow(
    val habitId: Long,
    val title: String,
    val iconName: String,
    val colorHex: Long,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompletions: Int
)

data class MilestoneData(
    val id: String,
    val requiredStreak: Int,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val isUnlocked: Boolean,
    val unlockedDate: String? = null
)
