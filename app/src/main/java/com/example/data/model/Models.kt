package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val username: String,
    val email: String,
    val passwordHash: String,
    val profilePic: String = "",
    val bio: String = "",
    val isVerified: Boolean = false,
    val isAdmin: Boolean = false,
    val isBanned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val authorId: String,
    val authorName: String,
    val authorVerified: Boolean = false,
    val categoryId: String,
    val categoryName: String,
    val coverResName: String,
    val totalChapters: Int = 1,
    val readCount: Int = 0,
    val likesCount: Int = 0,
    val rating: Float = 5.0f,
    val ratingCount: Int = 1,
    val isPublished: Boolean = true,
    val isFeatured: Boolean = false,
    val isPinned: Boolean = false,
    val publishedDate: String = "আজ",
    val tags: String = ""
)

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey val id: String,
    val storyId: String,
    val chapterNumber: Int,
    val chapterTitle: String,
    val content: String,
    val wordCount: Int,
    val estimatedMinutes: Int
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconName: String,
    val orderIndex: Int,
    val storyCount: Int = 0
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val storyId: String,
    val chapterNumber: Int,
    val paragraphIndex: Int,
    val paragraphSnippet: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val key: String, // userId_storyId
    val userId: String,
    val storyId: String,
    val lastChapterNumber: Int,
    val progressPercent: Float,
    val lastReadTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "highlights")
data class HighlightEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val storyId: String,
    val chapterNumber: Int,
    val paragraphIndex: Int,
    val highlightText: String,
    val colorHex: String = "#FFEB3B",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val storyId: String,
    val chapterNumber: Int = 0,
    val userId: String,
    val userName: String,
    val userAvatar: String = "",
    val userVerified: Boolean = false,
    val commentText: String,
    val likes: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
    val storyId: String,
    val userId: String,
    val userName: String,
    val userAvatar: String = "",
    val rating: Int = 5,
    val reviewText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteStoryEntity(
    @PrimaryKey val id: String, // userId_storyId
    val userId: String,
    val storyId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "followed_authors")
data class FollowAuthorEntity(
    @PrimaryKey val id: String, // userId_authorName
    val userId: String,
    val authorName: String
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String, // specific userId or "ALL"
    val title: String,
    val message: String,
    val type: String, // STORY, CHAPTER, ANNOUNCEMENT, SYSTEM
    val relatedStoryId: String? = null,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val configKey: String,
    val configValue: String
)

// UI and Navigation Models
enum class BottomNavTab(val title: String) {
    HOME("হোম"),
    LIBRARY("লাইব্রেরি"),
    DISCOVER("আবিষ্কার"),
    COMMUNITY("কমিউনিটি"),
    PROFILE("প্রোফাইল")
}

sealed class Screen {
    data object Splash : Screen()
    data object Auth : Screen()
    data class Main(val initialTab: BottomNavTab = BottomNavTab.HOME) : Screen()
    data class StoryDetail(val storyId: String) : Screen()
    data class StoryReader(val storyId: String, val chapterNumber: Int = 1) : Screen()
    data class CategoryStories(val categoryId: String, val categoryName: String) : Screen()
    data object AdminDashboard : Screen()
    data object AdminLogin : Screen()
    data object OwnerProfile : Screen()
    data object EditProfile : Screen()
    data object Notifications : Screen()
    data object Settings : Screen()
}

enum class ReadingThemeMode {
    LIGHT, SEPIA, DARK, NIGHT
}
