package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AppConfigEntity
import com.example.data.model.BookmarkEntity
import com.example.data.model.CommentEntity
import com.example.data.model.FavoriteStoryEntity
import com.example.data.model.FollowAuthorEntity
import com.example.data.model.HighlightEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.ReadingProgressEntity
import com.example.data.model.ReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InteractionDao {
    // Bookmarks
    @Query("SELECT * FROM bookmarks WHERE userId = :userId ORDER BY createdAt DESC")
    fun getBookmarks(userId: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE userId = :userId AND storyId = :storyId AND chapterNumber = :chapterNumber AND paragraphIndex = :paragraphIndex")
    suspend fun removeBookmarkAt(userId: String, storyId: String, chapterNumber: Int, paragraphIndex: Int)

    // Reading Progress
    @Query("SELECT * FROM reading_progress WHERE userId = :userId ORDER BY lastReadTimestamp DESC")
    fun getReadingProgressList(userId: String): Flow<List<ReadingProgressEntity>>

    @Query("SELECT * FROM reading_progress WHERE userId = :userId AND storyId = :storyId LIMIT 1")
    suspend fun getProgressForStory(userId: String, storyId: String): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress WHERE userId = :userId AND storyId = :storyId LIMIT 1")
    fun observeProgressForStory(userId: String, storyId: String): Flow<ReadingProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReadingProgress(progress: ReadingProgressEntity)

    // Highlights
    @Query("SELECT * FROM highlights WHERE userId = :userId AND storyId = :storyId AND chapterNumber = :chapterNumber")
    fun getHighlights(userId: String, storyId: String, chapterNumber: Int): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllHighlights(userId: String): Flow<List<HighlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity)

    @Delete
    suspend fun deleteHighlight(highlight: HighlightEntity)

    // Comments
    @Query("SELECT * FROM comments WHERE storyId = :storyId ORDER BY timestamp DESC")
    fun getCommentsForStory(storyId: String): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments ORDER BY timestamp DESC")
    fun getAllComments(): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Delete
    suspend fun deleteComment(comment: CommentEntity)

    // Reviews
    @Query("SELECT * FROM reviews WHERE storyId = :storyId ORDER BY timestamp DESC")
    fun getReviewsForStory(storyId: String): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)

    // Favorites
    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY timestamp DESC")
    fun getFavorites(userId: String): Flow<List<FavoriteStoryEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND storyId = :storyId)")
    fun isFavorite(userId: String, storyId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(fav: FavoriteStoryEntity)

    @Delete
    suspend fun removeFavorite(fav: FavoriteStoryEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND storyId = :storyId")
    suspend fun removeFavoriteById(userId: String, storyId: String)

    // Followed Authors
    @Query("SELECT * FROM followed_authors WHERE userId = :userId")
    fun getFollowedAuthors(userId: String): Flow<List<FollowAuthorEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM followed_authors WHERE userId = :userId AND authorName = :authorName)")
    fun isFollowingAuthor(userId: String, authorName: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun followAuthor(follow: FollowAuthorEntity)

    @Delete
    suspend fun unfollowAuthor(follow: FollowAuthorEntity)

    @Query("DELETE FROM followed_authors WHERE userId = :userId AND authorName = :authorName")
    suspend fun unfollowAuthorByName(userId: String, authorName: String)

    // Notifications
    @Query("SELECT * FROM notifications WHERE userId = :userId OR userId = 'ALL' ORDER BY timestamp DESC")
    fun getNotifications(userId: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE (userId = :userId OR userId = 'ALL') AND isRead = 0")
    fun getUnreadNotificationCount(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId OR userId = 'ALL'")
    suspend fun markAllNotificationsAsRead(userId: String)

    @Delete
    suspend fun deleteNotification(notification: NotificationEntity)

    // App Config
    @Query("SELECT configValue FROM app_config WHERE configKey = :key LIMIT 1")
    suspend fun getConfig(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setConfig(config: AppConfigEntity)
}
