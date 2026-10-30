package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BookmarkEntity
import com.example.data.model.BottomNavTab
import com.example.data.model.CategoryEntity
import com.example.data.model.ChapterEntity
import com.example.data.model.CommentEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.ReadingProgressEntity
import com.example.data.model.ReadingThemeMode
import com.example.data.model.ReviewEntity
import com.example.data.model.Screen
import com.example.data.model.StoryEntity
import com.example.data.model.UserEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.StoryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val storyRepository = StoryRepository(database.storyDao(), database.interactionDao())
    val authRepository = AuthRepository(database.userDao(), database.interactionDao(), application, viewModelScope)

    // Navigation BackStack
    private val _backStack = MutableStateFlow<List<Screen>>(listOf(Screen.Splash))
    val currentScreen: StateFlow<Screen> = _backStack.combine(MutableStateFlow(Unit)) { stack, _ ->
        stack.lastOrNull() ?: Screen.Main()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Screen.Splash)

    val currentTab = MutableStateFlow(BottomNavTab.HOME)

    // Current User & Admin
    val currentUser: StateFlow<UserEntity?> = authRepository.currentUser
    val isAdminAuthenticated: StateFlow<Boolean> = authRepository.isAdminAuthenticated

    // Theme state
    val themeMode = MutableStateFlow("SYSTEM") // SYSTEM, LIGHT, DARK

    // Stories & Categories data
    val allStories: StateFlow<List<StoryEntity>> = storyRepository.allStories
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val featuredStories: StateFlow<List<StoryEntity>> = storyRepository.featuredStories
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val popularStories: StateFlow<List<StoryEntity>> = storyRepository.popularStories
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val newStories: StateFlow<List<StoryEntity>> = storyRepository.newStories
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val topRatedStories: StateFlow<List<StoryEntity>> = storyRepository.topRatedStories
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val categories: StateFlow<List<CategoryEntity>> = storyRepository.categories
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Search
    val searchQuery = MutableStateFlow("")
    private val _searchResults = MutableStateFlow<List<StoryEntity>>(emptyList())
    val searchResults: StateFlow<List<StoryEntity>> = _searchResults.asStateFlow()

    // Reading preferences
    val readerFontSize = MutableStateFlow(18f)
    val readerLineHeight = MutableStateFlow(1.7f)
    val readerThemeMode = MutableStateFlow(ReadingThemeMode.LIGHT)
    val readerAutoScroll = MutableStateFlow(false)
    val readerScrollSpeed = MutableStateFlow(1) // 1, 2, 3
    val readerTimerSeconds = MutableStateFlow(0L)

    private var timerJob: Job? = null

    // Notifications
    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        // Collect notifications for current user
        viewModelScope.launch {
            currentUser.collect { user ->
                val uid = user?.id ?: "guest"
                launch {
                    authRepository.getNotifications(uid).collect { list ->
                        _notifications.value = list
                    }
                }
                launch {
                    authRepository.getUnreadCount(uid).collect { count ->
                        _unreadCount.value = count
                    }
                }
            }
        }
    }

    // Navigation Methods
    fun navigateTo(screen: Screen) {
        val current = _backStack.value.toMutableList()
        current.add(screen)
        _backStack.value = current
    }

    fun navigateBack(): Boolean {
        val current = _backStack.value.toMutableList()
        if (current.size > 1) {
            current.removeAt(current.size - 1)
            _backStack.value = current
            return true
        }
        return false
    }

    fun switchTab(tab: BottomNavTab) {
        currentTab.value = tab
        // If not in Main screen, return to Main
        val current = _backStack.value.toMutableList()
        if (current.lastOrNull() !is Screen.Main) {
            current.clear()
            current.add(Screen.Main(tab))
            _backStack.value = current
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
        } else {
            viewModelScope.launch {
                storyRepository.searchStories(query.trim()).collect {
                    _searchResults.value = it
                }
            }
        }
    }

    // Story Actions
    fun openStoryDetails(storyId: String) {
        navigateTo(Screen.StoryDetail(storyId))
    }

    fun startReading(storyId: String, chapterNumber: Int = 1) {
        viewModelScope.launch {
            storyRepository.incrementReadCount(storyId)
        }
        resetReaderTimer()
        startReaderTimer()
        navigateTo(Screen.StoryReader(storyId, chapterNumber))
    }

    fun toggleFavorite(storyId: String, currentStatus: Boolean) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            storyRepository.toggleFavorite(uid, storyId, currentStatus)
        }
    }

    fun toggleFollowAuthor(authorName: String, currentStatus: Boolean) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            storyRepository.toggleFollowAuthor(uid, authorName, currentStatus)
        }
    }

    // Reader Progress & Bookmarks
    fun saveProgress(storyId: String, chapterNumber: Int, percent: Float) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            storyRepository.saveReadingProgress(uid, storyId, chapterNumber, percent)
        }
    }

    fun addBookmark(storyId: String, chapterNumber: Int, paragraphIndex: Int, snippet: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            storyRepository.addBookmark(uid, storyId, chapterNumber, paragraphIndex, snippet)
        }
    }

    fun removeBookmark(storyId: String, chapterNumber: Int, paragraphIndex: Int) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            storyRepository.removeBookmarkAt(uid, storyId, chapterNumber, paragraphIndex)
        }
    }

    fun addHighlight(storyId: String, chapterNumber: Int, paragraphIndex: Int, text: String, colorHex: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            storyRepository.addHighlight(uid, storyId, chapterNumber, paragraphIndex, text, colorHex)
        }
    }

    fun addComment(storyId: String, chapterNumber: Int, text: String) {
        val user = currentUser.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            storyRepository.addComment(
                storyId = storyId,
                chapterNumber = chapterNumber,
                userId = user.id,
                userName = user.fullName,
                userAvatar = user.profilePic,
                userVerified = user.isVerified,
                text = text.trim()
            )
        }
    }

    fun addReview(storyId: String, rating: Int, text: String) {
        val user = currentUser.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            storyRepository.addReview(
                storyId = storyId,
                userId = user.id,
                userName = user.fullName,
                rating = rating,
                text = text.trim()
            )
        }
    }

    // Timer
    private fun startReaderTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                readerTimerSeconds.value += 1
            }
        }
    }

    fun stopReaderTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun resetReaderTimer() {
        readerTimerSeconds.value = 0L
    }

    // Admin Operations
    fun saveStory(story: StoryEntity) {
        viewModelScope.launch {
            storyRepository.insertStory(story)
        }
    }

    fun updateStory(story: StoryEntity) {
        viewModelScope.launch {
            storyRepository.updateStory(story)
        }
    }

    fun deleteStory(story: StoryEntity) {
        viewModelScope.launch {
            storyRepository.deleteStory(story)
        }
    }

    fun saveChapter(chapter: ChapterEntity) {
        viewModelScope.launch {
            storyRepository.insertChapter(chapter)
        }
    }

    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch {
            storyRepository.insertCategory(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            storyRepository.deleteCategory(category)
        }
    }

    fun sendBroadcast(title: String, message: String, type: String) {
        viewModelScope.launch {
            authRepository.broadcastNotification(title, message, type)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopReaderTimer()
    }
}
