package com.example.data.repository

import android.content.Context
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitDao
import com.example.data.local.HabitEntity
import com.example.data.model.CategoryStatData
import com.example.data.model.DailyBarData
import com.example.data.model.DayStatus
import com.example.data.model.HabitCategory
import com.example.data.model.HabitItemUi
import com.example.data.model.HabitStreakRow
import com.example.data.model.HeatmapDayData
import com.example.data.model.MilestoneData
import com.example.data.model.StatsData
import com.example.data.model.StreakInfo
import com.example.reminder.ReminderScheduler
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HabitRepository(
    private val context: Context,
    private val habitDao: HabitDao
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dayNameFormat = SimpleDateFormat("EEE", Locale("uk"))

    fun todayDateString(): String = dateFormat.format(Date())

    val activeHabits: Flow<List<HabitEntity>> = habitDao.getAllActiveHabits()
    val allHabits: Flow<List<HabitEntity>> = habitDao.getAllHabits()
    val allCompletions: Flow<List<HabitCompletionEntity>> = habitDao.getAllCompletions()

    /**
     * Combined flow providing the list of active habits with completion status for today,
     * calculated streaks, and 7-day history dots.
     */
    val habitItemsUi: Flow<List<HabitItemUi>> = combine(
        habitDao.getAllActiveHabits(),
        habitDao.getAllCompletions()
    ) { habits, completions ->
        val today = todayDateString()
        val completionsByHabit = completions.groupBy { it.habitId }

        habits.map { habit ->
            val habitCompletions = completionsByHabit[habit.id] ?: emptyList()
            val completedDatesSet = habitCompletions.map { it.date }.toSet()
            val isCompletedToday = completedDatesSet.contains(today)

            val currentStreak = calculateIndividualStreak(completedDatesSet, today)
            val longestStreak = calculateLongestStreak(completedDatesSet)

            val last7Days = generateLast7Days(completedDatesSet, today)

            HabitItemUi(
                habit = habit,
                isCompletedToday = isCompletedToday,
                currentStreak = currentStreak,
                longestStreak = maxOf(longestStreak, currentStreak),
                totalCompletions = habitCompletions.size,
                last7DaysCompletion = last7Days
            )
        }
    }

    /**
     * Overall streak info flow.
     */
    val streakInfoFlow: Flow<StreakInfo> = combine(
        habitDao.getAllActiveHabits(),
        habitDao.getAllCompletions()
    ) { habits, completions ->
        calculateOverallStreak(habits, completions)
    }

    /**
     * Deep statistics flow for StatsScreen.
     */
    val statsFlow: Flow<StatsData> = combine(
        habitDao.getAllActiveHabits(),
        habitDao.getAllCompletions()
    ) { habits, completions ->
        calculateDetailedStats(habits, completions)
    }

    suspend fun toggleHabitCompletion(habitId: Long, date: String = todayDateString()): Boolean =
        withContext(Dispatchers.IO) {
            val existing = habitDao.getCompletionsForDateDirect(date).firstOrNull { it.habitId == habitId }
            val completedNow: Boolean
            if (existing != null) {
                habitDao.deleteCompletion(habitId, date)
                completedNow = false
            } else {
                habitDao.insertCompletion(
                    HabitCompletionEntity(
                        habitId = habitId,
                        date = date,
                        completedAt = System.currentTimeMillis()
                    )
                )
                completedNow = true
            }

            // Sync widget immediately
            WidgetUpdater.updateAllWidgets(context)
            completedNow
        }

    suspend fun saveHabit(habit: HabitEntity): Long = withContext(Dispatchers.IO) {
        val id = if (habit.id == 0L) {
            habitDao.insertHabit(habit)
        } else {
            habitDao.updateHabit(habit)
            habit.id
        }

        // Manage reminder
        val updatedHabit = habit.copy(id = id)
        if (updatedHabit.reminderEnabled) {
            ReminderScheduler.scheduleReminder(context, updatedHabit)
        } else {
            ReminderScheduler.cancelReminder(context, updatedHabit.id)
        }

        WidgetUpdater.updateAllWidgets(context)
        id
    }

    suspend fun deleteHabit(habitId: Long) = withContext(Dispatchers.IO) {
        ReminderScheduler.cancelReminder(context, habitId)
        habitDao.deleteCompletionsForHabit(habitId)
        habitDao.deleteHabitById(habitId)
        WidgetUpdater.updateAllWidgets(context)
    }

    suspend fun toggleArchiveHabit(habit: HabitEntity) = withContext(Dispatchers.IO) {
        val newArchived = !habit.isArchived
        habitDao.updateHabit(habit.copy(isArchived = newArchived))
        if (newArchived) {
            ReminderScheduler.cancelReminder(context, habit.id)
        } else if (habit.reminderEnabled) {
            ReminderScheduler.scheduleReminder(context, habit)
        }
        WidgetUpdater.updateAllWidgets(context)
    }

    private fun generateLast7Days(completedDates: Set<String>, today: String): List<DayStatus> {
        val list = mutableListOf<DayStatus>()
        val cal = Calendar.getInstance()

        for (i in 6 downTo 0) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = dateFormat.format(cal.time)
            val dayName = dayNameFormat.format(cal.time).replaceFirstChar { it.uppercase() }
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)

            list.add(
                DayStatus(
                    date = dateStr,
                    dayName = dayName,
                    dayOfMonth = dayOfMonth,
                    isCompleted = completedDates.contains(dateStr),
                    isToday = dateStr == today
                )
            )
        }
        return list
    }

    private fun calculateIndividualStreak(completedDates: Set<String>, today: String): Int {
        if (completedDates.isEmpty()) return 0

        val cal = Calendar.getInstance()
        var streak = 0

        // Check if completed today
        if (completedDates.contains(today)) {
            streak++
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            // Check yesterday
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = dateFormat.format(cal.time)
            if (!completedDates.contains(yesterday)) {
                return 0
            }
        }

        // Loop backwards
        while (true) {
            val dateStr = dateFormat.format(cal.time)
            if (completedDates.contains(dateStr)) {
                if (dateStr != today) streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return streak
    }

    private fun calculateLongestStreak(completedDates: Set<String>): Int {
        if (completedDates.isEmpty()) return 0
        val sorted = completedDates.sorted()
        var maxStreak = 1
        var current = 1
        val cal = Calendar.getInstance()

        for (i in 1 until sorted.size) {
            try {
                val prev = dateFormat.parse(sorted[i - 1]) ?: continue
                val curr = dateFormat.parse(sorted[i]) ?: continue
                cal.time = prev
                cal.add(Calendar.DAY_OF_YEAR, 1)

                if (dateFormat.format(cal.time) == dateFormat.format(curr)) {
                    current++
                    if (current > maxStreak) maxStreak = current
                } else if (prev != curr) {
                    current = 1
                }
            } catch (e: Exception) {
                // ignore parsing exceptions
            }
        }
        return maxStreak
    }

    private fun calculateOverallStreak(
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>
    ): StreakInfo {
        val today = todayDateString()
        val totalTodayHabits = habits.size
        val completedTodayCount = completions.count { it.date == today }
        val isStreakActiveToday = completedTodayCount > 0

        // Group completions by date
        val daysWithCompletions = completions.map { it.date }.toSet()

        val cal = Calendar.getInstance()
        var currentStreak = 0

        if (daysWithCompletions.contains(today)) {
            currentStreak++
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = dateFormat.format(cal.time)
            if (!daysWithCompletions.contains(yesterday)) {
                currentStreak = 0
            }
        }

        if (currentStreak > 0 || daysWithCompletions.contains(dateFormat.format(cal.time))) {
            while (true) {
                val checkDate = dateFormat.format(cal.time)
                if (daysWithCompletions.contains(checkDate)) {
                    if (checkDate != today) currentStreak++
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        }

        val longestStreak = maxOf(currentStreak, calculateLongestStreak(daysWithCompletions))
        val rate = if (totalTodayHabits > 0) completedTodayCount.toFloat() / totalTodayHabits else 0f

        return StreakInfo(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalCompletions = completions.size,
            completedToday = completedTodayCount,
            totalTodayHabits = totalTodayHabits,
            todayCompletionRate = rate,
            isStreakActiveToday = isStreakActiveToday
        )
    }

    private fun calculateDetailedStats(
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>
    ): StatsData {
        val streakInfo = calculateOverallStreak(habits, completions)
        val today = todayDateString()
        val cal = Calendar.getInstance()

        // 1. Weekly Chart (Last 7 days)
        val weeklyBars = mutableListOf<DailyBarData>()
        var weeklyCompletedSum = 0
        var weeklyTotalSum = 0

        for (i in 6 downTo 0) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = dateFormat.format(cal.time)
            val label = dayNameFormat.format(cal.time).replaceFirstChar { it.uppercase() }
            val countForDay = completions.count { it.date == dateStr }
            val totalForDay = habits.size.coerceAtLeast(1)
            val ratio = (countForDay.toFloat() / totalForDay).coerceIn(0f, 1f)

            weeklyCompletedSum += countForDay
            weeklyTotalSum += totalForDay
            weeklyBars.add(
                DailyBarData(
                    date = dateStr,
                    label = label,
                    completedCount = countForDay,
                    totalCount = totalForDay,
                    completionRatio = ratio
                )
            )
        }

        val weeklyRate = if (weeklyTotalSum > 0) weeklyCompletedSum.toFloat() / weeklyTotalSum else 0f

        // 2. Monthly Heatmap (Days of current month)
        val heatmapDays = mutableListOf<HeatmapDayData>()
        val monthCal = Calendar.getInstance()
        monthCal.time = Date()
        val currentMonth = monthCal.get(Calendar.MONTH)
        val currentYear = monthCal.get(Calendar.YEAR)
        monthCal.set(Calendar.DAY_OF_MONTH, 1)

        val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        var monthlyCompletedSum = 0
        var monthlyTotalSum = 0

        for (day in 1..daysInMonth) {
            monthCal.set(currentYear, currentMonth, day)
            val dateStr = dateFormat.format(monthCal.time)
            val countForDay = completions.count { it.date == dateStr }
            val totalForDay = habits.size.coerceAtLeast(1)
            val ratio = (countForDay.toFloat() / totalForDay).coerceIn(0f, 1f)

            monthlyCompletedSum += countForDay
            monthlyTotalSum += totalForDay

            heatmapDays.add(
                HeatmapDayData(
                    date = dateStr,
                    dayNumber = day,
                    completionRatio = ratio,
                    completedCount = countForDay,
                    isCurrentMonth = true,
                    isToday = dateStr == today
                )
            )
        }

        val monthlyRate = if (monthlyTotalSum > 0) monthlyCompletedSum.toFloat() / monthlyTotalSum else 0f

        // 3. Category Stats
        val categoryStats = HabitCategory.values().mapNotNull { cat ->
            val matchingHabits = habits.filter { it.category.equals(cat.name, ignoreCase = true) }
            if (matchingHabits.isEmpty()) null
            else {
                val matchingIds = matchingHabits.map { it.id }.toSet()
                val completedCount = completions.count { matchingIds.contains(it.habitId) }
                CategoryStatData(
                    category = cat,
                    habitCount = matchingHabits.size,
                    completedCount = completedCount,
                    color = cat.defaultColor
                )
            }
        }

        // 4. Habit Streaks Ranking
        val habitStreaks = habits.map { habit ->
            val dates = completions.filter { it.habitId == habit.id }.map { it.date }.toSet()
            val cur = calculateIndividualStreak(dates, today)
            val best = calculateLongestStreak(dates)
            HabitStreakRow(
                habitId = habit.id,
                title = habit.title,
                iconName = habit.iconName,
                colorHex = habit.colorHex,
                currentStreak = cur,
                longestStreak = maxOf(cur, best),
                totalCompletions = dates.size
            )
        }.sortedByDescending { it.currentStreak }

        // 5. Milestones
        val bestStreak = maxOf(streakInfo.currentStreak, streakInfo.longestStreak)
        val milestones = listOf(
            MilestoneData("m1", 1, "Перша іскра", "Зроби перший крок до регулярності", "🌱", bestStreak >= 1),
            MilestoneData("m3", 3, "Теплий вогник", "Утримуй серію 3 дні поспіль", "🔥", bestStreak >= 3),
            MilestoneData("m7", 7, "Тижневий розпал", "1 повний тиждень без перерви", "⚡", bestStreak >= 7),
            MilestoneData("m14", 14, "Два тижні сталі", "14 днів невпинного руху", "💎", bestStreak >= 14),
            MilestoneData("m21", 21, "Сила звички", "Науково доведений час формування звички", "🧠", bestStreak >= 21),
            MilestoneData("m30", 30, "Місячний титан", "30 днів непохитної дисципліни", "🏆", bestStreak >= 30),
            MilestoneData("m50", 50, "Полум'яний майстер", "50 днів високої ефективності", "🌟", bestStreak >= 50),
            MilestoneData("m100", 100, "Легенда вогню", "100 днів абсолютного тріумфу", "👑", bestStreak >= 100)
        )

        return StatsData(
            streakInfo = streakInfo,
            weeklyCompletionRate = weeklyRate,
            monthlyCompletionRate = monthlyRate,
            allTimeCompletionRate = if (completions.isNotEmpty()) 1.0f else 0f,
            weeklyChartBars = weeklyBars,
            monthlyHeatmapDays = heatmapDays,
            categoryStats = categoryStats,
            habitStreaks = habitStreaks,
            milestones = milestones
        )
    }
}
