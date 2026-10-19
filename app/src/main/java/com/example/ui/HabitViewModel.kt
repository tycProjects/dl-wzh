package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.HabitDatabase
import com.example.data.local.HabitEntity
import com.example.data.model.HabitItemUi
import com.example.data.model.StatsData
import com.example.data.model.StreakInfo
import com.example.data.repository.HabitRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HabitRepository

    init {
        val database = HabitDatabase.getDatabase(application)
        repository = HabitRepository(application, database.habitDao())
    }

    val habitItems: StateFlow<List<HabitItemUi>> = repository.habitItemsUi
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val streakInfo: StateFlow<StreakInfo> = repository.streakInfoFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StreakInfo()
        )

    val statsData: StateFlow<StatsData> = repository.statsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StatsData()
        )

    val allHabits: StateFlow<List<HabitEntity>> = repository.allHabits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _editingHabit = MutableStateFlow<HabitEntity?>(null)
    val editingHabit: StateFlow<HabitEntity?> = _editingHabit.asStateFlow()

    private val _isAddEditOpen = MutableStateFlow(false)
    val isAddEditOpen: StateFlow<Boolean> = _isAddEditOpen.asStateFlow()

    // Trigger for fire burst / confetti celebration
    private val _celebrationEvent = MutableSharedFlow<String>()
    val celebrationEvent: SharedFlow<String> = _celebrationEvent.asSharedFlow()

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun openAddHabit() {
        _editingHabit.value = null
        _isAddEditOpen.value = true
    }

    fun openEditHabit(habit: HabitEntity) {
        _editingHabit.value = habit
        _isAddEditOpen.value = true
    }

    fun dismissAddEdit() {
        _editingHabit.value = null
        _isAddEditOpen.value = false
    }

    fun toggleHabit(habitId: Long) {
        viewModelScope.launch {
            val newlyCompleted = repository.toggleHabitCompletion(habitId)
            if (newlyCompleted) {
                val currentItems = habitItems.value
                val remainingUnfinished = currentItems.count { it.habit.id != habitId && !it.isCompletedToday }
                if (remainingUnfinished == 0) {
                    _celebrationEvent.emit("Всі звички на сьогодні виконано! Вогник горить яскраво! 🔥🔥🔥")
                } else {
                    _celebrationEvent.emit("Чудово! Ще один крок до непереможної серії! 🔥")
                }
            }
        }
    }

    fun saveHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.saveHabit(habit)
            dismissAddEdit()
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
            if (_editingHabit.value?.id == habitId) {
                dismissAddEdit()
            }
        }
    }

    fun toggleArchive(habit: HabitEntity) {
        viewModelScope.launch {
            repository.toggleArchiveHabit(habit)
        }
    }
}
